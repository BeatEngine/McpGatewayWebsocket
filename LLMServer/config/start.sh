#!/bin/bash
# LOAD ENVIRONMENT
set -a 
source /environmentconfigurations.env 
set +a  
# 1. START OLLAMA IN BACKGROUND
echo "Serve $OLLAMA_URL ... "
ollama serve &
OLLAMA_PID=$!
# 2. PREPARE DOMAIN, PORT
#OLLAMA_URL http://DOMAIN:PORT
TEXT=$OLLAMA_URL
SEPPERATOR="//"
REPLACE=""
WORK_STRING="${TEXT/$SEPPERATOR/$REPLACE}"
LLM_DOMAIN=""
LLM_PORT=""
IFS=":" read -r empty LLM_DOMAIN LLM_PORT <<< "$WORK_STRING"
# 3. WAIT OLLAMA HAS STARTED
echo "Wait for GET $OLLAMA_URL"
bash -c '/wait-for-ollama-serve.sh localhost 11434'
# 4. PULL THE LLM OLLAMA_MODEL
echo "Pull LLM '$OLLAMA_MODEL'"
bash -c '/ollama-pull-model.sh'
echo 'RUNNING' > /state.txt
# Stay open
wait $OLLAMA_PID
