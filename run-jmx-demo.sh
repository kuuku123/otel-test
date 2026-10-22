#!/usr/bin/env bash
set -e

# Change to project root directory
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "========================================================"
echo "  Compiling Project and Preparing Classpath..."
echo "========================================================"
mvn compile -q

# Build classpath cache file for fast subsequent runs
CP_FILE="target/classpath.cache"
if [ ! -f "$CP_FILE" ]; then
    echo "Generating classpath cache..."
    mvn dependency:build-classpath -Dmdep.outputFile="$CP_FILE" -q
fi
CLASSPATH="target/classes:$(cat "$CP_FILE")"

echo "========================================================"
echo "  Launching JMX Application with Remote RMI on Port 9999"
echo "========================================================"
echo ""

java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.rmi.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -Djava.rmi.server.hostname=localhost \
     -cp "$CLASSPATH" \
     com.example.otel.jmxdemos.JmxMain "$@"
