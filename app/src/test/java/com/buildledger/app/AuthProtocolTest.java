package com.buildledger.app;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthProtocolTest {
    @Test public void pkceMatchesRfc7636Vector() throws Exception {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",AuthProtocol.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }
    @Test public void callbackRejectsOtherAppHostNonceAndExpiredFlow(){
        assertTrue(AuthProtocol.callbackMatches("com.buildledger.app","auth","/callback/secret","com.buildledger.app","secret",1000,2000));
        assertFalse(AuthProtocol.callbackMatches("another.app","auth","/callback/secret","com.buildledger.app","secret",1000,2000));
        assertFalse(AuthProtocol.callbackMatches("com.buildledger.app","evil","/callback/secret","com.buildledger.app","secret",1000,2000));
        assertFalse(AuthProtocol.callbackMatches("com.buildledger.app","auth","/callback/wrong","com.buildledger.app","secret",1000,2000));
        assertFalse(AuthProtocol.callbackMatches("com.buildledger.app","auth","/callback/secret","com.buildledger.app","secret",1000,601000));
        assertFalse(AuthProtocol.callbackMatches("com.buildledger.app","auth","/callback/secret","com.buildledger.app","secret",1000,500));
    }
    @Test public void identityCannotEscapeAccountDirectory() throws Exception {
        assertEquals("678a81bc-a934-42a3-9867-d9b4ae61a1f4",AuthProtocol.accountId("678a81bc-a934-42a3-9867-d9b4ae61a1f4"));
        for(String bad:new String[]{"../legacy","legacy","user@example.com","accounts/123"})try{AuthProtocol.accountId(bad);fail("Accepted invalid identity");}catch(java.io.IOException expected){}
    }
    @Test public void verifierHasFreshCryptographicEntropy(){String first=AuthProtocol.random();assertTrue(first.matches("[a-zA-Z0-9_-]{43}"));assertNotEquals(first,AuthProtocol.random());}
}
