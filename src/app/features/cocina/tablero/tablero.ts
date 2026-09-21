import { Component, OnInit, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PedidoService, Pedido } from '../../../core/services/pedido.service';

@Component({
  selector: 'app-cocina-tablero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tablero.html',
  styleUrl: './tablero.css'
})
export class CocinaTablero implements OnInit, OnDestroy {
  private pedidoService = inject(PedidoService);
  
  pedidosPendientes = signal<Pedido[]>([]);
  pedidosPreparacion = signal<Pedido[]>([]);
  
  cargando = signal<boolean>(true);
  errorMsg = signal<string>('');
  
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
        }
      },
      error: (e) => {
        console.error(e);
        if (mostrarLoader) this.errorMsg.set('No se pudieron cargar los pedidos de cocina.');
        this.cargando.set(false);
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
}
