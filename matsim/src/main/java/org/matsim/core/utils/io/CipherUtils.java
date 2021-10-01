package org.matsim.core.utils.io;

import com.google.common.io.CharStreams;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Utility class for reading encrypted input file.
 * This class is also integrated into {@link IOUtils} to decrypt files ending with ".enc" automatically.
 * <p>
 * This class expects files to be encrypted in one specific way with certain parameters.
 * This can be achieved with {@code openssl} from the command line using:
 * <pre>
 * openssl enc -aes256 -md sha512 -pbkdf2 -iter 10000 -in [some.secret] -out [some.secret.enc]
 * </pre>
 * <p>
 * Please note, that the arguments <emph>-aes256 -md sha512 -pbkdf2 -iter 10000</emph> are strictly required and can
 * not be changed.
 * <p>
 * The password for decryption has to be made available as environment variable or system property to the program at runtime.
 * Set {@code MATSIM_DECRYPTION_PASSWORD} to the password needed for the files. This password has to be the same for all encrypted files.
 */
public class CipherUtils {

    public static final  String ENVIRONMENT_VARIABLE = "MATSIM_DECRYPTION_PASSWORD";

    private CipherUtils() {
    }

    private static Key getKeyForFile(byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

        String pw = System.getProperty(ENVIRONMENT_VARIABLE, System.getenv(ENVIRONMENT_VARIABLE));

        if (pw == null)
            throw new IllegalStateException("No password specified for encrypted file. Please set " + ENVIRONMENT_VARIABLE + " environment variable.");

        // generates 256bit key and 128bit iv
        PBEKeySpec keySpec = new PBEKeySpec(pw.toCharArray(), salt, 10000, 256 + 128);

        return factory.generateSecret(keySpec);
    }


    /**
     * Decrypts an input stream.
     * For more information how to encrypt files and how supply the password please see {@link CipherUtils}.
     *
     * @param is input stream to decrypt
     * @see CipherUtils
     */
    public static InputStream getDecryptedInput(InputStream is) throws IOException, GeneralSecurityException {

        byte[] header = new byte[16];

        if (is.read(header) != 16)
            throw new IllegalStateException("Read too few bytes. Encrypted file is most likely corrupted.");

        String h = new String(header, 0, 8, StandardCharsets.US_ASCII);

        if (!h.equals("Salted__"))
            throw new IllegalStateException("File header must start with 'Salted__'.");

        byte[] salt = Arrays.copyOfRange(header, 8, 16);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        Key tmp = getKeyForFile(salt);

        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), 0, 32, "AES");
        IvParameterSpec ivspec = new IvParameterSpec(tmp.getEncoded(), 32, 16);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);

        return new CipherInputStream(is, cipher);
    }


    public static void main(String[] args) throws Exception {

        System.setProperty(CipherUtils.ENVIRONMENT_VARIABLE, "abc123");

        InputStream is = getDecryptedInput(new FileInputStream("some.secret.enc"));

        System.out.println(CharStreams.toString(new InputStreamReader(is)));

    }

}
