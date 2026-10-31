const router = require('express').Router();
const auth = require('../middleware/auth');
const db = require('../db');

const STATUSES = ['ACCEPTED', 'BOUGHT', 'DELIVERED', 'CANCELLED'];

// POST /requests  { runId, item, tip }
router.post('/', auth, async (req, res) => {
  const { runId, item, tip } = req.body;
  if (!runId || !item || !Number.isInteger(tip) || tip < 0) {
    return res.status(400).json({ error: 'runId, item, tip required' });
  }

  const { rows } = await db.query(
    `INSERT INTO requests (run_id, requester_id, item, tip)
     SELECT r.id, $2, $3, $4 FROM runs r
     WHERE r.id = $1 AND r.active AND r.expires_at > now() AND r.runner_id <> $2
     RETURNING *`,
    [runId, req.uid, item, tip]
  );
  if (!rows[0]) return res.status(400).json({ error: 'Run not available' });
  res.status(201).json(rows[0]);
});


router.get('/incoming', auth, async (req, res) => {
  const { rows } = await db.query(
    `SELECT q.id, q.run_id, q.item, q.tip, q.status, q.created_at,
            r.store_name, u.name AS requester_name, u.phone AS requester_phone
     FROM requests q
     JOIN runs r  ON r.id = q.run_id
     JOIN users u ON u.id = q.requester_id
     WHERE r.runner_id = $1
     ORDER BY q.created_at DESC`, [req.uid]);
  res.json(rows);
});

router.get('/mine', auth, async (req, res) => {
  const { rows } = await db.query(
    `SELECT q.id, q.run_id, q.item, q.tip, q.status, q.created_at,
            r.store_name, u.name AS runner_name,
            CASE WHEN q.status <> 'OPEN' THEN u.phone END AS runner_phone
     FROM requests q
     JOIN runs r  ON r.id = q.run_id
     JOIN users u ON u.id = r.runner_id
     WHERE q.requester_id = $1
     ORDER BY q.created_at DESC`, [req.uid]);
  res.json(rows);
});


router.patch('/:id', auth, async (req, res) => {
  const { status } = req.body;
  if (!STATUSES.includes(status)) return res.status(400).json({ error: 'Invalid status' });

  const runnerOwns = 'run_id IN (SELECT id FROM runs WHERE runner_id = $3)';
  const who = status === 'CANCELLED' ? `(requester_id = $3 OR ${runnerOwns})` : runnerOwns;

  const { rows } = await db.query(
    `UPDATE requests SET status = $1 WHERE id = $2 AND ${who} RETURNING *`,
    [status, req.params.id, req.uid]
  );
  if (!rows[0]) return res.status(403).json({ error: 'Not allowed' });
  res.json(rows[0]);
});

module.exports = router;
