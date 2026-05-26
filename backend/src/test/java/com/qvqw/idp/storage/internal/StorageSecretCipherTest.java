package com.qvqw.idp.storage.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StorageSecretCipher} 单元测试：覆盖加解密、空值、不同密钥隔离。
 */
class StorageSecretCipherTest {

    @Test
    void encryptThenDecryptShouldReturnOriginal() {
        StorageSecretCipher cipher = new StorageSecretCipher("test-cipher-key-1");
        String plain = "my-secret-key-XYZ-12345";
        String cipherText = cipher.encrypt(plain);

        assertThat(cipherText).isNotNull().isNotEqualTo(plain);
        assertThat(cipher.decrypt(cipherText)).isEqualTo(plain);
    }

    @Test
    void encryptNullOrEmptyReturnsNull() {
        StorageSecretCipher cipher = new StorageSecretCipher("test-cipher-key-1");
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("")).isNull();
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("")).isNull();
    }

    @Test
    void differentKeysProduceDifferentCipherText() {
        StorageSecretCipher cipherA = new StorageSecretCipher("key-A");
        StorageSecretCipher cipherB = new StorageSecretCipher("key-B");
        String text = "secret";
        assertThat(cipherA.encrypt(text)).isNotEqualTo(cipherB.encrypt(text));
    }

    @Test
    void sameInputProducesDifferentCipherTextDueToRandomIv() {
        StorageSecretCipher cipher = new StorageSecretCipher("test-cipher-key");
        String text = "secret";
        String first = cipher.encrypt(text);
        String second = cipher.encrypt(text);
        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo(text);
        assertThat(cipher.decrypt(second)).isEqualTo(text);
    }
}
