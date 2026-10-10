/**
 * MilkTea Cashier POS & Table Management Logic
 * Tuân thủ frontend_rule.md và acceptance_tests.md
 */

const CashierApp = (() => {
    let currentTables = [];
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
            MilkTeaApi.showToast('Khách báo đã chuyển khoản đơn!', 'warning');
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
                <div class="col-6 col-md-4 col-lg-3">
                    <div class="table-card ${t.status}">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="fw-bold mb-0">${MilkTeaReceipts.esc(t.name)}</h6>
                            <span class="badge ${isBusy ? 'bg-danger' : 'bg-success'} rounded-pill">
                                ${isBusy ? 'Có Khách' : 'Trống'}
                            </span>
                        </div>
                        <div class="small text-muted mb-3">
                            ${isBusy ? `Phiên #${t.activeSessionId}` : 'Sẵn sàng phục vụ'}
                        </div>
                        <div class="d-grid gap-1">
                            ${!isBusy ? `
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
                </div>
            `;
        });
        grid.innerHTML = html;
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
            await MilkTeaApi.post(`/api/v1/cashier/table-sessions/${sessionId}/close`);
            MilkTeaApi.showToast(`Đã đóng phiên của ${tableName} thành công!`, 'success');
            await loadTables();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Không thể đóng phiên bàn', 'error');
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
                note: ''
            });
        }
        renderCounterCart();
    }

    function renderCounterCart() {
        const container = document.getElementById('counterCartList');
        if (!container) return;

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
            html += `
                <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                    <div>
                        <div class="fw-semibold small">${MilkTeaReceipts.esc(it.productName)} (${MilkTeaReceipts.esc(it.size)})</div>
                        <div class="text-muted small">${MilkTeaApi.formatVND(it.price)} x ${it.quantity}</div>
                    </div>
                    <div class="d-flex align-items-center gap-1">
                        <button class="btn btn-outline-secondary btn-sm px-2 py-0" onclick="CashierApp.adjustCounterCart(${idx}, -1)">-</button>
                        <span class="small fw-bold px-1">${it.quantity}</span>
                        <button class="btn btn-outline-secondary btn-sm px-2 py-0" onclick="CashierApp.adjustCounterCart(${idx}, 1)">+</button>
                    </div>
                </div>
            `;
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
                note: it.note
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
            renderCounterCart();
            submitBtn.innerHTML = 'Tạo đơn & thanh toán';
            await loadTables();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo đơn / ghi thu', 'error');
            if (pendingCounterOrder) submitBtn.textContent = 'Thử lại thanh toán đơn #' + pendingCounterOrder.orderId;
        } finally { submitting = false; submitBtn.disabled = false; }
    }

    return {init,openSession,closeSession,addCounterCart,adjustCounterCart,submitCounterOrder};
})();
