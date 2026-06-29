const { contextBridge, ipcRenderer } = require('electron');

const desktopApi = {
  restartApp: () => ipcRenderer.invoke('restart-app'),
  saveWeeklyReport: (payload) => ipcRenderer.invoke('save-weekly-report', payload)
};

contextBridge.exposeInMainWorld('clawMachineDesktop', desktopApi);

window.addEventListener('DOMContentLoaded', () => {
  document.documentElement.dataset.desktopShell = 'electron';
});
