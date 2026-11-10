#!/bin/bash
# LOAD ENVIRONMENT
set -e
# 1. START DATABASE IN BACKGROUND
echo "Start postgresql ... "
bash -c '/usr/local/bin/docker-entrypoint.sh postgres' &
DB_PID=$!

bash -c '/wait-for-database.sh localhost 5432'
# 2. CREATE DB
echo "Create database: '$POSTGRES_START_DB'"
bash -c '/create-database.sh'
echo 'RUNNING' > /state.txt

# Stay open
wait $DB_PID
