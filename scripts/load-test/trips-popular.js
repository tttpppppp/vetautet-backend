import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiPath = __ENV.API_PATH || '/api/v1/trips/popular?limit=6';
const vus = Number(__ENV.VUS || 50);
const duration = __ENV.DURATION || '30s';
const thinkTimeMs = Number(__ENV.THINK_TIME_MS || 0);

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    checks: ['rate>0.99'],
  },
};

const url = `${baseUrl}${apiPath}`;

export function setup() {
  const warmup = http.get(url);
  check(warmup, {
    'warmup status is 200': (r) => r.status === 200,
  });
}

export default function () {
  const res = http.get(url, {
    headers: {
      Accept: 'application/json',
    },
    tags: {
      name: 'trips-popular',
    },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body is not empty': (r) => !!r.body && r.body.length > 0,
  });

  if (thinkTimeMs > 0) {
    sleep(thinkTimeMs / 1000);
  }
}
