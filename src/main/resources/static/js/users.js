document.addEventListener('DOMContentLoaded', () => {
    const apiBase = '/api/users';

    const usersList = document.getElementById('usersList');
    const reloadUsersBtn = document.getElementById('reloadUsersBtn');
    const usersAlertContainer = document.getElementById('usersAlertContainer');

    const searchPhone = document.getElementById('searchPhone');
    const searchUserBtn = document.getElementById('searchUserBtn');
    const selectedUserContainer = document.getElementById('selectedUserContainer');

    const addPointsForm = document.getElementById('addPointsForm');
    const pointsToAdd = document.getElementById('pointsToAdd');
    const addPointsBtn = document.getElementById('addPointsBtn');

    const clearUserBtn = document.getElementById('clearUserBtn');

    let selectedUser = null;

    function showAlert(message, type = 'success') {
        usersAlertContainer.innerHTML = `
            <div class="users-alert users-alert--${type}">
                ${message}
            </div>
        `;

        setTimeout(() => {
            usersAlertContainer.innerHTML = '';
        }, 4000);
    }

    function clearSelectedUser() {
        selectedUser = null;

        pointsToAdd.value = '';
        pointsToAdd.disabled = true;
        addPointsBtn.disabled = true;
        clearUserBtn.disabled = true;

        selectedUserContainer.className = 'user-card-result user-card-result--empty';
        selectedUserContainer.innerHTML = `
        <div class="user-card-result__placeholder">
            Aquí aparecerá la información del usuario encontrado.
        </div>
    `;
    }

    function enableSelectedUserActions() {
        pointsToAdd.disabled = false;
        addPointsBtn.disabled = false;
        clearUserBtn.disabled = false;
    }

    function renderSelectedUser(user) {
        selectedUserContainer.className = 'user-card-result user-card-result--found';
        selectedUserContainer.innerHTML = `
            <div class="user-card-result__content">
                <p class="user-card-result__title">El usuario seleccionado es:</p>

                <div class="user-card-result__grid">
                    <div class="user-card-result__item">
                        <span class="user-card-result__label">Nombre</span>
                        <span class="user-card-result__value">${user.name ?? ''}</span>
                    </div>

                    <div class="user-card-result__item">
                        <span class="user-card-result__label">Teléfono</span>
                        <span class="user-card-result__value">${user.phone ?? ''}</span>
                    </div>

                    <div class="user-card-result__item">
                        <span class="user-card-result__label">Puntos</span>
                        <span class="user-card-result__value">${user.points ?? 0}</span>
                    </div>
                </div>
            </div>
        `;
    }

    function renderSelectedUserError(message) {
        selectedUserContainer.className = 'user-card-result user-card-result--error';
        selectedUserContainer.innerHTML = `
            <div class="user-card-result__content">
                <p class="user-card-result__message">${message}</p>
            </div>
        `;
        clearSelectedUser();
    }

    function renderSelectedUserLoading() {
        selectedUserContainer.className = 'user-card-result user-card-result--empty';
        selectedUserContainer.innerHTML = `
            <div class="user-card-result__placeholder">Buscando usuario...</div>
        `;
        clearSelectedUser();
    }

    function attachSelectUserActions() {
        document.querySelectorAll('.user-list-card__button').forEach(button => {
            button.addEventListener('click', async () => {
                const phone = button.getAttribute('data-phone');
                if (!phone) return;
                await searchUserByPhone(phone);
            });
        });
    }

    function renderUsers(users) {
        if (!users || users.length === 0) {
            usersList.innerHTML = `<div class="empty-state">No hay usuarios registrados por el momento.</div>`;
            return;
        }

        usersList.innerHTML = users.map(user => `
            <article class="app-card user-list-card">
                <div class="user-list-card__content">
                    <h3 class="user-list-card__title">Nombre del usuario: ${user.name ?? ''}</h3>

                    <div class="user-list-card__meta">
                        <div class="user-list-card__meta-item">
                            Teléfono:
                            <strong>${user.phone ?? ''}</strong>
                        </div>
                        <div class="user-list-card__meta-item">
                            Puntos:
                            <strong>${user.points ?? 0}</strong>
                        </div>
                    </div>
                </div>

                <div class="user-list-card__actions">
                    <button type="button"
                            class="user-list-card__button"
                            data-phone="${user.phone ?? ''}">
                        Seleccionar
                    </button>
                </div>
            </article>
        `).join('');

        attachSelectUserActions();
    }

    async function loadUsers() {
        usersList.innerHTML = `<div class="empty-state">Cargando usuarios...</div>`;

        try {
            const response = await fetch(apiBase);

            if (!response.ok) {
                throw new Error('No se pudieron cargar los usuarios.');
            }

            const users = await response.json();
            renderUsers(users);
        } catch (error) {
            usersList.innerHTML = `<div class="empty-state">No fue posible cargar los usuarios.</div>`;
        }
    }

    async function searchUserByPhone(phoneOverride = null) {
        const phone = (phoneOverride ?? searchPhone.value).trim();

        if (!phone) {
            renderSelectedUserError('Debes ingresar un teléfono para realizar la búsqueda.');
            return;
        }

        renderSelectedUserLoading();

        try {
            const response = await fetch(`${apiBase}/by-phone/${encodeURIComponent(phone)}`);

            if (!response.ok) {
                renderSelectedUserError('No existe usuario con ese teléfono.');
                return;
            }

            const user = await response.json();
            selectedUser = user;

            renderSelectedUser(user);
            enableSelectedUserActions();
            searchPhone.value = user.phone ?? '';
        } catch (error) {
            renderSelectedUserError('Ocurrió un error al buscar el usuario.');
        }
    }

    async function addPoints(event) {
        event.preventDefault();

        if (!selectedUser) {
            showAlert('Primero debes seleccionar un usuario.', 'error');
            return;
        }

        const points = Number(pointsToAdd.value);

        if (!Number.isFinite(points) || points <= 0) {
            showAlert('Debes capturar una cantidad válida de puntos.', 'error');
            return;
        }

        const payload = {
            phone: selectedUser.phone,
            points: points
        };

        try {
            const response = await fetch(`${apiBase}/add-points`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                throw new Error('No fue posible agregar puntos.');
            }

            const updatedUser = await response.json();
            selectedUser = updatedUser;

            renderSelectedUser(updatedUser);
            pointsToAdd.value = '';
            searchPhone.value = updatedUser.phone ?? '';

            showAlert('Puntos agregados correctamente.', 'success');
            await loadUsers();
        } catch (error) {
            showAlert('Ocurrió un error al agregar puntos.', 'error');
        }
    }

    reloadUsersBtn?.addEventListener('click', loadUsers);
    searchUserBtn?.addEventListener('click', () => searchUserByPhone());
    clearUserBtn?.addEventListener('click', clearSelectedUser);

    searchPhone?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchUserByPhone();
        }
    });

    addPointsForm?.addEventListener('submit', addPoints);

    loadUsers();
});