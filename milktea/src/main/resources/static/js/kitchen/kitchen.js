/**
 * MilkTea Kitchen KDS & Inventory Logic
 * Tuân thủ frontend_rule.md, workflow_system.md và các yêu cầu định mức sơ chế cố định
 */

const KitchenApp = (() => {
    let kitchenOrders = [];
    let bepStock = [];
    let prepRecipes = [];
    let preparationHistory = [];
    let shiftSettlementRows = [];
    let preparing = false;
    let preparationKey = null;

    // Filters
    let prepFilter = { from: null, to: null, all: false };
    let settlementFilter = { from: null, to: null, all: false };

    async function init() {
        window.MilkTeaContext = { role: 'KITCHEN' };
        MilkTeaRealtime.connect();

        setupRealtime();
        setupHeaderTabs();
        initDateFilters();

        await loadOrders();
        await loadStock();
        await loadPrepRecipes();
        await loadPreparationHistory();
        await loadPastSettlements();
    }

    function initDateFilters() {
        const todayStr = getLocalDateString();
        const pFrom = document.getElementById('prepFilterFrom');
        const pTo = document.getElementById('prepFilterTo');
        if (pFrom && !pFrom.value) pFrom.value = todayStr;
        if (pTo && !pTo.value) pTo.value = todayStr;

        const sFrom = document.getElementById('settlementFilterFrom');
        const sTo = document.getElementById('settlementFilterTo');
        if (sFrom && !sFrom.value) sFrom.value = todayStr;
        if (sTo && !sTo.value) sTo.value = todayStr;

        prepFilter = { from: todayStr, to: todayStr, all: false };
        settlementFilter = { from: todayStr, to: todayStr, all: false };
    }

    function getLocalDateString() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function setupHeaderTabs() {
        function activateTab(targetId) {
            if (!targetId) targetId = '#tabKds';
            const targetEl = document.querySelector(targetId);
            if (!targetEl) return;
            document.querySelectorAll('.tab-content > .tab-pane').forEach(el => {
                el.classList.remove('show', 'active');
            });
            targetEl.classList.add('show', 'active');
            document.querySelectorAll('.kitchen-header-link').forEach(link => {
                const linkTarget = link.getAttribute('data-target') || (link.getAttribute('href') ? '#' + link.getAttribute('href').split('#')[1] : '');
                link.classList.toggle('active', linkTarget === targetId);
            });
        }

        document.querySelectorAll('.kitchen-header-link').forEach(link => {
            link.addEventListener('click', (e) => {
                if (window.location.pathname.startsWith('/kitchen')) {
                    e.preventDefault();
                    const target = link.getAttribute('data-target') || (link.getAttribute('href') ? '#' + link.getAttribute('href').split('#')[1] : '#tabKds');
                    history.pushState(null, '', target);
                    activateTab(target);
                }
            });
        });

        if (window.location.hash) {
            activateTab(window.location.hash);
        }

        window.addEventListener('hashchange', () => {
            if (window.location.hash) {
                activateTab(window.location.hash);
            }
        });
    }

    function setupRealtime() {
        MilkTeaRealtime.on('ORDER_CREATED', () => {
            loadOrders();
        });

        MilkTeaRealtime.on('ORDER_STATUS_CHANGED', () => {
            loadOrders();
        });

        MilkTeaRealtime.on('STOCK_CHANGED', () => {
            loadStock();
            loadPreparationHistory();
            loadPastSettlements();
        });

        MilkTeaRealtime.on('BATCH_RECORDED', () => {
            loadStock();
            loadPreparationHistory();
        });

        MilkTeaRealtime.on('RECIPE_CHANGED', () => {
            loadStock();
        });

        MilkTeaRealtime.on('SETTINGS_CHANGED', async () => {
            try {
                const info = await MilkTeaApi.get('/api/v1/public/shop-info');
                if (info && info.logoUrl) {
                    document.querySelectorAll('.navbar-milktea .navbar-brand').forEach(brand => {
                        let img = brand.querySelector('img.navbar-shop-logo');
                        const badge = brand.querySelector('.navbar-brand-badge');
                        if (!img) {
                            img = document.createElement('img');
                            img.alt = 'Logo';
                            img.className = 'rounded-circle shadow-sm navbar-shop-logo';
                            img.style = 'width: 38px; height: 38px; object-fit: cover; border: 1px solid rgba(0,0,0,0.08);';
                            brand.prepend(img);
                        }
                        img.src = info.logoUrl;
                        img.style.display = 'inline-block';
                        if (badge) badge.style.setProperty('display', 'none', 'important');
                    });
                }
                if (info && info.shopName) {
                    document.querySelectorAll('.navbar-shop-name').forEach(el => el.textContent = info.shopName);
                }
            } catch (e) {
                console.debug('Error refreshing shop info on SETTINGS_CHANGED:', e);
            }
        });

        MilkTeaRealtime.onReconnect(() => {
            loadOrders();
            loadStock();
            loadPreparationHistory();
            loadPastSettlements();
        });

        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                loadOrders();
                loadStock();
            }
        });
    }

    // ==========================================
    // 1. HÀNG ĐỢI CHẾ BIẾN CỦA BẾP (KDS)
    // ==========================================

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
            const tableLabel = o.tableName ? o.tableName : (o.source === 'POS' ? 'Quầy Mang Về' : 'Đặt Tại Bàn');

            let itemsHtml = '';
            (o.items || []).forEach(it => {
                let toppingsHtml = '';
                if (it.toppings && it.toppings.length > 0) {
                    toppingsHtml = `<div class="small text-muted ps-1 mt-1">` +
                        it.toppings.map(t => `<span class="badge bg-white text-secondary border me-1"><i class="bi bi-plus-circle me-1 text-primary"></i>${escapeHtml(t.name)}</span>`).join('') +
                        `</div>`;
                }

                itemsHtml += `
                    <div class="py-2 border-bottom">
                        <div class="d-flex justify-content-between align-items-center">
                            <strong class="text-dark fs-6">${escapeHtml(it.productName)}</strong>
                            <span class="badge bg-dark rounded-pill fs-6 px-3">x${it.quantity}</span>
                        </div>
                        <div class="d-flex flex-wrap align-items-center gap-2 mt-1">
                            <span class="badge bg-light text-dark border small">Size ${escapeHtml(it.size)}</span>
                            ${it.note ? `<span class="badge bg-warning text-dark small"><i class="bi bi-chat-left-text me-1"></i>${escapeHtml(it.note)}</span>` : ''}
                        </div>
                        ${toppingsHtml}
                    </div>
                `;
            });

            html += `
                <div class="col-md-6 col-lg-4">
                    <div class="kds-order-card ${o.status}">
                        <div class="d-flex justify-content-between align-items-center mb-2 pb-2 border-bottom">
                            <div>
                                <h5 class="fw-bold mb-0 text-primary">Đơn #${o.orderId}</h5>
                                <span class="badge bg-light text-dark border mt-1">${escapeHtml(tableLabel)}</span>
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
                                    <button class="btn btn-sm btn-outline-info text-dark flex-grow-1 rounded-pill" onclick="KitchenApp.viewOrderDetail(${o.orderId})">
                                        <i class="bi bi-eye me-1"></i> Chi Tiết
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

    // Xem Chi Tiết Đơn Hàng (Modal Chỉ Đọc)
    function viewOrderDetail(orderId) {
        const order = kitchenOrders.find(o => o.orderId === Number(orderId));
        if (!order) {
            MilkTeaApi.showToast('Không tìm thấy thông tin đơn #' + orderId, 'warning');
            return;
        }

        document.getElementById('orderDetailTitle').innerHTML = `<i class="bi bi-receipt me-1 text-primary"></i> Chi Tiết Đơn Hàng #${order.orderId}`;
        document.getElementById('orderDetailSubtitle').textContent = order.source ? `Nguồn: ${order.source}` : '';
        document.getElementById('orderDetailCode').textContent = `#${order.orderId}`;
        document.getElementById('orderDetailTable').textContent = order.tableName || (order.source === 'POS' ? 'Quầy Mang Về' : 'Đặt Tại Bàn');

        const statusEl = document.getElementById('orderDetailStatus');
        const isCooking = order.status === 'DANG_CHE_BIEN';
        statusEl.className = `badge ${isCooking ? 'bg-primary' : 'bg-warning text-dark'}`;
        statusEl.textContent = isCooking ? 'Đang Pha Chế' : 'Chờ Pha Chế';

        document.getElementById('orderDetailTime').textContent = formatDateTime(order.createdAt);

        const tbody = document.getElementById('orderDetailItemsBody');
        tbody.innerHTML = (order.items || []).map(it => {
            let toppingsStr = '';
            if (it.toppings && it.toppings.length > 0) {
                toppingsStr = `<div class="mt-1 small text-muted">` +
                    it.toppings.map(t => `<span class="badge bg-light text-dark border me-1"><i class="bi bi-plus me-1 text-primary"></i>${escapeHtml(t.name)}</span>`).join('') +
                    `</div>`;
            }
            return `
                <tr>
                    <td>
                        <strong class="text-dark">${escapeHtml(it.productName)}</strong>
                        ${toppingsStr}
                    </td>
                    <td class="text-center"><span class="badge bg-light text-dark border">${escapeHtml(it.size)}</span></td>
                    <td class="text-center fw-bold fs-6">x${it.quantity}</td>
                    <td class="small text-muted">${escapeHtml(it.note || '-')}</td>
                </tr>
            `;
        }).join('');

        const modal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
        modal.show();
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

    
    // ==========================================
    // 2. TỒN KHO BẾP & SƠ CHẾ THEO ĐỊNH MỨC CỐ ĐỊNH
    // ==========================================

    async function loadStock() {
        try {
            const stock = await MilkTeaApi.get('/api/v1/inventory/stocks?location=BEP');
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
        bepStock
            .filter(s => s.type === 'SOCHE')
            .forEach(s => {
                const isAlert = s.lowStock;
                html += `
                    <tr class="${isAlert ? 'table-warning' : ''}">
                        <td class="fw-semibold">${escapeHtml(s.materialName)}</td>
                        <td>
                            <span class="badge ${s.type === 'THO' ? 'bg-secondary' : 'bg-primary'}">
                                ${s.type}
                            </span>
                        </td>
                        <td class="fw-bold">${formatQty(s.quantity)} ${escapeHtml(s.unit)}</td>
                        <td class="text-muted">${formatQty(s.threshold)} ${escapeHtml(s.unit)}</td>
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
                    Chưa có dữ liệu sơ chế tại Bếp
                </td>
            </tr>
        `;
    }

    async function loadPrepRecipes() {
        try {
            const recipes = await MilkTeaApi.get('/api/v1/inventory/preparation-recipes');
            // Chỉ lấy các định mức đang áp dụng (active === true)
            prepRecipes = (recipes || []).filter(r => r.active !== false);

            const select = document.getElementById('prepOutputMaterialSelect');
            if (select) {
                select.innerHTML = '<option value="">-- Chọn thành phẩm cần sơ chế --</option>';
                prepRecipes.forEach(r => {
                    const ratioDesc = r.ratioText ? ` (${r.ratioText})` : ` (${formatQty(r.standardOutputQuantity)} ${r.outputUnit || ''})`;
                    select.innerHTML += `<option value="${r.outputMaterialId}">${escapeHtml(r.outputMaterialName)}${escapeHtml(ratioDesc)}</option>`;
                });
            }
        } catch (e) {
            console.warn('Lỗi nạp công thức sơ chế:', e);
        }
    }

    function onPrepOutputMaterialChange() {
        const select = document.getElementById('prepOutputMaterialSelect');
        const matId = Number(select?.value);
        const recipe = prepRecipes.find(r => r.outputMaterialId === matId);

        const infoEl = document.getElementById('prepRecipeInfo');
        const unitEl = document.getElementById('prepOutputUnit');
        const desiredInput = document.getElementById('prepDesiredQty');

        if (!recipe) {
            if (infoEl) infoEl.classList.add('d-none');
            if (unitEl) unitEl.textContent = 'ml';
            updateNeededRawMaterials();
            return;
        }

        if (unitEl) unitEl.textContent = recipe.outputUnit || 'ml';

        if (infoEl) {
            const unitCostText = (recipe.estimatedUnitCost != null && Number(recipe.estimatedUnitCost) > 0)
                ? `${Number(recipe.estimatedUnitCost).toLocaleString('vi-VN')} ₫/${escapeHtml(recipe.outputUnit || '')}`
                : 'Chưa có giá vốn';

            infoEl.innerHTML = `
                <div class="fw-semibold text-success mb-1"><i class="bi bi-check-circle-fill me-1"></i>Định mức đang áp dụng (v${recipe.version || 1}):</div>
                <div class="small text-muted">
                    Thành phẩm: <strong>${escapeHtml(recipe.outputMaterialName)}</strong><br>
                    Tỉ lệ chuẩn: <span class="badge bg-light text-primary border fw-bold">${escapeHtml(recipe.ratioText || '-')}</span><br>
                    Sản lượng chuẩn: <strong>${formatQty(recipe.standardOutputQuantity)} ${escapeHtml(recipe.outputUnit || '')}</strong><br>
                    Giá vốn ước tính: <strong>${unitCostText}</strong>
                    ${recipe.note ? `<br>Ghi chú định mức: <em>${escapeHtml(recipe.note)}</em>` : ''}
                </div>
            `;
            infoEl.classList.remove('d-none');
        }

        updateNeededRawMaterials();
    }

    function onDesiredQtyChange() {
        updateNeededRawMaterials();
    }

    function updateNeededRawMaterials() {
        const select = document.getElementById('prepOutputMaterialSelect');
        const matId = Number(select?.value);
        const recipe = prepRecipes.find(r => r.outputMaterialId === matId);

        const container = document.getElementById('prepNeededRawMaterialsContainer');
        const contentEl = document.getElementById('prepNeededRawMaterials');
        const desiredQty = parsePositiveNumber(document.getElementById('prepDesiredQty')?.value);

        if (!recipe || !desiredQty || desiredQty <= 0) {
            if (container) container.classList.add('d-none');
            return;
        }

        const stdOutput = Number(recipe.standardOutputQuantity) || 1;
        const factor = desiredQty / stdOutput;

        let rowsHtml = '';
        let totalEstCost = 0;

        (recipe.inputItems || []).forEach(item => {
            const stdQty = Number(item.standardQuantity) || 0;
            const neededQty = stdQty * factor;
            rowsHtml += `
                <div class="d-flex justify-content-between align-items-center py-1 border-bottom">
                    <div>
                        <strong class="text-dark">${escapeHtml(item.inputMaterialName)}</strong>
                        <div class="text-muted" style="font-size: 0.8rem;">
                            Định mức chuẩn: ${formatQty(stdQty)} ${escapeHtml(item.unit)}
                        </div>
                    </div>
                    <div class="text-end">
                        <span class="badge bg-primary fs-6 px-3 py-1">
                            ${formatQty(neededQty)} ${escapeHtml(item.unit)}
                        </span>
                    </div>
                </div>
            `;
        });

        if (contentEl) {
            contentEl.innerHTML = `
                <div class="mb-2 text-muted fst-italic">
                    Hệ số mẻ = ${desiredQty} / ${stdOutput} = ${factor.toFixed(4)}
                </div>
                ${rowsHtml}
            `;
        }
        if (container) container.classList.remove('d-none');
    }

    async function submitPreparation() {
        if (preparing) return;

        const select = document.getElementById('prepOutputMaterialSelect');
        const matId = Number(select?.value);
        const recipe = prepRecipes.find(r => r.outputMaterialId === matId);
        const desiredQty = parsePositiveNumber(document.getElementById('prepDesiredQty')?.value);
        const note = document.getElementById('prepNote')?.value.trim();

        if (!recipe || !matId || desiredQty == null || desiredQty <= 0) {
            MilkTeaApi.showToast('Vui lòng chọn thành phẩm và nhập sản lượng mong muốn (> 0)', 'warning');
            return;
        }

        const payload = {
            outputMaterialId: matId,
            recipeVersion: recipe.version || 1,
            desiredQuantity: desiredQty,
            note: note || undefined
        };

        const button = document.getElementById('btnSubmitPrep');
        if (button) button.disabled = true;
        preparing = true;

        const idempotencyKey = crypto.randomUUID();

        try {
            await MilkTeaApi.post(
                '/api/v1/inventory/preparation-batches',
                payload,
                { 'Idempotency-Key': idempotencyKey }
            );

            MilkTeaApi.showToast('Đã ghi nhận mẻ sơ chế thành công theo định mức!', 'success');

            document.getElementById('prepDesiredQty').value = '';
            document.getElementById('prepNote').value = '';
            const container = document.getElementById('prepNeededRawMaterialsContainer');
            if (container) container.classList.add('d-none');

            await loadStock();
            await loadPreparationHistory();

        } catch (e) {
            if (e.status === 409 || (e.message && e.message.includes('phiên bản mới'))) {
                MilkTeaApi.showToast('Định mức sơ chế đã thay đổi phiên bản. Đang tải lại thông tin mới nhất...', 'warning');
                await loadPrepRecipes();
                onPrepOutputMaterialChange();
            } else {
                MilkTeaApi.showToast(e.message || 'Không thể ghi nhận mẻ sơ chế', 'error');
            }
        } finally {
            preparing = false;
            if (button) button.disabled = false;
        }
    }

    // ==========================================
    // 3. LỊCH SỬ SƠ CHẾ (9 CỘT THEO YÊU CẦU)
    // ==========================================

    function filterPreparationHistory(isAll) {
        if (isAll) {
            prepFilter = { all: true, from: null, to: null };
        } else {
            const f = document.getElementById('prepFilterFrom')?.value;
            const t = document.getElementById('prepFilterTo')?.value;
            prepFilter = { all: false, from: f || null, to: t || null };
        }
        loadPreparationHistory();
    }

    async function loadPreparationHistory() {
        const tbody = document.getElementById('preparationHistoryTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center text-muted py-4">
                        <span class="spinner-border spinner-border-sm me-2"></span>Đang nạp lịch sử sơ chế...
                    </td>
                </tr>
            `;
        }

        let url = '/api/v1/inventory/preparation-batches';
        if (prepFilter.all) {
            url += '?all=true';
        } else if (prepFilter.from || prepFilter.to) {
            const params = new URLSearchParams();
            if (prepFilter.from) params.append('from', prepFilter.from);
            if (prepFilter.to) params.append('to', prepFilter.to);
            url += '?' + params.toString();
        }

        try {
            const batches = await MilkTeaApi.get(url);
            preparationHistory = batches || [];
            renderPreparationHistory();
        } catch (e) {
            console.warn('Lỗi nạp lịch sử sơ chế:', e);
            if (tbody) {
                tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4">Không thể nạp lịch sử sơ chế</td></tr>`;
            }
        }
    }

    function renderPreparationHistory() {
        const tbody = document.getElementById('preparationHistoryTableBody');
        if (!tbody) return;

        if (preparationHistory.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-4">Chưa có mẻ sơ chế nào trong khoảng thời gian này</td></tr>`;
            return;
        }

        tbody.innerHTML = preparationHistory.map(batch => {
            let inputItemsSummary = '-';
            if (batch.items && batch.items.length > 0) {
                inputItemsSummary = batch.items.map(it =>
                    `<span class="d-block text-nowrap"><strong>${escapeHtml(it.materialName)}:</strong> ${formatQty(it.actualQuantity)} ${escapeHtml(it.unit)}</span>`
                ).join('');
            }

            const unitCostFormatted = batch.unitCost != null && Number(batch.unitCost) > 0
                ? `${Number(batch.unitCost).toLocaleString('vi-VN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} ₫/${escapeHtml(batch.unit || '')}`
                : '-';

            const totalCostFormatted = batch.totalCost != null && Number(batch.totalCost) > 0
                ? `${Number(batch.totalCost).toLocaleString('vi-VN')} ₫`
                : '-';

            return `
                <tr>
                    <td class="text-nowrap small">${formatDateTime(batch.postedAt)}</td>
                    <td class="fw-bold text-primary">#${batch.id}</td>
                    <td class="fw-semibold">${escapeHtml(batch.outputMaterialName || '-')}</td>
                    <td class="small">${inputItemsSummary}</td>
                    <td class="fw-bold fs-6 text-success">${formatQty(batch.actualQuantity)} ${escapeHtml(batch.unit || '')}</td>
                    <td class="small fw-semibold text-primary">${unitCostFormatted}</td>
                    <td class="small fw-bold text-dark">${totalCostFormatted}</td>
                    <td class="small">${escapeHtml(batch.createdByName || batch.createdByEmail || '-')}</td>
                    <td class="small text-muted">${escapeHtml(batch.discrepancyReason || '-')}</td>
                </tr>
            `;
        }).join('');
    }

    // =========================================================================
    // 4. KIỂM KÊ & TÍNH HAO HỤT CUỐI CA
    // =========================================================================

    async function loadShiftSettlementPreview() {
        const tbody = document.getElementById('shiftSettlementTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4"><span class="spinner-border spinner-border-sm me-2"></span>Đang nạp dữ liệu tồn sổ & giá vốn bình quân...</td></tr>';
        }
        try {
            const res = await MilkTeaApi.get('/api/v1/inventory/shift-settlement/preview?location=BEP');
            shiftSettlementRows = (res?.items || []).map(it => ({
                materialId: it.materialId,
                materialName: it.materialName,
                unit: it.unit,
                bookQuantity: Number(it.bookQuantity || 0),
                actualQuantity: Number(it.bookQuantity || 0),
                discardedQuantity: 0,
                retainedQuantity: Number(it.bookQuantity || 0),
                unitCost: Number(it.estimatedUnitCost || 0),
                lossCost: 0,
                reason: ''
            }));
            renderShiftSettlementTable();
            await loadPastSettlements();
        } catch (e) {
            if (tbody) {
                tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4">Lỗi nạp dữ liệu kiểm kê: ${escapeHtml(e.message)}</td></tr>`;
            }
        }
    }

    function renderShiftSettlementTable() {
        const tbody = document.getElementById('shiftSettlementTableBody');
        if (!tbody) return;

        if (shiftSettlementRows.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">Không có nguyên liệu sơ chế nào tại Bếp</td></tr>';
            return;
        }

        tbody.innerHTML = shiftSettlementRows.map((r, i) => {
            const costFormatted = r.unitCost > 0
                ? `${r.unitCost.toLocaleString('vi-VN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} ₫/${escapeHtml(r.unit)}`
                : '<span class="text-muted small">Chưa có mẻ</span>';
            const lossFormatted = `${r.lossCost.toLocaleString('vi-VN')} ₫`;

            return `
                <tr>
                    <td class="fw-semibold">${escapeHtml(r.materialName)}</td>
                    <td class="text-muted">${escapeHtml(r.unit)}</td>
                    <td class="fw-bold">${formatQty(r.bookQuantity)}</td>
                    <td>
                        <input type="number" step="0.001" min="0" class="form-control form-control-sm"
                               value="${r.actualQuantity}" oninput="KitchenApp.onShiftRowInput(${i}, 'actual', this.value)">
                    </td>
                    <td>
                        <input type="number" step="0.001" min="0" max="${r.bookQuantity}" class="form-control form-control-sm text-danger fw-semibold"
                               value="${r.discardedQuantity}" oninput="KitchenApp.onShiftRowInput(${i}, 'discarded', this.value)">
                    </td>
                    <td class="text-primary fw-semibold" id="retained-val-${i}">
                        ${formatQty(r.retainedQuantity)}
                    </td>
                    <td class="small">${costFormatted}</td>
                    <td class="fw-bold text-danger" id="loss-cost-${i}">${lossFormatted}</td>
                    <td>
                        <input type="text" class="form-control form-control-sm" placeholder="Hết hạn, hỏng..."
                               value="${escapeHtml(r.reason)}" oninput="KitchenApp.onShiftRowInput(${i}, 'reason', this.value)">
                    </td>
                </tr>
            `;
        }).join('');

        updateTotalLossDisplay();
    }

    function onShiftRowInput(index, field, value) {
        const row = shiftSettlementRows[index];
        if (!row) return;

        if (field === 'actual') {
            row.actualQuantity = parseFloat(value) || 0;
            row.retainedQuantity = Math.max(0, row.actualQuantity - row.discardedQuantity);
        } else if (field === 'discarded') {
            row.discardedQuantity = parseFloat(value) || 0;
            row.retainedQuantity = Math.max(0, row.actualQuantity - row.discardedQuantity);
            row.lossCost = Math.round(row.discardedQuantity * row.unitCost);
        } else if (field === 'reason') {
            row.reason = value;
        }

        const retainedEl = document.getElementById(`retained-val-${index}`);
        if (retainedEl) retainedEl.textContent = formatQty(row.retainedQuantity);

        const lossEl = document.getElementById(`loss-cost-${index}`);
        if (lossEl) lossEl.textContent = `${row.lossCost.toLocaleString('vi-VN')} ₫`;

        updateTotalLossDisplay();
    }

    function updateTotalLossDisplay() {
        const total = shiftSettlementRows.reduce((sum, r) => sum + (r.lossCost || 0), 0);
        const totalEl = document.getElementById('shiftSettlementTotalCost');
        if (totalEl) totalEl.textContent = `${total.toLocaleString('vi-VN')} ₫`;
    }

    async function submitShiftSettlement() {
        if (shiftSettlementRows.length === 0) {
            MilkTeaApi.showToast('Vui lòng nạp dữ liệu kiểm kê trước khi chốt', 'warning');
            return;
        }

        const note = document.getElementById('shiftSettlementNote').value.trim();
        const payload = {
            location: 'BEP',
            note: note,
            items: shiftSettlementRows.map(r => ({
                materialId: r.materialId,
                actualQuantity: r.actualQuantity,
                discardedQuantity: r.discardedQuantity,
                retainedQuantity: r.retainedQuantity,
                reason: r.reason
            }))
        };

        const totalLoss = shiftSettlementRows.reduce((sum, r) => sum + (r.lossCost || 0), 0);
        if (!confirm(`Xác nhận chốt kết ca kho Bếp?\nTổng chi phí hao hụt: ${totalLoss.toLocaleString('vi-VN')} ₫\nThao tác này sẽ giảm tồn đúng lượng loại bỏ và lưu hồ sơ kết ca.`)) {
            return;
        }

        const key = crypto.randomUUID();
        try {
            await MilkTeaApi.post('/api/v1/inventory/shift-settlement', payload, { 'Idempotency-Key': key });
            MilkTeaApi.showToast('Đã chốt kết ca và ghi nhận hao hụt thành công!', 'success');
            await loadStock();
            await loadShiftSettlementPreview();
            await loadPastSettlements();
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi khi chốt kết ca', 'error');
        }
    }

    function filterPastSettlements(isAll) {
        if (isAll) {
            settlementFilter = { all: true, from: null, to: null };
        } else {
            const f = document.getElementById('settlementFilterFrom')?.value;
            const t = document.getElementById('settlementFilterTo')?.value;
            settlementFilter = { all: false, from: f || null, to: t || null };
        }
        loadPastSettlements();
    }

    async function loadPastSettlements() {
        const tbody = document.getElementById('pastSettlementsTableBody');
        if (!tbody) return;

        let url = '/api/v1/inventory/shift-settlement?location=BEP';
        if (settlementFilter.all) {
            url += '&all=true';
        } else if (settlementFilter.from || settlementFilter.to) {
            if (settlementFilter.from) url += `&from=${settlementFilter.from}`;
            if (settlementFilter.to) url += `&to=${settlementFilter.to}`;
        }

        try {
            const list = await MilkTeaApi.get(url);
            if (!list || list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">Chưa có biên bản kết ca nào trong khoảng thời gian này</td></tr>';
                return;
            }
            tbody.innerHTML = list.map(s => `
                <tr>
                    <td class="fw-bold text-primary">#${s.id}</td>
                    <td class="text-nowrap">${formatDateTime(s.closedAt)}</td>
                    <td>${escapeHtml(s.closedByName || s.closedByEmail || '-')}</td>
                    <td class="fw-bold text-danger">${Number(s.totalLossCost || 0).toLocaleString('vi-VN')} ₫</td>
                    <td class="text-muted small">${escapeHtml(s.note || '-')}</td>
                </tr>
            `).join('');
        } catch (e) {
            console.warn('Lỗi nạp lịch sử kết ca:', e);
        }
    }

    // ==========================================
    // TIỆN ÍCH
    // ==========================================

    function formatDateTime(value) {
        if (!value) return '-';
        return new Date(value).toLocaleString('vi-VN', {
            hour: '2-digit',
            minute: '2-digit',
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    function formatQty(value) {
        const number = Number(value || 0);
        return number.toLocaleString('vi-VN', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 3
        });
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    function parsePositiveNumber(value) {
        if (value == null) return null;
        if (typeof value === 'number') {
            return Number.isFinite(value) && value > 0 ? value : null;
        }
        const cleaned = String(value).trim().replace(',', '.');
        if (!cleaned) return null;
        const num = parseFloat(cleaned);
        return Number.isFinite(num) && num > 0 ? num : null;
    }

    return {
        init,
        startCooking,
        completeOrder,
        viewOrderDetail,
        onPrepOutputMaterialChange,
        onDesiredQtyChange,
        submitPreparation,
        filterPreparationHistory,
        loadPreparationHistory,
        loadShiftSettlementPreview,
        onShiftRowInput,
        submitShiftSettlement,
        filterPastSettlements,
        loadPastSettlements
    };
})();
