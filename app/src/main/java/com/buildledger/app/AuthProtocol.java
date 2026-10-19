package com.buildledger.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Pure protocol helpers, also exercised by local JVM tests. */
final class AuthProtocol {
    static final List<String> PROVIDERS=Arrays.asList("google","apple","azure");
    static String random(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    static String challenge(String verifier) throws Exception {return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));}
    static String accountId(String id) throws IOException {try{return UUID.fromString(id).toString();}catch(Exception e){throw new IOException("Invalid account identity.");}}
    static boolean callbackMatches(String scheme,String host,String path,String expectedScheme,String nonce,long started,long now){return expectedScheme.equals(scheme)&&"auth".equals(host)&&("/callback/"+nonce).equals(path)&&!nonce.isEmpty()&&now>=started&&now-started<600_000;}
}
