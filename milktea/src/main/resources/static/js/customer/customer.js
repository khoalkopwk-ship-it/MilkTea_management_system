/**
 * MilkTea Customer Client Logic (Menu, Cart, Orders, Realtime)
 * Tuân thủ frontend_rule.md và author_rule.md
 */

window.MilkTeaContext = {
    tableId: null,
    tableName: null,
    qrCode: null,
    sessionId: null,
    sessionStatus: null,
    tableToken: null,
    cartVersion: "0"
};

const CustomerApp = (() => {
    let localCart = []; // Giỏ RAM khi chưa có phiên bàn mở
    let serverCart = []; // Giỏ server khi đã có phiên bàn mở

    function getLocalCartStorageKey() {
        const ctx = window.MilkTeaContext || {};
        if (ctx.qrCode) return `milktea.localCart.qr.${ctx.qrCode}`;
        if (ctx.tableId) return `milktea.localCart.table.${ctx.tableId}`;
        return 'milktea.localCart';
    }

    function loadLocalCart() {
        try {
            const raw = localStorage.getItem(getLocalCartStorageKey());
            localCart = raw ? JSON.parse(raw) : [];
            if (!Array.isArray(localCart)) localCart = [];
        } catch (e) {
            localCart = [];
        }
    }

    function saveLocalCart() {
        localStorage.setItem(getLocalCartStorageKey(), JSON.stringify(localCart));
    }

    function clearLocalCart() {
        localCart = [];
        localStorage.removeItem(getLocalCartStorageKey());
    }

    // Khởi tạo ngữ cảnh bàn
    async function initContext(qrCodeParam, tableParam) {
        let qr = qrCodeParam;
        const urlParams = new URLSearchParams(window.location.search);
        if (!qr) qr = urlParams.get('qr');
        let table = tableParam || urlParams.get('table');

        if (qr || table) {
            try {
                let url = '/api/v1/public/table-context?';
                if (qr) url += 'qrCode=' + encodeURIComponent(qr);
                else if (table) url += 'table=' + encodeURIComponent(table);

                const ctx = await MilkTeaApi.get(url);
                if (ctx) {
                    window.MilkTeaContext.tableId = ctx.tableId;
                    window.MilkTeaContext.tableName = ctx.tableName;
                    window.MilkTeaContext.qrCode = ctx.qrCode;
                    window.MilkTeaContext.sessionId = ctx.sessionId;
                    window.MilkTeaContext.sessionStatus = ctx.sessionStatus;
                    window.MilkTeaContext.tableToken = ctx.tableToken;

                    console.log('Đã nạp ngữ cảnh bàn.');

                    // Kết nối WebSocket nếu có phiên
                    if (window.MilkTeaContext.sessionId) {
                        MilkTeaRealtime.connect();
                        // Nạp giỏ hàng từ server
                        await loadServerCart();
                    }
                }
            } catch (e) {
                console.warn('Không thể nạp ngữ cảnh bàn:', e.message);
            }
        }

        if (!window.MilkTeaContext.tableId) window.MilkTeaContext.counterToken = sessionStorage.getItem('milktea.counterToken');
        loadLocalCart();
        updateCartBadge();
        setupRealtimeListeners();
    }

    function setupRealtimeListeners() {
        MilkTeaRealtime.on('CART_CHANGED', (event) => {
            console.log('Giỏ hàng thay đổi từ thiết bị khác. Đang tải lại giỏ hàng...');
            loadServerCart();
        });

        MilkTeaRealtime.on('ORDER_STATUS_CHANGED', (event) => {
            console.log('Trạng thái đơn hàng cập nhật:', event);
            if (typeof loadOrdersList === 'function') {
                loadOrdersList();
            }
        });

        MilkTeaRealtime.onReconnect(() => {
            console.log('WebSocket reconnect: Làm mới dữ liệu từ server...');
            if (window.MilkTeaContext.sessionId) {
                loadServerCart();
            }
            if (typeof loadOrdersList === 'function') {
                loadOrdersList();
            }
        });
    }

    // Thêm món vào giỏ
    function addToCart(product, quantity = 1, note = '') {
        const item = {
            productId: product.id,
            productName: product.name,
            size: product.size,
            price: product.price,
            quantity: parseInt(quantity),
            note: note || ''
        };

        if (window.MilkTeaContext.sessionId) {
            // Đã có phiên -> Gửi lên giỏ server
            addServerCartItem(item);
        } else {
            // Chưa có phiên -> Lưu giỏ RAM
            const existing = localCart.find(it => it.productId === item.productId && it.note === item.note);
            if (existing) {
                existing.quantity += item.quantity;
            } else {
                localCart.push(item);
            }
            saveLocalCart();
            updateCartBadge();
            MilkTeaApi.showToast(`Đã thêm ${item.productName} vào giỏ`, 'success');
        }
    }

    async function loadServerCart() {
        if (!window.MilkTeaContext.sessionId) return;
        try {
            const cartData = await MilkTeaApi.get(`/api/v1/table-sessions/${window.MilkTeaContext.sessionId}/cart`);
            if (cartData) {
                window.MilkTeaContext.cartVersion = cartData.version;
                serverCart = cartData.items || [];
                updateCartBadge();
                if (typeof renderCartItems === 'function') {
                    renderCartItems(serverCart, cartData.totalAmount);
                }
            }
        } catch (e) {
            console.warn('Lỗi khi tải giỏ hàng server:', e);
        }
    }

    async function addServerCartItem(newItem) {
        const itemsToSend = [...serverCart.map(it => ({
            productId: it.productId,
            quantity: it.quantity,
            note: it.note
        }))];

        const existing = itemsToSend.find(it => it.productId === newItem.productId && it.note === newItem.note);
        if (existing) {
            existing.quantity += newItem.quantity;
        } else {
            itemsToSend.push({
                productId: newItem.productId,
                quantity: newItem.quantity,
                note: newItem.note
            });
        }

        try {
            const ifMatch = `"${window.MilkTeaContext.cartVersion}"`;
            const updatedCart = await MilkTeaApi.put(
                `/api/v1/table-sessions/${window.MilkTeaContext.sessionId}/cart`,
                { items: itemsToSend },
                { 'If-Match': ifMatch }
            );
            window.MilkTeaContext.cartVersion = updatedCart.version;
            serverCart = updatedCart.items || [];
            updateCartBadge();
            MilkTeaApi.showToast(`Đã thêm ${newItem.productName} vào giỏ hàng`, 'success');
        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Lỗi khi cập nhật giỏ hàng', 'error');
            await loadServerCart(); // Tải lại bản mới nếu bị xung đột version
        }
    }

    async function updateQuantity(index, delta) {
        const currentItems = window.MilkTeaContext.sessionId ? serverCart : localCart;
        if (!currentItems[index]) return;

        const newQty = currentItems[index].quantity + delta;
        if (newQty <= 0) {
            currentItems.splice(index, 1);
        } else {
            currentItems[index].quantity = newQty;
        }

        if (window.MilkTeaContext.sessionId) {
            const itemsToSend = currentItems.map(it => ({
                productId: it.productId,
                quantity: it.quantity,
                note: it.note
            }));
            try {
                const ifMatch = `"${window.MilkTeaContext.cartVersion}"`;
                const updatedCart = await MilkTeaApi.put(
                    `/api/v1/table-sessions/${window.MilkTeaContext.sessionId}/cart`,
                    { items: itemsToSend },
                    { 'If-Match': ifMatch }
                );
                window.MilkTeaContext.cartVersion = updatedCart.version;
                serverCart = updatedCart.items || [];
            } catch (e) {
                MilkTeaApi.showToast('Không thể cập nhật giỏ hàng: ' + e.message, 'error');
                await loadServerCart();
            }
        } else {
            saveLocalCart();
        }

        updateCartBadge();
        if (typeof renderCartItems === 'function') {
            const total = currentItems.reduce((acc, it) => acc + (it.price || it.unitPrice || 0) * it.quantity, 0);
            renderCartItems(currentItems, total);
        }
    }

    function updateCartBadge() {
        const badge = document.getElementById('cartCountBadge');
        if (!badge) return;
        const currentItems = window.MilkTeaContext.sessionId ? serverCart : localCart;
        const count = currentItems.reduce((acc, it) => acc + it.quantity, 0);
        if (count > 0) {
            badge.textContent = count;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }

    // Đặt đơn hàng
    async function submitOrder() {
        const currentItems = window.MilkTeaContext.sessionId ? serverCart : localCart;
        if (currentItems.length === 0) {
            MilkTeaApi.showToast('Giỏ hàng đang trống!', 'warning');
            return;
        }

        const btn = document.getElementById('checkoutBtn');
        if (btn) btn.disabled = true;

        try {
            const items = currentItems.map(it => ({
                productId: it.productId,
                quantity: it.quantity,
                note: it.note
            }));

            const hasTableContext = !!window.MilkTeaContext.tableId;
            const payload = hasTableContext
                ? {
                    tableId: window.MilkTeaContext.tableId,
                    expectedSessionId: window.MilkTeaContext.sessionId,
                    items: items,
                    remainingCartItems: []
                }
                : {
                    items: items
                };

            const order = await MilkTeaApi.post(
                hasTableContext ? '/api/v1/orders/table' : '/api/v1/orders/counter',
                payload
            );
            MilkTeaApi.showToast('Đặt đơn thành công! Đang chuyển hướng...', 'success');

            // Cập nhật context phiên nếu là đơn đầu tiên mở phiên
            if (order.sessionId) {
                window.MilkTeaContext.sessionId = order.sessionId;
            }
            if (order.guestTableToken) {
                window.MilkTeaContext.tableToken = order.guestTableToken;
            }
            if (order.guestCounterToken) {
                window.MilkTeaContext.counterToken = order.guestCounterToken;
                sessionStorage.setItem('milktea.counterToken', order.guestCounterToken);
            }

            // Xóa giỏ hàng
            clearLocalCart();
            serverCart = [];
            updateCartBadge();

            // Chuyển hướng sang trang theo dõi đơn hàng
            const qrParam = window.MilkTeaContext.qrCode ? `?qr=${encodeURIComponent(window.MilkTeaContext.qrCode)}` : '';
            setTimeout(() => {
                window.location.href = '/orders' + qrParam + (qrParam ? '&' : '?') + 'invoice=' + order.orderId;
            }, 800);

        } catch (e) {
            MilkTeaApi.showToast(e.message || 'Không thể tạo đơn hàng', 'error');
            if (btn) btn.disabled = false;
        }
    }

    return {
        initContext,
        addToCart,
        updateQuantity,
        submitOrder,
        loadServerCart,
        getLocalCart: () => localCart,
        getServerCart: () => serverCart
    };
})();
