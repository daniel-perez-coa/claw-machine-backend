document.addEventListener('DOMContentLoaded', () => {
    const phoneSearchInput = document.getElementById('phoneSearch');
    const searchUserBtn = document.getElementById('searchUserBtn');
    const userResultContainer = document.getElementById('userResultContainer');
    const selectedUserPhone = document.getElementById('selectedUserPhone');
    const prizeSelect = document.getElementById('prizeId');
    const redeemBtn = document.getElementById('redeemBtn');
    const redeemForm = document.getElementById('redeemForm');
    const printTicketBtn = document.getElementById('printTicketBtn');
    const alertContainer = document.getElementById('redemptionsAlertContainer');
    const redemptionResultSection = document.getElementById('redemptionResultSection');
    const redemptionResultGrid = document.getElementById('redemptionResultGrid');
    const redemptionsContent = document.getElementById('redemptionsContent');
    const redemptionsUnavailableState = document.getElementById('redemptionsUnavailableState');

    if (!phoneSearchInput || !searchUserBtn || !userResultContainer || !selectedUserPhone || !prizeSelect ||
        !redeemBtn || !redeemForm || !printTicketBtn || !alertContainer || !redemptionResultSection ||
        !redemptionResultGrid || !redemptionsContent || !redemptionsUnavailableState) {
        return;
    }

    let lastRedemptionId = null;

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function sanitizePhone(value) {
        return String(value ?? '').replaceAll(/\D/g, '').slice(0, 10);
    }

    function showAlert(message, type = 'success') {
        alertContainer.innerHTML = `
            <div class="redemptions-alert redemptions-alert--${type === 'error' ? 'error' : 'success'}">
                ${escapeHtml(message)}
            </div>
        `;

        setTimeout(() => {
            alertContainer.innerHTML = '';
        }, 4000);
    }

    function showUnavailableState() {
        redemptionsContent.style.display = 'none';
        redemptionResultSection.style.display = 'none';
        redemptionsUnavailableState.style.display = '';
        resetSelection();
    }

    function showContent() {
        redemptionsContent.style.display = '';
        redemptionsUnavailableState.style.display = 'none';
    }

    function resetSelection() {
        selectedUserPhone.value = '';
        prizeSelect.disabled = true;
        redeemBtn.disabled = true;
        prizeSelect.value = '';
        lastRedemptionId = null;
        printTicketBtn.style.display = 'none';
    }

    function hideResult() {
        redemptionResultSection.style.display = 'none';
        redemptionResultGrid.innerHTML = '';
        lastRedemptionId = null;
        printTicketBtn.style.display = 'none';
    }

    function renderEmpty(message) {
        userResultContainer.className = 'user-result user-result--empty';
        userResultContainer.innerHTML = `
            <div class="user-result__placeholder">${escapeHtml(message)}</div>
        `;
        resetSelection();
        hideResult();
    }

    function renderLoading() {
        userResultContainer.className = 'user-result user-result--loading';
        userResultContainer.innerHTML = `
            <div class="user-result__placeholder">Buscando usuario...</div>
        `;
        resetSelection();
    }

    function renderUser(user) {
        userResultContainer.className = 'user-result user-result--found';
        userResultContainer.innerHTML = `
            <div class="user-result__content">
                <p class="user-result__title">El usuario seleccionado es:</p>

                <div class="user-result__grid">
                    <div class="user-result__item">
                        <span class="user-result__label">Nombre</span>
                        <span class="user-result__value">${escapeHtml(user.name)}</span>
                    </div>

                    <div class="user-result__item">
                        <span class="user-result__label">Telefono</span>
                        <span class="user-result__value">${escapeHtml(user.phone)}</span>
                    </div>

                    <div class="user-result__item">
                        <span class="user-result__label">Puntos</span>
                        <span class="user-result__value">${user.points ?? 0}</span>
                    </div>
                </div>
            </div>
        `;

        selectedUserPhone.value = user.phone ?? '';
        prizeSelect.disabled = false;
        redeemBtn.disabled = false;
    }

    function renderNotFound() {
        userResultContainer.className = 'user-result user-result--not-found';
        userResultContainer.innerHTML = `
            <div class="user-result__content">
                <p class="user-result__message">No existe usuario con ese telefono.</p>
            </div>
        `;
        resetSelection();
        hideResult();
    }

    function renderError() {
        userResultContainer.className = 'user-result user-result--not-found';
        userResultContainer.innerHTML = `
            <div class="user-result__content">
                <p class="user-result__message">Ocurrio un error al buscar el usuario.</p>
            </div>
        `;
        resetSelection();
        hideResult();
    }

    function renderPrizeOptions(prizes) {
        prizeSelect.innerHTML = `
            <option value="">Selecciona un premio</option>
            ${prizes.map((prize) => `
                <option value="${prize.id}">
                    ${escapeHtml(prize.name)} - ${prize.pointsCost ?? 0} pts
                </option>
            `).join('')}
        `;
    }

    function renderRedemptionResult(result) {
        redemptionResultGrid.innerHTML = `
            <div class="redemption-result-item">
                <span class="redemption-result-item__label">Usuario</span>
                <span class="redemption-result-item__value">${escapeHtml(result.userName)}</span>
            </div>

            <div class="redemption-result-item">
                <span class="redemption-result-item__label">Telefono</span>
                <span class="redemption-result-item__value">${escapeHtml(result.userPhone)}</span>
            </div>

            <div class="redemption-result-item">
                <span class="redemption-result-item__label">Premio</span>
                <span class="redemption-result-item__value">${escapeHtml(result.prizeName)}</span>
            </div>

            <div class="redemption-result-item">
                <span class="redemption-result-item__label">Puntos gastados</span>
                <span class="redemption-result-item__value">${result.pointsSpent ?? 0}</span>
            </div>

            <div class="redemption-result-item">
                <span class="redemption-result-item__label">Puntos restantes</span>
                <span class="redemption-result-item__value">${result.remainingPoints ?? 0}</span>
            </div>
        `;

        redemptionResultSection.style.display = '';
        lastRedemptionId = result.redemptionId ?? null;
        printTicketBtn.style.display = lastRedemptionId ? '' : 'none';
    }

    async function hasOpenCampaign() {
        const response = await fetch('/api/campaigns');

        if (!response.ok) {
                throw new Error('No se pudieron cargar las campañas.');
        }

        const campaigns = await response.json();
        return Array.isArray(campaigns) && campaigns.some((campaign) => campaign.status === 'OPEN');
    }

    async function loadPrizes() {
        try {
            const openCampaignExists = await hasOpenCampaign();

            if (!openCampaignExists) {
                showUnavailableState();
                return;
            }

            const response = await fetch('/api/prizes/active');

            if (!response.ok) {
                throw new Error('No se pudieron cargar los premios.');
            }

            const prizes = (await response.json()).filter((prize) => Number.isInteger(prize.pointsCost) && prize.pointsCost > 0);

            if (!prizes || prizes.length === 0) {
                showContent();
                renderPrizeOptions([]);
                return;
            }

            showContent();
            renderPrizeOptions(prizes);
        } catch (error) {
            showUnavailableState();
        }
    }

    async function searchUserByPhone() {
        const phone = sanitizePhone(phoneSearchInput.value).trim();
        phoneSearchInput.value = phone;

        if (!phone) {
            renderEmpty('Debes ingresar un telefono para realizar la busqueda.');
            return;
        }

        renderLoading();

        try {
            const response = await fetch(`/api/users/by-phone/${encodeURIComponent(phone)}`);

            if (!response.ok) {
                renderNotFound();
                return;
            }

            const user = await response.json();
            renderUser(user);
        } catch (error) {
            renderError();
        }
    }

    async function refreshUserCard(phone) {
        const response = await fetch(`/api/users/by-phone/${encodeURIComponent(phone)}`);

        if (!response.ok) {
            throw new Error('No fue posible recargar el usuario.');
        }

        const user = await response.json();
        renderUser(user);
    }

    async function redeemPrize(event) {
        event.preventDefault();

        const phone = selectedUserPhone.value.trim();
        const prizeId = Number(prizeSelect.value);

        if (!phone) {
            showAlert('Primero debes seleccionar un usuario.', 'error');
            return;
        }

        if (!Number.isFinite(prizeId) || prizeId <= 0) {
            showAlert('Debes seleccionar un premio valido.', 'error');
            return;
        }

        redeemBtn.disabled = true;

        try {
            const response = await fetch('/api/prize-redemption', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    phone,
                    prizeId
                })
            });

            if (!response.ok) {
                throw new Error('No fue posible realizar el canje.');
            }

            const result = await response.json();
            renderRedemptionResult(result);
            showAlert('Canje realizado correctamente.', 'success');
            await refreshUserCard(phone);
            prizeSelect.value = '';
        } catch (error) {
            showAlert('Ocurrio un error al realizar el canje.', 'error');
            hideResult();
        } finally {
            redeemBtn.disabled = !selectedUserPhone.value;
        }
    }

    async function printTicket() {
        if (!lastRedemptionId || !window.appReportPrinter) {
            return;
        }

        try {
            await window.appReportPrinter.printThermalTicketFromUrl(`/api/reports/tickets/user-redemption/${lastRedemptionId}/thermal-print`);
        } catch (error) {
            showAlert('No fue posible imprimir el ticket termico.', 'error');
        }
    }

    searchUserBtn.addEventListener('click', searchUserByPhone);
    redeemForm.addEventListener('submit', redeemPrize);
    printTicketBtn.addEventListener('click', printTicket);

    phoneSearchInput.addEventListener('input', () => {
        const sanitizedValue = sanitizePhone(phoneSearchInput.value);
        if (sanitizedValue !== phoneSearchInput.value) {
            phoneSearchInput.value = sanitizedValue;
        }
    });

    phoneSearchInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchUserByPhone();
        }
    });

    loadPrizes();
});
