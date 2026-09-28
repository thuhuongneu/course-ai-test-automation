import * as dotenv from 'dotenv';

dotenv.config();

function required(key: string): string {
  const value = process.env[key];
  if (!value) {
    throw new Error(`Thiếu biến môi trường bắt buộc: ${key}. Copy .env.example thành .env rồi điền giá trị.`);
  }
  return value;
}

/** Đọc số nguyên dương; giá trị rỗng hoặc sai định dạng phải fail ngay, không âm thầm thành 0/NaN. */
function positiveInt(key: string, fallback: number): number {
  const raw = process.env[key];
  if (raw === undefined || raw.trim() === '') {
    return fallback;
  }
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`Biến môi trường ${key} phải là số nguyên dương, đang là "${raw}".`);
  }
  return value;
}

/** Cấu hình tập trung — mọi giá trị môi trường đọc qua đây, KHÔNG hardcode trong test. */
export const env = {
  baseURL: required('BASE_URL'),
  username: required('TEST_USERNAME'),
  password: required('TEST_PASSWORD'),
  pmEmail: required('PM_EMAIL'),
  pmPassword: required('PM_PASSWORD'),
  headless: process.env.HEADLESS !== 'false',
  timeout: positiveInt('TIMEOUT', 90_000),
  workers: positiveInt('WORKERS', 5),
} as const;

/** Đường dẫn các trang chính của hệ thống — dùng chung cho Page Object và test. */
export const routes = {
  home: '/',
  login: '/admin/authentication',
  logout: '/admin/authentication/logout',
  dashboardPath: '/admin/',
  /** Dashboard sau khi đăng nhập; chấp nhận cả có và không có dấu / ở cuối */
  dashboard: /\/admin\/?$/,
  customers: /\/admin\/clients/,
} as const;

/** URL tuyệt đối — dùng khi TC yêu cầu thanh địa chỉ dừng ĐÚNG một địa chỉ, không bị đẩy đi đâu khác */
export function absoluteUrl(path: string): string {
  return new URL(path, env.baseURL).href;
}
