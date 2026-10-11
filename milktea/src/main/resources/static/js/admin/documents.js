const DocumentsApp = (() => {
    const esc=MilkTeaReceipts.esc, date=MilkTeaReceipts.date;
    let cancellations=[],refunds=[],issues=[],imports=[],movements=[],orders=new Map(),busy=false;
    const btn=(action,id,label,css='primary')=>`<button class="btn btn-sm btn-${css}" data-doc-action="${action}" data-id="${id}">${label}</button>`;
    const card=body=>`<article class="card shadow-sm p-3">${body}</article>`;
    function render() {
        const table=rows=>`<div class="table-responsive"><table class="table align-middle"><thead><tr><th>Nguyên liệu</th><th>Vị trí</th><th>Thay đổi</th><th>Tồn sau</th></tr></thead><tbody>${rows.map(m=>`<tr><td>${esc(m.materialName)}</td><td>${esc(m.location)}</td><td>${esc(m.deltaQuantity)}</td><td>${esc(m.afterQuantity)}</td></tr>`).join('')}</tbody></table></div>`;
        document.getElementById('importDocuments').innerHTML=imports.map(i=>card(`<div class="d-flex justify-content-between"><h6>PHIẾU NHẬP PN${String(i.id).padStart(3,'0')}</h6><span class="badge bg-success">Đã nhập</span></div><p class="text-muted small">${date(i.createdAt)} · ${esc(i.createdByEmail || 'Nhân viên')}</p><p>Nhà cung cấp: ${esc(i.supplier || 'Không ghi')} · ${esc(i.reason || '')}</p>${table(movements.filter(m=>m.sourceType==='IMPORT' && m.sourceId===i.id))}`)).join('') || '<p class="text-muted py-4">Chưa có phiếu nhập kho.</p>';
        const type=document.getElementById('movementTypeFilter').value;
        const names={IMPORT:'Nhập kho',ISSUE:'Chuyển kho',ORDER:'Tiêu hao đơn',BATCH:'Sơ chế',WASTE:'Hao hụt',REMAKE:'Pha bù'};
        const rows=movements.filter(m=>!type || m.sourceType===type);
        document.getElementById('movementDocuments').innerHTML=`<div class="table-responsive"><table class="table table-hover align-middle"><thead><tr><th>Ngày lập</th><th>Nghiệp vụ</th><th>Nguyên liệu</th><th>Vị trí</th><th>Thay đổi</th><th>Trước / sau</th><th>Lý do</th></tr></thead><tbody>${rows.map(m=>`<tr><td class="text-nowrap">${date(m.createdAt)}</td><td>${esc(names[m.sourceType] || m.sourceType)} #${esc(m.sourceId)}</td><td>${esc(m.materialName)}</td><td>${esc(m.location)}</td><td class="${Number(m.deltaQuantity)<0?'text-danger':'text-success'}">${Number(m.deltaQuantity)>0?'+':''}${esc(m.deltaQuantity)}</td><td>${esc(m.beforeQuantity)} → ${esc(m.afterQuantity)}</td><td>${esc(m.reason || '—')}</td></tr>`).join('') || '<tr><td colspan="7" class="text-center py-4 text-muted">Chưa có chứng từ phù hợp.</td></tr>'}</tbody></table></div>`;

        document.getElementById('cancellationDocuments').innerHTML=cancellations.map(c=> {
            const o=orders.get(c.orderId),r=refunds.find(r=>r.orderId===c.orderId);
            return card(`<h6>PHIẾU HỦY #${c.id} · ${MilkTeaReceipts.code(c.orderId)}</h6><p>Ngày lập: ${date(c.createdAt)} · Khách: ${esc(o?.customerName || 'Khách vãng lai')} · Bàn: ${esc(o?.tableName || 'Quầy')}</p>${o?MilkTeaReceipts.lines(o):''}<p>Lý do: ${esc(c.reason)}</p><p>Trạng thái: ${esc({CHO:'Chờ xác nhận',CHAP_THUAN:'Đã xác nhận hủy',TU_CHOI:'Từ chối hủy'}[c.status])}</p>${c.decidedAt?`<p>Nhân viên xác nhận: ${esc(c.decidedBy || 'Nhân viên')} · ${date(c.decidedAt)}</p>`:''}<div class="d-flex gap-2">${c.status==='CHO'?btn('approveCancel',c.id,'Xác nhận hủy','danger')+btn('rejectCancel',c.id,'Từ chối','secondary'):c.status==='CHAP_THUAN'&&o?.paid&&!r?'<span>Chờ nhân viên gửi yêu cầu hoàn tiền.</span>':r?`<span>Phiếu hoàn RF${r.id} · ${esc({CHO_DUYET:'Chờ duyệt',DA_DUYET:'Đã duyệt, chờ thực chi',DA_HOAN:'Đã hoàn tiền',TU_CHOI:'Từ chối'}[r.status])}</span>`:'<span>Không có khoản thu cần hoàn.</span>'}</div>`);
        }).join('')||'<p class="text-muted">Chưa có phiếu hủy.</p>';
        document.getElementById('refundDocuments').innerHTML=refunds.map(r=>card(`<h6>YÊU CẦU HOÀN TIỀN RF${r.id} · ${MilkTeaReceipts.code(r.orderId)}</h6><p>Tự ánh xạ từ phiếu hủy: ${esc(r.reason)}</p><p>Số tiền: <b>${MilkTeaApi.formatVND(r.amount)}</b> · ${esc({CHO_DUYET:'Chờ Admin duyệt',DA_DUYET:'Đã duyệt, chờ thực chi',DA_HOAN:'Đã hoàn tiền',TU_CHOI:'Bị từ chối'}[r.status])}</p><div class="d-flex gap-2">${r.status==='CHO_DUYET'?btn('approveRefund',r.id,'Duyệt hoàn tiền','success')+btn('rejectRefund',r.id,'Từ chối','danger'):['DA_DUYET','DA_HOAN'].includes(r.status)?btn('receipt',r.orderId,'Xem phiếu hoàn tiền','outline-secondary'):''}</div>`)).join('')||'<p class="text-muted">Chưa có yêu cầu hoàn tiền.</p>';
        document.getElementById('issueDocuments').innerHTML=issues.map(i=>card(`<h6>PHIẾU LẤY NGUYÊN LIỆU #${i.id}</h6><p>Người lập: ${esc(i.createdBy)} · Thời gian chi tiết: ${date(i.createdAt)}</p><table class="table"><thead><tr><th>Nguyên liệu</th><th>Số lượng</th><th>Đơn vị</th></tr></thead><tbody>${i.items.map(l=>`<tr><td>${esc(l.materialName)}</td><td>${esc(l.quantity)}</td><td>${esc(l.unit)}</td></tr>`).join('')}</tbody></table><small>Đã chuyển kho → bếp, không cần phê duyệt.</small>`)).join('')||'<p class="text-muted">Chưa có phiếu lấy nguyên liệu.</p>';
    }
    async function load() {
        try {
            [cancellations,refunds,issues,imports,movements]=await Promise.all([MilkTeaApi.get('/api/v1/admin/cancellation-requests'),MilkTeaApi.get('/api/v1/admin/refunds'),MilkTeaApi.get('/api/v1/inventory/issues'),MilkTeaApi.get('/api/v1/inventory/imports'),MilkTeaApi.get('/api/v1/inventory/movements')]);
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
        document.getElementById('movementTypeFilter').addEventListener('change',render);
        window.MilkTeaContext={role:'ADMIN'};MilkTeaRealtime.on('STOCK_CHANGED',load);
        ['ORDER_STATUS_CHANGED','REFUND_CHANGED'].forEach(t=>MilkTeaRealtime.on(t,load));MilkTeaRealtime.onReconnect(load);MilkTeaRealtime.connect();
        document.addEventListener('click',event=>{const b=event.target.closest('[data-doc-action]');if(b)action(b.dataset.docAction,b.dataset.id);});load();setInterval(()=>{if(!document.hidden&&!busy)load();},15000);
    });
    return {load};
})();
