/**
 * MilkTea Cashier POS & Table Management Logic
 * Tuân thủ frontend_rule.md và acceptance_tests.md
 */

const CashierApp = (() => {
    let currentTables = [];
    let products = [];
    let selectedCategory = "";
    let filtersReady = false;
    let counterCart = [];
    let pendingCounterOrder = null;
    let submitting = false;
    let creationKey = null;
    let creationFingerprint = null;
    let realtimeReady = false;

    async function init() {
        window.MilkTeaContext = { role: document.getElementById('posRole')?.dataset.role || 'CASHIER' };
        MilkTeaRealtime.connect();

        if (!realtimeReady) { setupRealtime(); realtimeReady = true; }
        setupProducts();
        await loadTables();
    }

    function setupRealtime() {
        MilkTeaRealtime.on('ORDER_CREATED', () => {
            MilkTeaApi.showToast('Có đơn hàng mới!', 'success');
            loadTables();
        });

        MilkTeaRealtime.on('ORDER_STATUS_CHANGED', () => {
        });

        MilkTeaRealtime.on('TABLE_SESSION_OPENED', () => {
            loadTables();
        });

        MilkTeaRealtime.on('TABLE_SESSION_CLOSED', () => {
            loadTables();
        });

        MilkTeaRealtime.on('PAYMENT_RECORDED', () => {
        });

        MilkTeaRealtime.on('PAYMENT_NOTICE_CREATED', (e) => {
            MilkTeaApi.showToast('Có yêu cầu thanh toán mới. Mở Đơn Đã Đặt để kiểm tra và xác nhận thu tiền.', 'warning');
        });

        MilkTeaRealtime.onReconnect(() => {
            loadTables();
        });
    }

    async function loadTables() {
        try {
            const tables = await MilkTeaApi.get('/api/v1/cashier/tables');
            currentTables = tables || [];
            renderTablesGrid();
        } catch (e) {
            console.warn('Lỗi nạp danh sách bàn:', e);
        }
    }

    function renderTablesGrid() {
        const grid = document.getElementById('tablesGrid');
        if (!grid) return;

        let html = '';
        currentTables.forEach(t => {
            const isBusy = t.status === 'CO_KHACH';
            html += `
                <div class="pos-table-card ${t.status}" data-table-id="${t.id}">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="fw-bold mb-0">${MilkTeaReceipts.esc(t.name)}</h6>
                            <span class="badge ${isBusy ? 'bg-danger' : 'bg-success'} rounded-pill">
                                ${isBusy ? 'Có khách' : t.status==='BAO_TRI' ? 'Bảo trì' : 'Trống'}
                            </span>
                        </div>
                        <div class="small text-muted mb-3">
                            ${isBusy ? `Phiên #${t.activeSessionId}` : 'Sẵn sàng phục vụ'}
                        </div>
                        <div class="d-grid gap-1">
                            ${t.status==='BAO_TRI' ? '<span class="text-muted small">Tạm ngừng phục vụ</span>' : !isBusy ? `
                                <button type="button" class="btn btn-sm btn-outline-primary rounded-pill" onclick="CashierApp.openSession(${t.id})">
                                    <i class="bi bi-box-arrow-in-right me-1"></i> Mở Bàn
                                </button>
                            ` : `
                                <button type="button" class="btn btn-sm btn-outline-danger rounded-pill" onclick="CashierApp.closeSession(${t.activeSessionId})">
                                    <i class="bi bi-door-closed me-1"></i> Đóng Phiên
                                </button>
                            `}
                        </div>
                </div>
            `;
        });
        grid.innerHTML = html;
        const select=document.getElementById('counterTableId');
        const selected=select.value;
        select.innerHTML='<option value="">Quầy / mang về</option>'+currentTables.filter(t=>t.status!=='BAO_TRI').map(t=>`<option value="${t.id}">${MilkTeaReceipts.esc(t.name)}</option>`).join('');
        select.value=selected;
        grid.querySelectorAll('[data-table-id]').forEach(card=>card.addEventListener('click',event=> {
            if(event.target.closest('button') || currentTables.find(t=>String(t.id)===card.dataset.tableId)?.status==='BAO_TRI') return;
            select.value=card.dataset.tableId;
            grid.querySelectorAll('.selected').forEach(c=>c.classList.remove('selected'));card.classList.add('selected');
        }));
    }

    async function openSession(tableId) {
        try {
            await MilkTeaApi.post(`/api/v1/cashier/tables/${tableId}/open-session`);
            MilkTeaApi.showToast('Đã mở phiên cho bàn!', 'success');
            await loadTables();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi mở phiên bàn', 'error');
        }
    }

    async function closeSession(sessionId) {
        const tableName = currentTables.find(t => t.activeSessionId === sessionId)?.name || "bàn";
        if (!confirm(`Bạn có chắc chắn muốn đóng phiên của ${tableName}?\nLưu ý: Mọi đơn hàng phải hoàn tất hoặc hủy và đã thanh toán đủ.`)) {
            return;
        }

        try {
            if(await showSessionBlockers(sessionId,tableName)) return;
            await MilkTeaApi.post(`/api/v1/cashier/table-sessions/${sessionId}/close`);
            MilkTeaApi.showToast(`Đã đóng phiên của ${tableName} thành công!`, 'success');
            await loadTables();
        } catch (e) {
            if(!await showSessionBlockers(sessionId,tableName)) MilkTeaApi.showToast(e.message || 'Không thể đóng phiên bàn', 'error');
        }
    }

    // POS Quầy: Thêm món vào giỏ quầy
    function addCounterCart(p) {
        if (submitting || pendingCounterOrder) { MilkTeaApi.showToast('Hoàn tất thanh toán đơn đang tạo trước.', 'warning'); return; }
        const existing = counterCart.find(it => it.productId === p.id);
        if (existing) {
            existing.quantity++;
        } else {
            counterCart.push({
                productId: p.id,
                productName: p.name,
                size: p.size,
                price: p.price,
                quantity: 1,
                imageUrl: p.imageUrl,
                note: ''
            });
        }
        renderCounterCart();
    }

    function renderCounterCart() {
        const container = document.getElementById('counterCartList');
        if (!container) return;

        document.getElementById('counterItemCount').textContent='('+counterCart.reduce((n,i)=>n+i.quantity,0)+')';
        if (counterCart.length === 0) {
            container.innerHTML = '<div class="text-center text-muted small py-4">Chưa chọn món nào</div>';
            document.getElementById('counterCartTotal').textContent = '0 ₫';
            return;
        }

        let total = 0;
        let html = '';
        counterCart.forEach((it, idx) => {
            const lineTotal = it.price * it.quantity;
            total += lineTotal;
            html += `<div class="pos-cart-item"><img src="${MilkTeaReceipts.esc(it.imageUrl || '/images/receipt-logo.svg')}" alt="${MilkTeaReceipts.esc(it.productName)}"><div class="pos-cart-info"><div class="d-flex justify-content-between gap-2"><strong>${MilkTeaReceipts.esc(it.productName)}</strong><button class="btn btn-sm p-0 text-muted" aria-label="Xóa món" onclick="CashierApp.removeItem(${idx})">×</button></div><small class="text-muted">Size ${MilkTeaReceipts.esc(it.size)}</small><div class="d-flex justify-content-between align-items-center mt-2"><div class="pos-qty"><button aria-label="Giảm số lượng" onclick="CashierApp.adjustCounterCart(${idx},-1)">−</button><span>${it.quantity}</span><button aria-label="Tăng số lượng" onclick="CashierApp.adjustCounterCart(${idx},1)">+</button></div><span class="pos-cart-price">${MilkTeaApi.formatVND(lineTotal)}</span></div></div></div>`;
        });
        container.innerHTML = html;
        document.getElementById('counterCartTotal').textContent = MilkTeaApi.formatVND(total);
    }

    function adjustCounterCart(idx, delta) {
        if (submitting || pendingCounterOrder) return;
        if (!counterCart[idx]) return;
        counterCart[idx].quantity += delta;
        if (counterCart[idx].quantity <= 0) {
            counterCart.splice(idx, 1);
        }
        renderCounterCart();
    }

    async function submitCounterOrder() {
        if (submitting) return;
        if (counterCart.length === 0 && !pendingCounterOrder) {
            MilkTeaApi.showToast('Vui lòng chọn món trước khi tạo đơn', 'warning');
            return;
        }

        submitting = true;
        const submitBtn = document.getElementById('counterSubmitBtn');
        submitBtn.disabled = true;
        try {
            const items = counterCart.map(it => ({
                productId: it.productId,
                quantity: it.quantity,
                note: [it.note, document.getElementById('counterOrderNote')?.value.trim()].filter(Boolean).join(' · ')
            }));

            const tableId = document.getElementById('counterTableId').value;
            const table = currentTables.find(t => String(t.id) === tableId);
            const payload = tableId ? {tableId, expectedSessionId: table?.activeSessionId || null, items, remainingCartItems: []} : {items};
            const fingerprint = JSON.stringify(payload);
            if (fingerprint !== creationFingerprint) { creationFingerprint = fingerprint; creationKey = crypto.randomUUID(); }
            const created = pendingCounterOrder || await MilkTeaApi.post(tableId ? '/api/v1/orders/table' : '/api/v1/orders/counter', payload, {'Idempotency-Key':creationKey});
            pendingCounterOrder = created;
            await MilkTeaApi.post(`/api/v1/cashier/orders/${created.orderId}/payments`, {method: document.getElementById('counterPayMethod').value, reference: 'POS_' + created.orderId}, {'Idempotency-Key': 'POS_PAYMENT_' + created.orderId});
            pendingCounterOrder = null;
            creationKey = null; creationFingerprint = null;
            await MilkTeaReceipts.open(created.orderId);
            MilkTeaApi.showToast(`Tạo đơn #${created.orderId} thành công!`, 'success');
            counterCart = [];
            document.getElementById('counterOrderNote').value='';
            renderCounterCart();
            submitBtn.innerHTML = 'Thanh toán';
            await loadTables();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo đơn / ghi thu', 'error');
            if (pendingCounterOrder) submitBtn.textContent = 'Thử lại thanh toán đơn #' + pendingCounterOrder.orderId;
        } finally { submitting = false; submitBtn.disabled = false; }
    }


    function setupProducts() {
        products=[...document.querySelectorAll('#posProductsData [data-id]')].map(el=>({id:Number(el.dataset.id),name:el.dataset.name,size:el.dataset.size,price:Number(el.dataset.price),imageUrl:el.dataset.image,category:el.dataset.category}));
        if(!filtersReady) {
            ['quickProductSearch','posProductSearch'].forEach(id=>document.getElementById(id).addEventListener('input',event=> {
                ['quickProductSearch','posProductSearch'].forEach(other=>document.getElementById(other).value=event.target.value);
                renderProducts();
            }));
            document.getElementById('posCategories').addEventListener('click',event=> {
                const b=event.target.closest('[data-category]');if(!b)return;
                selectedCategory=b.dataset.category;
                document.querySelectorAll('#posCategories button').forEach(el=>el.classList.toggle('active',el===b));renderProducts();
            });
            document.getElementById('posProductGrid').addEventListener('click',event=> {
                const b=event.target.closest('[data-product-id]');if(b)addCounterCart(products.find(p=>String(p.id)===b.dataset.productId));
            });filtersReady=true;
        }renderProducts();
    }
    function renderProducts() {
        const query=document.getElementById('posProductSearch').value.trim().toLocaleLowerCase('vi');
        const groups=new Map();
        products.filter(p=>(!selectedCategory || p.category===selectedCategory) && p.name.toLocaleLowerCase('vi').includes(query)).forEach(p=> {
            const key=p.name+'|'+p.category;if(!groups.has(key))groups.set(key,[]);groups.get(key).push(p);
        });
        document.getElementById('posProductGrid').innerHTML=[...groups.values()].map(variants=> {
            variants.sort((a,b)=>a.price-b.price);const p=variants[0],esc=MilkTeaReceipts.esc;
            return `<article class="pos-product"><img src="${esc(p.imageUrl || '/images/receipt-logo.svg')}" alt="${esc(p.name)}" loading="lazy"><div class="pos-product-body"><h6>${esc(p.name)}</h6><div class="pos-product-price">${variants.length>1?'Từ ':''}${MilkTeaApi.formatVND(p.price)}</div><div class="pos-sizes">${variants.map(v=>`<button data-product-id="${v.id}" title="Size ${esc(v.size)} · ${MilkTeaApi.formatVND(v.price)}">${esc(v.size)}</button>`).join('')}</div></div></article>`;
        }).join('') || '<p class="text-muted p-3">Không có món phù hợp.</p>';
    }
    function clearCart() {if(submitting || pendingCounterOrder)return;counterCart=[];renderCounterCart();}
    function removeItem(idx) {if(submitting || pendingCounterOrder)return;counterCart.splice(idx,1);renderCounterCart();}
    async function showSessionBlockers(sessionId,tableName) {
        try {
            const orders=await MilkTeaApi.get('/api/v1/cashier/orders');
            const sessionOrders=(orders || []).filter(o=>String(o.sessionId)===String(sessionId));
            const unpaid=sessionOrders.filter(o=>o.status!=='DA_HUY' && !o.paid && Number(o.totalAmount)>0);
            const unfinished=sessionOrders.filter(o=>!['DA_HUY','HOAN_THANH'].includes(o.status));
            if(!unpaid.length&&!unfinished.length)return false;
            const esc=MilkTeaReceipts.esc;
            const section=(title,list)=>list.length?`<h6 class="mt-3">${title}</h6><div class="table-responsive"><table class="table"><thead><tr><th>Đơn hàng</th><th>Tổng tiền</th><th>Kiểm tra</th></tr></thead><tbody>${list.map(o=>`<tr><td>${MilkTeaReceipts.code(o.orderId)}</td><td>${MilkTeaApi.formatVND(o.totalAmount)}</td><td><a class="btn btn-sm btn-outline-primary" href="/orders?order=${o.orderId}">Xem đơn</a></td></tr>`).join('')}</tbody></table></div>`:'';
            document.getElementById('tableBlockersTitle').textContent='Chưa thể đóng phiên '+tableName;
            document.getElementById('tableBlockersBody').innerHTML='<p class="text-muted">Kiểm tra các đơn dưới đây trước khi đóng phiên.</p>'+section('Đơn chưa thanh toán',unpaid)+section('Đơn chưa hoàn thành',unfinished);
            bootstrap.Modal.getOrCreateInstance(document.getElementById('tableBlockersModal')).show();return true;
        }catch(e){return false;}
    }
    return {init,refreshTables:loadTables,openSession,closeSession,addCounterCart,adjustCounterCart,submitCounterOrder,clearCart,removeItem};
})();
