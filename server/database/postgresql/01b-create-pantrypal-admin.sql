-- Idempotent admin user creation script for PantryPal (Execution Order: 01b)
-- Mount point: /docker-entrypoint-initdb.d/01b-create-pantrypal-admin.sql
-- Environment variables used: PANTRY_PAL_ADMIN (required), PANTRY_PAL_DB (required), PANTRY_PAL_SCHEMA (required)
-- Secrets required: pantrypal_admin_password
-- Dependencies: Requires database to exist first (runs after 01-create-pantrypal-db.sql)

\set schema `echo ${PANTRY_PAL_SCHEMA}`
\set db `echo ${PANTRY_PAL_DB}`
\set username `echo ${PANTRY_PAL_ADMIN}`

-- Create the admin user if it does not already exist
SELECT 'CREATE USER :' || :username || ' WITH PASSWORD ''' || trim(pg_read_file('/run/secrets/pantrypal_admin_password')) || ''';'
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = :username)
\gexec

-- Grant full database privileges to the admin user
GRANT ALL PRIVILEGES ON DATABASE :db TO :username;

-- Connect to database and grant schema-level permissions
\c :db

-- Grant ownership of configured schema to the admin user
GRANT ALL ON SCHEMA :schema TO :username;

-- Grant read/write/execute privileges on all objects in configured schema
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA :schema TO :username;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA :schema TO :username;

-- Set default privileges for future tables/functions created by the admin user
ALTER DEFAULT PRIVILEGES IN SCHEMA :schema FOR ROLE :username GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :username;
ALTER DEFAULT PRIVILEGES IN SCHEMA :schema FOR ROLE :username GRANT EXECUTE ON FUNCTIONS TO :username;
