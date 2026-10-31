const router = require('express').Router();
const auth = require('../middleware/auth');
const db = require('../db');

// GET /users/me
router.get('/me', auth, async (req, res) => {
  const { rows } = await db.query(
    'SELECT id, name, email, phone FROM users WHERE id = $1', [req.uid]);
  if (!rows[0]) return res.sendStatus(404);
  res.json(rows[0]);
});

// PATCH /users/me  { phone }   (call feature ke liye number chahiye)
router.patch('/me', auth, async (req, res) => {
  const { phone } = req.body;
  if (!/^\+?\d{10,13}$/.test(phone || '')) {
    return res.status(400).json({ error: 'Invalid phone number' });
  }
  await db.query('UPDATE users SET phone = $1 WHERE id = $2', [phone, req.uid]);
  res.sendStatus(200);
});

module.exports = router;
