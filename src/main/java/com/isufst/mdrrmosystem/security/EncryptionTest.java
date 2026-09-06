package com.isufst.mdrrmosystem.security;

public class EncryptionTest {

    public static void main(String[] args) {

        String key =
                "RFsRmVJyzO6jMIYaGGkqMQG9Qqo3LfNaU/5+ELsxe94=";

        String encrypted =
                EncryptionUtil.encrypt("110000", key);

        System.out.println("Encrypted:");
        System.out.println(encrypted);

        System.out.println("Decrypted:");
        System.out.println(
                EncryptionUtil.decrypt(encrypted, key)
        );
    }
}