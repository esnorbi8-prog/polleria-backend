import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService, Producto } from '../../../core/services/producto.service';
import { PedidoService } from '../../../core/services/pedido.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private productoService = inject(ProductoService);
  private pedidoService = inject(PedidoService);
  private http = inject(HttpClient);

  productos = signal<Producto[]>([]);
  
  // Estadisticas
  pedidosHoy = signal<number>(0);
  ingresosHoy = signal<number>(0);
  empleadosActivos = signal<number>(0);

  // Gráfico de Barras
  ventasPorDia = signal<{fecha: string, total: number}[]>([]);
  maxVenta = signal<number>(1);

  mostrarFormulario = false;
  productoEnEdicion: Partial<Producto> | null = null;
  // Para la foto en base64
  imagenBase64 = '';

  ngOnInit() {
    this.cargarProductos();
    this.cargarEstadisticas();
  }

  cargarEstadisticas() {
    // Cargar Ventas/Pedidos
    this.pedidoService.getPedidos().subscribe({
      next: (pedidos) => {
        const hoy = new Date().toISOString().substring(0, 10);
        let delDia = pedidos.filter(p => p.fechaCreacion?.startsWith(hoy));
        if (delDia.length === 0) delDia = pedidos;

        this.pedidosHoy.set(delDia.length);
        const suma = delDia.reduce((acc, p) => acc + (p.total || 0), 0);
        this.ingresosHoy.set(suma);

        // -- Lógica para el Gráfico de Barras (Últimos 7 días) --
        const mapaVentas = new Map<string, number>();
        pedidos.forEach(p => {
          if (!p.fechaCreacion) return;
          const fecha = p.fechaCreacion.substring(0, 10);
          mapaVentas.set(fecha, (mapaVentas.get(fecha) || 0) + (p.total || 0));
        });

        const ultimos7Dias = [];
        for (let i = 6; i >= 0; i--) {
          const d = new Date();
          d.setDate(d.getDate() - i);
          const fStr = d.toISOString().substring(0, 10);
          ultimos7Dias.push({
            fecha: fStr,
            total: mapaVentas.get(fStr) || 0
          });
        }

        const maximo = Math.max(...ultimos7Dias.map(v => v.total));
        this.maxVenta.set(maximo > 0 ? maximo : 1); // Evitar división por 0
        this.ventasPorDia.set(ultimos7Dias);
      },
      error: (e) => console.error(e)
    });

    // Cargar Empleados (excluyendo CLIENTE)
    this.http.get<any[]>(environment.apiUrl + '/usuarios').subscribe({
      next: (usuarios) => {
        const staff = usuarios.filter(u => u.rol !== 'CLIENTE' && u.activo === true);
        this.empleadosActivos.set(staff.length);
      },
      error: (e) => console.error(e)
    });
  }

  cargarProductos() {
    this.productoService.getProductos().subscribe(res => this.productos.set(res));
  }

  nuevoProducto() {
    this.productoEnEdicion = { nombre: '', descripcion: '', precio: 0, stock: 0 };
    this.imagenBase64 = '';
    this.mostrarFormulario = true;
  }

  editarProducto(prod: Producto) {
    this.productoEnEdicion = { ...prod };
    this.imagenBase64 = (prod as any).imagen || '';
    this.mostrarFormulario = true;
  }

  eliminarProducto(id: number) {
    if (confirm('¿Seguro que deseas eliminar este producto?')) {
      this.productoService.eliminarProducto(id).subscribe(() => this.cargarProductos());
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagenBase64 = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  guardarProducto() {
    if (!this.productoEnEdicion) return;

    const datosGuardar = { ...this.productoEnEdicion, imagen: this.imagenBase64 };

    if (this.productoEnEdicion.id) {
      this.productoService.actualizarProducto(this.productoEnEdicion.id, datosGuardar)
        .subscribe(() => {
          this.mostrarFormulario = false;
          this.cargarProductos();
        });
    } else {
      this.productoService.crearProducto(datosGuardar)
        .subscribe(() => {
          this.mostrarFormulario = false;
          this.cargarProductos();
        });
    }
  }

  cancelarEdicion() {
    this.mostrarFormulario = false;
    this.productoEnEdicion = null;
  }
}
