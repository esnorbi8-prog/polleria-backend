import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../core/auth/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {
  authService = inject(Auth);
  
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
}
