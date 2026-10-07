import { HttpErrorResponse } from '@angular/common/http';

/**
 * La API devuelve los errores en formato Problem Details:
 * { status, detail, errors?: { campo: mensaje } }
 */
interface ProblemDetail {
  detail?: string;
  errors?: Record<string, string>;
}

const CONNECTION_ERROR =
  'No se puede conectar con el servidor. Comprueba que la API está arrancada.';

/** Mensaje legible para mostrar al usuario a partir de un error HTTP. */
export function apiErrorMessage(
  error: unknown,
  fallback = 'No se ha podido completar la operación. Inténtalo de nuevo.',
): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 || error.status >= 502) {
      return CONNECTION_ERROR;
    }
    const problem = error.error as ProblemDetail | null;
    if (problem?.detail) {
      return problem.detail;
    }
  }
  return fallback;
}

/** Errores de validación por campo, si la API los ha enviado. */
export function apiFieldErrors(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | null;
    return problem?.errors ?? {};
  }
  return {};
}
