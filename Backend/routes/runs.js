const router = require('express').Router();
const auth = require('../middleware/auth');
const { Run, User, Op } = require('../models');

// GET /runs?lat=..&lng=..  -> nearby active runs (0.01 degree ~ 1 km box)
router.get('/', auth, async (req, res) => {
  const lat = parseFloat(req.query.lat);
  const lng = parseFloat(req.query.lng);
  if (isNaN(lat) || isNaN(lng)) {
    return res.status(400).json({ error: 'lat/lng required' });
  }

  const runs = await Run.findAll({
    where: {
      active: true,
      expiresAt: { [Op.gt]: new Date() },
      lat: { [Op.between]: [lat - 0.01, lat + 0.01] },
      lng: { [Op.between]: [lng - 0.01, lng + 0.01] },
    },
    include: [
      {
        model: User,
        as: 'runner',
        attributes: ['name'],
      },
    ],
  });

  const response = runs.map((r) => ({
    id: r.id,
    runnerId: r.runnerId,
    storeName: r.storeName,
    lat: r.lat,
    lng: r.lng,
    expiresAt: r.expiresAt,
    runnerName: r.runner ? r.runner.name : null,
  }));

  res.json(response);
});

// GET /runs/mine -> active run of current user (or null)
router.get('/mine', auth, async (req, res) => {
  const run = await Run.findOne({
    where: {
      runnerId: req.uid,
      active: true,
      expiresAt: { [Op.gt]: new Date() },
    },
    attributes: ['id', 'storeName', 'lat', 'lng', 'expiresAt'],
  });
  res.json(run || null);
});

// POST /runs  { storeName, lat, lng }  -> "I'm at the store" button
router.post('/', auth, async (req, res) => {
  const { storeName, lat, lng } = req.body;
  if (!storeName || typeof lat !== 'number' || typeof lng !== 'number') {
    return res.status(400).json({ error: 'storeName, lat, lng required' });
  }

  // Deactivate any currently active run for this user
  await Run.update(
    { active: false },
    { where: { runnerId: req.uid, active: true } }
  );

  const expiresAt = new Date(Date.now() + 30 * 60 * 1000);
  const run = await Run.create({
    runnerId: req.uid,
    storeName,
    lat,
    lng,
    expiresAt,
  });

  res.status(201).json({
    id: run.id,
    storeName: run.storeName,
    lat: run.lat,
    lng: run.lng,
    expiresAt: run.expiresAt,
  });
});

// POST /runs/:id/end -> "I'm leaving the store"
router.post('/:id/end', auth, async (req, res) => {
  const [affected] = await Run.update(
    { active: false },
    { where: { id: req.params.id, runnerId: req.uid } }
  );
  res.sendStatus(affected ? 200 : 404);
});

module.exports = router;
