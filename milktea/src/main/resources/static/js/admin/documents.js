const DocumentsApp = (() => {
    const esc = MilkTeaReceipts.esc;
    const date = MilkTeaReceipts.date;

    let cancellations = [];
    let refunds = [];
    let imports = [];
    let movements = [];
    let orders = new Map();
    let busy = false;
    let appliedDateFrom = '';
    let appliedDateTo = '';

    const statusText = {
        CHO: 'Chờ xác nhận',
        CHAP_THUAN: 'Đã xác nhận hủy',
        TU_CHOI: 'Từ chối',
        CHO_DUYET: 'Chờ Admin duyệt',
        DA_DUYET: 'Đã duyệt, chờ thực chi',
        DA_HOAN: 'Đã hoàn tiền'
    };

    const movementNames = {
        IMPORT: 'Nhập kho',
        ISSUE: 'Chuyển kho',
        ORDER: 'Tiêu hao đơn',
        BATCH: 'Sơ chế',
        WASTE: 'Hao hụt',
        REMAKE: 'Pha bù'
    };

    const btn = (action, id, label, css = 'primary') =>
        `<button class="btn btn-sm btn-${css}" data-doc-action="${action}" data-id="${id}">${label}</button>`;

    const card = body => `<article class="card shadow-sm p-3">${body}</article>`;
    
    const expandedDocuments = new Set();

    function collapsibleCard(key, title, summary, body, status = '') {
        const isOpen = expandedDocuments.has(key);

        return `
            <details class="document-fold"
                    data-document-key="${esc(key)}"
                    ${isOpen ? 'open' : ''}>
                <summary class="document-fold-header">
                    <div class="flex-grow-1">
                        <div class="document-fold-title">
                            ${title}
                        </div>
                        <div class="document-fold-summary">
                            ${summary}
                        </div>
                    </div>
                    ${status ? `<span class="badge bg-secondary">${esc(status)}</span>` : ''}
                    <i class="bi bi-chevron-down document-fold-arrow"></i>
                </summary>
                <div class="document-fold-body">
                    ${body}
                </div>
            </details>
        `;
    }


    function selectedRange() {
        const fromValue = appliedDateFrom;
        const toValue = appliedDateTo;
        const from = fromValue ? new Date(`${fromValue}T00:00:00`) : null;
        const to = toValue ? new Date(`${toValue}T23:59:59.999`) : null;
        return { from, to };
    }

    function inDateRange(value) {
        if (!value) return true;
        const { from, to } = selectedRange();
        const time = new Date(value);
        if (Number.isNaN(time.getTime())) return true;
        if (from && time < from) return false;
        return !(to && time > to);
    }

    function refundDocumentDate(refund) {
        const cancellation = cancellations.find(c => c.orderId === refund.orderId);
        const order = orders.get(refund.orderId);
        return refund.refundedAt || refund.approvedAt || cancellation?.decidedAt || order?.createdAt;
    }

    function orderLines(order) {
        return order ? MilkTeaReceipts.lines(order) : '';
    }

    function renderStockTable(rows) {
        return `<div class="table-responsive">
            <table class="table align-middle">
                <thead>
                    <tr>
                        <th>Nguyên liệu</th>
                        <th>Vị trí</th>
                        <th>Thay đổi</th>
                        <th>Tồn sau</th>
                    </tr>
                </thead>
                <tbody>
                    ${rows.map(m => `<tr>
                        <td>${esc(m.materialName)}</td>
                        <td>${esc(m.location)}</td>
                        <td>${esc(m.deltaQuantity)}</td>
                        <td>${esc(m.afterQuantity)}</td>
                    </tr>`).join('')}
                </tbody>
            </table>
        </div>`;
    }

    
    function renderImports() {
        const filtered = imports.filter(i => inDateRange(i.createdAt));

        document.getElementById('importDocuments').innerHTML =
            filtered.map(i => {
                const title = `PHIẾU NHẬP PN${String(i.id).padStart(3, '0')}`;

                const summary = `
                    ${date(i.createdAt)} ·
                    ${esc(i.createdByEmail || 'Nhân viên')}
                `;

                const body = `
                    <p>
                        Nhà cung cấp:
                        ${esc(i.supplier || 'Không ghi')}
                    </p>
                    ${i.reason
                        ? `<p>Lý do: ${esc(i.reason)}</p>`
                        : ''}
                    ${renderStockTable(
                        movements.filter(m =>
                            m.sourceType === 'IMPORT' &&
                            m.sourceId === i.id
                        )
                    )}
                `;

                return collapsibleCard(
                    `import-${i.id}`,
                    title,
                    summary,
                    body,
                    'Đã nhập'
                );
            }).join('')
            || '<p class="text-muted py-4">Chưa có phiếu nhập kho phù hợp.</p>';
    }


    function renderMovements() {
        const type = document.getElementById('movementTypeFilter').value;
        const rows = movements.filter(m => (!type || m.sourceType === type) && inDateRange(m.createdAt));

        document.getElementById('movementDocuments').innerHTML = `<div class="table-responsive">
            <table class="table table-hover align-middle">
                <thead>
                    <tr>
                        <th>Ngày lập</th>
                        <th>Nghiệp vụ</th>
                        <th>Nguyên liệu</th>
                        <th>Vị trí</th>
                        <th>Thay đổi</th>
                        <th>Trước / sau</th>
                        <th>Lý do</th>
                    </tr>
                </thead>
                <tbody>
                    ${rows.map(m => `<tr>
                        <td class="text-nowrap">${date(m.createdAt)}</td>
                        <td>${esc(movementNames[m.sourceType] || m.sourceType)} #${esc(m.sourceId)}</td>
                        <td>${esc(m.materialName)}</td>
                        <td>${esc(m.location)}</td>
                        <td class="${Number(m.deltaQuantity) < 0 ? 'text-danger' : 'text-success'}">
                            ${Number(m.deltaQuantity) > 0 ? '+' : ''}${esc(m.deltaQuantity)}
                        </td>
                        <td>${esc(m.beforeQuantity)} -> ${esc(m.afterQuantity)}</td>
                        <td>${esc(m.reason || '-')}</td>
                    </tr>`).join('') || '<tr><td colspan="7" class="text-center py-4 text-muted">Chưa có chứng từ phù hợp.</td></tr>'}
                </tbody>
            </table>
        </div>`;
    }

    
    function renderCancellations() {
        const filtered = cancellations.filter(c => inDateRange(c.createdAt));

        document.getElementById('cancellationDocuments').innerHTML =
            filtered.map(c => {
                const order = orders.get(c.orderId);
                const refund = refunds.find(r => r.orderId === c.orderId);

                const refundState = refund
                    ? `<span>Phiếu hoàn RF${refund.id} · ${esc(statusText[refund.status] || refund.status)}</span>`
                    : c.status === 'CHAP_THUAN' && order?.paid
                        ? '<span>Chờ nhân viên gửi yêu cầu hoàn tiền.</span>'
                        : '<span>Không có khoản thu cần hoàn.</span>';

                const title = `
                    PHIẾU HỦY #${c.id} ·
                    ${MilkTeaReceipts.code(c.orderId)}
                `;

                const summary = `
                    ${date(c.createdAt)} ·
                    Khách: ${esc(order?.customerName || 'Khách vãng lai')} ·
                    Bàn: ${esc(order?.tableName || 'Quầy')}
                `;

                const body = `
                    ${orderLines(order)}
                    <p>Lý do: ${esc(c.reason)}</p>
                    <p>
                        Trạng thái:
                        ${esc(statusText[c.status] || c.status)}
                    </p>
                    ${c.decidedAt
                        ? `<p>Nhân viên xác nhận:
                            ${esc(c.decidedBy || 'Nhân viên')} ·
                            ${date(c.decidedAt)}</p>`
                        : ''}
                    <div class="d-flex gap-2">
                        ${c.status === 'CHO'
                            ? '<span class="text-warning small"><i class="bi bi-hourglass-split me-1"></i>Đang chờ thu ngân xác nhận hủy đơn tại POS/Đơn hàng.</span>'
                            : refundState}
                    </div>
                `;

                return collapsibleCard(
                    `cancel-${c.id}`,
                    title,
                    summary,
                    body,
                    statusText[c.status] || c.status
                );
            }).join('')
            || '<p class="text-muted">Chưa có phiếu hủy phù hợp.</p>';
    }

    
    function renderRefunds() {
        const filtered = refunds.filter(r =>
            inDateRange(refundDocumentDate(r))
        );

        document.getElementById('refundDocuments').innerHTML =
            filtered.map(r => {
                const order = orders.get(r.orderId);
                const cancellation = cancellations.find(
                    c => c.orderId === r.orderId
                );
                const status = statusText[r.status] || r.status;

                const title = `
                    PHIẾU HOÀN RF${r.id} ·
                    ${MilkTeaReceipts.code(r.orderId)}
                `;

                const summary = `
                    ${date(refundDocumentDate(r))} ·
                    Khách: ${esc(order?.customerName || 'Khách vãng lai')} ·
                    Bàn: ${esc(order?.tableName || 'Quầy')}
                `;

                const body = `
                    ${orderLines(order)}

                    <p>
                        Lý do hoàn:
                        ${esc(r.reason || cancellation?.reason || '-')}
                    </p>

                    <p>
                        Số tiền hoàn:
                        <b>${MilkTeaApi.formatVND(r.amount)}</b>
                    </p>

                    <p>Trạng thái: ${esc(status)}</p>

                    ${r.approvedAt
                        ? `<p>Admin xác nhận:
                            ${esc(r.approvedBy || 'Admin')} ·
                            ${date(r.approvedAt)}</p>`
                        : ''}

                    ${r.refundedAt
                        ? `<p>Thời gian hoàn:
                            ${date(r.refundedAt)} ·
                            Người thực hiện:
                            ${esc(r.refundedBy || r.approvedBy || 'Admin')}</p>`
                        : ''}

                    <div class="d-flex gap-2 flex-wrap">
                        ${r.status === 'CHO_DUYET'
                            ? btn(
                                'approveRefund',
                                r.id,
                                'Xác nhận đã hoàn chuyển khoản',
                                'success'
                            ) + btn(
                                'rejectRefund',
                                r.id,
                                'Từ chối',
                                'danger'
                            )
                            : ['DA_DUYET', 'DA_HOAN'].includes(r.status)
                                ? btn(
                                    'receipt',
                                    r.orderId,
                                    'Xem phiếu hoàn tiền',
                                    'outline-secondary'
                                )
                                : ''}
                    </div>
                `;

                return collapsibleCard(
                    `refund-${r.id}`,
                    title,
                    summary,
                    body,
                    status
                );
            }).join('')
            || '<p class="text-muted">Chưa có phiếu hoàn phù hợp.</p>';
    }


    function render() {
        renderImports();
        renderMovements();
        renderCancellations();
        renderRefunds();
        refreshAdminRefundBadge();
    }

    async function refreshAdminRefundBadge() {
        try {
            const res = await MilkTeaApi.get('/api/v1/admin/documents/refunds/pending-count');
            const count = (res && typeof res.pendingCount === 'number') ? res.pendingCount : 0;
            document.querySelectorAll('.admin-refund-badge').forEach(el => {
                el.textContent = count;
                el.classList.toggle('d-none', count === 0);
            });
        } catch (e) {}
    }

    async function load() {
        try {
            [cancellations, refunds, imports, movements] = await Promise.all([
                MilkTeaApi.get('/api/v1/admin/cancellation-requests'),
                MilkTeaApi.get('/api/v1/admin/refunds'),
                MilkTeaApi.get('/api/v1/inventory/imports'),
                MilkTeaApi.get('/api/v1/inventory/movements')
            ]);

            const ids = [...new Set([
                ...cancellations.map(c => c.orderId),
                ...refunds.map(r => r.orderId)
            ])];
            const results = await Promise.all(ids.map(id => MilkTeaApi.get(`/api/v1/customer/orders/${id}`)));
            orders = new Map(results.map(o => [o.orderId, o]));

            document.getElementById('documentError').classList.add('d-none');
            render();
        } catch (e) {
            const box = document.getElementById('documentError');
            box.textContent = e.message;
            box.classList.remove('d-none');
        }
    }

    async function action(name, id) {
        if (name === 'receipt') {
            await MilkTeaReceipts.open(id);
            return;
        }

        if (busy) return;
        busy = true;
        document.querySelectorAll('[data-doc-action]').forEach(b => b.disabled = true);

        try {
            if (name === 'approveRefund' || name === 'rejectRefund') {
                const approved = name === 'approveRefund';
                await MilkTeaApi.post(`/api/v1/admin/refunds/${id}/decision`, {
                    approved: approved,
                    reason: approved ? 'Admin xác nhận đã hoàn chuyển khoản qua ngân hàng' : 'Admin từ chối yêu cầu hoàn tiền'
                });
                MilkTeaApi.showToast(approved ? 'Đã xác nhận hoàn chuyển khoản thành công!' : 'Đã từ chối yêu cầu hoàn tiền.');
            }

            await load();
        } catch (e) {
            MilkTeaApi.showToast(e.message, 'error');
        } finally {
            busy = false;
            document.querySelectorAll('[data-doc-action]').forEach(b => b.disabled = false);
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('movementTypeFilter').addEventListener('change', render);
        document.getElementById('applyDocumentDateFilter').addEventListener('click', () => {
            appliedDateFrom = document.getElementById('documentDateFrom').value;
            appliedDateTo = document.getElementById('documentDateTo').value;
            render();
        });

        window.MilkTeaContext = { role: 'ADMIN' };
        MilkTeaRealtime.on('STOCK_CHANGED', load);
        ['ORDER_STATUS_CHANGED', 'REFUND_CHANGED'].forEach(t => MilkTeaRealtime.on(t, load));
        MilkTeaRealtime.onReconnect(load);
        MilkTeaRealtime.connect();

        document.addEventListener('click', event => {
            const button = event.target.closest('[data-doc-action]');
            if (button) action(button.dataset.docAction, button.dataset.id);
        });

                
        document.addEventListener('toggle', event => {
            const detail = event.target;

            if (!detail.matches?.('.document-fold')) return;

            const key = detail.dataset.documentKey;

            if (!key) return;

            if (detail.open) {
                expandedDocuments.add(key);
            } else {
                expandedDocuments.delete(key);
            }
        }, true);


        load();
    });

    return { load };
})();
