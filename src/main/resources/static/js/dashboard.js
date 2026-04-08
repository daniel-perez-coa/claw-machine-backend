document.addEventListener('DOMContentLoaded', () => {
    const apiBase = '/api/dashboard';

    const statusContainer = document.getElementById('dashboardStatusContainer');
    const dashboardActions = document.getElementById('dashboardActions');
    const alertsList = document.getElementById('dashboardAlertsList');
    const resolveAlertsBtn = document.getElementById('resolveAlertsBtn');
    const dashboardContent = document.getElementById('dashboardContent');

    const totalMoneyRaised = document.getElementById('totalMoneyRaised');
    const remainingToJackpot = document.getElementById('remainingToJackpot');
    const surplusAfterJackpot = document.getElementById('surplusAfterJackpot');
    const firstPartnerShare = document.getElementById('firstPartnerShare');
    const secondPartnerShare = document.getElementById('secondPartnerShare');
    const servicesShare = document.getElementById('servicesShare');

    const moneyFormatter = new Intl.NumberFormat('es-MX', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    let pendingRestockItems = [];

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function formatMoney(value) {
        const parsedValue = Number(value ?? 0);
        return moneyFormatter.format(Number.isFinite(parsedValue) ? parsedValue : 0);
    }

    function setStatus(message, type) {
        if (!statusContainer) {
            return;
        }

        if (!message) {
            statusContainer.innerHTML = '';
            return;
        }

        statusContainer.innerHTML = `
            <div class="empty-state">${message}</div>
        `;

        if (type === 'error') {
            statusContainer.innerHTML = `
                <div class="empty-state">No fue posible cargar la informacion del dashboard.</div>
            `;
        }
    }

    function setResolveAlertsVisibility(visible) {
        if (!resolveAlertsBtn) {
            return;
        }

        resolveAlertsBtn.style.display = visible ? 'inline-flex' : 'none';
    }

    function renderAlerts(alerts, showMajorPrizeAlert) {
        if (!alertsList) {
            return;
        }

        const alertItems = [];

        if (showMajorPrizeAlert) {
            alertItems.push(`
                <div class="dashboard-alert dashboard-alert--success">
                    <div class="dashboard-alert__content">
                        <span class="dashboard-alert__text">Felicidades, ingrese el premio mayor.</span>
                    </div>
                </div>
            `);
        }

        if (alerts && alerts.length > 0) {
            alertItems.push(...alerts.map((alert) => `
                <div class="dashboard-alert">
                    <div class="dashboard-alert__content">
                        <span class="dashboard-alert__text">${escapeHtml(alert.description)}</span>
                    </div>
                </div>
            `));
        }

        if (alertItems.length === 0) {
            setResolveAlertsVisibility(false);
            alertsList.innerHTML = `
                <div class="empty-state">
                    No hay alertas activas por el momento.
                </div>
            `;
            return;
        }

        setResolveAlertsVisibility(Boolean(alerts && alerts.length > 0));
        alertsList.innerHTML = alertItems.join('');
    }

    async function loadPendingRestockItems() {
        const response = await fetch('/api/machine-expense-records/pending-restock');

        if (!response.ok) {
            throw new Error('No fue posible cargar las alertas pendientes.');
        }

        const expenses = await response.json();
        pendingRestockItems = Array.isArray(expenses)
            ? expenses.filter((item) => item && item.id && item.restocked === false)
            : [];
    }

    async function resolveAlerts() {
        try {
            if (!window.showAppConfirmModal) {
                throw new Error('No fue posible cargar el modal de confirmacion.');
            }

            await loadPendingRestockItems();

            if (!pendingRestockItems.length) {
                renderAlerts([]);
                showTemporaryStatus('No hay alertas pendientes por resolver.');
                return;
            }

            const confirmed = await window.showAppConfirmModal({
                title: 'Resolver alertas',
                body: 'Antes de cerrar las alertas confirme que haya realizado la carga de la máquina.',
                confirmText: 'Confirmar',
                cancelText: 'Cancelar',
                confirmVariant: 'success'
            });

            if (!confirmed) {
                return;
            }

            await Promise.all(
                pendingRestockItems.map((item) =>
                    fetch(`/api/machine-expense-records/${item.id}/restocked`, {
                        method: 'PATCH'
                    }).then((response) => {
                        if (!response.ok) {
                            throw new Error('No fue posible resolver todas las alertas.');
                        }
                    })
                )
            );

            pendingRestockItems = [];
            await loadDashboard();
            showTemporaryStatus('Alertas resueltas correctamente.');
        } catch (error) {
            showTemporaryStatus(error.message ?? 'No fue posible resolver las alertas.', 'error');
        }
    }

    function showTemporaryStatus(message, type) {
        setStatus(message, type);
        window.setTimeout(() => {
            setStatus('');
        }, 3200);
    }

    function renderDashboard(dashboard) {
        if (!dashboard) {
            setResolveAlertsVisibility(false);

            if (dashboardActions) {
                dashboardActions.style.display = 'none';
            }

            if (dashboardContent) {
                dashboardContent.style.display = 'none';
            }

            setStatus('No hay informacion que mostrar por el momento.');
            return;
        }

        if (dashboardActions) {
            dashboardActions.style.display = '';
        }

        if (dashboardContent) {
            dashboardContent.style.display = '';
        }

        setStatus('');

        totalMoneyRaised.textContent = formatMoney(dashboard.totalMoneyRaised);
        remainingToJackpot.textContent = formatMoney(dashboard.remainingToJackpot);
        surplusAfterJackpot.textContent = formatMoney(dashboard.surplusAfterJackpot);
        firstPartnerShare.textContent = formatMoney(dashboard.firstPartnerShare);
        secondPartnerShare.textContent = formatMoney(dashboard.secondPartnerShare);
        servicesShare.textContent = formatMoney(dashboard.servicesShare);
        renderAlerts(dashboard.alerts, Boolean(dashboard.majorPrizeAlertActive));
    }

    async function loadDashboard() {
        setResolveAlertsVisibility(false);

        if (dashboardActions) {
            dashboardActions.style.display = 'none';
        }

        if (dashboardContent) {
            dashboardContent.style.display = 'none';
        }

        setStatus('Cargando informacion del dashboard...');

        try {
            const response = await fetch(apiBase);

            if (!response.ok) {
                throw new Error('No se pudo cargar el dashboard.');
            }

            const responseText = await response.text();
            const dashboard = responseText ? JSON.parse(responseText) : null;

            renderDashboard(dashboard);
        } catch (error) {
            if (dashboardContent) {
                dashboardContent.style.display = 'none';
            }

            setStatus('No fue posible cargar la informacion del dashboard.', 'error');
        }
    }

    resolveAlertsBtn?.addEventListener('click', resolveAlerts);

    loadDashboard();
});
