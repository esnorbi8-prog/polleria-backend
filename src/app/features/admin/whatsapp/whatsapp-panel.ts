import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WhatsappService, EstadoWhatsapp, MensajeWhatsapp, EstadoEnvio } from '../../../core/services/whatsapp.service';

type Filtro = 'TODOS' | EstadoEnvio;

/** Cada cuánto se refresca el historial (ms). */
const INTERVALO_REFRESCO_MS = 10000;
/** Mientras el WhatsApp por QR no está listo, el estado se consulta más seguido (el QR cambia cada ~20 s). */
const INTERVALO_ESTADO_QR_MS = 3000;

/**
 * Panel del bot de WhatsApp para el administrador (RF-A10):
 * estado de la conexión, mensaje de prueba e historial de avisos enviados.
 */
@Component({
  selector: 'app-whatsapp-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './whatsapp-panel.html',
  styleUrl: './whatsapp-panel.css'
})
export class WhatsappPanel implements OnInit, OnDestroy {
  private whatsappService = inject(WhatsappService);

  estado = signal<EstadoWhatsapp | null>(null);
  mensajes = signal<MensajeWhatsapp[]>([]);
  cargando = signal<boolean>(true);
  errorMsg = signal<string>('');

  // Filtros del historial
  filtro = signal<Filtro>('TODOS');
  filtroPedido = signal<string>('');

  // Formulario de prueba
  telefonoPrueba = '';
  textoPrueba = '';
  enviandoPrueba = signal<boolean>(false);
  resultadoPrueba = signal<MensajeWhatsapp | null>(null);

  // Mensaje desplegado en el historial
  expandidoId = signal<number | null>(null);

  desvinculando = signal<boolean>(false);

  private refrescoInterval: any;
  private estadoTimeout: any;

  mensajesFiltrados = computed(() => {
    const f = this.filtro();
    const pedido = this.filtroPedido().trim().replace('#', '');
    return this.mensajes().filter(m =>
      (f === 'TODOS' || m.estado === f) &&
      (!pedido || String(m.pedidoId ?? '') === pedido)
    );
  });

  contadores = computed(() => {
    const c: Record<EstadoEnvio, number> = { ENVIADO: 0, SIMULADO: 0, ERROR: 0, OMITIDO: 0 };
    for (const m of this.mensajes()) c[m.estado]++;
    return c;
  });

  ngOnInit() {
    this.cargarEstado();
    this.cargarMensajes(true);
    this.refrescoInterval = setInterval(() => this.cargarMensajes(false), INTERVALO_REFRESCO_MS);
  }

  ngOnDestroy() {
    if (this.refrescoInterval) clearInterval(this.refrescoInterval);
    if (this.estadoTimeout) clearTimeout(this.estadoTimeout);
  }

  cargarEstado() {
    if (this.estadoTimeout) clearTimeout(this.estadoTimeout);
    this.whatsappService.estado().subscribe({
      next: (e) => {
        this.estado.set(e);
        this.programarEstado(e);
      },
      error: () => this.errorMsg.set('No se pudo consultar el estado del bot. ¿Está encendido el backend y actualizado con el módulo de WhatsApp?')
    });
  }

  /** Con WhatsApp por QR, sigue consultando el estado hasta que quede listo (para mostrar el QR al día). */
  private programarEstado(e: EstadoWhatsapp) {
    const qrPendiente = e.proveedor === 'webqr' && !e.servicio?.listo;
    const espera = qrPendiente ? INTERVALO_ESTADO_QR_MS : INTERVALO_REFRESCO_MS * 3;
    this.estadoTimeout = setTimeout(() => this.cargarEstado(), espera);
  }

  /** ¿El bot puede enviar de verdad? (con QR, además el servicio tiene que estar listo) */
  puedeEnviar(): boolean {
    const e = this.estado();
    if (!e || !e.conectado) return false;
    return e.proveedor !== 'webqr' || !!e.servicio?.listo;
  }

  textoEstadoQr(): string {
    const s = this.estado()?.servicio;
    if (!s) return '';
    const textos: Record<string, string> = {
      iniciando: 'Iniciando WhatsApp... (la primera vez puede tardar ~1 minuto)',
      esperando_qr: 'Escanea el código QR',
      autenticando: 'QR escaneado, cargando WhatsApp...',
      listo: 'Vinculado',
      desconectado: 'Desconectado, reintentando...',
      apagado: 'Servicio apagado',
      error: 'Error en el servicio'
    };
    return textos[s.estado] || s.estado;
  }

  desvincular() {
    if (!confirm('¿Desvincular este WhatsApp? Tendrás que escanear un QR nuevo para volver a enviar mensajes.')) return;
    this.desvinculando.set(true);
    this.whatsappService.cerrarSesionQr().subscribe({
      next: () => {
        this.desvinculando.set(false);
        this.cargarEstado();
      },
      error: (e) => {
        this.desvinculando.set(false);
        this.errorMsg.set('No se pudo desvincular: ' + (e.error?.mensaje || e.error?.message || e.message));
      }
    });
  }

  /** "51935274267" -> "+51 935 274 267" (también sin el +) */
  numeroVinculado(): string {
    const n = this.estado()?.servicio?.numero;
    return n ? this.telefonoLegible(n) : '';
  }

  cargarMensajes(mostrarLoader: boolean) {
    if (mostrarLoader) this.cargando.set(true);
    this.whatsappService.mensajes().subscribe({
      next: (lista) => {
        this.mensajes.set(lista ?? []);
        this.cargando.set(false);
      },
      error: () => {
        if (mostrarLoader) this.errorMsg.set('No se pudo cargar el historial de mensajes.');
        this.cargando.set(false);
      }
    });
  }

  refrescar() {
    this.errorMsg.set('');
    this.cargarEstado();
    this.cargarMensajes(true);
  }

  enviarPrueba() {
    const telefono = this.telefonoPrueba.replace(/\D/g, '');
    if (telefono.length < 9) {
      this.resultadoPrueba.set(null);
      this.errorMsg.set('Escribe un celular válido de 9 dígitos, por ejemplo 935274267.');
      return;
    }
    this.errorMsg.set('');
    this.enviandoPrueba.set(true);
    this.resultadoPrueba.set(null);

    this.whatsappService.enviarPrueba(telefono, this.textoPrueba.trim()).subscribe({
      next: (m) => {
        this.resultadoPrueba.set(m);
        this.enviandoPrueba.set(false);
        this.cargarMensajes(false);
      },
      error: (e) => {
        this.errorMsg.set('No se pudo enviar la prueba: ' + (e.error?.mensaje || e.message));
        this.enviandoPrueba.set(false);
      }
    });
  }

  trackById(_: number, m: MensajeWhatsapp) {
    return m.id;
  }

  alternar(id: number) {
    this.expandidoId.set(this.expandidoId() === id ? null : id);
  }

  // ===================== Ayudas de presentación =====================

  fecha(f: string | number[]): Date {
    if (Array.isArray(f)) return new Date(f[0], f[1] - 1, f[2], f[3] || 0, f[4] || 0, f[5] || 0);
    return new Date(f);
  }

  claseEstado(estado: EstadoEnvio): string {
    const clases: Record<EstadoEnvio, string> = {
      ENVIADO: 'bg-success',
      SIMULADO: 'bg-secondary',
      ERROR: 'bg-danger',
      OMITIDO: 'bg-light text-dark border'
    };
    return clases[estado];
  }

  etiquetaMotivo(motivo: string): string {
    const etiquetas: Record<string, string> = {
      RECIBIDO: 'Pedido recibido',
      PAGADO: 'Pago confirmado',
      EN_PREPARACION: 'En preparación',
      LISTO: 'Listo',
      EN_CAMINO: 'Salió a reparto',
      ENTREGADO: 'Entregado',
      CANCELADO: 'Cancelado',
      REPARTIDOR_CERCA: 'Motorizado llegando',
      PRUEBA: 'Prueba'
    };
    return etiquetas[motivo] || motivo;
  }

  iconoMotivo(motivo: string): string {
    const iconos: Record<string, string> = {
      PAGADO: 'bi-check-circle',
      EN_PREPARACION: 'bi-fire',
      LISTO: 'bi-box-seam',
      EN_CAMINO: 'bi-bicycle',
      ENTREGADO: 'bi-house-check',
      CANCELADO: 'bi-x-circle',
      REPARTIDOR_CERCA: 'bi-geo-alt',
      PRUEBA: 'bi-chat-dots'
    };
    return iconos[motivo] || 'bi-chat';
  }

  /** "51935274267" -> "+51 935 274 267" */
  telefonoLegible(t: string): string {
    const d = (t || '').replace(/\D/g, '');
    if (d.length === 11 && d.startsWith('51')) return `+51 ${d.slice(2, 5)} ${d.slice(5, 8)} ${d.slice(8)}`;
    return t;
  }

  /** Traduce los errores más comunes de Meta y Twilio a una indicación en español. */
  ayudaError(detalle: string | null): string | null {
    if (!detalle) return null;
    const casos: [RegExp, string][] = [
      [/6301[56]/, 'Ese celular no está unido al sandbox de Twilio o pasaron más de 24 h. Debe enviar el código "join …" (o un "hola") al +1 415 523 8886.'],
      [/21654/, 'Twilio exige una plantilla para este envío. Crea una en el Content Template Builder y configura TWILIO_CONTENT_SID con su SID (HX...).'],
      [/20003|HTTP 401/, 'Credenciales incorrectas: revisa TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN (o el token de Meta).'],
      [/21211/, 'Número mal formado. Usa los 9 dígitos del celular.'],
      [/131031/, 'Meta tiene bloqueada la cuenta de WhatsApp Business. Usa Twilio mientras se resuelve.'],
      [/131030/, 'En Meta, ese número no está en la lista de destinatarios de prueba.'],
      [/"code":\s*190/, 'El token de Meta venció. Genera uno nuevo.'],
      [/132001/, 'La plantilla de Meta no existe o aún no está aprobada.'],
      [/no tiene WhatsApp/, 'Ese número no tiene una cuenta de WhatsApp.'],
      [/no está listo|HTTP 503/, 'El WhatsApp vinculado por QR no está listo: escanea el QR en este panel.'],
      [/Servicio WhatsApp|localhost:3001/, 'El servicio de WhatsApp (whatsapp-service) no responde: ejecuta npm start en esa carpeta.'],
      [/Sin conexión/, 'El backend no pudo conectarse al proveedor. Si usas WhatsApp por QR, revisa que whatsapp-service esté corriendo.']
    ];
    for (const [patron, ayuda] of casos) {
      if (patron.test(detalle)) return ayuda;
    }
    return null;
  }
}
