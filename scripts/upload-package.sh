#!/bin/zsh
set -e

SCRIPT_DIR="$( dirname -- "$0" )"
VERSION=$1

if [[ -z $VERSION ]]; then
  echo "Usage: ./Scripts/upload-pod.sh <version>"
  exit 1
fi

# 1. Go to Root Folder
cd $SCRIPT_DIR/..

# 2. Update build.gradle & Version.kt
# coordinates("com.mobilyflow", "mobilyflow-android-sdk", "0.1.1-alpha.26")
sed -i '' -E "s/coordinates\(\"com.mobilyflow\", \"mobilyflow-android-sdk\", \"([0-9a-zA-Z.-]+)\"\)/coordinates(\"com.mobilyflow\", \"mobilyflow-android-sdk\", \"${VERSION}\"\)/" mobilyflow-android-sdk/build.gradle.kts


cat > mobilyflow-android-sdk/src/main/java/com/mobilyflow/mobilypurchasesdk/Version.kt <<EOL
package com.mobilyflow.mobilypurchasesdk

val MOBILYFLOW_SDK_VERSION = "${VERSION}"
EOL

# 4. Push and tag
git add .
git commit -m "Version ${VERSION}"
git push origin main
git tag "${VERSION}"
git push --tags

# 5. Upload package
./gradlew publishToMavenCentral
