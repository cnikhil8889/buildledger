package com.buildledger.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.print.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Offline application shell. Only packaged, trusted assets receive the native bridge. */
public final class MainActivity extends Activity {
    private static final String ORIGIN="https://appassets.androidplatform.net";
    private static final int AUTH=100,IMPORT=101,CSV=102,BACKUP=103,RESTORE=104;
    private static final Set<String> ASSETS=new HashSet<>(Arrays.asList("index.html","app.js","app.css","favicon.svg","privacy.html","brand-logo.png","brand-wordmark.png"));
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final ExecutorService teamIO=Executors.newSingleThreadExecutor();
    private WebView web;
    private android.widget.FrameLayout root;
    private volatile boolean unlocked=false,trustedPage=false;
    private boolean authenticating=false,pickerOpen=false,backRegistered=false;
    private long pausedAt=0;
    private JSONObject pendingDocument;
    private String pendingCsv;
    private AccountManager accounts;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable cloudTask=()->io.execute(()->{try{accounts.sync();}catch(Exception e){accounts.fail(e);}event("buildledger-account-changed");event("buildledger-document-changed");});
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);try{accounts=AccountManager.get(this);}catch(Exception e){new AlertDialog.Builder(this).setTitle("Account settings unavailable").setMessage("Your records have been kept. Close and reopen the app, or restore a saved backup if needed.").setPositiveButton("Close",(d,w)->finish()).show();return;}
        root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(247,244,238));setContentView(root);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets system=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime()|WindowInsets.Type.displayCutout());v.setPadding(system.left,system.top,system.right,system.bottom);return WindowInsets.CONSUMED;}else {v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();}});
        if(saved!=null){String p=saved.getString("pendingDocument");if(p!=null)try{pendingDocument=new JSONObject(p);}catch(JSONException ignored){}pickerOpen=saved.getBoolean("pickerOpen",false);}
        lockScreen();authenticate();
    }
    private void lockScreen(){TextView label=new TextView(this);label.setGravity(Gravity.CENTER);label.setText("BuildLedger\n\nUnlock to open your builder workspace");label.setTextColor(Color.rgb(27,44,67));label.setTextSize(20);label.setPadding(28,28,28,28);label.setBackgroundColor(Color.rgb(247,244,238));label.setTag("lock");root.addView(label,new FrameLayout.LayoutParams(-1,-1));}
    private void authenticate(){
        if(authenticating)return;KeyguardManager keyguard=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(keyguard!=null&&keyguard.isDeviceSecure()){
            Intent intent=keyguard.createConfirmDeviceCredentialIntent("BuildLedger","Unlock your private project records");
            if(intent!=null){authenticating=true;startActivityForResult(intent,AUTH);return;}
        }
        unlock();
    }
    private void unlock(){unlocked=true;authenticating=false;pausedAt=0;View lock=root.findViewWithTag("lock");if(lock!=null)root.removeView(lock);if(web==null)createWeb();else event("buildledger-unlocked");handleAuthIntent();scheduleCloud();}
    @SuppressWarnings("SetJavaScriptEnabled") private void createWeb(){
        web=new WebView(this);web.setBackgroundColor(Color.rgb(247,244,238));WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setBlockNetworkLoads(true);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setSupportMultipleWindows(false);settings.setSafeBrowsingEnabled(true);web.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);web.addJavascriptInterface(new NativeBridge(),"BuildLedgerNative");
        // Honor the packaged device-width viewport. Keep pinch zoom and the
        // user's Android font scale available without desktop overview scaling.
        settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(false);
        settings.setTextZoom(Math.round(100*getResources().getConfiguration().fontScale));
        settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){trustedPage=isLocal(Uri.parse(url));}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){Uri uri=req.getUrl();if(isLocal(uri))return false;if(req.hasGesture()&&"https".equals(uri.getScheme())&&"www.indiacode.nic.in".equals(uri.getHost()))openExternal(uri);return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){
                Uri uri=req.getUrl();if(!isLocal(uri))return denied();String asset=uri.getPath().substring("/assets/".length());
                try{String mime=asset.endsWith(".js")?"application/javascript":asset.endsWith(".css")?"text/css":asset.endsWith(".svg")?"image/svg+xml":asset.endsWith(".png")?"image/png":"text/html";return new WebResourceResponse(mime,"UTF-8",200,"OK",assetHeaders(),getAssets().open(asset));}catch(IOException e){return denied();}
            }
            @Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler handler,android.net.http.SslError error){handler.cancel();}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){trustedPage=false;root.removeView(web);web.destroy();web=null;createWeb();return true;}
        });
        web.setWebChromeClient(new WebChromeClient());root.addView(web,0,new FrameLayout.LayoutParams(-1,-1));trustedPage=true;web.loadUrl(ORIGIN+"/assets/index.html");
        if(Build.VERSION.SDK_INT>=33&&!backRegistered){getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::back);backRegistered=true;}
    }
    private boolean isLocal(Uri uri){return "https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost())&&uri.getPort()==-1&&uri.getPath()!=null&&uri.getPath().startsWith("/assets/")&&ASSETS.contains(uri.getPath().substring("/assets/".length()));}
    private Map<String,String> assetHeaders(){Map<String,String> headers=new HashMap<>();headers.put("Cache-Control","no-store");headers.put("X-Content-Type-Options","nosniff");headers.put("Content-Security-Policy","default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'none'; frame-src 'none'; base-uri 'none'; form-action 'none'");return headers;}
    private WebResourceResponse denied(){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Collections.emptyMap(),new ByteArrayInputStream("Blocked".getBytes(StandardCharsets.UTF_8)));}
    private void requireUnlocked(){if(!unlocked||!trustedPage)throw new SecurityException("Unlock BuildLedger to continue.");}
    private void scheduleCloud(){handler.removeCallbacks(cloudTask);handler.postDelayed(cloudTask,1200);}
    private void accountAction(ThrowingAction action){io.execute(()->{try{action.run();}catch(Exception e){accounts.fail(e);}event("buildledger-account-changed");event("buildledger-document-changed");});}
    private interface ThrowingAction{void run() throws Exception;}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(unlocked)handleAuthIntent();}
    private void handleAuthIntent(){Uri uri=getIntent().getData();if(uri!=null&&BuildConfig.AUTH_CALLBACK_SCHEME.equals(uri.getScheme())&&"auth".equals(uri.getHost())){getIntent().setData(null);accountAction(()->{accounts.callback(uri);scheduleCloud();});}}
    private String failed(Exception e){try{return new JSONObject().put("error",message(e)).toString();}catch(Exception ignored){return "{\"error\":\"Operation failed\"}";}}
    private String message(Exception e){String text=e.getMessage();return text==null||text.length()>220?"The operation could not be completed. Your existing records were kept.":text;}
    private void event(String name){runOnUiThread(()->{if(web!=null&&unlocked)web.evaluateJavascript("window.dispatchEvent(new Event("+JSONObject.quote(name)+"))",null);});}
    private void toast(String text){runOnUiThread(()->Toast.makeText(this,text,Toast.LENGTH_LONG).show());}
    private void openExternal(Uri uri){try{startActivity(new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE));}catch(ActivityNotFoundException e){toast("Install a browser to open the online workspace.");}}
    private void createDocument(int code,String mime,String title){if(pickerOpen){toast("Finish the current file selection first.");return;}Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE,title);try{pickerOpen=true;startActivityForResult(intent,code);}catch(ActivityNotFoundException e){pickerOpen=false;toast("No file provider is available on this phone.");}}
    private void teamReply(String requestId,String actor,JSONObject result){runOnUiThread(()->{try{if(isDestroyed()||isFinishing()||web==null||!unlocked||!trustedPage||!actor.equals(accounts.signedInId()))return;JSONObject detail=new JSONObject().put("id",requestId).put("result",result);web.evaluateJavascript("window.dispatchEvent(new CustomEvent('buildledger-team-response',{detail:JSON.parse("+JSONObject.quote(detail.toString())+")}))",null);}catch(Exception ignored){}});}
    public final class NativeBridge {
        @JavascriptInterface public void setWorkspaceMode(String mode){try{requireUnlocked();accountAction(()->accounts.setWorkspaceMode(mode));}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void teamRequest(String requestId,String raw){try{requireUnlocked();if(requestId==null||!requestId.matches("[a-f0-9-]{36}")||raw==null||raw.length()>22000)throw new IOException("Invalid company request.");final String actor=accounts.signedInId();final JSONObject request=new JSONObject(raw);teamIO.execute(()->{JSONObject result;try{result=accounts.teamRpc(request);}catch(Exception e){try{result=new JSONObject().put("error",message(e));}catch(Exception ignored){return;}}teamReply(requestId,actor,result);});}catch(Exception e){try{teamReply(requestId,accounts.signedInId(),new JSONObject().put("error",message(e)));}catch(Exception ignored){}}}
        @JavascriptInterface public void copyInvitation(String value){try{requireUnlocked();if(value==null||value.length()>500)throw new IOException("Invalid invitation.");runOnUiThread(()->{ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);ClipData clip=ClipData.newPlainText("BuildLedger invitation",value);if(Build.VERSION.SDK_INT>=33){PersistableBundle extras=new PersistableBundle();extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE,true);clip.getDescription().setExtras(extras);}clipboard.setPrimaryClip(clip);toast("Invitation code copied.");});}catch(Exception e){toast(message(e));}}

        @JavascriptInterface public String readAccount(){try{requireUnlocked();return accounts.status().toString();}catch(Exception e){return failed(e);}}
        @JavascriptInterface public void signIn(String provider){try{requireUnlocked();Uri url=accounts.beginSignIn(provider);runOnUiThread(()->openExternal(url));}catch(Exception e){accounts.fail(e);event("buildledger-account-changed");}}
        @JavascriptInterface public void chooseStorage(String mode){try{requireUnlocked();accountAction(()->accounts.selectStorage(mode));}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void useExistingPhone(){try{requireUnlocked();accountAction(()->accounts.useLegacy());}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void syncCloud(){try{requireUnlocked();accountAction(()->accounts.sync());}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void signOut(){try{requireUnlocked();runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Sign out of BuildLedger?").setMessage("Saved phone records stay private to this account. Cloud changes still waiting to upload remain on this phone until you sign in again.").setNegativeButton("Cancel",null).setPositiveButton("Sign out",(d,w)->accountAction(()->accounts.signOut())).show());}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void downloadCloud(){try{requireUnlocked();runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Replace phone copy with cloud records?").setMessage("Export a phone backup first if you need to keep pending phone changes. This replaces this account's phone copy with its latest cloud snapshot.").setNegativeButton("Cancel",null).setPositiveButton("Download cloud records",(d,w)->accountAction(()->accounts.replaceFromCloud())).show());}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void copyExistingPhone(){try{requireUnlocked();runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Copy your earlier phone records?").setMessage("The earlier builder workspace will be copied into this signed-in account if it is empty. The original phone records are kept.").setNegativeButton("Cancel",null).setPositiveButton("Copy records",(d,w)->accountAction(()->{accounts.importLegacy();scheduleCloud();})).show());}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void deleteAccount(){try{requireUnlocked();runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Permanently delete this account?").setMessage("This removes the signed-in account, its BuildLedger cloud folder and its records on this phone. Other devices and backups you exported must be deleted separately. This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete account",(d,w)->accountAction(()->accounts.deleteAccount())).show());}catch(Exception e){toast(message(e));}}

        @JavascriptInterface public String readWorkspace(){try{requireUnlocked();return accounts.store().workspace().toString();}catch(Exception e){return failed(e);}}
        @JavascriptInterface public String readFiles(){try{requireUnlocked();return accounts.store().files().toString();}catch(Exception e){return failed(e);}}
        @JavascriptInterface public String saveWorkspace(String data,long revision){try{requireUnlocked();String result=accounts.store().save(data,revision).toString();scheduleCloud();return result;}catch(Exception e){return failed(e);}}
        @JavascriptInterface public void pickDocument(String request){try{requireUnlocked();JSONObject data=new JSONObject(request);runOnUiThread(()->{if(pickerOpen){toast("Finish the current file selection first.");return;}pendingDocument=data;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");try{pickerOpen=true;startActivityForResult(intent,IMPORT);}catch(ActivityNotFoundException e){pickerOpen=false;toast("No document picker is available.");}});}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void openDocument(String id){try{requireUnlocked();JSONObject item=accounts.store().document(id);Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".documents").appendPath(id).build();runOnUiThread(()->{try{Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,item.optString("mime","application/octet-stream")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("BuildLedger document",uri));startActivity(Intent.createChooser(intent,"Open document with"));}catch(ActivityNotFoundException e){toast("No app can open this file type. It remains saved in BuildLedger.");}});}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void exportCsv(String csv){try{requireUnlocked();if(csv.getBytes(StandardCharsets.UTF_8).length>3_000_000)throw new IOException("Report is too large.");runOnUiThread(()->{if(pickerOpen){toast("Finish the current file selection first.");return;}pendingCsv=csv;createDocument(CSV,"text/csv","BuildLedger-report-"+java.time.LocalDate.now()+".csv");});}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void printReport(){try{requireUnlocked();runOnUiThread(()->{PrintManager manager=(PrintManager)getSystemService(PRINT_SERVICE);if(manager!=null&&web!=null)manager.print("BuildLedger report",web.createPrintDocumentAdapter("BuildLedger project report"),new PrintAttributes.Builder().build());});}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void saveBackup(){try{requireUnlocked();runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Save a complete backup").setMessage("This ZIP includes your project records, financial details, buyer information and documents. It is not encrypted. Choose a private location you trust, such as your Google Drive folder. Android will show the available providers.").setNegativeButton("Cancel",null).setPositiveButton("Choose location",(d,w)->createDocument(BACKUP,"application/zip","BuildLedger-backup-"+java.time.LocalDate.now()+".zip")).show());}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void restoreBackup(){try{requireUnlocked();runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Replace phone records from a backup?").setMessage("A valid BuildLedger backup will replace this phone's current workspace and documents. Save a backup of your current records first if you need to keep them. If this account uses cloud storage, restored records will sync to its cloud folder.").setNegativeButton("Cancel",null).setPositiveButton("Select backup",(d,w)->{if(pickerOpen)return;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip");try{pickerOpen=true;startActivityForResult(intent,RESTORE);}catch(ActivityNotFoundException e){pickerOpen=false;toast("No document picker is available.");}}).show());}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void openCloudWorkspace(){try{requireUnlocked();if(BuildConfig.CLOUD_WORKSPACE_URL.isEmpty()){toast("No separate web workspace is configured.");return;}runOnUiThread(()->openExternal(Uri.parse(BuildConfig.CLOUD_WORKSPACE_URL)));}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void showPrivacy(){try{requireUnlocked();runOnUiThread(()->{web.loadUrl(ORIGIN+"/assets/privacy.html");});}catch(Exception e){toast(message(e));}}
        @JavascriptInterface public void clearLocalData(){try{requireUnlocked();runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Delete all phone records?").setMessage("This permanently removes the builder profile, projects, payments, buyers and imported documents stored in this app on this phone. Exported backups and cloud records are kept. Cloud accounts will ask you to choose storage again so the phone copy can be downloaded safely.").setNegativeButton("Cancel",null).setPositiveButton("Delete phone records",(d,w)->io.execute(()->{try{accounts.clearPhoneCopy();event("buildledger-account-changed");runOnUiThread(()->{web.loadUrl(ORIGIN+"/assets/index.html");toast("Phone records deleted.");});}catch(Exception e){toast(message(e));}})).show());}catch(Exception e){toast(message(e));}}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==AUTH){authenticating=false;if(result==RESULT_OK)unlock();else finish();return;}
        pickerOpen=false;if(result!=RESULT_OK||data==null||data.getData()==null){if(request==CSV)pendingCsv=null;if(request==IMPORT)pendingDocument=null;return;}
        Uri uri=data.getData();JSONObject documentRequest=pendingDocument;String csv=pendingCsv;pendingDocument=null;pendingCsv=null;
        io.execute(()->{try{
            if(request==IMPORT){if(documentRequest==null)throw new IOException("The document selection expired. Choose the file again.");accounts.store().importDocument(uri,documentRequest);scheduleCloud();toast("Document imported into the project folder.");event("buildledger-document-changed");}
            else if(request==CSV){if(csv==null)throw new IOException("The report selection expired. Export the report again.");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Cannot write report.");out.write(csv.getBytes(StandardCharsets.UTF_8));}toast("Report saved.");}
            else if(request==BACKUP){try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Cannot write backup.");accounts.store().exportBackup(out);}toast("Full backup saved.");}
            else if(request==RESTORE){try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Cannot read backup.");accounts.store().restoreBackup(in);}scheduleCloud();toast("Backup restored.");runOnUiThread(()->web.loadUrl(ORIGIN+"/assets/index.html"));}
        }catch(Exception e){toast(message(e));}});
    }
    @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(pendingDocument!=null)out.putString("pendingDocument",pendingDocument.toString());out.putBoolean("pickerOpen",pickerOpen);}
    @Override protected void onPause(){super.onPause();pausedAt=SystemClock.elapsedRealtime();}
    @Override protected void onResume(){super.onResume();if(web!=null&&!authenticating&&!pickerOpen&&pausedAt>0&&SystemClock.elapsedRealtime()-pausedAt>60_000){unlocked=false;lockScreen();authenticate();}else if(web!=null&&unlocked)event("buildledger-team-refresh");}
    private void back(){if(!unlocked){finish();return;}if(web!=null&&web.getUrl()!=null&&web.getUrl().endsWith("privacy.html")){web.loadUrl(ORIGIN+"/assets/index.html");return;}if(web!=null){web.evaluateJavascript("Boolean(window.BuildLedgerBack && window.BuildLedgerBack())",result->{if(!"true".equals(result))confirmExit();});}else confirmExit();}
    private void confirmExit(){new AlertDialog.Builder(this).setTitle("Close BuildLedger?").setMessage("Your saved phone records will be here when you return.").setNegativeButton("Stay",null).setPositiveButton("Close",(d,w)->finish()).show();}

    // Android 13+ uses the platform dispatcher above; this override serves API 26–32 only.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){back();}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("BuildLedgerNative");web.destroy();}handler.removeCallbacks(cloudTask);io.shutdown();teamIO.shutdownNow();super.onDestroy();}
}
