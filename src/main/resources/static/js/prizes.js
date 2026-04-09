document.addEventListener('DOMContentLoaded', () => {
    const prizesList = document.getElementById('prizesList');
    const prizesPagination = document.getElementById('prizesPagination');
    const alertContainer = document.getElementById('prizesAlertContainer');
    const pageSize = 6;
    let allPrizes = [];
    let currentPage = 1;

    const moneyFormatter = new Intl.NumberFormat('es-MX', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function formatMoney(value) {
        const parsedValue = Number(value ?? 0);
        return moneyFormatter.format(Number.isFinite(parsedValue) ? parsedValue : 0);
    }

    function showAlert(message, type = 'success') {
        alertContainer.innerHTML = `
            <div class="alert alert-${type === 'error' ? 'danger' : 'success'}">
                ${escapeHtml(message)}
            </div>
        `;

        window.setTimeout(() => {
            alertContainer.innerHTML = '';
        }, 4000);
    }

    async function loadPrizes() {
        prizesList.innerHTML = '<div class="empty-state">Cargando premios...</div>';
        if (prizesPagination) {
            prizesPagination.innerHTML = '';
        }

        try {
            const response = await fetch('/api/prizes/active');

            if (!response.ok) {
                throw new Error('No se pudieron cargar los premios.');
            }

            allPrizes = await response.json();
            currentPage = 1;
            renderPrizes();
        } catch (error) {
            prizesList.innerHTML = '<div class="empty-state">No fue posible cargar los premios.</div>';
            if (prizesPagination) {
                prizesPagination.innerHTML = '';
            }
        }
    }

    function attachDeactivateActions() {
        document.querySelectorAll('.js-deactivate-prize').forEach((button) => {
            button.addEventListener('click', async () => {
                const prizeId = button.getAttribute('data-id');

                if (!prizeId) {
                    return;
                }

                if (!window.confirm('¿Deseas desactivar este premio?')) {
                    return;
                }

                try {
                    const response = await fetch(`/api/prizes/${prizeId}/deactivate`, {
                        method: 'PATCH'
                    });

                    if (!response.ok) {
                        throw new Error('No se pudo desactivar el premio.');
                    }

                    showAlert('Premio desactivado correctamente.');
                    await loadPrizes();
                } catch (error) {
                    showAlert('Ocurrió un error al desactivar el premio.', 'error');
                }
            });
        });
    }

    function renderPagination(totalItems) {
        if (!prizesPagination) {
            return;
        }

        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

        prizesPagination.innerHTML = `
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

    function renderPrizes() {
        if (!allPrizes || allPrizes.length === 0) {
            prizesList.innerHTML = '<div class="empty-state">No hay premios registrados por el momento.</div>';
            renderPagination(0);
            return;
        }

        const totalPages = Math.max(1, Math.ceil(allPrizes.length / pageSize));
        currentPage = Math.min(Math.max(1, currentPage), totalPages);
        const startIndex = (currentPage - 1) * pageSize;
        const prizes = allPrizes.slice(startIndex, startIndex + pageSize);

        prizesList.innerHTML = prizes.map((prize) => `
            <article class="app-card prize-card">
                <div class="prize-card__content">
                    <h3 class="prize-card__title">${escapeHtml(prize.name)}</h3>

                    <p class="prize-card__detail">
                        <strong>Categoria:</strong>
                        <span>${escapeHtml(prize.prizeCategory)}</span>
                    </p>

                    <p class="prize-card__detail">
                        <strong>Codigo:</strong>
                        <span>${escapeHtml(prize.prizeCode)}</span>
                    </p>

                    ${prize.description ? `
                        <p class="prize-card__detail">
                            <strong>Descripcion:</strong>
                            <span>${escapeHtml(prize.description)}</span>
                        </p>
                    ` : ''}

                    <div class="prize-card__meta">
                        <div class="prize-card__target">
                            Costo en puntos:
                            <strong>${prize.pointsCost ?? 0}</strong>
                        </div>

                        <div class="prize-card__target">
                            Costo real:
                            <strong>$${formatMoney(prize.cost)}</strong>
                        </div>
                    </div>
                </div>

                <div class="prize-card__actions">
                    <a class="prize-card__button" href="/prizes/${prize.id}">
                        Actualizar
                    </a>

                    <button type="button"
                            class="prize-card__button prize-card__button--danger js-deactivate-prize"
                            data-id="${prize.id}">
                        Eliminar
                    </button>
                </div>
            </article>
        `).join('');

        attachDeactivateActions();
        renderPagination(allPrizes.length);
    }

    prizesPagination?.addEventListener('click', (event) => {
        const button = event.target.closest('[data-page-action]');

        if (!button || button.disabled) {
            return;
        }

        if (button.dataset.pageAction === 'previous' && currentPage > 1) {
            currentPage -= 1;
            renderPrizes();
            return;
        }

        if (button.dataset.pageAction === 'next' && currentPage < Math.ceil(allPrizes.length / pageSize)) {
            currentPage += 1;
            renderPrizes();
        }
    });

    loadPrizes();
});
