const requiredVars = ['CSC_LINK'];
const missing = requiredVars.filter((name) => !process.env[name] || !String(process.env[name]).trim());

if (missing.length > 0) {
  console.error('Faltan variables de entorno para generar un instalador firmado.');
  console.error(`Variables requeridas: ${missing.join(', ')}`);
  console.error('Define al menos CSC_LINK y, si tu certificado PFX usa contrasena, tambien CSC_KEY_PASSWORD.');
  process.exit(1);
}

console.log('Variables de firma detectadas. Continuando con electron-builder.');
