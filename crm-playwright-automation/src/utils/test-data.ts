/**
 * Sinh test data unique + traceable — nhìn vào DB/UI biết ngay test nào tạo ra.
 * Format: auto_<testName>_<timestamp>_<random>
 */
export class TestData {
  private static suffix(): string {
    const random = Math.random().toString(36).slice(2, 6);
    return `${Date.now()}_${random}`;
  }

  static email(testName: string): string {
    return `auto_${testName}_${this.suffix()}@auto.test`;
  }

  static username(testName: string): string {
    return `auto_${testName}_${this.suffix()}`;
  }

  static password(): string {
    return `Auto@${this.suffix()}`;
  }

  static code(prefix: string): string {
    return `${prefix}_${this.suffix()}`;
  }
}
