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
            return 'REP-reporte-semanal.pdf';
        }

        const utf8Match = dispositionHeader.match(/filename\*=UTF-8''([^;]+)/i);
        if (utf8Match?.[1]) {
            return decodeURIComponent(utf8Match[1]);
        }

        const basicMatch = dispositionHeader.match(/filename="?([^"]+)"?/i);
        return basicMatch?.[1] ?? 'REP-reporte-semanal.pdf';
    }

    function getReportMonthName(fileName) {
        const match = fileName.match(/REP-(\d{2})-(\d{2})-(\d{4})-a-/);
        const monthIndex = match?.[2] ? Number.parseInt(match[2], 10) - 1 : new Date().getMonth();
        const year = match?.[3] ? Number.parseInt(match[3], 10) : new Date().getFullYear();
        const reportDate = new Date(year, Number.isNaN(monthIndex) ? new Date().getMonth() : monthIndex, 1);
        return reportDate.toLocaleDateString('es-MX', { month: 'long' }).replace(/^\p{Ll}/u, (letter) => letter.toUpperCase());
    }

    async function downloadPdfFromUrl(url) {
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('No fue posible descargar el reporte semanal.');
        }

        const fileName = getFileNameFromHeader(response.headers.get('content-disposition'));
        const fileBlob = await response.blob();

        if (window.clawMachineDesktop?.saveWeeklyReport) {
            const content = Array.from(new Uint8Array(await fileBlob.arrayBuffer()));
            const result = await window.clawMachineDesktop.saveWeeklyReport({
                fileName,
                monthName: getReportMonthName(fileName),
                content
            });
            return result?.path ?? fileName;
        }

        const downloadUrl = window.URL.createObjectURL(fileBlob);
        const anchor = document.createElement('a');

        anchor.href = downloadUrl;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        window.URL.revokeObjectURL(downloadUrl);
        return fileName;
    }

    async function generateWeeklyReport() {
        if (!generateButton) {
            return;
        }

        try {
            generateButton.disabled = true;
            setStatus('Generando reporte semanal...');
            const savedPath = await downloadPdfFromUrl('/api/reports/weekly-summary/current');
            setStatus(`Reporte semanal guardado correctamente: ${savedPath}`, 'success');
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
