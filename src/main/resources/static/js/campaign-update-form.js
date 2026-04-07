document.addEventListener('DOMContentLoaded', () => {
    const pathSegments = window.location.pathname.split('/').filter(Boolean);
    const campaignId = Number(pathSegments[pathSegments.length - 1]);

    const form = document.getElementById('campaignUpdateForm');
    const statusSelect = document.getElementById('campaignStatus');
    const notesGroup = document.getElementById('campaignNotesGroup');
    const notesInput = document.getElementById('campaignNotes');
    const submitButton = document.getElementById('submitCampaignUpdateBtn');
    const alertContainer = document.getElementById('campaignUpdateAlertContainer');
    const summaryContent = document.getElementById('campaignUpdateSummaryContent');

    if (!form || !statusSelect || !notesGroup || !notesInput || !submitButton || !alertContainer || !summaryContent) {
        return;
    }

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
        return new Intl.NumberFormat('es-MX', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(Number.isFinite(parsedValue) ? parsedValue : 0);
    }

    function showAlert(message, type = 'success') {
        alertContainer.innerHTML = `
            <div class="alert alert-${type === 'error' ? 'danger' : 'success'}">
                ${escapeHtml(message)}
            </div>
        `;
    }

    function toggleNotesField() {
        const isCancelled = statusSelect.value === 'CANCELLED';
        notesGroup.style.display = isCancelled ? '' : 'none';
        notesInput.required = isCancelled;

        if (!isCancelled) {
            notesInput.value = '';
        }
    }

    function renderSummary(campaign) {
        const statusLabel = campaign.status === 'OPEN'
            ? 'Abierta'
            : campaign.status === 'CLOSED'
                ? 'Cerrada'
                : 'Cancelada';

        summaryContent.className = 'campaign-update-summary__content';
        summaryContent.innerHTML = `
            <div class="campaign-update-summary__grid">
                <div class="campaign-update-summary__item">
                    <span class="campaign-update-summary__label">Nombre</span>
                    <span class="campaign-update-summary__value">${escapeHtml(campaign.name)}</span>
                </div>
                <div class="campaign-update-summary__item">
                    <span class="campaign-update-summary__label">Premio mayor</span>
                    <span class="campaign-update-summary__value">${escapeHtml(campaign.prizeName)}</span>
                </div>
                <div class="campaign-update-summary__item">
                    <span class="campaign-update-summary__label">Estado actual</span>
                    <span class="campaign-update-summary__value">${statusLabel}</span>
                </div>
                <div class="campaign-update-summary__item">
                    <span class="campaign-update-summary__label">Meta base</span>
                    <span class="campaign-update-summary__value">$${formatMoney(campaign.baseTargetAmount)}</span>
                </div>
            </div>
        `;
    }

    async function loadCampaign() {
        if (!Number.isFinite(campaignId) || campaignId <= 0) {
            summaryContent.textContent = 'No se pudo identificar la campana.';
            return;
        }

        try {
            const response = await fetch('/api/campaigns');

            if (!response.ok) {
                throw new Error('No se pudieron cargar las campanas.');
            }

            const campaigns = await response.json();
            const campaign = campaigns.find((item) => item.id === campaignId);

            if (!campaign) {
                summaryContent.textContent = 'No se encontro la campana solicitada.';
                submitButton.disabled = true;
                return;
            }

            renderSummary(campaign);
        } catch (error) {
            summaryContent.textContent = 'No fue posible cargar la campana.';
            submitButton.disabled = true;
        }
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const status = statusSelect.value;
        const notes = notesInput.value.trim();

        if (!status) {
            showAlert('Debes seleccionar una accion para la campana.', 'error');
            return;
        }

        if (status === 'CANCELLED' && !notes) {
            showAlert('Debes indicar el motivo de cancelacion.', 'error');
            return;
        }

        submitButton.disabled = true;

        try {
            const response = await fetch(`/api/campaigns/${campaignId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    status,
                    notes
                })
            });

            if (!response.ok) {
                throw new Error('No fue posible actualizar la campana.');
            }

            const responseText = await response.text();
            const updatedCampaign = responseText ? JSON.parse(responseText) : null;

            if (!updatedCampaign) {
                showAlert('No fue posible actualizar la campana.', 'error');
                return;
            }

            showAlert('Campana actualizada correctamente.');
            window.setTimeout(() => {
                window.location.href = '/campaigns';
            }, 1800);
        } catch (error) {
            showAlert('Ocurrio un error al actualizar la campana.', 'error');
        } finally {
            submitButton.disabled = false;
        }
    });

    statusSelect.addEventListener('change', toggleNotesField);

    toggleNotesField();
    loadCampaign();
});
