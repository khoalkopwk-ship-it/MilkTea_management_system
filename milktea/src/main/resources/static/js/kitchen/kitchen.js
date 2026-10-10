/**
 * MilkTea Kitchen KDS & Inventory Logic
 * Tuân thủ frontend_rule.md và workflow_system.md
 */

const KitchenApp = (() => {
    let kitchenOrders = [];
    let bepStock = [];
    let prepRecipes = [];
    let currentIncidentOrderId = null;
    let currentRemakeOrderId = null;
    let issuing = false;
    let issuingKey = null;
    let issuingFingerprint = null;
    let preparing = false;
    let preparationKey = null;
    let preparationFingerprint = null;

    async function init() {
        window.MilkTeaContext = { role: 'KITCHEN' };
        MilkTeaRealtime.connect();

        setupRealtime();
        await loadOrders();
        await loadStock();
        await loadPrepRecipes();
        await loadIssueMaterials();
    }

    function setupRealtime() {
        MilkTeaRealtime.on('ORDER_STATUS_CHANGED', () => {
            loadOrders();
        });

        MilkTeaRealtime.on('STOCK_CHANGED', () => {
            loadStock();
        });

        MilkTeaRealtime.onReconnect(() => {
            loadOrders();
            loadStock();
        });
    }

    async function loadOrders() {
        try {
            const orders = await MilkTeaApi.get('/api/v1/kitchen/orders');
            kitchenOrders = orders || [];
            renderOrdersGrid();
        } catch (e) {
            console.warn('Lỗi nạp đơn bếp:', e);
        }
    }

    function renderOrdersGrid() {
        const grid = document.getElementById('kitchenOrdersGrid');
        if (!grid) return;

        if (kitchenOrders.length === 0) {
            grid.innerHTML = `
                <div class="col-12 py-5 text-center text-muted">
                    <i class="bi bi-cup-straw fs-1 d-block mb-2"></i>
                    <p class="fs-5 fw-semibold mb-0">Hàng đợi trống. Hiện không có đơn nào cần pha chế!</p>
                </div>
            `;
            return;
        }

        let html = '';
        kitchenOrders.forEach(o => {
            const isCooking = o.status === 'DANG_CHE_BIEN';
            const tableLabel = o.tableName ? o.tableName : 'Quầy Mang Về';

            let itemsHtml = '';
            (o.items || []).forEach(it => {
                itemsHtml += `
                    <div class="py-2 border-bottom">
                        <div class="d-flex justify-content-between align-items-center">
                            <strong class="text-dark fs-6">${it.productName}</strong>
                            <span class="badge bg-dark rounded-pill fs-6 px-3">x${it.quantity}</span>
                        </div>
                        <div class="d-flex align-items-center gap-2 mt-1">
                            <span class="badge bg-light text-dark border small">Size ${it.size}</span>
                            ${it.note ? `<span class="badge bg-warning text-dark small"><i class="bi bi-chat-left-text me-1"></i>${it.note}</span>` : ''}
                        </div>
                    </div>
                `;
            });

            html += `
                <div class="col-md-6 col-lg-4">
                    <div class="kds-order-card ${o.status}">
                        <div class="d-flex justify-content-between align-items-center mb-2 pb-2 border-bottom">
                            <div>
                                <h5 class="fw-bold mb-0">Đơn #${o.orderId}</h5>
                                <span class="badge bg-light text-dark border mt-1">${tableLabel}</span>
                            </div>
                            <div class="text-end">
                                <span class="badge-status badge-${o.status} mb-1">${isCooking ? 'Đang Pha' : 'Chờ Pha'}</span>
                                <div class="text-muted small">${new Date(o.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</div>
                            </div>
                        </div>

                        <div class="flex-grow-1 my-2">
                            ${itemsHtml}
                        </div>

                        <div class="pt-3 border-top mt-auto">
                            <div class="d-grid gap-2">
                                ${!isCooking ? `
                                    <button class="btn btn-primary-custom py-2 rounded-pill fw-bold" onclick="KitchenApp.startCooking(${o.orderId})">
                                        <i class="bi bi-play-circle-fill me-1"></i> Bắt Đầu Pha Chế
                                    </button>
                                ` : `
                                    <button class="btn btn-success py-2 rounded-pill fw-bold fs-6" onclick="KitchenApp.completeOrder(${o.orderId})">
                                        <i class="bi bi-check2-all me-1"></i> Hoàn Thành Toàn Đơn
                                    </button>
                                `}

                                <div class="d-flex gap-2">
                                    <button class="btn btn-sm btn-outline-warning text-dark flex-grow-1 rounded-pill" onclick="KitchenApp.openIncidentModal(${o.orderId})">
                                        <i class="bi bi-exclamation-triangle me-1"></i> Sự Cố
                                    </button>
                                    <button class="btn btn-sm btn-outline-secondary flex-grow-1 rounded-pill" onclick="KitchenApp.openRemakeModal(${o.orderId})">
                                        <i class="bi bi-arrow-repeat me-1"></i> Pha Bù
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
        grid.innerHTML = html;
    }

    async function startCooking(orderId) {
        try {
            await MilkTeaApi.post(`/api/v1/kitchen/orders/${orderId}/start`);
            MilkTeaApi.showToast(`Bắt đầu pha chế đơn #${orderId}!`, 'success');
            await loadOrders();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi bắt đầu chế biến', 'error');
        }
    }

    async function completeOrder(orderId) {
        try {
            await MilkTeaApi.post(`/api/v1/kitchen/orders/${orderId}/complete`);
            MilkTeaApi.showToast(`Đơn #${orderId} đã hoàn thành và trừ tồn kho Bếp!`, 'success');
            await loadOrders();
            await loadStock();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Không thể hoàn thành đơn (Có thể do thiếu nguyên liệu Bếp)', 'error');
        }
    }

    async function loadIssueMaterials() {
        try {
            const stocks=await MilkTeaApi.get('/api/v1/inventory/stocks?location=KHO');
            const select=document.getElementById('kitchenIssueMaterial');
            const selected=select.value;
            select.replaceChildren();
            const placeholder=document.createElement('option');placeholder.value='';placeholder.textContent='Chọn nguyên liệu';select.append(placeholder);
            (stocks||[]).forEach(m=>{const option=document.createElement('option');option.value=m.materialId;option.textContent=`${m.materialName} (${m.unit}, kho còn ${m.quantity})`;select.append(option);});
            select.value=selected;
            document.getElementById('kitchenIssueDate').value=new Date().toLocaleDateString('vi-VN');
        }catch(e){MilkTeaApi.showToast(e.message,'error');}
    }

    async function submitIssue() {
        if (issuing) return;

        const form = document.getElementById('kitchenIssueForm');
        if (!form.reportValidity()) return;

        const materialId = Number(
            document.getElementById('kitchenIssueMaterial').value
        );

        const quantity = Number(
            document.getElementById('kitchenIssueQty').value
        );

        if (!Number.isInteger(materialId) || materialId <= 0 ||
            !Number.isFinite(quantity) || quantity <= 0) {
            MilkTeaApi.showToast(
                'Vui lòng chọn nguyên liệu và nhập số lượng hợp lệ',
                'warning'
            );
            return;
        }

        issuing = true;

        const submitBtn = document.getElementById('kitchenIssueSubmit');
        submitBtn.disabled = true;

        try {
            const payload = {
                reason: 'Bếp lấy nguyên liệu',
                items: [{
                    materialId: materialId,
                    quantity: quantity
                }]
            };

            const fingerprint = JSON.stringify(payload);

            if (issuingFingerprint !== fingerprint || !issuingKey) {
                issuingFingerprint = fingerprint;
                issuingKey = crypto.randomUUID();
            }

            await MilkTeaApi.post(
                '/api/v1/inventory/issues',
                payload,
                { 'Idempotency-Key': issuingKey }
            );

            // Chỉ xóa mã khi server xác nhận thành công
            issuingKey = null;
            issuingFingerprint = null;

            document.getElementById('kitchenIssueResult').textContent =
                'Đã ghi phiếu, chuyển nguyên liệu sang bếp và thông báo Admin.';

            document.getElementById('kitchenIssueQty').value = '';

            // Cập nhật tồn Bếp và danh sách nguyên liệu Kho
            await Promise.all([
                loadStock(),
                loadIssueMaterials()
            ]);

        } catch (e) {
            MilkTeaApi.showToast(
                e.message || 'Không thể lập phiếu lấy nguyên liệu',
                'error'
            );
        } finally {
            issuing = false;
            submitBtn.disabled = false;
        }
    }

    // Modal Sự Cố
    function openIncidentModal(orderId) {
        currentIncidentOrderId = orderId;
        document.getElementById('incidentReason').value = '';
        const modal = new bootstrap.Modal(document.getElementById('incidentModal'));
        modal.show();
    }

    async function submitIncident() {
        if (!currentIncidentOrderId) return;
        const reason = document.getElementById('incidentReason').value.trim();
        if (!reason) {
            MilkTeaApi.showToast('Vui lòng nhập lý do sự cố', 'warning');
            return;
        }

        try {
            await MilkTeaApi.post(`/api/v1/kitchen/orders/${currentIncidentOrderId}/incidents`, { reason });
            MilkTeaApi.showToast('Đã ghi nhận sự cố đơn hàng', 'success');
            const modalEl = document.getElementById('incidentModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi ghi sự cố', 'error');
        }
    }

    // Modal Pha Bù (Remake)
    function openRemakeModal(orderId) {
        currentRemakeOrderId = orderId;
        document.getElementById('remakeReason').value = '';
        const select = document.getElementById('remakeMaterialSelect');
        select.innerHTML = '';
        bepStock.forEach(s => {
            select.innerHTML += `<option value="${s.materialId}">${s.materialName} (Tồn Bếp: ${s.quantity} ${s.unit})</option>`;
        });
        const modal = new bootstrap.Modal(document.getElementById('remakeModal'));
        modal.show();
    }

    async function submitRemake() {
        if (!currentRemakeOrderId) return;
        const matId = document.getElementById('remakeMaterialSelect').value;
        const qty = parseFloat(document.getElementById('remakeQuantity').value) || 1;
        const reason = document.getElementById('remakeReason').value.trim() || 'Pha lại do hỏng';

        try {
            await MilkTeaApi.post(`/api/v1/kitchen/orders/${currentRemakeOrderId}/consumption`, {
                reason: reason,
                items: [{ materialId: parseInt(matId), quantity: qty }]
            });
            MilkTeaApi.showToast('Đã ghi nhận tiêu hao pha bù vào kho Bếp!', 'success');
            const modalEl = document.getElementById('remakeModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            await loadStock();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi ghi tiêu hao pha bù', 'error');
        }
    }

    // Tab Tồn Kho Bếp & Sơ Chế
    async function loadStock() {
        try {
            const stock = await MilkTeaApi.get(
                '/api/v1/inventory/stocks?location=BEP'
            );
            bepStock = stock || [];
            renderStockTable();
        } catch (e) {
            console.warn('Lỗi nạp tồn kho:', e);
        }
    }

    
    function renderStockTable() {
        const tbody = document.getElementById('bepStockTableBody');
        if (!tbody) return;

        let html = '';

        bepStock.forEach(s => {
            const isAlert = s.lowStock;

            html += `
                <tr class="${isAlert ? 'table-warning' : ''}">
                    <td class="fw-semibold">${s.materialName}</td>
                    <td>
                        <span class="badge ${s.type === 'THO' ? 'bg-secondary' : 'bg-primary'}">
                            ${s.type}
                        </span>
                    </td>
                    <td class="fw-bold">${s.quantity} ${s.unit}</td>
                    <td class="text-muted">${s.threshold} ${s.unit}</td>
                    <td>
                        ${isAlert
                            ? '<span class="badge bg-danger">Sắp hết hàng!</span>'
                            : '<span class="badge bg-success">Đủ dùng</span>'}
                    </td>
                </tr>
            `;
        });

        tbody.innerHTML = html || `
            <tr>
                <td colspan="5" class="text-center text-muted">
                    Chưa có dữ liệu tồn kho tại Bếp
                </td>
            </tr>
        `;
    }


    async function loadPrepRecipes() {
        try {
            const recipes = await MilkTeaApi.get(
                '/api/v1/inventory/preparation-recipes'
            );
            prepRecipes = recipes || [];
            const select = document.getElementById('prepRecipeSelect');
            if (select) {
                select.innerHTML = '<option value="">-- Chọn công thức sơ chế --</option>';
                prepRecipes.forEach(r => {
                    select.innerHTML += `<option value="${r.id}">${r.name} (Sản lượng chuẩn: ${r.standardOutputQuantity} ${r.outputUnit})</option>`;
                });
            }
        } catch (e) {
            console.warn('Lỗi nạp công thức sơ chế:', e);
        }
    }

    
    async function submitPreparation() {
        if (preparing) return;

        const recipeId = document.getElementById('prepRecipeSelect').value;
        const actualQty = Number(document.getElementById('prepActualQty').value);
        const reason = document.getElementById('prepReason').value.trim();

        if (!recipeId || !Number.isFinite(actualQty) || actualQty <= 0) {
            MilkTeaApi.showToast(
                'Vui lòng chọn công thức và nhập sản lượng hợp lệ',
                'warning'
            );
            return;
        }

        const payload = {
            recipeId: Number(recipeId),
            actualQuantity: actualQty,
            discrepancyReason: reason
        };

        const fingerprint = JSON.stringify(payload);

        // Cùng một thao tác sẽ giữ nguyên mã khi thử gửi lại.
        if (!preparationKey || preparationFingerprint !== fingerprint) {
            preparationKey = crypto.randomUUID();
            preparationFingerprint = fingerprint;
        }

        preparing = true;

        const button = document.querySelector(
            '[onclick="KitchenApp.submitPreparation()"]'
        );

        if (button) button.disabled = true;

        try {
            await MilkTeaApi.post(
                '/api/v1/inventory/preparation-batches',
                payload,
                { 'Idempotency-Key': preparationKey }
            );

            MilkTeaApi.showToast(
                'Đã ghi nhận mẻ sơ chế thành công!',
                'success'
            );

            preparationKey = null;
            preparationFingerprint = null;

            document.getElementById('prepActualQty').value = '';
            document.getElementById('prepReason').value = '';

            await loadStock();

        } catch (e) {
            MilkTeaApi.showToast(
                e.message || 'Không thể ghi nhận mẻ sơ chế',
                'error'
            );
        } finally {
            preparing = false;
            if (button) button.disabled = false;
        }
    }

    return {
        submitIssue,
        init,
        startCooking,
        completeOrder,
        openIncidentModal,
        submitIncident,
        openRemakeModal,
        submitRemake,
        submitPreparation
    };
})();
