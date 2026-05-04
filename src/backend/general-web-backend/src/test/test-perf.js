import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom metrics
const loginDuration = new Trend('login_duration');
const tokenValidationDuration = new Trend('token_validation_duration');
const errorRate = new Rate('errors');

// Test user credentials (pre-seeded)
const TEST_EMAIL = __ENV.TEST_EMAIL || 'user@test.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'User1234!';

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '1m',
      tags: { scenario: 'smoke' },
    },
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 10 },
        { duration: '3m', target: 50 },
        { duration: '1m', target: 0 },
      ],
      tags: { scenario: 'load' },
      startTime: '1m',
    },
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '1m', target: 200 },
        { duration: '30s', target: 0 },
      ],
      tags: { scenario: 'stress' },
      startTime: '6m',
    },
    soak: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30m',
      tags: { scenario: 'soak' },
      startTime: '9m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    login_duration: ['p(95)<500'],
  },
};

export default function () {
  // 1. Health check
  let healthRes = http.get(`${BASE_URL}/actuator/health/liveness`);
  check(healthRes, { 'health OK': (r) => r.status === 200 });
  errorRate.add(healthRes.status !== 200);

  // 2. Login
  let loginStart = Date.now();
  let loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    identifier: TEST_EMAIL,
    credential: TEST_PASSWORD,
    captchaOutput: 'dev-bypass',
    lotNumber: 'dev',
    passToken: 'dev',
    genTime: String(Math.floor(Date.now() / 1000)),
  }), { headers: { 'Content-Type': 'application/json' } });
  loginDuration.add(Date.now() - loginStart);

  let loggedIn = loginRes.status === 200;
  check(loginRes, { 'login OK': (r) => r.status === 200 || r.status === 401 || r.status === 429 });
  errorRate.add(loginRes.status !== 200 && loginRes.status !== 401 && loginRes.status !== 429);

  if (loggedIn) {
    let data = JSON.parse(loginRes.body);
    let token = data.data.accessToken;

    // 3. Token validate
    let validateStart = Date.now();
    let validateRes = http.get(`${BASE_URL}/api/v1/auth/token/validate`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    tokenValidationDuration.add(Date.now() - validateStart);
    check(validateRes, { 'token valid': (r) => r.status === 200 });

    // 4. Get current user
    let meRes = http.get(`${BASE_URL}/api/v1/users/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    check(meRes, { 'me OK': (r) => r.status === 200 });

    // 5. Get roles (public)
    let rolesRes = http.get(`${BASE_URL}/api/v1/roles`);
    check(rolesRes, { 'roles OK': (r) => r.status === 401 || r.status === 403 || r.status === 200 });
  }

  sleep(1);
}

// Summary output at end
export function handleSummary(data) {
  return {
    'stdout': JSON.stringify({
      total_requests: data.metrics.http_reqs.values.count,
      failed_rate: data.metrics.http_req_failed.values.rate,
      p95_duration: data.metrics.http_req_duration.values['p(95)'],
      p99_duration: data.metrics.http_req_duration.values['p(99)'],
      login_p95: data.metrics.login_duration ? data.metrics.login_duration.values['p(95)'] : null,
    }, null, 2),
  };
}
