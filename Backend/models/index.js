const { Sequelize, DataTypes, Op } = require('sequelize');

const databaseUrl = process.env.DATABASE_URL;
if (!databaseUrl) {
  console.warn('DATABASE_URL is not defined in environment variables.');
}

const isSupabase = databaseUrl && databaseUrl.includes('supabase.com');

const sequelize = new Sequelize(databaseUrl, {
  dialect: 'postgres',
  dialectOptions: isSupabase
    ? {
        ssl: {
          require: true,
          rejectUnauthorized: false,
        },
      }
    : {},
  logging: process.env.NODE_ENV === 'development' ? console.log : false,
  define: {
    timestamps: false,
    underscored: true,
    freezeTableName: true,
  },
});

// User Model
const User = sequelize.define('users', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  googleId: {
    type: DataTypes.TEXT,
    field: 'google_id',
    unique: true,
    allowNull: false,
  },
  name: {
    type: DataTypes.TEXT,
  },
  email: {
    type: DataTypes.TEXT,
  },
  phone: {
    type: DataTypes.TEXT,
  },
  createdAt: {
    type: DataTypes.DATE,
    field: 'created_at',
    defaultValue: DataTypes.NOW,
  },
});

// Run Model
const Run = sequelize.define('runs', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  runnerId: {
    type: DataTypes.UUID,
    field: 'runner_id',
    allowNull: false,
    references: {
      model: 'users',
      key: 'id',
    },
  },
  storeName: {
    type: DataTypes.TEXT,
    field: 'store_name',
    allowNull: false,
  },
  lat: {
    type: DataTypes.DOUBLE,
    allowNull: false,
  },
  lng: {
    type: DataTypes.DOUBLE,
    allowNull: false,
  },
  active: {
    type: DataTypes.BOOLEAN,
    defaultValue: true,
  },
  expiresAt: {
    type: DataTypes.DATE,
    field: 'expires_at',
    allowNull: false,
  },
  createdAt: {
    type: DataTypes.DATE,
    field: 'created_at',
    defaultValue: DataTypes.NOW,
  },
});

// Request Model
const Request = sequelize.define('requests', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  runId: {
    type: DataTypes.UUID,
    field: 'run_id',
    allowNull: false,
    references: {
      model: 'runs',
      key: 'id',
    },
  },
  requesterId: {
    type: DataTypes.UUID,
    field: 'requester_id',
    allowNull: false,
    references: {
      model: 'users',
      key: 'id',
    },
  },
  item: {
    type: DataTypes.TEXT,
    allowNull: false,
  },
  tip: {
    type: DataTypes.INTEGER,
    allowNull: false,
    validate: {
      min: 0,
    },
  },
  status: {
    type: DataTypes.ENUM('OPEN', 'ACCEPTED', 'BOUGHT', 'DELIVERED', 'CANCELLED'),
    defaultValue: 'OPEN',
    allowNull: false,
  },
  createdAt: {
    type: DataTypes.DATE,
    field: 'created_at',
    defaultValue: DataTypes.NOW,
  },
});

// Associations
User.hasMany(Run, { foreignKey: 'runnerId', as: 'runs' });
Run.belongsTo(User, { foreignKey: 'runnerId', as: 'runner' });

Run.hasMany(Request, { foreignKey: 'runId', as: 'requests' });
Request.belongsTo(Run, { foreignKey: 'runId', as: 'run' });

User.hasMany(Request, { foreignKey: 'requesterId', as: 'sentRequests' });
Request.belongsTo(User, { foreignKey: 'requesterId', as: 'requester' });

module.exports = {
  sequelize,
  Sequelize,
  Op,
  User,
  Run,
  Request,
};

