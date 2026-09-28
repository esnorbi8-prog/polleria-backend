import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService, Producto } from '../../../core/services/producto.service';
import { PedidoService, Pedido } from '../../../core/services/pedido.service';
import { ClienteService, Cliente } from '../../../core/services/cliente.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pos.html',
  styleUrl: './pos.css'
})
export class Pos implements OnInit {
  private productoService = inject(ProductoService);
  private pedidoService = inject(PedidoService);
  private clienteService = inject(ClienteService);
  private http = inject(HttpClient);

  productos = signal<Producto[]>([]);
  carrito = signal<{ producto: Producto, cantidad: number }[]>([]);
  miPerfil = signal<Cliente | null>(null);
  
  // Tab control
  activeTab = signal<'NUEVO' | 'ENTREGAS'>('NUEVO');
  
  // Lista de entregas locales
  pedidosLocal = signal<Pedido[]>([]);

  ngOnInit() {
    this.clienteService.getMiPerfil().subscribe({
      next: (perfil) => this.miPerfil.set(perfil),
      error: (err) => {
        console.warn("La cajera no tiene perfil de cliente. Usando Cliente Genérico.");
        // Si no tiene perfil, cargamos el primer cliente de la base de datos (Cliente Genérico / de Mostrador)
        this.http.get<Cliente[]>(environment.apiUrl + '/clientes').subscribe(clientes => {
          if (clientes && clientes.length > 0) {
            this.miPerfil.set(clientes[0]);
          }
        });
      }
    });
    
    this.productoService.getProductos().subscribe(res => {
      this.productos.set(res.filter(p => p.stock > 0));
    });
    this.cargarEntregas();
    setInterval(() => this.cargarEntregas(), 10000); // Auto-refresh 10s
  }

  cargarEntregas() {
    if (this.activeTab() !== 'ENTREGAS') return;
    this.pedidoService.getPedidos().subscribe(res => {
      const locales = res.filter(p => p.tipoEntrega === 'LOCAL' && p.estado !== 'ENTREGADO');
      this.pedidosLocal.set(locales);
    });
  }

  cambiarTab(tab: 'NUEVO' | 'ENTREGAS') {
    this.activeTab.set(tab);
    if (tab === 'ENTREGAS') this.cargarEntregas();
  }

  agregarAlCarrito(producto: Producto) {
    const current = this.carrito();
    const existe = current.find(item => item.producto.id === producto.id);
    if (existe) {
      existe.cantidad++;
      this.carrito.set([...current]);
    } else {
      this.carrito.set([...current, { producto, cantidad: 1 }]);
    }
  }

  quitarDelCarrito(productoId: number) {
    const current = this.carrito().filter(item => item.producto.id !== productoId);
    this.carrito.set(current);
  }

  get totalCarrito() {
    return this.carrito().reduce((sum, item) => sum + (item.producto.precio * item.cantidad), 0);
  }

  // Modal y Pago
  mostrarModalCobro = false;
  metodoPago = 'EFECTIVO';
  pedidoExitoso = false;
  ultimoPedidoId = 0;

  abrirModalCobro() {
    if (this.carrito().length === 0) return;
    this.metodoPago = 'EFECTIVO';
    this.pedidoExitoso = false;
    this.mostrarModalCobro = true;
  }

  cerrarModal() {
    this.mostrarModalCobro = false;
    if (this.pedidoExitoso) {
      this.carrito.set([]); // Limpiar carrito si ya se pagó
    }
  }

  confirmarPago() {
    const perfil = this.miPerfil();
    if (!perfil) {
      alert("Error: No se pudo identificar a la cajera (perfil cliente no encontrado).");
      return;
    }

    // Nota: Podríamos guardar el metodoPago si el backend lo soportara,
    // por ahora el backend asume pago completado para LOCAL.
    const payload = {
      clienteId: perfil.id,
      tipoEntrega: 'LOCAL',
      direccionEntrega: 'MESA/CAJA - ' + this.metodoPago,
      items: this.carrito().map(item => ({
        productoId: item.producto.id,
        cantidad: item.cantidad
      }))
    };

    this.pedidoService.crearPedido(payload).subscribe({
      next: (res) => {
        this.ultimoPedidoId = res.id;
        this.pedidoExitoso = true; // Cambia el modal a vista de éxito
      },
      error: () => alert('Error al crear el pedido en caja')
    });
  }

  entregarPedido(id: number) {
    this.pedidoService.cambiarEstado(id, 'ENTREGADO', 'Entregado en local por cajera').subscribe(() => {
      this.cargarEntregas();
    });
  }
}
