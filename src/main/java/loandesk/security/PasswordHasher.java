package loandesk.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Salted password hashing using PBKDF2 with HMAC-SHA256 from the Java standard library.
 */
public class PasswordHasher {

    /** Key-derivation function provided by {@link SecretKeyFactory}. */
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Default iteration count (OWASP's recommended figure for PBKDF2-HMAC-SHA256). */
    public static final int DEFAULT_ITERATIONS = 210_000;

    /** Salt length: 16 bytes = 128 bits. */
    public static final int SALT_BYTES = 16;

    /** Output (hash) length: 256 bits = 32 bytes. */
    public static final int KEY_LENGTH_BITS = 256;

    /** Cryptographically strong random number generator for salts. */
    private final SecureRandom secureRandom = new SecureRandom();

    /** Iterations used for NEW hashes (existing hashes keep their own stored count). */
    private final int iterations;

    /** Creates a hasher with the default (secure) iteration count. */
    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    /**
     * Creates a hasher with a custom iteration count. Tests use a small number so they run
     * quickly; the real application always uses {@link #DEFAULT_ITERATIONS}.
     *
     * @param iterations number of PBKDF2 iterations (must be positive)
     */
    public PasswordHasher(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be positive");
        }
        this.iterations = iterations;
    }

    /**
     * Hashes a password with a brand-new random salt.
     *
     * @param password the plaintext password
     * @return the salt, hash and parameters to store
     */
    public HashedPassword hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt); // a different salt every time
        byte[] hash = derive(password, salt, iterations, KEY_LENGTH_BITS);

        Base64.Encoder encoder = Base64.getEncoder();
        return new HashedPassword(ALGORITHM, iterations, encoder.encodeToString(salt),
                encoder.encodeToString(hash));
    }

    /**
     * Checks a typed password against a stored hash.
     *
     * @param password the password the user typed
     * @param stored   the stored salt/hash/parameters
     * @return true if the password is correct
     */
    public boolean verify(String password, HashedPassword stored) {
        if (password == null || !ALGORITHM.equals(stored.algorithm())) {
            return false;
        }
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] salt = decoder.decode(stored.saltBase64());
        byte[] expectedHash = decoder.decode(stored.hashBase64());

        // Re-derive using the SAME salt, iteration count and output length as when stored.
        byte[] actualHash = derive(password, salt, stored.iterations(), expectedHash.length * 8);

        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    /**
     * Runs PBKDF2 to turn a password + salt into a fixed-length hash.
     */
    private static byte[] derive(String password, byte[] salt, int iterationCount, int keyLengthBits) {
        char[] passwordChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterationCount, keyLengthBits);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available: " + e.getMessage(), e);
        } finally {
            //Remove the password characters from memory as soon as its been used.
            spec.clearPassword();
            Arrays.fill(passwordChars, '\0');
        }
    }
}
