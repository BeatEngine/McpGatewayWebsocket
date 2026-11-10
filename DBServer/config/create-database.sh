#!/bin/bash
ddlvar="CREATE DATABASE $POSTGRES_START_DB OWNER $POSTGRES_USER;"
state=$(psql -c "$ddlvar" -h localhost -p 5432 -U postgres 2>&1)
echo "INIT DB: $state"