package br.com.supermercado.presentation.cli;

import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Escrita no terminal: mensagens padronizadas e formatação de valores
public class TerminalOutput {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PrintStream out;

    public TerminalOutput(PrintStream out) {
        this.out = out;
    }

    public void println(String text) {
        out.println(text);
    }

    public void blank() {
        out.println();
    }

    public void print(String text) {
        out.print(text);
        out.flush();
    }

    public void error(String text) {
        out.println("  [ERRO] " + text);
    }

    public void success(String text) {
        out.println("  [OK] " + text);
    }

    public void warning(String text) {
        out.println("  [!] " + text);
    }

    public void title(String title) {
        out.println();
        out.println("=== " + title + " ===");
    }

    // ---------- formatação ----------

    // Ex.: R$ 1.234,50
    public static String money(double value) {
        return String.format(BRAZIL, "R$ %,.2f", value);
    }

    // Corta o texto para caber na coluna da tabela
    public static String cut(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "…";
    }

    public static String dateTime(LocalDateTime date) {
        if (date == null) {
            return "--/--/---- --:--";
        }
        return date.format(DATE_TIME);
    }

    public static String hour(LocalDateTime date) {
        if (date == null) {
            return "--:--:--";
        }
        return date.format(HOUR);
    }

    // Ex.: 3m 05s
    public static String duration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 0) {
            seconds = 0;
        }
        return (seconds / 60) + "m " + String.format("%02d", seconds % 60) + "s";
    }
}
