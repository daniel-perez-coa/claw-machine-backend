document.addEventListener('DOMContentLoaded', () => {
    const generateButton = document.getElementById('generateWeeklySummaryBtn');
    const statusElement = document.getElementById('weeklyReportStatus');

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
            return 'reporte-semanal.pdf';
        }

        const utf8Match = dispositionHeader.match(/filename\*=UTF-8''([^;]+)/i);
        if (utf8Match?.[1]) {
            return decodeURIComponent(utf8Match[1]);
        }

        const basicMatch = dispositionHeader.match(/filename="?([^"]+)"?/i);
        return basicMatch?.[1] ?? 'reporte-semanal.pdf';
    }

    async function downloadPdfFromUrl(url) {
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('No fue posible descargar el reporte semanal.');
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
    }

    async function generateWeeklyReport() {
        if (!generateButton) {
            return;
        }

        try {
            generateButton.disabled = true;
            setStatus('Generando reporte semanal...');
            await downloadPdfFromUrl('/api/reports/weekly-summary/current');
            setStatus('Reporte semanal descargado correctamente.', 'success');
        } catch (error) {
            setStatus(error.message ?? 'No fue posible descargar el reporte semanal.', 'error');
        } finally {
            generateButton.disabled = false;
        }
    }

    generateButton?.addEventListener('click', () => {
        void generateWeeklyReport();
    });
});
