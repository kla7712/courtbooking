// Tipos que devuelve la API. Reflejan los DTO del back.

export type Role = 'MEMBER' | 'ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  /** Segundos de validez del token. */
  expiresIn: number;
  user: User;
}

export type Surface = 'CLAY' | 'HARD' | 'ARTIFICIAL_GRASS';

export interface Court {
  id: number;
  name: string;
  surface: Surface;
  indoor: boolean;
  hasLighting: boolean;
  active: boolean;
}

export const SURFACE_LABELS: Record<Surface, string> = {
  CLAY: 'Tierra batida',
  HARD: 'Pista dura',
  ARTIFICIAL_GRASS: 'Césped artificial',
};
