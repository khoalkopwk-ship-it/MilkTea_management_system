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

        // Nếu URL không có, thử phục hồi từ cookie hoặc localStorage
        if (!qr && !table) {
            const cookies = document.cookie.split(';');
            for (let c of cookies) {
                const parts = c.trim().split('=');
                if (parts[0] === 'MILKTEA_TABLE_QR' && parts[1]) qr = decodeURIComponent(parts[1]);
                else if (parts[0] === 'MILKTEA_TABLE_ID' && parts[1]) table = decodeURIComponent(parts[1]);
            }
            if (!qr) qr = localStorage.getItem('milktea.lastTableQr');
            if (!table) table = localStorage.getItem('milktea.lastTableId');
        }

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

                    if (ctx.qrCode) localStorage.setItem('milktea.lastTableQr', ctx.qrCode);
                    if (ctx.tableId) localStorage.setItem('milktea.lastTableId', ctx.tableId);

                    // Cập nhật nhãn bàn trên navbar nếu đang ẩn
                    const badgeContainer = document.getElementById('navbarTableBadgeContainer');
                    const nameEl = document.getElementById('navbarTableName');
                    if (badgeContainer && nameEl && ctx.tableName) {
                        nameEl.textContent = ctx.tableName;
                        badgeContainer.style.display = 'flex';
                        badgeContainer.classList.remove('d-none');
                    }

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
    function addToCart(product, quantity = 1, note = '', selectedToppings = []) {
        const toppingIds = (selectedToppings || []).map(t => Number(t.id || t.productId)).filter(Boolean).sort((a, b) => a - b);
        const toppingSum = (selectedToppings || []).reduce((acc, t) => acc + (Number(t.price) || 0), 0);
        const unitPriceWithTopping = (Number(product.price) || 0) + toppingSum;
        const lineTotal = unitPriceWithTopping * parseInt(quantity);

        const item = {
            productId: product.id,
            productName: product.name,
            size: product.size,
            price: product.price,
            quantity: parseInt(quantity),
            note: note || '',
            toppingIds: toppingIds,
            toppings: selectedToppings || [],
            lineTotal: lineTotal
        };

        if (window.MilkTeaContext.sessionId) {
            // Đã có phiên -> Gửi lên giỏ server
            addServerCartItem(item);
        } else {
            // Chưa có phiên -> Lưu giỏ RAM, chỉ gộp nếu cùng productId, note VÀ cùng toppings
            const tKey = toppingIds.join(',');
            const existing = localCart.find(it =>
                it.productId === item.productId &&
                (it.note || '') === (item.note || '') &&
                ((it.toppingIds || []).slice().sort((a, b) => a - b).join(',')) === tKey
            );
            if (existing) {
                existing.quantity += item.quantity;
                const existingToppingSum = (existing.toppings || []).reduce((acc, t) => acc + (Number(t.price) || 0), 0);
                existing.lineTotal = (existing.price + existingToppingSum) * existing.quantity;
            } else {
                localCart.push(item);
            }
            saveLocalCart();
            updateCartBadge();
            MilkTeaApi.showToast(`Đã thêm ${item.productName} vào giỏ`, 'success');
        }
    }

    function extractToppingIds(item) {
        if (!item) return [];
        if (Array.isArray(item.toppingIds) && item.toppingIds.length > 0) {
            return item.toppingIds.map(Number).filter(Boolean);
        }
        if (Array.isArray(item.toppings) && item.toppings.length > 0) {
            return item.toppings.map(t => Number(t.id || t.productId)).filter(Boolean);
        }
        return [];
    }

    async function loadServerCart() {
        if (!window.MilkTeaContext.sessionId) return;
        try {
            const cartData = await MilkTeaApi.get(`/api/v1/table-sessions/${window.MilkTeaContext.sessionId}/cart`);
            if (cartData) {
                window.MilkTeaContext.cartVersion = cartData.version;
                serverCart = (cartData.items || []).map(it => ({
                    ...it,
                    toppingIds: extractToppingIds(it)
                }));
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
            note: it.note,
            toppingIds: extractToppingIds(it)
        }))];

        const newTKey = extractToppingIds(newItem).slice().sort((a, b) => a - b).join(',');
        const existing = itemsToSend.find(it =>
            it.productId === newItem.productId &&
            (it.note || '') === (newItem.note || '') &&
            (it.toppingIds || []).slice().sort((a, b) => a - b).join(',') === newTKey
        );
        if (existing) {
            existing.quantity += newItem.quantity;
        } else {
            itemsToSend.push({
                productId: newItem.productId,
                quantity: newItem.quantity,
                note: newItem.note,
                toppingIds: extractToppingIds(newItem)
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
            serverCart = (updatedCart.items || []).map(it => ({
                ...it,
                toppingIds: extractToppingIds(it)
            }));
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
            const toppingSum = (currentItems[index].toppings || []).reduce((acc, t) => acc + (Number(t.price) || 0), 0);
            currentItems[index].lineTotal = ((currentItems[index].price || currentItems[index].unitPrice || 0) + toppingSum) * newQty;
        }

        if (window.MilkTeaContext.sessionId) {
            const itemsToSend = currentItems.map(it => ({
                productId: it.productId,
                quantity: it.quantity,
                note: it.note,
                toppingIds: extractToppingIds(it)
            }));
            try {
                const ifMatch = `"${window.MilkTeaContext.cartVersion}"`;
                const updatedCart = await MilkTeaApi.put(
                    `/api/v1/table-sessions/${window.MilkTeaContext.sessionId}/cart`,
                    { items: itemsToSend },
                    { 'If-Match': ifMatch }
                );
                window.MilkTeaContext.cartVersion = updatedCart.version;
                serverCart = (updatedCart.items || []).map(it => ({
                    ...it,
                    toppingIds: extractToppingIds(it)
                }));
            } catch (e) {
                MilkTeaApi.showToast('Không thể cập nhật giỏ hàng: ' + e.message, 'error');
                await loadServerCart();
            }
        } else {
            saveLocalCart();
        }

        updateCartBadge();
        if (typeof renderCartItems === 'function') {
            const total = currentItems.reduce((acc, it) => acc + (it.lineTotal || (it.price || it.unitPrice || 0) * it.quantity), 0);
            renderCartItems(currentItems, total);
        }
    }

    async function removeItem(index) {
        await updateQuantity(index, -999999);
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
                note: it.note,
                toppingIds: extractToppingIds(it)
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

            // Xóa giỏ hàng chỉ khi tạo đơn thành công
            clearLocalCart();
            serverCart = [];
            updateCartBadge();

            // Chuyển hướng sang trang theo dõi đơn hàng
            const qrParam = window.MilkTeaContext.qrCode ? `?qr=${encodeURIComponent(window.MilkTeaContext.qrCode)}` : '';
            setTimeout(() => {
                window.location.href = '/orders' + qrParam + (qrParam ? '&' : '?') + 'invoice=' + order.orderId;
            }, 800);

        } catch (e) {
            // Giữ giỏ hàng và hiển thị lỗi, không xóa giỏ hay chuyển trang
            MilkTeaApi.showToast(e.message || 'Không thể tạo đơn hàng', 'error');
            if (btn) btn.disabled = false;
        }
    }

    return {
        initContext,
        addToCart,
        updateQuantity,
        removeItem,
        submitOrder,
        loadServerCart,
        getLocalCart: () => localCart,
        getServerCart: () => serverCart
    };
})();
