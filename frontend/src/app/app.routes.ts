import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guard';
import { Admin } from './features/admin/admin';
import { AdminBlocks } from './features/admin/admin-blocks';
import { AdminBookings } from './features/admin/admin-bookings';
import { AdminCourts } from './features/admin/admin-courts';
import { AdminPrices } from './features/admin/admin-prices';
import { Login } from './features/auth/login';
import { Register } from './features/auth/register';
import { MyBookings } from './features/bookings/my-bookings';
import { CourtDetail } from './features/courts/court-detail';
import { CourtList } from './features/courts/court-list';

export const routes: Routes = [
  { path: '', component: CourtList, title: 'Pistas · Pista Libre' },
  { path: 'pistas/:id', component: CourtDetail, title: 'Reservar pista · Pista Libre' },
  { path: 'mis-reservas', component: MyBookings, canActivate: [authGuard], title: 'Mis reservas · Pista Libre' },
  {
    path: 'admin',
    component: Admin,
    canActivate: [adminGuard],
    title: 'Administración · Pista Libre',
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'reservas' },
      { path: 'reservas', component: AdminBookings },
      { path: 'pistas', component: AdminCourts },
      { path: 'tarifas', component: AdminPrices },
      { path: 'bloqueos', component: AdminBlocks },
    ],
  },
  { path: 'login', component: Login, canActivate: [guestGuard], title: 'Iniciar sesión · Pista Libre' },
  { path: 'registro', component: Register, canActivate: [guestGuard], title: 'Crear cuenta · Pista Libre' },
  { path: '**', redirectTo: '' },
];
