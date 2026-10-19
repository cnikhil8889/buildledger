package com.buildledger.app;

import android.content.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Native OAuth sessions and explicit per-account phone/cloud storage choices. */
final class AccountManager {
    private static AccountManager instance;
    static synchronized AccountManager get(Context context) throws Exception {if(instance==null)instance=new AccountManager(context.getApplicationContext());return instance;}
    private final android.app.Application context;
    private final SecureVault vault;
    private final CloudHttp http=new CloudHttp();
    private JSONObject data;
    private String error="",syncState="idle";
    private boolean conflict=false;
    private volatile LedgerStore selectedStore;
    private AccountManager(Context c) throws Exception {context=(android.app.Application)c;vault=new SecureVault(c);data=vault.read();activate();}
    private void persist() throws Exception {vault.write(data);}
    private String id() throws Exception {return AuthProtocol.accountId(data.getJSONObject("session").getJSONObject("user").getString("id"));}
    private boolean legacy(){return data.optBoolean("legacyMode")&&!data.has("session");}
    private String scope() throws Exception {return legacy()?"legacy":data.has("session")?id():"locked";}
    private void activate() throws Exception {selectedStore=(legacy()||(data.has("session")&&!prefs().optString("storage").isEmpty()))?LedgerStore.forScope(context,scope()):null;if(!context.getSharedPreferences("active-account",Context.MODE_PRIVATE).edit().putString("scope",scope()).commit())throw new IOException("Cannot open private account storage.");}
    private JSONObject prefs() throws Exception {if(!data.has("accounts"))data.put("accounts",new JSONObject());JSONObject all=data.getJSONObject("accounts");String id=id();if(!all.has(id))all.put(id,new JSONObject());return all.getJSONObject(id);}
    LedgerStore store() throws Exception {LedgerStore current=selectedStore;if(current==null)throw new IOException("Sign in and choose your storage first.");return current;}
    private LedgerStore accountStore() throws Exception {if(!data.has("session"))throw new IOException("Sign in first.");return LedgerStore.forScope(context,id());}
    synchronized JSONObject status() throws Exception {
        JSONObject out=new JSONObject().put("configured",CloudHttp.configured()).put("providers",new JSONArray(Arrays.asList(BuildConfig.AUTH_PROVIDERS.split(",")))).put("canDeleteAccount",BuildConfig.ACCOUNT_DELETION_ENABLED).put("authenticated",data.has("session")).put("legacyMode",legacy()).put("legacyAvailable",!LedgerStore.forScope(context,"legacy").workspace().isNull("workspace")).put("error",error).put("syncState",syncState).put("conflict",conflict);
        if(data.has("session")){JSONObject user=data.getJSONObject("session").getJSONObject("user"),meta=user.optJSONObject("user_metadata");out.put("user",new JSONObject().put("id",id()).put("email",user.optString("email")).put("name",meta==null?"Builder":meta.optString("full_name",meta.optString("name","Builder"))).put("provider",data.optString("provider")));JSONObject pref=prefs();String storage=pref.optString("storage");out.put("workspaceMode",pref.optString("workspaceMode","personal")).put("storage",storage).put("folder",storage.equals("cloud")?id()+"/BuildLedger/":"").put("lastSynced",pref.optString("lastSynced")).put("dirty",storage.equals("cloud")&&accountStore().workspace().getLong("revision")!=pref.optLong("syncedLocalRevision",-1));}
        else if(legacy())out.put("storage","mobile").put("user",new JSONObject().put("name","Existing phone workspace").put("email",""));
        return out;
    }
    synchronized Uri beginSignIn(String provider) throws Exception {
        CloudHttp.requireConfigured();if(!AuthProtocol.PROVIDERS.contains(provider))throw new IOException("Choose Google, Apple or Microsoft.");
        if(!Arrays.asList(BuildConfig.AUTH_PROVIDERS.split(",")).contains(provider))throw new IOException("This login provider is awaiting setup. Please use Google.");
        if(data.has("session"))throw new IOException("Sign out before switching accounts.");
        String verifier=AuthProtocol.random()+AuthProtocol.random(),nonce=AuthProtocol.random();
        data.put("pending",new JSONObject().put("verifier",verifier).put("nonce",nonce).put("started",System.currentTimeMillis()).put("provider",provider));persist();error="";
        String callback=BuildConfig.AUTH_CALLBACK_SCHEME+"://auth/callback/"+nonce;
        return Uri.parse(BuildConfig.SUPABASE_URL+"/auth/v1/authorize").buildUpon().appendQueryParameter("provider",provider).appendQueryParameter("redirect_to",callback).appendQueryParameter("code_challenge",AuthProtocol.challenge(verifier)).appendQueryParameter("code_challenge_method","s256").appendQueryParameter("scopes",provider.equals("azure")?"email openid profile":provider.equals("google")?"openid email profile":"email name").build();
    }
    synchronized void callback(Uri uri) throws Exception {
        JSONObject pending=data.optJSONObject("pending");
        if(pending==null||!AuthProtocol.callbackMatches(uri.getScheme(),uri.getHost(),uri.getPath(),BuildConfig.AUTH_CALLBACK_SCHEME,pending.optString("nonce"),pending.optLong("started"),System.currentTimeMillis()))throw new IOException("This sign-in link expired. Please start again.");
        data.remove("pending");persist();
        if(uri.getQueryParameter("error")!=null)throw new IOException("Sign-in was cancelled or refused. You can try again.");
        String code=uri.getQueryParameter("code");if(code==null||code.length()>5000)throw new IOException("The sign-in response was incomplete. Please start again.");
        JSONObject session=http.json("/auth/v1/token?grant_type=pkce","POST",null,new JSONObject().put("auth_code",code).put("code_verifier",pending.getString("verifier")));
        JSONObject verified=http.json("/auth/v1/user","GET",session.getString("access_token"),null);AuthProtocol.accountId(verified.getString("id"));
        session.put("user",verified).put("expires_at",System.currentTimeMillis()/1000+session.getLong("expires_in"));
        // No Google, Apple or Azure provider access tokens are required for BuildLedger storage.
        session.remove("provider_token");session.remove("provider_refresh_token");
        data.put("session",session).put("provider",pending.getString("provider")).remove("legacyMode");persist();activate();error="";conflict=false;syncState="idle";
    }
    synchronized void fail(Exception e){error=e.getMessage()==null?"The operation could not be completed.":e.getMessage();syncState="error";}
    synchronized void useLegacy() throws Exception {if(LedgerStore.forScope(context,"legacy").workspace().isNull("workspace"))throw new IOException("No earlier phone records were found.");if(data.has("session"))throw new IOException("Sign out to open the earlier phone workspace.");data.put("legacyMode",true);persist();activate();error="";}
    private String token() throws Exception {
        JSONObject session=data.getJSONObject("session");
        if(session.optLong("expires_at")<System.currentTimeMillis()/1000+120){JSONObject fresh=http.json("/auth/v1/token?grant_type=refresh_token","POST",null,new JSONObject().put("refresh_token",session.getString("refresh_token")));JSONObject user=http.json("/auth/v1/user","GET",fresh.getString("access_token"),null);if(!id().equals(AuthProtocol.accountId(user.getString("id"))))throw new IOException("Account verification failed. Sign in again.");fresh.put("user",user).put("expires_at",System.currentTimeMillis()/1000+fresh.getLong("expires_in"));fresh.remove("provider_token");fresh.remove("provider_refresh_token");data.put("session",fresh);persist();session=fresh;}
        return session.getString("access_token");
    }
    synchronized void signOut() throws Exception {
        // Local logout works offline. Cached records remain isolated behind the next sign-in.
        data.remove("session");data.remove("pending");data.remove("legacyMode");persist();activate();error="";conflict=false;syncState="idle";
    }
    synchronized void setWorkspaceMode(String mode) throws Exception {
        if(!data.has("session")||!(mode.equals("personal")||mode.equals("team")))throw new IOException("Sign in before opening a company workspace.");
        prefs().put("workspaceMode",mode);persist();
    }
    synchronized String signedInId() throws Exception {if(!data.has("session"))throw new IOException("Sign in to open your company.");return id();}
    JSONObject teamRpc(JSONObject request) throws Exception {
        final String actor,access;
        synchronized(this){actor=signedInId();access=token();}
        String kind=request.getString("kind");JSONObject args;String route;
        if(kind.equals("read")){route="bl_team_read";args=new JSONObject().put("company_id",request.opt("companyId"));}
        else if(kind.equals("action")){route="bl_team_action";args=new JSONObject().put("operation",request.getString("operation")).put("payload",request.getJSONObject("payload")).put("request_id",request.getString("commandId"));}
        else throw new IOException("Unknown company request.");
        JSONObject result=http.json("/rest/v1/rpc/"+route,"POST",access,args);
        synchronized(this){if(!actor.equals(signedInId()))throw new IOException("The account changed. Reopen your company workspace.");}
        return result;
    }
    synchronized void selectStorage(String mode) throws Exception {
        if(!data.has("session"))throw new IOException("Sign in before choosing storage.");
        JSONObject pref=prefs();if(!pref.optString("storage").isEmpty())throw new IOException("Storage is already selected. Keep this choice or use a backup to move records.");
        if(mode.equals("mobile")){pref.put("storage","mobile");persist();activate();error="";return;}
        if(!mode.equals("cloud"))throw new IOException("Choose phone or cloud storage.");
        String token=token();JSONObject remote=remote(token);LedgerStore ledger=accountStore();
        if(remote!=null&&remote.optString("object_path").length()>0){if(!ledger.workspace().isNull("workspace")&&!(pref.optLong("remoteRevision",-1)==remote.getLong("revision")&&pref.optLong("syncedLocalRevision",-1)==ledger.workspace().getLong("revision")))throw new IOException("This account has phone and cloud records. Export the phone records before restoring the cloud workspace.");download(remote,token);pref.put("remoteRevision",remote.getLong("revision")).put("syncedLocalRevision",ledger.workspace().getLong("revision"));persist();}
        else pref.put("remoteRevision",remote==null?0:remote.getLong("revision"));
        folder(token);pref.put("storage","cloud");persist();activate();
        // On a new cloud account, create the first snapshot before completing onboarding.
        try{sync();}catch(Exception e){pref.remove("storage");persist();activate();throw e;}
    }
    private JSONObject remote(String token) throws Exception {JSONArray rows=http.json("/rest/v1/buildledger_snapshots?select=revision,object_path,sha256,byte_size&user_id=eq."+id(),"GET",token,null).getJSONArray("rows");return rows.length()==0?null:rows.getJSONObject(0);}
    private void folder(String token) throws Exception {File marker=File.createTempFile("folder-",".json",context.getCacheDir());try{try(OutputStream out=new FileOutputStream(marker)){out.write(new JSONObject().put("application","BuildLedger").put("owner",id()).put("formatVersion",1).toString().getBytes(StandardCharsets.UTF_8));}http.upload(id()+"/BuildLedger/.folder.json",token,marker,"application/json");}finally{marker.delete();}}
    private static String digest(File file) throws Exception {MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder out=new StringBuilder();for(byte b:d.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    private void validateRemote(JSONObject remote) throws Exception {String path=remote.getString("object_path");long size=remote.getLong("byte_size");if(!path.matches(id()+"/BuildLedger/snapshots/[a-f0-9-]{36}\\.zip")||!remote.getString("sha256").matches("[a-f0-9]{64}")||size<=0||size>LedgerStore.MAX_TOTAL+6_000_000)throw new IOException("The cloud backup information is invalid.");}
    private void download(JSONObject remote,String token) throws Exception {validateRemote(remote);File zip=File.createTempFile("cloud-download-",".zip",context.getCacheDir());try{http.download(remote.getString("object_path"),token,zip,remote.getLong("byte_size"));if(!digest(zip).equals(remote.getString("sha256")))throw new IOException("Cloud backup integrity check failed.");try(InputStream in=new FileInputStream(zip)){accountStore().restoreBackup(in);}}finally{zip.delete();}}
    synchronized void sync() throws Exception {
        if(!data.has("session")||!prefs().optString("storage").equals("cloud"))return;
        if(conflict)throw new IOException("Cloud records changed on another device. Save a phone backup, then download cloud records.");
        syncState="syncing";String token=token();JSONObject pref=prefs(),remote=remote(token);long expected=pref.optLong("remoteRevision"),actual=remote==null?0:remote.getLong("revision"),local=accountStore().workspace().getLong("revision");
        if(actual!=expected){conflict=true;throw new IOException("Another device changed the cloud records. Your phone changes are safe. Save a phone backup, then download cloud records.");}
        if(local==pref.optLong("syncedLocalRevision",-1)&&actual>0){syncState="synced";error="";return;}
        File zip=File.createTempFile("cloud-upload-",".zip",context.getCacheDir());String path=id()+"/BuildLedger/snapshots/"+UUID.randomUUID()+".zip";
        try{try(OutputStream out=new FileOutputStream(zip)){accountStore().exportBackup(out);}http.upload(path,token,zip,"application/zip");JSONObject result;
            result=http.json("/rest/v1/rpc/bl_commit_snapshot","POST",token,new JSONObject().put("expected_revision",expected).put("new_path",path).put("new_sha256",digest(zip)).put("new_size",zip.length()));
            // A timeout can happen after commit. Keep the uploaded object if the result is uncertain.
            pref.put("remoteRevision",result.getLong("value")).put("syncedLocalRevision",local).put("lastSynced",java.time.Instant.now().toString());persist();syncState="synced";error="";
            if(remote!=null&&!remote.optString("object_path").isEmpty())try{http.remove(remote.getString("object_path"),token);}catch(Exception ignored){}
        }finally{zip.delete();}
    }
    synchronized void replaceFromCloud() throws Exception {if(!prefs().optString("storage").equals("cloud"))throw new IOException("This account uses phone storage.");String token=token();JSONObject remote=remote(token);if(remote==null)throw new IOException("No cloud records were found.");download(remote,token);prefs().put("remoteRevision",remote.getLong("revision")).put("syncedLocalRevision",accountStore().workspace().getLong("revision")).put("lastSynced",java.time.Instant.now().toString());persist();conflict=false;error="";syncState="synced";}
    synchronized void importLegacy() throws Exception {LedgerStore target=store();JSONObject existing=target.workspace().optJSONObject("workspace");if(legacy()||(existing!=null&&(existing.getJSONArray("projects").length()>0||existing.getJSONArray("buyers").length()>0||existing.getJSONArray("payments").length()>0||target.files().getJSONArray("files").length()>0)))throw new IOException("Earlier phone records can only be copied into an empty signed-in workspace.");File zip=File.createTempFile("legacy-copy-",".zip",context.getCacheDir());try{try(OutputStream out=new FileOutputStream(zip)){LedgerStore.forScope(context,"legacy").exportBackup(out);}try(InputStream in=new FileInputStream(zip)){target.restoreBackup(in);}}finally{zip.delete();}}
    synchronized void clearPhoneCopy() throws Exception {LedgerStore ledger=store();boolean cloud=data.has("session")&&prefs().optString("storage").equals("cloud");ledger.wipe();if(cloud){prefs().remove("storage");prefs().remove("syncedLocalRevision");persist();activate();}error="";conflict=false;syncState="idle";}
    synchronized void deleteAccount() throws Exception {if(!BuildConfig.ACCOUNT_DELETION_ENABLED)throw new IOException("Account deletion is not enabled in this private test build.");String token=token(),id=id();http.json("/functions/v1/delete-account","POST",token,new JSONObject());accountStore().wipe();data.getJSONObject("accounts").remove(id);signOut();}
}
