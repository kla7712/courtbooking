import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { apiErrorMessage, apiFieldErrors } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import { CourtDiagram } from '../courts/court-diagram';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, CourtDiagram],
  templateUrl: './register.html',
  styleUrl: './auth.scss',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  // Mismas reglas que valida la API, para avisar antes de enviar
  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Errores por campo devueltos por la API. */
  protected readonly serverErrors = signal<Record<string, string>>({});

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.serverErrors.set({});

    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (error: unknown) => {
        this.serverErrors.set(apiFieldErrors(error));
        this.error.set(apiErrorMessage(error));
        this.submitting.set(false);
      },
    });
  }
}
