package com.isufst.mdrrmosystem.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final String AES = "AES";

    private EncryptionUtil() {
    }

    public static String encrypt(String value, String base64SecretKey) {

        if (value == null) {
            return null;
        }

        try {
            // Decode Base64 key into the actual 32-byte AES-256 key
            byte[] keyBytes =
                    Base64.getDecoder().decode(base64SecretKey);

            if (keyBytes.length != 32) {
                throw new IllegalArgumentException(
                        "Encryption key must be exactly 32 bytes for AES-256."
                );
            }

            SecretKeySpec key =
                    new SecretKeySpec(keyBytes, AES);

            // Generate a random IV for every encryption
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(ALGORITHM);

            GCMParameterSpec gcmSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    gcmSpec
            );

            byte[] encrypted =
                    cipher.doFinal(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            /*
             * Store:
             *
             * IV + encrypted data
             *
             * The IV does not need to be secret.
             */
            byte[] combined =
                    new byte[iv.length + encrypted.length];

            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encrypted,
                    0,
                    combined,
                    iv.length,
                    encrypted.length
            );

            return Base64.getEncoder()
                    .encodeToString(combined);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to encrypt value",
                    e
            );
        }
    }

    public static String decrypt(
            String encryptedValue,
            String base64SecretKey
    ) {

        if (encryptedValue == null) {
            return null;
        }

        try {
            // Decode the Base64 AES key
            byte[] keyBytes =
                    Base64.getDecoder().decode(base64SecretKey);

            if (keyBytes.length != 32) {
                throw new IllegalArgumentException(
                        "Encryption key must be exactly 32 bytes for AES-256."
                );
            }

            SecretKeySpec key =
                    new SecretKeySpec(keyBytes, AES);

            // Decode encrypted database value
            byte[] combined =
                    Base64.getDecoder()
                            .decode(encryptedValue);

            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException(
                        "Invalid encrypted value."
                );
            }

            // Extract IV
            byte[] iv =
                    new byte[IV_LENGTH];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            // Extract encrypted data
            byte[] encrypted =
                    new byte[
                            combined.length - IV_LENGTH
                            ];

            System.arraycopy(
                    combined,
                    IV_LENGTH,
                    encrypted,
                    0,
                    encrypted.length
            );

            Cipher cipher =
                    Cipher.getInstance(ALGORITHM);

            GCMParameterSpec gcmSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    gcmSpec
            );

            byte[] decrypted =
                    cipher.doFinal(encrypted);

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to decrypt value",
                    e
            );
        }
    }

//    public static void test(String base64SecretKey) {
//
//        String original = "60000";
//
//        String encrypted = encrypt(
//                original,
//                base64SecretKey
//        );
//
//        String decrypted = decrypt(
//                encrypted,
//                base64SecretKey
//        );
//
//        System.out.println("===== AES TEST =====");
//        System.out.println("Original:  " + original);
//        System.out.println("Encrypted: " + encrypted);
//        System.out.println("Decrypted: " + decrypted);
//        System.out.println("====================");
//    }
}