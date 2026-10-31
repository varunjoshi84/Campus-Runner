require('dotenv').config();
require('express-async-errors');
const express = require('express');

const app = express();
app.use(express.json());

app.get('/health', (req, res) => res.json({ ok: true }));
app.use('/auth', require('./routes/auth'));
app.use('/users', require('./routes/users'));
app.use('/runs', require('./routes/runs'));
app.use('/requests', require('./routes/requests'));

app.use((err, req, res, next) => {
  console.error(err);
  if (
    err.code === '22P02' ||
    (err.original && err.original.code === '22P02') ||
    err.name === 'SequelizeDatabaseError' && err.message.includes('invalid input syntax for type uuid')
  ) {
    return res.status(400).json({ error: 'Invalid id' });
  }
  res.status(500).json({ error: 'Server error' });
});

const port = process.env.PORT || 3000;
app.listen(port, '0.0.0.0', () => console.log(`API running on port ${port}`));
