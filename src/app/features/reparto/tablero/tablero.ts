import { Component, OnInit, inject, signal, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PedidoService, Pedido } from '../../../core/services/pedido.service';
import { UbicacionService } from '../../../core/services/ubicacion.service';
import {
  Coordenada, CENTRO_ICA, coordenadasDeDireccion, textoDireccion,
  crearMapa, marcadorDestino, marcadorRepartidor, encuadrar, urlRutaOsm
} from '../../../core/services/mapa.util';

type EstadoGps = 'inactivo' | 'buscando' | 'activo' | 'error';

interface MapaPedido {
  mapa: any;
  repartidor: any | null;
  destino: Coordenada | null;
  encuadrado: boolean;
}

/** Cada cuánto se manda la posición al backend (ms). */
const INTERVALO_ENVIO_MS = 5000;

@Component({
  selector: 'app-reparto-tablero',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tablero.html',
  styleUrl: './tablero.css'
})
export class RepartoTablero implements OnInit, OnDestroy {
  private pedidoService = inject(PedidoService);
  private ubicacionService = inject(UbicacionService);
  private cdr = inject(ChangeDetectorRef);

  // Cancelación de entrega en reparto
  cancelandoId = signal<number | null>(null);
  pedidoACancelar = signal<Pedido | null>(null);
  motivoCancelacion = '';
  errorCancelacion = signal<string>('');

  pedidosListos = signal<Pedido[]>([]);
  pedidosEnCamino = signal<Pedido[]>([]);

  cargando = signal<boolean>(true);
  errorMsg = signal<string>('');

  // --- GPS del repartidor (RF-D02) ---
  gpsEstado = signal<EstadoGps>('inactivo');
  gpsError = signal<string>('');
  precisionGps = signal<number | null>(null);
  ultimoEnvio = signal<Date | null>(null);
  ubicacionActual = signal<Coordenada | null>(null);

  private refreshInterval: any;
  private envioInterval: any;
  private gpsWatchId: number | null = null;
  private mapas = new Map<number, MapaPedido>();

  textoDireccion = textoDireccion;

  ngOnInit() {
    this.cargarTablero();
    this.refreshInterval = setInterval(() => {
      this.cargarTablero(false);
    }, 10000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
    this.detenerGps();
    this.eliminarMapas();
  }

  trackById(_: number, p: Pedido) {
    return p.id;
  }

  cargarTablero(mostrarLoader = true) {
    if (mostrarLoader) {
      // El loader quita las tarjetas del DOM, así que los mapas se vuelven a crear después.
      this.eliminarMapas();
      this.cargando.set(true);
    }

    this.pedidoService.getPedidos().subscribe({
      next: (todos) => {
        try {
          if (!todos) todos = [];

          todos.forEach(p => {
             const d = p.fechaCreacion as any;
             if (Array.isArray(d)) {
               p.fechaCreacion = new Date(d[0], d[1] - 1, d[2], d[3] || 0, d[4] || 0, d[5] || 0).toISOString();
             } else if (!d) {
               p.fechaCreacion = new Date().toISOString();
             }
          });

          // Solo filtramos pedidos de tipo DELIVERY que estén LISTOS
          const listos = todos.filter(p => p.estado === 'LISTO' && p.tipoEntrega === 'DELIVERY')
                              .sort((a,b) => new Date(a.fechaCreacion).getTime() - new Date(b.fechaCreacion).getTime());

          // Pedidos que ya están en camino
          const enCamino = todos.filter(p => p.estado === 'EN_CAMINO' && p.tipoEntrega === 'DELIVERY')
                                .sort((a,b) => new Date(a.fechaCreacion).getTime() - new Date(b.fechaCreacion).getTime());

          this.pedidosListos.set(listos);
          this.pedidosEnCamino.set(enCamino);
        } catch(e) {
          console.error(e);
          this.errorMsg.set('Error procesando el listado.');
        } finally {
          this.cargando.set(false);
          this.sincronizarGps();
          // Esperamos a que Angular pinte las tarjetas para poder montar los mapas.
          setTimeout(() => this.sincronizarMapas());
        }
      },
      error: (e) => {
        console.error(e);
        if (mostrarLoader) this.errorMsg.set('No se pudieron cargar los pedidos de reparto.');
        this.cargando.set(false);
      }
    });
  }

  iniciarReparto(pedidoId: number) {
    this.pedidoService.cambiarEstado(pedidoId, 'EN_CAMINO', 'El repartidor va en camino').subscribe({
      next: () => this.cargarTablero(false),
      error: () => alert('Error al actualizar a En Camino')
    });
  }

  marcarEntregado(pedidoId: number) {
    this.pedidoService.cambiarEstado(pedidoId, 'ENTREGADO', 'El pedido fue entregado al cliente con éxito').subscribe({
      next: () => this.cargarTablero(false),
      error: () => alert('Error al actualizar a Entregado')
    });
  }

  abrirModalCancelar(pedido: Pedido) {
    this.pedidoACancelar.set(pedido);
    this.motivoCancelacion = '';
    this.errorCancelacion.set('');
    this.cdr.detectChanges();
  }

  cerrarModalCancelar() {
    if (this.cancelandoId() !== null) return;
    this.pedidoACancelar.set(null);
    this.motivoCancelacion = '';
    this.errorCancelacion.set('');
    this.cdr.detectChanges();
  }

  confirmarCancelacion() {
    const pedido = this.pedidoACancelar();
    if (!pedido) return;

    this.cancelandoId.set(pedido.id);
    this.errorCancelacion.set('');
    this.cdr.detectChanges();

    const motivo = this.motivoCancelacion.trim() || 'Cancelado por repartidor (incidente en entrega)';

    this.pedidoService.cancelarPedido(pedido.id, motivo).subscribe({
      next: () => {
        this.cancelandoId.set(null);
        this.pedidoACancelar.set(null);
        this.motivoCancelacion = '';
        this.cargarTablero(false);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al cancelar entrega desde reparto:', err);
        const msg = err.error?.mensaje || 'No se pudo cancelar el pedido. Intenta nuevamente.';
        this.errorCancelacion.set(msg);
        this.cancelandoId.set(null);
        this.cdr.detectChanges();
      }
    });
  }

  // ===================== GPS =====================

  /** El GPS se prende solo mientras haya pedidos en camino. */
  private sincronizarGps() {
    const hayEnCamino = this.pedidosEnCamino().length > 0;
    if (hayEnCamino && this.gpsWatchId === null) this.iniciarGps();
    if (!hayEnCamino && this.gpsWatchId !== null) this.detenerGps();
  }

  iniciarGps() {
    // Si se reintenta tras un error, evitamos dejar dos "watch" del GPS abiertos.
    if (this.gpsWatchId !== null) this.detenerGps();
    if (!('geolocation' in navigator)) {
      this.gpsEstado.set('error');
      this.gpsError.set('Este navegador no soporta GPS.');
      return;
    }
    this.gpsEstado.set('buscando');
    this.gpsError.set('');

    this.gpsWatchId = navigator.geolocation.watchPosition(
      (pos) => {
        const primeraLectura = this.ubicacionActual() === null;
        this.ubicacionActual.set({ lat: pos.coords.latitude, lng: pos.coords.longitude });
        this.precisionGps.set(Math.round(pos.coords.accuracy));
        this.gpsEstado.set('activo');
        this.gpsError.set('');
        this.actualizarMarcadores();
        if (primeraLectura) this.enviarUbicacion();
      },
      (err) => {
        this.gpsEstado.set('error');
        const mensajes: Record<number, string> = {
          1: 'Permiso de ubicación denegado. Actívalo en el navegador (ícono del candado junto a la dirección).',
          2: 'No se pudo obtener la ubicación. Revisa que el GPS del dispositivo esté encendido.',
          3: 'El GPS tardó demasiado en responder. Reintentando...'
        };
        this.gpsError.set(mensajes[err.code] || 'Error de GPS.');
      },
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 }
    );

    // watchPosition solo avisa cuando el teléfono se mueve; reenviamos la última
    // posición cada pocos segundos para que el cliente vea que la señal sigue viva.
    this.envioInterval = setInterval(() => this.enviarUbicacion(), INTERVALO_ENVIO_MS);
  }

  private detenerGps() {
    if (this.gpsWatchId !== null) {
      navigator.geolocation.clearWatch(this.gpsWatchId);
      this.gpsWatchId = null;
    }
    if (this.envioInterval) {
      clearInterval(this.envioInterval);
      this.envioInterval = null;
    }
    this.gpsEstado.set('inactivo');
  }

  /** Manda la posición actual al backend por cada pedido que va en camino. */
  private enviarUbicacion() {
    const pos = this.ubicacionActual();
    if (!pos) return;
    for (const p of this.pedidosEnCamino()) {
      this.ubicacionService.enviar(p.id, pos.lat, pos.lng, this.precisionGps()).subscribe({
        next: () => this.ultimoEnvio.set(new Date()),
        error: (e) => console.warn('No se pudo enviar la ubicación del pedido ' + p.id, e)
      });
    }
  }

  // ===================== MAPAS =====================

  /** Crea los mapas de los pedidos nuevos en camino y borra los que ya no están. */
  private sincronizarMapas() {
    const ids = new Set(this.pedidosEnCamino().map(p => p.id));

    for (const [id, entrada] of this.mapas) {
      if (!ids.has(id)) {
        entrada.mapa.remove();
        this.mapas.delete(id);
      }
    }

    for (const p of this.pedidosEnCamino()) {
      if (this.mapas.has(p.id)) continue;
      const elemento = document.getElementById('mapa-' + p.id);
      if (!elemento) continue;

      const destino = coordenadasDeDireccion(p.direccionEntrega);
      const mapa = crearMapa(elemento, destino ?? this.ubicacionActual() ?? CENTRO_ICA);
      if (destino) marcadorDestino(destino).addTo(mapa);

      this.mapas.set(p.id, { mapa, repartidor: null, destino, encuadrado: false });
    }

    this.actualizarMarcadores();
  }

  /** Mueve el punto del repartidor en todos los mapas abiertos. */
  private actualizarMarcadores() {
    const pos = this.ubicacionActual();
    if (!pos) return;

    for (const entrada of this.mapas.values()) {
      if (!entrada.repartidor) {
        entrada.repartidor = marcadorRepartidor(pos).addTo(entrada.mapa);
      } else {
        entrada.repartidor.setLatLng([pos.lat, pos.lng]);
      }
      // Encuadramos una sola vez, para no pelearnos con el repartidor si mueve el mapa.
      if (!entrada.encuadrado) {
        encuadrar(entrada.mapa, entrada.destino ? [pos, entrada.destino] : [pos]);
        entrada.encuadrado = true;
      }
    }
  }

  private eliminarMapas() {
    for (const entrada of this.mapas.values()) entrada.mapa.remove();
    this.mapas.clear();
  }

  centrarEnMi(pedidoId: number) {
    const entrada = this.mapas.get(pedidoId);
    const pos = this.ubicacionActual();
    if (!entrada || !pos) return;
    encuadrar(entrada.mapa, entrada.destino ? [pos, entrada.destino] : [pos]);
  }

  urlRuta(p: Pedido): string | null {
    return urlRutaOsm(this.ubicacionActual(), coordenadasDeDireccion(p.direccionEntrega));
  }

  textoGps(): string {
    switch (this.gpsEstado()) {
      case 'buscando': return 'Buscando señal GPS...';
      case 'activo': {
        const precision = this.precisionGps();
        const envio = this.ultimoEnvio();
        const hora = envio ? envio.toLocaleTimeString() : '—';
        return `GPS activo (±${precision ?? '?'} m) · enviado ${hora}`;
      }
      case 'error': return 'GPS sin señal';
      default: return 'GPS apagado';
    }
  }

  claseGps(): string {
    switch (this.gpsEstado()) {
      case 'activo': return 'text-success fw-bold';
      case 'buscando': return 'text-warning fw-bold';
      case 'error': return 'text-danger fw-bold';
      default: return 'text-muted';
    }
  }
}
