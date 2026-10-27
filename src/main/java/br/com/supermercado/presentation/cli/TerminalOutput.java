package br.com.supermercado.presentation.cli;

import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Concentra a escrita no terminal: mensagens padronizadas e formatação de valores. */
public final class TerminalOutput {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PrintStream out;

    public TerminalOutput(PrintStream out) {
        this.out = Objects.requireNonNull(out);
    }

    public void println(String text) { out.println(text); }

    public void blank() { out.println(); }

    public void print(String text) {
        out.print(text);
        out.flush();
    }

    public void error(String text) { out.println("  [ERRO] " + text); }

    public void success(String text) { out.println("  [OK] " + text); }

    public void warning(String text) { out.println("  [!] " + text); }

    public void title(String title) {
        out.println();
        out.println("=== " + title + " ===");
    }

    // ================================================================ formatação
    public static String money(double value) {
        return String.format(PT_BR, "R$ %,.2f", value);
    }

    /** Corta o texto para caber em colunas de tabela. */
    public static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    public static String dateTime(LocalDateTime date) {
        return date == null ? "--/--/---- --:--" : date.format(DATE_TIME);
    }

    public static String hour(LocalDateTime date) {
        return date == null ? "--:--:--" : date.format(HOUR);
    }

    public static String duration(Duration duration) {
        long total = Math.max(0, duration.getSeconds());
        return (total / 60) + "m " + String.format("%02d", total % 60) + "s";
    }
}
