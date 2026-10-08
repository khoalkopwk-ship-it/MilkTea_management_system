/**
 * MilkTea Admin Panel Logic (Catalog, Cloudinary Upload, Inventory, Reports)
 * Tuân thủ media_storage_rule.md, frontend_rule.md, acceptance_tests.md
 */

const AdminApp = (() => {

    async function init() {
        window.MilkTeaContext = { role: 'ADMIN' };
        MilkTeaRealtime.connect();
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

    // === 2. QUẢN LÝ SẢN PHẨM & DANH MỤC ===
    async function createCategory(name) {
        if (!name || !name.trim()) return;
        try {
            await MilkTeaApi.post('/api/v1/admin/categories', { name: name.trim() });
            MilkTeaApi.showToast('Tạo danh mục mới thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo danh mục', 'error');
        }
    }

    async function createProduct(categoryId, name, size, price, description) {
        try {
            await MilkTeaApi.post('/api/v1/admin/products', {
                categoryId: parseInt(categoryId),
                name: name.trim(),
                size: size.trim(),
                price: parseFloat(price),
                description: description ? description.trim() : null
            });
            MilkTeaApi.showToast('Thêm món uống thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi thêm món', 'error');
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
            await MilkTeaApi.put(`/api/v1/admin/accounts/${id}/status`, { active });
            MilkTeaApi.showToast('Cập nhật trạng thái tài khoản thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật trạng thái', 'error');
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
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi tạo phiếu nhập kho', 'error');
        }
    }

    async function submitIssue(reason, items) {
        try {
            // Chuyển KHO -> BẾP không cần Admin duyệt theo domain_system.md
            await MilkTeaApi.post('/api/v1/inventory/issues', {
                reason,
                items
            });
            MilkTeaApi.showToast('Chuyển kho sang Bếp thành công! Đã tăng tồn Bếp.', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi chuyển kho sang Bếp', 'error');
        }
    }

    async function submitWaste(location, reason, items) {
        try {
            await MilkTeaApi.post('/api/v1/inventory/waste', {
                location,
                reason,
                items
            });
            MilkTeaApi.showToast('Ghi nhận hao hụt thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi ghi hao hụt', 'error');
        }
    }

    async function updateThreshold(materialId, location, threshold) {
        try {
            await MilkTeaApi.put('/api/v1/admin/materials/thresholds', {
                materialId: parseInt(materialId),
                location: location,
                minThreshold: parseFloat(threshold)
            });
            MilkTeaApi.showToast('Cập nhật ngưỡng cảnh báo tồn tối thiểu thành công!', 'success');
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật ngưỡng tồn', 'error');
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
    async function updateSettings(shopName, address, bankName, bankAccountNumber, bankAccountHolder, discountPercent) {
        try {
            await MilkTeaApi.put('/api/v1/admin/settings', {
                shopName,
                address,
                bankName,
                bankAccountNumber,
                bankAccountHolder,
                discountPercent: parseFloat(discountPercent || 0)
            });
            MilkTeaApi.showToast('Cập nhật thông tin quán và khuyến mãi thành công!', 'success');
            setTimeout(() => window.location.reload(), 500);
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi cập nhật cài đặt', 'error');
        }
    }

    return {
        init,
        uploadProductImage,
        createCategory,
        createProduct,
        updateRecipe,
        createTable,
        createStaff,
        toggleStaffStatus,
        submitImport,
        submitIssue,
        submitWaste,
        updateThreshold,
        loadRevenueReport,
        updateSettings
    };
})();
