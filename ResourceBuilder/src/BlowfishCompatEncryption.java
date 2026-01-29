

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;

/**
 * Decrypts Escapists map and resource files, using their default key.
 * <br>
 * <br>
 * Solution idea from - http://stackoverflow.com/questions/11422497/whats-the-difference-between-blowfish-and-blowfish-compat
 * <br>
 * Removing the null bytes at the end of the decrypted content is fine since we are dealing with strings
 * http://stackoverflow.com/questions/5672012/mcrypt-encrypt-adding-s-bunch-of-00-to-end-of-string
 *
 * @author Zirow (code) - Major kudos!
 **/
public final class BlowfishCompatEncryption {
    private BlowfishCompatEncryption() {}

    /**
     * The default encryption key used by the Escapists.
     *
     * This is a magic value!
     */
    private static final String ENCRYPTION_KEY = "mothking";

    /**
     * Decrypts a Blowfish encrypted file using the built-in key.
     *
     * @param t The file to read and decrypt
     * @return The byte contents of this file decrypted
     **/
//    public static byte[] decrypt(File t) throws IOException {
//        // get the data
//        byte[] data = IOUtils.toByteArray(t);
//
//        return decryptBytes(data, ENCRYPTION_KEY.getBytes());
//    }

    /**
     * Decrypts a Blowfish encrypted array using the built-in key.
     *
     * @param t The file to read and decrypt
     * @return The byte contents of this file decrypted
     **/
    public static byte[] decrypt(byte[] t) throws IOException {
        return decryptBytes(t, ENCRYPTION_KEY.getBytes());
    }

    /**
     * Decrypts the specified byte array encrypted via Blowfish using
     * the specified key.
     *
     * @param data The data to decrypt
     * @param key The key to use for decryption
     * @return The decrypted bytes
     **/
    private static byte[] decryptBytes(byte[] data, byte[] key) throws IOException {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher;

            // get the cipher
            String cipherInstName = "Blowfish/ECB/NoPadding";
            cipher = Cipher.getInstance(cipherInstName);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);

            // reverse
            inplaceReverse(data);

            // decrypt
            byte[] decrypted = cipher.doFinal(data);
            inplaceReverse(decrypted);

            // Remove all null bytes at the end until a character is found
            ///*
            int end = decrypted.length - 1;

            while (decrypted[end] == 0)
                end--;

            byte[] result = new byte[end + 1];

            System.arraycopy(decrypted, 0, result, 0, end + 1);

            decrypted = result;
            //*/
            return decrypted;
        } catch (Exception e) {
            // Generic Exception handler to make sure things don't break upstream.
            throw new IOException(e);
        }
    }

    /**
     * Encrypts a Blowfish encrypted file using the built-in key.
     *
     * @param t The file to read and encrypt
     * @return The byte contents of this file encrypted
     **/
//    public static byte[] encrypt(File t) throws IOException {
//        // get the data
//        byte[] data = IOUtils.toByteArray(t);
//
//        return encryptBytes(data, ENCRYPTION_KEY.getBytes());
//    }

    /**
     * Encrypts a Blowfish encrypted array using the built-in key.
     *
     * @param t The file to read and encrypt
     * @return The byte contents of this file encrypted
     **/
    public static byte[] encrypt(byte[] t) throws IOException {
        return encryptBytes(t, ENCRYPTION_KEY.getBytes());
    }


    /**
     * Encrypted the specified byte array via Blowfish using
     * the specified key.
     *
     * @param data The data to encrypt
     * @param key The key to use for encryption
     * @return The encrypted bytes
     **/
    private static byte[] encryptBytes(byte[] data, byte[] key) throws IOException {
        try {
            ///*
            if (data.length % 8 > 0) {
                // we need to add padding nulls
                int left = 8 - (data.length % 8);

                byte[] newData = new byte[data.length + left];

                System.arraycopy(data, 0, newData, 0, data.length);
                // no need to put 0's, it's byte's default value
                data = newData;
            }
            //*/

            SecretKeySpec skeySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher;

            // get the cipher
            // shouldn't i need to do some kind of padding here?
            String cipherInstName = "Blowfish/ECB/NoPadding";
            cipher = Cipher.getInstance(cipherInstName);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            inplaceReverse(data);
            // decrypt
            byte[] encrypted = cipher.doFinal(data);
            inplaceReverse(encrypted);
            //inplaceReverse(data);
            return encrypted;

        } catch (Exception e) {
            // Generic Exception handler to make sure things don't break upstream.
            throw new IOException(e);
        }
    }

    /**
     * Swaps around bytes in the specified array to remain compatible with
     * Blowfish-Compat encryption.
     *
     * @param data The data to reverse.
     */
    private static void inplaceReverse(byte[] data) {
        byte a0, a1, a2, a3;
        for (int i = 0; i < data.length; i += 4) {
            a0 = data[i];
            a1 = data[i + 1];
            a2 = data[i + 2];
            a3 = data[i + 3];

            data[i] = a3;
            data[i + 1] = a2;
            data[i + 2] = a1;
            data[i + 3] = a0;
        }
    }
}