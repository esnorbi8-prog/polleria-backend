import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../core/auth/auth';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  authService = inject(Auth);
  cdr = inject(ChangeDetectorRef);
  http = inject(HttpClient);
  
  credentials = {
    email: '',
    password: ''
  };

  errorMessage = '';
  loading = false;
  
  // Lógica de verificación
  mostrarVerificacion = false;
  token = '';
  verificando = false;
  mensajeVerificacion = '';
  exitoVerificacion = false;

  onSubmit() {
    this.loading = true;
    this.errorMessage = '';
    this.exitoVerificacion = false;
    this.cdr.detectChanges(); 

    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.mensaje || 'Credenciales inválidas. Intente nuevamente.';
        
        // Si el error es de verificación, mostramos la opción de verificar
        if (this.errorMessage.toLowerCase().includes('verificar')) {
           this.mostrarVerificacion = true;
        }
        this.cdr.detectChanges(); 
      }
    });
  }

  verificarToken() {
    if (!this.token) return;
    this.verificando = true;
    this.mensajeVerificacion = '';
    this.cdr.detectChanges();

    this.http.post(environment.apiUrl + '/auth/verificar-email?token=' + this.token, {}).subscribe({
      next: () => {
        this.verificando = false;
        this.exitoVerificacion = true;
        this.mostrarVerificacion = false;
        this.token = '';
        this.errorMessage = '';
        this.mensajeVerificacion = '¡Cuenta verificada con éxito! Ya puedes iniciar sesión.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.verificando = false;
        this.mensajeVerificacion = err.error?.mensaje || 'El token ingresado no es válido o ya fue usado.';
        this.cdr.detectChanges();
      }
    });
  }

  cancelarVerificacion() {
    this.mostrarVerificacion = false;
    this.token = '';
    this.mensajeVerificacion = '';
  }
}
