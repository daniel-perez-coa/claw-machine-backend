# claw-machine-backend

Backend Spring Boot para Máquina de garra admin y shell de escritorio basada en Electron.

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

- `desktop-electron\dist\Máquina de Garra Admin-1.0.0.exe`

## Firma de codigo para Windows

Para evitar bloqueos de SmartScreen / Smart App Control en equipos administrados, el instalador debe generarse firmado con un certificado de code signing confiable.

`electron-builder` ya quedo preparado para firmar automaticamente si defines estas variables de entorno:

- `CSC_LINK`
  Puede apuntar a un archivo `.pfx` local o a una URL/secret compatible con `electron-builder`.
- `CSC_KEY_PASSWORD`
  Necesaria si tu `.pfx` tiene contrasena.

Ejemplo con un certificado `.pfx` local:

```powershell
$env:CSC_LINK="C:\certs\claw-machine-admin.pfx"
$env:CSC_KEY_PASSWORD="tu-password"

cd .\desktop-electron
npm run dist:signed
```

Notas:

- Si ejecutas `npm run dist` sin variables de firma, el build intentara empaquetar sin certificado.
- `npm run dist` usa una configuracion separada no firmada para evitar errores de empaquetado en Windows cuando no existe certificado.
- Si necesitas distribucion para terceros, usa `npm run dist:signed`.
- El archivo `desktop-electron\electron-builder.yml` ya no bloquea el firmado del ejecutable.

## Comportamiento

- Electron abre una ventana propia de escritorio.
- La shell lanza el `jar` localmente en `http://127.0.0.1:18080/`.
- Al cerrar la app, Electron cierra el backend.
- La base H2 se guarda en `%USERPROFILE%\.claw-machine-admin\data\`.
- Los logs del backend se guardan en `%APPDATA%\claw-machine-admin\logs\`.

## Desktop Linux

El empaquetado Linux genera:

- `AppImage` portable
- paquete `.deb` instalable, con acceso desde el menu de aplicaciones del escritorio

Importante: el runtime Java generado con `jlink` depende del sistema operativo. Para crear una app Linux funcional, ejecuta este flujo desde Linux, WSL con entorno compatible para empaquetado, una VM Linux o un runner Linux de CI.

```bash
cd desktop-electron
npm install
npm run dist:linux
```

Salida esperada:

- `desktop-electron/dist/Maquina de garra-1.0.0-x86_64.AppImage`
- `desktop-electron/dist/maquina-de-garra_1.0.0_amd64.deb`

Para iniciar desde el escritorio Linux, instala el `.deb`; Electron Builder crea la entrada de aplicacion usando la configuracion de `desktop-electron/electron-builder.linux.yml`.

## Actualizaciones

- El instalador puede reemplazarse por una version nueva sin perder la BD.
- Los datos persisten porque viven fuera de la carpeta de instalacion.

## Icono Windows

- `packaging\windows\claw-machine-admin.ico`

## Notas

- Si `gradlew.bat` no funciona, regenera el wrapper con `gradle wrapper` desde una instalacion local de Gradle.
