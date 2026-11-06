import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UbicacionRepartidor {
  pedidoId: number;
  latitud: number;
  longitud: number;
  precisionMetros: number | null;
  repartidorEmail: string | null;
  fechaActualizacion: string | number[];
}

/**
 * Geolocalización del repartidor (RF-D02 / RF-C09).
 * Solo maneja coordenadas: el mapa se dibuja con Leaflet + OpenStreetMap (gratis, sin API key).
 */
@Injectable({ providedIn: 'root' })
export class UbicacionService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/pedidos';

  /** El repartidor manda su posición actual para un pedido EN_CAMINO. */
  enviar(pedidoId: number, latitud: number, longitud: number, precisionMetros: number | null): Observable<UbicacionRepartidor> {
    return this.http.post<UbicacionRepartidor>(`${this.apiUrl}/${pedidoId}/ubicacion`, { latitud, longitud, precisionMetros });
  }

  /** Última posición conocida del repartidor (null si todavía no envió ninguna). */
  obtener(pedidoId: number): Observable<UbicacionRepartidor | null> {
    return this.http.get<UbicacionRepartidor | null>(`${this.apiUrl}/${pedidoId}/ubicacion`);
  }
}
