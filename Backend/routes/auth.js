const router = require('express').Router();
const crypto = require('crypto');
const { OAuth2Client } = require('google-auth-library');
const jwt = require('jsonwebtoken');
const { User } = require('../models');

const google = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
const sign = (uid) => jwt.sign({ uid }, process.env.JWT_SECRET, { expiresIn: '7d' });

function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(password, salt, 64).toString('hex');
  return `${salt}:${hash}`;
}

function verifyPassword(password, storedHash) {
  if (!storedHash || !storedHash.includes(':')) return false;
  const [salt, key] = storedHash.split(':');
  const hash = crypto.scryptSync(password, salt, 64).toString('hex');
  return hash === key;
}

async function saveGoogleUser(googleId, name, email) {
  let user = await User.findOne({ where: { googleId } });
  if (user) {
    user.name = name;
    user.email = email;
    await user.save();
  } else {
    user = await User.create({
      googleId,
      name,
      email,
    });
  }

  return {
    id: user.id,
    name: user.name,
    email: user.email,
    phone: user.phone || null,
  };
}

// POST /auth/signup - Custom User Registration
router.post('/signup', async (req, res) => {
  const { name, email, password, phone } = req.body;
  if (!name || !email || !password) {
    return res.status(400).json({ error: 'Name, email, and password are required' });
  }

  const trimmedEmail = email.trim().toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
    return res.status(400).json({ error: 'Invalid email address' });
  }

  if (password.length < 6) {
    return res.status(400).json({ error: 'Password must be at least 6 characters' });
  }

  if (phone && !/^\+?\d{10,13}$/.test(phone.trim())) {
    return res.status(400).json({ error: 'Invalid phone number' });
  }

  const existing = await User.findOne({ where: { email: trimmedEmail } });
  if (existing) {
    return res.status(400).json({ error: 'An account with this email already exists' });
  }

  const passwordHash = hashPassword(password);
  const user = await User.create({
    name: name.trim(),
    email: trimmedEmail,
    passwordHash,
    phone: phone ? phone.trim() : null,
  });

  const userData = {
    id: user.id,
    name: user.name,
    email: user.email,
    phone: user.phone || null,
  };

  res.status(201).json({
    token: sign(user.id),
    user: userData,
  });
});

// POST /auth/login - Custom User Login
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required' });
  }

  const trimmedEmail = email.trim().toLowerCase();
  const user = await User.findOne({ where: { email: trimmedEmail } });
  if (!user || !user.passwordHash || !verifyPassword(password, user.passwordHash)) {
    return res.status(401).json({ error: 'Invalid email or password' });
  }

  const userData = {
    id: user.id,
    name: user.name,
    email: user.email,
    phone: user.phone || null,
  };

  res.json({
    token: sign(user.id),
    user: userData,
  });
});

// POST /auth/google - Google OAuth ID Token Login
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

  const user = await saveGoogleUser(payload.sub, payload.name, payload.email);
  res.json({ token: sign(user.id), user });
});

// POST /auth/dev - Development bypass login
if (process.env.DEV_LOGIN === 'true') {
  router.post('/dev', async (req, res) => {
    const { name, email } = req.body;
    if (!email) return res.status(400).json({ error: 'email required' });
    const user = await saveGoogleUser('dev-' + email, name || email, email);
    res.json({ token: sign(user.id), user });
  });
}

module.exports = router;
