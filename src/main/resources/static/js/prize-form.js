document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('prizeForm');
    const alertContainer = document.getElementById('prizeFormAlertContainer');
    const submitButton = document.getElementById('submitPrizeBtn');
    const categorySelect = document.getElementById('prizeCategory');
    const nameInput = document.getElementById('prizeName');
    const descriptionInput = document.getElementById('prizeDescription');
    const pointsCostInput = document.getElementById('prizePointsCost');
    const costInput = document.getElementById('prizeCost');
    const inactivePrizesList = document.getElementById('inactivePrizesList');

    if (!form || !alertContainer || !submitButton || !categorySelect || !nameInput ||
        !descriptionInput || !pointsCostInput || !costInput || !inactivePrizesList) {
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

    function getCreatePrizeErrorMessage(response) {
        if (response.status === 409) {
            return 'Ya existe un premio con ese nombre.';
        }

        if (response.status === 400) {
            return 'La informacion del premio no es valida.';
        }

        return 'No fue posible crear el premio.';
    }

    function buildPrizePayload() {
        return {
            prizeCategory: categorySelect.value.trim(),
            name: nameInput.value.trim(),
            description: descriptionInput.value.trim(),
            pointsCost: Number(pointsCostInput.value),
            cost: Number(costInput.value)
        };
    }

    async function findPrizeByName(name) {
        const response = await fetch(`/api/prizes/by-name?name=${encodeURIComponent(name)}`);

        if (!response.ok) {
            throw new Error('No fue posible verificar el premio existente.');
        }

        return response.json();
    }

    async function reactivatePrize(prizeId, payload) {
        const response = await fetch(`/api/prizes/${prizeId}/reactivate`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error('No fue posible reactivar el premio.');
        }

        return response.json();
    }

    function formatMoney(value) {
        const amount = Number(value ?? 0);
        return new Intl.NumberFormat('es-MX', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(Number.isFinite(amount) ? amount : 0);
    }

    function renderInactivePrizes(prizes) {
        if (!prizes || prizes.length === 0) {
            inactivePrizesList.innerHTML = '<div class="empty-state">No hay premios desactivados.</div>';
            return;
        }

        inactivePrizesList.innerHTML = prizes.map((prize) => `
            <article class="prize-inactive-item">
                <div>
                    <h4 class="prize-inactive-item__title">${escapeHtml(prize.name)}</h4>
                </div>

                <p class="prize-inactive-item__detail">
                    <strong>Categoria:</strong>
                    <span>${escapeHtml(prize.prizeCategory)}</span>
                </p>

                <p class="prize-inactive-item__detail">
                    <strong>Puntos:</strong>
                    <span>${prize.pointsCost ?? 0}</span>
                </p>

                <p class="prize-inactive-item__detail">
                    <strong>Costo real:</strong>
                    <span>$${formatMoney(prize.cost)}</span>
                </p>

                <button type="button"
                        class="prize-form__button prize-form__button--primary prize-inactive-item__action js-reactivate-prize"
                        data-id="${prize.id}">
                    Reactivar
                </button>
            </article>
        `).join('');
    }

    async function loadInactivePrizes() {
        inactivePrizesList.innerHTML = '<div class="empty-state">Cargando premios desactivados...</div>';

        try {
            const response = await fetch('/api/prizes/inactive');

            if (!response.ok) {
                throw new Error('No fue posible cargar los premios desactivados.');
            }

            const prizes = await response.json();
            renderInactivePrizes(prizes);
            attachReactivateActions();
        } catch (error) {
            inactivePrizesList.innerHTML = '<div class="empty-state">No fue posible cargar los premios desactivados.</div>';
        }
    }

    function attachReactivateActions() {
        inactivePrizesList.querySelectorAll('.js-reactivate-prize').forEach((button) => {
            button.addEventListener('click', async () => {
                const prizeId = Number(button.getAttribute('data-id'));

                if (!Number.isFinite(prizeId) || prizeId <= 0) {
                    return;
                }

                const confirmed = await window.showAppConfirmModal?.({
                    title: 'Reactivar premio',
                    body: 'Ya existe un premio desactivado, ¿desea reactivarlo?',
                    confirmText: 'Reactivar',
                    cancelText: 'Cancelar',
                    confirmVariant: 'success'
                });

                if (!confirmed) {
                    return;
                }

                button.disabled = true;

                try {
                    await reactivatePrize(prizeId);
                    showAlert('Premio reactivado correctamente.');
                    window.setTimeout(() => {
                        window.location.href = '/prizes';
                    }, 900);
                } catch (error) {
                    showAlert(error.message || 'No fue posible reactivar el premio.', 'error');
                    button.disabled = false;
                }
            });
        });
    }

    function renderCategoryOptions(categories) {
        categorySelect.innerHTML = `
            <option value="">Selecciona una categoria</option>
            ${categories.map((category) => `
                <option value="${escapeHtml(category.code)}">${escapeHtml(category.name)}</option>
            `).join('')}
        `;
    }

    async function loadCategories() {
        try {
            const response = await fetch('/api/prize-categories');

            if (!response.ok) {
                throw new Error('No se pudieron cargar las categorias.');
            }

            const categories = await response.json();
            renderCategoryOptions(categories);
        } catch (error) {
            showAlert('No fue posible cargar las categorias.', 'error');
        }
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const payload = buildPrizePayload();
        const { prizeCategory, name, pointsCost, cost } = payload;

        if (!prizeCategory || !name || !Number.isFinite(pointsCost) || pointsCost <= 0 || !Number.isFinite(cost) || cost <= 0) {
            showAlert('Debes completar todos los campos obligatorios con valores validos.', 'error');
            return;
        }

        submitButton.disabled = true;

        try {
            const response = await fetch('/api/prizes', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                if (response.status === 409) {
                    const existingPrize = await findPrizeByName(name);

                    if (existingPrize && existingPrize.isActive === false) {
                        const confirmed = await window.showAppConfirmModal?.({
                            title: 'Reactivar premio',
                            body: 'Ya existe un premio con ese nombre, ¿desea reactivarlo?',
                            confirmText: 'Reactivar',
                            cancelText: 'Cancelar',
                            confirmVariant: 'success'
                        });

                        if (!confirmed) {
                            throw new Error('Ya existe un premio con ese nombre.');
                        }

                        await reactivatePrize(existingPrize.id, payload);
                        showAlert('Premio reactivado correctamente.');
                        window.setTimeout(() => {
                            window.location.href = '/prizes';
                        }, 1800);
                        return;
                    }
                }

                throw new Error(getCreatePrizeErrorMessage(response));
            }

            const responseText = await response.text();
            const prize = responseText ? JSON.parse(responseText) : null;

            if (!prize) {
                showAlert('No fue posible crear el premio.', 'error');
                return;
            }

            showAlert('Premio creado correctamente.');
            form.reset();
            window.setTimeout(() => {
                window.location.href = '/prizes';
            }, 1800);
        } catch (error) {
            showAlert(error.message || 'Ocurrio un error al crear el premio.', 'error');
        } finally {
            submitButton.disabled = false;
        }
    });

    loadCategories();
    loadInactivePrizes();
});
