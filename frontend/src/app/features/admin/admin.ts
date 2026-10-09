import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** Marco del panel de administración: título, pestañas y la sección elegida. */
@Component({
  selector: 'app-admin',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="page">
      <h1 class="admin-title">Administración</h1>

      <nav class="tabs" aria-label="Secciones de administración">
        <a routerLink="pistas" routerLinkActive="is-active">Pistas</a>
        <a routerLink="tarifas" routerLinkActive="is-active">Tarifas</a>
        <a routerLink="bloqueos" routerLinkActive="is-active">Bloqueos</a>
      </nav>

      <router-outlet />
    </div>
  `,
  styleUrl: './admin.scss',
})
export class Admin {}
