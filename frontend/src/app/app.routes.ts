import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth/auth.guard';
import { Login } from './features/auth/login';
import { Register } from './features/auth/register';
import { MyBookings } from './features/bookings/my-bookings';
import { CourtDetail } from './features/courts/court-detail';
import { CourtList } from './features/courts/court-list';

export const routes: Routes = [
  { path: '', component: CourtList, title: 'Pistas · Pista Libre' },
  { path: 'pistas/:id', component: CourtDetail, title: 'Reservar pista · Pista Libre' },
  { path: 'mis-reservas', component: MyBookings, canActivate: [authGuard], title: 'Mis reservas · Pista Libre' },
  { path: 'login', component: Login, canActivate: [guestGuard], title: 'Iniciar sesión · Pista Libre' },
  { path: 'registro', component: Register, canActivate: [guestGuard], title: 'Crear cuenta · Pista Libre' },
  { path: '**', redirectTo: '' },
];
