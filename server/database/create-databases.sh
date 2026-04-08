##!/bin/bash

# Helper function to create a database, user, and schemas
create_db_user_schemas() {
    DB_SUPERUSER=$1
    DB=$2
    DB_NAME=$3
    DB_USER=$4
    DB_USER_PASSWORD=$5
    DB_SCHEMAS=$6

    # Create the database
    echo "Creating database $DB_NAME..."
    psql -U $DB_SUPERUSER -d $DB -c "CREATE DATABASE $DB_NAME;"

    # Create the user and set the password
    echo "Creating user $DB_USER..."
    psql -U $DB_SUPERUSER -d $DB -c "CREATE USER $DB_USER WITH PASSWORD '$DB_USER_PASSWORD';"

    # Grant privileges on the database to the user
    echo "Granting privileges to user $DB_USER on $DB_NAME..."
    psql -U $DB_SUPERUSER -d $DB -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"

    # Create schemas in the database
    IFS=',' read -ra schemas <<< "$DB_SCHEMAS"
    for schema in "${schemas[@]}"; do
        echo "Creating schema $schema in database $DB_NAME..."
        psql -U $DB_SUPERUSER -d $DB_NAME -c "CREATE SCHEMA $schema;"

        # Grant usage and create privileges on the schema to the user
        echo "Granting privileges on schema $schema to user $DB_USER..."
        psql -U $DB_SUPERUSER -d $DB_NAME -c "GRANT USAGE, CREATE ON SCHEMA $schema TO $DB_USER;"
    done
}

# Helper function to create a user for database
create_user() {
    DB_SUPERUSER=$1
    DB=$2
    DB_NAME=$3
    DB_USER=$4
    DB_USER_PASSWORD=$5
    DB_SCHEMAS=$6

    # Create the user and set the password
    echo "Creating user $DB_USER..."
    psql -U $DB_SUPERUSER -d $DB -c "CREATE USER $DB_USER WITH PASSWORD '$DB_USER_PASSWORD';"

    # Grant privileges on the database to the user
    echo "Granting privileges to user $DB_USER on $DB_NAME..."
    psql -U $DB_SUPERUSER -d $DB -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"

    # Grant usage and create privileges on the schema to the user
    echo "Granting privileges on schema public to user $DB_USER..."
    psql -U $DB_SUPERUSER -d $DB_NAME -c "GRANT USAGE, CREATE ON SCHEMA public TO $DB_USER;"

    # Create schemas in the database
    IFS=',' read -ra schemas <<< "$DB_SCHEMAS"
    for schema in "${schemas[@]}"; do
        echo "Creating schema $schema in database $DB_NAME..."
        psql -U $DB_SUPERUSER -d $DB_NAME -c "CREATE SCHEMA $schema;"

        # Grant usage and create privileges on the schema to the user
        echo "Granting privileges on schema $schema to user $DB_USER..."
        psql -U $DB_SUPERUSER -d $DB_NAME -c "GRANT USAGE, CREATE ON SCHEMA $schema TO $DB_USER;"
    done
}


# Create databases, users, and schemas based on environment variables
echo "Initializing databases..."

create_user "$POSTGRES_USER" "$PANTRY_PAL_DB" "$PANTRY_PAL_DB" "$PANTRY_PAL_USER" "$PANTRY_PAL_USER_PASSWORD" "$PANTRY_PAL_SCHEMA"
create_db_user_schemas "$POSTGRES_USER" "$PANTRY_PAL_DB" "$KEYCLOAK_DB" "$KEYCLOAK_ADMIN" "$KEYCLOAK_ADMIN_PASSWORD" "$KEYCLOAK_SCHEMAS"
create_user "$POSTGRES_USER" "$PANTRY_PAL_DB" "$KEYCLOAK_DB" "$KEYCLOAK_USER" "$KEYCLOAK_USER_PASSWORD" "$KEYCLOAK_SCHEMAS"

echo "Database initialization complete."
