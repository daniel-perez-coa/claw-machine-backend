(function () {
    const rowsContainer = document.getElementById('expenseRows');
    const templateElement = document.getElementById('expenseRowTemplate');
    const formElement = document.getElementById('machineExpenseForm');
    const alertContainer = document.getElementById('machineExpenseAlertContainer');
    const submitButton = document.getElementById('submitMachineExpenseBtn');

    if (!rowsContainer || !templateElement || !formElement || !alertContainer || !submitButton) {
        return;
    }

    const rowTemplate = templateElement.innerHTML;
    let prizes = [];

    function showAlert(message, type) {
        alertContainer.innerHTML = `
            <div class="alert alert-${type === 'error' ? 'danger' : 'success'} machine-expense-feedback">
                ${message}
            </div>
        `;
    }

    function redirectToDashboardWithDelay() {
        window.setTimeout(() => {
            window.location.href = '/dashboard';
        }, 1800);
    }

    function getRows() {
        return Array.from(rowsContainer.querySelectorAll('.machine-expense-row'));
    }

    function getPrizeOptionsMarkup() {
        return `
            <option value="">Selecciona un premio</option>
            ${prizes.map((prize) => `
                <option value="${prize.id}">${prize.prizeCategory ?? ''} - ${prize.name ?? ''}</option>
            `).join('')}
        `;
    }

    function hydratePrizeSelects() {
        rowsContainer.querySelectorAll('.js-prize-select').forEach((select) => {
            const currentValue = select.value;
            select.innerHTML = getPrizeOptionsMarkup();

            if (currentValue) {
                select.value = currentValue;
            }
        });
    }

    function isRowFilled(row) {
        const prize = row.querySelector('.js-prize-select')?.value?.trim();
        const quantity = row.querySelector('.js-quantity-input')?.value?.trim();
        return prize !== '' && quantity !== '' && Number(quantity) > 0;
    }

    function isRowEmpty(row) {
        const prize = row.querySelector('.js-prize-select')?.value?.trim();
        const quantity = row.querySelector('.js-quantity-input')?.value?.trim();
        return prize === '' && quantity === '';
    }

    function isRowPartial(row) {
        const prize = row.querySelector('.js-prize-select')?.value?.trim();
        const quantity = row.querySelector('.js-quantity-input')?.value?.trim();

        return (prize === '' && quantity !== '') ||
            (prize !== '' && (quantity === '' || Number(quantity) <= 0));
    }

    function addRow() {
        const index = getRows().length;
        const html = rowTemplate.replaceAll('__INDEX__', index);
        rowsContainer.insertAdjacentHTML('beforeend', html);
        hydratePrizeSelects();
        refreshRemoveButtons();
    }

    function refreshRemoveButtons() {
        const rows = getRows();

        rows.forEach((row, idx) => {
            const removeBtn = row.querySelector('.machine-expense-form__remove');
            if (!removeBtn) {
                return;
            }

            removeBtn.disabled = rows.length === 1;
            removeBtn.classList.toggle('is-disabled', rows.length === 1);
            row.dataset.index = idx;
        });
    }

    function getPayloadItems() {
        return getRows()
            .filter(isRowFilled)
            .map((row) => ({
                prizeId: Number(row.querySelector('.js-prize-select')?.value),
                quantity: Number(row.querySelector('.js-quantity-input')?.value)
            }));
    }

    function ensureTrailingEmptyRow() {
        const rows = getRows();
        const lastRow = rows[rows.length - 1];

        if (lastRow && isRowFilled(lastRow)) {
            addRow();
        }
    }

    function resetFormRows() {
        rowsContainer.innerHTML = `
            <div class="machine-expense-row" data-index="0">
                <div class="machine-expense-form__field machine-expense-form__field--prize">
                    <select class="machine-expense-form__select js-prize-select">
                        <option value="">Selecciona un premio</option>
                    </select>
                </div>

                <div class="machine-expense-form__field machine-expense-form__field--quantity">
                    <input type="number"
                           min="1"
                           step="1"
                           class="machine-expense-form__input js-quantity-input"
                           placeholder="Cantidad">
                </div>

                <div class="machine-expense-form__field machine-expense-form__field--actions">
                    <button type="button"
                            class="machine-expense-form__remove"
                            disabled>
                        Quitar
                    </button>
                </div>
            </div>
        `;

        hydratePrizeSelects();
        refreshRemoveButtons();
    }

    async function printQuickRedemptionTickets(expenseIds) {
        if (!Array.isArray(expenseIds) || expenseIds.length === 0 || !window.appReportPrinter) {
            return;
        }

        const params = new URLSearchParams();
        expenseIds.forEach((expenseId) => params.append('expenseIds', String(expenseId)));
        await window.appReportPrinter.printThermalTicketFromUrl(`/api/reports/tickets/quick-redemption/thermal-print?${params.toString()}`);
    }

    async function loadPrizes() {
        try {
            const response = await fetch('/api/prizes/active');

            if (!response.ok) {
                throw new Error('No se pudieron cargar los premios.');
            }

            prizes = await response.json();
            hydratePrizeSelects();
        } catch (error) {
            showAlert('No fue posible cargar los premios activos.', 'error');
        }
    }

    rowsContainer.addEventListener('input', function () {
        ensureTrailingEmptyRow();
    });

    rowsContainer.addEventListener('change', function () {
        ensureTrailingEmptyRow();
    });

    rowsContainer.addEventListener('click', function (event) {
        const removeBtn = event.target.closest('.machine-expense-form__remove');
        if (!removeBtn || removeBtn.disabled) {
            return;
        }

        const row = removeBtn.closest('.machine-expense-row');
        if (!row) {
            return;
        }

        row.remove();
        refreshRemoveButtons();

        const rows = getRows();
        const hasEmptyRow = rows.some(isRowEmpty);

        if (!hasEmptyRow) {
            addRow();
        }
    });

    formElement.addEventListener('submit', async function (event) {
        event.preventDefault();

        let hasPartialRow = false;

        getRows().forEach((row) => {
            const select = row.querySelector('.js-prize-select');
            const input = row.querySelector('.js-quantity-input');

            if (!select || !input) {
                return;
            }

            select.required = false;
            input.required = false;

            if (isRowPartial(row)) {
                hasPartialRow = true;

                if (select.value === '') {
                    select.required = true;
                }

                if (input.value === '' || Number(input.value) <= 0) {
                    input.required = true;
                }
            }
        });

        if (hasPartialRow) {
            return;
        }

        const items = getPayloadItems();

        if (items.length === 0) {
            showAlert('Debes registrar al menos un premio con cantidad valida.', 'error');
            return;
        }

        submitButton.disabled = true;
        try {
            const response = await fetch('/api/machine-expense-records', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ items })
            });

            if (!response.ok) {
                throw new Error('No se pudo registrar el canje.');
            }

            const result = await response.json();
            showAlert('Canje registrado correctamente.', 'success');

            try {
                await printQuickRedemptionTickets(result.expenseIds ?? []);
            } catch (printError) {
                showAlert('El canje se guardo, pero no fue posible imprimir el ticket termico.', 'error');
            }

            resetFormRows();
            redirectToDashboardWithDelay();
        } catch (error) {
            showAlert('Ocurrio un error al registrar el canje.', 'error');
        } finally {
            submitButton.disabled = false;
        }
    });

    refreshRemoveButtons();
    loadPrizes();
})();
