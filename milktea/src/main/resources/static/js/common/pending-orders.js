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
        const events = [
            'ORDER_CREATED',
            'ORDER_STATUS_CHANGED',
            'ORDER_ASSIGNED_TO_TABLE',
            'PAYMENT_NOTICE_CREATED',
            'PAYMENT_RECORDED',
            'CANCELLATION_REQUESTED',
            'CANCELLATION_DECIDED',
            'REFUND_CHANGED',
            'REFUND_STATUS_CHANGED',
            'TABLE_SESSION_OPENED',
            'TABLE_SESSION_CLOSED'
        ];

        events.forEach(t => {
            if (window.MilkTeaRealtime && typeof MilkTeaRealtime.on === 'function') {
                MilkTeaRealtime.on(t, refresh);
            }
        });

        if (window.MilkTeaRealtime && typeof MilkTeaRealtime.onReconnect === 'function') {
            MilkTeaRealtime.onReconnect(refresh);
        }

        if (window.MilkTeaRealtime && typeof MilkTeaRealtime.connect === 'function') {
            MilkTeaRealtime.connect();
        }

        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                refresh();
            }
        });

        refresh();
    });
    return { refresh };
})();
