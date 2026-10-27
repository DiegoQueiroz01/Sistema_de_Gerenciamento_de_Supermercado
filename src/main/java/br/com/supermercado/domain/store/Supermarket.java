package br.com.supermercado.domain.store;

/**
 * Dados cadastrais do estabelecimento, exibidos no cabeçalho do sistema e no cupom.
 * O CNPJ é opcional, mas, se informado, precisa ser válido (dígitos verificadores).
 */
public record Supermarket(String name, String cnpj, String address, String phone) {

    public Supermarket {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do supermercado não pode ser vazio.");
        }
        name = name.trim();
        cnpj = normalizeCnpj(cnpj);
        address = address == null ? "" : address.trim();
        phone = phone == null ? "" : phone.trim();
    }

    /** Guarda o CNPJ só com números; vazio é aceito. */
    private static String normalizeCnpj(String cnpj) {
        String digits = cnpj == null ? "" : cnpj.replaceAll("\\D", "");
        if (!digits.isEmpty() && !isValidCnpj(digits)) {
            throw new IllegalArgumentException("CNPJ inválido: " + cnpj.trim());
        }
        return digits;
    }

    /** Valida os 14 dígitos e os dois dígitos verificadores do CNPJ. */
    public static boolean isValidCnpj(String digits) {
        if (digits == null || !digits.matches("\\d{14}") || digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 12) == digits.charAt(12) - '0'
                && checkDigit(digits, 13) == digits.charAt(13) - '0';
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        int weight = length - 7; // 5 para o 1º dígito, 6 para o 2º; os pesos vão de 2 a 9 e recomeçam
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * weight;
            weight = weight == 2 ? 9 : weight - 1;
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    /** CNPJ no formato 00.000.000/0000-00 (ou "-" se não informado). */
    public String formattedCnpj() {
        if (cnpj.isEmpty()) return "-";
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/"
                + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
