document.addEventListener('DOMContentLoaded', () => {
    const campaignSelector = document.getElementById('campaignSelector');
    const reloadButton = document.getElementById('reloadCampaignTransactionsBtn');
    const alertElement = document.getElementById('campaignAddPointsAlert');
    const summaryElement = document.getElementById('campaignAddPointsSummary');
    const listElement = document.getElementById('campaignAddPointsList');
    const paginationElement = document.getElementById('campaignAddPointsPagination');
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

    function getSelectedScope() {
        const selectedValue = campaignSelector?.value ?? 'ALL';
        return {
            selectedValue,
            isAll: selectedValue === 'ALL',
            isUnassigned: selectedValue === 'UNASSIGNED',
            campaignId: selectedValue !== 'ALL' && selectedValue !== 'UNASSIGNED' ? selectedValue : ''
        };
    }

    function renderSummary(campaign, transactionCount) {
        if (!summaryElement) {
            return;
        }

        const scope = getSelectedScope();

        if (scope.isAll) {
            summaryElement.innerHTML = `
                <article class="campaign-add-points-summary__card">
                    <div class="campaign-add-points-summary__item">
                        <span class="campaign-add-points-summary__label">Vista</span>
                        <strong class="campaign-add-points-summary__value">Todas las transacciones</strong>
                    </div>
                    <div class="campaign-add-points-summary__item">
                        <span class="campaign-add-points-summary__label">Transacciones</span>
                        <strong class="campaign-add-points-summary__value">${transactionCount}</strong>
                    </div>
                </article>
            `;
            return;
        }

        if (scope.isUnassigned) {
            summaryElement.innerHTML = `
                <article class="campaign-add-points-summary__card">
                    <div class="campaign-add-points-summary__item">
                        <span class="campaign-add-points-summary__label">Vista</span>
                        <strong class="campaign-add-points-summary__value">Sin campaña</strong>
                    </div>
                    <div class="campaign-add-points-summary__item">
                        <span class="campaign-add-points-summary__label">Transacciones</span>
                        <strong class="campaign-add-points-summary__value">${transactionCount}</strong>
                    </div>
                </article>
            `;
            return;
        }

        if (!campaign) {
            summaryElement.innerHTML = '';
            return;
        }

        summaryElement.innerHTML = `
            <article class="campaign-add-points-summary__card">
                <div class="campaign-add-points-summary__item">
                    <span class="campaign-add-points-summary__label">Campaña</span>
                    <strong class="campaign-add-points-summary__value">${escapeHtml(campaign.name)}</strong>
                </div>
                <div class="campaign-add-points-summary__item">
                    <span class="campaign-add-points-summary__label">Estado</span>
                    <strong class="campaign-add-points-summary__value">${escapeHtml(getStatusLabel(campaign))}</strong>
                </div>
                <div class="campaign-add-points-summary__item">
                    <span class="campaign-add-points-summary__label">Abierta</span>
                    <strong class="campaign-add-points-summary__value">${escapeHtml(campaign.openedAt ?? campaign.createdAt ?? 'Sin fecha')}</strong>
                </div>
                <div class="campaign-add-points-summary__item">
                    <span class="campaign-add-points-summary__label">Transacciones</span>
                    <strong class="campaign-add-points-summary__value">${transactionCount}</strong>
                </div>
            </article>
        `;
    }

    function attachPrintActions() {
        document.querySelectorAll('[data-transaction-id]').forEach((button) => {
            button.addEventListener('click', async () => {
                const transactionId = button.getAttribute('data-transaction-id');
                if (!transactionId || !window.appReportPrinter) {
                    return;
                }

                const printWindow = window.appReportPrinter.openPrintWindow('Preparando ticket de puntos...');

                try {
                    button.disabled = true;
                    await window.appReportPrinter.printPdfFromUrl(`/api/reports/tickets/add-points/${transactionId}`, printWindow);
                } catch (error) {
                    printWindow?.close();
                    setAlert('No fue posible abrir la impresion del ticket.', 'error');
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
                    id="campaignAddPointsPrevBtn"
                    ${currentPage === 1 ? 'disabled' : ''}>
                Anterior
            </button>
            <span class="reports-pagination__info">Pagina ${currentPage} de ${totalPages}</span>
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    id="campaignAddPointsNextBtn"
                    ${currentPage === totalPages ? 'disabled' : ''}>
                Siguiente
            </button>
        `;

        document.getElementById('campaignAddPointsPrevBtn')?.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage -= 1;
                void loadTransactions();
            }
        });

        document.getElementById('campaignAddPointsNextBtn')?.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage += 1;
                void loadTransactions();
            }
        });
    }

    function renderTransactions(campaign, transactions) {
        renderSummary(campaign, transactions.length);

        if (!listElement) {
            return;
        }

        if (!transactions.length) {
            const scope = getSelectedScope();
            let emptyMessage = 'No hay transacciones de agregar puntos registradas.';

            if (scope.isUnassigned) {
                emptyMessage = 'No hay transacciones de agregar puntos sin campaña.';
            } else if (!scope.isAll) {
                emptyMessage = 'No hay transacciones de agregar puntos para esta campaña.';
            }

            listElement.innerHTML = `<div class="empty-state">${emptyMessage}</div>`;
            if (paginationElement) {
                paginationElement.innerHTML = '';
            }
            return;
        }

        const totalPages = Math.max(1, Math.ceil(transactions.length / pageSize));
        currentPage = Math.min(currentPage, totalPages);
        const startIndex = (currentPage - 1) * pageSize;
        const paginatedTransactions = transactions.slice(startIndex, startIndex + pageSize);

        listElement.innerHTML = paginatedTransactions.map((transaction) => `
            <article class="app-card campaign-add-points-transaction">
                <div class="campaign-add-points-transaction__content">
                    <h3 class="campaign-add-points-transaction__title">${escapeHtml(transaction.userName)}</h3>
                    <div class="campaign-add-points-transaction__meta">
                        <span>Telefono: <strong>${escapeHtml(transaction.userPhone)}</strong></span>
                        <span>Fecha: <strong>${escapeHtml(transaction.createdAt)}</strong></span>
                        <span>Campaña: <strong>${escapeHtml(transaction.campaignName ?? 'Sin campaña')}</strong></span>
                        <span>Puntos agregados: <strong>${escapeHtml(transaction.pointsAdded)}</strong></span>
                        <span>Saldo anterior: <strong>${escapeHtml(transaction.previousBalance)}</strong></span>
                        <span>Saldo nuevo: <strong>${escapeHtml(transaction.newBalance)}</strong></span>
                    </div>
                </div>

                <div class="campaign-add-points-transaction__actions">
                    <button type="button"
                            class="reports-action-card__button reports-action-card__button--success campaign-add-points-transaction__button"
                            data-transaction-id="${transaction.transactionId}">
                        Imprimir ticket
                    </button>
                </div>
            </article>
        `).join('');

        attachPrintActions();
        renderPagination(transactions.length);
    }

    async function fetchCampaigns() {
        const response = await fetch('/api/campaigns');

        if (!response.ok) {
            throw new Error('No fue posible cargar las campañas.');
        }

        return response.json();
    }

    function getCampaignIdFromQuery() {
        const params = new URLSearchParams(window.location.search);
        const rawValue = params.get('campaignId');
        const campaignId = Number(rawValue);
        return Number.isFinite(campaignId) && campaignId > 0 ? String(campaignId) : '';
    }

    function populateCampaignSelector(campaigns) {
        if (!campaignSelector) {
            return;
        }

        const selectedCampaignId = getCampaignIdFromQuery();
        campaignSelector.innerHTML = `
            <option value="ALL" ${!selectedCampaignId ? 'selected' : ''}>Todas las transacciones</option>
            <option value="UNASSIGNED">Sin campaña</option>
            ${campaigns.map((campaign) => `
                <option value="${campaign.id}" ${String(campaign.id) === selectedCampaignId ? 'selected' : ''}>
                    ${escapeHtml(campaign.name)} (${escapeHtml(getStatusLabel(campaign))})
                </option>
            `).join('')}
        `;
    }

    async function loadTransactions() {
        const scope = getSelectedScope();

        reloadButton.disabled = true;
        setAlert('Cargando transacciones...');
        if (listElement) {
            listElement.innerHTML = '<div class="empty-state">Cargando transacciones...</div>';
        }

        try {
            const query = new URLSearchParams();
            if (scope.campaignId) {
                query.set('campaignId', scope.campaignId);
            }
            if (scope.isUnassigned) {
                query.set('unassignedOnly', 'true');
            }

            const [campaignsResponse, transactionsResponse] = await Promise.all([
                fetch('/api/campaigns'),
                fetch(`/api/reports/add-points-transactions?${query.toString()}`)
            ]);

            if (!campaignsResponse.ok) {
                throw new Error('No fue posible cargar las campañas.');
            }

            if (!transactionsResponse.ok) {
                throw new Error('No fue posible cargar las transacciones.');
            }

            const campaigns = await campaignsResponse.json();
            const transactions = await transactionsResponse.json();
            const campaign = scope.campaignId
                ? campaigns.find((item) => String(item.id) === String(scope.campaignId))
                : null;

            renderTransactions(campaign, transactions);
            setAlert(`Se cargaron ${transactions.length} transacciones.`, 'success');
        } catch (error) {
            renderSummary(null, 0);
            if (listElement) {
                listElement.innerHTML = '<div class="empty-state">No fue posible cargar las transacciones.</div>';
            }
            setAlert(error.message ?? 'No fue posible cargar las transacciones.', 'error');
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
            await loadTransactions();
        } catch (error) {
            setAlert(error.message ?? 'No fue posible cargar las campañas.', 'error');
            if (listElement) {
                listElement.innerHTML = '<div class="empty-state">No fue posible cargar las campañas.</div>';
            }
        }
    }

    campaignSelector?.addEventListener('change', () => {
        currentPage = 1;
        void loadTransactions();
    });

    reloadButton?.addEventListener('click', () => {
        currentPage = 1;
        void loadTransactions();
    });

    void initialize();
});
