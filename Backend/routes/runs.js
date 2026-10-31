const router = require('express').Router();
const auth = require('../middleware/auth');
const db = require('../db');

// GET /runs?lat=..&lng=..  -> nearby active runs (0.01 degree ~ 1 km box)
router.get('/', auth, async (req, res) => {
  const lat = parseFloat(req.query.lat);
  const lng = parseFloat(req.query.lng);
  if (isNaN(lat) || isNaN(lng)) return res.status(400).json({ error: 'lat/lng required' });

  const { rows } = await db.query(
    `SELECT r.id, r.runner_id, r.store_name, r.lat, r.lng, r.expires_at,
            u.name AS runner_name
     FROM runs r JOIN users u ON u.id = r.runner_id
     WHERE r.active AND r.expires_at > now()
       AND abs(r.lat - $1) < 0.01 AND abs(r.lng - $2) < 0.01`,
    [lat, lng]
  );
  res.json(rows);
});

// GET /runs/mine -> meri abhi ki active run (ya null)
router.get('/mine', auth, async (req, res) => {
  const { rows } = await db.query(
    `SELECT id, store_name, lat, lng, expires_at FROM runs
     WHERE runner_id = $1 AND active AND expires_at > now()`, [req.uid]);
  res.json(rows[0] || null);
});

// POST /runs  { storeName, lat, lng }  -> "I'm at the store" button
router.post('/', auth, async (req, res) => {
  const { storeName, lat, lng } = req.body;
  if (!storeName || typeof lat !== 'number' || typeof lng !== 'number') {
    return res.status(400).json({ error: 'storeName, lat, lng required' });
  }
  // ek user ki ek hi active run
  await db.query('UPDATE runs SET active = false WHERE runner_id = $1 AND active', [req.uid]);
  const { rows } = await db.query(
    `INSERT INTO runs (runner_id, store_name, lat, lng, expires_at)
     VALUES ($1, $2, $3, $4, now() + interval '30 minutes')
     RETURNING id, store_name, lat, lng, expires_at`,
    [req.uid, storeName, lat, lng]
  );
  res.status(201).json(rows[0]);
});

// POST /runs/:id/end -> "I'm leaving the store"
router.post('/:id/end', auth, async (req, res) => {
  const { rowCount } = await db.query(
    'UPDATE runs SET active = false WHERE id = $1 AND runner_id = $2', [req.params.id, req.uid]);
  res.sendStatus(rowCount ? 200 : 404);
});

module.exports = router;
