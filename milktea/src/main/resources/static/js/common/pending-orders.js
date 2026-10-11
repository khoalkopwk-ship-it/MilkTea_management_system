window.PendingOrders = (() => {
    let loading=false;
    async function refresh() {
        const badge=document.getElementById('pendingOrdersBadge');
        if(!badge || loading) return;
        loading=true;
        try {
            const orders=await MilkTeaApi.get('/api/v1/cashier/orders?status=CHO_XAC_NHAN');
            const count=(orders || []).filter(o=>!o.staffCreated && !o.cancellationPending).length;
            badge.textContent=count; badge.classList.toggle('d-none',count===0);
            badge.setAttribute('aria-label',count+' đơn cần xác nhận');
        } catch(e) { console.warn('Chưa tải được số đơn chờ xác nhận'); }
        finally {loading=false;}
    }
    document.addEventListener('DOMContentLoaded',()=> {
        ['ORDER_CREATED','ORDER_STATUS_CHANGED'].forEach(t=>MilkTeaRealtime.on(t,refresh));
        MilkTeaRealtime.onReconnect(refresh); refresh();
        setInterval(()=>{if(!document.hidden)refresh();},10000);
    });
    return {refresh};
})();
