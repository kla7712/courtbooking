import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { apiErrorMessage } from '../../core/api-error';
import { Court, SURFACE_LABELS, Surface } from '../../core/models';

@Component({
  selector: 'app-admin-courts',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-courts.html',
  styleUrl: './admin.scss',
})
export class AdminCourts {
  private readonly http = inject(HttpClient);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly surfaceLabels = SURFACE_LABELS;
  protected readonly surfaces = Object.keys(SURFACE_LABELS) as Surface[];

  protected readonly courts = signal<Court[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  /** Pista que se está editando; null cuando el formulario crea una nueva. */
  protected readonly editing = signal<Court | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  /** Pista para la que se pide confirmar la desactivación. */
  protected readonly confirmingId = signal<number | null>(null);
  protected readonly deactivatingId = signal<number | null>(null);
  protected readonly activatingId = signal<number | null>(null);
  protected readonly rowError = signal<{ id: number; message: string } | null>(null);

  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(50)]],
    surface: this.formBuilder.control<Surface>('CLAY'),
    indoor: [false],
    hasLighting: [false],
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.http.get<Court[]>('/api/admin/courts').subscribe({
      next: (courts) => {
        this.courts.set(courts);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiErrorMessage(error, 'No se han podido cargar las pistas.'));
        this.loading.set(false);
      },
    });
  }

  protected edit(court: Court): void {
    this.editing.set(court);
    this.formError.set(null);
    this.form.setValue({
      name: court.name,
      surface: court.surface,
      indoor: court.indoor,
      hasLighting: court.hasLighting,
    });
  }

  protected resetForm(): void {
    this.editing.set(null);
    this.formError.set(null);
    this.form.reset();
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const current = this.editing();
    const body = { ...this.form.getRawValue(), name: this.form.controls.name.value.trim() };
    const request = current
      ? this.http.put<Court>(`/api/courts/${current.id}`, body)
      : this.http.post<Court>('/api/courts', body);

    this.saving.set(true);
    this.formError.set(null);

    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.courts.update((courts) =>
          (current ? courts.map((court) => (court.id === saved.id ? saved : court)) : [...courts, saved]).sort(
            (a, b) => a.name.localeCompare(b.name, 'es', { numeric: true }),
          ),
        );
        this.resetForm();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.formError.set(apiErrorMessage(error, 'No se ha podido guardar la pista.'));
      },
    });
  }

  protected askToDeactivate(court: Court): void {
    this.rowError.set(null);
    this.confirmingId.set(court.id);
  }

  protected keep(): void {
    this.confirmingId.set(null);
  }

  protected activate(court: Court): void {
    this.activatingId.set(court.id);
    this.rowError.set(null);

    this.http.put<Court>(`/api/courts/${court.id}/activate`, null).subscribe({
      next: (saved) => {
        this.activatingId.set(null);
        this.courts.update((courts) => courts.map((item) => (item.id === saved.id ? saved : item)));
      },
      error: (error: unknown) => {
        this.activatingId.set(null);
        this.rowError.set({
          id: court.id,
          message: apiErrorMessage(error, 'No se ha podido activar la pista.'),
        });
      },
    });
  }

  protected deactivate(court: Court): void {
    this.deactivatingId.set(court.id);

    this.http.delete<void>(`/api/courts/${court.id}`).subscribe({
      next: () => {
        this.deactivatingId.set(null);
        this.confirmingId.set(null);
        // La pista no se borra: sigue en la lista, marcada como desactivada
        this.courts.update((courts) =>
          courts.map((item) => (item.id === court.id ? { ...item, active: false } : item)),
        );
        if (this.editing()?.id === court.id) {
          this.resetForm();
        }
      },
      error: (error: unknown) => {
        this.deactivatingId.set(null);
        this.confirmingId.set(null);
        this.rowError.set({
          id: court.id,
          message: apiErrorMessage(error, 'No se ha podido desactivar la pista.'),
        });
      },
    });
  }
}
