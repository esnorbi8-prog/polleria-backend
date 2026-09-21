import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PedidoService, Pedido } from '../../../core/services/pedido.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './pedidos.html',
  styleUrl: './pedidos.css'
})
export class Pedidos implements OnInit {
  private pedidoService = inject(PedidoService);
  private clienteService = inject(ClienteService);

  misPedidos = signal<Pedido[]>([]);
  cargando = signal<boolean>(true);
  errorMsg = signal<string>('');
  procesandoPagoId = signal<number | null>(null);

  ngOnInit() {
    this.cargarMisPedidos();
  }

  cargarMisPedidos() {
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
}
