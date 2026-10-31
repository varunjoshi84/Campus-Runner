const router = require('express').Router();
const auth = require('../middleware/auth');
const { Request, Run, User, Op } = require('../models');

const STATUSES = ['ACCEPTED', 'BOUGHT', 'DELIVERED', 'CANCELLED'];

// POST /requests  { runId, item, tip }
router.post('/', auth, async (req, res) => {
  const { runId, item, tip } = req.body;
  if (!runId || !item || !Number.isInteger(tip) || tip < 0) {
    return res.status(400).json({ error: 'runId, item, tip required' });
  }

  // Ensure target run exists, is active, not expired, and runner is not self
  const run = await Run.findOne({
    where: {
      id: runId,
      active: true,
      expiresAt: { [Op.gt]: new Date() },
      runnerId: { [Op.ne]: req.uid },
    },
  });

  if (!run) {
    return res.status(400).json({ error: 'Run not available' });
  }

  const request = await Request.create({
    runId,
    requesterId: req.uid,
    item,
    tip,
    status: 'OPEN',
  });

  res.status(201).json(request);
});

// GET /requests/incoming -> requests for the runner's active runs
router.get('/incoming', auth, async (req, res) => {
  const requests = await Request.findAll({
    include: [
      {
        model: Run,
        as: 'run',
        where: { runnerId: req.uid },
        attributes: ['storeName'],
      },
      {
        model: User,
        as: 'requester',
        attributes: ['name', 'phone'],
      },
    ],
    order: [['createdAt', 'DESC']],
  });

  const response = requests.map((q) => ({
    id: q.id,
    runId: q.runId,
    item: q.item,
    tip: q.tip,
    status: q.status,
    createdAt: q.createdAt,
    storeName: q.run ? q.run.storeName : null,
    requesterName: q.requester ? q.requester.name : null,
    requesterPhone: q.requester ? q.requester.phone : null,
  }));

  res.json(response);
});

// GET /requests/mine -> requests made by the current user
router.get('/mine', auth, async (req, res) => {
  const requests = await Request.findAll({
    where: { requesterId: req.uid },
    include: [
      {
        model: Run,
        as: 'run',
        attributes: ['storeName'],
        include: [
          {
            model: User,
            as: 'runner',
            attributes: ['name', 'phone'],
          },
        ],
      },
    ],
    order: [['createdAt', 'DESC']],
  });

  const response = requests.map((q) => ({
    id: q.id,
    runId: q.runId,
    item: q.item,
    tip: q.tip,
    status: q.status,
    createdAt: q.createdAt,
    storeName: q.run ? q.run.storeName : null,
    runnerName: q.run && q.run.runner ? q.run.runner.name : null,
    runnerPhone:
      q.status !== 'OPEN' && q.run && q.run.runner
        ? q.run.runner.phone
        : null,
  }));

  res.json(response);
});

// PATCH /requests/:id  { status }
router.patch('/:id', auth, async (req, res) => {
  const { status } = req.body;
  if (!STATUSES.includes(status)) {
    return res.status(400).json({ error: 'Invalid status' });
  }

  const request = await Request.findByPk(req.params.id, {
    include: [{ model: Run, as: 'run', attributes: ['runnerId'] }],
  });

  if (!request) {
    return res.status(403).json({ error: 'Not allowed' });
  }

  const isRunner = request.run && request.run.runnerId === req.uid;
  const isRequester = request.requesterId === req.uid;

  const canUpdate = status === 'CANCELLED' ? (isRequester || isRunner) : isRunner;
  if (!canUpdate) {
    return res.status(403).json({ error: 'Not allowed' });
  }

  request.status = status;
  await request.save();

  res.json({
    id: request.id,
    runId: request.runId,
    requesterId: request.requesterId,
    item: request.item,
    tip: request.tip,
    status: request.status,
    createdAt: request.createdAt,
  });
});

module.exports = router;
