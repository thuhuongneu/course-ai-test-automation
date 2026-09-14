import { createLogger, format, transports } from 'winston';

/**
 * Logger dùng chung — thay cho console.log (anti-pattern).
 * Mỗi worker Playwright là một process riêng nên ghi ra file riêng theo chỉ số worker,
 * tránh các dòng log của 5 luồng xen vào nhau.
 */
const workerIndex = process.env.TEST_PARALLEL_INDEX ?? '0';

export const logger = createLogger({
  level: process.env.LOG_LEVEL ?? 'info',
  format: format.combine(
    format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    format.printf(({ timestamp, level, message }) => `${timestamp} [${level.toUpperCase()}] ${message}`),
  ),
  transports: [
    new transports.File({ filename: `reports/logs/test-execution-worker-${workerIndex}.log` }),
  ],
});
