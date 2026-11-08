import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Estado del servicio de WhatsApp vinculado por QR (solo con proveedor "webqr"). */
export interface EstadoServicioQr {
  disponible: boolean;
  estado: 'iniciando' | 'esperando_qr' | 'autenticando' | 'listo' | 'desconectado' | 'apagado' | 'error';
  listo?: boolean;
  numero?: string | null;
  nombre?: string | null;
  qr?: string | null;       // imagen PNG en base64 lista para <img [src]>
  error?: string | null;
}

export interface EstadoWhatsapp {
  proveedor: 'meta' | 'twilio' | 'webqr';
  conectado: boolean;
  modoEnvio: string;
  servicio?: EstadoServicioQr;
}

export type EstadoEnvio = 'ENVIADO' | 'SIMULADO' | 'ERROR' | 'OMITIDO';

export interface MensajeWhatsapp {
  id: number;
  pedidoId: number | null;
  destinatario: string;
  motivo: string;
  texto: string;
  estado: EstadoEnvio;
  detalle: string | null;
  fecha: string | number[];
}

/** Supervisión del bot de WhatsApp (RF-A10). Solo ADMINISTRADOR. */
@Injectable({ providedIn: 'root' })
export class WhatsappService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/whatsapp';

  estado(): Observable<EstadoWhatsapp> {
    return this.http.get<EstadoWhatsapp>(`${this.apiUrl}/estado`);
  }

  mensajes(): Observable<MensajeWhatsapp[]> {
    return this.http.get<MensajeWhatsapp[]>(`${this.apiUrl}/mensajes`);
  }

  mensajesDePedido(pedidoId: number): Observable<MensajeWhatsapp[]> {
    return this.http.get<MensajeWhatsapp[]>(`${this.apiUrl}/mensajes/pedido/${pedidoId}`);
  }

  enviarPrueba(telefono: string, texto: string): Observable<MensajeWhatsapp> {
    return this.http.post<MensajeWhatsapp>(`${this.apiUrl}/prueba`, { telefono, texto });
  }

  /** Desvincula el WhatsApp vinculado por QR (el servicio mostrará un QR nuevo). */
  cerrarSesionQr(): Observable<{ mensaje: string }> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/webqr/cerrar-sesion`, {});
  }
}
