import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // Health check
  let health = http.get(`${BASE_URL}/actuator/health/liveness`);
  check(health, { 'health OK': (r) => r.status === 200 });

  // Register
  let username = `k6_${Date.now()}_${__VU}_${__ITER}`;
  let email = `${username}@test.com`;

  // Send code
  let codeRes = http.post(`${BASE_URL}/api/v1/auth/code/send`, JSON.stringify({
    identifier: email, identityType: 'email', purpose: 'register'
  }), { headers: { 'Content-Type': 'application/json' } });

  // Register (code is '000000' in dev mode - auto-stored)
  let regRes = http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
    username: username, email: email, password: 'Test1234!',
    identityType: 'email', verificationCode: '000000'
  }), { headers: { 'Content-Type': 'application/json' } });

  // Login
  let loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    identifier: email, credential: 'Test1234!'
  }), { headers: { 'Content-Type': 'application/json' } });

  check(loginRes, { 'login OK': (r) => r.status === 200 });

  sleep(1);
}
