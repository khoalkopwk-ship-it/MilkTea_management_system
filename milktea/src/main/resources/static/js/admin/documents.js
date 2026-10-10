const DocumentsApp = (() => {
    const esc=MilkTeaReceipts.esc, date=MilkTeaReceipts.date;
    let cancellations=[],refunds=[],issues=[],orders=new Map(),busy=false;
    const btn=(action,id,label,css='primary')=>`<button class="btn btn-sm btn-${css}" data-doc-action="${action}" data-id="${id}">${label}</button>`;
    const card=body=>`<article class="card shadow-sm p-3">${body}</article>`;
    function render() {
        document.getElementById('cancellationDocuments').innerHTML=cancellations.map(c=> {
            const o=orders.get(c.orderId),r=refunds.find(r=>r.orderId===c.orderId);
            return card(`<h6>PHIẾU HỦY #${c.id} · ${MilkTeaReceipts.code(c.orderId)}</h6><p>Ngày lập: ${date(c.createdAt)} · Khách: ${esc(o?.customerName || 'Khách vãng lai')} · Bàn: ${esc(o?.tableName || 'Quầy')}</p>${o?MilkTeaReceipts.lines(o):''}<p>Lý do: ${esc(c.reason)}</p><p>Trạng thái: ${esc({CHO:'Chờ xác nhận',CHAP_THUAN:'Đã xác nhận hủy',TU_CHOI:'Từ chối hủy'}[c.status])}</p>${c.decidedAt?`<p>Nhân viên xác nhận: ${esc(c.decidedBy || 'Nhân viên')} · ${date(c.decidedAt)}</p>`:''}<div class="d-flex gap-2">${c.status==='CHO'?btn('approveCancel',c.id,'Xác nhận hủy','danger')+btn('rejectCancel',c.id,'Từ chối','secondary'):c.status==='CHAP_THUAN'&&o?.paid&&!r?'<span>Chờ nhân viên gửi yêu cầu hoàn tiền.</span>':r?`<span>Phiếu hoàn RF${r.id} · ${esc({CHO_DUYET:'Chờ duyệt',DA_DUYET:'Đã duyệt, chờ thực chi',DA_HOAN:'Đã hoàn tiền',TU_CHOI:'Từ chối'}[r.status])}</span>`:'<span>Không có khoản thu cần hoàn.</span>'}</div>`);
        }).join('')||'<p class="text-muted">Chưa có phiếu hủy.</p>';
        document.getElementById('refundDocuments').innerHTML=refunds.map(r=>card(`<h6>YÊU CẦU HOÀN TIỀN RF${r.id} · ${MilkTeaReceipts.code(r.orderId)}</h6><p>Tự ánh xạ từ phiếu hủy: ${esc(r.reason)}</p><p>Số tiền: <b>${MilkTeaApi.formatVND(r.amount)}</b> · ${esc({CHO_DUYET:'Chờ Admin duyệt',DA_DUYET:'Đã duyệt, chờ thực chi',DA_HOAN:'Đã hoàn tiền',TU_CHOI:'Bị từ chối'}[r.status])}</p><div class="d-flex gap-2">${r.status==='CHO_DUYET'?btn('approveRefund',r.id,'Duyệt hoàn tiền','success')+btn('rejectRefund',r.id,'Từ chối','danger'):['DA_DUYET','DA_HOAN'].includes(r.status)?btn('receipt',r.orderId,'Xem phiếu hoàn tiền','outline-secondary'):''}</div>`)).join('')||'<p class="text-muted">Chưa có yêu cầu hoàn tiền.</p>';
        document.getElementById('issueDocuments').innerHTML=issues.map(i=>card(`<h6>PHIẾU LẤY NGUYÊN LIỆU #${i.id}</h6><p>Người lập: ${esc(i.createdBy)} · Thời gian chi tiết: ${date(i.createdAt)}</p><table class="table"><thead><tr><th>Nguyên liệu</th><th>Số lượng</th><th>Đơn vị</th></tr></thead><tbody>${i.items.map(l=>`<tr><td>${esc(l.materialName)}</td><td>${esc(l.quantity)}</td><td>${esc(l.unit)}</td></tr>`).join('')}</tbody></table><small>Đã chuyển kho → bếp, không cần phê duyệt.</small>`)).join('')||'<p class="text-muted">Chưa có phiếu lấy nguyên liệu.</p>';
    }
    async function load() {
        try {
            [cancellations,refunds,issues]=await Promise.all([MilkTeaApi.get('/api/v1/admin/cancellation-requests'),MilkTeaApi.get('/api/v1/admin/refunds'),MilkTeaApi.get('/api/v1/inventory/issues')]);
            const ids=[...new Set(cancellations.map(c=>c.orderId))];
            const results=await Promise.all(ids.map(id=>MilkTeaApi.get(`/api/v1/customer/orders/${id}`)));orders=new Map(results.map(o=>[o.orderId,o]));
            document.getElementById('documentError').classList.add('d-none');render();
        } catch(e) {const box=document.getElementById('documentError');box.textContent=e.message;box.classList.remove('d-none');}
    }
    async function action(name,id) {
        if(name==='receipt'){await MilkTeaReceipts.open(id);return;}
        if(busy)return;busy=true;document.querySelectorAll('[data-doc-action]').forEach(b=>b.disabled=true);
        try {
            if(name==='approveCancel'||name==='rejectCancel')await MilkTeaApi.post(`/api/v1/admin/cancellation-requests/${id}/decision`,{approved:name==='approveCancel',reason:'Admin xác nhận'});
            else await MilkTeaApi.post(`/api/v1/admin/refunds/${id}/decision`,{approved:name==='approveRefund',reason:'Admin kiểm tra phiếu hủy'});
            MilkTeaApi.showToast('Đã cập nhật phiếu.');await load();
        }catch(e){MilkTeaApi.showToast(e.message,'error');}finally{busy=false;document.querySelectorAll('[data-doc-action]').forEach(b=>b.disabled=false);}
    }
    document.addEventListener('DOMContentLoaded',()=> {
        window.MilkTeaContext={role:'ADMIN'};MilkTeaRealtime.on('STOCK_CHANGED',load);
        ['ORDER_STATUS_CHANGED','REFUND_CHANGED'].forEach(t=>MilkTeaRealtime.on(t,load));MilkTeaRealtime.onReconnect(load);MilkTeaRealtime.connect();
        document.addEventListener('click',event=>{const b=event.target.closest('[data-doc-action]');if(b)action(b.dataset.docAction,b.dataset.id);});load();setInterval(()=>{if(!document.hidden&&!busy)load();},15000);
    });
    return {load};
})();
