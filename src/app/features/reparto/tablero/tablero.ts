import { Component, OnInit, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PedidoService, Pedido } from '../../../core/services/pedido.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-reparto-tablero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tablero.html',
  styleUrl: './tablero.css'
})
export class RepartoTablero implements OnInit, OnDestroy {
  private pedidoService = inject(PedidoService);
  private sanitizer = inject(DomSanitizer);
  
  pedidosListos = signal<Pedido[]>([]);
  pedidosEnCamino = signal<Pedido[]>([]);
  
  cargando = signal<boolean>(true);
  errorMsg = signal<string>('');
  
  private refreshInterval: any;

  ngOnInit() {
    this.cargarTablero();
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

  getMapaUrl(direccion: string): SafeResourceUrl {
    if (!direccion) {
      direccion = 'Ica, Peru';
    }

    let query = direccion;
    // Si la dirección viene con coordenadas exactas invisibles (Ej: "Calle Tulipanes | -14.075,-75.729")
    if (direccion.includes('|')) {
      const partes = direccion.split('|');
      query = partes[1].trim(); // Usamos las coordenadas exactas para el pin rojo
    } else {
      // Nominatim genera direcciones con muchas comas. Extraemos solo la calle y ciudad.
      const comas = direccion.split(',');
      if (comas.length >= 4) {
        query = `${comas[0].trim()}, Ica, Perú`;
      }
    }

    const url = `https://maps.google.com/maps?q=${encodeURIComponent(query)}&t=&z=17&ie=UTF8&iwloc=B&output=embed`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
