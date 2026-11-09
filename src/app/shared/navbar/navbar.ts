import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../core/auth/auth';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService } from '../../core/services/cliente.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {
  authService = inject(Auth);
  clienteService = inject(ClienteService);
  cdr = inject(ChangeDetectorRef);
  
  // Estado del Modal de Perfil
  mostrarModalPerfil = false;
  perfilCargando = false;
  guardandoPerfil = false;
  mensajeExito = '';
  mensajeError = '';

  datosPerfil = {
    nombre: '',
    email: '',
    telefono: '',
    direccion: '',
    password: ''
  };

  get user() {
    return this.authService.currentUser();
  }

  get rutaInicio() {
    const rol = this.user?.rol;
    if (rol === 'CLIENTE') return '/cliente/home';
    if (rol === 'COCINERO') return '/cocina';
    if (rol === 'REPARTIDOR') return '/reparto';
    if (rol === 'CAJERA') return '/caja';
    if (rol === 'ADMINISTRADOR') return '/admin';
    return '/login';
  }

  logout() {
    this.authService.logout();
  }

  abrirModalPerfil() {
    this.mensajeExito = '';
    this.mensajeError = '';
    this.mostrarModalPerfil = true;
    this.perfilCargando = true;

    // Inicializar datos base desde la sesión
    this.datosPerfil.nombre = this.user?.nombreCompleto || '';
    this.datosPerfil.email = this.user?.email || '';
    this.datosPerfil.password = '';

    // Cargar teléfono y dirección real desde el endpoint de cliente
    this.clienteService.getMiPerfil().subscribe({
      next: (perfil) => {
        if (perfil) {
          if (perfil.nombre) this.datosPerfil.nombre = perfil.nombre;
          if (perfil.email) this.datosPerfil.email = perfil.email;
          this.datosPerfil.telefono = perfil.telefono || '';
          this.datosPerfil.direccion = perfil.direccion || '';
        }
        this.perfilCargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        // Si aún no tiene registro de cliente, usamos los datos del usuario en sesión
        this.perfilCargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cerrarModalPerfil() {
    this.mostrarModalPerfil = false;
    this.mensajeExito = '';
    this.mensajeError = '';
  }

  guardarPerfil() {
    if (!this.datosPerfil.nombre.trim()) {
      this.mensajeError = 'El nombre no puede estar vacío.';
      return;
    }

    this.guardandoPerfil = true;
    this.mensajeError = '';
    this.mensajeExito = '';

    const payload: any = {
      nombre: this.datosPerfil.nombre.trim(),
      telefono: this.datosPerfil.telefono.trim(),
      direccion: this.datosPerfil.direccion.trim()
    };

    if (this.datosPerfil.password && this.datosPerfil.password.trim().length > 0) {
      if (this.datosPerfil.password.trim().length < 6) {
        this.mensajeError = 'La contraseña debe tener al menos 6 caracteres.';
        this.guardandoPerfil = false;
        return;
      }
      payload.password = this.datosPerfil.password.trim();
    }

    this.clienteService.actualizarMiPerfil(payload).subscribe({
      next: (actualizado) => {
        this.guardandoPerfil = false;
        this.mensajeExito = '¡Tus datos se actualizaron correctamente!';
        
        // Actualizamos el nombre en el signal reactivo de sesión
        this.authService.updateCurrentUser({ nombreCompleto: actualizado.nombre || this.datosPerfil.nombre });
        
        this.cdr.detectChanges();
        setTimeout(() => {
          this.cerrarModalPerfil();
          this.cdr.detectChanges();
        }, 1500);
      },
      error: (err) => {
        this.guardandoPerfil = false;
        this.mensajeError = err.error?.mensaje || 'No se pudieron guardar los cambios.';
        this.cdr.detectChanges();
      }
    });
  }
}
