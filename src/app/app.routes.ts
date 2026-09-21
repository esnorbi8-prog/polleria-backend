import { Routes } from '@angular/router';
import { roleGuard } from './core/guards/role-guard';
import { Login } from './auth/login/login';
import { Registro } from './auth/registro/registro';
import { Home } from './features/cliente/home/home';
import { CocinaTablero } from './features/cocina/tablero/tablero';
import { RepartoTablero } from './features/reparto/tablero/tablero';
import { Pos } from './features/caja/pos/pos';

import { Pedidos } from './features/cliente/pedidos/pedidos';
import { Exito } from './features/pago/exito/exito';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'registro', component: Registro },
  { 
    path: 'cliente/home', 
    component: Home,
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE'] }
  },
  { 
    path: 'cliente/pedidos', 
    component: Pedidos,
    canActivate: [roleGuard],
    data: { roles: ['CLIENTE'] }
  },
  {
    path: 'pago/exito',
    component: Exito
  },
  { 
    path: 'cocina', 
    component: CocinaTablero,
    canActivate: [roleGuard],
    data: { roles: ['COCINERO', 'ADMINISTRADOR'] }
  },
  { 
    path: 'reparto', 
    component: RepartoTablero,
    canActivate: [roleGuard],
    data: { roles: ['REPARTIDOR', 'ADMINISTRADOR'] }
  },
  { 
    path: 'caja', 
    component: Pos,
    canActivate: [roleGuard],
    data: { roles: ['CAJERA', 'ADMINISTRADOR'] }
  },
  { 
    path: 'admin', 
    loadComponent: () => import('./features/admin/dashboard/dashboard').then(m => m.Dashboard),
    canActivate: [roleGuard],
    data: { roles: ['ADMINISTRADOR'] }
  },
  { path: '**', redirectTo: 'login' }
];
