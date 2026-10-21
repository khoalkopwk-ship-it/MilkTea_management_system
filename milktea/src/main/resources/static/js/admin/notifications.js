document.addEventListener('DOMContentLoaded', () => {
    window.MilkTeaContext = {...(window.MilkTeaContext || {}), role:'ADMIN'};
    
    MilkTeaRealtime.on('REFUND_CHANGED', event => {
        if (event.payload?.status === 'CHO_DUYET') MilkTeaApi.showToast('Có yêu cầu hoàn tiền mới. Xem tại Phiếu nghiệp vụ.', 'success');
    });
    MilkTeaRealtime.on('CANCELLATION_REQUESTED', event => {
        MilkTeaApi.showToast('Có yêu cầu hủy đơn mới chờ duyệt.', 'warning');
    });
    MilkTeaRealtime.connect();
});
