import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Tempo {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Tempo() {
    }

    public static String formatar(float segundos) {
        int total = (int) segundos;
        int minutos = total / 60;
        int resto = total % 60;
        return minutos + "m " + (resto < 10 ? "0" : "") + resto + "s";
    }

    public static String hora(LocalDateTime dataHora) {
        if (dataHora == null) {
            return "--:--:--";
        }
        return dataHora.format(FORMATO_HORA);
    }
}