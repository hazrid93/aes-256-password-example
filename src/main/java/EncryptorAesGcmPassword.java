import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptorAesGcmPassword {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";

    /* best practice
      Use a 12 byte initialization vector that is never reused with the same key (use a strong pseudorandom number generator like SecureRandom)
      Use a 128 bit authentication tag length
      Use a 128 bit key length or 256 (128 seems fine!)
     */
    private static final int TAG_LENGTH_BIT = 128; // must be one of {128, 120, 112, 104, 96}
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    // return a base64 encoded AES encrypted text
    public static String encrypt(byte[] pText, String password) throws Exception {

        // 16 bytes salt
        byte[] salt = CryptoUtils.getRandomNonce(SALT_LENGTH_BYTE);

        // GCM recommended 12 bytes iv
        byte[] iv = CryptoUtils.getRandomNonce(IV_LENGTH_BYTE);

        // secret key from password
        SecretKey aesKeyFromPassword = CryptoUtils.getAESKeyFromPassword(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

        // ASE-GCM needs GCMParameterSpec
        cipher.init(Cipher.ENCRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] cipherText = cipher.doFinal(pText);

        // prefix IV and Salt to cipher text
        byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
                .put(iv)
                .put(salt)
                .put(cipherText)
                .array();

        System.out.println("encoded data: " +  cipherTextWithIvSalt);
        // string representation, base64, send this string to other for decryption.
        return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);

    }

    // we need the same password, salt and iv to decrypt it
    private static String decrypt(String cText, String password) throws Exception {

        byte[] decode = Base64.getDecoder().decode(cText.getBytes(UTF_8));

        // get back the iv and salt from the cipher text
        ByteBuffer bb = ByteBuffer.wrap(decode);

        System.out.println("decoded data: " +  decode);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);
        byte[] salt = new byte[SALT_LENGTH_BYTE];
        bb.get(salt);
        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);
        // get back the aes key from the same password and salt
        SecretKey aesKeyFromPassword = CryptoUtils.getAESKeyFromPassword(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

        cipher.init(Cipher.DECRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText, UTF_8);

    }

    public static void main(String[] args) throws Exception {

        String OUTPUT_FORMAT = "%-30s:%s";
        String PASSWORD = "=3K&P@ZCaP$_q0z";
        String pText = "hazrid love apple";

        String encryptedTextBase64 = EncryptorAesGcmPassword.encrypt(pText.getBytes(UTF_8), PASSWORD);

        System.out.println("\n------ AES GCM Password-based Encryption ------");
        System.out.println(String.format(OUTPUT_FORMAT, "Input (plain text)", pText));
        System.out.println(String.format(OUTPUT_FORMAT, "Encrypted (base64) ", encryptedTextBase64));

        System.out.println("\n------ AES GCM Password-based Decryption ------");
        System.out.println(String.format(OUTPUT_FORMAT, "Input (base64)", encryptedTextBase64));

        String decryptedText = EncryptorAesGcmPassword.decrypt(encryptedTextBase64, PASSWORD);
        System.out.println(String.format(OUTPUT_FORMAT, "Decrypted (plain text)", decryptedText));

    }

}