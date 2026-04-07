document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('prizeForm');
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

        const prizeCategory = categorySelect.value.trim();
        const name = nameInput.value.trim();
        const description = descriptionInput.value.trim();
        const pointsCost = Number(pointsCostInput.value);
        const cost = Number(costInput.value);

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
                body: JSON.stringify({
                    prizeCategory,
                    name,
                    description,
                    pointsCost,
                    cost
                })
            });

            if (!response.ok) {
                throw new Error('No fue posible crear el premio.');
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
            showAlert('Ocurrio un error al crear el premio.', 'error');
        } finally {
            submitButton.disabled = false;
        }
    });

    loadCategories();
});
