#!/usr/bin/env bash
set -euo pipefail

: "${MONGO_HOST:?MONGO_HOST is required}"
: "${MONGODB_ROOT_USER:?MONGODB_ROOT_USER is required}"
: "${MONGODB_ROOT_PASSWORD:?MONGODB_ROOT_PASSWORD is required}"
: "${MONGODB_PANTRY_PAL_DATABASE:?MONGODB_PANTRY_PAL_DATABASE is required}"
: "${MONGODB_PANTRY_PAL_USER:?MONGODB_PANTRY_PAL_USER is required}"
: "${MONGODB_PANTRY_PAL_PASSWORD:?MONGODB_PANTRY_PAL_PASSWORD is required}"

echo "Creating MongoDB application user..."

mongosh \
    --host "$MONGO_HOST" \
    --username "$MONGODB_ROOT_USER" \
    --password "$MONGODB_ROOT_PASSWORD" \
    --authenticationDatabase admin \
    --quiet \
    --eval '
        const databaseName = process.env.MONGODB_PANTRY_PAL_DATABASE;
        const username = process.env.MONGODB_PANTRY_PAL_USER;
        const password = process.env.MONGODB_PANTRY_PAL_PASSWORD;

        const database = db.getSiblingDB(databaseName);
        const existingUser = database.getUser(username);

        if (existingUser) {
            print(`User ${username} already exists`);
        } else {
            database.createUser({
                user: username,
                pwd: password,
                roles: [
                    {
                        role: "readWrite",
                        db: databaseName
                    }
                ]
            });

            print(`Created user ${username}`);
        }
    '

echo "MongoDB initialization complete."
