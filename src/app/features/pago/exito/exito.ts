import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PedidoService } from '../../../core/services/pedido.service';

@Component({
  selector: 'app-exito',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './exito.html',
  styleUrl: './exito.css'
})
export class Exito implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private pedidoService = inject(PedidoService);

  estado = signal<'cargando' | 'exito' | 'error'>('cargando');
  mensaje = signal<string>('Validando tu pago con Mercado Pago...');

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const pedidoId = params['pedido'];
      const paymentId = params['payment_id'];

      if (!pedidoId || !paymentId) {
        this.estado.set('error');
        this.mensaje.set('Datos incompletos desde Mercado Pago.');
        return;
      }

      this.pedidoService.confirmarPagoSimulado(pedidoId, paymentId).subscribe({
        next: () => {
          this.estado.set('exito');
          this.mensaje.set('¡Tu pago ha sido confirmado con éxito!');
          
          // Redirigir de vuelta a Mis Pedidos después de unos segundos
          setTimeout(() => {
            this.router.navigate(['/cliente/pedidos']);
          }, 3000);
        },
        error: (err) => {
          this.estado.set('error');
          this.mensaje.set('Error al confirmar el pago: ' + (err.error?.mensaje || err.message));
        }
      });
    });
  }
}
