document.addEventListener('DOMContentLoaded', () => {
    const apiBase = '/api/dashboard';

    const statusContainer = document.getElementById('dashboardStatusContainer');
    const alertsList = document.getElementById('dashboardAlertsList');
    const dashboardContent = document.getElementById('dashboardContent');

    const totalMoneyRaised = document.getElementById('totalMoneyRaised');
    const remainingToJackpot = document.getElementById('remainingToJackpot');
    const surplusAfterJackpot = document.getElementById('surplusAfterJackpot');
    const firstPartnerShare = document.getElementById('firstPartnerShare');
    const secondPartnerShare = document.getElementById('secondPartnerShare');
    const servicesShare = document.getElementById('servicesShare');
    const firstPartnerShareQuick = document.getElementById('firstPartnerShareQuick');
    const secondPartnerShareQuick = document.getElementById('secondPartnerShareQuick');
    const servicesShareQuick = document.getElementById('servicesShareQuick');

    const moneyFormatter = new Intl.NumberFormat('es-MX', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

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

    function renderAlerts(alerts) {
        if (!alertsList) {
            return;
        }

        if (!alerts || alerts.length === 0) {
            alertsList.innerHTML = `
                <div class="empty-state">
                    No hay alertas activas por el momento.
                </div>
            `;
            return;
        }

        alertsList.innerHTML = alerts.map((alert) => `
            <div class="dashboard-alert">
                <div class="dashboard-alert__content">
                    <span class="dashboard-alert__text">${escapeHtml(alert.description)}</span>
                </div>
            </div>
        `).join('');
    }

    function renderDashboard(dashboard) {
        if (!dashboard) {
            if (dashboardContent) {
                dashboardContent.style.display = 'none';
            }

            setStatus('No hay informacion que mostrar por el momento.');
            return;
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
        firstPartnerShareQuick.textContent = formatMoney(dashboard.firstPartnerShare);
        secondPartnerShareQuick.textContent = formatMoney(dashboard.secondPartnerShare);
        servicesShareQuick.textContent = formatMoney(dashboard.servicesShare);

        renderAlerts(dashboard.alerts);
    }

    async function loadDashboard() {
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

    loadDashboard();
});
