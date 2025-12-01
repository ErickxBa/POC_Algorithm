/**
 * Utilidades para cálculos de costos
 */

/**
 * Calcula el incremento de riesgo basado en severidad
 * Severidad 1 → +0.5, Severidad 10 → +5.0
 */
export function calculateRiskIncrement(severity: number): number {
  if (severity < 1 || severity > 10) {
    throw new Error('Severity debe estar entre 1 y 10');
  }
  return (severity / 10) * 5;
}

/**
 * Normaliza un valor a una escala 0-10
 */
export function normalizeToScale(value: number, min: number, max: number): number {
  if (max === min) return 0;
  const normalized = ((value - min) / (max - min)) * 10;
  return Math.max(0, Math.min(10, normalized));
}

/**
 * Calcula el costo compuesto de una arista
 */
export function calculateCompositeCost(
  distance: number,
  riskScore: number,
  alpha: number = 0.5,
  beta: number = 0.5
): number {
  const normalizedDistance = Math.min(distance / 1000, 10);
  return alpha * normalizedDistance + beta * riskScore;
}

/**
 * Valida que alpha y beta sean válidos y sumen a 1
 */
export function validateWeights(
  alpha: number,
  beta: number,
  tolerance: number = 0.01
): boolean {
  const sum = alpha + beta;
  return Math.abs(sum - 1.0) < tolerance && alpha >= 0 && beta >= 0;
}

/**
 * Calcula el perfil de seguridad basado en alpha y beta
 */
export function getProfileName(
  alpha: number,
  beta: number
): 'Más Rápido' | 'Balanceado' | 'Más Seguro' {
  if (alpha > 0.6) return 'Más Rápido';
  if (alpha < 0.4) return 'Más Seguro';
  return 'Balanceado';
}
