package br.com.supermercado.domain.store;

// Dados do supermercado (aparecem no cabeçalho e no cupom).
// O CNPJ é opcional, mas se for informado precisa ser válido.
public class Supermarket {
    private final String name;
    private final String cnpj; // só os números
    private final String address;
    private final String phone;

    public Supermarket(String name, String cnpj, String address, String phone) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do supermercado não pode ser vazio.");
        }
        this.name = name.trim();
        this.cnpj = onlyDigits(cnpj);
        this.address = address == null ? "" : address.trim();
        this.phone = phone == null ? "" : phone.trim();
    }

    private static String onlyDigits(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        String digits = cnpj.replaceAll("[^0-9]", "");
        if (!digits.isEmpty() && !isValidCnpj(digits)) {
            throw new IllegalArgumentException("CNPJ inválido: " + cnpj.trim());
        }
        return digits;
    }

    // Confere se tem 14 números e se os dois dígitos verificadores estão certos
    public static boolean isValidCnpj(String digits) {
        if (digits == null || !digits.matches("[0-9]{14}")) {
            return false;
        }
        // CNPJ com todos os números iguais (ex.: 11111111111111) é inválido
        boolean allEqual = true;
        for (int i = 1; i < digits.length(); i++) {
            if (digits.charAt(i) != digits.charAt(0)) {
                allEqual = false;
            }
        }
        if (allEqual) {
            return false;
        }
        int first = checkDigit(digits, 12);
        int second = checkDigit(digits, 13);
        return first == digits.charAt(12) - '0' && second == digits.charAt(13) - '0';
    }

    // Calcula um dígito verificador usando os primeiros "length" números.
    // Os pesos vão diminuindo até 2 e depois voltam para 9.
    private static int checkDigit(String digits, int length) {
        int sum = 0;
        int weight = length - 7; // 5 para o 1º dígito e 6 para o 2º
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * weight;
            weight--;
            if (weight < 2) {
                weight = 9;
            }
        }
        int rest = sum % 11;
        if (rest < 2) {
            return 0;
        }
        return 11 - rest;
    }

    public String name() { return name; }
    public String cnpj() { return cnpj; }
    public String address() { return address; }
    public String phone() { return phone; }

    // CNPJ no formato 00.000.000/0000-00
    public String formattedCnpj() {
        if (cnpj.isEmpty()) {
            return "-";
        }
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/"
                + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
