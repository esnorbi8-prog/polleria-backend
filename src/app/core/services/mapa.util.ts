/**
 * Utilidades de mapa compartidas por reparto y cliente.
 * Leaflet se carga desde el CDN en index.html, por eso se declara como global.
 */
declare let L: any;

export interface Coordenada {
  lat: number;
  lng: number;
}

/** Centro por defecto: Ica, Perú. */
export const CENTRO_ICA: Coordenada = { lat: -14.0677, lng: -75.7286 };

/**
 * La dirección del pedido puede traer las coordenadas exactas al final,
 * separadas por "|" (así la guarda el checkout del cliente):
 * "Calle Tulipanes 123 |-14.0754,-75.7296"
 */
export function coordenadasDeDireccion(direccion: string | null | undefined): Coordenada | null {
  if (!direccion || !direccion.includes('|')) return null;
  const [lat, lng] = direccion.split('|')[1].split(',').map(v => parseFloat(v.trim()));
  return Number.isFinite(lat) && Number.isFinite(lng) ? { lat, lng } : null;
}

/** Texto de la dirección sin las coordenadas. */
export function textoDireccion(direccion: string | null | undefined): string {
  return direccion ? direccion.split('|')[0].trim() : '';
}

/** Crea un mapa de OpenStreetMap en el elemento indicado. */
export function crearMapa(elemento: HTMLElement, centro: Coordenada, zoom = 15): any {
  const mapa = L.map(elemento).setView([centro.lat, centro.lng], zoom);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap'
  }).addTo(mapa);
  return mapa;
}

/** Pin del destino (casa del cliente). */
export function marcadorDestino(c: Coordenada): any {
  return L.marker([c.lat, c.lng], { title: 'Destino' }).bindPopup('Destino de entrega');
}

/** Punto verde del repartidor (no necesita imágenes). */
export function marcadorRepartidor(c: Coordenada): any {
  return L.circleMarker([c.lat, c.lng], {
    radius: 10,
    color: '#ffffff',
    weight: 3,
    fillColor: '#198754',
    fillOpacity: 1
  }).bindPopup('Repartidor');
}

/** Encuadra el mapa para que se vean todos los puntos. */
export function encuadrar(mapa: any, puntos: Coordenada[]): void {
  if (puntos.length === 0) return;
  if (puntos.length === 1) {
    mapa.setView([puntos[0].lat, puntos[0].lng], 16);
    return;
  }
  mapa.fitBounds(puntos.map(p => [p.lat, p.lng]), { padding: [30, 30], maxZoom: 17 });
}

/** Ruta en OpenStreetMap (gratis), para abrir en una pestaña nueva. */
export function urlRutaOsm(desde: Coordenada | null, hasta: Coordenada | null): string | null {
  if (!hasta) return null;
  const destino = `${hasta.lat},${hasta.lng}`;
  if (!desde) return `https://www.openstreetmap.org/?mlat=${hasta.lat}&mlon=${hasta.lng}#map=17/${destino.replace(',', '/')}`;
  return `https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=${desde.lat},${desde.lng};${destino}`;
}
