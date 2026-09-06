package com.isufst.mdrrmosystem.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptedDoubleConverter
        implements AttributeConverter<Double, String> {

    private static String encryptionKey;

    @Value("${mdrrmo.encryption.key}")
    public void setEncryptionKey(String key) {
        EncryptedDoubleConverter.encryptionKey = key;
    }

    @Override
    public String convertToDatabaseColumn(Double value) {

        if (value == null) {
            return null;
        }

        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException(
                    "Encryption key is not configured."
            );
        }

        return EncryptionUtil.encrypt(
                value.toString(),
                encryptionKey
        );
    }

    @Override
    public Double convertToEntityAttribute(String encryptedValue) {

        if (encryptedValue == null) {
            return null;
        }

        try {
            if (encryptionKey == null || encryptionKey.isBlank()) {
                throw new IllegalStateException(
                        "Encryption key is null or empty"
                );
            }

            System.out.println("=== ENCRYPTION DEBUG ===");
            System.out.println("Encrypted value: " + encryptedValue);
            System.out.println("Key loaded: " + !encryptionKey.isBlank());
            System.out.println("Key length: " + encryptionKey.length());

            String decrypted = EncryptionUtil.decrypt(
                    encryptedValue,
                    encryptionKey
            );

            System.out.println("Decrypted value: " + decrypted);
            System.out.println("========================");

            return Double.parseDouble(decrypted);

        } catch (Exception e) {

            System.err.println("=== ENCRYPTION ERROR ===");
            System.err.println("Encrypted value: " + encryptedValue);
            System.err.println("Key loaded: " + (encryptionKey != null));
            System.err.println("Key length: " +
                    (encryptionKey == null ? "NULL" : encryptionKey.length()));

            e.printStackTrace();

            System.err.println("========================");

            throw new IllegalStateException(
                    "Failed to decrypt database value",
                    e
            );
        }
    }
}