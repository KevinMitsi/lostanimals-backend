package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.security;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

public final class PersonalDataCipher {
    private static final String VERSION = "v1";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec lookupKey;
    private final SecureRandom secureRandom;

    public PersonalDataCipher(String encryptionKey, String lookupKey) {
        this(encryptionKey, lookupKey, new SecureRandom());
    }

    PersonalDataCipher(String encryptionKey, String lookupKey, SecureRandom secureRandom) {
        byte[] encryptionKeyBytes = decodeKey(encryptionKey, "PERSONAL_DATA_ENCRYPTION_KEY");
        byte[] lookupKeyBytes = decodeKey(lookupKey, "PERSONAL_DATA_LOOKUP_KEY");
        if (Arrays.equals(encryptionKeyBytes, lookupKeyBytes)) {
            throw new IllegalStateException("Personal data protection keys must be different");
        }
        this.encryptionKey = new SecretKeySpec(encryptionKeyBytes, "AES");
        this.lookupKey = new SecretKeySpec(lookupKeyBytes, "HmacSHA256");
        this.secureRandom = secureRandom;
    }

    public String encryptPhone(String value) {
        return encrypt(value, "phone");
    }

    public String decryptPhone(String value) {
        return decrypt(value, "phone");
    }

    public String phoneLookup(String value) {
        return lookup(value, "phone");
    }

    public String encryptDocumentNumber(String value) {
        return encrypt(value, "document-number");
    }

    public String decryptDocumentNumber(String value) {
        return decrypt(value, "document-number");
    }

    public String documentNumberLookup(String value) {
        return lookup(value, "document-number");
    }

    private String encrypt(String value, String purpose) {
        if (value == null) return null;
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return VERSION + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Personal data encryption failed", error);
        }
    }

    private String decrypt(String value, String purpose) {
        if (value == null) return null;
        try {
            String[] parts = value.split("\\.", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0])) throw new IllegalArgumentException("Unsupported protected value");
            byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
            if (nonce.length != NONCE_BYTES) throw new IllegalArgumentException("Invalid protected value");
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (AEADBadTagException error) {
            throw new IllegalArgumentException("Protected personal data cannot be authenticated", error);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            if (error instanceof IllegalArgumentException invalid) throw invalid;
            throw new IllegalStateException("Personal data decryption failed", error);
        }
    }

    private String lookup(String value, String purpose) {
        if (value == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(lookupKey);
            byte[] normalized = normalize(value).getBytes(StandardCharsets.UTF_8);
            mac.update(purpose.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            return java.util.HexFormat.of().formatHex(mac.doFinal(normalized));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Personal data lookup protection failed", error);
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] decodeKey(String encoded, String name) {
        if (encoded == null || encoded.isBlank()) throw new IllegalStateException(name + " is required");
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            if (decoded.length != 32) throw new IllegalStateException(name + " must decode to exactly 32 bytes");
            return decoded;
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException(name + " must be valid Base64", error);
        }
    }
}
