#!/bin/bash
DB_HOST=$1
DB_PORT=$2
echo "Wait for DB $DB_HOST:$DB_PORT..."
sleep 2
until pg_isready -h "$DB_HOST" -p "$DB_PORT" -q; do
  sleep 2
done
echo "Port $DB_PORT is open!"
exit 0
