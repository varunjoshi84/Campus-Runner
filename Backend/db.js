const { Pool } = require('pg');
const pool = new Pool({ connectionString: process.env.DATABASE_URL });
console.log("Database Connected");
const toCamel = (s) => s.replace(/_([a-z])/g, (_, c) => c.toUpperCase());
async function query(text, params) {
  const result = await pool.query(text, params);
  result.rows = result.rows.map((row) =>
    Object.fromEntries(Object.entries(row).map(([k, v]) => [toCamel(k), v]))
  );
  return result;
}

module.exports = { query };
