const { app, BrowserWindow, dialog, shell } = require('electron');
const { spawn } = require('child_process');
const fs = require('fs');
const http = require('http');
const path = require('path');

const APP_NAME = 'Claw Machine Admin';
const BACKEND_PORT = process.env.APP_PORT || '18080';
const BACKEND_URL = `http://127.0.0.1:${BACKEND_PORT}/`;
const START_TIMEOUT_MS = 120000;
const POLL_INTERVAL_MS = 1000;

app.setName(APP_NAME);
app.setPath('userData', path.join(app.getPath('appData'), 'claw-machine-admin'));

let splashWindow;
let mainWindow;
let backendProcess;
let isQuitting = false;

function getResourcePath(...segments) {
  const basePath = app.isPackaged ? process.resourcesPath : path.join(__dirname, '..', 'build', 'electron');
  return path.join(basePath, ...segments);
}

function getJavaCommand() {
  const runtimeDir = getResourcePath('runtime', 'bin');
  return path.join(runtimeDir, process.platform === 'win32' ? 'java.exe' : 'java');
}

function getJarPath() {
  return getResourcePath('backend', 'claw-machine-backend.jar');
}

function getAppIconPath() {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'app-icon.png')
    : path.join(__dirname, '..', 'packaging', 'windows', 'claw-machine-admin.png');
}

function getLogsDir() {
  return path.join(app.getPath('userData'), 'logs');
}

function ensureLogsDir() {
  fs.mkdirSync(getLogsDir(), { recursive: true });
}

function writeLaunchUrl() {
  const appHome = path.join(app.getPath('home'), '.claw-machine-admin');
  fs.mkdirSync(appHome, { recursive: true });
  fs.writeFileSync(path.join(appHome, 'last-launch-url.txt'), BACKEND_URL, 'utf8');
}

function openSplashWindow() {
  splashWindow = new BrowserWindow({
    width: 520,
    height: 320,
    frame: false,
    resizable: false,
    movable: true,
    show: false,
    backgroundColor: '#111111',
    icon: getAppIconPath(),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js')
    }
  });

  splashWindow.loadFile(path.join(__dirname, 'splash.html'));
  splashWindow.once('ready-to-show', () => splashWindow.show());
}

function openMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 960,
    minWidth: 1100,
    minHeight: 760,
    show: false,
    backgroundColor: '#0f1720',
    icon: getAppIconPath(),
    autoHideMenuBar: true,
    title: APP_NAME,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js')
    }
  });

  mainWindow.loadURL(BACKEND_URL);
  mainWindow.once('ready-to-show', () => {
    if (splashWindow && !splashWindow.isDestroyed()) {
      splashWindow.close();
    }
    mainWindow.show();
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (!url || url === 'about:blank' || url.startsWith(BACKEND_URL)) {
      return {
        action: 'allow',
        overrideBrowserWindowOptions: {
          width: 520,
          height: 720,
          autoHideMenuBar: true,
          backgroundColor: '#111827',
          icon: getAppIconPath(),
          parent: mainWindow,
          webPreferences: {
            preload: path.join(__dirname, 'preload.js')
          }
        }
      };
    }

    shell.openExternal(url);
    return { action: 'deny' };
  });
}

function startBackend() {
  ensureLogsDir();
  writeLaunchUrl();

  const stdout = fs.openSync(path.join(getLogsDir(), 'backend.out.log'), 'a');
  const stderr = fs.openSync(path.join(getLogsDir(), 'backend.err.log'), 'a');
  const javaCommand = getJavaCommand();
  const jarPath = getJarPath();

  if (!fs.existsSync(javaCommand)) {
    throw new Error(`No se encontro el runtime Java empaquetado: ${javaCommand}`);
  }

  if (!fs.existsSync(jarPath)) {
    throw new Error(`No se encontro el jar del backend: ${jarPath}`);
  }

  backendProcess = spawn(
    javaCommand,
    ['-Dfile.encoding=UTF-8', '-jar', jarPath],
    {
      cwd: path.dirname(jarPath),
      windowsHide: true,
      env: {
        ...process.env,
        APP_PORT: String(BACKEND_PORT)
      },
      stdio: ['ignore', stdout, stderr]
    }
  );

  backendProcess.on('exit', (code) => {
    if (isQuitting) {
      return;
    }

    const message = code === 0
      ? 'El backend se cerro antes de que la ventana pudiera abrirse.'
      : `El backend termino con codigo ${code}. Revisa los logs en:\n${getLogsDir()}`;

    dialog.showErrorBox(APP_NAME, message);
    app.quit();
  });
}

function waitForBackend(url, timeoutMs) {
  const startedAt = Date.now();

  return new Promise((resolve, reject) => {
    const attempt = () => {
      const request = http.get(url, (response) => {
        response.resume();
        resolve();
      });

      request.on('error', () => {
        if (Date.now() - startedAt >= timeoutMs) {
          reject(new Error(`Timeout esperando al backend en ${url}`));
          return;
        }

        setTimeout(attempt, POLL_INTERVAL_MS);
      });

      request.setTimeout(5000, () => {
        request.destroy();
      });
    };

    attempt();
  });
}

async function bootstrap() {
  openSplashWindow();
  startBackend();
  await waitForBackend(BACKEND_URL, START_TIMEOUT_MS);
  openMainWindow();
}

function shutdownBackend() {
  if (!backendProcess || backendProcess.killed) {
    return;
  }

  try {
    backendProcess.kill();
  } catch (error) {
    console.error('No fue posible cerrar el backend.', error);
  }
}

app.whenReady().then(async () => {
  try {
    await bootstrap();
  } catch (error) {
    dialog.showErrorBox(APP_NAME, `${error.message}\n\nLogs:\n${getLogsDir()}`);
    app.quit();
  }
});

app.on('before-quit', () => {
  isQuitting = true;
  shutdownBackend();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0 && !isQuitting) {
    openMainWindow();
  }
});
