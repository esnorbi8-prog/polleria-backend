import { Component, OnInit, inject, signal, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PedidoService, Pedido } from '../../../core/services/pedido.service';

@Component({
  selector: 'app-cocina-tablero',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tablero.html',
  styleUrl: './tablero.css'
})
export class CocinaTablero implements OnInit, OnDestroy {
  private pedidoService = inject(PedidoService);
  private cdr = inject(ChangeDetectorRef);
  
  pedidosPendientes = signal<Pedido[]>([]);
  pedidosPreparacion = signal<Pedido[]>([]);
  
  cargando = signal<boolean>(true);
  errorMsg = signal<string>('');

  // Cancelación de comanda en cocina
  cancelandoId = signal<number | null>(null);
  pedidoACancelar = signal<Pedido | null>(null);
  motivoCancelacion = '';
  errorCancelacion = signal<string>('');
  
  private refreshInterval: any;

  ngOnInit() {
    this.cargarTablero();
    // Refresco automático cada 10 segundos
    this.refreshInterval = setInterval(() => {
      this.cargarTablero(false);
    }, 10000);
  }
  
  ngOnDestroy() {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  cargarTablero(mostrarLoader = true) {
    if (mostrarLoader) this.cargando.set(true);
    
    this.pedidoService.getPedidos().subscribe({
      next: (todos) => {
        try {
          if (!todos) todos = [];
          
          // Formatear fechas
          todos.forEach(p => {
             const d = p.fechaCreacion as any;
             if (Array.isArray(d)) {
               p.fechaCreacion = new Date(d[0], d[1] - 1, d[2], d[3] || 0, d[4] || 0, d[5] || 0).toISOString();
             } else if (!d) {
               p.fechaCreacion = new Date().toISOString();
             }
          });
          
          // Clasificar por estado y ordenar los más antiguos primero (FIFO)
          const pendientes = todos.filter(p => p.estado === 'PAGADO')
                                  .sort((a,b) => new Date(a.fechaCreacion).getTime() - new Date(b.fechaCreacion).getTime());
          
          const enPreparacion = todos.filter(p => p.estado === 'EN_PREPARACION')
                                     .sort((a,b) => new Date(a.fechaCreacion).getTime() - new Date(b.fechaCreacion).getTime());
          
          this.pedidosPendientes.set(pendientes);
          this.pedidosPreparacion.set(enPreparacion);
        } catch(e) {
          console.error(e);
          this.errorMsg.set('Error procesando el listado.');
        } finally {
          this.cargando.set(false);
          this.cdr.detectChanges();
        }
      },
      error: (e) => {
        console.error(e);
        if (mostrarLoader) this.errorMsg.set('No se pudieron cargar los pedidos de cocina.');
        this.cargando.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  marcarEnPreparacion(pedidoId: number) {
    this.pedidoService.cambiarEstado(pedidoId, 'EN_PREPARACION', 'El chef ha empezado a cocinar').subscribe({
      next: () => this.cargarTablero(false),
      error: () => alert('Error al actualizar a En Preparación')
    });
  }

  marcarListo(pedidoId: number) {
    this.pedidoService.cambiarEstado(pedidoId, 'LISTO', 'Pedido empacado y listo en barra').subscribe({
      next: () => this.cargarTablero(false),
      error: () => alert('Error al actualizar a Listo')
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

    const motivo = this.motivoCancelacion.trim() || 'Cancelado por cocina (incidente o falta de insumos)';

    this.pedidoService.cancelarPedido(pedido.id, motivo).subscribe({
      next: () => {
        this.cancelandoId.set(null);
        this.pedidoACancelar.set(null);
        this.motivoCancelacion = '';
        this.cargarTablero(false);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al cancelar pedido desde cocina:', err);
        const msg = err.error?.mensaje || 'No se pudo cancelar el pedido. Intenta nuevamente.';
        this.errorCancelacion.set(msg);
        this.cancelandoId.set(null);
        this.cdr.detectChanges();
      }
    });
  }
}
