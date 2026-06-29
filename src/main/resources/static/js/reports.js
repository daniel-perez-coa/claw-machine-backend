document.addEventListener('DOMContentLoaded', () => {
    const downloadButton = document.getElementById('downloadDatabaseBackupBtn');
    const importFileInput = document.getElementById('importDatabaseBackupFile');
    const deleteDatabaseButton = document.getElementById('deleteDatabaseBtn');
    const resetUserPointsButton = document.getElementById('resetUserPointsBtn');
    const systemUpdateButton = document.getElementById('systemUpdateBtn');
    const databaseStatusElement = document.getElementById('reportsStatus');
    const resetPointsStatusElement = document.getElementById('resetPointsStatus');
    const destructiveDeleteMessageHtml = 'BORRAR LA BASE DE DATOS <span class="app-confirm-modal__text-accent--danger">ELIMINAR&Aacute;</span> TODA LA INFORMACION HASTA EL MOMENTO, <span class="app-confirm-modal__text-accent--warning">SI NO ESTA SEGURO DE REALIZAR ESTA ACCION CANCELE O PREVIAMENTE PREPARE UNA COPIA DE SEGURIDAD.</span>';
    const resetUserPointsMessageHtml = '<span class="app-confirm-modal__text-accent--danger"><strong>Esta acci&oacute;n es irreversible</strong></span>, al hacer esto <span class="app-confirm-modal__text-accent--danger"><strong>regresar&aacute; los puntos de los usuarios a 0</strong></span>, consulte previamente con su administrador antes de realizarlo.';
    const systemUpdateMessageHtml = 'Se comenzar&aacute; el proceso para buscar actualizaciones disponibles. Si existe una nueva versi&oacute;n, la aplicaci&oacute;n la preparar&aacute; e instalar&aacute; autom&aacute;ticamente.';
    const systemUpdateProgressSteps = [
        { progress: 12, message: 'Buscando actualizaciones disponibles...' },
        { progress: 32, message: 'Preparando la actualizacion...' },
        { progress: 58, message: 'Descargando e instalando componentes...' },
        { progress: 82, message: 'Aplicando los ultimos ajustes...' }
    ];
    let systemUpdateProgressTimer = null;
    let systemUpdateModalElement = null;
    let systemUpdateModalInstance = null;
    let systemRestartTimer = null;

    function setStatus(statusElement, message, type = '') {
        if (!statusElement) {
            return;
        }

        statusElement.textContent = message ?? '';
        statusElement.classList.remove('reports-status--success', 'reports-status--error');

        if (type) {
            statusElement.classList.add(`reports-status--${type}`);
        }
    }

    function getFileNameFromHeader(dispositionHeader) {
        if (!dispositionHeader) {
            return 'claw-machine-backup.sql';
        }

        const utf8Match = dispositionHeader.match(/filename\*=UTF-8''([^;]+)/i);
        if (utf8Match?.[1]) {
            return decodeURIComponent(utf8Match[1]);
        }

        const basicMatch = dispositionHeader.match(/filename="?([^"]+)"?/i);
        return basicMatch?.[1] ?? 'claw-machine-backup.sql';
    }

    async function getErrorMessage(response, fallbackMessage) {
        const contentType = response.headers.get('content-type') ?? '';

        if (contentType.includes('application/json')) {
            const payload = await response.json().catch(() => null);
            return payload?.message ?? payload?.detail ?? payload?.error ?? fallbackMessage;
        }

        const errorText = await response.text();
        try {
            const payload = JSON.parse(errorText || '{}');
            if (payload && typeof payload === 'object') {
                return payload.message ?? payload.detail ?? payload.error ?? fallbackMessage;
            }
        } catch (parseError) {
            return errorText || fallbackMessage;
        }

        return errorText || fallbackMessage;
    }

    function ensureSystemUpdateModal() {
        if (systemUpdateModalElement) {
            return;
        }

        systemUpdateModalElement = document.createElement('div');
        systemUpdateModalElement.className = 'modal fade';
        systemUpdateModalElement.id = 'systemUpdateModal';
        systemUpdateModalElement.tabIndex = -1;
        systemUpdateModalElement.setAttribute('aria-hidden', 'true');
        systemUpdateModalElement.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content reports-update-modal">
                    <div class="modal-body reports-update-modal__body">
                        <h2 class="reports-update-modal__title">Actualizando aplicacion</h2>
                        <p class="reports-update-modal__message" data-system-update-message></p>
                        <div class="reports-progress reports-progress--active" aria-hidden="false">
                            <div class="reports-progress__track">
                                <div class="reports-progress__bar" data-system-update-progress-bar></div>
                            </div>
                            <div class="reports-progress__text" data-system-update-progress-text></div>
                        </div>
                        <div class="reports-update-modal__actions">
                            <button type="button" class="reports-update-modal__button" data-system-update-close hidden>Cerrar</button>
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(systemUpdateModalElement);

        const closeButton = systemUpdateModalElement.querySelector('[data-system-update-close]');
        closeButton.addEventListener('click', () => {
            systemUpdateModalInstance?.hide();
        });

        systemUpdateModalInstance = new window.bootstrap.Modal(systemUpdateModalElement, {
            backdrop: 'static',
            keyboard: false
        });
    }

    function setSystemUpdateProgress(progress, message) {
        ensureSystemUpdateModal();

        const progressBarElement = systemUpdateModalElement.querySelector('[data-system-update-progress-bar]');
        const progressTextElement = systemUpdateModalElement.querySelector('[data-system-update-progress-text]');
        const messageElement = systemUpdateModalElement.querySelector('[data-system-update-message]');

        progressBarElement.style.width = `${Math.min(progress, 100)}%`;
        progressTextElement.textContent = message;
        messageElement.textContent = 'No cierre la aplicacion mientras se completa el proceso.';
        messageElement.classList.remove('reports-update-modal__message--success', 'reports-update-modal__message--error');
    }

    function startSystemUpdateProgress() {
        let currentStepIndex = 0;
        clearInterval(systemUpdateProgressTimer);
        showSystemUpdateModal(false);
        setSystemUpdateProgress(
            systemUpdateProgressSteps[currentStepIndex].progress,
            systemUpdateProgressSteps[currentStepIndex].message
        );

        systemUpdateProgressTimer = setInterval(() => {
            currentStepIndex = Math.min(currentStepIndex + 1, systemUpdateProgressSteps.length - 1);
            setSystemUpdateProgress(
                systemUpdateProgressSteps[currentStepIndex].progress,
                systemUpdateProgressSteps[currentStepIndex].message
            );
        }, 9000);
    }

    function finishSystemUpdateProgress(message) {
        clearInterval(systemUpdateProgressTimer);
        systemUpdateProgressTimer = null;
        setSystemUpdateProgress(100, message);
    }

    function showSystemUpdateModal(canClose) {
        ensureSystemUpdateModal();
        const closeButton = systemUpdateModalElement.querySelector('[data-system-update-close]');
        closeButton.hidden = !canClose;
        systemUpdateModalInstance.show();
    }

    function showSystemUpdateResult(message, type = 'success') {
        ensureSystemUpdateModal();
        clearInterval(systemUpdateProgressTimer);
        systemUpdateProgressTimer = null;

        const messageElement = systemUpdateModalElement.querySelector('[data-system-update-message]');
        const progressTextElement = systemUpdateModalElement.querySelector('[data-system-update-progress-text]');
        const progressBarElement = systemUpdateModalElement.querySelector('[data-system-update-progress-bar]');
        const closeButton = systemUpdateModalElement.querySelector('[data-system-update-close]');

        progressBarElement.style.width = '100%';
        progressTextElement.textContent = 'Proceso finalizado.';
        messageElement.textContent = message;
        messageElement.classList.remove('reports-update-modal__message--success', 'reports-update-modal__message--error');
        messageElement.classList.add(`reports-update-modal__message--${type}`);
        closeButton.hidden = false;
        systemUpdateModalInstance.show();
    }

    function restartDesktopApp() {
        if (window.clawMachineDesktop?.restartApp) {
            void window.clawMachineDesktop.restartApp();
            return;
        }

        showSystemUpdateResult('Actualizacion lista. Cierre y abra la aplicacion para terminar.', 'success');
    }

    async function showRestartPrompt() {
        if (!window.clawMachineDesktop?.restartApp) {
            showSystemUpdateResult('Actualizacion lista. Cierre y abra la aplicacion para terminar.', 'success');
            return;
        }

        if (!window.showAppConfirmModal) {
            restartDesktopApp();
            return;
        }

        let secondsRemaining = 5;
        clearInterval(systemRestartTimer);

        const updateCountdownMessage = () => (
            `La actualizaci&oacute;n termin&oacute; correctamente. La aplicaci&oacute;n se reiniciar&aacute; en <strong>${secondsRemaining}</strong> segundos.`
        );

        const restartPromise = window.showAppConfirmModal({
            title: 'Reiniciar aplicacion',
            bodyHtml: updateCountdownMessage(),
            confirmText: 'OK',
            confirmVariant: 'success',
            hideCancel: true,
            preventDismiss: true
        });

        const modalTextElement = document.querySelector('#appConfirmModal .app-confirm-modal__text');
        systemRestartTimer = setInterval(() => {
            secondsRemaining -= 1;

            if (modalTextElement) {
                modalTextElement.innerHTML = updateCountdownMessage();
            }

            if (secondsRemaining <= 0) {
                clearInterval(systemRestartTimer);
                systemRestartTimer = null;
                restartDesktopApp();
            }
        }, 1000);

        const confirmed = await restartPromise;
        clearInterval(systemRestartTimer);
        systemRestartTimer = null;

        if (confirmed) {
            restartDesktopApp();
        }
    }

    async function exportDatabaseBackup() {
        if (!downloadButton) {
            return;
        }

        downloadButton.disabled = true;
        setStatus(databaseStatusElement, 'Generando respaldo de la base de datos...');

        try {
            const response = await fetch('/api/reports/database-backup');

            if (!response.ok) {
                throw new Error('No fue posible exportar la base de datos.');
            }

            const fileName = getFileNameFromHeader(response.headers.get('content-disposition'));
            const fileBlob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(fileBlob);
            const anchor = document.createElement('a');

            anchor.href = downloadUrl;
            anchor.download = fileName;
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            window.URL.revokeObjectURL(downloadUrl);

            setStatus(databaseStatusElement, 'Respaldo descargado correctamente.', 'success');
        } catch (error) {
            setStatus(databaseStatusElement, error.message ?? 'No fue posible exportar la base de datos.', 'error');
        } finally {
            downloadButton.disabled = false;
        }
    }

    async function importDatabaseBackup(file) {
        if (!file || !importFileInput) {
            return;
        }

        importFileInput.disabled = true;
        if (deleteDatabaseButton) {
            deleteDatabaseButton.disabled = true;
        }
        if (downloadButton) {
            downloadButton.disabled = true;
        }
        setStatus(databaseStatusElement, 'Importando base de datos...');

        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch('/api/reports/database-backup/import', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error(await getErrorMessage(response, 'No fue posible importar la base de datos.'));
            }

            setStatus(databaseStatusElement, 'Base de datos importada correctamente. Recargue la pantalla si es necesario.', 'success');
            importFileInput.value = '';
        } catch (error) {
            setStatus(databaseStatusElement, error.message ?? 'No fue posible importar la base de datos.', 'error');
            importFileInput.value = '';
        } finally {
            importFileInput.disabled = false;
            if (deleteDatabaseButton) {
                deleteDatabaseButton.disabled = false;
            }
            if (downloadButton) {
                downloadButton.disabled = false;
            }
        }
    }

    async function deleteDatabase() {
        if (!deleteDatabaseButton) {
            return;
        }

        try {
            if (!window.showAppConfirmModal) {
                throw new Error('No fue posible cargar el modal de confirmacion.');
            }

            const confirmed = await window.showAppConfirmModal({
                title: 'Eliminar base de datos',
                bodyHtml: destructiveDeleteMessageHtml,
                confirmText: 'Eliminar',
                cancelText: 'Cancelar',
                cancelVariant: 'secondary',
                confirmVariant: 'danger'
            });

            if (!confirmed) {
                return;
            }

            deleteDatabaseButton.disabled = true;
            if (importFileInput) {
                importFileInput.disabled = true;
            }
            if (downloadButton) {
                downloadButton.disabled = true;
            }
            setStatus(databaseStatusElement, 'Eliminando base de datos...');

            const response = await fetch('/api/reports/database-backup', {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error(await getErrorMessage(response, 'No fue posible eliminar la base de datos.'));
            }

            setStatus(databaseStatusElement, 'Base de datos reiniciada correctamente.', 'success');
        } catch (error) {
            setStatus(databaseStatusElement, error.message ?? 'No fue posible eliminar la base de datos.', 'error');
        } finally {
            deleteDatabaseButton.disabled = false;
            if (importFileInput) {
                importFileInput.disabled = false;
            }
            if (downloadButton) {
                downloadButton.disabled = false;
            }
        }
    }

    async function resetUserPoints() {
        if (!resetUserPointsButton) {
            return;
        }

        try {
            if (!window.showAppConfirmModal) {
                throw new Error('No fue posible cargar el modal de confirmacion.');
            }

            const confirmed = await window.showAppConfirmModal({
                title: 'Resetear puntos',
                bodyHtml: resetUserPointsMessageHtml,
                confirmText: 'Resetear',
                cancelText: 'Cancelar',
                cancelVariant: 'secondary',
                confirmVariant: 'danger'
            });

            if (!confirmed) {
                return;
            }

            resetUserPointsButton.disabled = true;
            setStatus(resetPointsStatusElement, 'Reseteando puntos de usuarios...');

            const response = await fetch('/api/reports/reset-user-points', {
                method: 'POST'
            });

            if (!response.ok) {
                throw new Error(await getErrorMessage(response, 'No fue posible resetear los puntos de los usuarios.'));
            }

            setStatus(resetPointsStatusElement, 'Los puntos de los usuarios se reiniciaron correctamente.', 'success');
        } catch (error) {
            setStatus(resetPointsStatusElement, error.message ?? 'No fue posible resetear los puntos de los usuarios.', 'error');
        } finally {
            resetUserPointsButton.disabled = false;
        }
    }

    async function updateSystem() {
        if (!systemUpdateButton) {
            return;
        }

        try {
            if (!window.showAppConfirmModal) {
                throw new Error('No fue posible cargar el modal de confirmacion.');
            }

            const confirmed = await window.showAppConfirmModal({
                title: 'Actualizar la aplicacion',
                bodyHtml: systemUpdateMessageHtml,
                confirmText: 'Buscar actualizaciones',
                cancelText: 'Cancelar',
                cancelVariant: 'secondary',
                confirmVariant: 'success'
            });

            if (!confirmed) {
                return;
            }

            systemUpdateButton.disabled = true;
            startSystemUpdateProgress();

            const response = await fetch('/api/reports/system-update', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(await getErrorMessage(response, 'No fue posible actualizar el sistema.'));
            }

            const result = await response.json();
            finishSystemUpdateProgress('Proceso finalizado.');
            showSystemUpdateResult(result.message ?? 'Aplicacion actualizada correctamente.', 'success');

            if (result.restartRequired) {
                systemUpdateModalInstance?.hide();
                await showRestartPrompt();
            }
        } catch (error) {
            showSystemUpdateResult(error.message ?? 'No fue posible actualizar el sistema.', 'error');
        } finally {
            systemUpdateButton.disabled = false;
        }
    }

    downloadButton?.addEventListener('click', exportDatabaseBackup);
    importFileInput?.addEventListener('change', (event) => {
        const [selectedFile] = event.target.files ?? [];
        void importDatabaseBackup(selectedFile);
    });
    deleteDatabaseButton?.addEventListener('click', () => {
        void deleteDatabase();
    });
    resetUserPointsButton?.addEventListener('click', () => {
        void resetUserPoints();
    });
    systemUpdateButton?.addEventListener('click', () => {
        void updateSystem();
    });

    document.body.dataset.reportsReady = 'true';
});
