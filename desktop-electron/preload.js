const { contextBridge, ipcRenderer } = require('electron');

const desktopApi = {
  restartApp: () => ipcRenderer.invoke('restart-app')
};

contextBridge.exposeInMainWorld('clawMachineDesktop', desktopApi);

window.addEventListener('DOMContentLoaded', () => {
  document.documentElement.dataset.desktopShell = 'electron';
});
