import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface ItemPedidoRequest {
  productoId: number;
  cantidad: number;
}

export interface PedidoRequest {
  clienteId: number;
  tipoEntrega: string;
  direccionEntrega?: string;
  items: ItemPedidoRequest[];
}

export interface Pedido {
  id: number;
  tipoEntrega: string;
  direccionEntrega: string;
  estado: string;
  total: number;
  fechaCreacion: string;
  cliente: {
    id: number;
    nombre: string;
    telefono: string;
  };
  items: any[];
}

@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/pedidos';

  crearPedido(request: PedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.apiUrl, request);
  }

  getPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.apiUrl);
  }

  crearPreferenciaPago(pedidoId: number): Observable<{initPoint: string, sandboxInitPoint: string, preferenciaId: string}> {
    return this.http.post<any>(`${this.apiUrl}/${pedidoId}/pago`, {});
  }

  confirmarPagoSimulado(pedidoId: number, paymentId: string = '123456789'): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.apiUrl}/${pedidoId}/pago/confirmar?paymentId=${paymentId}`, {});
  }

  cambiarEstado(pedidoId: number, nuevoEstado: string, observacion: string = ''): Observable<Pedido> {
    return this.http.patch<Pedido>(`${this.apiUrl}/${pedidoId}/estado`, { nuevoEstado, observacion });
  }
}
