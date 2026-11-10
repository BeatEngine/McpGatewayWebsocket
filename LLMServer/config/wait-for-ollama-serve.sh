#!/bin/bash
HOST=$1
PORT=$2
echo "Wait for Ollama $HOST:$PORT serve..."
until (
  exec 3<>/dev/tcp/"$HOST"/"$PORT" 2>/dev/null && \
  printf "GET / HTTP/1.1\r\nHost: %s:%s\r\nConnection: close\r\n\r\n" "$HOST" "$PORT" >&3 && \
  cat <&3 | grep -q "HTTP/1.1 200 OK"
); do
  sleep 1
done
echo "Ollama has been served."

exit 0
