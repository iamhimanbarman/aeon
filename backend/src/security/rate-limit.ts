export function authRateLimit(max: number, timeWindow = 60_000) {
  return {
    rateLimit: {
      max,
      timeWindow
    }
  };
}

export const strictAuthRateLimit = authRateLimit(8);
export const normalAuthRateLimit = authRateLimit(20);
export const tokenAuthRateLimit = authRateLimit(30);
