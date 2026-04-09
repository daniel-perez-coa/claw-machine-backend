window.appReportPrinter = (() => {
    function getPopupFeatures() {
        const popupWidth = 520;
        const popupHeight = 720;
        const dualScreenLeft = window.screenLeft ?? window.screenX ?? 0;
        const dualScreenTop = window.screenTop ?? window.screenY ?? 0;
        const viewportWidth = window.innerWidth ?? document.documentElement.clientWidth ?? screen.width;
        const viewportHeight = window.innerHeight ?? document.documentElement.clientHeight ?? screen.height;
        const left = Math.max(0, dualScreenLeft + Math.round((viewportWidth - popupWidth) / 2));
        const top = Math.max(0, dualScreenTop + Math.round((viewportHeight - popupHeight) / 2));

        return [
            'resizable=yes',
            'scrollbars=yes',
            `width=${popupWidth}`,
            `height=${popupHeight}`,
            `left=${left}`,
            `top=${top}`
        ].join(',');
    }

    function openPrintWindow(loadingMessage = 'Preparando ticket...') {
        const printWindow = window.open('', 'clawMachinePrintWindow', getPopupFeatures());
        if (!printWindow) {
            return null;
        }

        printWindow.focus();
        printWindow.document.open();
        printWindow.document.write(`
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <title>${loadingMessage}</title>
                <style>
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: grid;
                        place-items: center;
                        font-family: Arial, sans-serif;
                        background: #0b1220;
                        color: #f3f7ff;
                    }
                </style>
            </head>
            <body>${loadingMessage}</body>
            </html>
        `);
        printWindow.document.close();
        return printWindow;
    }

    async function printPdfFromUrl(url, existingWindow = null) {
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('No fue posible generar el ticket.');
        }

        const blob = await response.blob();
        printPdfBlob(blob, existingWindow);
    }

    function printPdfBlob(blob, existingWindow = null) {
        const printWindow = existingWindow ?? openPrintWindow();

        if (!printWindow) {
            throw new Error('El navegador bloqueo la ventana de impresion.');
        }

        const blobUrl = window.URL.createObjectURL(blob);

        printWindow.document.open();
        printWindow.document.write(`
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <title>Imprimir ticket</title>
                <style>
                    html, body, iframe {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        border: 0;
                        background: #111827;
                    }
                </style>
            </head>
            <body>
                <iframe id="reportFrame" src="${blobUrl}"></iframe>
                <script>
                    const frame = document.getElementById('reportFrame');
                    frame.addEventListener('load', () => {
                        setTimeout(() => {
                            window.focus();
                            window.print();
                        }, 350);
                    });

                    window.addEventListener('afterprint', () => {
                        setTimeout(() => window.close(), 150);
                    });

                    window.addEventListener('beforeunload', () => {
                        URL.revokeObjectURL('${blobUrl}');
                    });
                </script>
            </body>
            </html>
        `);
        printWindow.document.close();
    }

    return {
        openPrintWindow,
        printPdfFromUrl,
        printPdfBlob
    };
})();
