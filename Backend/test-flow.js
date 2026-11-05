// Poore backend ka automatic test. Server chalu rakhiye (npm start), phir: node test-flow.js
// Node 18+ chahiye. Server .env mein DEV_LOGIN=true hona chahiye.
const BASE = process.env.BASE_URL || 'http://localhost:3000';
let passed = 0, failed = 0;

async function call(method, path, token, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token && { Authorization: 'Bearer ' + token }) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let data = null;
  try { data = JSON.parse(text); } catch {}
  return { status: res.status, data };
}

function check(name, condition) {
  console.log((condition ? 'PASS  ' : 'FAIL  ') + name);
  condition ? passed++ : failed++;
}

(async () => {
  const id = Date.now();
  const lat = 31.256, lng = 75.705;

  check('health', (await call('GET', '/health')).status === 200);
  check('no token -> 401', (await call('GET', '/users/me')).status === 401);

  // Custom Signup & Login test
  const customEmail = `user${id}@campus.edu`;
  const signupRes = await call('POST', '/auth/signup', null, { name: 'Custom User', email: customEmail, password: 'password123', phone: '9876543210' });
  check('custom signup -> 201', signupRes.status === 201 && signupRes.data?.token);

  const loginRes = await call('POST', '/auth/login', null, { email: customEmail, password: 'password123' });
  check('custom login -> 200', loginRes.status === 200 && loginRes.data?.user?.email === customEmail);

  const badLoginRes = await call('POST', '/auth/login', null, { email: customEmail, password: 'wrongpassword' });
  check('wrong password -> 401', badLoginRes.status === 401);

  // Do users: A = runner, B = requester
  const a = (await call('POST', '/auth/dev', null, { name: 'A Runner', email: `a${id}@x.com` })).data;
  const b = (await call('POST', '/auth/dev', null, { name: 'B Buyer', email: `b${id}@x.com` })).data;
  check('dev login A and B', a?.token && b?.token);

  check('GET /users/me', (await call('GET', '/users/me', a.token)).data?.name === 'A Runner');
  check('invalid phone -> 400', (await call('PATCH', '/users/me', a.token, { phone: '123' })).status === 400);
  check('valid phone saved', (await call('PATCH', '/users/me', a.token, { phone: '9876543210' })).status === 200);

  // Run
  const run = await call('POST', '/runs', a.token, { storeName: 'Campus Mart', lat, lng });
  check('A starts run -> 201', run.status === 201 && run.data?.storeName === 'Campus Mart');
  check('GET /runs/mine', (await call('GET', '/runs/mine', a.token)).data?.id === run.data.id);
  const near = await call('GET', `/runs?lat=${lat + 0.001}&lng=${lng + 0.001}`, b.token);
  check('B sees nearby run', near.data?.some((r) => r.id === run.data.id));
  const far = await call('GET', `/runs?lat=${lat + 1}&lng=${lng + 1}`, b.token);
  check('far location sees nothing', far.data?.length === 0);
  check('missing lat/lng -> 400', (await call('GET', '/runs', b.token)).status === 400);

  // Requests
  check('A cannot request own run', (await call('POST', '/requests', a.token, { runId: run.data.id, item: 'x', tip: 5 })).status === 400);
  check('bad tip -> 400', (await call('POST', '/requests', b.token, { runId: run.data.id, item: 'Maggi', tip: -1 })).status === 400);
  check('bad uuid -> 400', (await call('POST', '/requests', b.token, { runId: 'abc', item: 'Maggi', tip: 10 })).status === 400);
  const req = await call('POST', '/requests', b.token, { runId: run.data.id, item: 'Maggi', tip: 10 });
  check('B creates request -> 201', req.status === 201 && req.data?.status === 'OPEN');

  const incoming = await call('GET', '/requests/incoming', a.token);
  check('A sees incoming request', incoming.data?.length === 1 && incoming.data[0].requesterName === 'B Buyer');

  let mine = await call('GET', '/requests/mine', b.token);
  check('B mine: OPEN, runner phone hidden', mine.data?.[0]?.status === 'OPEN' && mine.data[0].runnerPhone === null);

  // Status changes
  check('B cannot ACCEPT -> 403', (await call('PATCH', `/requests/${req.data.id}`, b.token, { status: 'ACCEPTED' })).status === 403);
  check('invalid status -> 400', (await call('PATCH', `/requests/${req.data.id}`, a.token, { status: 'HACKED' })).status === 400);
  check('A accepts', (await call('PATCH', `/requests/${req.data.id}`, a.token, { status: 'ACCEPTED' })).data?.status === 'ACCEPTED');

  mine = await call('GET', '/requests/mine', b.token);
  check('B mine: ACCEPTED, runner phone visible', mine.data?.[0]?.status === 'ACCEPTED' && mine.data[0].runnerPhone === '9876543210');

  check('A marks BOUGHT', (await call('PATCH', `/requests/${req.data.id}`, a.token, { status: 'BOUGHT' })).data?.status === 'BOUGHT');
  check('A marks DELIVERED', (await call('PATCH', `/requests/${req.data.id}`, a.token, { status: 'DELIVERED' })).data?.status === 'DELIVERED');

  // Requester cancel
  const req2 = await call('POST', '/requests', b.token, { runId: run.data.id, item: 'Pen', tip: 5 });
  check('B can cancel own request', (await call('PATCH', `/requests/${req2.data.id}`, b.token, { status: 'CANCELLED' })).data?.status === 'CANCELLED');

  // End run
  check('A ends run', (await call('POST', `/runs/${run.data.id}/end`, a.token)).status === 200);
  check('request on ended run -> 400', (await call('POST', '/requests', b.token, { runId: run.data.id, item: 'Pen', tip: 5 })).status === 400);
  check('ended run not nearby', !(await call('GET', `/runs?lat=${lat}&lng=${lng}`, b.token)).data?.some((r) => r.id === run.data.id));

  console.log(`\n${passed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error('Test crashed (server chalu hai?):', e.message); process.exit(1); });