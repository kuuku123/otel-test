#!/usr/bin/env bash
set -e

# Change to project root directory
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

echo "========================================================"
echo "  Compiling Project and Preparing Classpath..."
echo "========================================================"
mvn compile -q

# Ensure opentelemetry-javaagent.jar is present
if [ ! -f "opentelemetry-javaagent.jar" ]; then
    echo "Downloading OpenTelemetry Java Agent (2.10.0)..."
    mvn dependency:copy -Dartifact=io.opentelemetry.javaagent:opentelemetry-javaagent:2.10.0:jar -DoutputDirectory=. -Dmdep.stripVersion=true -q
fi

# Build classpath file in target directory for fast subsequent runs
CP_FILE="target/classpath.cache"
if [ ! -f "$CP_FILE" ]; then
    echo "Generating classpath cache..."
    mvn dependency:build-classpath -Dmdep.outputFile="$CP_FILE" -q
fi
CLASSPATH="target/classes:$(cat "$CP_FILE")"

echo "========================================================"
echo "  Launching Java with -javaagent:opentelemetry-javaagent.jar"
echo "========================================================"
echo ""

java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=otel-javaagent-study \
     -Dotel.traces.exporter=logging \
     -Dotel.metrics.exporter=logging \
     -Dotel.logs.exporter=logging \
     -Dotel.metric.export.interval=3000 \
     -cp "$CLASSPATH" \
     com.example.otel.agentdemos.AgentMain "$@"
