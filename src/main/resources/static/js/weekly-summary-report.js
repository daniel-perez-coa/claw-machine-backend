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

    async function generateWeeklyReport() {
        if (!generateButton || !window.appReportPrinter) {
            return;
        }

        const printWindow = window.appReportPrinter.openPrintWindow('Preparando reporte semanal...');

        try {
            generateButton.disabled = true;
            setStatus('Generando reporte semanal...');
            await window.appReportPrinter.printPdfFromUrl('/api/reports/weekly-summary/current', printWindow);
            setStatus('Reporte semanal generado correctamente.', 'success');
        } catch (error) {
            printWindow?.close();
            setStatus('No fue posible generar el reporte semanal.', 'error');
        } finally {
            generateButton.disabled = false;
        }
    }

    generateButton?.addEventListener('click', () => {
        void generateWeeklyReport();
    });
});
