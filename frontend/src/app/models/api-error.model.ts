/* BUY-01 learning header
 * File purpose: Defines TypeScript contracts for api error data.
 * Learning focus: End-to-end type safety between Angular and backend DTOs.
 */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
