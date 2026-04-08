document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('userForm');
    const alertContainer = document.getElementById('userFormAlertContainer');
    const submitButton = document.getElementById('submitUserBtn');
    const nameInput = document.getElementById('userName');
    const phoneInput = document.getElementById('userPhone');
    const confirmPhoneInput = document.getElementById('confirmUserPhone');
    const userPointsSection = document.getElementById('userPointsSection');
    const currentPointsInput = document.getElementById('userCurrentPoints');
    const removePointsAmountInput = document.getElementById('removePointsAmount');
    const removePointsNotesInput = document.getElementById('removePointsNotes');
    const removePointsButton = document.getElementById('removePointsBtn');
    const deleteUserButton = document.getElementById('deleteUserBtn');
    const originalPhone = form?.dataset.userPhone?.trim() ?? '';
    const isEditMode = Boolean(originalPhone);
    let currentPoints = 0;

    if (!form || !alertContainer || !submitButton || !nameInput || !phoneInput) {
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

    function sanitizeName(value) {
        return value.replaceAll(/[^A-Za-zÁÉÍÓÚáéíóúÑñÜü ]/g, '');
    }

    function sanitizePhone(value) {
        return value.replaceAll(/\D/g, '').slice(0, 10);
    }

    function sanitizePositiveInteger(value) {
        return value.replaceAll(/\D/g, '');
    }

    function toTitleCase(value) {
        return String(value ?? '')
            .trim()
            .replaceAll(/\s+/g, ' ')
            .split(' ')
            .filter(Boolean)
            .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
            .join(' ');
    }

    function getUserSaveErrorMessage(response, isEditing) {
        if (response.status === 409) {
            return isEditing
                ? 'Ya existe otro usuario con ese nombre o telefono.'
                : 'Ya existe un usuario con ese nombre o telefono.';
        }

        if (response.status === 400) {
            return isEditing
                ? 'La informacion del usuario no es valida.'
                : 'La informacion del usuario no es valida.';
        }

        return isEditing
            ? 'No fue posible actualizar el usuario.'
            : 'No fue posible crear el usuario.';
    }

    async function lookupUser(name, phone) {
        const response = await fetch(`/api/users/lookup?name=${encodeURIComponent(name)}&phone=${encodeURIComponent(phone)}`);

        if (!response.ok) {
            throw new Error('No fue posible verificar el usuario existente.');
        }

        return response.json();
    }

    async function reactivateUser(userId, payload) {
        const response = await fetch(`/api/users/${userId}/reactivate`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error('No fue posible reactivar el usuario.');
        }

        return response.json();
    }

    function syncCurrentPoints() {
        if (currentPointsInput) {
            currentPointsInput.value = String(currentPoints);
        }
    }

    async function loadUser() {
        const response = await fetch(`/api/users/by-phone/${encodeURIComponent(originalPhone)}`);

        if (!response.ok) {
            throw new Error('No fue posible cargar el usuario.');
        }

        const user = await response.json();
        nameInput.value = sanitizeName(user.name ?? '');
        phoneInput.value = sanitizePhone(user.phone ?? '');
        currentPoints = Number(user.points ?? 0);
        syncCurrentPoints();
    }

    nameInput.addEventListener('input', () => {
        const sanitizedValue = sanitizeName(nameInput.value);
        if (sanitizedValue !== nameInput.value) {
            nameInput.value = sanitizedValue;
        }
    });

    nameInput.addEventListener('blur', () => {
        nameInput.value = toTitleCase(sanitizeName(nameInput.value));
    });

    phoneInput.addEventListener('input', () => {
        const sanitizedValue = sanitizePhone(phoneInput.value);
        if (sanitizedValue !== phoneInput.value) {
            phoneInput.value = sanitizedValue;
        }
    });

    confirmPhoneInput?.addEventListener('input', () => {
        const sanitizedValue = sanitizePhone(confirmPhoneInput.value);
        if (sanitizedValue !== confirmPhoneInput.value) {
            confirmPhoneInput.value = sanitizedValue;
        }
    });

    removePointsAmountInput?.addEventListener('input', () => {
        const sanitizedValue = sanitizePositiveInteger(removePointsAmountInput.value);
        if (sanitizedValue !== removePointsAmountInput.value) {
            removePointsAmountInput.value = sanitizedValue;
        }
    });

    async function removePoints() {
        if (!isEditMode || !removePointsAmountInput || !removePointsNotesInput || !removePointsButton) {
            return;
        }

        const pointsValue = sanitizePositiveInteger(removePointsAmountInput.value).trim();
        const notes = removePointsNotesInput.value.trim();
        const points = Number(pointsValue);

        removePointsAmountInput.value = pointsValue;
        removePointsNotesInput.value = notes;

        if (!pointsValue || !Number.isInteger(points) || points <= 0) {
            showAlert('Debes ingresar una cantidad valida de puntos a quitar.', 'error');
            return;
        }

        if (!notes) {
            showAlert('Debes ingresar observaciones para modificar los puntos del usuario.', 'error');
            return;
        }

        if (points > currentPoints) {
            showAlert('No es posible quitar mas puntos de los que el usuario tiene disponibles.', 'error');
            return;
        }

        const confirmed = await window.showAppConfirmModal?.({
            title: 'Confirmar ajuste',
            body: `Se quitaran ${points} punto(s) al usuario ${nameInput.value.trim()}. Esta accion modificara su saldo actual.`,
            confirmText: 'Confirmar',
            cancelText: 'Cancelar',
            confirmVariant: 'success'
        });

        if (!confirmed) {
            return;
        }

        removePointsButton.disabled = true;

        try {
            const response = await fetch('/api/users/remove-points', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    phone: originalPhone,
                    points,
                    notes
                })
            });

            if (!response.ok) {
                throw new Error('No fue posible quitar los puntos del usuario.');
            }

            const user = await response.json();
            currentPoints = Number(user.points ?? 0);
            syncCurrentPoints();
            removePointsAmountInput.value = '';
            removePointsNotesInput.value = '';
            showAlert('Puntos quitados correctamente.');
            window.setTimeout(() => {
                window.location.href = '/users';
            }, 900);
        } catch (error) {
            showAlert('Ocurrio un error al quitar los puntos del usuario.', 'error');
        } finally {
            removePointsButton.disabled = false;
        }
    }

    async function deleteUser() {
        if (!isEditMode || !deleteUserButton) {
            return;
        }

        const confirmed = await window.showAppConfirmModal?.({
            title: 'Eliminar usuario',
            body: `Se desactivara el usuario ${nameInput.value.trim()}. Podra reactivarse despues con el mismo nombre o telefono.`,
            confirmText: 'Desactivar',
            cancelText: 'Cancelar',
            cancelVariant: 'secondary',
            confirmVariant: 'danger'
        });

        if (!confirmed) {
            return;
        }

        deleteUserButton.disabled = true;

        try {
            const response = await fetch(`/api/users/${encodeURIComponent(originalPhone)}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('No fue posible desactivar el usuario.');
            }

            showAlert('Usuario desactivado correctamente.');
            window.setTimeout(() => {
                window.location.href = '/users';
            }, 900);
        } catch (error) {
            showAlert(error.message || 'Ocurrio un error al desactivar el usuario.', 'error');
            deleteUserButton.disabled = false;
        }
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const name = toTitleCase(sanitizeName(nameInput.value));
        const phone = sanitizePhone(phoneInput.value).trim();
        const confirmedPhone = sanitizePhone(confirmPhoneInput?.value ?? '').trim();

        nameInput.value = name;
        phoneInput.value = phone;
        if (confirmPhoneInput) {
            confirmPhoneInput.value = confirmedPhone;
        }

        if (!name || !phone || (!isEditMode && !confirmedPhone)) {
            showAlert('Debes completar todos los campos obligatorios.', 'error');
            return;
        }

        if (/\d/.test(name)) {
            showAlert('El nombre no puede contener numeros.', 'error');
            return;
        }

        if (!/^\d{10}$/.test(phone)) {
            showAlert('El telefono debe contener exactamente 10 digitos numericos.', 'error');
            return;
        }

        if (!isEditMode && !/^\d{10}$/.test(confirmedPhone)) {
            showAlert('La confirmacion del telefono debe contener exactamente 10 digitos numericos.', 'error');
            return;
        }

        if (!isEditMode && phone !== confirmedPhone) {
            showAlert('Los telefonos capturados no coinciden.', 'error');
            return;
        }

        submitButton.disabled = true;

        try {
            const payload = isEditMode
                ? { name, phone, points: currentPoints }
                : { name, phone };

            const response = await fetch(isEditMode ? `/api/users/${encodeURIComponent(originalPhone)}` : '/api/users', {
                method: isEditMode ? 'PUT' : 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                if (!isEditMode && response.status === 409) {
                    const existingUser = await lookupUser(name, phone);

                    if (existingUser && existingUser.isActive === false) {
                        const confirmed = await window.showAppConfirmModal?.({
                            title: 'Reactivar usuario',
                            body: 'Ya existe un usuario inactivo con ese nombre o telefono. ¿Desea reactivarlo?',
                            confirmText: 'Reactivar',
                            cancelText: 'Cancelar',
                            cancelVariant: 'secondary',
                            confirmVariant: 'success'
                        });

                        if (!confirmed) {
                            throw new Error('Ya existe un usuario con ese nombre o telefono.');
                        }

                        await reactivateUser(existingUser.id, { name, phone });
                        showAlert('Usuario reactivado correctamente.');
                        window.setTimeout(() => {
                            window.location.href = '/users';
                        }, 1200);
                        return;
                    }
                }

                throw new Error(getUserSaveErrorMessage(response, isEditMode));
            }

            const responseText = await response.text();
            const user = responseText ? JSON.parse(responseText) : null;

            if (!user) {
                showAlert(isEditMode ? 'No fue posible actualizar el usuario.' : 'No fue posible crear el usuario.', 'error');
                return;
            }

            showAlert(isEditMode ? 'Usuario actualizado correctamente.' : 'Usuario creado correctamente.');

            if (!isEditMode) {
                form.reset();
            }

            window.setTimeout(() => {
                window.location.href = '/users';
            }, 1800);
        } catch (error) {
            showAlert(error.message || (isEditMode ? 'Ocurrio un error al actualizar el usuario.' : 'Ocurrio un error al crear el usuario.'), 'error');
        } finally {
            submitButton.disabled = false;
        }
    });

    if (isEditMode) {
        if (userPointsSection) {
            userPointsSection.style.display = '';
        }

        removePointsButton?.addEventListener('click', removePoints);
        deleteUserButton?.addEventListener('click', deleteUser);

        (async function init() {
            try {
                await loadUser();
            } catch (error) {
                showAlert(error.message ?? 'No fue posible cargar la informacion del usuario.', 'error');
                submitButton.disabled = true;
            }
        })();
    }
});
