package io.github.teams4j.bot;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

/** Tokens as the Bot Framework would issue them, signed with a real RSA key, and the key set to match. */
public final class TestTokens {

    public static final String ISSUER = "https://api.botframework.com";

    private TestTokens() {}

    public static KeyPair rsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A token for the app, valid for an hour from {@code now}. */
    public static String token(String kid, KeyPair key, String appId, String serviceUrl, Instant now) {
        return token(
                kid,
                key,
                claimsFor(
                        ISSUER,
                        appId,
                        now.getEpochSecond(),
                        now.plusSeconds(3600).getEpochSecond(),
                        serviceUrl));
    }

    public static String token(String kid, KeyPair key, String claims) {
        return tokenWithHeader("{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"" + kid + "\"}", key, claims);
    }

    /** The {@code Authorization} header value: {@code Bearer} and the signed compact JWT. */
    public static String tokenWithHeader(String header, KeyPair key, String claims) {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String signed = b64.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(claims.getBytes(StandardCharsets.UTF_8));
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key.getPrivate());
            signature.update(signed.getBytes(StandardCharsets.US_ASCII));
            return "Bearer " + signed + "." + b64.encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String jwk(String kid, KeyPair key) {
        RSAPublicKey pub = (RSAPublicKey) key.getPublic();
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return "{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"" + kid + "\",\"n\":\""
                + b64.encodeToString(unsigned(pub.getModulus().toByteArray())) + "\",\"e\":\""
                + b64.encodeToString(unsigned(pub.getPublicExponent().toByteArray())) + "\"}";
    }

    /** BigInteger.toByteArray carries a sign byte; JWK wants the magnitude. */
    private static byte[] unsigned(byte[] twosComplement) {
        if (twosComplement.length > 1 && twosComplement[0] == 0) {
            byte[] out = new byte[twosComplement.length - 1];
            System.arraycopy(twosComplement, 1, out, 0, out.length);
            return out;
        }
        return twosComplement;
    }

    public static String jwks(String... keys) {
        return "{\"keys\":[" + String.join(",", keys) + "]}";
    }

    public static String claimsFor(String iss, String aud, long nbf, long exp, String serviceUrl) {
        return "{\"iss\":\"" + iss + "\",\"aud\":\"" + aud + "\",\"serviceurl\":\"" + serviceUrl + "\",\"nbf\":" + nbf
                + ",\"exp\":" + exp + "}";
    }
}
