document.addEventListener('DOMContentLoaded', () => {
    const campaignsList = document.getElementById('campaignsList');

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

    function getStatusMarkup(status) {
        if (status === 'OPEN') {
            return `
                <div class="campaign-card__status campaign-card__status--open">
                    <span class="campaign-card__status-dot"></span>
                    <span>Abierta</span>
                </div>
            `;
        }

        if (status === 'CLOSED') {
            return `
                <div class="campaign-card__status campaign-card__status--closed">
                    <span class="campaign-card__status-dot"></span>
                    <span>Cerrada</span>
                </div>
            `;
        }

        return `
            <div class="campaign-card__status campaign-card__status--cancelled">
                <span class="campaign-card__status-dot"></span>
                <span>Cancelada</span>
            </div>
        `;
    }

    function renderCampaigns(campaigns) {
        if (!campaigns || campaigns.length === 0) {
            campaignsList.innerHTML = '<div class="empty-state">No hay campanas registradas por el momento.</div>';
            return;
        }

        campaignsList.innerHTML = campaigns.map((campaign) => `
            <article class="app-card campaign-card">
                <div class="campaign-card__content">
                    <h3 class="campaign-card__title">${escapeHtml(campaign.name)}</h3>

                    <p class="campaign-card__prize">
                        <strong>Premio mayor:</strong>
                        <span>${escapeHtml(campaign.prizeName)}</span>
                        ${campaign.prizeDescription ? `<span> - ${escapeHtml(campaign.prizeDescription)}</span>` : ''}
                    </p>

                    <div class="campaign-card__meta">
                        ${getStatusMarkup(campaign.status)}

                        <div class="campaign-card__target">
                            Meta base siguiente:
                            <strong style="margin-left: 6px;">$${formatMoney(campaign.baseTargetAmount)}</strong>
                        </div>
                    </div>
                </div>

                <div class="campaign-card__actions">
                    <a class="campaign-card__button" href="/campaigns/${campaign.id}">
                        Actualizar
                    </a>
                </div>
            </article>
        `).join('');
    }

    async function loadCampaigns() {
        campaignsList.innerHTML = '<div class="empty-state">Cargando campanas...</div>';

        try {
            const response = await fetch('/api/campaigns');

            if (!response.ok) {
                throw new Error('No se pudieron cargar las campanas.');
            }

            const campaigns = await response.json();
            renderCampaigns(campaigns);
        } catch (error) {
            campaignsList.innerHTML = '<div class="empty-state">No fue posible cargar las campanas.</div>';
        }
    }

    loadCampaigns();
});
