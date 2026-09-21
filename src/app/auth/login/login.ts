import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../core/auth/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  authService = inject(Auth);
  
  credentials = {
    email: '',
    password: ''
  };

  errorMessage = '';
  loading = false;

  onSubmit() {
    this.loading = true;
    this.errorMessage = '';
    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.loading = false;
        // La redirección ya la hace el Auth
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Credenciales inválidas. Intente nuevamente.';
      }
    });
  }
}
