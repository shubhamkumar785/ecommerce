(() => {
    const state = {
        charts: {},
        analytics: {
            stats: null,
            states: [],
            userGrowth: [],
            revenue: [],
            orders: []
        },
        tables: {
            users: { role: "ROLE_USER", page: 0, size: 10, search: "", payload: null },
            sellers: { role: "ROLE_SELLER", page: 0, size: 10, search: "", payload: null }
        }
    };

    const currencyFormatter = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    });
    const numberFormatter = new Intl.NumberFormat("en-IN");
    const body = document.body;
    const toast = document.getElementById("dashboardToast");
    let toastTimer = null;

    document.addEventListener("DOMContentLoaded", init);

    async function init() {
        bindNavigation();
        bindTableControls("users");
        bindTableControls("sellers");
        bindRefresh();
        await loadDashboard();
    }

    function bindNavigation() {
        const navLinks = document.querySelectorAll("[data-section-target]");
        const panels = document.querySelectorAll(".content-panel");
        const sidebar = document.getElementById("sidebar");
        const backdrop = document.getElementById("sidebarBackdrop");
        const menuToggle = document.getElementById("menuToggle");

        navLinks.forEach((link) => {
            link.addEventListener("click", () => {
                const target = link.dataset.sectionTarget;

                navLinks.forEach((item) => item.classList.toggle("is-active", item === link));
                panels.forEach((panel) => panel.classList.toggle("is-active", panel.id === `section-${target}`));

                sidebar.classList.remove("is-open");
                backdrop.classList.remove("is-visible");
            });
        });

        menuToggle?.addEventListener("click", () => {
            sidebar.classList.toggle("is-open");
            backdrop.classList.toggle("is-visible");
        });

        backdrop?.addEventListener("click", () => {
            sidebar.classList.remove("is-open");
            backdrop.classList.remove("is-visible");
        });
    }


    function bindTableControls(key) {
        const searchForm = document.getElementById(`${key}SearchForm`);
        const searchInput = document.getElementById(`${key}SearchInput`);
        const exportButton = document.getElementById(`${key}ExportBtn`);
        const prevButton = document.getElementById(`${key}PrevBtn`);
        const nextButton = document.getElementById(`${key}NextBtn`);

        searchForm?.addEventListener("submit", async (event) => {
            event.preventDefault();
            state.tables[key].page = 0;
            state.tables[key].search = searchInput.value.trim();
            await loadTable(key);
        });

        exportButton?.addEventListener("click", () => {
            const tableState = state.tables[key];
            const params = new URLSearchParams({
                role: tableState.role,
                search: tableState.search
            });
            window.location.href = `/api/admin/export/users.csv?${params.toString()}`;
        });

        prevButton?.addEventListener("click", async () => {
            if (state.tables[key].page > 0) {
                state.tables[key].page -= 1;
                await loadTable(key);
            }
        });

        nextButton?.addEventListener("click", async () => {
            const payload = state.tables[key].payload;
            if (payload && !payload.last) {
                state.tables[key].page += 1;
                await loadTable(key);
            }
        });
    }

    function bindRefresh() {
        const refreshButton = document.getElementById("analyticsRefreshBtn");
        refreshButton?.addEventListener("click", async () => {
            await loadAnalytics();
            showToast("Analytics refreshed", "success");
        });

        document.addEventListener("click", async (event) => {
            const actionButton = event.target.closest("[data-action]");
            if (!actionButton) {
                return;
            }

            const action = actionButton.dataset.action;
            const userId = actionButton.dataset.userId;
            const table = actionButton.dataset.table;

            if (!userId || !table) {
                return;
            }

            if (action === "toggle-status") {
                const enabled = actionButton.dataset.nextEnabled === "true";
                const label = enabled ? "unblock" : "block";
                if (!window.confirm(`Do you want to ${label} this account?`)) {
                    return;
                }

                try {
                    await fetchJson(`/api/admin/users/${userId}/status?enabled=${enabled}`, { method: "PATCH" });
                    showToast("Account status updated", "success");
                    await Promise.all([loadStats(), loadTable(table)]);
                } catch (error) {
                    showToast(error.message, "error");
                }
            }

            if (action === "delete-user") {
                if (!window.confirm("Delete this account permanently?")) {
                    return;
                }

                try {
                    await fetchJson(`/api/admin/users/${userId}`, { method: "DELETE" });
                    showToast("Account deleted", "success");
                    await Promise.all([loadStats(), loadTable(table)]);
                } catch (error) {
                    showToast(error.message, "error");
                }
            }
        });
    }

    async function loadDashboard() {
        try {
            await Promise.all([loadStats(), loadAnalytics(), loadTable("users"), loadTable("sellers")]);
        } catch (error) {
            showToast(error.message, "error");
        }
    }

    async function loadStats() {
        const stats = await fetchJson("/api/admin/stats");
        state.analytics.stats = stats;

        setText("statTotalUsers", numberFormatter.format(stats.totalUsers ?? 0));
        setText("statTotalSellers", numberFormatter.format(stats.totalSellers ?? 0));
        setText("statTotalProducts", numberFormatter.format(stats.totalProducts ?? 0));
        setText("statTotalOrders", numberFormatter.format(stats.totalOrders ?? 0));
        setText("statTotalRevenue", currencyFormatter.format(stats.totalRevenue ?? 0));

        setText("productsBadge", `${numberFormatter.format(stats.totalProducts ?? 0)} Products`);
        setText("productsMetric", numberFormatter.format(stats.totalProducts ?? 0));
        setText("ordersMetric", numberFormatter.format(stats.totalOrders ?? 0));
        setText("revenueMetric", currencyFormatter.format(stats.totalRevenue ?? 0));
        setText("sellersMetric", numberFormatter.format(stats.totalSellers ?? 0));
        setText("analyticsTotalUsers", numberFormatter.format(stats.totalUsers ?? 0));
    }

    async function loadAnalytics() {
        const [states, revenue, userGrowth, orders] = await Promise.all([
            fetchJson("/api/admin/users-by-state"),
            fetchJson("/api/admin/monthly-revenue"),
            fetchJson("/api/admin/user-growth"),
            fetchJson("/api/admin/orders-growth")
        ]);

        state.analytics.states = states;
        state.analytics.revenue = revenue;
        state.analytics.userGrowth = userGrowth;
        state.analytics.orders = orders;

        renderStateTable(states);
        renderInsights(states);
        redrawCharts();
    }

    async function loadTable(key) {
        const tableState = state.tables[key];
        const params = new URLSearchParams({
            role: tableState.role,
            search: tableState.search,
            page: String(tableState.page),
            size: String(tableState.size)
        });

        const payload = await fetchJson(`/api/admin/users?${params.toString()}`);
        tableState.payload = payload;
        renderTable(key, payload);
    }

    function renderTable(key, payload) {
        const tableBody = document.getElementById(`${key}TableBody`);
        const paginationLabel = document.getElementById(`${key}PaginationLabel`);
        const prevButton = document.getElementById(`${key}PrevBtn`);
        const nextButton = document.getElementById(`${key}NextBtn`);
        const rows = payload?.content ?? [];

        if (!rows.length) {
            tableBody.innerHTML = `<tr><td colspan="7" class="table-empty">No records found.</td></tr>`;
        } else {
            tableBody.innerHTML = rows.map((row) => {
                const statusClass = row.enabled ? "is-active" : "is-blocked";
                const statusLabel = row.enabled ? "Active" : "Blocked";
                const toggleLabel = row.enabled ? "Block" : "Unblock";
                const toggleClass = row.enabled ? "table-action--block" : "table-action--unblock";

                return `
                    <tr>
                        <td>${escapeHtml(row.name)}</td>
                        <td>${escapeHtml(row.email)}</td>
                        <td><span class="role-badge">${formatRole(row.role)}</span></td>
                        <td>${escapeHtml(row.state || "Unknown")}</td>
                        <td><span class="status-badge ${statusClass}">${statusLabel}</span></td>
                        <td>${formatDate(row.createdAt)}</td>
                        <td>
                            <div class="table-actions">
                                <button type="button" class="table-action ${toggleClass}" data-action="toggle-status" data-next-enabled="${!row.enabled}" data-user-id="${row.id}" data-table="${key}">${toggleLabel}</button>
                                <button type="button" class="table-action table-action--delete" data-action="delete-user" data-user-id="${row.id}" data-table="${key}">Delete</button>
                            </div>
                        </td>
                    </tr>
                `;
            }).join("");
        }

        const totalPages = Math.max(payload?.totalPages ?? 0, 1);
        const currentPage = (payload?.page ?? 0) + 1;
        paginationLabel.textContent = `Page ${currentPage} of ${totalPages}`;
        prevButton.disabled = payload?.first ?? true;
        nextButton.disabled = payload?.last ?? true;
    }

    function renderStateTable(states) {
        const stateTableBody = document.getElementById("stateTableBody");

        if (!states.length) {
            stateTableBody.innerHTML = `<tr><td colspan="2" class="table-empty">No state analytics available yet.</td></tr>`;
            setText("trackedStates", "0");
            setText("analyticsTrackedStates", "0");
            setText("topStateName", "-");
            setText("topStateCount", "No state data available yet.");
            setText("analyticsTopState", "-");
            return;
        }

        stateTableBody.innerHTML = states.map((row) => `
            <tr>
                <td>${escapeHtml(row.state)}</td>
                <td>${numberFormatter.format(row.count)}</td>
            </tr>
        `).join("");
    }

    function renderInsights(states) {
        const topState = states[0];
        setText("trackedStates", numberFormatter.format(states.length));
        setText("analyticsTrackedStates", numberFormatter.format(states.length));

        if (topState) {
            setText("topStateName", topState.state);
            setText("topStateCount", `${numberFormatter.format(topState.count)} users`);
            setText("analyticsTopState", `${topState.state} (${numberFormatter.format(topState.count)})`);
        } else {
            setText("topStateName", "-");
            setText("topStateCount", "No state data available yet.");
            setText("analyticsTopState", "-");
        }
    }

    function redrawCharts() {
        destroyCharts();
        const chartColors = getChartColors();

        state.charts.userGrowth = createLineChart("userGrowthChart", state.analytics.userGrowth, {
            label: "Users",
            borderColor: chartColors.primary,
            backgroundColor: chartColors.primarySoft
        }, chartColors, false);

        state.charts.revenue = createLineChart("revenueChart", state.analytics.revenue, {
            label: "Revenue",
            borderColor: chartColors.green,
            backgroundColor: "rgba(31, 157, 97, 0.18)"
        }, chartColors, true);

        state.charts.orders = createBarChart("ordersChart", state.analytics.orders, {
            label: "Orders",
            backgroundColor: "rgba(255, 143, 71, 0.42)",
            borderColor: chartColors.orange
        }, chartColors);

        state.charts.states = createBarChart("stateUsersChart", state.analytics.states, {
            label: "Users",
            backgroundColor: "rgba(122, 92, 255, 0.42)",
            borderColor: chartColors.purple
        }, chartColors, "state", "count");
    }

    function createLineChart(canvasId, rows, palette, chartColors, isCurrency) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) {
            return null;
        }

        return new window.Chart(canvas, {
            type: "line",
            data: {
                labels: rows.map((row) => row.label),
                datasets: [{
                    label: palette.label,
                    data: rows.map((row) => row.value),
                    borderColor: palette.borderColor,
                    backgroundColor: palette.backgroundColor,
                    fill: true,
                    tension: 0.35,
                    borderWidth: 3,
                    pointRadius: 4,
                    pointHoverRadius: 5
                }]
            },
            options: buildChartOptions(chartColors, isCurrency)
        });
    }

    function createBarChart(canvasId, rows, palette, chartColors, labelKey = "label", valueKey = "value") {
        const canvas = document.getElementById(canvasId);
        if (!canvas) {
            return null;
        }

        return new window.Chart(canvas, {
            type: "bar",
            data: {
                labels: rows.map((row) => row[labelKey]),
                datasets: [{
                    label: palette.label,
                    data: rows.map((row) => row[valueKey]),
                    borderRadius: 12,
                    backgroundColor: palette.backgroundColor,
                    borderColor: palette.borderColor,
                    borderWidth: 1.5
                }]
            },
            options: buildChartOptions(chartColors, false)
        });
    }

    function buildChartOptions(chartColors, isCurrency) {
        return {
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {
                        color: chartColors.text
                    }
                }
            },
            scales: {
                x: {
                    grid: { color: chartColors.grid },
                    ticks: { color: chartColors.textSoft }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: chartColors.grid },
                    ticks: {
                        color: chartColors.textSoft,
                        callback: (value) => isCurrency ? currencyFormatter.format(value) : numberFormatter.format(value)
                    }
                }
            }
        };
    }

    function getChartColors() {
        const computed = window.getComputedStyle(body);
        return {
            text: computed.getPropertyValue("--text").trim(),
            textSoft: computed.getPropertyValue("--text-soft").trim(),
            grid: computed.getPropertyValue("--chart-grid").trim(),
            primary: computed.getPropertyValue("--primary").trim(),
            primarySoft: computed.getPropertyValue("--primary-soft").trim(),
            green: computed.getPropertyValue("--green").trim(),
            orange: computed.getPropertyValue("--orange").trim(),
            purple: computed.getPropertyValue("--purple").trim()
        };
    }

    function destroyCharts() {
        Object.values(state.charts).forEach((chart) => {
            if (chart) {
                chart.destroy();
            }
        });
        state.charts = {};
    }

    async function fetchJson(url, options = {}) {
        const response = await window.fetch(url, {
            headers: { Accept: "application/json" },
            ...options
        });

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        const isJson = contentType.includes("application/json");
        const payload = isJson ? await response.json() : await response.text();

        if (!response.ok) {
            let message = "Request failed";
            if (response.status === 403) {
                message = "Unauthorized";
            } else if (payload && typeof payload === "object" && payload.message) {
                message = payload.message;
            } else if (typeof payload === "string" && payload.trim()) {
                message = payload;
            }
            throw new Error(message);
        }

        return payload;
    }

    function showToast(message, tone = "success") {
        if (!toast) {
            return;
        }

        toast.textContent = message;
        toast.className = `toast is-visible ${tone === "error" ? "is-error" : "is-success"}`;

        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => {
            toast.className = "toast";
        }, 2800);
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    function formatDate(value) {
        if (!value) {
            return "-";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }

        return new Intl.DateTimeFormat("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric"
        }).format(date);
    }

    function formatRole(role) {
        return String(role || "ROLE_USER")
            .replace("ROLE_", "")
            .toLowerCase()
            .replace(/(^|\s)\S/g, (char) => char.toUpperCase());
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }
})();
