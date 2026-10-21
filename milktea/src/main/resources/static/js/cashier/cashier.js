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
            loadTables();
        });

        MilkTeaRealtime.on('ORDER_ASSIGNED_TO_TABLE', () => {
            loadTables();
        });

        MilkTeaRealtime.on('TABLE_SESSION_OPENED', () => {
            loadTables();
        });

        MilkTeaRealtime.on('TABLE_SESSION_CLOSED', () => {
            loadTables();
        });

        MilkTeaRealtime.on('PAYMENT_RECORDED', () => {
            loadTables();
        });

        MilkTeaRealtime.on('REFUND_CHANGED', () => {
            loadTables();
        });

        MilkTeaRealtime.on('PAYMENT_NOTICE_CREATED', (e) => {
            MilkTeaApi.showToast('Có yêu cầu thanh toán mới. Mở Đơn Đã Đặt để kiểm tra và xác nhận thu tiền.', 'warning');
        });

        MilkTeaRealtime.on('SETTINGS_CHANGED', async () => {
            try {
                const info = await MilkTeaApi.get('/api/v1/public/shop-info');
                if (info && info.logoUrl) {
                    document.querySelectorAll('.navbar-milktea .navbar-brand').forEach(brand => {
                        let img = brand.querySelector('img.navbar-shop-logo');
                        const badge = brand.querySelector('.navbar-brand-badge');
                        if (!img) {
                            img = document.createElement('img');
                            img.alt = 'Logo';
                            img.className = 'rounded-circle shadow-sm navbar-shop-logo';
                            img.style = 'width: 38px; height: 38px; object-fit: cover; border: 1px solid rgba(0,0,0,0.08);';
                            brand.prepend(img);
                        }
                        img.src = info.logoUrl;
                        img.style.display = 'inline-block';
                        if (badge) badge.style.setProperty('display', 'none', 'important');
                    });
                }
                if (info && info.shopName) {
                    document.querySelectorAll('.navbar-shop-name').forEach(el => el.textContent = info.shopName);
                }
            } catch (e) {
                console.debug('Error refreshing shop info on SETTINGS_CHANGED:', e);
            }
        });

        MilkTeaRealtime.onReconnect(() => {
            loadTables();
        });

        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                loadTables();
            }
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
                                ${isBusy ? 'Có khách' : t.status === 'BAO_TRI' ? 'Bảo trì' : 'Trống'}
                            </span>
                        </div>
                        <div class="small text-muted mb-3">
                            ${isBusy ? `Phiên #${t.activeSessionId}` : 'Sẵn sàng phục vụ'}
                        </div>
                        <div class="d-grid gap-1">
                            ${t.status === 'BAO_TRI' ? '<span class="text-muted small">Tạm ngừng phục vụ</span>' : !isBusy ? `
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
        const select = document.getElementById('counterTableId');
        const selected = select.value;
        select.innerHTML = '<option value="">Quầy / mang về</option>' + currentTables.filter(t => t.status !== 'BAO_TRI').map(t => `<option value="${t.id}">${MilkTeaReceipts.esc(t.name)}</option>`).join('');
        select.value = selected;
        grid.querySelectorAll('[data-table-id]').forEach(card => card.addEventListener('click', event => {
            if (event.target.closest('button') || currentTables.find(t => String(t.id) === card.dataset.tableId)?.status === 'BAO_TRI') return;
            select.value = card.dataset.tableId;
            grid.querySelectorAll('.selected').forEach(c => c.classList.remove('selected')); card.classList.add('selected');
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
            if (await showSessionBlockers(sessionId, tableName)) return;
            await MilkTeaApi.post(`/api/v1/cashier/table-sessions/${sessionId}/close`);
            MilkTeaApi.showToast(`Đã đóng phiên của ${tableName} thành công!`, 'success');
            await loadTables();
        } catch (e) {
            if (!await showSessionBlockers(sessionId, tableName)) MilkTeaApi.showToast(e.message || 'Không thể đóng phiên bàn', 'error');
        }
    }

    let activeToppingLineId = null;

    function isToppingProduct(p) {
        if (!p) return false;
        const catName = (p.categoryName || '').toLowerCase();
        return catName.includes('topping');
    }

    // POS Quầy: Thêm món vào giỏ quầy
    function addCounterCart(p) {
        if (submitting || pendingCounterOrder) { MilkTeaApi.showToast('Hoàn tất thanh toán đơn đang tạo trước.', 'warning'); return; }

        // Nếu đang ở chế độ thêm topping cho một dòng món
        if (activeToppingLineId) {
            const targetLine = counterCart.find(it => it.lineId === activeToppingLineId);
            if (!targetLine) {
                exitToppingMode();
            } else {
                const isTopping = p.isTopping || isToppingProduct(p);
                if (isTopping) {
                    const targetProd = products.find(prod => prod.id === targetLine.productId);
                    if (targetProd && targetProd.allowTopping === false) {
                        MilkTeaApi.showToast(`Món ${targetLine.productName} không cho phép chọn topping đi kèm!`, 'warning');
                        return;
                    }

                    if (!targetLine.toppings) targetLine.toppings = [];
                    const existsIdx = targetLine.toppings.findIndex(t => t.id === p.id);
                    if (existsIdx >= 0) {
                        targetLine.toppings.splice(existsIdx, 1);
                        MilkTeaApi.showToast(`Đã bỏ topping ${p.name}`, 'info');
                    } else {
                        targetLine.toppings.push({ id: p.id, name: p.name, price: p.price });
                        MilkTeaApi.showToast(`Đã thêm ${p.name} vào ${targetLine.productName}`, 'success');
                    }
                    renderCounterCart();
                    return;
                } else {
                    // Nhấn vào món nước khác -> Tự động kết thúc chế độ topping
                    exitToppingMode();
                }
            }
        }

        // Tạo dòng mới có stable lineId
        const lineId = 'line_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7);
        counterCart.push({
            lineId: lineId,
            productId: p.id,
            productName: p.name,
            size: p.size,
            price: p.price,
            quantity: 1,
            imageUrl: p.imageUrl,
            note: '',
            toppings: [],
            noteOpen: false
        });
        renderCounterCart();
    }

    function enterToppingMode(lineId) {
        const item = counterCart.find(it => it.lineId === lineId);
        if (!item) return;

        const prod = products.find(p => p.id === item.productId);
        if (prod && prod.allowTopping === false) {
            MilkTeaApi.showToast(`Món ${item.productName} thuộc danh mục không cho phép chọn topping!`, 'warning');
            return;
        }

        activeToppingLineId = lineId;

        // Chuyển sang tab Topping
        const toppingBtn = [...document.querySelectorAll('#posCategories button')].find(b => b.textContent.trim().toLowerCase().includes('topping'));
        if (toppingBtn) {
            selectedCategory = toppingBtn.dataset.category;
            document.querySelectorAll('#posCategories button').forEach(el => el.classList.toggle('active', el === toppingBtn));
        }

        const banner = document.getElementById('posToppingModeBanner');
        if (banner) {
            document.getElementById('toppingTargetLineName').textContent = `${item.productName} (Size ${item.size})`;
            banner.classList.remove('d-none');
        }
        renderProducts();
        MilkTeaApi.showToast(`Chọn topping để thêm/bỏ cho ${item.productName}`, 'info');
    }

    function exitToppingMode() {
        activeToppingLineId = null;
        const banner = document.getElementById('posToppingModeBanner');
        if (banner) banner.classList.add('d-none');
        selectedCategory = '';
        document.querySelectorAll('#posCategories button').forEach((el, idx) => el.classList.toggle('active', idx === 0));
        renderProducts();
    }

    function changeLineSize(lineId, newProductId) {
        const item = counterCart.find(it => it.lineId === lineId);
        const newProd = products.find(p => p.id === Number(newProductId));
        if (item && newProd) {
            item.productId = newProd.id;
            item.size = newProd.size;
            item.price = newProd.price;
            renderCounterCart();
        }
    }

    function toggleLineNote(lineId) {
        const item = counterCart.find(it => it.lineId === lineId);
        if (item) {
            item.noteOpen = !item.noteOpen;
            renderCounterCart();
        }
    }

    function updateLineNote(lineId, val) {
        const item = counterCart.find(it => it.lineId === lineId);
        if (item) {
            item.note = val.trim();
        }
    }

    function removeLineTopping(lineId, toppingId) {
        const item = counterCart.find(it => it.lineId === lineId);
        if (item && item.toppings) {
            item.toppings = item.toppings.filter(t => t.id !== toppingId);
            renderCounterCart();
        }
    }

    function removeLine(lineId) {
        if (submitting || pendingCounterOrder) return;
        if (activeToppingLineId === lineId) exitToppingMode();
        counterCart = counterCart.filter(it => it.lineId !== lineId);
        renderCounterCart();
    }

    function renderCounterCart() {
        const container = document.getElementById('counterCartList');
        if (!container) return;

        document.getElementById('counterItemCount').textContent = '(' + counterCart.reduce((n, i) => n + i.quantity, 0) + ')';
        if (counterCart.length === 0) {
            container.innerHTML = '<div class="text-center text-muted small py-4">Chưa chọn món nào</div>';
            document.getElementById('counterCartTotal').textContent = '0 ₫';
            return;
        }

        let total = 0;
        let html = '';
        counterCart.forEach((it) => {
            const toppingSum = (it.toppings || []).reduce((acc, t) => acc + (t.price || 0), 0);
            const unitPrice = (it.price || 0) + toppingSum;
            const lineTotal = unitPrice * it.quantity;
            total += lineTotal;

            // Lấy danh sách size từ catalog cho sản phẩm này
            const variants = products.filter(p => p.name === it.productName);

            let sizeSelector = '';
            if (variants.length > 1) {
                sizeSelector = `<select class="form-select form-select-sm py-0 px-2 my-1" style="font-size: 0.8rem; height: 26px;" onchange="CashierApp.changeLineSize('${it.lineId}', this.value)">
                    ${variants.map(v => `<option value="${v.id}" ${v.id === it.productId ? 'selected' : ''}>Size ${v.size} (${MilkTeaApi.formatVND(v.price)})</option>`).join('')}
                </select>`;
            } else {
                sizeSelector = `<small class="text-muted d-block my-1">Size ${it.size} · ${MilkTeaApi.formatVND(it.price)}</small>`;
            }

            const isCurrentToppingTarget = activeToppingLineId === it.lineId;
            const targetProd = products.find(p => p.id === it.productId);
            const isProdTopping = targetProd ? (targetProd.isTopping || isToppingProduct(targetProd)) : false;
            const canHaveTopping = !isProdTopping && (!targetProd || targetProd.allowTopping !== false);

            let toppingControlHtml = '';
            if (canHaveTopping) {
                toppingControlHtml = `
                    <div class="my-1">
                        <button type="button" class="btn btn-sm btn-link p-0 text-decoration-none fw-semibold" style="font-size: 0.8rem;" onclick="CashierApp.enterToppingMode('${it.lineId}')">
                            <i class="bi bi-plus-circle me-1"></i>${it.toppings && it.toppings.length ? 'Sửa topping' : 'Thêm topping'}
                        </button>
                        <div class="d-flex flex-wrap gap-1 mt-1">
                            ${(it.toppings || []).map(t => `
                                <span class="badge bg-light text-dark border d-inline-flex align-items-center py-1 px-2" style="font-size: 0.75rem;">
                                    ${MilkTeaReceipts.esc(t.name)} (+${MilkTeaApi.formatVND(t.price)})
                                    <button type="button" class="btn-close ms-1 p-0" style="font-size: 0.5rem;" aria-label="Xóa topping" onclick="CashierApp.removeLineTopping('${it.lineId}', ${t.id})"></button>
                                </span>
                            `).join('')}
                        </div>
                    </div>
                `;
            } else {
                toppingControlHtml = `
                    <div class="my-1">
                        <small class="text-muted fst-italic" style="font-size: 0.75rem;">Không áp dụng topping</small>
                    </div>
                `;
            }

            html += `
                <div class="pos-cart-item ${isCurrentToppingTarget ? 'border border-primary bg-primary-subtle' : ''}">
                    <img src="${MilkTeaReceipts.esc(it.imageUrl || '/images/receipt-logo.svg')}" alt="${MilkTeaReceipts.esc(it.productName)}">
                    <div class="pos-cart-info">
                        <div class="d-flex justify-content-between gap-2">
                            <strong>${MilkTeaReceipts.esc(it.productName)}</strong>
                            <button class="btn btn-sm p-0 text-muted" aria-label="Xóa món" onclick="CashierApp.removeLine('${it.lineId}')">×</button>
                        </div>
                        ${sizeSelector}

                        <!-- Topping gắn vào món -->
                        ${toppingControlHtml}

                        <!-- Ghi chú dòng món -->
                        <div class="d-flex align-items-center gap-2 mt-1">
                            <button type="button" class="btn btn-sm btn-link p-0 text-secondary" title="Ghi chú món" onclick="CashierApp.toggleLineNote('${it.lineId}')">
                                <i class="bi bi-chat-left-text${it.note ? '-fill text-primary' : ''}"></i>
                            </button>
                            ${it.note && !it.noteOpen ? `<small class="text-muted fst-italic text-truncate" style="max-width: 140px;">${MilkTeaReceipts.esc(it.note)}</small>` : ''}
                        </div>
                        ${it.noteOpen ? `
                            <input type="text" class="form-control form-control-sm mt-1" style="font-size: 0.8rem;" placeholder="Ghi chú món này..." value="${MilkTeaReceipts.esc(it.note || '')}" oninput="CashierApp.updateLineNote('${it.lineId}', this.value)">
                        ` : ''}

                        <div class="d-flex justify-content-between align-items-center mt-2">
                            <div class="pos-qty">
                                <button aria-label="Giảm số lượng" onclick="CashierApp.adjustCounterCart('${it.lineId}', -1)">−</button>
                                <span>${it.quantity}</span>
                                <button aria-label="Tăng số lượng" onclick="CashierApp.adjustCounterCart('${it.lineId}', 1)">+</button>
                            </div>
                            <span class="pos-cart-price">${MilkTeaApi.formatVND(lineTotal)}</span>
                        </div>
                    </div>
                </div>
            `;
        });
        container.innerHTML = html;
        document.getElementById('counterCartTotal').textContent = MilkTeaApi.formatVND(total);
    }

    function adjustCounterCart(lineId, delta) {
        if (submitting || pendingCounterOrder) return;
        const it = counterCart.find(i => i.lineId === lineId);
        if (!it) return;
        it.quantity += delta;
        if (it.quantity <= 0) {
            removeLine(lineId);
        } else {
            renderCounterCart();
        }
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
                note: it.note || '',
                toppingIds: (it.toppings || []).map(t => t.id)
            }));

            const tableId = document.getElementById('counterTableId').value;
            const table = currentTables.find(t => String(t.id) === tableId);
            const payload = tableId ? { tableId, expectedSessionId: table?.activeSessionId || null, items, remainingCartItems: [] } : { items };
            const fingerprint = JSON.stringify(payload);
            if (fingerprint !== creationFingerprint) { creationFingerprint = fingerprint; creationKey = crypto.randomUUID(); }
            const created = pendingCounterOrder || await MilkTeaApi.post(tableId ? '/api/v1/orders/table' : '/api/v1/orders/counter', payload, { 'Idempotency-Key': creationKey });
            pendingCounterOrder = created;
            await MilkTeaApi.post(`/api/v1/cashier/orders/${created.orderId}/payments`, { method: document.getElementById('counterPayMethod').value, reference: 'POS_' + created.orderId }, { 'Idempotency-Key': 'POS_PAYMENT_' + created.orderId });
            pendingCounterOrder = null;
            creationKey = null; creationFingerprint = null;
            await MilkTeaReceipts.open(created.orderId);
            MilkTeaApi.showToast(`Tạo đơn #${created.orderId} thành công!`, 'success');
            counterCart = [];
            exitToppingMode();
            renderCounterCart();
            submitBtn.innerHTML = '<i class="bi bi-check-circle-fill me-2"></i>Thanh toán';
            await loadTables();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo đơn / ghi thu', 'error');
            if (pendingCounterOrder) submitBtn.textContent = 'Thử lại thanh toán đơn #' + pendingCounterOrder.orderId;
        } finally { submitting = false; submitBtn.disabled = false; }
    }


    function setupProducts() {
        products = [...document.querySelectorAll('#posProductsData [data-id]')].map(el => {
            const catName = el.dataset.categoryName || '';
            const isTopping = catName.toLowerCase().includes('topping');
            return {
                id: Number(el.dataset.id),
                name: el.dataset.name,
                size: el.dataset.size,
                price: Number(el.dataset.price),
                imageUrl: el.dataset.image,
                category: el.dataset.category,
                categoryName: catName,
                allowTopping: el.dataset.categoryAllowTopping !== 'false',
                isTopping: isTopping
            };
        });
        if (!filtersReady) {
            ['quickProductSearch', 'posProductSearch'].forEach(id => document.getElementById(id).addEventListener('input', event => {
                ['quickProductSearch', 'posProductSearch'].forEach(other => document.getElementById(other).value = event.target.value);
                renderProducts();
            }));
            document.getElementById('posCategories').addEventListener('click', event => {
                const b = event.target.closest('[data-category]'); if (!b) return;
                selectedCategory = b.dataset.category;
                document.querySelectorAll('#posCategories button').forEach(el => el.classList.toggle('active', el === b)); renderProducts();
            });
            document.getElementById('posProductGrid').addEventListener('click', event => {
                const b = event.target.closest('[data-product-id]'); if (b) addCounterCart(products.find(p => String(p.id) === b.dataset.productId));
            }); filtersReady = true;
        } renderProducts();
    }
    function renderProducts() {
        const query = document.getElementById('posProductSearch').value.trim().toLocaleLowerCase('vi');
        const groups = new Map();
        products.filter(p => (!selectedCategory || p.category === selectedCategory) && p.name.toLocaleLowerCase('vi').includes(query)).forEach(p => {
            const key = p.name + '|' + p.category; if (!groups.has(key)) groups.set(key, []); groups.get(key).push(p);
        });
        document.getElementById('posProductGrid').innerHTML = [...groups.values()].map(variants => {
            variants.sort((a, b) => a.price - b.price); const p = variants[0], esc = MilkTeaReceipts.esc;
            return `<article class="pos-product"><img src="${esc(p.imageUrl || '/images/receipt-logo.svg')}" alt="${esc(p.name)}" loading="lazy"><div class="pos-product-body"><h6>${esc(p.name)}</h6><div class="pos-product-price">${variants.length > 1 ? 'Từ ' : ''}${MilkTeaApi.formatVND(p.price)}</div><div class="pos-sizes">${variants.map(v => `<button data-product-id="${v.id}" title="Size ${esc(v.size)} · ${MilkTeaApi.formatVND(v.price)}">${esc(v.size)}</button>`).join('')}</div></div></article>`;
        }).join('') || '<p class="text-muted p-3">Không có món phù hợp.</p>';
    }
    function clearCart() { if (submitting || pendingCounterOrder) return; counterCart = []; exitToppingMode(); renderCounterCart(); }
    function removeItem(idx) { if (submitting || pendingCounterOrder) return; counterCart.splice(idx, 1); renderCounterCart(); }
    async function showSessionBlockers(sessionId, tableName) {
        try {
            const orders = await MilkTeaApi.get('/api/v1/cashier/orders');
            const sessionOrders = (orders || []).filter(o => String(o.sessionId) === String(sessionId));
            const unpaid = sessionOrders.filter(o => o.status !== 'DA_HUY' && !o.paid && Number(o.totalAmount) > 0);
            const unfinished = sessionOrders.filter(o => !['DA_HUY', 'HOAN_THANH'].includes(o.status));
            if (!unpaid.length && !unfinished.length) return false;
            const esc = MilkTeaReceipts.esc;
            const section = (title, list) => list.length ? `<h6 class="mt-3">${title}</h6><div class="table-responsive"><table class="table"><thead><tr><th>Đơn hàng</th><th>Tổng tiền</th><th>Kiểm tra</th></tr></thead><tbody>${list.map(o => `<tr><td>${MilkTeaReceipts.code(o.orderId)}</td><td>${MilkTeaApi.formatVND(o.totalAmount)}</td><td><a class="btn btn-sm btn-outline-primary" href="/orders?order=${o.orderId}">Xem đơn</a></td></tr>`).join('')}</tbody></table></div>` : '';
            document.getElementById('tableBlockersTitle').textContent = 'Chưa thể đóng phiên ' + tableName;
            document.getElementById('tableBlockersBody').innerHTML = '<p class="text-muted">Kiểm tra các đơn dưới đây trước khi đóng phiên.</p>' + section('Đơn chưa thanh toán', unpaid) + section('Đơn chưa hoàn thành', unfinished);
            bootstrap.Modal.getOrCreateInstance(document.getElementById('tableBlockersModal')).show(); return true;
        } catch (e) { return false; }
    }
    return {
        init,
        refreshTables: loadTables,
        openSession,
        closeSession,
        addCounterCart,
        adjustCounterCart,
        submitCounterOrder,
        clearCart,
        removeItem,
        removeLine,
        enterToppingMode,
        exitToppingMode,
        changeLineSize,
        toggleLineNote,
        updateLineNote,
        removeLineTopping
    };
})();
