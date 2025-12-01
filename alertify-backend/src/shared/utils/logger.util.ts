/**
 * Utilidades para logging y debugging
 */

import { Logger } from '@nestjs/common';

export class LoggerUtil {
  static log(context: string, message: string, data?: any): void {
    const logger = new Logger(context);
    const logMessage = data ? `${message} ${JSON.stringify(data)}` : message;
    logger.log(logMessage);
  }

  static error(context: string, message: string, error?: any): void {
    const logger = new Logger(context);
    if (error instanceof Error) {
      logger.error(`${message}: ${error.message}`, error.stack);
    } else {
      logger.error(`${message}`, error);
    }
  }

  static warn(context: string, message: string, data?: any): void {
    const logger = new Logger(context);
    const logMessage = data ? `${message} ${JSON.stringify(data)}` : message;
    logger.warn(logMessage);
  }

  static debug(context: string, message: string, data?: any): void {
    const logger = new Logger(context);
    if (process.env.LOG_LEVEL === 'debug') {
      const logMessage = data ? `${message} ${JSON.stringify(data, null, 2)}` : message;
      logger.debug(logMessage);
    }
  }
}

/**
 * Formatea un objeto para logging
 */
export function formatLog(obj: any): string {
  return JSON.stringify(obj, null, 2);
}

/**
 * Calcula el tiempo de ejecución
 */
export function measureTime(label: string, fn: () => void): number {
  const start = performance.now();
  fn();
  const end = performance.now();
  const duration = end - start;
  console.log(`[${label}] Tiempo: ${duration.toFixed(2)}ms`);
  return duration;
}
