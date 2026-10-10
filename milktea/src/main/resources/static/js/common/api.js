/**
 * MilkTea Unified HTTP Client & API Utilities
 * Tuân thủ author_rule.md và frontend_rule.md
 */
const MilkTeaApi = (() => {
    let csrfToken = null;
    let csrfHeaderName = 'X-CSRF-TOKEN';

    // Khởi tạo CSRF token
    async function initCsrf() {
        if (csrfToken) return csrfToken;
        try {
            const res = await fetch('/api/v1/auth/csrf');
            const json = await res.json();
            if (json && json.data) {
                csrfToken = json.data.token;
                csrfHeaderName = json.data.headerName || 'X-CSRF-TOKEN';
            }
        } catch (e) {
            console.warn('Không thể nạp CSRF token ban đầu:', e);
        }
        return csrfToken;
    }

    // Format tiền tệ VND
    function formatVND(amount) {
        if (amount === null || amount === undefined) return '0 ₫';
        const num = typeof amount === 'string' ? parseFloat(amount) : amount;
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num);
    }

    // Unified fetch
    async function request(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = options.headers ? { ...options.headers } : {};

        // Đính kèm CSRF token cho các mutation methods
        if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
            await initCsrf();
            if (csrfToken) {
                headers[csrfHeaderName] = csrfToken;
            }
        }

        // Đính kèm Guest Token nếu có trong RAM session hiện tại
        if (window.MilkTeaContext) {
            if (window.MilkTeaContext.tableToken && !headers['X-Table-Session-Token']) {
                headers['X-Table-Session-Token'] = window.MilkTeaContext.tableToken;
            }
            if (window.MilkTeaContext.counterToken && !headers['X-Counter-Order-Token']) {
                headers['X-Counter-Order-Token'] = window.MilkTeaContext.counterToken;
            }
        }

        // Nếu body là object và chưa set Content-Type
        let body = options.body;
        if (body && typeof body === 'object' && !(body instanceof FormData) && !(body instanceof URLSearchParams)) {
            headers['Content-Type'] = 'application/json';
            body = JSON.stringify(body);
        }

        const fetchOptions = {
            method,
            headers,
            credentials: 'same-origin', // Gửi HttpOnly cookie milktea_access_token tự động
            body
        };

        try {
            const response = await fetch(url, fetchOptions);

            // Kiểm tra Content-Type
            const contentType = response.headers.get('content-type') || '';
            let result = null;
            if (contentType.includes('application/json')) {
                result = await response.json();
            } else {
                result = { success: response.ok, data: await response.text() };
            }

            if (!response.ok) {
                const errorCode = result && result.error ? result.error.code : 'HTTP_' + response.status;
                const errorMessage = result && result.error ? result.error.message : response.statusText;
                
                // Nếu bị 401 hoặc 403, kiểm tra nếu cần đăng nhập
                if (response.status === 401) {
                    console.warn('Phiên đăng nhập hết hạn hoặc chưa đăng nhập');
                }

                const err = new Error(errorMessage || 'Đã có lỗi xảy ra');
                err.status = response.status;
                err.code = errorCode;
                err.details = result && result.error ? result.error.details : null;
                throw err;
            }

            return result && result.data !== undefined ? result.data : result;
        } catch (error) {
            console.error(`API Error [${method} ${url}]:`, error);
            throw error;
        }
    }

    // Tiện ích Toast thông báo
    function showToast(message, type = 'success') {
        const toastEl = document.getElementById('liveToast');
        if (toastEl) {
            const toastBody = toastEl.querySelector('.toast-message') || toastEl.querySelector('.toast-body');
            const toastTitle = toastEl.querySelector('.toast-title');
            if (toastBody) toastBody.textContent = message;
            if (toastTitle) {
                toastTitle.textContent = type === 'success' ? 'Thành công' : (type === 'error' ? 'Lỗi' : 'Thông báo');
            }
            toastEl.className = `toast align-items-center text-white border-0 bg-${type === 'error' ? 'danger' : (type === 'warning' ? 'warning text-dark' : 'success')}`;
            const toast = new bootstrap.Toast(toastEl, { delay: 3500 });
            toast.show();
        } else {
            alert(message);
        }
    }

    return {
        get: (url, headers) => request(url, { method: 'GET', headers }),
        post: (url, body, headers) => request(url, { method: 'POST', body, headers }),
        put: (url, body, headers) => request(url, { method: 'PUT', body, headers }),
        delete: (url, headers) => request(url, { method: 'DELETE', headers }),
        upload: (url, formData) => request(url, { method: 'POST', body: formData }),
        formatVND,
        showToast,
        initCsrf
    };
})();

// Khởi chạy nạp CSRF khi trang load
document.addEventListener('DOMContentLoaded', () => {
    MilkTeaApi.initCsrf();
});
