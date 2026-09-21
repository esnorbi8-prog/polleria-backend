import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro.html',
  styleUrl: './registro.css'
})
export class Registro {
  private http = inject(HttpClient);
  
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

  onSubmit() {
    this.loading = true;
    this.errorMensaje = '';
    
    this.http.post(environment.apiUrl + '/auth/registro', this.registroData).subscribe({
      next: () => {
        this.loading = false;
        this.exito = true;
      },
      error: (err) => {
        this.loading = false;
        this.errorMensaje = err.error?.mensaje || 'Error al registrar cuenta. Puede que el correo ya exista.';
      }
    });
  }
}
