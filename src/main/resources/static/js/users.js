document.addEventListener('DOMContentLoaded', () => {
    const apiBase = '/api/users';

    const usersList = document.getElementById('usersList');
    const usersPagination = document.getElementById('usersPagination');
    const reloadUsersBtn = document.getElementById('reloadUsersBtn');
    const usersAlertContainer = document.getElementById('usersAlertContainer');
    const pageSize = 6;

    const searchPhone = document.getElementById('searchPhone');
    const searchUserBtn = document.getElementById('searchUserBtn');
    const clearFiltersBtn = document.getElementById('clearFiltersBtn');

    const addPointsForm = document.getElementById('addPointsForm');
    const pointsToAdd = document.getElementById('pointsToAdd');
    const addPointsBtn = document.getElementById('addPointsBtn');

    const clearUserBtn = document.getElementById('clearUserBtn');
    const confirmAddPointsModalElement = document.getElementById('confirmAddPointsModal');
    const confirmPointsAmount = document.getElementById('confirmPointsAmount');
    const confirmUserName = document.getElementById('confirmUserName');
    const confirmAddPointsBtn = document.getElementById('confirmAddPointsBtn');
    const confirmAddPointsModal = confirmAddPointsModalElement && window.bootstrap
        ? new window.bootstrap.Modal(confirmAddPointsModalElement)
        : null;

    let selectedUser = null;
    let allUsers = [];
    let currentPage = 1;

    function sanitizePhone(value) {
        return String(value ?? '').replaceAll(/\D/g, '').slice(0, 10);
    }

    function sanitizePositiveInteger(value) {
        return String(value ?? '').replaceAll(/\D/g, '');
    }

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

    function getFilteredUsers() {
        const query = searchPhone?.value.trim().toLowerCase() ?? '';

        if (!query) {
            return allUsers;
        }

        return allUsers.filter((user) =>
            String(user.phone ?? '').toLowerCase().includes(query) ||
            String(user.name ?? '').toLowerCase().includes(query)
        );
    }

    function renderPagination(totalItems) {
        if (!usersPagination) {
            return;
        }

        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

        usersPagination.innerHTML = `
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    data-page-action="previous"
                    ${currentPage === 1 ? 'disabled' : ''}>
                Anterior
            </button>
            <span class="reports-pagination__info">Pagina ${currentPage} de ${totalPages}</span>
            <button type="button"
                    class="reports-action-card__button reports-action-card__button--secondary reports-pagination__button"
                    data-page-action="next"
                    ${currentPage === totalPages ? 'disabled' : ''}>
                Siguiente
            </button>
        `;
    }

    function clearSelectedUser() {
        selectedUser = null;

        if (searchPhone) {
            searchPhone.value = '';
        }

        pointsToAdd.value = '';
        pointsToAdd.disabled = true;
        addPointsBtn.disabled = true;
        clearUserBtn.disabled = true;

        currentPage = 1;
        renderUsers(allUsers);
    }

    function enableSelectedUserActions() {
        pointsToAdd.disabled = false;
        addPointsBtn.disabled = false;
        clearUserBtn.disabled = false;
    }

    function attachSelectUserActions() {
        document.querySelectorAll('.user-list-card__button').forEach((button) => {
            button.addEventListener('click', () => {
                const phone = button.getAttribute('data-phone');
                if (!phone) {
                    return;
                }

                const user = allUsers.find((item) => item.phone === phone);
                if (!user) {
                    return;
                }

                selectedUser = user;
                enableSelectedUserActions();
                searchPhone.value = user.phone ?? '';
                renderUsers(getFilteredUsers());
            });
        });
    }

    function renderUsers(users) {
        if (!users || users.length === 0) {
            usersList.innerHTML = `<div class="empty-state">No hay usuarios que coincidan con la busqueda.</div>`;
            renderPagination(0);
            return;
        }

        const totalPages = Math.max(1, Math.ceil(users.length / pageSize));
        currentPage = Math.min(Math.max(1, currentPage), totalPages);
        const startIndex = (currentPage - 1) * pageSize;
        const paginatedUsers = users.slice(startIndex, startIndex + pageSize);

        usersList.innerHTML = paginatedUsers.map((user) => {
            const isSelected = selectedUser?.phone === user.phone;

            return `
                <article class="app-card user-list-card ${isSelected ? 'user-list-card--selected' : ''}">
                    <div class="user-list-card__content">
                        <h3 class="user-list-card__title">Nombre del usuario: ${user.name ?? ''}</h3>

                        <div class="user-list-card__meta">
                            <div class="user-list-card__meta-item">
                                Telefono:
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
                                class="user-list-card__button ${isSelected ? 'user-list-card__button--selected' : ''}"
                                data-phone="${user.phone ?? ''}">
                            Seleccionar
                        </button>

                        <a class="user-list-card__link"
                           href="/users/${encodeURIComponent(user.phone ?? '')}/edit">
                            Editar
                        </a>
                    </div>
                </article>
            `;
        }).join('');

        attachSelectUserActions();
        renderPagination(users.length);
    }

    async function loadUsers() {
        usersList.innerHTML = `<div class="empty-state">Cargando usuarios...</div>`;
        if (usersPagination) {
            usersPagination.innerHTML = '';
        }

        try {
            const response = await fetch(apiBase);

            if (!response.ok) {
                throw new Error('No se pudieron cargar los usuarios.');
            }

            allUsers = await response.json();
            currentPage = 1;
            renderUsers(getFilteredUsers());
        } catch (error) {
            usersList.innerHTML = `<div class="empty-state">No fue posible cargar los usuarios.</div>`;
            if (usersPagination) {
                usersPagination.innerHTML = '';
            }
        }
    }

    function filterUsers() {
        currentPage = 1;
        renderUsers(getFilteredUsers());
    }

    function clearFilters() {
        if (searchPhone) {
            searchPhone.value = '';
        }

        currentPage = 1;
        renderUsers(allUsers);
    }

    async function submitAddPoints() {
        if (!selectedUser) {
            showAlert('Primero debes seleccionar un usuario.', 'error');
            return;
        }

        const points = Number(pointsToAdd.value);

        if (!Number.isFinite(points) || points <= 0) {
            showAlert('Debes capturar una cantidad valida de puntos.', 'error');
            return;
        }

        const payload = {
            phone: selectedUser.phone,
            points
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

            const result = await response.json();
            const updatedUser = result.user;

            selectedUser = updatedUser;
            allUsers = allUsers.map((user) => user.phone === updatedUser.phone ? updatedUser : user);

            pointsToAdd.value = '';
            confirmAddPointsModal?.hide();
            clearSelectedUser();

            showAlert('Puntos agregados correctamente.', 'success');

            try {
                if (result.transactionId && window.appReportPrinter) {
                    await window.appReportPrinter.printThermalTicketFromUrl(`/api/reports/tickets/add-points/${result.transactionId}/thermal-print`);
                }
            } catch (printError) {
                showAlert('Los puntos se agregaron, pero no fue posible imprimir el ticket termico.', 'error');
            }
        } catch (error) {
            showAlert('Ocurrio un error al agregar puntos.', 'error');
        }
    }

    function openAddPointsConfirmation(event) {
        event.preventDefault();

        if (!selectedUser) {
            showAlert('Primero debes seleccionar un usuario.', 'error');
            return;
        }

        const points = Number(pointsToAdd.value);

        if (!Number.isFinite(points) || points <= 0) {
            showAlert('Debes capturar una cantidad valida de puntos.', 'error');
            return;
        }

        if (confirmPointsAmount) {
            confirmPointsAmount.textContent = String(points);
        }

        if (confirmUserName) {
            confirmUserName.textContent = selectedUser.name ?? '';
        }

        confirmAddPointsModal?.show();
    }

    reloadUsersBtn?.addEventListener('click', loadUsers);
    searchUserBtn?.addEventListener('click', filterUsers);
    clearFiltersBtn?.addEventListener('click', clearFilters);
    clearUserBtn?.addEventListener('click', clearSelectedUser);
    confirmAddPointsBtn?.addEventListener('click', submitAddPoints);

    searchPhone?.addEventListener('input', () => {
        const sanitizedValue = sanitizePhone(searchPhone.value);
        if (sanitizedValue !== searchPhone.value) {
            searchPhone.value = sanitizedValue;
        }
    });

    pointsToAdd?.addEventListener('input', () => {
        const sanitizedValue = sanitizePositiveInteger(pointsToAdd.value);
        if (sanitizedValue !== pointsToAdd.value) {
            pointsToAdd.value = sanitizedValue;
        }
    });

    searchPhone?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            filterUsers();
        }
    });

    usersPagination?.addEventListener('click', (event) => {
        const button = event.target.closest('[data-page-action]');

        if (!button || button.disabled) {
            return;
        }

        const filteredUsers = getFilteredUsers();

        if (button.dataset.pageAction === 'previous' && currentPage > 1) {
            currentPage -= 1;
            renderUsers(filteredUsers);
            return;
        }

        if (button.dataset.pageAction === 'next' && currentPage < Math.ceil(filteredUsers.length / pageSize)) {
            currentPage += 1;
            renderUsers(filteredUsers);
        }
    });

    addPointsForm?.addEventListener('submit', openAddPointsConfirmation);

    loadUsers();
});
