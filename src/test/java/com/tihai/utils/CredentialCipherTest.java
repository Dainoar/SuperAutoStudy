package com.tihai.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialCipherTest {

    @Test
    void encryptsSensitiveValuesWithANonDeterministicAuthenticatedCiphertext() {
        CredentialCipher cipher = new CredentialCipher("0123456789abcdef0123456789abcdef");

        String first = cipher.encrypt("sensitive-value");
        String second = cipher.encrypt("sensitive-value");

        assertTrue(cipher.isEncrypted(first));
        assertFalse(first.equals(second));
        assertEquals("sensitive-value", cipher.decrypt(first));
        assertEquals("legacy-value", cipher.decrypt("legacy-value"));
    }
}
