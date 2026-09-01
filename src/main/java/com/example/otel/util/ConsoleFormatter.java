package com.example.otel.util;

/**
 * Utility class for formatting console outputs with ANSI colors and structured boxes
 * to make OpenTelemetry learning intuitive and visually clear.
 */
public final class ConsoleFormatter {

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String RED = "\u001B[31m";
    public static final String GRAY = "\u001B[90m";

    private ConsoleFormatter() {}

    public static void printHeader(String title) {
        String bar = "=".repeat(75);
        System.out.println("\n" + CYAN + BOLD + bar);
        System.out.println("  " + title);
        System.out.println(bar + RESET);
    }

    public static void printSubHeader(String subtitle) {
        System.out.println("\n" + YELLOW + BOLD + ">>> " + subtitle + RESET);
    }

    public static void printExplanation(String explanation) {
        System.out.println(GRAY + "ℹ️  " + explanation + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "✔ " + message + RESET);
    }

    public static void printInfo(String key, Object value) {
        System.out.printf("  %s%-24s%s : %s%s%s%n", PURPLE, key, RESET, BOLD, value, RESET);
    }
}
