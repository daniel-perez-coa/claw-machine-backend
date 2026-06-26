document.addEventListener('DOMContentLoaded', () => {
    const campaignSelector = document.getElementById('campaignRedemptionsSelector');
    const reloadButton = document.getElementById('reloadCampaignRedemptionsBtn');
    const alertElement = document.getElementById('campaignRedemptionsAlert');
    const summaryElement = document.getElementById('campaignRedemptionsSummary');
    const listElement = document.getElementById('campaignRedemptionsList');
    const paginationElement = document.getElementById('campaignRedemptionsPagination');
    const pageSize = 6;
    let currentPage = 1;

    function setAlert(message, type = '') {
        if (!alertElement) {
            return;
        }

        alertElement.textContent = message ?? '';
        alertElement.classList.remove('reports-status--success', 'reports-status--error');

        if (type) {
            alertElement.classList.add(`reports-status--${type}`);
        }
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function getStatusLabel(campaign) {
        if (!campaign) {
            return '';
        }

        if (campaign.status === 'OPEN') {
            return 'Abierta';
        }

        if (campaign.status === 'CLOSED') {
            return 'Cerrada';
        }

        if (campaign.status === 'CANCELLED') {
            return 'Cancelada';
        }

        return campaign.status ?? '';
    }

    function renderSummary(campaign, redemptionCount) {
        if (!summaryElement) {
            return;
        }

        if (!campaign) {
            summaryElement.innerHTML = '';
            return;
        }

        summaryElement.innerHTML = `
            <article class="campaign-redemptions-summary__card">
                <div class="campaign-redemptions-summary__item">
                    <span class="campaign-redemptions-summary__label">Campaña</span>
                    <strong class="campaign-redemptions-summary__value">${escapeHtml(campaign.name)}</strong>
                </div>
                <div class="campaign-redemptions-summary__item">
                    <span class="campaign-redemptions-summary__label">Estado</span>
                    <strong class="campaign-redemptions-summary__value">${escapeHtml(getStatusLabel(campaign))}</strong>
                </div>
                <div class="campaign-redemptions-summary__item">
                    <span class="campaign-redemptions-summary__label">Abierta</span>
                    <strong class="campaign-redemptions-summary__value">${escapeHtml(campaign.openedAt ?? campaign.createdAt ?? 'Sin fecha')}</strong>
                </div>
                <div class="campaign-redemptions-summary__item">
                    <span class="campaign-redemptions-summary__label">Canjes</span>
                    <strong class="campaign-redemptions-summary__value">${redemptionCount}</strong>
                </div>
            </article>
        `;
    }

    function attachPrintActions() {
        document.querySelectorAll('[data-redemption-id]').forEach((button) => {
            button.addEventListener('click', async () => {
                const redemptionId = button.getAttribute('data-redemption-id');
                if (!redemptionId || !window.appReportPrinter) {
                    return;
                }

                try {
                    button.disabled = true;
                    await window.appReportPrinter.printThermalTicketFromUrl(`/api/reports/tickets/user-redemption/${redemptionId}/thermal-print`);
                } catch (error) {
                    setAlert('No fue posible imprimir el ticket termico.', 'error');
                } finally {
                    button.disabled = false;
                }
            });
        });
    }

    function renderPagination(totalItems) {
        if (!paginationElement) {
            return;
        }

        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

        if (totalItems <= pageSize) {
            paginationElement.innerHTML = '';
            return;
        }

        paginationElement.innerHTML = `
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    id="campaignRedemptionsPrevBtn"
                    ${currentPage === 1 ? 'disabled' : ''}>
                Anterior
            </button>
            <span class="reports-pagination__info">Pagina ${currentPage} de ${totalPages}</span>
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    id="campaignRedemptionsNextBtn"
                    ${currentPage === totalPages ? 'disabled' : ''}>
                Siguiente
            </button>
        `;

        document.getElementById('campaignRedemptionsPrevBtn')?.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage -= 1;
                void loadRedemptions();
            }
        });

        document.getElementById('campaignRedemptionsNextBtn')?.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage += 1;
                void loadRedemptions();
            }
        });
    }

    function renderRedemptions(campaign, redemptions) {
        renderSummary(campaign, redemptions.length);

        if (!listElement) {
            return;
        }

        if (!redemptions.length) {
            listElement.innerHTML = '<div class="empty-state">No hay canjes registrados para esta campaña.</div>';
            if (paginationElement) {
                paginationElement.innerHTML = '';
            }
            return;
        }

        const totalPages = Math.max(1, Math.ceil(redemptions.length / pageSize));
        currentPage = Math.min(currentPage, totalPages);
        const startIndex = (currentPage - 1) * pageSize;
        const paginatedRedemptions = redemptions.slice(startIndex, startIndex + pageSize);

        listElement.innerHTML = paginatedRedemptions.map((redemption) => `
            <article class="app-card campaign-redemption-card">
                <div class="campaign-redemption-card__content">
                    <h3 class="campaign-redemption-card__title">${escapeHtml(redemption.userName)}</h3>
                    <div class="campaign-redemption-card__meta">
                        <span>Telefono: <strong>${escapeHtml(redemption.userPhone)}</strong></span>
                        <span>Premio: <strong>${escapeHtml(redemption.prizeName)}</strong></span>
                        <span>Categoria: <strong>${escapeHtml(redemption.prizeCategory)}</strong></span>
                        <span>Fecha: <strong>${escapeHtml(redemption.redeemedAt)}</strong></span>
                        <span>Puntos gastados: <strong>${escapeHtml(redemption.pointsSpent)}</strong></span>
                        <span>Puntos anteriores: <strong>${escapeHtml(redemption.previousPoints)}</strong></span>
                        <span>Puntos restantes: <strong>${escapeHtml(redemption.remainingPoints)}</strong></span>
                    </div>
                </div>

                <div class="campaign-redemption-card__actions">
                    <button type="button"
                            class="reports-action-card__button reports-action-card__button--success campaign-redemption-card__button"
                            data-redemption-id="${redemption.redemptionId}">
                        Imprimir ticket
                    </button>
                </div>
            </article>
        `).join('');

        attachPrintActions();
        renderPagination(redemptions.length);
    }

    async function fetchCampaigns() {
        const response = await fetch('/api/campaigns');

        if (!response.ok) {
            throw new Error('No fue posible cargar las campañas.');
        }

        return response.json();
    }

    function populateCampaignSelector(campaigns) {
        if (!campaignSelector) {
            return;
        }

        campaignSelector.innerHTML = `
            <option value="">Seleccione una campaña</option>
            ${campaigns.map((campaign) => `
                <option value="${campaign.id}">
                    ${escapeHtml(campaign.name)} (${escapeHtml(getStatusLabel(campaign))})
                </option>
            `).join('')}
        `;

        if (campaigns.length) {
            const openCampaign = campaigns.find((campaign) => campaign.status === 'OPEN');
            if (openCampaign) {
                campaignSelector.value = String(openCampaign.id);
            }
        }
    }

    async function loadRedemptions() {
        const campaignId = campaignSelector?.value ?? '';
        if (!campaignId) {
            currentPage = 1;
            renderSummary(null, 0);
            if (listElement) {
                listElement.innerHTML = '<div class="empty-state">Seleccione una campaña para consultar canjes.</div>';
            }
            if (paginationElement) {
                paginationElement.innerHTML = '';
            }
            return;
        }

        reloadButton.disabled = true;
        setAlert('Cargando canjes...');
        if (listElement) {
            listElement.innerHTML = '<div class="empty-state">Cargando canjes...</div>';
        }

        try {
            const [campaignsResponse, redemptionsResponse] = await Promise.all([
                fetch('/api/campaigns'),
                fetch(`/api/reports/campaigns/${campaignId}/prize-redemptions`)
            ]);

            if (!campaignsResponse.ok) {
                throw new Error('No fue posible cargar la campaña seleccionada.');
            }

            if (!redemptionsResponse.ok) {
                throw new Error('No fue posible cargar los canjes.');
            }

            const campaigns = await campaignsResponse.json();
            const redemptions = await redemptionsResponse.json();
            const campaign = campaigns.find((item) => String(item.id) === String(campaignId));

            renderRedemptions(campaign, redemptions);
            setAlert(`Se cargaron ${redemptions.length} canjes.`, 'success');
        } catch (error) {
            renderSummary(null, 0);
            if (listElement) {
                listElement.innerHTML = '<div class="empty-state">No fue posible cargar los canjes.</div>';
            }
            setAlert(error.message ?? 'No fue posible cargar los canjes.', 'error');
        } finally {
            reloadButton.disabled = false;
        }
    }

    async function initialize() {
        try {
            setAlert('Cargando campañas...');
            const campaigns = await fetchCampaigns();
            populateCampaignSelector(campaigns);
            setAlert('');

            if (campaignSelector?.value) {
                await loadRedemptions();
            }
        } catch (error) {
            setAlert(error.message ?? 'No fue posible cargar las campañas.', 'error');
            if (listElement) {
                listElement.innerHTML = '<div class="empty-state">No fue posible cargar las campañas.</div>';
            }
        }
    }

    campaignSelector?.addEventListener('change', () => {
        currentPage = 1;
        void loadRedemptions();
    });

    reloadButton?.addEventListener('click', () => {
        currentPage = 1;
        void loadRedemptions();
    });

    void initialize();
});
