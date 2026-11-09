import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

export interface LoginResponse {
  token: string;
  email: string;
  rol: string;
  nombreCompleto: string;
}

@Injectable({
  providedIn: 'root'
})
export class Auth {
  private apiUrl = environment.apiUrl + '/auth';
  
  // Usamos signals para el estado reactivo (Angular 17+)
  currentUser = signal<LoginResponse | null>(null);

  constructor(private http: HttpClient, private router: Router) {
    this.loadUserFromStorage();
  }

  /**
   * Carga el usuario desde sessionStorage.
   * Al usar sessionStorage, si el usuario cierra la pestaña o ventana,
   * la sesión se destruye automáticamente por seguridad.
   */
  private loadUserFromStorage() {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      const userStr = sessionStorage.getItem('polleria_user');
      if (userStr) {
        try {
          this.currentUser.set(JSON.parse(userStr));
        } catch (e) {
          console.error("Error parsing user from sessionStorage", e);
          sessionStorage.removeItem('polleria_user');
        }
      }
    }
  }

  login(credentials: any) {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        if (res.token) {
          if (typeof window !== 'undefined' && window.sessionStorage) {
            sessionStorage.setItem('polleria_user', JSON.stringify(res));
          }
          this.currentUser.set(res);
          this.redirectByRole(res.rol);
        }
      })
    );
  }

  registro(data: any) {
    return this.http.post(`${this.apiUrl}/registro`, data);
  }

  logout() {
    if (typeof window !== 'undefined' && window.sessionStorage) {
      sessionStorage.removeItem('polleria_user');
    }
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    const user = this.currentUser();
    return user ? user.token : null;
  }

  setToken(newToken: string) {
    const user = this.currentUser();
    if (user) {
      const updatedUser = { ...user, token: newToken };
      if (typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.setItem('polleria_user', JSON.stringify(updatedUser));
      }
      this.currentUser.set(updatedUser);
    }
  }

  updateCurrentUser(updates: Partial<LoginResponse>) {
    const user = this.currentUser();
    if (user) {
      const updatedUser = { ...user, ...updates };
      if (typeof window !== 'undefined' && window.sessionStorage) {
        sessionStorage.setItem('polleria_user', JSON.stringify(updatedUser));
      }
      this.currentUser.set(updatedUser);
    }
  }

  private redirectByRole(rol: string) {
    switch(rol) {
      case 'CLIENTE': this.router.navigate(['/cliente/home']); break;
      case 'COCINERO': this.router.navigate(['/cocina']); break;
      case 'REPARTIDOR': this.router.navigate(['/reparto']); break;
      case 'CAJERA': this.router.navigate(['/caja']); break;
      case 'ADMINISTRADOR': this.router.navigate(['/admin']); break;
      default: this.router.navigate(['/']); break;
    }
  }
}
