window.PendingOrders = (() => {
    let loading = false;
    async function refresh() {
        const badge = document.getElementById('pendingOrdersBadge');
        if (!badge || loading) return;
        loading = true;
        try {
            const summary = await MilkTeaApi.get('/api/v1/cashier/orders/work-summary');
            const count = summary ? (summary.pendingActionCount || 0) : 0;
            badge.textContent = count;
            badge.classList.toggle('d-none', count === 0);
            badge.setAttribute('aria-label', count + ' công việc cần xử lý');
        } catch(e) {
            // Non-staff accounts ignore error
        } finally {
            loading = false;
        }
    }
    document.addEventListener('DOMContentLoaded', () => {
        ['ORDER_CREATED', 'ORDER_STATUS_CHANGED', 'PAYMENT_NOTICE_CREATED', 'PAYMENT_RECORDED', 'CANCELLATION_REQUESTED', 'CANCELLATION_DECIDED', 'REFUND_STATUS_CHANGED'].forEach(t => {
            if (window.MilkTeaRealtime && typeof MilkTeaRealtime.on === 'function') {
                MilkTeaRealtime.on(t, refresh);
            }
        });
        if (window.MilkTeaRealtime && typeof MilkTeaRealtime.onReconnect === 'function') {
            MilkTeaRealtime.onReconnect(refresh);
        }
        refresh();
        setInterval(() => { if (!document.hidden) refresh(); }, 8000);
    });
    return { refresh };
})();
