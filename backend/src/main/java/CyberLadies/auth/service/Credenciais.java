package CyberLadies.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Credenciais {
    private static final SecureRandom RANDOM=new SecureRandom();
    private Credenciais() {}
    public static String token() {
        byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes); return HexFormat.of().formatHex(bytes);
    }
    public static String hashBearer(String header) {
        if(header==null || !header.matches("Bearer [0-9a-f]{64}"))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Entre para acessar sua conta.");
        return hash(header.substring(7));
    }
    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}