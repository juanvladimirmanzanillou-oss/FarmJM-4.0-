package com.unev.farmjm.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de dinero y fechas compartidos. */
public final class Fmt {

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", new Locale("es", "DO"));

    private Fmt() { }

    public static String dinero(double v) { return String.format(Locale.US, "RD$ %,.2f", v); }

    public static String fecha(LocalDate d) { return d == null ? "-" : d.format(FECHA); }

    public static String fechaHora(LocalDateTime d) { return d == null ? "-" : d.format(FECHA_HORA); }
}
