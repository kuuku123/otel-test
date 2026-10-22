#!/usr/bin/env bash
set -e

# Change to project root directory
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

SCRAPER_JAR="opentelemetry-jmx-scraper.jar"
SCRAPER_VERSION="v1.60.0"
SCRAPER_URL="https://github.com/open-telemetry/opentelemetry-java-contrib/releases/download/${SCRAPER_VERSION}/opentelemetry-jmx-scraper.jar"

# 1. Download OpenTelemetry JMX Scraper if missing
if [ ! -f "$SCRAPER_JAR" ]; then
    echo "========================================================"
    echo "  Downloading OpenTelemetry JMX Scraper (${SCRAPER_VERSION})..."
    echo "========================================================"
    curl -sSL -o "$SCRAPER_JAR" "$SCRAPER_URL"
    echo "✔ Download complete: $SCRAPER_JAR"
fi

JMX_URL="${OTEL_JMX_SERVICE_URL:-service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi}"
OTEL_ENDPOINT="${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4317}"
CONFIG_FILE="${OTEL_JMX_CONFIG:-jmx-scraper-config.yaml}"
INTERVAL_MS="${OTEL_METRIC_EXPORT_INTERVAL:-3000}"

echo "========================================================"
echo "  Launching OpenTelemetry JMX Scraper"
echo "========================================================"
echo "  • JMX Target   : $JMX_URL"
echo "  • OTLP Endpoint: $OTEL_ENDPOINT"
echo "  • Config Rules : $CONFIG_FILE"
echo "  • Interval     : ${INTERVAL_MS}ms"
echo "========================================================"
echo ""

java -Dotel.jmx.service.url="$JMX_URL" \
     -Dotel.jmx.config="$CONFIG_FILE" \
     -Dotel.jmx.target.system=jvm \
     -Dotel.metrics.exporter=otlp \
     -Dotel.exporter.otlp.endpoint="$OTEL_ENDPOINT" \
     -Dotel.metric.export.interval="$INTERVAL_MS" \
     -Dotel.service.name=otel-jmx-study-app \
     -jar "$SCRAPER_JAR" "$@"
