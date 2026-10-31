const router = require('express').Router();
const auth = require('../middleware/auth');
const { User } = require('../models');

// GET /users/me
router.get('/me', auth, async (req, res) => {
  const user = await User.findByPk(req.uid, {
    attributes: ['id', 'name', 'email', 'phone'],
  });
  if (!user) return res.sendStatus(404);
  res.json(user);
});

// PATCH /users/me  { phone }   (call feature ke liye number chahiye)
router.patch('/me', auth, async (req, res) => {
  const { phone } = req.body;
  if (!/^\+?\d{10,13}$/.test(phone || '')) {
    return res.status(400).json({ error: 'Invalid phone number' });
  }

  const [affected] = await User.update(
    { phone },
    { where: { id: req.uid } }
  );
  if (!affected) return res.sendStatus(404);

  res.sendStatus(200);
});

module.exports = router;
