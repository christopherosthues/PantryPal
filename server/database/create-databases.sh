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


#
#set -e
#set -u
#
#function create_user_and_database() {
#  local database=$1
#  local user=$2
#  if psql -lqt --username "$user" | cut -d \| -f 1 | grep -qw $database; then
#      echo "Database $database already exists."
#  else
#      echo "  Creating user '$user' and database '$database'"
#      psql -v ON_ERROR_STOP=1 --username "$user" <<-EOSQL
#          CREATE USER $user;
#          CREATE DATABASE $database;
#          GRANT ALL PRIVILEGES ON DATABASE $database TO $user;
#EOSQL
#  fi
#}
#
##if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
##  echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
##  IFS=';' read -r -a databases <<< "$POSTGRES_MULTIPLE_DATABASES"
##  for database_user in "${databases[@]}"; do
##    database_user=$(echo "$database_user" | tr -d '[:space:]' | tr -d '()')
##    IFS=',' read -r database user <<< "$database_user"
##    create_user_and_database $database $user
##  done
##  echo "Multiple databases created"
##fi
#
#if [ ${#databases[@]} -ne ${#users[@]} ] || [ ${#databases[@]} -ne ${#schemas[@]} ]; then
#    echo "Error: The number of databases, users, and schemas must be the same."
#    exit 1
#fi
#
#for i in "${!databases[@]}"; do
#    db="${databases[$i]}"
#    user="${users[$i]}"
#    schema_list="${schemas[$i]}"
#
#    echo "Creating database $db, user $user, and schemas $schema_list..."
#
#    # Create the database
#    PGPASSWORD=$PGPASSWORD psql -U $PGUSER -h $PGHOST -p $PGPORT -c "CREATE DATABASE $db;"
#
#    # Create the user and set the password
#    PGPASSWORD=$PGPASSWORD psql -U $PGUSER -h $PGHOST -p $PGPORT -c "CREATE USER $user WITH PASSWORD 'password_for_$user';"
#
#    # Grant all privileges on the database to the user
#    PGPASSWORD=$PGPASSWORD psql -U $PGUSER -h $PGHOST -p $PGPORT -c "GRANT ALL PRIVILEGES ON DATABASE $db TO $user;"
#
#    # Create schemas in the database
#    IFS=',' read -ra schema_array <<< "$schema_list"  # Split the schemas string by commas
#    for schema in "${schema_array[@]}"; do
#        echo "Creating schema $schema in database $db..."
#        PGPASSWORD=$PGPASSWORD psql -U $PGUSER -h $PGHOST -p $PGPORT -d $db -c "CREATE SCHEMA $schema;"
#
#        # Grant usage and create privileges on the schema to the user
#        PGPASSWORD=$PGPASSWORD psql -U $PGUSER -h $PGHOST -p $PGPORT -d $db -c "GRANT USAGE, CREATE ON SCHEMA $schema TO $user;"
#    done
#
#    echo "Database $db, user $user, and schemas created and privileges granted."
#done
