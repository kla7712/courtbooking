import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';

import { AuthResponse, User } from '../models';

interface Session {
  token: string;
  /** Momento de caducidad, en milisegundos desde 1970. */
  expiresAt: number;
  user: User;
}

const STORAGE_KEY = 'pistalibre.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // La sesión se guarda en localStorage para que sobreviva a una recarga de la página
  private readonly session = signal<Session | null>(readStoredSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  /** Token vigente, o null si no hay sesión o ya ha caducado. */
  currentToken(): string | null {
    const session = this.session();
    if (!session) {
      return null;
    }
    if (Date.now() >= session.expiresAt) {
      this.clear();
      return null;
    }
    return session.token;
  }

  login(credentials: { email: string; password: string }): Observable<User> {
    return this.http.post<AuthResponse>('/api/auth/login', credentials).pipe(
      tap((response) => this.store(response)),
      map((response) => response.user),
    );
  }

  register(data: { name: string; email: string; password: string }): Observable<User> {
    return this.http.post<AuthResponse>('/api/auth/register', data).pipe(
      tap((response) => this.store(response)),
      map((response) => response.user),
    );
  }

  logout(): void {
    this.clear();
    this.router.navigateByUrl('/');
  }

  /** La API ha rechazado el token: se cierra la sesión y se pide entrar de nuevo. */
  expire(): void {
    this.clear();
    this.router.navigate(['/login'], { queryParams: { caducada: 1 } });
  }

  private store(response: AuthResponse): void {
    const session: Session = {
      token: response.token,
      expiresAt: Date.now() + response.expiresIn * 1000,
      user: response.user,
    };
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Sin acceso a localStorage la sesión sigue funcionando hasta recargar la página
    }
  }

  private clear(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nada que limpiar
    }
  }
}

function readStoredSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    if (!session.token || !session.user || Date.now() >= session.expiresAt) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}
