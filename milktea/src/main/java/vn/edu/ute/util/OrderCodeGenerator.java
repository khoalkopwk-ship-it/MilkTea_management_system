package vn.edu.ute.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class OrderCodeGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderCodeGenerator() {
    }

    public static String generate() {

        String time =
                LocalDateTime.now()
                        .format(FORMATTER);

        String random =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase();

        return "ORD-" + time + "-" + random;
    }
}