package br.com.supermercado.presentation.cli;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Objects;
import java.util.Scanner;

/**
 * Concentra a leitura do teclado com validação: repete a pergunta até a entrada ser válida.
 * Se o teclado fechar (EOF), o Scanner lança NoSuchElementException, tratada na aplicação.
 */
public final class InputReader {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;
    private final TerminalOutput out;

    public InputReader(Scanner scanner, TerminalOutput out) {
        this.scanner = Objects.requireNonNull(scanner);
        this.out = Objects.requireNonNull(out);
    }

    public String readLine(String prompt) {
        out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String readRequired(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) return text;
            out.error("Este campo é obrigatório.");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) return value;
                out.error("Informe um número entre " + min + " e " + max + ".");
            } catch (NumberFormatException e) {
                out.error("Entrada inválida: digite um número inteiro.");
            }
        }
    }

    /** Aceita vírgula ou ponto como separador decimal; exige valor maior que zero. */
    public double readPositiveDecimal(String prompt) {
        while (true) {
            String text = readLine(prompt).replace(',', '.');
            try {
                double value = Double.parseDouble(text);
                if (value > 0 && !Double.isInfinite(value)) return value;
                out.error("O valor deve ser maior que zero.");
            } catch (NumberFormatException e) {
                out.error("Entrada inválida: digite um número (ex.: 4,20).");
            }
        }
    }

    /** Data no formato dd/mm/aaaa; vazio devolve {@code defaultDate}. */
    public LocalDate readDate(String prompt, LocalDate defaultDate) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty() && defaultDate != null) return defaultDate;
            try {
                return LocalDate.parse(text, DATE);
            } catch (DateTimeParseException e) {
                out.error("Data inválida: use o formato dd/mm/aaaa.");
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String text = readLine(prompt + " (s/n): ").toLowerCase();
            if (text.equals("s") || text.equals("sim")) return true;
            if (text.equals("n") || text.equals("nao") || text.equals("não")) return false;
            out.error("Responda com 's' ou 'n'.");
        }
    }

    /** Menu numerado (0 = voltar/sair); devolve a opção escolhida. */
    public int readMenuOption(String title, String... options) {
        return readMenuWithZeroLabel(title, "Voltar", options);
    }

    /** Igual a {@link #readMenuOption}, mas com um texto próprio para a opção 0. */
    public int readMenuWithZeroLabel(String title, String zeroLabel, String... options) {
        out.title(title);
        for (int i = 0; i < options.length; i++) {
            out.println("  " + (i + 1) + ") " + options[i]);
        }
        out.println("  0) " + zeroLabel);
        return readInt("Escolha: ", 0, options.length);
    }
}
