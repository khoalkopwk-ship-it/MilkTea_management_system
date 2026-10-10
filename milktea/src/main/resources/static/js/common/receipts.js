const MilkTeaReceipts = (() => {
    const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const money = v => MilkTeaApi.formatVND(v || 0);
    const date = v => v ? new Date(v).toLocaleString('vi-VN') : '—';
    const code = id => 'ORD' + String(id).padStart(3, '0');
    let currentOrder;
    function lines(o) {
        return `<table class="receipt-items"><thead><tr><th>Tên món</th><th>Số lượng</th><th>Thành tiền</th></tr></thead><tbody>${(o.items || []).map(i => `<tr><td>${esc(i.productName)} <small>${esc(i.size)}</small></td><td>${i.quantity}</td><td>${money(i.lineTotal)}</td></tr>`).join('')}</tbody></table>`;
    }
    function shop() {
        const e = document.getElementById('receiptShop');
        return {name:e?.dataset.name || 'Milktea Café', address:e?.dataset.address || ''};
    }
    function invoice(o) {
        const s = shop();
        return `<article class="milk-receipt invoice"><header><img class="receipt-logo" src="/images/receipt-logo.svg" alt="MilkTea"><div><b>${esc(s.name)}</b><div>${esc(s.address)}</div></div></header><hr><h4>HÓA ĐƠN THANH TOÁN</h4>
        <h6>Thông tin hóa đơn</h6><div class="receipt-meta"><span>Mã hóa đơn: HD${o.invoiceId != null ? String(o.invoiceId).padStart(3,'0') : '—'}</span><span>Ngày lập: ${date(o.createdAt)}</span><span>Mã đơn hàng: ${code(o.orderId)}</span><span>Nhân viên: ${esc(o.cashierName || 'Chưa ghi thu')}</span></div>
        <h6>Thông tin khách hàng</h6><div class="receipt-meta"><span>Khách hàng: ${esc(o.customerName || 'Khách vãng lai')}</span><span>Bàn: ${esc(o.tableName || 'Quầy / mang về')}</span></div>
        ${lines(o)}<div class="receipt-total"><div><span>Tạm tính:</span><b>${money(o.subtotal)}</b></div><div><span>Giảm giá:</span><b>${money(o.discountAmount)}</b></div><div><span>Phụ thu:</span><b>${money(o.surchargeAmount)}</b></div></div>
        <p>Phương thức thanh toán: ${esc(o.paymentMethod === 'CASH' ? 'Tiền mặt' : o.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản' : 'Chưa ghi nhận')}</p>
        <div class="receipt-grand">Tổng cộng: <b>${money(o.totalAmount)}</b></div><p class="receipt-state">${o.status === 'DA_HUY' ? 'ĐƠN ĐÃ HỦY' : o.paid ? 'ĐÃ THANH TOÁN' : 'CHƯA THU TIỀN — Chờ thu ngân xác nhận'}</p>${o.paidAt ? `<p>Thu tiền lúc: ${date(o.paidAt)}</p>` : ''}</article>`;
    }
    function refund(o) {
        const r=o.refund;
        return `<article class="milk-receipt refund"><header><h4>PHIẾU HOÀN TIỀN</h4><div class="text-end"><img class="receipt-logo" src="/images/receipt-logo.svg" alt="MilkTea"><b class="d-block">${esc(shop().name)}</b></div></header><hr>
        <div class="receipt-meta"><span>Số phiếu: RF${String(r.id).padStart(3,'0')}</span><span>Ngày lập: ${date(r.approvedAt)}</span><span>Mã đơn hàng: ${code(o.orderId)}</span><span>Khách hàng: ${esc(o.customerName || 'Khách vãng lai')}</span><span>Nhân viên thu: ${esc(o.cashierName || '—')}</span></div>${lines(o)}
        ${Number(o.discountAmount) > 0 ? `<p>Tổng giá món: ${money(o.subtotal)} · Giảm giá: ${money(o.discountAmount)}</p>` : ''}
        <div class="receipt-grand">Tổng tiền hoàn: <b>${money(r.amount)}</b></div><p>Lý do: ${esc(r.reason)}</p><h5>Thông tin phê duyệt</h5><div class="receipt-meta"><span>Người phê duyệt: ${esc(r.approvedBy || 'Admin')}</span><span>Thời gian duyệt: ${date(r.approvedAt)}</span></div>
        <p>${r.status === 'DA_HOAN' ? 'ĐÃ THỰC CHI HOÀN TIỀN · ' + date(r.refundedAt) : 'ĐÃ DUYỆT — CHỜ THỰC CHI HOÀN TIỀN'}</p></article>`;
    }
    function ensureModal() {
        if (document.getElementById('milkReceiptModal')) return;
        document.body.insertAdjacentHTML('beforeend', `<div class="modal fade" id="milkReceiptModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">Hóa đơn / chứng từ</h5><button class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div><div class="modal-body" id="milkReceiptBody"></div><div class="modal-footer" id="milkReceiptFooter"><button class="btn btn-primary-custom" onclick="MilkTeaReceipts.print()">In hóa đơn / phiếu</button><button class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button></div></div></div></div>`);
    }
    function show(o, forceInvoice=false) {
        currentOrder = o; ensureModal();
        document.getElementById('milkReceiptFooter').innerHTML = '<button class="btn btn-primary-custom" onclick="MilkTeaReceipts.print()">In hóa đơn / phiếu</button><button class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>';
        document.getElementById('milkReceiptBody').innerHTML = !forceInvoice && o.status === 'DA_HUY' && ['DA_DUYET','DA_HOAN'].includes(o.refund?.status) ? refund(o) : invoice(o);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('milkReceiptModal')).show();
    }
    async function open(id) {
        try { show(await MilkTeaApi.get(`/api/v1/customer/orders/${id}`)); } catch(e) { MilkTeaApi.showToast(e.message,'error'); }
    }
    function print() {
        if (!currentOrder) return;
        const w = window.open('', '_blank', 'width=800,height=750');
        if (!w) { MilkTeaApi.showToast('Hãy cho phép mở cửa sổ để in','warning'); return; }
        w.document.write(`<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><title>Hóa đơn</title><link rel="stylesheet" href="/css/receipts.css"></head><body>${document.getElementById('milkReceiptBody').innerHTML}</body></html>`);
        w.document.close(); w.onload = () => w.print();
    }
    return {esc, date, code, lines, show, open, print};
})();
