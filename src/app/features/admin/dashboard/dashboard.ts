import { Component, OnInit, inject, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService, Producto } from '../../../core/services/producto.service';
import { PedidoService } from '../../../core/services/pedido.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { forkJoin } from 'rxjs';

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
  private cdr = inject(ChangeDetectorRef);

  productos = signal<Producto[]>([]);
  
  // Estadisticas
  pedidosHoy = signal<number>(0);
  ingresosHoy = signal<number>(0);
  empleadosActivos = signal<number>(0);
  empleados = signal<any[]>([]);

  // Gráfico de Barras
  ventasPorDia = signal<{fecha: string, total: number}[]>([]);
  maxVenta = signal<number>(1);

  mostrarFormulario = false;
  productoEnEdicion: Partial<Producto> | null = null;
  imagenBase64 = '';
  cargandoDatos = true; // Indicador de carga añadido

  ngOnInit() {
    this.cargarProductos();
    this.cargarEstadisticas();
  }

  cargarEstadisticas() {
    this.cargandoDatos = true;

    const reqPedidos = this.pedidoService.getPedidos();
    const reqUsuarios = this.http.get<any[]>(environment.apiUrl + '/usuarios');

    forkJoin([reqPedidos, reqUsuarios]).subscribe({
      next: ([pedidos, usuarios]) => {
        // --- Procesar Pedidos ---
        const hoy = new Date().toISOString().substring(0, 10);
        let delDia = pedidos.filter(p => p.fechaCreacion?.startsWith(hoy));
        if (delDia.length === 0) delDia = pedidos;

        this.pedidosHoy.set(delDia.length);
        const suma = delDia.reduce((acc, p) => acc + (p.total || 0), 0);
        this.ingresosHoy.set(suma);

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
        this.maxVenta.set(maximo > 0 ? maximo : 1); 
        this.ventasPorDia.set(ultimos7Dias);

        // --- Procesar Usuarios ---
        const staff = usuarios.filter(u => u.rol !== 'CLIENTE');
        this.empleados.set(staff);
        this.empleadosActivos.set(staff.filter(u => u.activo === true).length);

        // Apagar loader
        this.cargandoDatos = false;
        this.cdr.detectChanges();
      },
      error: (e) => {
        console.error(e);
        this.cargandoDatos = false;
        this.cdr.detectChanges();
      }
    });
  }

  cambiarEstadoEmpleado(id: number, activoActual: boolean) {
    const nuevoEstado = !activoActual;
    const msg = nuevoEstado 
      ? 'Creemos en las segundas oportunidades. ¿Le devolvemos el acceso a este empleado para que vuelva a trabajar con nosotros?' 
      : 'Vamos a quitarle las llaves del negocio. Este empleado no podrá ingresar al sistema hasta que lo perdones. ¿Procedemos?';
    
    this.modalConfirmacion = {
      mostrar: true,
      titulo: nuevoEstado ? 'Reactivar Acceso' : 'Suspender Acceso',
      mensaje: msg,
      tipo: 'warning',
      textoBoton: nuevoEstado ? 'Sí, darle acceso' : 'Sí, suspender',
      imagenUrl: '',
      accionConfirmar: () => {
        this.cerrarModalConfirmacion();
        this.http.patch(environment.apiUrl + `/usuarios/${id}/estado?activo=${nuevoEstado}`, {}).subscribe({
          next: () => {
            this.cargarEstadisticas();
          },
          error: (e) => {
            this.modalNotificacion = { mostrar: true, titulo: 'Error', mensaje: 'No pudimos procesar el cambio en los permisos.', tipo: 'error' };
          }
        });
      }
    };
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
    this.modalConfirmacion = {
      mostrar: true,
      titulo: 'Retirar del Menú',
      mensaje: 'Este platillo desaparecerá de nuestra carta para siempre y nuestros clientes ya no podrán pedirlo. ¿Estás seguro?',
      tipo: 'danger',
      textoBoton: 'Quitar del menú',
      imagenUrl: '',
      accionConfirmar: () => {
        this.cerrarModalConfirmacion();
        this.productoService.eliminarProducto(id).subscribe(() => this.cargarProductos());
      }
    };
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

  // --- Lógica para Nuevo Empleado ---
  mostrarFormularioEmpleado = false;
  empleadoNuevo = { nombreCompleto: '', email: '', password: '', rol: 'COCINERO' };
  empleadoEnEdicion: any = null;

  // Estado del Modal de Notificación
  modalNotificacion = {
    mostrar: false,
    titulo: '',
    mensaje: '',
    tipo: 'success'
  };

  // Estado del Modal de Confirmación
  modalConfirmacion = {
    mostrar: false,
    titulo: '',
    mensaje: '',
    tipo: 'danger',
    textoBoton: 'Confirmar',
    imagenUrl: '', // <- Nueva propiedad para imagen personalizada
    accionConfirmar: () => {}
  };

  cerrarModal() {
    this.modalNotificacion.mostrar = false;
  }

  cerrarModalConfirmacion() {
    this.modalConfirmacion.mostrar = false;
  }

  nuevoEmpleado() {
    this.empleadoNuevo = { nombreCompleto: '', email: '', password: '', rol: 'COCINERO' };
    this.empleadoEnEdicion = null;
    this.mostrarFormularioEmpleado = true;
    this.mostrarFormulario = false;
  }

  editarEmpleado(emp: any) {
    this.empleadoNuevo = { 
      nombreCompleto: emp.nombreCompleto, 
      email: emp.email, 
      password: '', // opcional en edición
      rol: emp.rol 
    };
    this.empleadoEnEdicion = emp;
    this.mostrarFormularioEmpleado = true;
    this.mostrarFormulario = false;
  }

  eliminarEmpleado(id: number) {
    this.modalConfirmacion = {
      mostrar: true,
      titulo: 'Despido Definitivo',
      mensaje: 'Te vamos a despedir de la familia de pollos hermanos, te enviaremos con San Cuchito.',
      tipo: 'danger',
      textoBoton: 'Sí, enviar con Cuchito',
      imagenUrl: '/img/cuchito.jpg', // <- La imagen que subió el usuario
      accionConfirmar: () => {
        this.cerrarModalConfirmacion();
        this.http.delete(environment.apiUrl + `/usuarios/${id}`).subscribe({
          next: () => {
            this.cargarEstadisticas();
            this.modalNotificacion = { mostrar: true, titulo: '¡Amén!', mensaje: 'El empleado ya se fue con San Cuchito.', tipo: 'success' };
          },
          error: (e) => {
            this.modalNotificacion = { mostrar: true, titulo: 'Hubo un problema', mensaje: e.error?.mensaje || 'No pudimos procesar el despido.', tipo: 'error' };
          }
        });
      }
    };
  }

  cancelarEmpleado() {
    this.mostrarFormularioEmpleado = false;
    this.empleadoEnEdicion = null;
  }

  guardarEmpleado() {
    if (!this.empleadoNuevo.nombreCompleto || !this.empleadoNuevo.email || (!this.empleadoEnEdicion && !this.empleadoNuevo.password)) {
      this.modalNotificacion = {
        mostrar: true,
        titulo: 'Faltan Datos',
        mensaje: 'Por favor, complete todos los campos obligatorios del formulario.',
        tipo: 'error'
      };
      return;
    }
    
    if (this.empleadoEnEdicion) {
      // Modo Edición
      this.http.put(environment.apiUrl + `/usuarios/${this.empleadoEnEdicion.id}`, this.empleadoNuevo).subscribe({
        next: () => {
          this.mostrarFormularioEmpleado = false;
          this.empleadoEnEdicion = null;
          this.cargarEstadisticas(); 
          this.modalNotificacion = {
            mostrar: true,
            titulo: '¡Empleado Actualizado!',
            mensaje: `Los datos se han guardado con éxito.`,
            tipo: 'success'
          };
        },
        error: (e) => {
          this.modalNotificacion = { mostrar: true, titulo: 'Error', mensaje: e.error?.mensaje || 'No se pudo actualizar.', tipo: 'error' };
        }
      });
    } else {
      // Modo Creación
      this.http.post(environment.apiUrl + '/usuarios', this.empleadoNuevo).subscribe({
        next: () => {
          this.mostrarFormularioEmpleado = false;
          this.cargarEstadisticas(); 
          this.modalNotificacion = {
            mostrar: true,
            titulo: '¡Empleado Creado!',
            mensaje: `El empleado ${this.empleadoNuevo.nombreCompleto} ha sido registrado con éxito.`,
            tipo: 'success'
          };
        },
        error: (e) => {
          this.modalNotificacion = {
            mostrar: true,
            titulo: 'Error',
            mensaje: e.error?.mensaje || 'No se pudo crear el empleado. Verifica los datos.',
            tipo: 'error'
          };
        }
      });
    }
  }
}
