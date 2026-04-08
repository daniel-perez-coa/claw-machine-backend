document.addEventListener('DOMContentLoaded', () => {
    const downloadButton = document.getElementById('downloadDatabaseBackupBtn');
    const importFileInput = document.getElementById('importDatabaseBackupFile');
    const deleteDatabaseButton = document.getElementById('deleteDatabaseBtn');
    const statusElement = document.getElementById('reportsStatus');
    const destructiveDeleteMessageHtml = 'BORRAR LA BASE DE DATOS <span class="app-confirm-modal__text-accent--danger">ELIMINAR&Aacute;</span> TODA LA INFORMACION HASTA EL MOMENTO, <span class="app-confirm-modal__text-accent--warning">SI NO ESTA SEGURO DE REALIZAR ESTA ACCION CANCELE O PREVIAMENTE PREPARE UNA COPIA DE SEGURIDAD.</span>';

    function setStatus(message, type = '') {
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
        setStatus('Generando respaldo de la base de datos...');

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

            setStatus('Respaldo descargado correctamente.', 'success');
        } catch (error) {
            setStatus(error.message ?? 'No fue posible exportar la base de datos.', 'error');
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
        setStatus('Importando base de datos...');

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

            setStatus('Base de datos importada correctamente. Recargue la pantalla si es necesario.', 'success');
            importFileInput.value = '';
        } catch (error) {
            setStatus(error.message ?? 'No fue posible importar la base de datos.', 'error');
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
            setStatus('Eliminando base de datos...');

            const response = await fetch('/api/reports/database-backup', {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'No fue posible eliminar la base de datos.');
            }

            setStatus('Base de datos reiniciada correctamente.', 'success');
        } catch (error) {
            setStatus(error.message ?? 'No fue posible eliminar la base de datos.', 'error');
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

    downloadButton?.addEventListener('click', exportDatabaseBackup);
    importFileInput?.addEventListener('change', (event) => {
        const [selectedFile] = event.target.files ?? [];
        void importDatabaseBackup(selectedFile);
    });
    deleteDatabaseButton?.addEventListener('click', () => {
        void deleteDatabase();
    });

    document.body.dataset.reportsReady = 'true';
});
