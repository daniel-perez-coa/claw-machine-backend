document.addEventListener('DOMContentLoaded', () => {
    const campaignsList = document.getElementById('campaignsList');
    const campaignsPagination = document.getElementById('campaignsPagination');
    const pageSize = 6;
    let allCampaigns = [];
    let currentPage = 1;

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

    function getCampaignDateMarkup(campaign) {
        if (campaign.status === 'CLOSED' || campaign.status === 'CANCELLED') {
            return `
                <p class="campaign-card__prize">
                    <strong>Fecha de apertura:</strong>
                    <span>${escapeHtml(campaign.createdAt ?? 'Sin fecha')}</span>
                </p>
                <p class="campaign-card__prize">
                    <strong>Fecha de cierre:</strong>
                    <span>${escapeHtml(campaign.closedAt ?? 'Sin fecha')}</span>
                </p>
            `;
        }

        return `
            <p class="campaign-card__prize">
                <strong>Fecha de creacion:</strong>
                <span>${escapeHtml(campaign.createdAt ?? 'Sin fecha')}</span>
            </p>
        `;
    }

    function getCampaignNotesMarkup(campaign) {
        if (campaign.status !== 'CANCELLED' || !campaign.notes) {
            return '';
        }

        return `
            <p class="campaign-card__notes">
                <strong>Motivo de cancelacion:</strong>
                <span>${escapeHtml(campaign.notes)}</span>
            </p>
        `;
    }

    function renderPagination(totalItems) {
        if (!campaignsPagination) {
            return;
        }

        if (totalItems <= pageSize) {
            campaignsPagination.innerHTML = '';
            return;
        }

        const totalPages = Math.ceil(totalItems / pageSize);
        const previousDisabled = currentPage <= 1 ? 'disabled' : '';
        const nextDisabled = currentPage >= totalPages ? 'disabled' : '';

        campaignsPagination.innerHTML = `
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    data-page-action="previous"
                    ${previousDisabled}>
                Anterior
            </button>
            <span class="reports-pagination__info">Pagina ${currentPage} de ${totalPages}</span>
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    data-page-action="next"
                    ${nextDisabled}>
                Siguiente
            </button>
        `;
    }

    function renderCampaignsPage() {
        if (!allCampaigns || allCampaigns.length === 0) {
            campaignsList.innerHTML = '<div class="empty-state">No hay campanas registradas por el momento.</div>';
            if (campaignsPagination) {
                campaignsPagination.innerHTML = '';
            }
            return;
        }

        const totalPages = Math.max(1, Math.ceil(allCampaigns.length / pageSize));
        currentPage = Math.min(Math.max(1, currentPage), totalPages);
        const startIndex = (currentPage - 1) * pageSize;
        const campaigns = allCampaigns.slice(startIndex, startIndex + pageSize);

        campaignsList.innerHTML = campaigns.map((campaign) => `
            <article class="app-card campaign-card">
                <div class="campaign-card__content">
                    <h3 class="campaign-card__title">${escapeHtml(campaign.name)}</h3>

                    <p class="campaign-card__prize">
                        <strong>Premio mayor:</strong>
                        <span>${escapeHtml(campaign.prizeName)}</span>
                        ${campaign.prizeDescription ? `<span> - ${escapeHtml(campaign.prizeDescription)}</span>` : ''}
                    </p>

                    ${getCampaignDateMarkup(campaign)}
                    ${getCampaignNotesMarkup(campaign)}

                    <div class="campaign-card__meta">
                        ${getStatusMarkup(campaign.status)}

                        <div class="campaign-card__target">
                            Meta de dinero a recaudar:
                            <strong style="margin-left: 6px;">$${formatMoney(campaign.baseTargetAmount)}</strong>
                            <span class="campaign-card__target-separator"></span>
                            Dinero recaudado:
                            <strong style="margin-left: 6px;">$${formatMoney(campaign.totalMoneyRaised)}</strong>
                        </div>
                    </div>
                </div>

                ${campaign.status === 'OPEN' ? `
                    <div class="campaign-card__actions">
                        <a class="campaign-card__button" href="/campaigns/${campaign.id}">
                            Actualizar
                        </a>
                    </div>
                ` : ''}
            </article>
        `).join('');

        renderPagination(allCampaigns.length);
    }

    async function loadCampaigns() {
        campaignsList.innerHTML = '<div class="empty-state">Cargando campanas...</div>';
        if (campaignsPagination) {
            campaignsPagination.innerHTML = '';
        }

        try {
            const response = await fetch('/api/campaigns');

            if (!response.ok) {
                throw new Error('No se pudieron cargar las campanas.');
            }

            allCampaigns = await response.json();
            currentPage = 1;
            renderCampaignsPage();
        } catch (error) {
            campaignsList.innerHTML = '<div class="empty-state">No fue posible cargar las campanas.</div>';
            if (campaignsPagination) {
                campaignsPagination.innerHTML = '';
            }
        }
    }

    campaignsPagination?.addEventListener('click', (event) => {
        const button = event.target.closest('[data-page-action]');

        if (!button || button.disabled) {
            return;
        }

        if (button.dataset.pageAction === 'previous' && currentPage > 1) {
            currentPage -= 1;
            renderCampaignsPage();
            return;
        }

        if (button.dataset.pageAction === 'next' && currentPage < Math.ceil(allCampaigns.length / pageSize)) {
            currentPage += 1;
            renderCampaignsPage();
        }
    });

    loadCampaigns();
});
