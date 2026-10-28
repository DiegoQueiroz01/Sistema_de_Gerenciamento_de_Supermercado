package br.com.supermercado.domain.checkout;

import java.time.LocalDateTime;

// Cliente esperando na fila. A senha (ticket) segue o formato S001, S002...
public class Customer {
    public static final String DEFAULT_NAME = "Cliente";

    private final long id;
    private final String ticket;
    private final String name;
    private final boolean priority;
    private final LocalDateTime arrivalTime;

    public Customer(long id, String ticket, String name, boolean priority, LocalDateTime arrivalTime) {
        if (id <= 0) {
            throw new IllegalArgumentException("O identificador do cliente deve ser positivo.");
        }
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalArgumentException("A senha do cliente é obrigatória.");
        }
        if (arrivalTime == null) {
            throw new IllegalArgumentException("O horário de chegada é obrigatório.");
        }
        this.id = id;
        this.ticket = ticket;
        if (name == null || name.isBlank()) {
            this.name = DEFAULT_NAME;
        } else {
            this.name = name.trim();
        }
        this.priority = priority;
        this.arrivalTime = arrivalTime;
    }

    public Customer(long id, boolean priority, LocalDateTime arrivalTime) {
        this(id, formatTicket(id), DEFAULT_NAME, priority, arrivalTime);
    }

    public static String formatTicket(long sequence) {
        return String.format("S%03d", sequence);
    }

    public long id() { return id; }
    public String ticket() { return ticket; }
    public String name() { return name; }
    public boolean priority() { return priority; }
    public LocalDateTime arrivalTime() { return arrivalTime; }

    @Override
    public String toString() {
        String text = ticket + " " + name;
        if (priority) {
            text += " [PRIO]";
        }
        return text;
    }
}
