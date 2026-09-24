##!/bin/bash

# Read PostgreSQL superuser password from secret file
export PGPASSWORD=$(cat /run/secrets/postgres_password)

# Helper function to create a database, admin user, and schemas
create_db_admin_schemas() {
    DB_SUPERUSER=$1
    DB=$2
    DB_NAME=$3
    DB_ADMIN=$4
    DB_SCHEMAS=$5

    # Read password from secret file
    DB_ADMIN_PASSWORD=$(cat /run/secrets/pantrypal_admin_password)

    # Create the database if it doesn't exist
    echo "Creating database $DB_NAME..."
    psql -U $DB_SUPERUSER -d $DB -c "CREATE DATABASE $DB_NAME;" || echo "Database $DB_NAME may already exist."

    # Create the admin user and set the password
    echo "Creating admin user $DB_ADMIN..."
    psql -U $DB_SUPERUSER -d $DB -c "CREATE USER $DB_ADMIN WITH PASSWORD '$DB_ADMIN_PASSWORD';" || echo "Admin user $DB_ADMIN may already exist."

    # Grant full privileges on the database to the admin user
    echo "Granting privileges to admin user $DB_ADMIN on $DB_NAME..."
    psql -U $DB_SUPERUSER -d $DB -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_ADMIN;"

    # Create schemas in the database
    IFS=',' read -ra schemas <<< "$DB_SCHEMAS"
    for schema in "${schemas[@]}"; do
        echo "Creating schema $schema in database $DB_NAME..."
        psql -U $DB_SUPERUSER -d $DB_NAME -c "CREATE SCHEMA $schema;" || echo "Schema $schema may already exist."

        # Grant full ownership of the schema to the admin user
        echo "Granting privileges on schema $schema to admin user $DB_ADMIN..."
        psql -U $DB_SUPERUSER -d $DB_NAME -c "GRANT ALL ON SCHEMA $schema TO $DB_ADMIN;"
    done
}

# Helper function to create an application user for a database
create_app_user() {
    DB_SUPERUSER=$1
    DB=$2
    DB_NAME=$3
    DB_USER=$4
    DB_SCHEMAS=$5

    # Read password from secret file
    DB_USER_PASSWORD=$(cat /run/secrets/pantrypal_user_password)

    # Create the application user and set the password
    echo "Creating app user $DB_USER..."
    psql -U $DB_SUPERUSER -d $DB -c "CREATE USER $DB_USER WITH PASSWORD '$DB_USER_PASSWORD';" || echo "App user $DB_USER may already exist."

    # Grant read/write privileges on the database to the user
    echo "Granting privileges to app user $DB_USER on $DB_NAME..."
    psql -U $DB_SUPERUSER -d $DB -c "GRANT ALL ON DATABASE $DB_NAME TO $DB_USER;"

    # Grant usage and create privileges on schemas to the application user
    IFS=',' read -ra schemas <<< "$DB_SCHEMAS"
    for schema in "${schemas[@]}"; do
        echo "Granting privileges on schema $schema to app user $DB_USER..."
        psql -U $DB_SUPERUSER -d $DB_NAME -c "GRANT USAGE, CREATE ON SCHEMA $schema TO $DB_USER;" || echo "Schema $schema may already exist."
    done
}


# Create databases, admin users, and application users based on environment variables
echo "Initializing databases..."

# PantryPal: create database with admin user, then create application user
create_db_admin_schemas "$POSTGRES_USER" "$PANTRY_PAL_DB" "$PANTRY_PAL_DB" "$PANTRY_PAL_ADMIN" "$PANTRY_PAL_SCHEMA"
create_app_user "$POSTGRES_USER" "$PANTRY_PAL_DB" "$PANTRY_PAL_DB" "$PANTRY_PAL_USER" "$PANTRY_PAL_SCHEMA"

echo "Database initialization complete."
