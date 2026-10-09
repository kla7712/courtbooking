import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, filter, of, switchMap, tap } from 'rxjs';

import { apiErrorMessage } from '../../core/api-error';
import { clubNow, formatLongDate, shortTime } from '../../core/format';
import { Court, CourtBlock } from '../../core/models';

@Component({
  selector: 'app-admin-blocks',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-blocks.html',
  styleUrl: './admin.scss',
})
export class AdminBlocks {
  private readonly http = inject(HttpClient);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly shortTime = shortTime;

  protected readonly courts = signal<Court[]>([]);
  protected readonly courtsError = signal<string | null>(null);

  protected readonly courtId = signal<number | null>(null);
  protected readonly date = signal(clubNow().date);
  protected readonly dateLabel = computed(() => formatLongDate(this.date()));

  protected readonly blocks = signal<CourtBlock[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly deletingId = signal<number | null>(null);
  protected readonly rowError = signal<{ id: number; message: string } | null>(null);

  protected readonly form = this.formBuilder.group({
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
    reason: ['', [Validators.required, Validators.maxLength(200)]],
  });

  /** La hora de fin debe ser posterior a la de inicio (misma regla que valida la API). */
  protected endBeforeStart(): boolean {
    const { startTime, endTime } = this.form.getRawValue();
    return startTime !== '' && endTime !== '' && endTime <= startTime;
  }

  constructor() {
    this.http.get<Court[]>('/api/courts').subscribe({
      next: (courts) => {
        this.courts.set(courts);
        this.courtId.set(courts[0]?.id ?? null);
        if (courts.length === 0) {
          this.loading.set(false);
        }
      },
      error: (error: unknown) => {
        this.courtsError.set(apiErrorMessage(error, 'No se han podido cargar las pistas.'));
        this.loading.set(false);
      },
    });

    // Los bloqueos se vuelven a pedir cada vez que cambia la pista o el día
    const query = computed(() => ({ courtId: this.courtId(), date: this.date() }));
    toObservable(query)
      .pipe(
        filter((q): q is { courtId: number; date: string } => q.courtId !== null && q.date !== ''),
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
          this.rowError.set(null);
        }),
        switchMap(({ courtId, date }) =>
          this.http.get<CourtBlock[]>(`/api/courts/${courtId}/blocks`, { params: { date } }).pipe(
            catchError((error: unknown) => {
              this.error.set(apiErrorMessage(error, 'No se han podido cargar los bloqueos.'));
              return of<CourtBlock[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((blocks) => {
        this.blocks.set(blocks);
        this.loading.set(false);
      });
  }

  protected selectCourt(value: string): void {
    this.courtId.set(Number(value));
  }

  protected selectDate(value: string): void {
    this.date.set(value);
  }

  protected create(): void {
    const courtId = this.courtId();
    if (this.form.invalid || this.endBeforeStart() || courtId === null || this.date() === '') {
      this.form.markAllAsTouched();
      return;
    }
    const { startTime, endTime, reason } = this.form.getRawValue();
    this.saving.set(true);
    this.formError.set(null);

    this.http
      .post<CourtBlock>(`/api/courts/${courtId}/blocks`, {
        date: this.date(),
        startTime,
        endTime,
        reason: reason.trim(),
      })
      .subscribe({
        next: (block) => {
          this.saving.set(false);
          this.blocks.update((blocks) =>
            [...blocks, block].sort((a, b) => a.startTime.localeCompare(b.startTime)),
          );
          this.form.reset();
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.formError.set(apiErrorMessage(error, 'No se ha podido bloquear la pista.'));
        },
      });
  }

  protected remove(block: CourtBlock): void {
    this.deletingId.set(block.id);
    this.rowError.set(null);

    this.http.delete<void>(`/api/courts/${block.courtId}/blocks/${block.id}`).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.blocks.update((blocks) => blocks.filter((item) => item.id !== block.id));
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.rowError.set({
          id: block.id,
          message: apiErrorMessage(error, 'No se ha podido quitar el bloqueo.'),
        });
      },
    });
  }
}
