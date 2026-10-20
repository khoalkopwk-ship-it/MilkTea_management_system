
(() => {
    'use strict';

    const $ = id => document.getElementById(id);

    const money = value =>
        new Intl.NumberFormat('vi-VN', {
            maximumFractionDigits: 0
        }).format(Number(value) || 0) + 'đ';

    const number = value =>
        new Intl.NumberFormat('vi-VN').format(Number(value) || 0);

    const dateVN = str =>
        str
            ? str.substring(8, 10) + '/' +
              str.substring(5, 7) + '/' +
              str.substring(0, 4)
            : '—';

    const dateLocal = dt =>
        `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`;

    const palette = [
        '#F3BC98',
        '#ADD1E8',
        '#F2D58D',
        '#CCB8E2',
        '#B5DCC5',
        '#DDB2AD',
        '#BAC5D2',
        '#DDC9AE'
    ];

    let requestIndex = 0;

    function initializeDateRange() {
        const today = new Date();
        const start = new Date(
            today.getFullYear(),
            today.getMonth(),
            today.getDate()
        );

        start.setDate(start.getDate() - 6);

        $('filterFromDate').value = dateLocal(start);
        $('filterToDate').value = dateLocal(today);

        load();
    }

    function renderRevenue(rows) {
        const svg = $('dashboardRevenueChart');
        const NS = 'http://www.w3.org/2000/svg';

        svg.replaceChildren();

        const make = (tag, attrs, text) => {
            const el = document.createElementNS(NS, tag);

            Object.entries(attrs).forEach(([k, v]) =>
                el.setAttribute(k, String(v))
            );

            if (text !== undefined) {
                el.textContent = text;
            }

            svg.appendChild(el);
            return el;
        };

        if (!rows.length) {
            make('text', {
                x: 360,
                y: 145,
                'text-anchor': 'middle',
                fill: '#8391a4',
                'font-size': 15
            }, 'Chưa có dữ liệu');
            return;
        }

        const L = 70;
        const R = 695;
        const T = 23;
        const B = 245;
        const W = R - L;
        const H = B - T;

        const vals = rows.flatMap(d => [
            Number(d.received) || 0,
            Number(d.refunded) || 0,
            Number(d.netRevenue) || 0
        ]);

        const max = Math.max(1, ...vals, 0);
        const min = Math.min(0, ...vals);
        const range = max - min || 1;

        const y = v => B - (v - min) / range * H;

        const x = i =>
            rows.length === 1
                ? (L + R) / 2
                : L + i * (W / (rows.length - 1));

        for (let i = 0; i <= 4; i++) {
            const v = min + range * i / 4;
            const yy = y(v);

            make('line', {
                x1: L,
                y1: yy,
                x2: R,
                y2: yy,
                stroke: '#e8edf4',
                'stroke-dasharray': '4 5'
            });

            make('text', {
                x: L - 9,
                y: yy + 4,
                'text-anchor': 'end',
                fill: '#8b98aa',
                'font-size': 11
            }, Math.abs(v) >= 1000000
                ? (v / 1000000).toFixed(1) + 'tr'
                : Math.round(v / 1000) + 'k');
        }

        if (min < 0 && max > 0) {
            make('line', {
                x1: L,
                y1: y(0),
                x2: R,
                y2: y(0),
                stroke: '#8a97a8',
                'stroke-width': '1.4'
            });
        }

        const fields = [
            ['received', '#FFF176'],
            ['refunded', '#c08e38'],
            ['netRevenue', '#72c7d6']
        ];

        fields.forEach(([field, color]) => {
            const path = rows.map((r, i) =>
                `${i ? 'L' : 'M'}${x(i).toFixed(1)} ${y(Number(r[field]) || 0).toFixed(1)}`
            ).join(' ');

            make('path', {
                d: path,
                stroke: color,
                'stroke-width': 2.8,
                'stroke-linecap': 'round',
                'stroke-linejoin': 'round',
                fill: 'none'
            });

            if (rows.length <= 22) {
                rows.forEach((r, i) => {
                    const circle = make('circle', {
                        cx: x(i),
                        cy: y(Number(r[field]) || 0),
                        r: 3.5,
                        fill: '#fff',
                        stroke: color,
                        'stroke-width': 2
                    });

                    const title = document.createElementNS(NS, 'title');
                    title.textContent = `${dateVN(r.date)}: ${money(r[field])}`;
                    circle.appendChild(title);
                });
            }
        });

        const step = Math.max(1, Math.ceil(rows.length / 7));

        rows.forEach((r, i) => {
            if (i % step === 0 || i === rows.length - 1) {
                make('text', {
                    x: x(i),
                    y: B + 21,
                    'text-anchor': 'middle',
                    fill: '#8290a3',
                    'font-size': 11
                }, r.date.substring(8, 10) + '/' + r.date.substring(5, 7));
            }
        });
    }

    function renderStatuses(rows) {
        const total = rows.reduce(
            (v, r) => v + (Number(r.count) || 0),
            0
        );

        $('dashboardStatusTotal').textContent = number(total);

        const legend = $('dashboardStatusLegend');
        legend.replaceChildren();

        let pct = 0;
        const slices = [];

        rows.forEach((r, i) => {
            const color = palette[i % palette.length];
            const share = Number(r.count) / total * 100;

            slices.push(`${color} ${pct}% ${pct + share}%`);
            pct += share;

            const item = document.createElement('div');
            item.className = 'dashboard-status-row';

            const left = document.createElement('span');
            const dot = document.createElement('span');
            const label = document.createElement('span');
            const count = document.createElement('strong');

            dot.className = 'dashboard-status-dot';
            dot.style.background = color;

            label.textContent = r.statusLabel || r.status;
            count.textContent =
                number(r.count) +
                ` (${total ? (Number(r.count) / total * 100).toFixed(1) : 0}%)`;

            left.append(dot, label);
            item.append(left, count);
            legend.append(item);
        });

        $('dashboardStatusDonut').style.background =
            slices.length
                ? `conic-gradient(${slices.join(',')})`
                : '#eaf0f5';

        if (!total) {
            legend.textContent = 'Chưa có đơn hàng trong kỳ';
        }
    }

    function renderRankings(id, rows) {
        const box = $(id);

        box.replaceChildren();
        box.classList.remove('waste');

        if (!rows.length) {
            const empty = document.createElement('div');
            empty.className = 'dashboard-empty';
            empty.textContent = 'Chưa có dữ liệu trong khoảng ngày chọn';
            box.append(empty);
            return;
        }

        const largest = Math.max(
            1,
            ...rows.map(r => Number(r.quantitySold) || 0)
        );

        rows.forEach((r, i) => {
            const value = Number(r.quantitySold) || 0;

            const item = document.createElement('div');
            item.className = 'dashboard-rank';

            const top = document.createElement('div');
            top.className = 'dashboard-rank-meta';

            const title = document.createElement('div');
            title.className = 'dashboard-rank-title';
            title.title = r.productName;

            const order = document.createElement('small');
            order.textContent = String(i + 1).padStart(2, '0');

            title.append(order, document.createTextNode(r.productName));

            const amount = document.createElement('span');
            amount.className = 'dashboard-rank-value';
            amount.textContent = number(value) + ' món';

            top.append(title, amount);

            const bar = document.createElement('div');
            bar.className = 'dashboard-rank-track';

            const fill = document.createElement('div');
            fill.className = 'dashboard-rank-fill';
            fill.style.width = (value / largest * 100) + '%';

            bar.append(fill);
            item.append(top, bar);
            box.append(item);
        });
    }

    function renderWaste(rows) {
        const svg = $('dashboardTopWaste');
        const NS = 'http://www.w3.org/2000/svg';

        svg.replaceChildren();

        const make = (tag, attrs, text) => {
            const el = document.createElementNS(NS, tag);

            Object.entries(attrs).forEach(([k, v]) =>
                el.setAttribute(k, String(v))
            );

            if (text !== undefined) {
                el.textContent = text;
            }

            svg.appendChild(el);
            return el;
        };

        if (!rows.length) {
            make('text', {
                x: 240,
                y: 150,
                'text-anchor': 'middle',
                fill: '#7b8798',
                'font-size': 14
            }, 'Chưa có hao hụt trong kỳ');
            return;
        }

        const max = Math.max(
            1,
            ...rows.map(r => Number(r.lossCost) || 0)
        );

        const L = 60;
        const R = 465;
        const T = 35;
        const B = 245;

        for (let i = 0; i <= 4; i++) {
            const value = max * i / 4;
            const y = B - (B - T) * i / 4;

            make('line', {
                x1: L,
                x2: R,
                y1: y,
                y2: y,
                stroke: '#e8edf4'
            });

            make('text', {
                x: L - 8,
                y: y + 4,
                'text-anchor': 'end',
                fill: '#7b8798',
                'font-size': 11
            }, value >= 1000000
                ? (value / 1000000).toFixed(1) + 'tr'
                : Math.round(value / 1000) + 'k');
        }

        const slot = (R - L) / rows.length;

        rows.forEach((r, i) => {
            const value = Number(r.lossCost) || 0;
            const h = value / max * (B - T);
            const x = L + slot * i + slot * .18;

            const bar = make('rect', {
                x,
                y: B - h,
                width: slot * .64,
                height: h,
                rx: 6,
                fill: [
                    '#ff9945',
                    '#ffb052',
                    '#fb7b83',
                    '#fbc65d',
                    '#ffad73'
                ][i % 5]
            });

            const title = document.createElementNS(NS, 'title');
            title.textContent =
                `${r.materialName}: ${money(value)} · ${number(r.discardedQuantity)} ${r.unit || ''}`;

            bar.appendChild(title);

            make('text', {
                x: x + slot * .32,
                y: B - h - 9,
                'text-anchor': 'middle',
                fill: '#223149',
                'font-size': 11
            }, value >= 1000000
                ? (value / 1000000).toFixed(1) + 'tr'
                : Math.round(value / 1000) + 'k');

            const label = make('text', {
                x: x + slot * .32,
                y: B + 24,
                'text-anchor': 'middle',
                fill: '#53647b',
                'font-size': 11
            });

            const words = (r.materialName || 'Nguyên liệu').split(/\s+/);
            let line = '';
            const lines = [];

            words.forEach(word => {
                if ((line + ' ' + word).trim().length > 13 && line) {
                    lines.push(line);
                    line = word;
                } else {
                    line = (line + ' ' + word).trim();
                }
            });

            if (line) {
                lines.push(line);
            }

            lines.slice(0, 3).forEach((line, j) => {
                const span = document.createElementNS(NS, 'tspan');
                span.setAttribute('x', x + slot * .32);
                span.setAttribute('dy', j ? 14 : 0);
                span.textContent =
                    j === 2 && lines.length > 3 ? line + '…' : line;

                label.appendChild(span);
            });
        });
    }

    function renderStockAlerts(items) {
        const outEl = $('dashboardOutStockList');
        const lowEl = $('dashboardLowStockList');

        outEl.replaceChildren();
        lowEl.replaceChildren();

        const outItems = items.filter(
            item => item.status === 'HET_HANG'
        );

        const lowItems = items.filter(
            item => item.status === 'SAP_HET'
        );

        const renderGroup = (target, rows, type) => {
            if (!rows.length) {
                const msg = document.createElement('div');
                msg.className = 'dashboard-empty';
                msg.textContent =
                    type === 'out'
                        ? 'Không có nguyên liệu hết hàng.'
                        : 'Không có nguyên liệu sắp hết.';

                target.append(msg);
                return;
            }

            rows.forEach(item => {
                const row = document.createElement('div');
                row.className = 'dashboard-alert-item';

                const main = document.createElement('div');
                main.className = 'dashboard-alert-main';

                const name = document.createElement('strong');
                name.textContent = item.materialName || 'Nguyên liệu';
                name.title = item.materialName || '';

                const location = document.createElement('small');
                location.textContent =
                    item.location === 'BEP'
                        ? 'Kệ bếp'
                        : item.location === 'KHO'
                            ? 'Kho lớn'
                            : (item.location || '');

                main.append(name, location);

                const qty = document.createElement('div');
                qty.className = 'dashboard-alert-qty';
                qty.textContent =
                    'Tồn: ' +
                    number(item.quantity) +
                    ' ' +
                    (item.unit || '');

                if (item.status === 'SAP_HET' && item.threshold != null) {
                    qty.title =
                        'Ngưỡng cảnh báo: ' +
                        number(item.threshold) +
                        ' ' +
                        (item.unit || '');
                }

                const badge = document.createElement('span');
                badge.className =
                    'dashboard-alert-badge ' +
                    (type === 'out' ? 'out' : 'low');

                badge.textContent =
                    type === 'out' ? 'Hết hàng' : 'Cần nhập';

                row.append(main, qty, badge);
                target.append(row);
            });
        };

        renderGroup(outEl, outItems, 'out');
        renderGroup(lowEl, lowItems, 'low');
    }

    const dashboardMotionOff = () =>
        window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function animateDashboardCharts() {
        if (dashboardMotionOff()) return;

        const revenue = $('dashboardRevenueChart');

        if (revenue) {
            revenue.querySelectorAll('path[fill="none"]').forEach(path => {
                if (typeof path.getTotalLength !== 'function') return;

                const length = path.getTotalLength();

                if (!Number.isFinite(length) || length <= 0) return;

                path.style.setProperty(
                    '--dashboard-line-length',
                    `${length}px`
                );

                path.classList.add('dashboard-animate-line');
            });
        }

        const waste = $('dashboardTopWaste');

        if (waste) {
            waste.querySelectorAll('rect').forEach((bar, index) => {
                bar.style.setProperty(
                    '--dashboard-delay',
                    `${Math.min(index, 4) * 80}ms`
                );

                bar.classList.add('dashboard-animate-bar');
            });
        }

        const donut = $('dashboardStatusDonut');
        const statusTotalText =
            $('dashboardStatusTotal')?.textContent || '';

        if (
            donut &&
            Number(statusTotalText.replace(/\D/g, '')) > 0
        ) {
            donut.classList.remove('dashboard-animate-donut');
            void donut.offsetWidth;
            donut.classList.add('dashboard-animate-donut');
        }

        const rankings = $('dashboardTopProducts');

        if (rankings) {
            rankings.querySelectorAll('.dashboard-rank-fill').forEach(
                (bar, index) => {
                    bar.style.setProperty(
                        '--dashboard-delay',
                        `${Math.min(index, 4) * 80}ms`
                    );

                    bar.classList.add('dashboard-animate-rank');
                }
            );
        }
    }

    function render(data) {
        const summary = data.summary || {};
        const alerts = data.alerts || {};

        $('dashboardRangeText').textContent =
            dateVN(data.fromDate) + ' – ' + dateVN(data.toDate);

        $('dashboardTotalOrders').textContent = number(summary.totalOrders);
        $('dashboardNetRevenue').textContent = money(summary.netRevenue);
        $('dashboardReceived').textContent = money(summary.received);
        $('dashboardRefunded').textContent = money(summary.refunded);
        $('dashboardUnpaidOrders').textContent = number(summary.unpaidOrders);
        $('dashboardUnpaidAmount').textContent = money(summary.unpaidAmount);
        $('dashboardWasteCost').textContent = money(summary.wasteCost);

        $('dashboardWasteWarning').classList.toggle(
            'd-none',
            !(Number(summary.unpricedWasteCount) > 0)
        );

        $('dashboardOutStock').textContent = number(alerts.outOfStockCount);
        $('dashboardLowStock').textContent = number(alerts.lowStockCount);

        renderStockAlerts(alerts.stockItems || []);

        $('dashboardPendingRefund').textContent =
            number(alerts.pendingRefundCount);

        $('dashboardLastSettlement').textContent =
            alerts.lastSettlementAt
                ? dateVN(alerts.lastSettlementAt) + ' ' +
                  alerts.lastSettlementAt.substring(11, 16)
                : 'Chưa có';

        renderRevenue(data.revenueByDay || []);
        renderStatuses(data.ordersByStatus || []);
        renderRankings('dashboardTopProducts', data.topProducts || []);
        renderWaste(data.topWasteMaterials || []);

        animateDashboardCharts();
    }

    async function load() {
        const from = $('filterFromDate').value;
        const to = $('filterToDate').value;
        const error = $('dashboardError');

        if (!from || !to || from > to) {
            error.textContent =
                'Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.';
            error.classList.remove('d-none');
            return;
        }

        const current = ++requestIndex;

        error.classList.add('d-none');
        $('dashboardFilterButton').disabled = true;

        try {
            const data = await MilkTeaApi.get(
                '/api/v1/admin/reports/dashboard' +
                '?fromDate=' + encodeURIComponent(from) +
                '&toDate=' + encodeURIComponent(to)
            );

            if (current === requestIndex) {
                if (!data || typeof data !== 'object' || !data.summary) {
                    throw new Error(
                        'API không trả dữ liệu báo cáo hợp lệ. Kiểm tra phiên đăng nhập và phản hồi máy chủ.'
                    );
                }

                render(data);
            }
        } catch (e) {
            if (current === requestIndex) {
                error.textContent =
                    'Không tải được Dashboard: ' +
                    (e.message || 'Lỗi kết nối');
                error.classList.remove('d-none');
            }
        } finally {
            if (current === requestIndex) {
                $('dashboardFilterButton').disabled = false;
            }
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        try {
            if (typeof AdminApp !== 'undefined') {
                Promise.resolve(AdminApp.init()).catch(e =>
                    console.warn('Realtime admin chưa sẵn sàng:', e)
                );
            }
        } catch (e) {
            console.warn('Realtime admin chưa sẵn sàng:', e);
        }

        $('dashboardFilterForm').addEventListener('submit', e => {
            e.preventDefault();
            load();
        });

        initializeDateRange();
    });
})();
