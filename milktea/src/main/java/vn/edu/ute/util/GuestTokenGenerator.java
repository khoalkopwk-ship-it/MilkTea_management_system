package vn.edu.ute.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class GuestTokenGenerator {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private GuestTokenGenerator() {
    }

    public static String generate() {

        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}