// Utilidades de fechas, horas y precios.
// La API trabaja siempre en la hora local del club, así que "hoy" y "ahora"
// se calculan en esa zona horaria y no en la del navegador.

const CLUB_TIME_ZONE = 'Europe/Madrid';
const LOCALE = 'es-ES';

/** Fecha ("AAAA-MM-DD") y hora ("HH:mm") actuales en el club. */
export function clubNow(): { date: string; time: string } {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: CLUB_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date());
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)?.value ?? '';
  return {
    date: `${part('year')}-${part('month')}-${part('day')}`,
    time: `${part('hour')}:${part('minute')}`,
  };
}

/** Suma días a una fecha "AAAA-MM-DD". */
export function addDays(isoDate: string, days: number): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}

/** Da formato a una fecha "AAAA-MM-DD" sin que la zona horaria del navegador la desplace. */
export function formatDate(isoDate: string, options: Intl.DateTimeFormatOptions): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Intl.DateTimeFormat(LOCALE, { ...options, timeZone: 'UTC' }).format(
    new Date(Date.UTC(year, month - 1, day)),
  );
}

/** "viernes, 9 de octubre" */
export function formatLongDate(isoDate: string): string {
  return formatDate(isoDate, { weekday: 'long', day: 'numeric', month: 'long' });
}

/** La API puede enviar "18:00" o "18:00:00"; en pantalla siempre "18:00". */
export function shortTime(time: string): string {
  return time.slice(0, 5);
}

/** Suma minutos a una hora "HH:mm". */
export function addMinutes(time: string, minutes: number): string {
  const [hours, mins] = shortTime(time).split(':').map(Number);
  const total = hours * 60 + mins + minutes;
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${pad(Math.floor(total / 60) % 24)}:${pad(total % 60)}`;
}

const priceFormat = new Intl.NumberFormat(LOCALE, { style: 'currency', currency: 'EUR' });

/** "25,50 €" */
export function formatPrice(amount: number): string {
  return priceFormat.format(amount);
}
