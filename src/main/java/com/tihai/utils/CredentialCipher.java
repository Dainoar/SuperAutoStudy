package com.tihai.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Encrypts locally persisted credentials and session cookies with AES-GCM. */
@Component
public class CredentialCipher {

    private static final String VERSION = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialCipher(@Value("${security.credential-key:}") String keyMaterial) {
        byte[] keyBytes = keyMaterial == null ? new byte[0] : keyMaterial.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("security.credential-key 必须是 16、24 或 32 字节，并应通过环境变量提供");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return VERSION + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("敏感数据加密失败", e);
        }
    }

    /** Supports a gradual migration of existing plaintext rows on their next update. */
    public String decrypt(String value) {
        if (value == null || value.isEmpty() || !isEncrypted(value)) {
            return value;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(VERSION.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("敏感数据解密失败", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(VERSION);
    }
}
