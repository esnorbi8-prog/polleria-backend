/**
 * Servicio de WhatsApp de la pollería (prueba de concepto).
 *
 * Se vincula a un WhatsApp real escaneando un QR (igual que WhatsApp Web) usando
 * la librería whatsapp-web.js, y envía los mensajes que le pide el backend
 * Spring Boot (proveedor "webqr").
 *
 * IMPORTANTE: es una integración NO oficial. Usar un chip secundario, nunca un
 * número personal: WhatsApp puede bloquear números que automatizan mensajes.
 *
 * Endpoints (solo escucha en localhost y exige la cabecera X-Api-Key):
 *   GET  /estado         -> { estado, listo, numero, nombre, qr }   (qr = imagen PNG en base64 mientras espera)
 *   POST /enviar         -> { telefono: "51935274267", texto: "..." }  =>  { id }
 *   POST /cerrar-sesion  -> desvincula el número (para vincular otro)
 */
const express = require('express');
const qrcodeTerminal = require('qrcode-terminal');
const QRCode = require('qrcode');
const { Client, LocalAuth } = require('whatsapp-web.js');

const PUERTO = Number(process.env.PORT || 3001);
const API_KEY = process.env.WHATSAPP_SERVICE_KEY || 'polleria-dev-key';
// Pausa entre mensajes para no parecer spam (reduce el riesgo de bloqueo)
const PAUSA_ENTRE_MENSAJES_MS = 1500;

// ---------------------------------------------------------------------------
// Estado de la conexión con WhatsApp
// ---------------------------------------------------------------------------
const estado = {
  estado: 'iniciando', // iniciando | esperando_qr | autenticando | listo | desconectado
  qr: null,            // imagen del QR (data:image/png;base64,...) mientras espera escaneo
  numero: null,
  nombre: null,
  error: null
};

function log(...args) {
  console.log(new Date().toLocaleTimeString(), '-', ...args);
}

// ---------------------------------------------------------------------------
// Cliente de WhatsApp (whatsapp-web.js maneja un Chrome invisible con WhatsApp Web)
// ---------------------------------------------------------------------------
const whatsapp = new Client({
  // Guarda la sesión en la carpeta .wwebjs_auth: solo hay que escanear el QR una vez
  authStrategy: new LocalAuth({ clientId: 'polleria' }),
  puppeteer: {
    headless: true,
    // Opcional: usar un Chrome ya instalado (ej. si falla la descarga del navegador interno)
    executablePath: process.env.CHROME_PATH || undefined,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  }
});

whatsapp.on('qr', async (qr) => {
  estado.estado = 'esperando_qr';
  estado.qr = await QRCode.toDataURL(qr, { margin: 1, width: 300 });
  log('Escanea este QR con el WhatsApp de la pollería (Dispositivos vinculados → Vincular un dispositivo).');
  log('También puedes escanearlo desde el panel "Bot WhatsApp" del sistema.');
  qrcodeTerminal.generate(qr, { small: true });
});

whatsapp.on('authenticated', () => {
  estado.estado = 'autenticando';
  estado.qr = null;
  log('QR escaneado. Cargando WhatsApp...');
});

whatsapp.on('ready', () => {
  estado.estado = 'listo';
  estado.qr = null;
  estado.error = null;
  estado.numero = whatsapp.info?.wid?.user || null;
  estado.nombre = whatsapp.info?.pushname || null;
  log(`✅ WhatsApp listo. Enviando como +${estado.numero} (${estado.nombre || 'sin nombre'})`);
});

whatsapp.on('auth_failure', (mensaje) => {
  estado.estado = 'desconectado';
  estado.error = 'Falló la autenticación: ' + mensaje;
  log('❌', estado.error, '— borra la carpeta .wwebjs_auth y vuelve a escanear el QR.');
});

whatsapp.on('disconnected', (motivo) => {
  estado.estado = 'desconectado';
  estado.numero = null;
  estado.nombre = null;
  estado.error = 'WhatsApp se desconectó: ' + motivo;
  log('⚠️', estado.error, '— reconectando en 5 s...');
  setTimeout(reiniciarWhatsapp, 5000);
});

let iniciando = false;

async function iniciarWhatsapp() {
  if (iniciando) return;
  iniciando = true;
  try {
    estado.estado = 'iniciando';
    await whatsapp.initialize();
  } catch (e) {
    estado.estado = 'desconectado';
    estado.error = 'No se pudo iniciar WhatsApp: ' + e.message;
    log('❌', estado.error, '— reintentando en 15 s...');
    setTimeout(reiniciarWhatsapp, 15000);
  } finally {
    iniciando = false;
  }
}

/** Cierra el Chrome interno (si quedó abierto) y vuelve a iniciar WhatsApp. */
async function reiniciarWhatsapp() {
  try { await whatsapp.destroy(); } catch (e) { /* ya estaba cerrado */ }
  await iniciarWhatsapp();
}

// ---------------------------------------------------------------------------
// Cola de envíos: un mensaje a la vez, con una pausa entre cada uno
// ---------------------------------------------------------------------------
let cola = Promise.resolve();

function encolar(tarea) {
  const resultado = cola.then(tarea);
  cola = resultado
    .catch(() => {})
    .then(() => new Promise((r) => setTimeout(r, PAUSA_ENTRE_MENSAJES_MS)));
  return resultado;
}

class ErrorEnvio extends Error {
  constructor(status, mensaje) {
    super(mensaje);
    this.status = status;
  }
}

async function enviarMensaje(telefono, texto) {
  if (estado.estado !== 'listo') {
    throw new ErrorEnvio(503, 'WhatsApp no está listo (estado: ' + estado.estado + '). Escanea el QR primero.');
  }
  // Busca la cuenta de WhatsApp de ese número (devuelve null si no tiene WhatsApp)
  const cuenta = await whatsapp.getNumberId(telefono);
  if (!cuenta) {
    throw new ErrorEnvio(404, 'El número +' + telefono + ' no tiene WhatsApp.');
  }
  const mensaje = await whatsapp.sendMessage(cuenta._serialized, texto);
  return mensaje?.id?._serialized || 'sin id';
}

// ---------------------------------------------------------------------------
// API HTTP para el backend
// ---------------------------------------------------------------------------
const app = express();
app.use(express.json({ limit: '50kb' }));

// Solo el backend (que conoce la clave) puede usar el servicio
app.use((req, res, next) => {
  if (req.get('X-Api-Key') !== API_KEY) {
    return res.status(401).json({ mensaje: 'Clave del servicio inválida (X-Api-Key).' });
  }
  next();
});

app.get('/estado', (req, res) => {
  res.json({
    estado: estado.estado,
    listo: estado.estado === 'listo',
    numero: estado.numero,
    nombre: estado.nombre,
    qr: estado.qr,
    error: estado.error
  });
});

app.post('/enviar', async (req, res) => {
  const telefono = String(req.body?.telefono || '').replace(/\D/g, '');
  const texto = String(req.body?.texto || '').trim();

  if (telefono.length < 8 || !texto) {
    return res.status(400).json({ mensaje: 'Faltan "telefono" (con código de país, ej. 51935274267) o "texto".' });
  }

  try {
    const id = await encolar(() => enviarMensaje(telefono, texto));
    log(`📤 Enviado a +${telefono}`);
    res.json({ id });
  } catch (e) {
    const status = e instanceof ErrorEnvio ? e.status : 500;
    log(`❌ No se pudo enviar a +${telefono}: ${e.message}`);
    res.status(status).json({ mensaje: e.message });
  }
});

app.post('/cerrar-sesion', async (req, res) => {
  try {
    await whatsapp.logout();
    estado.estado = 'desconectado';
    estado.numero = null;
    estado.nombre = null;
    log('Sesión cerrada. Se mostrará un QR nuevo para vincular otro número.');
    setTimeout(reiniciarWhatsapp, 2000);
    res.json({ mensaje: 'Sesión cerrada. Escanea el nuevo QR para vincular otro número.' });
  } catch (e) {
    res.status(500).json({ mensaje: 'No se pudo cerrar la sesión: ' + e.message });
  }
});

// Solo escucha en la propia PC: nadie de afuera puede mandar mensajes con tu WhatsApp
app.listen(PUERTO, '127.0.0.1', () => {
  log(`Servicio de WhatsApp escuchando en http://localhost:${PUERTO}`);
  log('Iniciando WhatsApp (la primera vez puede tardar ~1 minuto)...');
  iniciarWhatsapp();
});

// Cierra Chrome correctamente con Ctrl + C
process.on('SIGINT', async () => {
  log('Cerrando servicio...');
  try { await whatsapp.destroy(); } catch (e) { /* ya estaba cerrado */ }
  process.exit(0);
});
