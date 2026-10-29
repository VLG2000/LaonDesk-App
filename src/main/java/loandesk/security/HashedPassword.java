package loandesk.security;

/**
 * Everything needed to verify a password later, exactly as stored in the database.
 * The plaintext password itself is never stored.
 *
 * @param algorithm  key-derivation algorithm name, e.g. "PBKDF2WithHmacSHA256"
 * @param iterations how many PBKDF2 iterations were used
 * @param saltBase64 the random per-account salt, Base64-encoded text
 * @param hashBase64 the derived hash, Base64-encoded text
 */
public record HashedPassword(String algorithm, int iterations, String saltBase64, String hashBase64) {
}
