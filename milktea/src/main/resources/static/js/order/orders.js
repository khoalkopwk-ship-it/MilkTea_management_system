const OrdersApp = (() => {
    const e = MilkTeaReceipts.esc;
    const labels = {
        CHO_THANH_TOAN: 'Chờ thanh toán',
        CHO_XAC_NHAN: 'Chờ xác nhận',
        CHO_CHE_BIEN: 'Chờ chế biến',
        DANG_CHE_BIEN: 'Đang chế biến',
        HOAN_THANH: 'Hoàn thành',
        DA_HUY: 'Đã hủy'
    };
    let orders = [], cancellations = [], staff = false, busy = false, cancelId = null;
    let currentTab = 'all';
    let currentPage = 1;
    let requestSeq = 0;
    const pageSize = 10;
    const detailActions = new Map();

    function button(action, id, label, css = 'outline-secondary') {
        return `<button class="btn btn-sm btn-${css} rounded-pill" data-action="${action}" data-id="${id}">${label}</button>`;
    }

    function matchesTab(o, tab) {
        if (!tab || tab === 'all') return true;
        if (tab === 'confirm') return o.status === 'CHO_XAC_NHAN' && !o.staffCreated;
        if (tab === 'cooking') return o.status === 'CHO_CHE_BIEN' || o.status === 'DANG_CHE_BIEN';
        if (tab === 'transfer') return !!o.transferNotified && !o.paid && o.status !== 'DA_HUY';
        if (tab === 'completed') return o.status === 'HOAN_THANH';
        if (tab === 'cancel') return !!o.cancellationPending && o.status === 'CHO_CHE_BIEN' && o.paid;
        if (tab === 'refund') return o.status === 'DA_HUY' && o.paid;
        return true;
    }

    function paymentState(o) {
        if (o.status === 'DA_HUY' && o.refund) {
            if (o.refund.status === 'DA_HOAN') return { label: 'Đã hoàn tiền', css: 'payment-done' };
            if (o.refund.status === 'DA_DUYET') return { label: 'Chờ chi hoàn', css: 'payment-wait' };
            if (o.refund.status === 'CHO_DUYET') return { label: 'Chờ duyệt hoàn', css: 'payment-wait' };
            if (o.refund.status === 'TU_CHOI') return { label: 'Hoàn tiền bị từ chối', css: 'payment-danger' };
        }
        if (o.paid) return { label: 'Đã thu tiền', css: 'payment-done' };
        if (o.status === 'DA_HUY') return { label: 'Không cần thu tiền', css: 'payment-muted' };
        if (o.transferNotified) return { label: 'Khách báo đã CK', css: 'payment-wait' };
        if (o.paymentRequestMethod === 'CASH') return { label: 'Chờ thu · Tiền mặt', css: 'payment-wait' };
        if (o.paymentRequestMethod === 'BANK_TRANSFER') return { label: 'Chờ thu · Chuyển khoản', css: 'payment-wait' };
        return { label: 'Chưa thu tiền', css: 'payment-wait' };
    }

    function render() {
        const searchInput = document.getElementById('orderSearch');
        const statusSelect = document.getElementById('orderStatus');
        const paymentSelect = document.getElementById('orderPaymentStatus');
        const refundSelect = document.getElementById('orderRefundStatus');
        const query = searchInput ? searchInput.value.trim().toUpperCase().replace(/^#/, '') : '';
        const status = (currentTab === 'all' && statusSelect) ? statusSelect.value : '';
        const paymentStatus = (currentTab === 'all' && paymentSelect) ? paymentSelect.value : '';
        const refundStatus = (currentTab === 'refund' && refundSelect) ? refundSelect.value : '';

        // Cập nhật badge trên từng tab dựa trên toàn bộ đơn chưa bị lọc
        if (staff) {
            ['all', 'confirm', 'cooking', 'transfer', 'completed', 'cancel', 'refund'].forEach(tabKey => {
                const badge = document.getElementById('tabBadge_' + tabKey);
                if (badge) {
                    let count = 0;
                    if (tabKey === 'refund') {
                        // Đếm đúng yêu cầu chờ xử lý, không tính lịch sử đã hoàn tất
                        count = orders.filter(o => o.status === 'DA_HUY' && o.paid && (!o.refund || o.refund.status !== 'DA_HOAN')).length;
                    } else {
                        count = orders.filter(o => matchesTab(o, tabKey)).length;
                    }
                    badge.textContent = count;
                    badge.classList.toggle('d-none', count === 0);
                }
            });
        }

        const baseFiltered = orders.filter(o => {
            const matchQuery = !query
                || MilkTeaReceipts.code(o.orderId).includes(query)
                || String(o.orderId) === query
                || (o.tableName && o.tableName.toUpperCase().includes(query))
                || (o.customerName && o.customerName.toUpperCase().includes(query));

            let matchStatus = true;
            if (currentTab === 'all' && status) {
                matchStatus = o.status === status;
            }

            let matchPayment = true;
            if (currentTab === 'all' && paymentStatus) {
                if (paymentStatus === 'PAID') {
                    matchPayment = o.paid;
                } else if (paymentStatus === 'UNPAID') {
                    matchPayment = !o.paid && o.status !== 'DA_HUY';
                } else if (paymentStatus === 'TRANSFER_PENDING') {
                    matchPayment = o.transferNotified && !o.paid;
                }
            }

            let matchRefund = true;
            if (currentTab === 'refund' && refundStatus) {
                const isBank = o.paymentMethod === 'BANK_TRANSFER';
                if (refundStatus === 'CAN_GUI_YEU_CAU') {
                    if (isBank) {
                        matchRefund = !o.refund || o.refund.status === 'TU_CHOI';
                    } else {
                        matchRefund = !o.refund || o.refund.status !== 'DA_HOAN';
                    }
                } else if (refundStatus === 'CHO_XAC_NHAN') {
                    matchRefund = !!(o.refund && o.refund.status === 'CHO_DUYET');
                } else if (refundStatus === 'HOAN_TAT') {
                    matchRefund = !!(o.refund && o.refund.status === 'DA_HOAN');
                } else if (refundStatus === 'TAT_CA') {
                    matchRefund = true;
                }
            }

            return matchQuery && matchStatus && matchPayment && matchRefund;
        });

        const tabFiltered = staff ? baseFiltered.filter(o => matchesTab(o, currentTab)) : baseFiltered;
        const totalItems = tabFiltered.length;
        const totalPages = Math.ceil(totalItems / pageSize) || 1;
        if (currentPage > totalPages) currentPage = totalPages;
        const pagedOrders = tabFiltered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

        detailActions.clear();
        const rows = pagedOrders.map(o => {
            const payment = paymentState(o);

            if (currentTab === 'refund') {
                let refundCell = '';
                const isBank = o.paymentMethod === 'BANK_TRANSFER';
                if (o.refund && o.refund.status === 'DA_HOAN') {
                    refundCell = '<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Hoàn tất</span>';
                } else if (o.refund && o.refund.status === 'CHO_DUYET') {
                    refundCell = '<span class="badge bg-warning text-dark"><i class="bi bi-clock me-1"></i>Chờ xác nhận</span>';
                } else if (o.refund && o.refund.status === 'TU_CHOI') {
                    refundCell = `<div class="d-flex flex-column gap-1"><span class="badge bg-danger">Bị từ chối</span>${button('requestBankRefund', o.orderId, 'Gửi lại yêu cầu', 'outline-warning')}</div>`;
                } else {
                    if (isBank) {
                        refundCell = button('requestBankRefund', o.orderId, 'Gửi yêu cầu', 'warning');
                    } else {
                        refundCell = button('refundCash', o.orderId, 'Hoàn tiền mặt', 'warning');
                    }
                }

                detailActions.set(String(o.orderId), button('receipt', o.orderId, 'Xem hóa đơn'));
                const actionCell = `<button title="Xem chi tiết đơn" aria-label="Xem chi tiết ${MilkTeaReceipts.code(o.orderId)}" class="order-detail-btn" data-action="details" data-id="${o.orderId}"><i class="bi bi-sliders"></i><span class="ms-2 d-none d-lg-inline">Chi tiết</span></button>`;

                return `<tr>
                    <td>
                        <b>${MilkTeaReceipts.code(o.orderId)}</b>
                        <small class="d-block text-muted mt-1">${e(o.tableName || (o.staffCreated ? 'Quầy thu ngân' : 'Quầy / mang về'))}</small>
                    </td>
                    <td class="text-end fw-semibold text-nowrap">${MilkTeaApi.formatVND(o.totalAmount)}</td>
                    <td>${refundCell}</td>
                    <td>
                        <span class="payment-pill ${payment.css}">${payment.label}</span>
                    </td>
                    <td class="text-center">${actionCell}</td>
                </tr>`;
            }

            const cancel = cancellations.find(c => c.orderId === o.orderId && c.status === 'CHO');
            let actions = button('receipt', o.orderId, 'Xem hóa đơn');
            let quickAction = '';

            if (staff) {
                if (cancel && currentTab === 'cancel') {
                    actions += button('approveCancel', cancel.id, 'Chấp thuận hủy', 'danger')
                        + button('rejectCancel', cancel.id, 'Từ chối hủy', 'outline-secondary');
                    quickAction = button('approveCancel', cancel.id, 'Duyệt hủy', 'danger');
                } else if (o.status === 'CHO_XAC_NHAN' && !o.staffCreated) {
                    actions += button('confirm', o.orderId, 'Xác nhận đơn', 'primary');
                    if (currentTab === 'confirm') {
                        quickAction = button('confirm', o.orderId, 'Xác nhận đơn', 'primary');
                    }
                }

                if (currentTab === 'transfer' && o.transferNotified && !o.paid && o.status !== 'DA_HUY') {
                    quickAction = button('confirmTransfer', o.orderId, 'Đã thanh toán', 'success');
                } else if (o.transferNotified && !o.paid && o.invoiceStatus === 'HIEU_LUC') {
                    actions += button('confirmTransfer', o.orderId, 'Đã thanh toán (CK)', 'success');
                } else if (!o.paid && o.status !== 'DA_HUY') {
                    actions += button('staffPayment', o.orderId, 'Đã thanh toán', 'success');
                }
            } else {
                if (o.status !== 'DA_HUY' && !o.paid) actions += button('pay', o.orderId, 'Thanh toán', 'primary');
                if (!o.cancellationPending && ['CHO_THANH_TOAN', 'CHO_XAC_NHAN', 'CHO_CHE_BIEN'].includes(o.status)) {
                    actions += button('cancel', o.orderId, 'Yêu cầu hủy', 'outline-danger');
                }
            }

            detailActions.set(String(o.orderId), actions);

            const actionCell = quickAction
                ? `<div class="d-flex justify-content-center align-items-center gap-1">${quickAction}<button title="Chi tiết ${MilkTeaReceipts.code(o.orderId)}" class="order-detail-btn" data-action="details" data-id="${o.orderId}"><i class="bi bi-sliders"></i></button></div>`
                : `<button title="Xem chi tiết đơn" aria-label="Xem chi tiết ${MilkTeaReceipts.code(o.orderId)}" class="order-detail-btn" data-action="details" data-id="${o.orderId}"><i class="bi bi-sliders"></i><span class="ms-2 d-none d-lg-inline">Chi tiết</span></button>`;

            let statusBadge = '';
            if (currentTab === 'cooking') {
                if (o.status === 'CHO_CHE_BIEN') {
                    statusBadge = '<span class="badge bg-info text-dark"><i class="bi bi-hourglass-split me-1"></i>Chờ Bếp bắt đầu</span>';
                } else if (o.status === 'DANG_CHE_BIEN') {
                    statusBadge = '<span class="badge bg-primary"><i class="bi bi-fire me-1"></i>Bếp đang chế biến</span>';
                } else {
                    statusBadge = `<span class="badge-status badge-${o.status}">${labels[o.status] || e(o.status)}</span>`;
                }
            } else {
                statusBadge = `<span class="badge-status badge-${o.status}">${o.cancellationPending ? 'Chờ xác nhận hủy' : (labels[o.status] || e(o.status))}</span>`;
            }

            return `<tr>
                <td>
                    <b>${MilkTeaReceipts.code(o.orderId)}</b>
                    <small class="d-block text-muted mt-1">${e(o.tableName || (o.staffCreated ? 'Quầy thu ngân' : 'Quầy / mang về'))}</small>
                </td>
                <td class="text-end fw-semibold text-nowrap">${MilkTeaApi.formatVND(o.totalAmount)}</td>
                <td>${statusBadge}</td>
                <td>
                    <span class="payment-pill ${payment.css}">${payment.label}</span>
                </td>
                <td class="text-center">${actionCell}</td>
            </tr>`;
        }).join('');

        const thirdColHeader = currentTab === 'refund' ? 'Trạng thái hoàn' : 'Trạng thái đơn';
        document.getElementById('ordersList').innerHTML = `<div class="orders-panel table-responsive">
            <table class="table table-hover orders-table align-middle mb-0">
                <thead>
                    <tr>
                        <th>Mã đơn</th>
                        <th class="text-end">Tổng tiền</th>
                        <th>${thirdColHeader}</th>
                        <th>Tình trạng thanh toán</th>
                        <th class="text-center">Chi tiết</th>
                    </tr>
                </thead>
                <tbody>${rows || '<tr><td colspan="5" class="text-center py-5 text-muted">Không có đơn hàng phù hợp trong mục này.</td></tr>'}</tbody>
            </table>
        </div>`;

        renderPagination(totalItems, totalPages);
        window.PendingOrders?.refresh();
    }

    function renderPagination(totalItems, totalPages) {
        const pagContainer = document.getElementById('ordersPagination');
        if (!pagContainer) return;
        if (totalPages <= 1) {
            pagContainer.innerHTML = '';
            return;
        }

        const startIdx = totalItems === 0 ? 0 : (currentPage - 1) * pageSize + 1;
        const endIdx = Math.min(currentPage * pageSize, totalItems);
        let pagesHtml = '';
        for (let p = 1; p <= totalPages; p++) {
            pagesHtml += `<li class="page-item ${p === currentPage ? 'active' : ''}"><button class="page-link" data-page="${p}">${p}</button></li>`;
        }

        pagContainer.innerHTML = `
            <div class="orders-pagination-container">
                <small class="text-muted">Hiển thị <b>${startIdx}-${endIdx}</b> trong <b>${totalItems}</b> đơn</small>
                <ul class="pagination pagination-sm mb-0">
                    <li class="page-item ${currentPage === 1 ? 'disabled' : ''}"><button class="page-link" data-page="${currentPage - 1}">«</button></li>
                    ${pagesHtml}
                    <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}"><button class="page-link" data-page="${currentPage + 1}">»</button></li>
                </ul>
            </div>`;
    }

    function switchTab(newTab) {
        currentTab = newTab;
        currentPage = 1;

        const colStatus = document.getElementById('colOrderStatus');
        const colPayment = document.getElementById('colPaymentStatus');
        const colRefund = document.getElementById('colRefundStatus');
        const colSearch = document.getElementById('colOrderSearch');
        const statusSelect = document.getElementById('orderStatus');
        const paymentSelect = document.getElementById('orderPaymentStatus');
        const refundSelect = document.getElementById('orderRefundStatus');

        if (newTab === 'all') {
            if (colStatus) colStatus.classList.remove('d-none');
            if (colPayment) colPayment.classList.remove('d-none');
            if (colRefund) colRefund.classList.add('d-none');
            if (colSearch) {
                colSearch.className = 'col-md-5';
            }
        } else if (newTab === 'refund') {
            if (colStatus) colStatus.classList.add('d-none');
            if (colPayment) colPayment.classList.add('d-none');
            if (colRefund) colRefund.classList.remove('d-none');
            if (colSearch) {
                colSearch.className = 'col-md-8';
            }
            if (refundSelect) refundSelect.value = 'CAN_GUI_YEU_CAU';
        } else {
            if (colStatus) colStatus.classList.add('d-none');
            if (colPayment) colPayment.classList.add('d-none');
            if (colRefund) colRefund.classList.add('d-none');
            if (colSearch) {
                colSearch.className = 'col-md-11';
            }
            if (statusSelect) statusSelect.value = '';
            if (paymentSelect) paymentSelect.value = '';
        }

        document.querySelectorAll('#orderStaffTabs .nav-link').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === newTab);
        });
        render();
    }

    async function load() {
        const seq = ++requestSeq;
        try {
            let fetchedOrders = [];
            let fetchedCancels = [];
            if (staff) {
                fetchedOrders = await MilkTeaApi.get('/api/v1/cashier/orders') || [];
                fetchedCancels = await MilkTeaApi.get('/api/v1/cashier/cancellation-requests') || [];
            } else {
                fetchedOrders = await MilkTeaApi.get('/api/v1/customer/orders') || [];
                fetchedCancels = [];
            }
            if (seq !== requestSeq) return; // Tránh race condition đè response cũ
            orders = fetchedOrders;
            cancellations = fetchedCancels;
            document.getElementById('ordersError').classList.add('d-none');
            render();
        } catch(err) {
            if (seq !== requestSeq) return;
            const box = document.getElementById('ordersError');
            box.textContent = err.message || 'Không thể tải danh sách đơn hàng. Nhấn Làm mới để thử lại.';
            box.classList.remove('d-none');
            document.getElementById('ordersList').textContent = '';
        }
    }

    function modal(title, body, footer) {
        document.getElementById('orderActionTitle').textContent = title;
        document.getElementById('orderActionBody').innerHTML = body;
        document.getElementById('orderActionFooter').innerHTML = footer;
        bootstrap.Modal.getOrCreateInstance(document.getElementById('orderActionModal')).show();
    }

    async function action(name, id) {
        const o = orders.find(o => String(o.orderId) === String(id));
        if (name === 'staffPayment') {
            modal('Xác nhận thanh toán ' + MilkTeaReceipts.code(id),
                `<p class="text-muted">Chỉ xác nhận sau khi đã nhận đủ tiền hoặc kiểm tra giao dịch chuyển khoản.</p>
                 <div class="payment-confirm-total">${MilkTeaApi.formatVND(o.totalAmount)}</div>
                 <p class="mt-3">${o.paymentRequestMethod ? 'Khách chọn ' + (o.paymentRequestMethod === 'CASH' ? 'tiền mặt' : 'chuyển khoản') + '.' : 'Chọn phương thức đã nhận tiền.'}</p>`,
                button('cash', id, 'Đã nhận tiền mặt', 'success') + button('bank', id, 'Đã nhận chuyển khoản', 'primary'));
            return;
        }

        if (name === 'confirmTransfer') {
            if (!confirm('Xác nhận đã nhận đủ số tiền chuyển khoản ' + MilkTeaApi.formatVND(o.totalAmount) + ' cho đơn ' + MilkTeaReceipts.code(id) + '?')) return;
            try {
                await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/confirm-transfer`);
                MilkTeaApi.showToast('Đã xác nhận thanh toán chuyển khoản thành công.', 'success');
                o.paid = true;
                o.transferNotified = false;
                const actionModal = document.getElementById('orderActionModal');
                if (actionModal) bootstrap.Modal.getInstance(actionModal)?.hide();
                render();
                window.PendingOrders?.refresh();
                await load();
            } catch(err) {
                MilkTeaApi.showToast(err.message || 'Lỗi khi xác nhận thanh toán', 'error');
            }
            return;
        }

        if (name === 'requestBankRefund') {
            if (!confirm('Gửi yêu cầu hoàn tiền chuyển khoản cho đơn ' + MilkTeaReceipts.code(id) + ' tới Quản trị viên?')) return;
            try {
                await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/refund-bank-request`);
                MilkTeaApi.showToast('Đã gửi yêu cầu hoàn tiền chuyển khoản tới Admin thành công.', 'success');
                await load();
            } catch(err) {
                MilkTeaApi.showToast(err.message || 'Lỗi khi gửi yêu cầu hoàn tiền', 'error');
            }
            return;
        }

        if (name === 'refundCash') {
            if (!confirm('Xác nhận đã thực trả ' + MilkTeaApi.formatVND(o.totalAmount) + ' tiền mặt cho đơn ' + MilkTeaReceipts.code(id) + '?')) return;
            try {
                await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/refund-cash`);
                MilkTeaApi.showToast('Đã ghi nhận hoàn tiền mặt thành công.', 'success');
                await load();
            } catch(err) {
                MilkTeaApi.showToast(err.message || 'Lỗi khi ghi nhận hoàn tiền mặt', 'error');
            }
            return;
        }

        if (name === 'executeRefund') {
            if (!confirm('Xác nhận chi trả tiền hoàn cho yêu cầu hoàn tiền #' + id + '?')) return;
            try {
                await MilkTeaApi.post(`/api/v1/cashier/refunds/${id}/execute`);
                MilkTeaApi.showToast('Đã ghi nhận chi tiền hoàn thành công.', 'success');
                const actionModal = document.getElementById('orderActionModal');
                bootstrap.Modal.getInstance(actionModal)?.hide();
                await load();
            } catch(err) {
                MilkTeaApi.showToast(err.message || 'Lỗi khi chi trả hoàn tiền', 'error');
            }
            return;
        }

        if (name === 'details') {
            const transferNoticeBanner = (o.transferNotified && !o.paid)
                ? `<div class="alert alert-warning py-2 mb-3"><i class="bi bi-info-circle me-1"></i> Khách hàng đã nhấn <b>"Tôi đã chuyển khoản"</b>. Vui lòng kiểm tra tài khoản ngân hàng trước khi xác nhận.</div>`
                : '';

            modal('Chi tiết ' + MilkTeaReceipts.code(id),
                `${transferNoticeBanner}
                 <p><b>${e(o.tableName || (o.staffCreated ? 'Quầy thu ngân' : 'Quầy / mang về'))}</b> · ${MilkTeaReceipts.date(o.createdAt)}</p>
                 ${MilkTeaReceipts.lines(o)}
                 <p class="mt-2">Tổng cộng: <b>${MilkTeaApi.formatVND(o.totalAmount)}</b> · ${paymentState(o).label}</p>
                 ${o.paymentRequestMethod && !o.transferNotified ? '<p class="text-warning"><i class="bi bi-exclamation-triangle me-1"></i> Khách chọn thanh toán ' + (o.paymentRequestMethod === 'CASH' ? 'tiền mặt' : 'chuyển khoản') + '.</p>' : ''}`,
                detailActions.get(String(id)) || '');
            return;
        }

        if (name === 'receipt') {
            const el = document.getElementById('orderActionModal');
            if (el.classList.contains('show')) {
                el.addEventListener('hidden.bs.modal', () => MilkTeaReceipts.open(id), { once: true });
                bootstrap.Modal.getInstance(el).hide();
            } else await MilkTeaReceipts.open(id);
            return;
        }

        if (name === 'cancel') {
            cancelId = id;
            modal('Yêu cầu hủy ' + MilkTeaReceipts.code(id),
                '<p>Chỉ hủy khi bếp chưa bắt đầu pha chế.</p><label for="orderCancelReason">Lý do hủy</label><textarea maxlength="500" id="orderCancelReason" class="form-control" rows="3"></textarea>',
                button('sendCancel', id, 'Gửi yêu cầu hủy', 'danger'));
            return;
        }

        if (name === 'pay') {
            modal('Thanh toán ' + MilkTeaReceipts.code(id),
                `<p>Số tiền: <b>${MilkTeaApi.formatVND(o.totalAmount)}</b></p><p>Chọn phương thức để thanh toán hoặc gọi nhân viên thu ngân:</p>`,
                button('noticeCash', id, 'Tiền mặt', 'success') + button('requestBank', id, 'Chuyển khoản ngân hàng', 'primary'));
            return;
        }

        if (name === 'requestBank') {
            try {
                const shop = document.getElementById('receiptShop') || {};
                const bank = (shop.dataset && shop.dataset.bank) || 'Vietcombank',
                      account = (shop.dataset && shop.dataset.account) || '',
                      holder = (shop.dataset && shop.dataset.holder) || '';
                const img = account
                    ? `<img alt="QR chuyển khoản" class="d-block mx-auto my-3 img-fluid border rounded p-2" style="max-width:240px" src="https://img.vietqr.io/image/${encodeURIComponent(bank)}-${encodeURIComponent(account)}-compact2.png?amount=${encodeURIComponent(o.totalAmount)}&addInfo=DH${id}&accountName=${encodeURIComponent(holder)}">`
                    : '<p class="text-muted">Vui lòng liên hệ thu ngân để nhận thông tin chuyển khoản.</p>';
                modal('Chuyển khoản ' + MilkTeaReceipts.code(id),
                    `<p>Số tiền: <b>${MilkTeaApi.formatVND(o.totalAmount)}</b></p>
                     <p class="mb-1"><b>Ngân hàng:</b> ${e(bank)}</p>
                     <p class="mb-1"><b>Số tài khoản:</b> ${e(account)}</p>
                     <p class="mb-2"><b>Chủ tài khoản:</b> ${e(holder)}</p>
                     ${img}
                     <p class="small text-muted mb-0">Sau khi chuyển khoản thành công, nhấn nút <b>"Tôi đã chuyển khoản"</b> để thông báo tới thu ngân xác nhận đã nhận tiền.</p>`,
                    button('noticeBank', id, 'Tôi đã chuyển khoản', 'primary'));
            } catch(err) {
                MilkTeaApi.showToast(err.message, 'error');
            }
            return;
        }

        if (busy) return;
        busy = true;
        document.querySelectorAll('[data-action]').forEach(b => b.disabled = true);
        try {
            if (name === 'cash' || name === 'bank') {
                if (!confirm('Xác nhận đã nhận đủ ' + MilkTeaApi.formatVND(o.totalAmount) + ' bằng ' + (name === 'cash' ? 'tiền mặt' : 'chuyển khoản') + '?')) return;
                await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/payments`, {
                    method: name === 'cash' ? 'CASH' : 'BANK_TRANSFER',
                    reference: 'RECEIPT_' + id
                });
                await load();
            } else if (name === 'confirm') {
                await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/confirm`);
            } else if (name === 'sendCancel') {
                const reason = document.getElementById('orderCancelReason').value.trim();
                if (!reason) throw new Error('Vui lòng nhập lý do hủy đơn.');
                await MilkTeaApi.post(`/api/v1/customer/orders/${cancelId}/cancellation-requests`, { reason });
            } else if (name === 'approveCancel' || name === 'rejectCancel') {
                await MilkTeaApi.post(`/api/v1/cashier/cancellation-requests/${id}/decision`, {
                    approved: name === 'approveCancel',
                    reason: 'Nhân viên xác nhận'
                });
            } else if (name === 'requestRefund') {
                await MilkTeaApi.post(`/api/v1/cashier/cancellation-requests/${id}/refund`);
            } else if (name === 'noticeCash') {
                await MilkTeaApi.post(`/api/v1/orders/${id}/payment-notices`, {
                    method: 'CASH',
                    reference: 'REQUEST_CASH_' + id
                });
            } else if (name === 'noticeBank') {
                await MilkTeaApi.post(`/api/v1/orders/${id}/payment-notices`, {
                    method: 'BANK_TRANSFER',
                    reference: 'CUSTOMER_TRANSFERRED_' + id
                });
            }

            const actionModal = document.getElementById('orderActionModal');
            const hideAction = () => bootstrap.Modal.getInstance(actionModal)?.hide();
            if (!actionModal.classList.contains('show') || getComputedStyle(actionModal).opacity === '1') {
                hideAction();
            } else {
                actionModal.addEventListener('shown.bs.modal', hideAction, { once: true });
            }
            MilkTeaApi.showToast('Đã ghi nhận thành công.', 'success');
            await load();
        } catch(err) {
            MilkTeaApi.showToast(err.message, 'error');
        } finally {
            busy = false;
            document.querySelectorAll('[data-action]').forEach(b => b.disabled = false);
        }
    }

    async function init() {
        staff = !!document.getElementById('staffOrders');
        const page = document.getElementById('ordersPage');
        await CustomerApp.initContext(page.dataset.qr || null, page.dataset.table || null);
        if (staff) {
            window.MilkTeaContext.role = document.getElementById('staffOrders').dataset.role || 'CASHIER';
        }

        document.body.insertAdjacentHTML('beforeend',
            '<div class="modal fade" id="orderActionModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered modal-lg"><div class="modal-content"><div class="modal-header"><h5 id="orderActionTitle"></h5><button class="btn-close" data-bs-dismiss="modal"></button></div><div id="orderActionBody" class="modal-body"></div><div id="orderActionFooter" class="modal-footer"></div></div></div></div>');

        const searchInput = document.getElementById('orderSearch');
        if (searchInput) {
            searchInput.addEventListener('input', () => {
                currentPage = 1;
                render();
            });
        }

        const statusSelect = document.getElementById('orderStatus');
        if (statusSelect) {
            statusSelect.addEventListener('change', () => {
                currentPage = 1;
                render();
            });
        }

        const paymentSelect = document.getElementById('orderPaymentStatus');
        if (paymentSelect) {
            paymentSelect.addEventListener('change', () => {
                currentPage = 1;
                render();
            });
        }

        const refundSelect = document.getElementById('orderRefundStatus');
        if (refundSelect) {
            refundSelect.addEventListener('change', () => {
                currentPage = 1;
                render();
            });
        }

        const tabsWrapper = document.getElementById('orderStaffTabs');
        if (tabsWrapper) {
            tabsWrapper.addEventListener('click', event => {
                const btn = event.target.closest('[data-tab]');
                if (btn) switchTab(btn.dataset.tab);
            });
        }

        const pagContainer = document.getElementById('ordersPagination');
        if (pagContainer) {
            pagContainer.addEventListener('click', event => {
                const btn = event.target.closest('[data-page]');
                if (btn && !btn.parentElement.classList.contains('disabled')) {
                    currentPage = parseInt(btn.dataset.page, 10);
                    render();
                }
            });
        }

        document.addEventListener('click', event => {
            const b = event.target.closest('[data-action]');
            if (b) action(b.dataset.action, b.dataset.id);
        });

        ['ORDER_CREATED', 'ORDER_STATUS_CHANGED', 'PAYMENT_RECORDED', 'PAYMENT_NOTICE_CREATED', 'CANCELLATION_REQUESTED', 'CANCELLATION_DECIDED', 'REFUND_STATUS_CHANGED', 'REFUND_CHANGED'].forEach(type => {
            MilkTeaRealtime.on(type, load);
        });
        MilkTeaRealtime.onReconnect(load);
        MilkTeaRealtime.connect();

        await load();

        const focusOrder = new URLSearchParams(location.search).get('order');
        if (focusOrder && orders.some(o => String(o.orderId) === focusOrder)) {
            await action('details', focusOrder);
        }
        const invoice = new URLSearchParams(location.search).get('invoice');
        if (invoice) {
            await MilkTeaReceipts.open(invoice);
            const url = new URL(location.href);
            url.searchParams.delete('invoice');
            history.replaceState(null, '', url.pathname + url.search);
        }

        setInterval(() => {
            if (!document.hidden && !busy) load();
        }, 12000);
    }

    document.addEventListener('DOMContentLoaded', init);
    return { load, action, switchTab };
})();
