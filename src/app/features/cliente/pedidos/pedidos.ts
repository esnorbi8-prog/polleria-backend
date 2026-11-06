import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PedidoService, Pedido } from '../../../core/services/pedido.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { DatePipe } from '@angular/common';
import { UbicacionService, UbicacionRepartidor } from '../../../core/services/ubicacion.service';
import {
  Coordenada, CENTRO_ICA, coordenadasDeDireccion,
  crearMapa, marcadorDestino, marcadorRepartidor, encuadrar
} from '../../../core/services/mapa.util';

interface MapaSeguimiento {
  mapa: any;
  repartidor: any | null;
  destino: Coordenada | null;
  encuadrado: boolean;
}

/** Cada cuánto el cliente pregunta dónde está el repartidor (ms). */
const INTERVALO_SEGUIMIENTO_MS = 5000;

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './pedidos.html',
  styleUrl: './pedidos.css'
})
export class Pedidos implements OnInit, OnDestroy {
  private pedidoService = inject(PedidoService);
  private clienteService = inject(ClienteService);
  private ubicacionService = inject(UbicacionService);

  misPedidos = signal<Pedido[]>([]);
  cargando = signal<boolean>(true);
  errorMsg = signal<string>('');
  procesandoPagoId = signal<number | null>(null);

  /** Última posición del repartidor por id de pedido (solo pedidos EN_CAMINO). */
  ubicaciones = signal<Record<number, UbicacionRepartidor>>({});
  private mapas = new Map<number, MapaSeguimiento>();
  private seguimientoInterval: any;

  ngOnInit() {
    this.cargarMisPedidos();
    this.seguimientoInterval = setInterval(() => this.actualizarSeguimiento(), INTERVALO_SEGUIMIENTO_MS);
  }

  ngOnDestroy() {
    if (this.seguimientoInterval) clearInterval(this.seguimientoInterval);
    this.eliminarMapas();
  }

  cargarMisPedidos() {
    // El spinner quita las tarjetas del DOM: los mapas se vuelven a crear después.
    this.eliminarMapas();
    this.cargando.set(true);
    this.clienteService.getMiPerfil().subscribe({
      next: (perfil) => {
        this.pedidoService.getPedidos().subscribe({
          next: (todosLosPedidos) => {
            try {
              if (!todosLosPedidos || !Array.isArray(todosLosPedidos)) {
                todosLosPedidos = [];
              }
              const filtrados = todosLosPedidos.filter(p => p.cliente && p.cliente.id === perfil.id).map(p => {
                const d = p.fechaCreacion as any;
                if (Array.isArray(d)) {
                  p.fechaCreacion = new Date(d[0], d[1] - 1, d[2], d[3] || 0, d[4] || 0, d[5] || 0).toISOString();
                } else if (!d) {
                  p.fechaCreacion = new Date().toISOString();
                }
                return p;
              });
              
              filtrados.sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime());
              this.misPedidos.set(filtrados);
            } catch (err) {
              console.error('Error procesando pedidos:', err);
              this.errorMsg.set('Ocurrió un problema al procesar los datos de tus pedidos.');
            } finally {
              this.cargando.set(false);
              setTimeout(() => this.actualizarSeguimiento());
            }
          },
          error: (err) => {
            console.error('Error obteniendo pedidos:', err);
            this.errorMsg.set('No se pudieron cargar los pedidos. ¿Reinicio el backend?');
            this.cargando.set(false);
          }
        });
      },
      error: (err) => {
        console.error('Error obteniendo perfil:', err);
        this.errorMsg.set('No se pudo verificar tu identidad de cliente. ¿Reiniciaste el backend después del último cambio?');
        this.cargando.set(false);
      }
    });
  }

  pagarMercadoPago(pedidoId: number) {
    this.procesandoPagoId.set(pedidoId);
    this.pedidoService.crearPreferenciaPago(pedidoId).subscribe({
      next: (res) => {
        // Usar el link de producción (initPoint) según lo solicitado
        window.open(res.initPoint, '_blank');
        this.procesandoPagoId.set(null);
      },
      error: () => {
        alert('Error al conectar con Mercado Pago');
        this.procesandoPagoId.set(null);
      }
    });
  }

  simularPagoExitoso(pedidoId: number) {
    this.procesandoPagoId.set(pedidoId);
    this.pedidoService.confirmarPagoSimulado(pedidoId, 'demo-999').subscribe({
      next: () => {
        this.procesandoPagoId.set(null);
        this.cargarMisPedidos();
      },
      error: (err) => {
        alert('Error en demo: ' + (err.error?.mensaje || err.message));
        this.procesandoPagoId.set(null);
      }
    });
  }

  getEstadoClase(estado: string): string {
    const clases: Record<string, string> = {
      'RECIBIDO': 'bg-secondary',
      'PAGADO': 'bg-primary',
      'EN_PREPARACION': 'bg-warning text-dark',
      'LISTO': 'bg-info text-dark',
      'EN_CAMINO': 'bg-pollos-yellow text-dark',
      'ENTREGADO': 'bg-success',
      'CANCELADO': 'bg-danger'
    };
    return clases[estado] || 'bg-secondary';
  }

  getEstadoLabel(estado: string): string {
    const labels: Record<string, string> = {
      'RECIBIDO': 'Recibido (Pendiente de Pago)',
      'PAGADO': 'Pagado (En cola)',
      'EN_PREPARACION': 'En Preparación',
      'LISTO': 'Listo para entregar',
      'EN_CAMINO': 'En Camino (Reparto)',
      'ENTREGADO': 'Entregado',
      'CANCELADO': 'Cancelado'
    };
    return labels[estado] || estado;
  }

  // ===================== SEGUIMIENTO EN VIVO =====================

  private pedidosEnCamino(): Pedido[] {
    return this.misPedidos().filter(p => p.estado === 'EN_CAMINO' && p.tipoEntrega === 'DELIVERY');
  }

  /** Pregunta al backend dónde está el repartidor y mueve el punto en el mapa. */
  private actualizarSeguimiento() {
    const enCamino = this.pedidosEnCamino();
    this.sincronizarMapas(enCamino);

    for (const p of enCamino) {
      this.ubicacionService.obtener(p.id).subscribe({
        next: (u) => {
          if (!u) return; // 204: el repartidor todavía no mandó su posición
          this.ubicaciones.update(actual => ({ ...actual, [p.id]: u }));
          this.moverRepartidor(p.id, { lat: u.latitud, lng: u.longitud });
        },
        error: (e) => console.warn('No se pudo obtener la ubicación del pedido ' + p.id, e)
      });
    }
  }

  private sincronizarMapas(enCamino: Pedido[]) {
    const ids = new Set(enCamino.map(p => p.id));
    for (const [id, entrada] of this.mapas) {
      if (!ids.has(id)) {
        entrada.mapa.remove();
        this.mapas.delete(id);
      }
    }
    for (const p of enCamino) {
      if (this.mapas.has(p.id)) continue;
      const elemento = document.getElementById('seguimiento-' + p.id);
      if (!elemento) continue;
      const destino = coordenadasDeDireccion(p.direccionEntrega);
      const mapa = crearMapa(elemento, destino ?? CENTRO_ICA);
      if (destino) marcadorDestino(destino).addTo(mapa);
      this.mapas.set(p.id, { mapa, repartidor: null, destino, encuadrado: false });
    }
  }

  private moverRepartidor(pedidoId: number, pos: Coordenada) {
    const entrada = this.mapas.get(pedidoId);
    if (!entrada) return;
    if (!entrada.repartidor) {
      entrada.repartidor = marcadorRepartidor(pos).addTo(entrada.mapa);
    } else {
      entrada.repartidor.setLatLng([pos.lat, pos.lng]);
    }
    if (!entrada.encuadrado) {
      encuadrar(entrada.mapa, entrada.destino ? [pos, entrada.destino] : [pos]);
      entrada.encuadrado = true;
    }
  }

  private eliminarMapas() {
    for (const entrada of this.mapas.values()) entrada.mapa.remove();
    this.mapas.clear();
  }

  textoSeguimiento(pedidoId: number): string {
    const u = this.ubicaciones()[pedidoId];
    if (!u) return 'Esperando la señal GPS del repartidor...';
    const f = u.fechaActualizacion as any;
    const fecha = Array.isArray(f)
      ? new Date(f[0], f[1] - 1, f[2], f[3] || 0, f[4] || 0, f[5] || 0)
      : new Date(f);
    const segundos = Math.max(0, Math.round((Date.now() - fecha.getTime()) / 1000));
    return segundos < 60
      ? `Ubicación del repartidor actualizada hace ${segundos} s`
      : `Última ubicación hace ${Math.round(segundos / 60)} min (el repartidor podría estar sin señal)`;
  }
}
