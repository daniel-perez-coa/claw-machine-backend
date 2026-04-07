document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('incomeRecordForm');
    const alertContainer = document.getElementById('incomeRecordAlertContainer');
    const submitButton = document.getElementById('submitIncomeRecordBtn');
    const amountInput = document.getElementById('amount');
    const notesInput = document.getElementById('notes');

    if (!form || !alertContainer || !submitButton || !amountInput || !notesInput) {
        return;
    }

    function showAlert(message, type = 'success') {
        alertContainer.innerHTML = `
            <div class="alert alert-${type === 'error' ? 'danger' : 'success'}">
                ${message}
            </div>
        `;
    }

    function redirectToDashboardWithDelay() {
        window.setTimeout(() => {
            window.location.href = '/dashboard';
        }, 1800);
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const amount = Number(amountInput.value);
        const notes = notesInput.value.trim();

        if (!Number.isFinite(amount) || amount <= 0) {
            showAlert('Debes capturar un monto valido.', 'error');
            return;
        }

        submitButton.disabled = true;

        try {
            const response = await fetch('/api/income-records', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    amount,
                    notes
                })
            });

            if (!response.ok) {
                throw new Error('No fue posible registrar el ingreso.');
            }

            showAlert('Ingreso registrado correctamente.');
            form.reset();
            redirectToDashboardWithDelay();
        } catch (error) {
            showAlert('Ocurrio un error al registrar el ingreso.', 'error');
        } finally {
            submitButton.disabled = false;
        }
    });
});
