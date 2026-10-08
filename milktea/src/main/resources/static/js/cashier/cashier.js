/**
 * MilkTea Cashier POS & Table Management Logic
 * Tuân thủ frontend_rule.md và acceptance_tests.md
 */

const CashierApp = (() => {
    let currentOrders = [];
    let currentTables = [];
    let counterCart = [];

    async function init() {
        window.MilkTeaContext = { role: 'CASHIER' };
        MilkTeaRealtime.connect();

        setupRealtime();
        await loadTables();
        await loadOrders();
    }

    function setupRealtime() {
        MilkTeaRealtime.on('ORDER_CREATED', () => {
            MilkTeaApi.showToast('Có đơn hàng mới!', 'success');
            loadOrders();
            loadTables();
        });

        MilkTeaRealtime.on('ORDER_STATUS_CHANGED', () => {
            loadOrders();
        });

        MilkTeaRealtime.on('TABLE_SESSION_OPENED', () => {
            loadTables();
        });

        MilkTeaRealtime.on('TABLE_SESSION_CLOSED', () => {
            loadTables();
            loadOrders();
        });

        MilkTeaRealtime.on('PAYMENT_RECORDED', () => {
            loadOrders();
        });

        MilkTeaRealtime.on('PAYMENT_NOTICE_CREATED', (e) => {
            MilkTeaApi.showToast('Khách báo đã chuyển khoản đơn!', 'warning');
            loadOrders();
        });

        MilkTeaRealtime.onReconnect(() => {
            loadTables();
            loadOrders();
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
                            <h6 class="fw-bold mb-0">${t.name}</h6>
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
                                <button type="button" class="btn btn-sm btn-outline-danger rounded-pill" onclick="CashierApp.closeSession(${t.activeSessionId}, '${t.name}')">
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

    async function closeSession(sessionId, tableName) {
        if (!confirm(`Bạn có chắc chắn muốn đóng phiên của ${tableName}?\nLưu ý: Mọi đơn hàng phải hoàn tất hoặc hủy và đã thanh toán đủ.`)) {
            return;
        }

        try {
            await MilkTeaApi.post(`/api/v1/cashier/table-sessions/${sessionId}/close`);
            MilkTeaApi.showToast(`Đã đóng phiên của ${tableName} thành công!`, 'success');
            await loadTables();
            await loadOrders();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Không thể đóng phiên bàn', 'error');
        }
    }

    async function loadOrders() {
        try {
            const orders = await MilkTeaApi.get('/api/v1/cashier/orders');
            currentOrders = orders || [];
            renderOrdersList();
        } catch (e) {
            console.warn('Lỗi nạp danh sách đơn thu ngân:', e);
        }
    }

    function renderOrdersList() {
        const list = document.getElementById('ordersListContainer');
        if (!list) return;

        if (currentOrders.length === 0) {
            list.innerHTML = `
                <div class="empty-state py-5 text-center text-muted">
                    <i class="bi bi-receipt fs-1 d-block mb-2"></i>
                    <p>Hiện không có đơn hàng nào cần xử lý</p>
                </div>
            `;
            return;
        }

        let html = '';
        currentOrders.forEach(o => {
            const isWaitingConfirm = o.status === 'CHO_XAC_NHAN';
            const isPaid = o.paid === true;
            const tableLabel = o.tableName ? o.tableName : 'Tại Quầy / Mang Về';

            let itemsText = (o.items || []).map(i => `${i.productName} (x${i.quantity})`).join(', ');

            html += `
                <div class="card border-0 shadow-sm rounded-4 mb-3">
                    <div class="card-body p-3">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <div>
                                <span class="fw-bold fs-6">Đơn #${o.orderId}</span>
                                <span class="badge bg-light text-dark border ms-2">${tableLabel}</span>
                                <span class="badge ${o.source === 'TABLE' ? 'bg-info text-dark' : 'bg-secondary'} ms-1">${o.source}</span>
                            </div>
                            <div>
                                <span class="badge-status badge-${o.status}">${o.status}</span>
                            </div>
                        </div>

                        <div class="small text-muted mb-2">${itemsText}</div>

                        <div class="d-flex justify-content-between align-items-center pt-2 border-top">
                            <div>
                                <span class="small text-muted">Tổng tiền:</span>
                                <strong class="fs-5 product-price ms-1">${MilkTeaApi.formatVND(o.totalAmount)}</strong>
                                ${isPaid ? '<span class="badge bg-success ms-2">Đã Thu</span>' : '<span class="badge bg-warning text-dark ms-2">Chưa Thu</span>'}
                            </div>

                            <div class="d-flex gap-2">
                                ${isWaitingConfirm ? `
                                    <button class="btn btn-sm btn-primary-custom rounded-pill" onclick="CashierApp.confirmOrder(${o.orderId})">
                                        <i class="bi bi-check-lg me-1"></i> Xác Nhận Đơn
                                    </button>
                                ` : ''}

                                ${!isPaid && o.status !== 'DA_HUY' ? `
                                    <button class="btn btn-sm btn-success rounded-pill" onclick="CashierApp.openPaymentModal(${o.orderId}, ${o.totalAmount})">
                                        <i class="bi bi-cash-coin me-1"></i> Ghi Thu Tiền
                                    </button>
                                ` : ''}

                                ${o.source === 'COUNTER' && !o.sessionId ? `
                                    <button class="btn btn-sm btn-outline-secondary rounded-pill" onclick="CashierApp.openAssignTableModal(${o.orderId})">
                                        <i class="bi bi-link-45deg me-1"></i> Gán Bàn
                                    </button>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
        list.innerHTML = html;
    }

    async function confirmOrder(orderId) {
        try {
            await MilkTeaApi.post(`/api/v1/cashier/orders/${orderId}/confirm`);
            MilkTeaApi.showToast(`Đã xác nhận đơn #${orderId}, chuyển tới Bếp!`, 'success');
            await loadOrders();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Không thể xác nhận đơn', 'error');
        }
    }

    function openPaymentModal(orderId, amount) {
        document.getElementById('receiptOrderId').value = orderId;
        document.getElementById('receiptAmount').textContent = MilkTeaApi.formatVND(amount);
        const modal = new bootstrap.Modal(document.getElementById('receiptModal'));
        modal.show();
    }

    async function submitReceipt(method) {
        const orderId = document.getElementById('receiptOrderId').value;
        try {
            await MilkTeaApi.post(`/api/v1/cashier/orders/${orderId}/payments`, {
                method: method,
                reference: `${method}_${orderId}_${Date.now()}`
            });
            MilkTeaApi.showToast(`Đã ghi nhận thanh toán đơn #${orderId}!`, 'success');
            const modalEl = document.getElementById('receiptModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            await loadOrders();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi ghi nhận thanh toán', 'error');
        }
    }

    function openAssignTableModal(orderId) {
        document.getElementById('assignOrderId').value = orderId;
        const select = document.getElementById('assignTableSelect');
        select.innerHTML = '';
        currentTables.forEach(t => {
            select.innerHTML += `<option value="${t.id}">${t.name} (${t.status === 'CO_KHACH' ? 'Có khách' : 'Trống'})</option>`;
        });
        const modal = new bootstrap.Modal(document.getElementById('assignTableModal'));
        modal.show();
    }

    async function submitAssignTable() {
        const orderId = document.getElementById('assignOrderId').value;
        const tableId = document.getElementById('assignTableSelect').value;
        try {
            await MilkTeaApi.post(`/api/v1/cashier/orders/${orderId}/assign-table`, { tableId });
            MilkTeaApi.showToast(`Đã gán đơn #${orderId} vào bàn thành công!`, 'success');
            const modalEl = document.getElementById('assignTableModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            await loadOrders();
            await loadTables();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi gán bàn', 'error');
        }
    }

    // POS Quầy: Thêm món vào giỏ quầy
    function addCounterCart(p) {
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
                        <div class="fw-semibold small">${it.productName} (${it.size})</div>
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
        if (!counterCart[idx]) return;
        counterCart[idx].quantity += delta;
        if (counterCart[idx].quantity <= 0) {
            counterCart.splice(idx, 1);
        }
        renderCounterCart();
    }

    async function submitCounterOrder() {
        if (counterCart.length === 0) {
            MilkTeaApi.showToast('Vui lòng chọn món trước khi tạo đơn', 'warning');
            return;
        }

        try {
            const items = counterCart.map(it => ({
                productId: it.productId,
                quantity: it.quantity,
                note: it.note
            }));

            const created = await MilkTeaApi.post('/api/v1/orders/counter', { items });
            MilkTeaApi.showToast(`Tạo đơn quầy #${created.orderId} thành công!`, 'success');
            counterCart = [];
            renderCounterCart();
            await loadOrders();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo đơn quầy', 'error');
        }
    }

    return {
        init,
        openSession,
        closeSession,
        confirmOrder,
        openPaymentModal,
        submitReceipt,
        openAssignTableModal,
        submitAssignTable,
        addCounterCart,
        adjustCounterCart,
        submitCounterOrder
    };
})();
