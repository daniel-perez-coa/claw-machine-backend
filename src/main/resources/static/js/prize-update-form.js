document.addEventListener('DOMContentLoaded', () => {
    const pathSegments = window.location.pathname.split('/').filter(Boolean);
    const prizeId = Number(pathSegments[pathSegments.length - 1]);

    const form = document.getElementById('prizeUpdateForm');
    const alertContainer = document.getElementById('prizeFormAlertContainer');
    const submitButton = document.getElementById('submitPrizeBtn');
    const categorySelect = document.getElementById('prizeCategory');
    const nameInput = document.getElementById('prizeName');
    const descriptionInput = document.getElementById('prizeDescription');
    const pointsCostInput = document.getElementById('prizePointsCost');
    const costInput = document.getElementById('prizeCost');

    if (!form || !alertContainer || !submitButton || !categorySelect || !nameInput ||
        !descriptionInput || !pointsCostInput || !costInput) {
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

    function getUpdatePrizeErrorMessage(response) {
        if (response.status === 409) {
            return 'Ya existe otro premio con ese nombre.';
        }

        if (response.status === 400) {
            return 'La informacion del premio no es valida.';
        }

        return 'No fue posible actualizar el premio.';
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
        const response = await fetch('/api/prize-categories');

        if (!response.ok) {
            throw new Error('No se pudieron cargar las categorias.');
        }

        const categories = await response.json();
        renderCategoryOptions(categories);
    }

    async function loadPrize() {
        const response = await fetch('/api/prizes');

        if (!response.ok) {
            throw new Error('No se pudieron cargar los premios.');
        }

        const prizes = await response.json();
        const prize = prizes.find((item) => item.id === prizeId);

        if (!prize) {
            throw new Error('No se encontro el premio solicitado.');
        }

        categorySelect.value = prize.prizeCode ?? '';
        nameInput.value = prize.name ?? '';
        descriptionInput.value = prize.description ?? '';
        pointsCostInput.value = prize.pointsCost ?? '';
        costInput.value = prize.cost ?? '';
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const prizeCode = categorySelect.value.trim();
        const name = nameInput.value.trim();
        const description = descriptionInput.value.trim();
        const pointsCost = Number(pointsCostInput.value);
        const cost = Number(costInput.value);

        if (!prizeCode || !name || !Number.isFinite(pointsCost) || pointsCost <= 0 || !Number.isFinite(cost) || cost <= 0) {
            showAlert('Debes completar todos los campos obligatorios con valores validos.', 'error');
            return;
        }

        submitButton.disabled = true;

        try {
            const response = await fetch(`/api/prizes/${prizeId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    id: prizeId,
                    prizeCode,
                    prizeCategory: prizeCode,
                    name,
                    description,
                    pointsCost,
                    cost
                })
            });

            if (!response.ok) {
                throw new Error(getUpdatePrizeErrorMessage(response));
            }

            const responseText = await response.text();
            const prize = responseText ? JSON.parse(responseText) : null;

            if (!prize) {
                showAlert('No fue posible actualizar el premio.', 'error');
                return;
            }

            showAlert('Premio actualizado correctamente.');
            window.setTimeout(() => {
                window.location.href = '/prizes';
            }, 1800);
        } catch (error) {
            showAlert(error.message || 'Ocurrio un error al actualizar el premio.', 'error');
        } finally {
            submitButton.disabled = false;
        }
    });

    (async function init() {
        try {
            await loadCategories();
            await loadPrize();
        } catch (error) {
            showAlert(error.message ?? 'No fue posible cargar la informacion del premio.', 'error');
            submitButton.disabled = true;
        }
    })();
});
