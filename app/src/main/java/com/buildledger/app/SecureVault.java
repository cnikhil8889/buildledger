package com.buildledger.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Tokens and account preferences never enter WebView storage or backups. */
final class SecureVault {
    private final AtomicFile file;
    private static final String KEY="BuildLedgerAccountKeyV1";
    SecureVault(Context context){file=new AtomicFile(new File(context.getFilesDir(),"account.enc"));}
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(KEY))return (SecretKey)ks.getKey(KEY,null);
        KeyGenerator generator=KeyGenerator.getInstance("AES","AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    synchronized JSONObject read() throws Exception {
        if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists())return new JSONObject();
        byte[] b;try(InputStream in=file.openRead()){b=CloudHttp.bounded(in,200_000);}
        if(b.length<29||b.length>200_000)throw new IOException("Account settings could not be read.");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,b,0,12));
        return new JSONObject(new String(cipher.doFinal(b,12,b.length-12),StandardCharsets.UTF_8));
    }
    synchronized void write(JSONObject value) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        FileOutputStream out=null;try{out=file.startWrite();out.write(cipher.getIV());out.write(cipher.doFinal(value.toString().getBytes(StandardCharsets.UTF_8)));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
}
