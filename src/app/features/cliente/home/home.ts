import { Component, OnInit, inject, signal, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService, Producto } from '../../../core/services/producto.service';
import { ClienteService, Cliente } from '../../../core/services/cliente.service';
import { PedidoService, PedidoRequest } from '../../../core/services/pedido.service';

declare let L: any;

interface CartItem {
  producto: Producto;
  cantidad: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {
  private productoService = inject(ProductoService);
  private clienteService = inject(ClienteService);
  private pedidoService = inject(PedidoService);

  productos = signal<Producto[]>([]);
  carrito = signal<CartItem[]>([]);
  miPerfil = signal<Cliente | null>(null);

  // Formulario Checkout
  tipoEntrega: 'DELIVERY' | 'RETIRO_LOCAL' = 'DELIVERY';
  direccionEntrega = '';
  procesando = false;
  pedidoExitoso = false;
  errorMensaje = '';

  ngOnInit() {
    this.cargarProductos();
    this.cargarPerfil();
  }

  cargarProductos() {
    this.productoService.getProductos().subscribe({
      next: (data) => this.productos.set(data),
      error: (err) => console.error('Error al cargar productos:', err)
    });
  }

  cargarPerfil() {
    this.clienteService.getMiPerfil().subscribe({
      next: (perfil) => {
        this.miPerfil.set(perfil);
        if (perfil.direccion) {
          this.direccionEntrega = perfil.direccion;
        }
      },
      error: (err) => {
        console.error('Error cargando perfil del cliente:', err);
        // Si no se reinició el backend, puede dar error, manejémoslo de forma limpia
      }
    });
  }

  agregarAlCarrito(producto: Producto) {
    if (producto.stock <= 0) return;

    const wasEmpty = this.carrito().length === 0;

    this.carrito.update(items => {
      const existente = items.find(i => i.producto.id === producto.id);
      if (existente) {
        if (existente.cantidad < producto.stock) {
          existente.cantidad++;
        }
        return [...items];
      }
      return [...items, { producto, cantidad: 1 }];
    });

    if (wasEmpty) {
      setTimeout(() => {
        if (this.map) {
          this.map.remove();
          this.map = null;
        }
        this.initMap();
      }, 100);
    }
  }

  quitarDelCarrito(productoId: number) {
    this.carrito.update(items => items.filter(i => i.producto.id !== productoId));
  }

  get totalCarrito() {
    return this.carrito().reduce((total, item) => total + (item.producto.precio * item.cantidad), 0);
  }

  realizarPedido() {
    const perfil = this.miPerfil();
    if (!perfil) {
      this.errorMensaje = 'No se ha podido cargar su perfil de cliente. ¿Reinició el backend?';
      return;
    }
    if (this.carrito().length === 0) return;
    
    if (this.tipoEntrega === 'DELIVERY' && !this.direccionEntrega.trim()) {
      this.errorMensaje = 'Debe ingresar una dirección para el envío por delivery.';
      return;
    }

    this.procesando = true;
    this.errorMensaje = '';

    const request: PedidoRequest = {
      clienteId: perfil.id,
      tipoEntrega: this.tipoEntrega,
      direccionEntrega: this.tipoEntrega === 'DELIVERY' ? this.direccionEntrega : undefined,
      items: this.carrito().map(item => ({
        productoId: item.producto.id,
        cantidad: item.cantidad
      }))
    };

    this.pedidoService.crearPedido(request).subscribe({
      next: (res) => {
        this.procesando = false;
        this.pedidoExitoso = true;
        this.carrito.set([]); // Limpiar carrito
      },
      error: (err) => {
        this.procesando = false;
        this.errorMensaje = err.error?.mensaje || 'Error al procesar el pedido. Intente nuevamente.';
      }
    });
  }

  private map: any = null;
  private marker: any = null;

  initMap() {
    const mapElement = document.getElementById('checkoutMap');
    if (!mapElement) return;

    // Coordenadas base (Ej: Centro de Lima)
    const lat = -12.0464;
    const lng = -77.0428;

    this.map = L.map('checkoutMap').setView([lat, lng], 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.marker = L.marker([lat, lng], { draggable: true }).addTo(this.map);

    // Actualizar dirección al hacer clic o arrastrar
    const updateAddress = (coord: any) => {
      this.direccionEntrega = `Lat: ${coord.lat.toFixed(4)}, Lng: ${coord.lng.toFixed(4)}`;
      // Geocodificación inversa gratuita con Nominatim
      fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${coord.lat}&lon=${coord.lng}`)
        .then(res => res.json())
        .then(data => {
          if (data && data.display_name) {
            this.direccionEntrega = data.display_name;
          }
        }).catch(e => console.error("Error obteniendo dirección", e));
    };

    this.map.on('click', (e: any) => {
      this.marker.setLatLng(e.latlng);
      updateAddress(e.latlng);
    });

    this.marker.on('dragend', () => {
      updateAddress(this.marker.getLatLng());
    });

    setTimeout(() => this.map.invalidateSize(), 500);
  }

  onTipoEntregaChange() {
    if (this.tipoEntrega === 'DELIVERY') {
      setTimeout(() => {
        if (this.map) {
          this.map.invalidateSize();
        } else {
          this.initMap();
        }
      }, 100);
    }
  }
}
