(function () {
    const rowsContainer = document.getElementById('expenseRows');
    const templateElement = document.getElementById('expenseRowTemplate');
    const formElement = document.getElementById('machineExpenseForm');

    if (!rowsContainer || !templateElement || !formElement) {
        return;
    }

    const rowTemplate = templateElement.innerHTML;

    function getRows() {
        return Array.from(rowsContainer.querySelectorAll('.machine-expense-row'));
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
        refreshRemoveButtons();
    }

    function refreshRemoveButtons() {
        const rows = getRows();

        rows.forEach((row, idx) => {
            const removeBtn = row.querySelector('.machine-expense-form__remove');
            if (!removeBtn) return;

            removeBtn.disabled = rows.length === 1;
            removeBtn.classList.toggle('is-disabled', rows.length === 1);

            const select = row.querySelector('.js-prize-select');
            const input = row.querySelector('.js-quantity-input');

            if (select) select.dataset.index = idx;
            if (input) input.dataset.index = idx;

            row.dataset.index = idx;
        });
    }

    function applyNamesOnlyToValidRows() {
        const rows = getRows();
        let validIndex = 0;

        rows.forEach((row) => {
            const select = row.querySelector('.js-prize-select');
            const input = row.querySelector('.js-quantity-input');

            if (!select || !input) return;

            if (isRowFilled(row)) {
                select.name = `items[${validIndex}].prizeId`;
                input.name = `items[${validIndex}].quantity`;
                validIndex++;
            } else {
                select.removeAttribute('name');
                input.removeAttribute('name');
            }
        });

        return validIndex;
    }

    function ensureTrailingEmptyRow() {
        const rows = getRows();
        const lastRow = rows[rows.length - 1];

        if (lastRow && isRowFilled(lastRow)) {
            addRow();
        }
    }

    rowsContainer.addEventListener('input', function (event) {
        const row = event.target.closest('.machine-expense-row');
        if (!row) return;
        ensureTrailingEmptyRow();
    });

    rowsContainer.addEventListener('change', function (event) {
        const row = event.target.closest('.machine-expense-row');
        if (!row) return;
        ensureTrailingEmptyRow();
    });

    rowsContainer.addEventListener('click', function (event) {
        const removeBtn = event.target.closest('.machine-expense-form__remove');
        if (!removeBtn || removeBtn.disabled) return;

        const row = removeBtn.closest('.machine-expense-row');
        if (!row) return;

        row.remove();
        refreshRemoveButtons();

        const rows = getRows();
        const hasEmptyRow = rows.some(isRowEmpty);

        if (!hasEmptyRow) {
            addRow();
        }
    });

    formElement.addEventListener('submit', function (event) {
        const rows = getRows();
        let hasPartialRow = false;

        rows.forEach((row) => {
            const select = row.querySelector('.js-prize-select');
            const input = row.querySelector('.js-quantity-input');

            if (!select || !input) return;

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
            event.preventDefault();
            return;
        }

        const validRows = applyNamesOnlyToValidRows();

        if (validRows === 0) {
            event.preventDefault();
            alert('Debes registrar al menos un premio con cantidad válida.');
            return;
        }
    });

    refreshRemoveButtons();
})();