# claw-machine-backend

Backend Spring Boot para Claw Machine Admin y shell de escritorio basada en Electron.

## Desktop Windows

La app de escritorio ya no usa `jpackage`. El flujo actual empaqueta:

- `jar` del backend Spring Boot
- runtime Java reducido con `jlink`
- shell Electron con ventana propia e instalador Windows

## Requisitos de build

- JDK 21
- Node.js 20 o superior
- npm

## Flujo de empaquetado

1. Preparar backend y runtime Java:

```powershell
.\gradlew.bat prepareElectronDist
```

Esto genera:

- `build\electron\backend\claw-machine-backend.jar`
- `build\electron\runtime\`

2. Generar instalador Windows desde Electron:

```powershell
cd .\desktop-electron
npm install
npm run dist
```

Salida esperada:

- `desktop-electron\dist\Claw Machine Admin-1.0.0.exe`

## Comportamiento

- Electron abre una ventana propia de escritorio.
- La shell lanza el `jar` localmente en `http://127.0.0.1:18080/`.
- Al cerrar la app, Electron cierra el backend.
- La base H2 se guarda en `%USERPROFILE%\.claw-machine-admin\data\`.
- Los logs del backend se guardan en `%APPDATA%\claw-machine-admin\logs\`.

## Actualizaciones

- El instalador puede reemplazarse por una version nueva sin perder la BD.
- Los datos persisten porque viven fuera de la carpeta de instalacion.

## Icono Windows

- `packaging\windows\claw-machine-admin.ico`

## Notas

- Si `gradlew.bat` no funciona, regenera el wrapper con `gradle wrapper` desde una instalacion local de Gradle.
