document.addEventListener('DOMContentLoaded', () => {
    window.MilkTeaContext = {...(window.MilkTeaContext || {}), role:'ADMIN'};
    MilkTeaRealtime.on('STOCK_CHANGED', event => {
        if (event.payload?.action === 'ISSUE') MilkTeaApi.showToast('Bếp đã ghi phiếu lấy nguyên liệu. Xem tại Phiếu nghiệp vụ.', 'success');
    });
    MilkTeaRealtime.on('REFUND_CHANGED', event => {
        if (event.payload?.status === 'CHO_DUYET') MilkTeaApi.showToast('Có yêu cầu hoàn tiền mới. Xem tại Phiếu nghiệp vụ.', 'success');
    });
    MilkTeaRealtime.connect();
});
