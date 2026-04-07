document.addEventListener('DOMContentLoaded', () => {
    const phoneSearchInput = document.getElementById('phoneSearch');
    const searchUserBtn = document.getElementById('searchUserBtn');
    const userResultContainer = document.getElementById('userResultContainer');
    const selectedUserPhone = document.getElementById('selectedUserPhone');
    const prizeSelect = document.getElementById('prizeId');
    const redeemBtn = document.getElementById('redeemBtn');
    const redeemForm = document.getElementById('redeemForm');
    const alertContainer = document.getElementById('redemptionsAlertContainer');
    const redemptionResultSection = document.getElementById('redemptionResultSection');
    const redemptionResultGrid = document.getElementById('redemptionResultGrid');

    if (!phoneSearchInput || !searchUserBtn || !userResultContainer || !selectedUserPhone || !prizeSelect ||
        !redeemBtn || !redeemForm || !alertContainer || !redemptionResultSection || !redemptionResultGrid) {
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
            <div class="redemptions-alert redemptions-alert--${type === 'error' ? 'error' : 'success'}">
                ${escapeHtml(message)}
            </div>
        `;

        setTimeout(() => {
            alertContainer.innerHTML = '';
        }, 4000);
    }

    function resetSelection() {
        selectedUserPhone.value = '';
        prizeSelect.disabled = true;
        redeemBtn.disabled = true;
        prizeSelect.value = '';
    }

    function hideResult() {
        redemptionResultSection.style.display = 'none';
        redemptionResultGrid.innerHTML = '';
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
    }

    async function loadPrizes() {
        try {
            const response = await fetch('/api/prizes');

            if (!response.ok) {
                throw new Error('No se pudieron cargar los premios.');
            }

            const prizes = await response.json();
            renderPrizeOptions(prizes);
        } catch (error) {
            showAlert('No fue posible cargar los premios.', 'error');
        }
    }

    async function searchUserByPhone() {
        const phone = phoneSearchInput.value.trim();

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
            await searchUserByPhone();
            prizeSelect.value = '';
        } catch (error) {
            showAlert('Ocurrio un error al realizar el canje.', 'error');
            hideResult();
        } finally {
            redeemBtn.disabled = !selectedUserPhone.value;
        }
    }

    searchUserBtn.addEventListener('click', searchUserByPhone);
    redeemForm.addEventListener('submit', redeemPrize);

    phoneSearchInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchUserByPhone();
        }
    });

    loadPrizes();
});
