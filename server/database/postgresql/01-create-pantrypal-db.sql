-- Idempotent database creation script for PantryPal (Execution Order: 01)
-- Mount point: /docker-entrypoint-initdb.d/01-create-pantrypal-db.sql
-- Environment variables used: PANTRY_PAL_DB (required)
-- Dependencies: None — runs first before user creation

\set db `echo ${PANTRY_PAL_DB}`

-- Create the database if it does not already exist
SELECT 'CREATE DATABASE :' || :db;
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :db)
\gexec
