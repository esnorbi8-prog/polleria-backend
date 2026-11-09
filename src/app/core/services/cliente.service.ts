import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface Cliente {
  id: number;
  nombre: string;
  telefono: string;
  email: string;
  direccion: string;
  usuarioId: number;
}

export interface ActualizarPerfilDTO {
  nombre?: string;
  telefono?: string;
  direccion?: string;
  password?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClienteService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/clientes';

  getMiPerfil(): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.apiUrl}/me`);
  }

  actualizarMiPerfil(datos: ActualizarPerfilDTO): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.apiUrl}/me`, datos);
  }
}
