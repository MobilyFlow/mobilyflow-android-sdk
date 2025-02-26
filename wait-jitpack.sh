#!/bin/sh
LAST_VERSION=$(git describe --tags)

echo "Wait version ${LAST_VERSION} be available on Jitpack"

while true; do
  STATUS=$(curl -s https://jitpack.io/api/builds/com.github.mobilyflow/mobilyflow-android-sdk | jq -r '."com.github.mobilyflow"."mobilyflow-android-sdk"."'${LAST_VERSION}'"')

  if [[ "$STATUS" == "ok" ]]; then
    break
  else
    echo "$STATUS, retry..."
  fi
  sleep 30
done