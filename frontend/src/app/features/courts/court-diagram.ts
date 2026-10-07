import { Component, input } from '@angular/core';

import { Surface } from '../../core/models';

/**
 * Dibujo de una pista vista desde arriba, a escala real (23,77 × 10,97 m),
 * pintada con el color de su superficie.
 */
@Component({
  selector: 'app-court-diagram',
  template: `
    <svg viewBox="0 0 240 120" [attr.data-surface]="surface()" role="img" [attr.aria-label]="label()">
      <rect class="surround" width="240" height="120" />
      <rect class="playing-area" x="20" y="14" width="200" height="92" />
      <g class="lines">
        <!-- Contorno de dobles -->
        <rect x="20" y="14" width="200" height="92" />
        <!-- Líneas laterales de individuales -->
        <path d="M20 25.5H220M20 94.5H220" />
        <!-- Líneas de saque y línea central -->
        <path d="M66.2 25.5V94.5M173.8 25.5V94.5M66.2 60H173.8" />
        <!-- Marcas centrales de fondo -->
        <path d="M20 60H23M217 60H220" />
      </g>
      <path class="net" d="M120 10V110" />
    </svg>
  `,
  styles: `
    :host {
      display: block;
    }

    svg {
      display: block;
      width: 100%;
      height: auto;
    }

    svg[data-surface='CLAY'] {
      --surface: var(--clay);
    }
    svg[data-surface='HARD'] {
      --surface: var(--hard);
    }
    svg[data-surface='ARTIFICIAL_GRASS'] {
      --surface: var(--grass);
    }

    .surround {
      fill: color-mix(in srgb, var(--surface) 78%, black);
    }

    .playing-area {
      fill: var(--surface);
    }

    .lines {
      fill: none;
      stroke: var(--line);
      stroke-width: 1.4;
    }

    .net {
      stroke: var(--line);
      stroke-width: 1;
      stroke-dasharray: 2 1.5;
      opacity: 0.85;
    }
  `,
})
export class CourtDiagram {
  readonly surface = input.required<Surface>();
  /** Texto alternativo; vacío cuando el dibujo es decorativo. */
  readonly label = input('');
}
