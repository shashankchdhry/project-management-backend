// k6 load test — 100 concurrent board viewers.
//
//   k6 run load-test/board-load.js
//   k6 run -e BASE_URL=https://your-host load-test/board-load.js
//
// Logs in once as the seeded demo member, then hammers the board read endpoint with 100 VUs.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    board_viewers: { executor: 'constant-vus', vus: 100, duration: '30s' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],       // <1% errors
    http_req_duration: ['p(95)<500'],     // p95 under 500ms
  },
};

// Runs once; the returned token is shared with every VU.
export function setup() {
  const res = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ email: 'member@demo.test', password: 'password' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  return { token: res.json('token') };
}

export default function (data) {
  const params = { headers: { Authorization: `Bearer ${data.token}` } };
  const res = http.get(`${BASE}/api/v1/projects/WEB/board`, params);
  check(res, {
    'board 200': (r) => r.status === 200,
    'has columns': (r) => Array.isArray(r.json('columns')),
  });
  sleep(1);
}
