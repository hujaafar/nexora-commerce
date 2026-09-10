/*
 * File purpose: Defines TypeScript contracts for api error data.
 */
export interface ApiError {
  timestamp: string;
  status: number;
  error?: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
  details?: Record<string, string>;
  code: string;
}
