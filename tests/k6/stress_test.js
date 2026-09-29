import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics for Grafana dashboard observation
export const failureRate = new Rate('failed_requests');
export const ttfbTrend = new Trend('time_to_first_byte');

export const options = {
  stages: [
    { duration: '20s', target: 20 },   // Ramp up to 20 concurrent VUs
    { duration: '40s', target: 100 },  // Sustained scale test with 100 VUs
    { duration: '20s', target: 200 },  // Peak burst stress test
    { duration: '20s', target: 0 },    // Ramp down to zero
  ],
  thresholds: {
    // 95% of requests must complete below 250ms
    http_req_duration: ['p(95)<250', 'p(99)<600'],
    // Error rate must remain under 1%
    failed_requests: ['rate<0.01'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.TARGET_URL || 'https://ais-dev-ysij5gfggpt2akxlk7vm3b-58584043488.asia-east1.run.app';

export default function () {
  const params = {
    headers: {
      'User-Agent': 'k6-Grafana-VibeSync-LoadTest/1.0',
      'Accept': 'text/html,application/json',
      'Cache-Control': 'max-age=60',
    },
  };

  // 1. App Launch & Screen Render
  const res = http.get(BASE_URL, params);
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response received': (r) => r.body && r.body.length > 0,
    'latency is within acceptable Spark limits': (r) => r.timings.duration < 800,
  });

  failureRate.add(!success);
  ttfbTrend.add(res.timings.waiting);

  // Think time between user interactions (simulates real human browsing)
  sleep(1);

  // 2. Metadata & Manifest Query
  const metaRes = http.get(`${BASE_URL}/metadata.json`, params);
  check(metaRes, {
    'metadata status is 200': (r) => r.status === 200,
  });

  sleep(0.5);
}
