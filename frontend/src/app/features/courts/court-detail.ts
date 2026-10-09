import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';

import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth/auth.service';
import {
  addDays,
  addMinutes,
  clubNow,
  formatDate,
  formatLongDate,
  formatPrice,
  shortTime,
} from '../../core/format';
import { Availability, Booking, Court, SURFACE_LABELS, Slot, SlotOption } from '../../core/models';
import { CourtDiagram } from './court-diagram';

/** Días que se pueden reservar: hoy y los siete siguientes (la antelación máxima de la API). */
const BOOKABLE_DAYS = 8;

interface DayOption {
  date: string;
  weekday: string;
  dayNumber: string;
}

@Component({
  selector: 'app-court-detail',
  imports: [CourtDiagram, RouterLink],
  templateUrl: './court-detail.html',
  styleUrl: './court-detail.scss',
})
export class CourtDetail {
  private readonly http = inject(HttpClient);
  protected readonly auth = inject(AuthService);

  /** Identificador de la pista, tomado de la URL (/pistas/:id). */
  readonly id = input.required<string>();

  protected readonly surfaceLabels = SURFACE_LABELS;
  protected readonly formatPrice = formatPrice;
  protected readonly shortTime = shortTime;

  protected readonly today = clubNow().date;
  protected readonly days: DayOption[] = Array.from({ length: BOOKABLE_DAYS }, (_, index) => {
    const date = addDays(this.today, index);
    return {
      date,
      weekday: index === 0 ? 'Hoy' : formatDate(date, { weekday: 'short' }).replace('.', ''),
      dayNumber: formatDate(date, { day: 'numeric' }),
    };
  });

  protected readonly court = signal<Court | null>(null);
  protected readonly courtError = signal<string | null>(null);

  protected readonly selectedDate = signal(this.today);
  protected readonly availability = signal<Availability | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  /** Hora y duración elegidas para reservar. */
  protected readonly selectedSlot = signal<Slot | null>(null);
  protected readonly selectedOption = signal<SlotOption | null>(null);

  protected readonly booking = signal(false);
  protected readonly bookingError = signal<string | null>(null);
  protected readonly confirmed = signal<Booking | null>(null);

  /** Cambia tras reservar para volver a pedir la disponibilidad del mismo día. */
  private readonly reload = signal(0);

  protected readonly selectedDateLabel = computed(() => formatLongDate(this.selectedDate()));

  protected readonly selectedEndTime = computed(() => {
    const slot = this.selectedSlot();
    const option = this.selectedOption();
    return slot && option ? addMinutes(slot.startTime, option.durationMinutes) : null;
  });

  /** Dirección de esta página, para volver a ella después de iniciar sesión. */
  protected readonly returnUrl = computed(() => `/pistas/${this.id()}`);

  constructor() {
    // Datos de la pista: se piden cada vez que cambia el id de la URL
    toObservable(this.id)
      .pipe(
        tap(() => {
          this.court.set(null);
          this.courtError.set(null);
        }),
        switchMap((id) =>
          this.http.get<Court>(`/api/courts/${id}`).pipe(
            catchError((error: unknown) => {
              this.courtError.set(apiErrorMessage(error, 'No se ha podido cargar la pista.'));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((court) => this.court.set(court));

    // Disponibilidad: se pide al cambiar de pista, de día o tras una reserva.
    // switchMap descarta la respuesta anterior si se cambia de día antes de que llegue.
    const query = computed(() => ({ id: this.id(), date: this.selectedDate(), reload: this.reload() }));
    toObservable(query)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
          this.clearSelection();
        }),
        switchMap(({ id, date }) =>
          this.http.get<Availability>(`/api/courts/${id}/availability`, { params: { date } }).pipe(
            catchError((error: unknown) => {
              this.error.set(apiErrorMessage(error, 'No se han podido cargar las horas libres.'));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((availability) => {
        this.availability.set(availability);
        this.loading.set(false);
      });
  }

  protected selectDate(date: string): void {
    this.confirmed.set(null);
    this.selectedDate.set(date);
  }

  protected selectSlot(slot: Slot): void {
    this.confirmed.set(null);
    this.bookingError.set(null);
    this.selectedSlot.set(slot);
    // Por defecto se marca la duración más corta disponible
    this.selectedOption.set(slot.options[0] ?? null);
  }

  protected selectOption(option: SlotOption): void {
    this.bookingError.set(null);
    this.selectedOption.set(option);
  }

  protected lowestPrice(slot: Slot): number {
    return Math.min(...slot.options.map((option) => option.price));
  }

  protected book(): void {
    const slot = this.selectedSlot();
    const option = this.selectedOption();
    if (!slot || !option || this.booking()) {
      return;
    }
    this.booking.set(true);
    this.bookingError.set(null);

    this.http
      .post<Booking>('/api/bookings', {
        courtId: Number(this.id()),
        date: this.selectedDate(),
        startTime: shortTime(slot.startTime),
        durationMinutes: option.durationMinutes,
      })
      .subscribe({
        next: (booking) => {
          this.booking.set(false);
          this.reload.update((count) => count + 1);
          this.confirmed.set(booking);
        },
        error: (error: unknown) => {
          this.booking.set(false);
          this.bookingError.set(apiErrorMessage(error, 'No se ha podido hacer la reserva.'));
        },
      });
  }

  protected confirmedDateLabel(booking: Booking): string {
    return formatLongDate(booking.date);
  }

  private clearSelection(): void {
    this.selectedSlot.set(null);
    this.selectedOption.set(null);
    this.bookingError.set(null);
  }
}
