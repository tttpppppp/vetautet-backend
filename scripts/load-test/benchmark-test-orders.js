import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiPath = __ENV.API_PATH || __ENV.ENDPOINT || '/api/v1/benchmark/test-orders/async';
const resetPath = __ENV.RESET_PATH || '/api/v1/benchmark/test-orders/reset';
const statsPath = __ENV.STATS_PATH || '/api/v1/benchmark/test-orders/stats';
const preparePath = __ENV.PREPARE_PATH || '/api/v1/benchmark/test-orders/prepare';

const vus = Number(__ENV.VUS || 20);
const duration = __ENV.DURATION || '30s';
const totalUsers = Number(__ENV.TOTAL_USERS || 0);
const thinkTimeMs = Number(__ENV.THINK_TIME_MS || 0);
const amount = Number(__ENV.AMOUNT || 100000);
const quantity = Number(__ENV.QUANTITY || 1);
const userBase = Number(__ENV.USER_BASE || 1000);
const ticketRef = __ENV.TICKET_REF ? Number(__ENV.TICKET_REF) : 23;
const stock = Number(__ENV.STOCK || 1000);

const successCounter = new Counter('orders_success');
const outOfStockCounter = new Counter('orders_out_of_stock');
const errorCounter = new Counter('orders_error');

function buildOptions() {
  if (totalUsers > 0) {
    return {
      scenarios: {
        flash_rush: {
          executor: 'shared-iterations',
          vus,
          iterations: totalUsers,
          maxDuration: __ENV.MAX_DURATION || '2m',
          gracefulStop: __ENV.GRACEFUL_STOP || '30s',
        },
      },
      thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<800', 'p(99)<1500'],
        checks: ['rate>0.99'],
      },
    };
  }

  return {
    vus,
    duration,
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<800', 'p(99)<1500'],
      checks: ['rate>0.99'],
    },
  };
}

export const options = buildOptions();

export function setup() {
  const resetRes = http.del(`${baseUrl}${resetPath}?ticketRef=${ticketRef}`);
  const prepareRes = http.post(`${baseUrl}${preparePath}?ticketRef=${ticketRef}&stock=${stock}`, null);
  console.log(`[SETUP OK] ticketRef=${ticketRef} | stock=${stock} | endpoint=${apiPath} | vus=${vus} | totalUsers=${totalUsers || 'duration-mode'} | reset=${resetRes.status} | prepare=${prepareRes.status}`);
  return {
    startedAt: Date.now(),
  };
}

export default function () {
  const userRef = userBase + (__VU % 1000);
  const body = JSON.stringify({
    userRef,
    ticketRef,
    quantity,
    amount,
    note: `k6-vu-${__VU}-iter-${__ITER}`,
  });

  const res = http.post(`${baseUrl}${apiPath}`, body, {
    headers: { 'Content-Type': 'application/json' },
  });

  const ok = check(res, {
    'http 200': (r) => r.status === 200,
    'business response present': (r) => {
      try {
        return r.json('success') !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  let responseCode = 'UNKNOWN';
  let success = false;
  try {
    success = !!res.json('success');
    responseCode = res.json('code') || 'UNKNOWN';
  } catch (e) {
    responseCode = 'PARSE_ERROR';
  }

  if (res.status === 0 || !ok) {
    errorCounter.add(1);
  } else if (success) {
    successCounter.add(1);
  } else if (responseCode === 'OUT_OF_STOCK') {
    outOfStockCounter.add(1);
  } else {
    errorCounter.add(1);
  }

  if (thinkTimeMs > 0) {
    sleep(thinkTimeMs / 1000);
  }
}

export function teardown() {
  let finalStats = null;
  for (let i = 0; i < 20; i++) {
    const res = http.get(`${baseUrl}${statsPath}?ticketRef=${ticketRef}`);
    if (res.status === 200) {
      finalStats = res.json();
      if (totalUsers <= 0 || (finalStats.createdOrders + finalStats.failedOrders) >= Math.min(stock, totalUsers)) {
        break;
      }
    }
    sleep(1);
  }

  if (finalStats) {
    console.log('');
    console.log('ASYNC DB CHECK');
    console.log(`Queue rows     : ${finalStats.totalOrders}`);
    console.log(`Created rows   : ${finalStats.createdOrders}`);
    console.log(`Pending rows   : ${finalStats.pendingOrders}`);
    console.log(`Failed rows    : ${finalStats.failedOrders}`);
    console.log(`Remaining stock: ${finalStats.remainingStock}`);
    console.log(`Latest at      : ${finalStats.latestProcessedAt || 'n/a'}`);
    console.log('');
  } else {
    console.log('[TEARDOWN] Unable to fetch benchmark stats.');
  }
}

function metricValue(metric, field, fallback = 0) {
  if (!metric || !metric.values || metric.values[field] === undefined || metric.values[field] === null) {
    return fallback;
  }
  return metric.values[field];
}

function formatNumber(value) {
  return Number(value || 0).toFixed(2);
}

function formatInt(value) {
  return Math.round(Number(value || 0)).toString();
}

function summaryBox(data) {
  const httpReqs = metricValue(data.metrics.http_reqs, 'count');
  const throughput = metricValue(data.metrics.http_reqs, 'rate');
  const p95 = metricValue(data.metrics.http_req_duration, 'p(95)');
  const p99 = metricValue(data.metrics.http_req_duration, 'p(99)');
  const success = metricValue(data.metrics.orders_success, 'count');
  const outOfStock = metricValue(data.metrics.orders_out_of_stock, 'count');
  const requestErrors = metricValue(data.metrics.orders_error, 'count');

  const title = totalUsers > 0 ? 'FLASH SALE BENCHMARK - RESULT' : 'ASYNC ORDER BENCHMARK - RESULT';
  const oversellOk = success <= stock;

  return [
    '',
    '==============================================',
    ` ${title}`,
    '==============================================',
    ` Endpoint         : ${apiPath}`,
    ` Ticket Ref       : ${ticketRef ?? 'n/a'}`,
    ` Stock            : ${stock}`,
    ` VUs              : ${vus}`,
    ` Total Requests   : ${totalUsers > 0 ? totalUsers : formatInt(httpReqs)}`,
    '----------------------------------------------',
    ` Đặt thành công   : ${formatInt(success)}`,
    ` Hết vé           : ${formatInt(outOfStock)}`,
    ` Lỗi server       : ${formatInt(requestErrors)}`,
    ` Tổng xử lý       : ${formatInt(httpReqs)}`,
    '----------------------------------------------',
    ` Throughput (RPS) : ${formatNumber(throughput)}`,
    ` Latency p95 (ms) : ${formatNumber(p95)}`,
    ` Latency p99 (ms) : ${formatNumber(p99)}`,
    '----------------------------------------------',
    ` Oversell Check   : ${oversellOk ? 'OK' : 'FAILED'} (${formatInt(success)} <= ${stock})`,
    '==============================================',
    '',
  ].join('\n');
}

export function handleSummary(data) {
  return {
    stdout: summaryBox(data),
  };
}
