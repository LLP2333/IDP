package com.qvqw.idp.storage.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * S3 SecretKey 加解密工具。
 *
 * <p>采用 AES/GCM/NoPadding 算法，密钥来自配置项 {@code idp.storage.secret-key-cipher}，
 * 通过 SHA-256 派生为 256-bit 密钥；每次加密使用随机 IV（12 字节），密文 = Base64(IV || ciphertext || tag)。</p>
 *
 * <p>选择 GCM 模式的原因：自带消息认证、防篡改；密钥派生避免直接使用配置串长度不固定的问题。</p>
 */
@Component
public class StorageSecretCipher {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public StorageSecretCipher(@Value("${idp.storage.secret-key-cipher:idp-default-storage-cipher-please-override}") String secret) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(hash, ALGORITHM);
        } catch (Exception e) {
            throw new IllegalStateException("初始化存储密钥加密器失败", e);
        }
    }

    /**
     * 对明文加密；空字符串原样返回 {@code null}，便于持久化层判断 “未设置”。
     *
     * @param plaintext 明文 SecretKey
     * @return Base64 编码的密文，输入为空时返回 {@code null}
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("加密 SecretKey 失败", e);
        }
    }

    /**
     * 解密；密文为空返回 {@code null}。
     *
     * @param ciphertextBase64 Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null || ciphertextBase64.isEmpty()) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(ciphertextBase64);
            if (all.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不合法");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] cipherText = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密 SecretKey 失败", e);
        }
    }
}
