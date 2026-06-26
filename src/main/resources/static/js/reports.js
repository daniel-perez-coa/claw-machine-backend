document.addEventListener('DOMContentLoaded', () => {
    const downloadButton = document.getElementById('downloadDatabaseBackupBtn');
    const importFileInput = document.getElementById('importDatabaseBackupFile');
    const deleteDatabaseButton = document.getElementById('deleteDatabaseBtn');
    const resetUserPointsButton = document.getElementById('resetUserPointsBtn');
    const systemUpdateButton = document.getElementById('systemUpdateBtn');
    const databaseStatusElement = document.getElementById('reportsStatus');
    const resetPointsStatusElement = document.getElementById('resetPointsStatus');
    const systemUpdateStatusElement = document.getElementById('systemUpdateStatus');
    const destructiveDeleteMessageHtml = 'BORRAR LA BASE DE DATOS <span class="app-confirm-modal__text-accent--danger">ELIMINAR&Aacute;</span> TODA LA INFORMACION HASTA EL MOMENTO, <span class="app-confirm-modal__text-accent--warning">SI NO ESTA SEGURO DE REALIZAR ESTA ACCION CANCELE O PREVIAMENTE PREPARE UNA COPIA DE SEGURIDAD.</span>';
    const resetUserPointsMessageHtml = '<span class="app-confirm-modal__text-accent--danger"><strong>Esta acci&oacute;n es irreversible</strong></span>, al hacer esto <span class="app-confirm-modal__text-accent--danger"><strong>regresar&aacute; los puntos de los usuarios a 0</strong></span>, consulte previamente con su administrador antes de realizarlo.';
    const systemUpdateMessageHtml = 'La app descargara cambios de <strong>develop</strong>, compilara el paquete Linux e intentara instalar el nuevo <strong>.deb</strong>. Este proceso puede tardar varios minutos.';

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
                const errorText = await response.text();
                throw new Error(errorText || 'No fue posible importar la base de datos.');
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
                const errorText = await response.text();
                throw new Error(errorText || 'No fue posible eliminar la base de datos.');
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
                const errorText = await response.text();
                throw new Error(errorText || 'No fue posible resetear los puntos de los usuarios.');
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
                title: 'Actualizar sistema',
                bodyHtml: systemUpdateMessageHtml,
                confirmText: 'Actualizar',
                cancelText: 'Cancelar',
                cancelVariant: 'secondary',
                confirmVariant: 'success'
            });

            if (!confirmed) {
                return;
            }

            systemUpdateButton.disabled = true;
            setStatus(systemUpdateStatusElement, 'Actualizando sistema... puede tardar varios minutos.');

            const response = await fetch('/api/reports/system-update', {
                method: 'POST'
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'No fue posible actualizar el sistema.');
            }

            const result = await response.json();
            setStatus(systemUpdateStatusElement, result.message ?? 'Sistema actualizado correctamente.', 'success');
        } catch (error) {
            setStatus(systemUpdateStatusElement, error.message ?? 'No fue posible actualizar el sistema.', 'error');
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
