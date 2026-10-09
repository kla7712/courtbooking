import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { apiErrorMessage } from '../../core/api-error';
import { clubNow, formatLongDate, formatPrice, shortTime } from '../../core/format';
import { Booking } from '../../core/models';

@Component({
  selector: 'app-my-bookings',
  imports: [RouterLink],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.scss',
})
export class MyBookings {
  private readonly http = inject(HttpClient);

  protected readonly formatLongDate = formatLongDate;
  protected readonly formatPrice = formatPrice;
  protected readonly shortTime = shortTime;

  protected readonly bookings = signal<Booking[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  /** Reserva para la que se está pidiendo confirmación de cancelación. */
  protected readonly confirmingId = signal<number | null>(null);
  protected readonly cancellingId = signal<number | null>(null);
  protected readonly cancelError = signal<{ id: number; message: string } | null>(null);

  /** Reservas confirmadas que todavía no han terminado, de la más próxima a la más lejana. */
  protected readonly upcoming = computed(() =>
    this.bookings()
      .filter((booking) => this.isUpcoming(booking))
      .sort((a, b) => this.sortKey(a).localeCompare(this.sortKey(b))),
  );

  /** Reservas ya jugadas o canceladas, de la más reciente a la más antigua. */
  protected readonly past = computed(() =>
    this.bookings()
      .filter((booking) => !this.isUpcoming(booking))
      .sort((a, b) => this.sortKey(b).localeCompare(this.sortKey(a))),
  );

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.http.get<Booking[]>('/api/bookings/mine').subscribe({
      next: (bookings) => {
        this.bookings.set(bookings);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiErrorMessage(error, 'No se han podido cargar tus reservas.'));
        this.loading.set(false);
      },
    });
  }

  protected askToCancel(booking: Booking): void {
    this.cancelError.set(null);
    this.confirmingId.set(booking.id);
  }

  protected keep(): void {
    this.confirmingId.set(null);
  }

  protected cancel(booking: Booking): void {
    this.cancellingId.set(booking.id);
    this.cancelError.set(null);

    this.http.delete<void>(`/api/bookings/${booking.id}`).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.confirmingId.set(null);
        // Se actualiza en pantalla sin volver a pedir toda la lista
        this.bookings.update((bookings) =>
          bookings.map((item) => (item.id === booking.id ? { ...item, status: 'CANCELLED' } : item)),
        );
      },
      error: (error: unknown) => {
        this.cancellingId.set(null);
        this.confirmingId.set(null);
        this.cancelError.set({
          id: booking.id,
          message: apiErrorMessage(error, 'No se ha podido cancelar la reserva.'),
        });
      },
    });
  }

  private isUpcoming(booking: Booking): boolean {
    const now = clubNow();
    return (
      booking.status === 'CONFIRMED' &&
      `${booking.date} ${shortTime(booking.endTime)}` > `${now.date} ${now.time}`
    );
  }

  private sortKey(booking: Booking): string {
    return `${booking.date} ${shortTime(booking.startTime)}`;
  }
}
