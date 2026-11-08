# Servicio de WhatsApp (vinculado por QR)

Pequeño servicio Node.js que se vincula a un WhatsApp real escaneando un QR
(igual que WhatsApp Web) y envía los avisos de pedidos que le pide el backend.
Usa la librería de código abierto [whatsapp-web.js](https://docs.wwebjs.dev/).

> ⚠️ Integración **no oficial**: usa un **chip secundario**, no tu número personal.
> WhatsApp puede bloquear números que automatizan mensajes. En producción se
> usaría la API oficial (Meta), que el backend ya soporta (`WHATSAPP_PROVEEDOR=meta`).

## Primera vez
```powershell
cd C:\proyectos\polleria-backend\whatsapp-service
npm install        # descarga también un Chrome interno (~150 MB)
npm start
```
Escanea el QR (en la terminal o en el panel **Bot WhatsApp** del sistema) desde el
celular de la pollería: **WhatsApp → Dispositivos vinculados → Vincular un dispositivo**.

La sesión se guarda en `.wwebjs_auth/`: las siguientes veces solo es `npm start`.

## Backend
```powershell
setx WHATSAPP_PROVEEDOR "webqr"
```
(y reiniciar VS Code). Opcionales: `WHATSAPP_SERVICE_URL` (por defecto `http://localhost:3001`)
y `WHATSAPP_SERVICE_KEY` (por defecto `polleria-dev-key`; si la cambias, ponla igual en ambos lados).

## Problemas comunes
- **"No se pudo iniciar WhatsApp"**: revisa tu internet. Si el Chrome interno no se descargó,
  usa el tuyo: `setx CHROME_PATH "C:\Program Files\Google\Chrome\Application\chrome.exe"`.
- **Pide QR de nuevo / "auth failure"**: borra la carpeta `.wwebjs_auth` y vuelve a escanear.
- **Dejó de funcionar de un día para otro**: WhatsApp actualizó su web → `npm update` y reinicia.
- **Cambiar de número**: botón "Desvincular" en el panel Bot WhatsApp (o borrar `.wwebjs_auth`).
