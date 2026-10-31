const { sequelize, Sequelize, Op, User, Run, Request } = require('./models');

// Test connection
sequelize
  .authenticate()
  .then(() => {
    console.log('Database Connected successfully via ORM');
  })
  .catch((err) => {
    console.error('Database connection error:', err.message);
  });

module.exports = {
  sequelize,
  Sequelize,
  Op,
  User,
  Run,
  Request,
};
