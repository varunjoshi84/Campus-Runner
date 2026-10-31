const router = require('express').Router();
const { OAuth2Client } = require('google-auth-library');
const jwt = require('jsonwebtoken');
const db = require('../db');

const google = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
const sign = (uid) => jwt.sign({ uid }, process.env.JWT_SECRET, { expiresIn: '7d' });

async function saveUser(googleId, name, email) {
  const { rows } = await db.query(
    `INSERT INTO users (google_id, name, email) VALUES ($1, $2, $3)
     ON CONFLICT (google_id) DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email
     RETURNING id, name, email, phone`,
    [googleId, name, email]
  );
  return rows[0];
}

router.post('/google', async (req, res) => {
  let payload;
  try {
    const ticket = await google.verifyIdToken({
      idToken: req.body.idToken,
      audience: process.env.GOOGLE_CLIENT_ID,
    });
    payload = ticket.getPayload();
  } catch {
    return res.status(401).json({ error: 'Invalid Google token' });
  }
  const user = await saveUser(payload.sub, payload.name, payload.email);
  res.json({ token: sign(user.id), user });
});


if (process.env.DEV_LOGIN === 'true') {
  router.post('/dev', async (req, res) => {
    const { name, email } = req.body;
    if (!email) return res.status(400).json({ error: 'email required' });
    const user = await saveUser('dev-' + email, name || email, email);
    res.json({ token: sign(user.id), user });
  });
}

module.exports = router;
