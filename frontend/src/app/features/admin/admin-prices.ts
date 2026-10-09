import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';

import { apiErrorMessage } from '../../core/api-error';
import { shortTime } from '../../core/format';
import { DAY_TYPE_LABELS, PriceRule, SURFACE_LABELS, Surface } from '../../core/models';

@Component({
  selector: 'app-admin-prices',
  templateUrl: './admin-prices.html',
  styleUrl: './admin.scss',
})
export class AdminPrices {
  private readonly http = inject(HttpClient);

  protected readonly surfaceLabels = SURFACE_LABELS;
  protected readonly dayTypeLabels = DAY_TYPE_LABELS;
  protected readonly shortTime = shortTime;

  protected readonly rules = signal<PriceRule[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  /** Precios escritos y todavía sin guardar, por id de tarifa. */
  protected readonly drafts = signal<Record<number, string>>({});
  protected readonly savingId = signal<number | null>(null);
  protected readonly savedId = signal<number | null>(null);
  protected readonly rowError = signal<{ id: number; message: string } | null>(null);

  /** Tarifas agrupadas por superficie, en el orden en que las devuelve la API. */
  protected readonly groups = computed(() => {
    const bySurface = new Map<Surface, PriceRule[]>();
    for (const rule of this.rules()) {
      bySurface.set(rule.surface, [...(bySurface.get(rule.surface) ?? []), rule]);
    }
    return [...bySurface.entries()].map(([surface, rules]) => ({ surface, rules }));
  });

  constructor() {
    this.http.get<PriceRule[]>('/api/price-rules').subscribe({
      next: (rules) => {
        this.rules.set(rules);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiErrorMessage(error, 'No se han podido cargar las tarifas.'));
        this.loading.set(false);
      },
    });
  }

  /** Valor del campo: el borrador si lo hay, o el precio guardado. */
  protected value(rule: PriceRule): string {
    return this.drafts()[rule.id] ?? rule.pricePerHour.toFixed(2).replace('.', ',');
  }

  protected setDraft(rule: PriceRule, value: string): void {
    this.savedId.set(null);
    this.rowError.set(null);
    this.drafts.update((drafts) => ({ ...drafts, [rule.id]: value }));
  }

  /** El precio escrito, o null si no es válido (número de 0 a 9999,99 con dos decimales como máximo). */
  protected parsed(rule: PriceRule): number | null {
    const text = this.value(rule).trim().replace(',', '.');
    return /^\d{1,4}(\.\d{1,2})?$/.test(text) ? Number(text) : null;
  }

  protected hasChanged(rule: PriceRule): boolean {
    return rule.id in this.drafts() && this.parsed(rule) !== rule.pricePerHour;
  }

  protected save(rule: PriceRule): void {
    const pricePerHour = this.parsed(rule);
    if (pricePerHour === null) {
      return;
    }
    this.savingId.set(rule.id);
    this.rowError.set(null);

    this.http.put<PriceRule>(`/api/price-rules/${rule.id}`, { pricePerHour }).subscribe({
      next: (saved) => {
        this.savingId.set(null);
        this.savedId.set(saved.id);
        this.rules.update((rules) => rules.map((item) => (item.id === saved.id ? saved : item)));
        this.drafts.update(({ [saved.id]: _removed, ...rest }) => rest);
      },
      error: (error: unknown) => {
        this.savingId.set(null);
        this.rowError.set({
          id: rule.id,
          message: apiErrorMessage(error, 'No se ha podido guardar el precio.'),
        });
      },
    });
  }
}
