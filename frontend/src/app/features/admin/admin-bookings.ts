import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, filter, of, switchMap, tap } from 'rxjs';

import { apiErrorMessage } from '../../core/api-error';
import { addDays, clubNow, formatLongDate, formatPrice, shortTime } from '../../core/format';
import { AdminBooking } from '../../core/models';

@Component({
  selector: 'app-admin-bookings',
  templateUrl: './admin-bookings.html',
  styleUrl: './admin.scss',
})
export class AdminBookings {
  private readonly http = inject(HttpClient);

  protected readonly formatPrice = formatPrice;
  protected readonly shortTime = shortTime;

  protected readonly date = signal(clubNow().date);
  protected readonly dateLabel = computed(() => formatLongDate(this.date()));

  protected readonly bookings = signal<AdminBooking[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  /** Reserva para la que se pide confirmar la cancelación. */
  protected readonly confirmingId = signal<number | null>(null);
  protected readonly cancellingId = signal<number | null>(null);
  protected readonly rowError = signal<{ id: number; message: string } | null>(null);

  protected readonly confirmedCount = computed(
    () => this.bookings().filter((booking) => booking.status === 'CONFIRMED').length,
  );

  constructor() {
    // Las reservas se vuelven a pedir cada vez que cambia el día
    toObservable(this.date)
      .pipe(
        filter((date) => date !== ''),
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
          this.rowError.set(null);
          this.confirmingId.set(null);
        }),
        switchMap((date) =>
          this.http.get<AdminBooking[]>('/api/admin/bookings', { params: { date } }).pipe(
            catchError((error: unknown) => {
              this.error.set(apiErrorMessage(error, 'No se han podido cargar las reservas.'));
              return of<AdminBooking[]>([]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((bookings) => {
        this.bookings.set(bookings);
        this.loading.set(false);
      });
  }

  protected selectDate(value: string): void {
    this.date.set(value);
  }

  protected shiftDay(days: number): void {
    this.date.update((date) => addDays(date, days));
  }

  /** Solo se puede cancelar una reserva confirmada que todavía no ha empezado. */
  protected canCancel(booking: AdminBooking): boolean {
    const now = clubNow();
    return (
      booking.status === 'CONFIRMED' &&
      `${booking.date} ${shortTime(booking.startTime)}` > `${now.date} ${now.time}`
    );
  }

  protected askToCancel(booking: AdminBooking): void {
    this.rowError.set(null);
    this.confirmingId.set(booking.id);
  }

  protected keep(): void {
    this.confirmingId.set(null);
  }

  protected cancel(booking: AdminBooking): void {
    this.cancellingId.set(booking.id);

    this.http.delete<void>(`/api/bookings/${booking.id}`).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.confirmingId.set(null);
        this.bookings.update((bookings) =>
          bookings.map((item) => (item.id === booking.id ? { ...item, status: 'CANCELLED' } : item)),
        );
      },
      error: (error: unknown) => {
        this.cancellingId.set(null);
        this.confirmingId.set(null);
        this.rowError.set({
          id: booking.id,
          message: apiErrorMessage(error, 'No se ha podido cancelar la reserva.'),
        });
      },
    });
  }
}
