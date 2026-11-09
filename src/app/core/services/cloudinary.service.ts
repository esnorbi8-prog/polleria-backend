import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CloudinaryService {
  // Credenciales de Cloudinary
  readonly cloudName = 'vctujysi';
  readonly uploadPreset = 'polleria_preset';

  /**
   * Sube una imagen directamente al CDN de Cloudinary mediante su API REST pública (Unsigned).
   * Devuelve la URL HTTPS permanente y optimizada de la imagen en la nube.
   */
  async subirImagen(file: File): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', this.uploadPreset);

    const response = await fetch(`https://api.cloudinary.com/v1_1/${this.cloudName}/image/upload`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.error?.message || 'Error al subir la imagen a Cloudinary');
    }

    const data = await response.json();
    return data.secure_url;
  }
}
