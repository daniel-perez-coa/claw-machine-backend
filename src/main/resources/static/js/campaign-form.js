document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('campaignForm');
    const alertContainer = document.getElementById('campaignFormAlertContainer');
    const submitButton = document.getElementById('submitCampaignBtn');
    const campaignNameInput = document.getElementById('campaignName');
    const majorPrizeSelect = document.getElementById('majorPrizeId');
    const baseTargetAmountInput = document.getElementById('baseTargetAmount');

    if (!form || !alertContainer || !submitButton || !campaignNameInput || !majorPrizeSelect || !baseTargetAmountInput) {
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

    function showAlert(message, type = 'success') {
        alertContainer.innerHTML = `
            <div class="alert alert-${type === 'error' ? 'danger' : 'success'}">
                ${escapeHtml(message)}
            </div>
        `;
    }

    function redirectToCampaignsWithDelay() {
        window.setTimeout(() => {
            window.location.href = '/campaigns';
        }, 1800);
    }

    function renderPrizeOptions(prizes) {
        majorPrizeSelect.innerHTML = `
            <option value="">Selecciona un premio</option>
            ${prizes.map((prize) => `
                <option value="${prize.id}">
                    ${escapeHtml(prize.name)} - ${prize.pointsCost ?? 0} pts
                </option>
            `).join('')}
        `;
    }

    async function loadPrizes() {
        try {
            const response = await fetch('/api/prizes/active');

            if (!response.ok) {
                throw new Error('No fue posible cargar los premios.');
            }

            const prizes = await response.json();
            const majorPrizes = prizes.filter((prize) => prize.prizeCode === 'MAYOR');

            renderPrizeOptions(majorPrizes);

            if (majorPrizes.length === 0) {
                showAlert('No hay premios activos con categoria MAYOR disponibles.', 'error');
            }
        } catch (error) {
            showAlert('No fue posible cargar los premios activos.', 'error');
        }
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const name = campaignNameInput.value.trim();
        const majorPrizeId = Number(majorPrizeSelect.value);
        const baseTargetAmount = Number(baseTargetAmountInput.value);

        if (!name) {
            showAlert('Debes capturar un nombre para la campana.', 'error');
            return;
        }

        if (!Number.isFinite(majorPrizeId) || majorPrizeId <= 0) {
            showAlert('Debes seleccionar un premio mayor valido.', 'error');
            return;
        }

        if (!Number.isFinite(baseTargetAmount) || baseTargetAmount <= 0) {
            showAlert('Debes capturar una meta base valida.', 'error');
            return;
        }

        submitButton.disabled = true;

        try {
            const response = await fetch('/api/campaigns', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    name,
                    majorPrizeId,
                    baseTargetAmount
                })
            });

            if (!response.ok) {
                throw new Error('No fue posible crear la campana.');
            }

            const responseText = await response.text();
            const campaign = responseText ? JSON.parse(responseText) : null;

            if (!campaign) {
                showAlert('No se pudo crear la campana. Verifica si ya existe una campana abierta o si el premio ya no esta disponible.', 'error');
                return;
            }

            showAlert('Campana creada correctamente.');
            form.reset();
            redirectToCampaignsWithDelay();
        } catch (error) {
            showAlert('Ocurrio un error al crear la campana.', 'error');
        } finally {
            submitButton.disabled = false;
        }
    });

    loadPrizes();
});
