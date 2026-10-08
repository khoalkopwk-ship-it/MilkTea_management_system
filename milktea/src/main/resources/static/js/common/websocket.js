/**
 * MilkTea Realtime STOMP over SockJS Client
 * Tuân thủ realtime_rule.md và frontend_rule.md
 */
const MilkTeaRealtime = (() => {
    let stompClient = null;
    let isConnected = false;
    let reconnectAttempts = 0;
    const maxReconnectAttempts = 8;
    const processedEvents = new Set();
    const eventListeners = new Map(); // type -> array of callbacks
    const reconnectCallbacks = [];
    const activeSubscriptions = [];

    function connect(headers = {}) {
        if (isConnected || (stompClient && stompClient.connected)) {
            return;
        }

        console.log('Đang kết nối WebSocket STOMP tới /ws...');
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Tắt debug log rác trong console

        // Đính kèm token guest nếu có
        const connectHeaders = { ...headers };
        if (window.MilkTeaContext) {
            if (window.MilkTeaContext.tableToken) {
                connectHeaders['X-Table-Session-Token'] = window.MilkTeaContext.tableToken;
            }
            if (window.MilkTeaContext.counterToken) {
                connectHeaders['X-Counter-Order-Token'] = window.MilkTeaContext.counterToken;
            }
        }

        stompClient.connect(connectHeaders, onConnected, onError);
    }

    function onConnected(frame) {
        console.log('WebSocket STOMP đã kết nối thành công!');
        isConnected = true;
        reconnectAttempts = 0;

        // Tự động subscribe theo vai trò / ngữ cảnh
        setupContextSubscriptions();

        // Chạy các callback khi reconnect để re-fetch snapshot chuẩn từ server
        reconnectCallbacks.forEach(cb => {
            try { cb(); } catch (e) { console.error('Lỗi khi chạy reconnect callback:', e); }
        });
    }

    function onError(error) {
        console.warn('Lỗi kết nối WebSocket STOMP:', error);
        isConnected = false;

        // Thử kết nối lại với exponential backoff có giới hạn
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            const timeout = Math.min(1000 * Math.pow(1.5, reconnectAttempts), 15000);
            console.log(`Sẽ kết nối lại sau ${(timeout / 1000).toFixed(1)}s (lần ${reconnectAttempts}/${maxReconnectAttempts})...`);
            setTimeout(() => {
                connect();
            }, timeout);
        } else {
            console.warn('Đã đạt giới hạn số lần kết nối lại WebSocket.');
        }
    }

    function setupContextSubscriptions() {
        if (!stompClient || !isConnected) return;

        // 1. Topic bàn nếu có sessionId
        if (window.MilkTeaContext && window.MilkTeaContext.sessionId) {
            subscribe(`/topic/table-sessions/${window.MilkTeaContext.sessionId}`);
        }

        // 2. Topic nhân viên nếu có role
        if (window.MilkTeaContext && window.MilkTeaContext.role) {
            const role = window.MilkTeaContext.role;
            if (role === 'CASHIER' || role === 'ADMIN') {
                subscribe('/topic/cashier');
            }
            if (role === 'KITCHEN' || role === 'ADMIN') {
                subscribe('/topic/kitchen');
                subscribe('/topic/stock');
            }
            if (role === 'ADMIN') {
                subscribe('/topic/admin');
            }
        }
    }

    function subscribe(destination) {
        if (!stompClient || !isConnected) return;

        // Tránh trùng lặp subscription
        if (activeSubscriptions.some(s => s.destination === destination)) {
            return;
        }

        try {
            const sub = stompClient.subscribe(destination, (message) => {
                handleIncomingMessage(message);
            });
            activeSubscriptions.push({ destination, sub });
            console.log(`Đã subscribe kênh: ${destination}`);
        } catch (e) {
            console.error(`Không thể subscribe kênh ${destination}:`, e);
        }
    }

    function handleIncomingMessage(message) {
        try {
            const event = JSON.parse(message.body);
            if (!event || !event.eventId) return;

            // Deduplication eventId
            if (processedEvents.has(event.eventId)) {
                return;
            }
            processedEvents.add(event.eventId);
            if (processedEvents.size > 200) {
                // Giữ bộ nhớ RAM nhỏ gọn
                const firstVal = processedEvents.values().next().value;
                processedEvents.delete(firstVal);
            }

            console.log(`[Realtime Event] ${event.type}:`, event);

            // Xử lý sự kiện đặc biệt TABLE_SESSION_CLOSED
            if (event.type === 'TABLE_SESSION_CLOSED') {
                handleSessionClosed();
            }

            // Gọi các listener đã đăng ký
            const listeners = eventListeners.get(event.type) || [];
            listeners.forEach(cb => {
                try { cb(event); } catch (e) { console.error('Lỗi khi xử lý listener:', e); }
            });

            // Gọi các listener '*' (bắt tất cả sự kiện)
            const allListeners = eventListeners.get('*') || [];
            allListeners.forEach(cb => {
                try { cb(event); } catch (e) { console.error('Lỗi khi xử lý listener *:', e); }
            });

        } catch (e) {
            console.error('Không thể phân tích gói tin STOMP:', e);
        }
    }

    function handleSessionClosed() {
        console.warn('Nhận sự kiện TABLE_SESSION_CLOSED: Phiên bàn đã kết thúc!');
        if (window.MilkTeaContext) {
            window.MilkTeaContext.sessionId = null;
            window.MilkTeaContext.tableToken = null;
        }

        // Hiển thị modal hoặc thông báo phiên kết thúc
        const modalEl = document.getElementById('sessionClosedModal');
        if (modalEl) {
            const modal = new bootstrap.Modal(modalEl);
            modal.show();
        } else {
            MilkTeaApi.showToast('Phiên bàn đã kết thúc. Cảm ơn quý khách!', 'warning');
        }

        // Ngắt các subscription của phiên cũ
        disconnect();
    }

    function on(eventType, callback) {
        if (!eventListeners.has(eventType)) {
            eventListeners.set(eventType, []);
        }
        eventListeners.get(eventType).push(callback);
    }

    function onReconnect(callback) {
        reconnectCallbacks.push(callback);
    }

    function disconnect() {
        if (stompClient) {
            try {
                stompClient.disconnect(() => {
                    console.log('WebSocket STOMP đã ngắt kết nối.');
                });
            } catch (e) {}
        }
        isConnected = false;
        activeSubscriptions.length = 0;
    }

    return {
        connect,
        disconnect,
        subscribe,
        on,
        onReconnect
    };
})();
