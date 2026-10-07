import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

import { apiErrorMessage } from '../../core/api-error';
import { Court, SURFACE_LABELS } from '../../core/models';
import { CourtDiagram } from './court-diagram';

@Component({
  selector: 'app-court-list',
  imports: [CourtDiagram],
  templateUrl: './court-list.html',
  styleUrl: './court-list.scss',
})
export class CourtList {
  private readonly http = inject(HttpClient);

  protected readonly courts = signal<Court[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly surfaceLabels = SURFACE_LABELS;

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
