package com.buildledger.app;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

/** Native HTTPS only. Redirects are disabled so credentials cannot cross origins. */
final class CloudHttp {
    static boolean configured(){return BuildConfig.SUPABASE_URL.matches("https://[a-z0-9-]+\\.supabase\\.co")&&!BuildConfig.SUPABASE_KEY.trim().isEmpty();}
    static void requireConfigured() throws IOException {if(!configured())throw new IOException("Sign-in is being configured. The app owner must connect the cloud project first.");}
    private HttpsURLConnection connection(String path,String method,String token,String mime) throws Exception {
        requireConfigured();if(!path.startsWith("/")||path.contains("\r")||path.contains("\n"))throw new IOException("Invalid cloud request.");
        HttpsURLConnection c=(HttpsURLConnection)new URL(BuildConfig.SUPABASE_URL+path).openConnection();
        c.setRequestMethod(method);c.setInstanceFollowRedirects(false);c.setConnectTimeout(20_000);c.setReadTimeout(90_000);
        c.setRequestProperty("apikey",BuildConfig.SUPABASE_KEY);if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("Content-Type",mime);return c;
    }
    JSONObject json(String path,String method,String token,JSONObject body) throws Exception {
        byte[] bytes=body==null?null:body.toString().getBytes(StandardCharsets.UTF_8);
        HttpsURLConnection c=connection(path,method,token,"application/json");
        try{if(bytes!=null){c.setDoOutput(true);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}check(c);try(InputStream in=c.getInputStream()){String result=new String(bounded(in,4_000_000),StandardCharsets.UTF_8);if(result.trim().isEmpty())return new JSONObject();if(result.startsWith("["))return new JSONObject().put("rows",new JSONArray(result));if(result.matches("[0-9]+"))return new JSONObject().put("value",Long.parseLong(result));return new JSONObject(result);}}finally{c.disconnect();}
    }
    void upload(String path,String token,File file,String mime) throws Exception {
        HttpsURLConnection c=connection("/storage/v1/object/buildledger/"+path,"POST",token,mime);c.setRequestProperty("x-upsert","true");
        try{c.setDoOutput(true);c.setFixedLengthStreamingMode(file.length());try(InputStream in=new FileInputStream(file);OutputStream out=c.getOutputStream()){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}check(c);}finally{c.disconnect();}
    }
    void download(String path,String token,File target,long expected) throws Exception {
        HttpsURLConnection c=connection("/storage/v1/object/authenticated/buildledger/"+path,"GET",token,"application/octet-stream");
        try{check(c);long size=0;try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(target)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){size+=n;if(size>expected||size>LedgerStore.MAX_TOTAL+6_000_000)throw new IOException("Cloud backup exceeds its declared size.");out.write(buffer,0,n);}}if(size!=expected)throw new IOException("Cloud backup download was incomplete.");}finally{c.disconnect();}
    }
    void remove(String path,String token) throws Exception {json("/storage/v1/object/buildledger","DELETE",token,new JSONObject().put("prefixes",new JSONArray().put(path)));}
    private void check(HttpsURLConnection c) throws Exception {
        int code=c.getResponseCode();if(code>=200&&code<300)return;
        if(code==401||code==403)throw new IOException("Cloud access was refused. Sign in again or ask the app owner to check access settings.");
        if(code==409)throw new IOException("Another device changed this workspace. Download cloud records before continuing.");
        // Do not reflect server messages, tokens, request URLs or provider errors into the UI.
        throw new IOException("Cloud request failed ("+code+"). Phone records were kept; try again when connected.");
    }
    static byte[] bounded(InputStream in,int limit) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Cloud response is too large.");out.write(b,0,n);}return out.toByteArray();}
}
