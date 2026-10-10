const OrdersApp = (() => {
    const e = MilkTeaReceipts.esc;
    const labels = {CHO_THANH_TOAN:'Chờ thanh toán',CHO_XAC_NHAN:'Chờ xác nhận',CHO_CHE_BIEN:'Chờ pha chế',DANG_CHE_BIEN:'Đang pha chế',HOAN_THANH:'Hoàn thành',DA_HUY:'Đã hủy'};
    let orders=[], cancellations=[], staff=false, busy=false, cancelId=null;
    function button(action,id,label,css='outline-secondary') {return `<button class="btn btn-sm btn-${css} rounded-pill" data-action="${action}" data-id="${id}">${label}</button>`;}
    function render() {
        const query=document.getElementById('orderSearch').value.trim().toUpperCase().replace(/^#/, '');
        const status=document.getElementById('orderStatus').value;
        const filtered=orders.filter(o => (!query || MilkTeaReceipts.code(o.orderId).includes(query) || String(o.orderId)===query) && (!status || (status==='CANCEL_PENDING' ? o.cancellationPending : o.status===status)));
        document.getElementById('ordersList').innerHTML = filtered.map(o => {
            const cancel=cancellations.find(c=>c.orderId===o.orderId && c.status==='CHO');
            let actions=button('receipt',o.orderId,o.status==='DA_HUY' && ['DA_DUYET','DA_HOAN'].includes(o.refund?.status) ? 'Phiếu hoàn tiền' : 'Xem hóa đơn');
            if (o.status!=='DA_HUY' && !o.paid) actions += staff ? (o.staffCreated ? button('cash',o.orderId,'Thu tiền mặt','success') : '')+button('bank',o.orderId,'Thu chuyển khoản','primary') : button('pay',o.orderId,'Thanh toán','primary');
            if (staff && o.status==='CHO_XAC_NHAN' && !o.cancellationPending) actions += button('confirm',o.orderId,'Xác nhận đơn','primary');
            if (!o.cancellationPending && ['CHO_THANH_TOAN','CHO_XAC_NHAN','CHO_CHE_BIEN'].includes(o.status)) actions += button('cancel',o.orderId,'Yêu cầu hủy','outline-danger');
            if (staff && cancel) actions += button('approveCancel',cancel.id,'Xác nhận hủy','danger')+button('rejectCancel',cancel.id,'Từ chối hủy');
            if(staff && o.source==='COUNTER' && !o.sessionId && o.status!=='DA_HUY') actions += button('assign',o.orderId,'Gán bàn');
            const approvedCancel=cancellations.find(c=>c.orderId===o.orderId && c.status==='CHAP_THUAN');
            if (staff && o.status==='DA_HUY' && o.paid && !o.refund && approvedCancel) actions += button('requestRefund',approvedCancel.id,'Yêu cầu hoàn tiền','warning');
            const refundLabel={CHO_DUYET:'Hoàn tiền chờ Admin duyệt',DA_DUYET:'Hoàn tiền đã duyệt, chờ thực chi',DA_HOAN:'Đã hoàn tiền',TU_CHOI:'Hoàn tiền bị từ chối'}[o.refund?.status];
            return `<article class="card border-0 shadow-sm rounded-4 p-3"><div class="d-flex justify-content-between gap-2"><b>${MilkTeaReceipts.code(o.orderId)} · ${e(o.tableName || 'Quầy / mang về')}</b><span class="badge-status badge-${o.status}">${o.cancellationPending ? 'Chờ xác nhận hủy' : labels[o.status] || e(o.status)}</span></div><small class="text-muted">${MilkTeaReceipts.date(o.createdAt)}</small><div class="my-2">${(o.items||[]).map(i=>`<div class="d-flex justify-content-between border-bottom py-2"><span>${e(i.productName)} · ${e(i.size)} × ${i.quantity}</span><b>${MilkTeaApi.formatVND(i.lineTotal)}</b></div>`).join('')}</div><div class="d-flex flex-wrap justify-content-between gap-2"><b>Tổng: ${MilkTeaApi.formatVND(o.totalAmount)} <small class="text-${o.paid?'success':'warning'}">${o.paid?'Đã thu':'Chưa thu'}</small></b><div class="d-flex flex-wrap gap-2">${actions}</div></div>${refundLabel ? `<p class="mt-2 mb-0 text-muted">${refundLabel}</p>` : ''}${cancel ? `<p class="mb-0 mt-2">Lý do hủy: ${e(cancel.reason)}</p>`:''}</article>`;
        }).join('') || '<div class="text-center py-5 text-muted">Không có đơn phù hợp.</div>';
    }
    async function load() {
        try {
            orders=await MilkTeaApi.get('/api/v1/customer/orders') || [];
            if(staff) cancellations=await MilkTeaApi.get('/api/v1/cashier/cancellation-requests') || [];
            document.getElementById('ordersError').classList.add('d-none'); render();
        } catch(err) { const box=document.getElementById('ordersError'); box.textContent=err.message || 'Không thể tải đơn. Nhấn Làm mới để thử lại.';box.classList.remove('d-none');document.getElementById('ordersList').textContent=''; }
    }
    function modal(title,body,footer) {
        document.getElementById('orderActionTitle').textContent=title;
        document.getElementById('orderActionBody').innerHTML=body;
        document.getElementById('orderActionFooter').innerHTML=footer;
        bootstrap.Modal.getOrCreateInstance(document.getElementById('orderActionModal')).show();
    }
    async function action(name,id) {
        const o=orders.find(o=>String(o.orderId)===String(id));
        if(name==='receipt') { await MilkTeaReceipts.open(id);return; }
        if(name==='assign') {
            try {
                const tables=await MilkTeaApi.get('/api/v1/cashier/tables');
                modal('Gán bàn '+MilkTeaReceipts.code(id),`<label for="assignOrderTable">Chọn bàn</label><select class="form-select" id="assignOrderTable">${tables.map(t=>`<option value="${t.id}">${e(t.name)} · ${t.status==='CO_KHACH'?'Có khách':'Trống'}</option>`).join('')}</select>`,button('sendAssign',id,'Xác nhận gán bàn','primary'));
            }catch(err){MilkTeaApi.showToast(err.message,'error');}return;
        }
        if(name==='cancel') {
            cancelId=id;modal('Yêu cầu hủy '+MilkTeaReceipts.code(id),'<p>Chỉ hủy khi bếp chưa bắt đầu pha chế.</p><label for="orderCancelReason">Lý do hủy</label><textarea maxlength="500" id="orderCancelReason" class="form-control" rows="3"></textarea>',button('sendCancel',id,'Gửi yêu cầu hủy','danger')); return;
        }
        if(name==='pay') {
            MilkTeaReceipts.show(o);
            document.getElementById('milkReceiptFooter').insertAdjacentHTML('afterbegin',button('paymentOptions',id,'Tiếp tục thanh toán','success'));
            return;
        }
        if(name==='paymentOptions') {
            const receiptModal=document.getElementById('milkReceiptModal');
            receiptModal.addEventListener('hidden.bs.modal',()=> {
                const shop=document.getElementById('receiptShop'); const bank=shop.dataset.bank || 'Vietcombank';
                const account=shop.dataset.account || ''; const holder=shop.dataset.holder || '';
                const img=account ? `<img alt="QR chuyển khoản" class="d-block mx-auto my-3" width="220" src="https://img.vietqr.io/image/${encodeURIComponent(bank)}-${encodeURIComponent(account)}-compact2.png?amount=${encodeURIComponent(o.totalAmount)}&addInfo=DH${id}&accountName=${encodeURIComponent(holder)}">` : '<p>Liên hệ thu ngân để nhận thông tin chuyển khoản.</p>';
                modal('Thanh toán '+MilkTeaReceipts.code(id),`<p>Số tiền: <b>${MilkTeaApi.formatVND(o.totalAmount)}</b></p><p>${e(bank)} · ${e(account)} · ${e(holder)}</p>${img}<p>Thông báo chuyển khoản cần được thu ngân kiểm tra, ghi thu.</p>`,button('noticeBank',id,'Tôi đã chuyển khoản','primary'));
            },{once:true});
            const hideReceipt=()=>bootstrap.Modal.getInstance(receiptModal).hide();
            if(getComputedStyle(receiptModal).opacity==='1') hideReceipt();
            else receiptModal.addEventListener('shown.bs.modal',hideReceipt,{once:true});
            return;
        }
        if(busy) return;
        busy=true; document.querySelectorAll('[data-action]').forEach(b=>b.disabled=true);
        try {
            if(name==='cash'||name==='bank') {
                await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/payments`,{method:name==='cash'?'CASH':'BANK_TRANSFER',reference:'RECEIPT_'+id});
                await load();await MilkTeaReceipts.open(id);
            } else if(name==='sendAssign') await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/assign-table`,{tableId:document.getElementById('assignOrderTable').value});
            else if(name==='confirm') await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/confirm`);
            else if(name==='sendCancel') {
                const reason=document.getElementById('orderCancelReason').value.trim();
                if(!reason) throw new Error('Nhập lý do hủy đơn.');
                await MilkTeaApi.post(`/api/v1/customer/orders/${cancelId}/cancellation-requests`,{reason});
            } else if(name==='approveCancel'||name==='rejectCancel') await MilkTeaApi.post(`/api/v1/cashier/cancellation-requests/${id}/decision`,{approved:name==='approveCancel',reason:'Nhân viên xác nhận'});
            else if(name==='requestRefund') await MilkTeaApi.post(`/api/v1/cashier/cancellation-requests/${id}/refund`);
            else if(name==='noticeBank') await MilkTeaApi.post(`/api/v1/orders/${id}/payment-notices`,{method:'BANK_TRANSFER',reference:'NOTICE_'+id});
            const actionModal=document.getElementById('orderActionModal');
            const hideAction=()=>bootstrap.Modal.getInstance(actionModal)?.hide();
            if(!actionModal.classList.contains('show') || getComputedStyle(actionModal).opacity==='1') hideAction();
            else actionModal.addEventListener('shown.bs.modal',hideAction,{once:true});
            MilkTeaApi.showToast('Đã ghi nhận thành công.','success');await load();
        } catch(err) { MilkTeaApi.showToast(err.message,'error'); }
        finally { busy=false; document.querySelectorAll('[data-action]').forEach(b=>b.disabled=false); }
    }
    async function init() {
        staff=!!document.getElementById('staffOrders');
        const page=document.getElementById('ordersPage');
        await CustomerApp.initContext(page.dataset.qr || null,page.dataset.table || null);
        if(staff) window.MilkTeaContext.role=document.getElementById('staffOrders').dataset.role || 'CASHIER';
        document.body.insertAdjacentHTML('beforeend','<div class="modal fade" id="orderActionModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered"><div class="modal-content"><div class="modal-header"><h5 id="orderActionTitle"></h5><button class="btn-close" data-bs-dismiss="modal"></button></div><div id="orderActionBody" class="modal-body"></div><div id="orderActionFooter" class="modal-footer"></div></div></div></div>');
        document.getElementById('orderSearch').addEventListener('input',render);document.getElementById('orderStatus').addEventListener('change',render);
        document.addEventListener('click',event=> {const b=event.target.closest('[data-action]');if(b) action(b.dataset.action,b.dataset.id);});
        ['ORDER_CREATED','ORDER_STATUS_CHANGED','PAYMENT_RECORDED','REFUND_CHANGED'].forEach(type=>MilkTeaRealtime.on(type,load));MilkTeaRealtime.onReconnect(load);MilkTeaRealtime.connect();
        await load();
        const invoice=new URLSearchParams(location.search).get('invoice');if(invoice) { await MilkTeaReceipts.open(invoice);const url=new URL(location.href);url.searchParams.delete('invoice');history.replaceState(null,'',url.pathname+url.search); }
        // HTTP refresh keeps account/counter orders current even without a session topic.
        setInterval(()=>{if(!document.hidden && !busy) load();},15000);
    }
    document.addEventListener('DOMContentLoaded',init);
    return {load,action};
})();
