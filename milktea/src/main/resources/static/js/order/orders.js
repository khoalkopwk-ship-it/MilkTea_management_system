const OrdersApp = (() => {
    const e = MilkTeaReceipts.esc;
    const labels = {CHO_THANH_TOAN:'Chờ thanh toán',CHO_XAC_NHAN:'Chờ xác nhận',CHO_CHE_BIEN:'Chờ chế biến',DANG_CHE_BIEN:'Đang chế biến',HOAN_THANH:'Hoàn thành',DA_HUY:'Đã hủy'};
    let orders=[], cancellations=[], staff=false, busy=false, cancelId=null;
    function button(action,id,label,css='outline-secondary') {return `<button class="btn btn-sm btn-${css} rounded-pill" data-action="${action}" data-id="${id}">${label}</button>`;}
    const detailActions = new Map();
    function render() {
        const query=document.getElementById('orderSearch').value.trim().toUpperCase().replace(/^#/, '');
        const status=document.getElementById('orderStatus').value;
        const filtered=orders.filter(o => (!query || MilkTeaReceipts.code(o.orderId).includes(query) || String(o.orderId)===query) && (!status || (status==='CANCEL_PENDING' ? o.cancellationPending : o.status===status)));
        const rows = filtered.map(o => {
            const cancel=cancellations.find(c=>c.orderId===o.orderId && c.status==='CHO');
            let actions=button('receipt',o.orderId,'Xem hóa đơn');
            if (staff) {
                if (cancel) actions += button('approveCancel',cancel.id,'Xác nhận hủy','danger')+button('rejectCancel',cancel.id,'Từ chối hủy');
                else if (o.status==='CHO_XAC_NHAN') actions += button('confirm',o.orderId,'Xác nhận đơn','primary');
                else if (o.status!=='DA_HUY' && !o.paid) actions += button('staffPayment',o.orderId,'Đã thanh toán','success');
                const approvedCancel=cancellations.find(c=>c.orderId===o.orderId && c.status==='CHAP_THUAN');
                if(o.status==='DA_HUY' && o.paid && !o.refund && approvedCancel) actions += button('requestRefund',approvedCancel.id,'Yêu cầu hoàn tiền','warning');
            } else {
                if(o.status!=='DA_HUY' && !o.paid) actions += button('pay',o.orderId,'Thanh toán','primary');
                if(!o.cancellationPending && ['CHO_THANH_TOAN','CHO_XAC_NHAN','CHO_CHE_BIEN'].includes(o.status)) actions += button('cancel',o.orderId,'Yêu cầu hủy','outline-danger');
            }
            detailActions.set(String(o.orderId), actions);
            const payment=paymentState(o);
            return `<tr><td><b>${MilkTeaReceipts.code(o.orderId)}</b><small class="d-block text-muted mt-1">${e(o.tableName || 'Quầy / mang về')}</small></td><td class="text-end fw-semibold text-nowrap">${MilkTeaApi.formatVND(o.totalAmount)}</td><td><span class="badge-status badge-${o.status}">${o.cancellationPending ? 'Chờ xác nhận hủy' : labels[o.status] || e(o.status)}</span></td><td><span class="payment-pill ${payment.css}">${payment.label}</span></td><td class="text-center"><button title="Xem chi tiết đơn" aria-label="Xem chi tiết ${MilkTeaReceipts.code(o.orderId)}" class="order-detail-btn" data-action="details" data-id="${o.orderId}"><i class="bi bi-sliders"></i><span class="ms-2 d-none d-lg-inline">Chi tiết</span></button></td></tr>`;
        }).join('');
        document.getElementById('ordersList').innerHTML = `<div class="orders-panel table-responsive"><table class="table table-hover orders-table align-middle mb-0"><thead><tr><th>Mã đơn</th><th class="text-end">Tổng tiền</th><th>Trạng thái đơn</th><th>Tình trạng</th><th class="text-center">Chi tiết</th></tr></thead><tbody>${rows || '<tr><td colspan="5" class="text-center py-5 text-muted">Không có đơn phù hợp.</td></tr>'}</tbody></table></div>`;
        window.PendingOrders?.refresh();
    }
    function paymentState(o) {
        if(o.status==='DA_HUY' && o.refund) {
            if(o.refund.status==='DA_HOAN') return {label:'Đã hoàn tiền',css:'payment-done'};
            if(['CHO_DUYET','DA_DUYET'].includes(o.refund.status)) return {label:'Chờ hoàn tiền',css:'payment-wait'};
            if(o.refund.status==='TU_CHOI') return {label:'Hoàn tiền bị từ chối',css:'payment-danger'};
        }
        if(o.paid) return {label:'Đã thu tiền',css:'payment-done'};
        if(o.status==='DA_HUY') return {label:'Không cần thu tiền',css:'payment-muted'};
        if(o.paymentRequestMethod) return {label:'Chờ thu · '+(o.paymentRequestMethod==='CASH'?'Tiền mặt':'Chuyển khoản'),css:'payment-wait'};
        return {label:'Chưa thu tiền',css:'payment-wait'};
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
        if(name==='staffPayment') {
            modal('Xác nhận thanh toán '+MilkTeaReceipts.code(id),`<p class="text-muted">Chỉ xác nhận sau khi đã nhận đủ tiền hoặc kiểm tra giao dịch chuyển khoản.</p><div class="payment-confirm-total">${MilkTeaApi.formatVND(o.totalAmount)}</div><p class="mt-3">${o.paymentRequestMethod ? 'Khách chọn '+(o.paymentRequestMethod==='CASH'?'tiền mặt':'chuyển khoản')+'.':'Chọn phương thức đã nhận tiền.'}</p>`,button('cash',id,'Đã nhận tiền mặt','success')+button('bank',id,'Đã nhận chuyển khoản','primary'));return;
        }
        if(name==='details') {
            modal('Chi tiết '+MilkTeaReceipts.code(id), `<p><b>${e(o.tableName || 'Quầy / mang về')}</b> · ${MilkTeaReceipts.date(o.createdAt)}</p>${MilkTeaReceipts.lines(o)}<p>Tổng: <b>${MilkTeaApi.formatVND(o.totalAmount)}</b> · ${paymentState(o).label}</p>${o.paymentRequestMethod ? '<p class="text-warning">Khách yêu cầu thanh toán '+(o.paymentRequestMethod==='CASH'?'tiền mặt':'chuyển khoản')+'. Kiểm tra khoản tiền trước khi xác nhận.</p>':''}`, detailActions.get(String(id)) || '');
            return;
        }
        if(name==='receipt') {
            const el=document.getElementById('orderActionModal');
            if(el.classList.contains('show')) {
                el.addEventListener('hidden.bs.modal',()=>MilkTeaReceipts.open(id),{once:true});
                bootstrap.Modal.getInstance(el).hide();
            } else await MilkTeaReceipts.open(id);
            return;
        }
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
            modal('Thanh toán '+MilkTeaReceipts.code(id), `<p>Số tiền: <b>${MilkTeaApi.formatVND(o.totalAmount)}</b></p><p>Chọn phương thức để gọi nhân viên. Khoản thu chỉ được ghi nhận sau khi nhân viên xác nhận.</p>`,button('noticeCash',id,'Tiền mặt','success')+button('requestBank',id,'Chuyển khoản','primary'));
            return;
        }
        if(name==='requestBank') {
            try {
                await MilkTeaApi.post(`/api/v1/orders/${id}/payment-notices`,{method:'BANK_TRANSFER',reference:'REQUEST_BANK_'+id});
                const shop=document.getElementById('receiptShop');
                const bank=shop.dataset.bank || 'Vietcombank', account=shop.dataset.account || '', holder=shop.dataset.holder || '';
                const img=account ? `<img alt="QR chuyển khoản" class="d-block mx-auto my-3" width="220" src="https://img.vietqr.io/image/${encodeURIComponent(bank)}-${encodeURIComponent(account)}-compact2.png?amount=${encodeURIComponent(o.totalAmount)}&addInfo=DH${id}&accountName=${encodeURIComponent(holder)}">` : '<p>Liên hệ thu ngân để nhận thông tin chuyển khoản.</p>';
                modal('Chuyển khoản '+MilkTeaReceipts.code(id),`<p>Số tiền: <b>${MilkTeaApi.formatVND(o.totalAmount)}</b></p><p>${e(bank)} · ${e(account)} · ${e(holder)}</p>${img}<p>Đã gọi nhân viên. Sau khi chuyển khoản, chờ nhân viên kiểm tra và xác nhận đã thu tiền.</p>`,button('noticeBank',id,'Tôi đã chuyển khoản','primary'));
                await load();
            } catch(err) {MilkTeaApi.showToast(err.message,'error');}
            return;
        }
        if(busy) return;
        busy=true; document.querySelectorAll('[data-action]').forEach(b=>b.disabled=true);
        try {
            if(name==='cash'||name==='bank') {
                if(!confirm('Xác nhận đã nhận đủ '+MilkTeaApi.formatVND(o.totalAmount)+' bằng '+(name==='cash'?'tiền mặt':'chuyển khoản')+'?')) return;
                await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/payments`,{method:name==='cash'?'CASH':'BANK_TRANSFER',reference:'RECEIPT_'+id});
                await load();
            } else if(name==='sendAssign') await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/assign-table`,{tableId:document.getElementById('assignOrderTable').value});
            else if(name==='confirm') await MilkTeaApi.post(`/api/v1/cashier/orders/${id}/confirm`);
            else if(name==='sendCancel') {
                const reason=document.getElementById('orderCancelReason').value.trim();
                if(!reason) throw new Error('Nhập lý do hủy đơn.');
                await MilkTeaApi.post(`/api/v1/customer/orders/${cancelId}/cancellation-requests`,{reason});
            } else if(name==='approveCancel'||name==='rejectCancel') await MilkTeaApi.post(`/api/v1/cashier/cancellation-requests/${id}/decision`,{approved:name==='approveCancel',reason:'Nhân viên xác nhận'});
            else if(name==='requestRefund') await MilkTeaApi.post(`/api/v1/cashier/cancellation-requests/${id}/refund`);
            else if(name==='noticeCash') await MilkTeaApi.post(`/api/v1/orders/${id}/payment-notices`,{method:'CASH',reference:'REQUEST_CASH_'+id});
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
        document.body.insertAdjacentHTML('beforeend','<div class="modal fade" id="orderActionModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered modal-lg"><div class="modal-content"><div class="modal-header"><h5 id="orderActionTitle"></h5><button class="btn-close" data-bs-dismiss="modal"></button></div><div id="orderActionBody" class="modal-body"></div><div id="orderActionFooter" class="modal-footer"></div></div></div></div>');
        document.getElementById('orderSearch').addEventListener('input',render);document.getElementById('orderStatus').addEventListener('change',render);
        document.addEventListener('click',event=> {const b=event.target.closest('[data-action]');if(b) action(b.dataset.action,b.dataset.id);});
        ['ORDER_CREATED','ORDER_STATUS_CHANGED','PAYMENT_RECORDED','PAYMENT_NOTICE_CREATED','REFUND_CHANGED'].forEach(type=>MilkTeaRealtime.on(type,load));MilkTeaRealtime.onReconnect(load);MilkTeaRealtime.connect();
        await load();
        const focusOrder=new URLSearchParams(location.search).get('order'); if(focusOrder && orders.some(o=>String(o.orderId)===focusOrder)) await action('details',focusOrder);
        const invoice=new URLSearchParams(location.search).get('invoice');if(invoice) { await MilkTeaReceipts.open(invoice);const url=new URL(location.href);url.searchParams.delete('invoice');history.replaceState(null,'',url.pathname+url.search); }
        // HTTP refresh keeps account/counter orders current even without a session topic.
        setInterval(()=>{if(!document.hidden && !busy) load();},15000);
    }
    document.addEventListener('DOMContentLoaded',init);
    return {load,action};
})();
