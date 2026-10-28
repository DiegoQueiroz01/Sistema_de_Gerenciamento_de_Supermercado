package br.com.supermercado.presentation.cli;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;

// Leitura do teclado. Os métodos repetem a pergunta até o usuário digitar algo válido.
public class InputReader {
    // STRICT faz datas como 31/02 darem erro em vez de virar outro dia
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;
    private final TerminalOutput out;

    public InputReader(Scanner scanner, TerminalOutput out) {
        this.scanner = scanner;
        this.out = out;
    }

    public String readLine(String prompt) {
        out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String readRequired(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            out.error("Este campo é obrigatório.");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                out.error("Informe um número entre " + min + " e " + max + ".");
            } catch (NumberFormatException e) {
                out.error("Entrada inválida: digite um número inteiro.");
            }
        }
    }

    // Aceita vírgula ou ponto (4,20 ou 4.20). O valor tem que ser maior que zero.
    public double readPositiveDecimal(String prompt) {
        while (true) {
            String text = readLine(prompt).replace(',', '.');
            try {
                double value = Double.parseDouble(text);
                if (value > 0 && !Double.isInfinite(value)) {
                    return value;
                }
                out.error("O valor deve ser maior que zero.");
            } catch (NumberFormatException e) {
                out.error("Entrada inválida: digite um número (ex.: 4,20).");
            }
        }
    }

    // Data no formato dd/mm/aaaa. Se ficar vazio, devolve defaultDate.
    public LocalDate readDate(String prompt, LocalDate defaultDate) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty() && defaultDate != null) {
                return defaultDate;
            }
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
            if (text.equals("s") || text.equals("sim")) {
                return true;
            }
            if (text.equals("n") || text.equals("nao") || text.equals("não")) {
                return false;
            }
            out.error("Responda com 's' ou 'n'.");
        }
    }

    // Mostra um menu numerado com "0) Voltar" e devolve a opção escolhida
    public int readMenuOption(String title, String... options) {
        return readMenuWithZeroLabel(title, "Voltar", options);
    }

    // Igual ao readMenuOption, mas com outro texto na opção 0
    public int readMenuWithZeroLabel(String title, String zeroLabel, String... options) {
        out.title(title);
        for (int i = 0; i < options.length; i++) {
            out.println("  " + (i + 1) + ") " + options[i]);
        }
        out.println("  0) " + zeroLabel);
        return readInt("Escolha: ", 0, options.length);
    }
}
