package com.example.otel.util;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.*;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collection;

/**
 * A custom OpenTelemetry MetricExporter that pretty-prints metrics to stdout
 * so you can inspect how counters, gauges, and histograms aggregate data.
 */
public class PrettyMetricExporter implements MetricExporter {

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (metrics.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }

        System.out.println(ConsoleFormatter.BLUE + "┌────────────────── [ EXPORTED METRICS BATCH ] ──────────────────" + ConsoleFormatter.RESET);
        for (MetricData metric : metrics) {
            System.out.printf("│ %sMetric: %s%s%s (Unit: '%s', Type: %s)%n",
                    ConsoleFormatter.BOLD, ConsoleFormatter.PURPLE, metric.getName(), ConsoleFormatter.RESET,
                    metric.getUnit(), metric.getType());
            if (!metric.getDescription().isEmpty()) {
                System.out.printf("│   Description: %s%n", metric.getDescription());
            }

            switch (metric.getType()) {
                case LONG_SUM -> {
                    SumData<LongPointData> sumData = metric.getLongSumData();
                    for (LongPointData point : sumData.getPoints()) {
                        System.out.printf("│   • %sValue: %d%s | Dimensions: %s%n",
                                ConsoleFormatter.GREEN, point.getValue(), ConsoleFormatter.RESET, formatAttributes(point.getAttributes()));
                    }
                }
                case DOUBLE_SUM -> {
                    SumData<DoublePointData> sumData = metric.getDoubleSumData();
                    for (DoublePointData point : sumData.getPoints()) {
                        System.out.printf("│   • %sValue: %.2f%s | Dimensions: %s%n",
                                ConsoleFormatter.GREEN, point.getValue(), ConsoleFormatter.RESET, formatAttributes(point.getAttributes()));
                    }
                }
                case LONG_GAUGE -> {
                    GaugeData<LongPointData> gaugeData = metric.getLongGaugeData();
                    for (LongPointData point : gaugeData.getPoints()) {
                        System.out.printf("│   • %sGauge Value: %d%s | Dimensions: %s%n",
                                ConsoleFormatter.CYAN, point.getValue(), ConsoleFormatter.RESET, formatAttributes(point.getAttributes()));
                    }
                }
                case DOUBLE_GAUGE -> {
                    GaugeData<DoublePointData> gaugeData = metric.getDoubleGaugeData();
                    for (DoublePointData point : gaugeData.getPoints()) {
                        System.out.printf("│   • %sGauge Value: %.2f%s | Dimensions: %s%n",
                                ConsoleFormatter.CYAN, point.getValue(), ConsoleFormatter.RESET, formatAttributes(point.getAttributes()));
                    }
                }
                case HISTOGRAM -> {
                    HistogramData histogramData = metric.getHistogramData();
                    for (HistogramPointData point : histogramData.getPoints()) {
                        System.out.printf("│   • %sCount: %d, Sum: %.2f, Min: %.2f, Max: %.2f%s | Dimensions: %s%n",
                                ConsoleFormatter.YELLOW,
                                point.getCount(),
                                point.getSum(),
                                point.hasMin() ? point.getMin() : 0.0,
                                point.hasMax() ? point.getMax() : 0.0,
                                ConsoleFormatter.RESET,
                                formatAttributes(point.getAttributes()));
                        System.out.printf("│     Buckets: %s, Counts: %s%n",
                                point.getBoundaries(), point.getCounts());
                    }
                }
                default -> System.out.printf("│   • [Data format: %s]%n", metric.getType());
            }
        }
        System.out.println(ConsoleFormatter.BLUE + "└─────────────────────────────────────────────────────────────────" + ConsoleFormatter.RESET);
        return CompletableResultCode.ofSuccess();
    }

    private String formatAttributes(Attributes attributes) {
        if (attributes.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        attributes.forEach((k, v) -> sb.append(k.getKey()).append("=").append(v).append(", "));
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
