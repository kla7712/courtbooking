import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { apiErrorMessage } from '../../core/api-error';
import { Court, SURFACE_LABELS, Surface } from '../../core/models';
import { CourtDiagram } from './court-diagram';

@Component({
  selector: 'app-court-list',
  imports: [CourtDiagram, RouterLink],
  templateUrl: './court-list.html',
  styleUrl: './court-list.scss',
})
export class CourtList {
  private readonly http = inject(HttpClient);

  protected readonly courts = signal<Court[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly surfaceLabels = SURFACE_LABELS;

  /** Superficie elegida en el filtro; null muestra todas. */
  protected readonly surfaceFilter = signal<Surface | null>(null);

  /** Solo se ofrecen en el filtro las superficies que el club tiene. */
  protected readonly surfaces = computed(() => {
    const present = new Set(this.courts().map((court) => court.surface));
    return (Object.keys(SURFACE_LABELS) as Surface[]).filter((surface) => present.has(surface));
  });

  protected readonly visibleCourts = computed(() => {
    const filter = this.surfaceFilter();
    return filter ? this.courts().filter((court) => court.surface === filter) : this.courts();
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.http.get<Court[]>('/api/courts').subscribe({
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
}
