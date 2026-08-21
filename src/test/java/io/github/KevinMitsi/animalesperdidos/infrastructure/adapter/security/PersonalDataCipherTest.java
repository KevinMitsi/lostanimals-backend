package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.security;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PersonalDataCipherTest {
    private final PersonalDataCipher cipher = new PersonalDataCipher(key(1), key(33));

    @Test
    void protectsAndRecoversPhoneAndDocumentNumber() {
        String phone = "+573001234567";
        String document = "1094912345";

        String protectedPhone = cipher.encryptPhone(phone);
        String protectedDocument = cipher.encryptDocumentNumber(document);

        assertNotEquals(phone, protectedPhone);
        assertNotEquals(document, protectedDocument);
        assertEquals(phone, cipher.decryptPhone(protectedPhone));
        assertEquals(document, cipher.decryptDocumentNumber(protectedDocument));
    }

    @Test
    void producesDifferentProtectedValuesForRepeatedEncryption() {
        assertNotEquals(cipher.encryptPhone("3001234567"), cipher.encryptPhone("3001234567"));
    }

    @Test
    void lookupValuesAreStableButSeparatedByPurpose() {
        assertEquals(cipher.phoneLookup(" 3001234567 "), cipher.phoneLookup("3001234567"));
        assertNotEquals(cipher.phoneLookup("3001234567"), cipher.documentNumberLookup("3001234567"));
    }

    @Test
    void rejectsTamperedOrCrossPurposeValues() {
        String protectedPhone = cipher.encryptPhone("3001234567");
        String tampered = protectedPhone.substring(0, protectedPhone.length() - 1)
                + (protectedPhone.endsWith("A") ? "B" : "A");

        assertThrows(IllegalArgumentException.class, () -> cipher.decryptPhone(tampered));
        assertThrows(IllegalArgumentException.class, () -> cipher.decryptDocumentNumber(protectedPhone));
    }

    @Test
    void rejectsMissingOrInvalidKeys() {
        assertThrows(IllegalStateException.class, () -> new PersonalDataCipher("", key(1)));
        assertThrows(IllegalStateException.class, () -> new PersonalDataCipher(key(1), Base64.getEncoder().encodeToString(new byte[16])));
        String duplicatedKey = key(7);
        assertThrows(IllegalStateException.class, () -> new PersonalDataCipher(duplicatedKey, duplicatedKey));
    }

    private static String key(int seed) {
        byte[] value = new byte[32];
        new SecureRandom(new byte[]{(byte) seed}).nextBytes(value);
        return Base64.getEncoder().encodeToString(value);
    }
}
