document.addEventListener('DOMContentLoaded', () => {
    const campaignSelector = document.getElementById('campaignQuickRedemptionsSelector');
    const reloadButton = document.getElementById('reloadCampaignQuickRedemptionsBtn');
    const alertElement = document.getElementById('campaignQuickRedemptionsAlert');
    const summaryElement = document.getElementById('campaignQuickRedemptionsSummary');
    const listElement = document.getElementById('campaignQuickRedemptionsList');
    const paginationElement = document.getElementById('campaignQuickRedemptionsPagination');
    const pageSize = 3;
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

    function getRestockedLabel(record) {
        return record.restocked ? 'Reabastecido' : 'Pendiente';
    }

    function getItemRestockedLabel(item) {
        return item.restocked ? 'Reabastecido' : 'Pendiente';
    }

    function renderSummary(campaign, recordCount) {
        if (!summaryElement) {
            return;
        }

        if (!campaign) {
            summaryElement.innerHTML = '';
            return;
        }

        summaryElement.innerHTML = `
            <article class="campaign-quick-redemptions-summary__card">
                <div class="campaign-quick-redemptions-summary__item">
                    <span class="campaign-quick-redemptions-summary__label">Campaña</span>
                    <strong class="campaign-quick-redemptions-summary__value">${escapeHtml(campaign.name)}</strong>
                </div>
                <div class="campaign-quick-redemptions-summary__item">
                    <span class="campaign-quick-redemptions-summary__label">Estado</span>
                    <strong class="campaign-quick-redemptions-summary__value">${escapeHtml(getStatusLabel(campaign))}</strong>
                </div>
                <div class="campaign-quick-redemptions-summary__item">
                    <span class="campaign-quick-redemptions-summary__label">Abierta</span>
                    <strong class="campaign-quick-redemptions-summary__value">${escapeHtml(campaign.openedAt ?? campaign.createdAt ?? 'Sin fecha')}</strong>
                </div>
                <div class="campaign-quick-redemptions-summary__item">
                    <span class="campaign-quick-redemptions-summary__label">Canjes rapidos</span>
                    <strong class="campaign-quick-redemptions-summary__value">${recordCount}</strong>
                </div>
            </article>
        `;
    }

    function attachPrintActions() {
        document.querySelectorAll('[data-expense-ids]').forEach((button) => {
            button.addEventListener('click', async () => {
                const rawExpenseIds = button.getAttribute('data-expense-ids');
                if (!rawExpenseIds || !window.appReportPrinter) {
                    return;
                }

                const params = new URLSearchParams();
                rawExpenseIds.split(',')
                    .map((value) => value.trim())
                    .filter((value) => value !== '')
                    .forEach((expenseId) => params.append('expenseIds', expenseId));

                try {
                    button.disabled = true;
                    await window.appReportPrinter.printThermalTicketFromUrl(`/api/reports/tickets/quick-redemption/thermal-print?${params.toString()}`);
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
                    id="campaignQuickRedemptionsPrevBtn"
                    ${currentPage === 1 ? 'disabled' : ''}>
                Anterior
            </button>
            <span class="reports-pagination__info">Pagina ${currentPage} de ${totalPages}</span>
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    id="campaignQuickRedemptionsNextBtn"
                    ${currentPage === totalPages ? 'disabled' : ''}>
                Siguiente
            </button>
        `;

        document.getElementById('campaignQuickRedemptionsPrevBtn')?.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage -= 1;
                void loadQuickRedemptions();
            }
        });

        document.getElementById('campaignQuickRedemptionsNextBtn')?.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage += 1;
                void loadQuickRedemptions();
            }
        });
    }

    function renderQuickRedemptions(campaign, records) {
        renderSummary(campaign, records.length);

        if (!listElement) {
            return;
        }

        if (!records.length) {
            listElement.innerHTML = '<div class="empty-state">No hay canjes rapidos registrados para esta campaña.</div>';
            if (paginationElement) {
                paginationElement.innerHTML = '';
            }
            return;
        }

        const totalPages = Math.max(1, Math.ceil(records.length / pageSize));
        currentPage = Math.min(currentPage, totalPages);
        const startIndex = (currentPage - 1) * pageSize;
        const paginatedRecords = records.slice(startIndex, startIndex + pageSize);

        listElement.innerHTML = paginatedRecords.map((record) => `
            <article class="app-card campaign-quick-redemption-card">
                <div class="campaign-quick-redemption-card__content">
                    <h3 class="campaign-quick-redemption-card__title">Operacion ${escapeHtml(record.registeredAt)}</h3>
                    <div class="campaign-quick-redemption-card__meta">
                        <span>Fecha: <strong>${escapeHtml(record.registeredAt)}</strong></span>
                        <span>Costo total: <strong>$${escapeHtml(record.totalCost)}</strong></span>
                        <span>Cantidad total: <strong>${escapeHtml(record.totalQuantity)}</strong></span>
                        <span>Estado: <strong>${escapeHtml(getRestockedLabel(record))}</strong></span>
                    </div>

                    <div class="campaign-quick-redemption-card__items">
                        ${record.items.map((item) => `
                            <div class="campaign-quick-redemption-card__item">
                                <strong class="campaign-quick-redemption-card__item-title">${escapeHtml(item.prizeName)}</strong>
                                <div class="campaign-quick-redemption-card__item-meta">
                                    <span>Categoria: <strong>${escapeHtml(item.prizeCategory)}</strong></span>
                                    <span>Cantidad: <strong>${escapeHtml(item.quantity)}</strong></span>
                                    <span>Costo unitario: <strong>$${escapeHtml(item.unitCost)}</strong></span>
                                    <span>Costo total: <strong>$${escapeHtml(item.totalCost)}</strong></span>
                                    <span>Estado: <strong>${escapeHtml(getItemRestockedLabel(item))}</strong></span>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>

                <div class="campaign-quick-redemption-card__actions">
                    <button type="button"
                            class="reports-action-card__button reports-action-card__button--success campaign-quick-redemption-card__button"
                            data-expense-ids="${record.expenseIds.map((expenseId) => String(expenseId)).join(',')}">
                        Imprimir ticket
                    </button>
                </div>
            </article>
        `).join('');

        attachPrintActions();
        renderPagination(records.length);
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

    async function loadQuickRedemptions() {
        const campaignId = campaignSelector?.value ?? '';
        if (!campaignId) {
            currentPage = 1;
            renderSummary(null, 0);
            if (listElement) {
                listElement.innerHTML = '<div class="empty-state">Seleccione una campaña para consultar canjes rapidos.</div>';
            }
            if (paginationElement) {
                paginationElement.innerHTML = '';
            }
            return;
        }

        reloadButton.disabled = true;
        setAlert('Cargando canjes rapidos...');
        if (listElement) {
            listElement.innerHTML = '<div class="empty-state">Cargando canjes rapidos...</div>';
        }

        try {
            const [campaignsResponse, recordsResponse] = await Promise.all([
                fetch('/api/campaigns'),
                fetch(`/api/reports/campaigns/${campaignId}/quick-redemptions`)
            ]);

            if (!campaignsResponse.ok) {
                throw new Error('No fue posible cargar la campaña seleccionada.');
            }

            if (!recordsResponse.ok) {
                throw new Error('No fue posible cargar los canjes rapidos.');
            }

            const campaigns = await campaignsResponse.json();
            const records = await recordsResponse.json();
            const campaign = campaigns.find((item) => String(item.id) === String(campaignId));

            renderQuickRedemptions(campaign, records);
            setAlert(`Se cargaron ${records.length} canjes rapidos.`, 'success');
        } catch (error) {
            renderSummary(null, 0);
            if (listElement) {
                listElement.innerHTML = '<div class="empty-state">No fue posible cargar los canjes rapidos.</div>';
            }
            setAlert(error.message ?? 'No fue posible cargar los canjes rapidos.', 'error');
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
                await loadQuickRedemptions();
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
        void loadQuickRedemptions();
    });

    reloadButton?.addEventListener('click', () => {
        currentPage = 1;
        void loadQuickRedemptions();
    });

    void initialize();
});
