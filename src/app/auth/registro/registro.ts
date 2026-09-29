import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './registro.html',
  styleUrl: './registro.css'
})
export class Registro {
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  
  registroData = {
    nombreCompleto: '',
    email: '',
    password: '',
    telefono: '',
    direccion: ''
  };

  loading = false;
  errorMensaje = '';
  exito = false;
  tokenGenerado = ''; // Para mostrar en la UI de demo

  onSubmit() {
    this.loading = true;
    this.errorMensaje = '';
    this.cdr.detectChanges(); 
    
    this.http.post<any>(environment.apiUrl + '/auth/registro', this.registroData).subscribe({
      next: (res) => {
        this.loading = false;
        this.exito = true;
        this.tokenGenerado = res.tokenVerificacion; // Lo extraemos de la respuesta
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        this.loading = false;
        this.errorMensaje = err.error?.mensaje || 'Error al registrar cuenta. Puede que el correo ya exista.';
        this.cdr.detectChanges(); 
      }
    });
  }
}
