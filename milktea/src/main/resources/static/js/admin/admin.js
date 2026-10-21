/**
 * MilkTea Admin Panel Logic (Catalog, Cloudinary Upload, Inventory, Reports)
 * Tuân thủ media_storage_rule.md, frontend_rule.md, acceptance_tests.md
 */

const AdminApp = (() => {

    async function init() {
        window.MilkTeaContext = { role: 'ADMIN' };
        MilkTeaRealtime.connect();
        refreshAdminRefundBadge();
        MilkTeaRealtime.on('REFUND_CHANGED', refreshAdminRefundBadge);
        MilkTeaRealtime.on('CANCELLATION_REQUESTED', refreshAdminRefundBadge);
        MilkTeaRealtime.on('CATALOG_CHANGED', () => {
            if (document.getElementById('productTableBody')) {
                loadProducts(1);
            }
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
        MilkTeaRealtime.onReconnect(refreshAdminRefundBadge);
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) refreshAdminRefundBadge();
        });

        if (document.getElementById('productTableBody')) {
            loadProducts(1);
        }
    }

    async function refreshAdminRefundBadge() {
        try {
            const res = await MilkTeaApi.get('/api/v1/admin/documents/refunds/pending-count');
            const count = (res && typeof res.pendingCount === 'number') ? res.pendingCount : 0;
            document.querySelectorAll('.admin-refund-badge').forEach(el => {
                el.textContent = count;
                el.classList.toggle('d-none', count === 0);
            });
        } catch (e) {
            console.debug('Error refreshing admin refund badge:', e);
        }
    }

    // === 1. QUẢN LÝ ẢNH CLOUDINARY ===
    async function uploadProductImage(productId, fileInput) {
        if (!fileInput.files || fileInput.files.length === 0) {
            MilkTeaApi.showToast('Vui lòng chọn một file ảnh', 'warning');
            return;
        }

        const file = fileInput.files[0];
        // Client-side validation: MIME type và file size <= 5MB
        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!validTypes.includes(file.type.toLowerCase())) {
            MilkTeaApi.showToast('Định dạng ảnh không hợp lệ (Chỉ nhận JPEG, PNG, WebP)', 'error');
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            MilkTeaApi.showToast('Kích thước ảnh vượt quá 5MB', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        const spinner = document.getElementById(`uploadSpinner_${productId}`);
        if (spinner) spinner.classList.remove('d-none');

        try {
            const data = await MilkTeaApi.upload(`/api/v1/admin/products/${productId}/image`, formData);
            MilkTeaApi.showToast('Đã tải ảnh lên Cloudinary thành công!', 'success');

            // Cập nhật ảnh hiển thị
            const imgEl = document.getElementById(`productImg_${productId}`);
            if (imgEl && data.imageUrl) {
                imgEl.src = data.imageUrl;
            }
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tải ảnh lên Cloudinary', 'error');
        } finally {
            if (spinner) spinner.classList.add('d-none');
            fileInput.value = '';
        }
    }

    async function uploadLogo(fileInput) {
        if (!fileInput.files || fileInput.files.length === 0) {
            MilkTeaApi.showToast('Vui lòng chọn một file ảnh logo', 'warning');
            return;
        }

        const file = fileInput.files[0];
        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!validTypes.includes(file.type.toLowerCase())) {
            MilkTeaApi.showToast('Định dạng ảnh không hợp lệ (Chỉ nhận JPEG, PNG, WebP)', 'error');
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            MilkTeaApi.showToast('Kích thước ảnh vượt quá 5MB', 'error');
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        const spinner = document.getElementById('logoUploadSpinner');
        if (spinner) spinner.classList.remove('d-none');

        try {
            const data = await MilkTeaApi.upload('/api/v1/admin/settings/logo', formData);
            MilkTeaApi.showToast('Đã tải logo quán lên Cloudinary thành công!', 'success');

            const logoImg = document.getElementById('shopLogoPreview');
            if (logoImg && data.logoUrl) {
                logoImg.src = data.logoUrl;
                logoImg.classList.remove('d-none');
            }
            const fallbackIcon = document.getElementById('shopLogoFallbackIcon');
            if (fallbackIcon) fallbackIcon.classList.add('d-none');

            // Cập nhật ngay lập tức logo trên navbar header
            if (data.logoUrl) {
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
                    img.src = data.logoUrl;
                    img.style.display = 'inline-block';
                    if (badge) badge.style.setProperty('display', 'none', 'important');
                });
            }
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tải ảnh logo lên Cloudinary', 'error');
        } finally {
            if (spinner) spinner.classList.add('d-none');
            fileInput.value = '';
        }
    }

    // === 2. QUẢN LÝ SẢN PHẨM & DANH MỤC ===
    async function createCategory(name, allowTopping = true) {
        if (!name || !name.trim()) {
            MilkTeaApi.showToast('Tên danh mục không được để trống', 'warning');
            return;
        }

        try {
            await MilkTeaApi.post('/api/v1/admin/categories', {
                name: name.trim(),
                allowTopping: Boolean(allowTopping)
            });
            MilkTeaApi.showToast('Tạo danh mục mới thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo danh mục', 'error');
        }
    }

    async function updateCategory(id, name, active, allowTopping = true) {
        if (!id) {
            MilkTeaApi.showToast('Không xác định được danh mục cần sửa', 'error');
            return;
        }

        if (!name || !name.trim()) {
            MilkTeaApi.showToast('Tên danh mục không được để trống', 'warning');
            return;
        }

        try {
            await MilkTeaApi.put(`/api/v1/admin/categories/${id}`, {
                name: name.trim(),
                active: Boolean(active),
                allowTopping: Boolean(allowTopping)
            });
            MilkTeaApi.showToast('Cập nhật danh mục thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật danh mục', 'error');
        }
    }

    async function deleteCategory(id) {
        if (!id) {
            MilkTeaApi.showToast('Không xác định được danh mục cần ẩn', 'error');
            return;
        }

        try {
            await MilkTeaApi.delete(`/api/v1/admin/categories/${id}`);
            MilkTeaApi.showToast('Đã ẩn danh mục thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi ẩn danh mục', 'error');
        }
    }

    async function createProduct(categoryId, name, size, price, description) {
        const parsedCategoryId = Number(categoryId);
        const productName = name ? name.trim() : '';
        const productSize = size ? size.trim().toUpperCase() : '';
        const normalizedPrice = String(price || '').replace(/[^\d.]/g, '');
        const parsedPrice = Number(normalizedPrice);

        if (!Number.isFinite(parsedCategoryId) || parsedCategoryId <= 0) {
            MilkTeaApi.showToast('Vui lòng chọn danh mục cho món uống', 'warning');
            return;
        }

        if (!productName) {
            MilkTeaApi.showToast('Tên món uống không được để trống', 'warning');
            return;
        }

        if (!productSize) {
            MilkTeaApi.showToast('Size không được để trống', 'warning');
            return;
        }

        if (!Number.isFinite(parsedPrice) || parsedPrice <= 0) {
            MilkTeaApi.showToast('Giá bán phải lớn hơn 0', 'warning');
            return;
        }

        try {
            await MilkTeaApi.post('/api/v1/admin/products', {
                categoryId: parsedCategoryId,
                name: productName,
                size: productSize,
                price: String(parsedPrice),
                description: description ? description.trim() : null,
                active: true
            });
            MilkTeaApi.showToast('Thêm món uống thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            const fieldMessage = e.details ? Object.values(e.details).filter(Boolean).join(', ') : '';
            MilkTeaApi.showToast(fieldMessage || e.message || 'Lỗi thêm món', 'error');
        }
    }

    async function updateRecipe(productId, items) {
        try {
            await MilkTeaApi.put(`/api/v1/admin/products/${productId}/recipes`, { items });
            MilkTeaApi.showToast('Cập nhật định lượng công thức thành công!', 'success');
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật công thức', 'error');
        }
    }

    // === 3. QUẢN LÝ BÀN ===
    async function createTable(name) {
        if (!name || !name.trim()) return;
        try {
            await MilkTeaApi.post('/api/v1/admin/tables', { name: name.trim() });
            MilkTeaApi.showToast('Thêm bàn mới thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo bàn', 'error');
        }
    }

    // === 4. QUẢN LÝ NHÂN VIÊN ===
    async function createStaff(fullName, email, password, phone, role) {
        try {
            await MilkTeaApi.post('/api/v1/admin/accounts', {
                fullName,
                email,
                password,
                phone,
                role
            });
            MilkTeaApi.showToast('Tạo tài khoản nhân viên thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo nhân viên', 'error');
        }
    }

    async function toggleStaffStatus(id, active) {
        try {
            await MilkTeaApi.post(`/api/v1/admin/accounts/${id}/toggle-status`, { active });
            MilkTeaApi.showToast('Cập nhật trạng thái tài khoản thành công!', 'success');
            return true;
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật trạng thái', 'error');
            throw e;
        }
    }

    async function updateStaff(id, fullName, email, phone, role) {
        try {
            await MilkTeaApi.put(`/api/v1/admin/accounts/${id}`, {
                fullName,
                email,
                phone,
                role
            });
            MilkTeaApi.showToast('Cập nhật thông tin nhân viên thành công!', 'success');
            return true;
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật nhân viên', 'error');
            throw e;
        }
    }

    // === 5. KHO & CHỨNG TỪ ===
    async function submitImport(supplier, reason, items) {
        try {
            await MilkTeaApi.post('/api/v1/inventory/imports', {
                supplier,
                reason,
                items
            });
            MilkTeaApi.showToast('Nhập kho thành công! Đã tăng tồn kho KHO.', 'success');
            const modalEl = document.getElementById('importModal');
            if (modalEl) {
                const modal = bootstrap.Modal.getInstance(modalEl);
                if (modal) modal.hide();
            }
            if (typeof loadStockData === 'function') {
                await loadStockData();
            }
            if (typeof refreshMaterialCombobox === 'function') {
                await refreshMaterialCombobox();
            }
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo phiếu nhập kho', 'error');
        }
    }

    async function submitWaste(location, reason, items) {
        try {
            await MilkTeaApi.post('/api/v1/inventory/waste', {
                location: 'KHO',
                reason,
                items
            });
            MilkTeaApi.showToast('Ghi nhận hao hụt thành công! Đã giảm tồn kho KHO.', 'success');
            const modalEl = document.getElementById('wasteModal');
            if (modalEl) {
                const modal = bootstrap.Modal.getInstance(modalEl);
                if (modal) modal.hide();
            }
            if (typeof loadStockData === 'function') {
                await loadStockData();
            }
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi ghi hao hụt', 'error');
        }
    }

    async function adjustRawMaterial(materialId, name, quantity, averageImportPrice, threshold, reason) {
        try {
            await MilkTeaApi.put('/api/v1/inventory/stocks/adjust', {
                materialId,
                name,
                quantity,
                averageImportPrice,
                threshold,
                reason
            });
            MilkTeaApi.showToast('Điều chỉnh thông tin nguyên liệu thành công!', 'success');
            const modalEl = document.getElementById('editStockModal');
            if (modalEl) {
                const modal = bootstrap.Modal.getInstance(modalEl);
                if (modal) modal.hide();
            }
            if (typeof loadStockData === 'function') {
                await loadStockData();
            }
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi điều chỉnh nguyên liệu', 'error');
        }
    }


    async function updateThreshold(materialId, location, threshold) {
        const value = Number(threshold);

        if (!Number.isFinite(value) || value < 0) {
            MilkTeaApi.showToast(
                'Ngưỡng tồn phải là số không âm',
                'warning'
            );
            return;
        }

        try {
            await MilkTeaApi.put(
                '/api/v1/inventory/stocks/threshold',
                {
                    materialId: Number(materialId),
                    location: location,
                    threshold: value
                }
            );

            MilkTeaApi.showToast(
                'Cập nhật ngưỡng cảnh báo thành công!',
                'success'
            );

            if (typeof loadStockData === 'function') {
                await loadStockData();
            } else {
                window.location.reload();
            }

        } catch (e) {
            MilkTeaApi.showToast(
                e.message || 'Không thể cập nhật ngưỡng tồn',
                'error'
            );
        }
    }


    // === 6. BÁO CÁO TÀI CHÍNH 4 CHỈ SỐ ===
    async function loadRevenueReport(startDate, endDate) {
        try {
            let url = '/api/v1/admin/reports/revenue?';
            if (startDate) url += 'fromDate=' + encodeURIComponent(startDate) + '&';
            if (endDate) url += 'toDate=' + encodeURIComponent(endDate);

            const data = await MilkTeaApi.get(url);
            if (data) {
                // 1. Doanh số đơn hoàn thành (completedOrderValue)
                const completedEl = document.getElementById('reportCompletedValue');
                if (completedEl) completedEl.textContent = MilkTeaApi.formatVND(data.completedOrderValue || 0);

                // 2. Tiền thực thu (received / actualReceiptsTotal)
                const collectedEl = document.getElementById('reportCollected');
                if (collectedEl) collectedEl.textContent = MilkTeaApi.formatVND(data.received || data.actualReceiptsTotal || 0);

                // 3. Tiền thực hoàn (refunded / actualRefundsTotal)
                const refundedEl = document.getElementById('reportRefunded');
                if (refundedEl) refundedEl.textContent = MilkTeaApi.formatVND(data.refunded || data.actualRefundsTotal || 0);

                // 4. Tiền còn phải thu / Dư nợ (currentOutstanding / currentOutstandingDebt)
                const receivableEl = document.getElementById('reportReceivable');
                if (receivableEl) receivableEl.textContent = MilkTeaApi.formatVND(data.currentOutstanding || data.currentOutstandingDebt || 0);

                // 5. Thu ròng (netReceived)
                const netEl = document.getElementById('reportNetReceived');
                if (netEl) netEl.textContent = MilkTeaApi.formatVND(data.netReceived || 0);
            }
        } catch (e) {
            console.warn('Lỗi nạp báo cáo doanh thu:', e);
            MilkTeaApi.showToast('Không thể tải báo cáo doanh thu', 'error');
        }
    }

    // === 7. CẤU HÌNH QUÁN & KHUYẾN MÃI ===
    async function updateSettings(shopName, address, bankName, bankAccountNumber, bankAccountHolder, discountPercent, discountType, discountValue) {
        try {
            await MilkTeaApi.put('/api/v1/admin/settings', {
                shopName,
                address,
                bankName,
                bankAccountNumber,
                bankAccountHolder,
                discountPercent: parseFloat(discountPercent || 0),
                discountType: discountType || 'PERCENT',
                discountValue: parseFloat(discountValue !== undefined && discountValue !== null ? discountValue : (discountPercent || 0))
            });
            MilkTeaApi.showToast('Cập nhật thông tin quán và khuyến mãi thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật cài đặt', 'error');
        }
    }

    // === 8. QUẢN LÝ MÓN UỐNG NÂNG CAO & CẤU HÌNH GỘP ===
    let currentProductPage = 1;
    let currentFullConfigRecipeRows = [];

    async function loadProducts(page = 1) {
        currentProductPage = page;
        const tbody = document.getElementById('productTableBody');
        if (!tbody) return;

        const catSelect = document.getElementById('productFilterCat');
        const activeSelect = document.getElementById('productFilterActive');
        const searchInput = document.getElementById('productSearchInput');

        let url = `/api/v1/admin/products?page=${page}&limit=10`;
        if (catSelect && catSelect.value) {
            url += `&categoryId=${encodeURIComponent(catSelect.value)}`;
        }
        if (activeSelect && activeSelect.value !== '') {
            url += `&active=${encodeURIComponent(activeSelect.value)}`;
        }
        if (searchInput && searchInput.value.trim()) {
            url += `&q=${encodeURIComponent(searchInput.value.trim())}`;
        }

        try {
            const data = await MilkTeaApi.get(url);
            renderProductTable(data);
        } catch (e) {
            console.error(e);
            MilkTeaApi.showToast('Không tải được danh sách món uống', 'error');
        }
    }

    function resetProductFilters() {
        const catSelect = document.getElementById('productFilterCat');
        const activeSelect = document.getElementById('productFilterActive');
        const searchInput = document.getElementById('productSearchInput');

        if (catSelect) catSelect.value = '';
        if (activeSelect) activeSelect.value = '';
        if (searchInput) searchInput.value = '';

        loadProducts(1);
    }
    function renderProductTable(data) {
        const tbody = document.getElementById('productTableBody');
        const pagContainer = document.getElementById('productPaginationContainer');
        if (!tbody) return;

        const items = data?.items || [];
        if (items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-5">Không có món uống nào phù hợp.</td></tr>';
            if (pagContainer) pagContainer.innerHTML = '';
            return;
        }

        const rowsHtml = items.map(p => {
            const defaultImg = 'https://images.unsplash.com/photo-1558857563-b37cf5efee6a?w=100&q=80';
            const imgUrl = (p.imageUrl && p.imageUrl.trim()) ? p.imageUrl : defaultImg;
            const priceFormatted = MilkTeaApi.formatVND ? MilkTeaApi.formatVND(p.price) : (Number(p.price).toLocaleString('vi-VN') + ' ₫');
            const statusBadge = p.active
                ? '<span class="badge bg-success">Đang bán</span>'
                : '<span class="badge bg-secondary">Ngừng bán</span>';

            return `<tr>
                <td>
                    <div class="position-relative" style="width: 50px; height: 50px;">
                        <img id="productImg_${p.id}" src="${imgUrl}" class="rounded-3 w-100 h-100 object-fit-cover border" alt="Ảnh">
                        <div id="uploadSpinner_${p.id}" class="spinner-border spinner-border-sm text-primary position-absolute top-50 start-50 translate-middle d-none"></div>
                    </div>
                </td>
                <td>
                    <strong class="d-block">${MilkTeaReceipts ? MilkTeaReceipts.esc(p.name) : p.name}</strong>
                    <small class="text-muted">${p.description ? (MilkTeaReceipts ? MilkTeaReceipts.esc(p.description) : p.description) : ''}</small>
                </td>
                <td><span class="badge bg-light text-dark border">${p.categoryName || 'Chưa phân loại'}</span></td>
                <td><span class="badge bg-secondary">${p.size || 'M'}</span></td>
                <td class="fw-bold product-price">${priceFormatted}</td>
                <td>${statusBadge}</td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-2"
                                onclick="AdminApp.openProductFullConfig(${p.id})" title="Cấu hình & Công thức">
                            <i class="bi bi-sliders me-1"></i> Cấu hình
                        </button>
                        <label class="btn btn-sm btn-outline-info rounded-pill px-2 mb-0" title="Đổi ảnh Cloudinary">
                            <i class="bi bi-cloud-arrow-up"></i>
                            <input type="file" class="d-none" accept="image/jpeg,image/png,image/webp"
                                   onchange="AdminApp.uploadProductImage(${p.id}, this)">
                        </label>
                    </div>
                </td>
            </tr>`;
        }).join('');

        tbody.innerHTML = rowsHtml;

        if (pagContainer) {
            const totalPages = data.totalPages || 1;
            const currentPage = data.page || 1;
            const totalItems = data.totalItems || items.length;

            if (totalPages <= 1) {
                pagContainer.innerHTML = `<small class="text-muted">Tổng cộng: <b>${totalItems}</b> món uống</small>`;
                return;
            }

            let pagBtns = '';
            for (let i = 1; i <= totalPages; i++) {
                pagBtns += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                    <button class="page-link" onclick="AdminApp.loadProducts(${i})">${i}</button>
                </li>`;
            }

            pagContainer.innerHTML = `
                <div class="d-flex justify-content-between align-items-center">
                    <small class="text-muted">Trang <b>${currentPage}</b> / <b>${totalPages}</b> (Tổng: <b>${totalItems}</b> món)</small>
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                            <button class="page-link" onclick="AdminApp.loadProducts(${currentPage - 1})">«</button>
                        </li>
                        ${pagBtns}
                        <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
                            <button class="page-link" onclick="AdminApp.loadProducts(${currentPage + 1})">»</button>
                        </li>
                    </ul>
                </div>
            `;
        }
    }

    async function openProductFullConfig(productId) {
        try {
            const [product, recipeData] = await Promise.all([
                MilkTeaApi.get(`/api/v1/admin/products/${productId}`),
                MilkTeaApi.get(`/api/v1/admin/products/${productId}/recipe`)
            ]);

            document.getElementById('cfgProductId').value = product.id;
            document.getElementById('cfgProductOldImageUrl').value = product.imageUrl || '';
            document.getElementById('cfgProdName').value = product.name || '';
            document.getElementById('cfgProdCategory').value = product.categoryId || '';
            document.getElementById('cfgProdActive').value = String(Boolean(product.active));
            document.getElementById('cfgProdDesc').value = product.description || '';
            document.getElementById('cfgProdSize').value = product.size || 'M';
            document.getElementById('cfgProdPrice').value = product.price || '';

            const defaultImg = 'https://images.unsplash.com/photo-1558857563-b37cf5efee6a?w=100&q=80';
            document.getElementById('cfgProdImgPreview').src = product.imageUrl || defaultImg;
            document.getElementById('cfgProdFileInput').value = '';

            document.getElementById('fullConfigTitle').textContent = `Cấu Hình: ${product.name} (${product.size})`;

            currentFullConfigRecipeRows = (recipeData?.ingredients || []).map(item => ({
                materialId: item.materialId,
                materialName: item.materialName,
                unit: item.unit,
                quantity: item.quantity != null ? item.quantity : 1
            }));

            renderFullConfigRecipeTable();

            bootstrap.Modal.getOrCreateInstance(document.getElementById('productFullConfigModal')).show();
        } catch (e) {
            console.error(e);
            MilkTeaApi.showToast('Không tải được thông tin món: ' + (e.message || ''), 'error');
        }
    }

    function previewFullConfigImage(fileInput) {
        if (!fileInput.files || fileInput.files.length === 0) return;
        const file = fileInput.files[0];
        const reader = new FileReader();
        reader.onload = e => {
            document.getElementById('cfgProdImgPreview').src = e.target.result;
        };
        reader.readAsDataURL(file);
    }

    function renderFullConfigRecipeTable() {
        const tbody = document.getElementById('cfgRecipeTableBody');
        if (!tbody) return;

        if (currentFullConfigRecipeRows.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Chưa có nguyên liệu nào trong công thức.</td></tr>';
            return;
        }

        tbody.innerHTML = currentFullConfigRecipeRows.map((row, index) => {
            return `<tr>
                <td class="fw-semibold">${row.materialName || 'Nguyên liệu #' + row.materialId}</td>
                <td>
                    <input type="number" step="0.001" min="0.001" class="form-control form-control-sm"
                           value="${row.quantity}" onchange="AdminApp.updateFullConfigRecipeQty(${index}, this.value)">
                </td>
                <td class="text-muted small">${row.unit || ''}</td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="AdminApp.deleteFullConfigRecipeRow(${index})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>`;
        }).join('');
    }

    function updateFullConfigRecipeQty(index, val) {
        if (currentFullConfigRecipeRows[index]) {
            currentFullConfigRecipeRows[index].quantity = parseFloat(val) || 0;
        }
    }

    function addFullConfigRecipeRow() {
        const select = document.getElementById('cfgAddMaterialSelect');
        const qtyInput = document.getElementById('cfgAddQuantityInput');
        const materialId = Number(select.value);
        const qty = parseFloat(qtyInput.value) || 1;

        if (!materialId) {
            MilkTeaApi.showToast('Vui lòng chọn nguyên liệu', 'warning');
            return;
        }
        if (qty <= 0) {
            MilkTeaApi.showToast('Định lượng phải lớn hơn 0', 'warning');
            return;
        }

        if (currentFullConfigRecipeRows.some(r => Number(r.materialId) === materialId)) {
            MilkTeaApi.showToast('Nguyên liệu này đã có trong công thức', 'warning');
            return;
        }

        const rawText = select.options[select.selectedIndex].text;
        let mName = rawText;
        let mUnit = '';
        const match = rawText.match(/^(.*?)\s*\((.*?)\)$/);
        if (match) {
            mName = match[1];
            mUnit = match[2];
        }

        currentFullConfigRecipeRows.push({
            materialId,
            materialName: mName,
            unit: mUnit,
            quantity: qty
        });

        renderFullConfigRecipeTable();
        qtyInput.value = '1';
    }

    function deleteFullConfigRecipeRow(index) {
        currentFullConfigRecipeRows.splice(index, 1);
        renderFullConfigRecipeTable();
    }

    async function saveProductFullConfig() {
        const productId = document.getElementById('cfgProductId').value;
        const name = document.getElementById('cfgProdName').value.trim();
        const categoryId = Number(document.getElementById('cfgProdCategory').value);
        const active = document.getElementById('cfgProdActive').value === 'true';
        const desc = document.getElementById('cfgProdDesc').value.trim();
        const size = document.getElementById('cfgProdSize').value.trim().toUpperCase();
        const price = parseFloat(document.getElementById('cfgProdPrice').value);
        const oldImg = document.getElementById('cfgProductOldImageUrl').value;
        const fileInput = document.getElementById('cfgProdFileInput');

        if (!name) {
            MilkTeaApi.showToast('Tên món không được để trống', 'warning');
            return;
        }
        if (!categoryId) {
            MilkTeaApi.showToast('Vui lòng chọn danh mục', 'warning');
            return;
        }
        if (!size) {
            MilkTeaApi.showToast('Size không được để trống', 'warning');
            return;
        }
        if (!price || price <= 0) {
            MilkTeaApi.showToast('Giá bán phải lớn hơn 0', 'warning');
            return;
        }

        const btnSave = document.getElementById('btnSaveFullConfig');
        if (btnSave) btnSave.disabled = true;

        try {
            let finalImageUrl = oldImg;

            // 1. Upload ảnh Cloudinary nếu người dùng đã chọn file mới
            if (fileInput.files && fileInput.files.length > 0) {
                const formData = new FormData();
                formData.append('file', fileInput.files[0]);
                const uploadRes = await MilkTeaApi.upload(`/api/v1/admin/products/${productId}/image`, formData);
                if (uploadRes && uploadRes.imageUrl) {
                    finalImageUrl = uploadRes.imageUrl;
                }
            }

            // 2. Cập nhật thông tin món
            await MilkTeaApi.put(`/api/v1/admin/products/${productId}`, {
                categoryId,
                name,
                size,
                price: String(price),
                description: desc || null,
                imageUrl: finalImageUrl || null,
                active
            });

            // 3. Cập nhật công thức
            const recipeItems = currentFullConfigRecipeRows.map(r => ({
                materialId: Number(r.materialId),
                quantity: Number(r.quantity) > 0 ? Number(r.quantity) : 1
            }));
            await MilkTeaApi.put(`/api/v1/admin/products/${productId}/recipe`, {
                items: recipeItems
            });

            MilkTeaApi.showToast('Cập nhật cấu hình món thành công!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('productFullConfigModal'))?.hide();
            await loadProducts(currentProductPage);
        } catch (e) {
            console.error(e);
            MilkTeaApi.showToast('Lỗi lưu cấu hình: ' + (e.message || ''), 'error');
        } finally {
            if (btnSave) btnSave.disabled = false;
        }
    }

    return {
        init,
        uploadProductImage,
        uploadLogo,
        createCategory,
        updateCategory,
        deleteCategory,
        createProduct,
        updateRecipe,
        createTable,
        createStaff,
        updateStaff,
        toggleStaffStatus,
        submitImport,
        submitWaste,
        adjustRawMaterial,
        updateThreshold,
        loadRevenueReport,
        updateSettings,
        loadProducts,
        resetProductFilters,
        openProductFullConfig,
        previewFullConfigImage,
        addFullConfigRecipeRow,
        deleteFullConfigRecipeRow,
        updateFullConfigRecipeQty,
        saveProductFullConfig,
        refreshAdminRefundBadge
    };
})();
