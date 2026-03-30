document.addEventListener('DOMContentLoaded', () => {
    const phoneSearchInput = document.getElementById('phoneSearch');
    const searchUserBtn = document.getElementById('searchUserBtn');
    const userResultContainer = document.getElementById('userResultContainer');
    const selectedUserPhone = document.getElementById('selectedUserPhone');
    const prizeSelect = document.getElementById('prizeId');
    const redeemBtn = document.getElementById('redeemBtn');

    if (!phoneSearchInput || !searchUserBtn || !userResultContainer || !selectedUserPhone || !prizeSelect || !redeemBtn) {
        return;
    }

    function resetSelection() {
        selectedUserPhone.value = '';
        prizeSelect.disabled = true;
        redeemBtn.disabled = true;
        prizeSelect.value = '';
    }

    function renderEmpty(message) {
        userResultContainer.className = 'user-result user-result--empty';
        userResultContainer.innerHTML = `
            <div class="user-result__placeholder">${message}</div>
        `;
        resetSelection();
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
                        <span class="user-result__value">${user.name ?? ''}</span>
                    </div>

                    <div class="user-result__item">
                        <span class="user-result__label">Teléfono</span>
                        <span class="user-result__value">${user.phone ?? ''}</span>
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
                <p class="user-result__message">No existe usuario con ese teléfono.</p>
            </div>
        `;
        resetSelection();
    }

    function renderError() {
        userResultContainer.className = 'user-result user-result--not-found';
        userResultContainer.innerHTML = `
            <div class="user-result__content">
                <p class="user-result__message">Ocurrió un error al buscar el usuario.</p>
            </div>
        `;
        resetSelection();
    }

    async function searchUserByPhone() {
        const phone = phoneSearchInput.value.trim();

        if (!phone) {
            renderEmpty('Debes ingresar un teléfono para realizar la búsqueda.');
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

    searchUserBtn.addEventListener('click', searchUserByPhone);

    phoneSearchInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchUserByPhone();
        }
    });
});