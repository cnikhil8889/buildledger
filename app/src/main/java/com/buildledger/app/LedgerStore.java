package com.buildledger.app;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.SecretKey;
import java.security.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.*;

/** App-private, encrypted metadata with explicitly selected file import/export. */
public final class LedgerStore {
    public static final long MAX_FILE=20L*1024*1024, MAX_TOTAL=250L*1024*1024;
    private static final String KEY="BuildLedgerLocalKeyV1";
    private static final Map<String,LedgerStore> instances=new HashMap<>();
    private final String keyAlias;
    private final android.app.Application context;
    private final AtomicFile state;
    private final File documents;
    public static final List<String> FOLDERS=Arrays.asList("Project details","Legal & approvals","30-year title history","Engineer & architect drawings","Buyer agreements & sale","Budget & estimates","Payment receipts","Project reports");
    public static synchronized LedgerStore get(Context c) {return forScope(c,c.getSharedPreferences("active-account",Context.MODE_PRIVATE).getString("scope","locked"));}
    public static synchronized LedgerStore forScope(Context c,String scope){if(!scope.equals("legacy")&&!scope.equals("locked")&&!scope.matches("[a-f0-9-]{36}"))throw new SecurityException("Invalid account scope");if(!instances.containsKey(scope))instances.put(scope,new LedgerStore(c.getApplicationContext(),scope));return instances.get(scope);}
    private LedgerStore(Context c,String scope){context=(android.app.Application)c;File base=scope.equals("legacy")?c.getFilesDir():new File(c.getFilesDir(),"accounts/"+scope);if(!base.isDirectory()&&!base.mkdirs())throw new IllegalStateException("Cannot create private account storage");keyAlias=scope.equals("legacy")?KEY:KEY+"_"+scope;state=new AtomicFile(new File(base,"ledger.enc"));documents=new File(base,"documents");if(!documents.isDirectory()&&!documents.mkdirs())throw new IllegalStateException("Cannot create private document storage");}
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(keyAlias))return (SecretKey)ks.getKey(keyAlias,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(keyAlias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }
    private JSONObject empty() throws JSONException {return new JSONObject().put("formatVersion",1).put("workspace",JSONObject.NULL).put("revision",0).put("files",new JSONArray());}
    private JSONObject read() throws Exception {
        if(!state.getBaseFile().exists()&&!new File(state.getBaseFile()+".bak").exists())return empty();
        byte[] bytes;try(InputStream in=state.openRead()){bytes=readBounded(in,4*1024*1024);}
        if(bytes.length<30||bytes[0]!='B'||bytes[1]!='L'||bytes[2]!=1)throw new IOException("Local data format could not be read. Restore a backup; existing data has not been overwritten.");
        byte[] iv=Arrays.copyOfRange(bytes,3,15);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
        return new JSONObject(new String(cipher.doFinal(Arrays.copyOfRange(bytes,15,bytes.length)),StandardCharsets.UTF_8));
    }
    private void write(JSONObject data) throws Exception {
        byte[] plain=data.toString().getBytes(StandardCharsets.UTF_8);if(plain.length>3_000_000)throw new IOException("Workspace metadata exceeds the 3 MB limit. Export your records before continuing.");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        FileOutputStream out=null;try{out=state.startWrite();out.write(new byte[]{'B','L',1});out.write(cipher.getIV());out.write(cipher.doFinal(plain));state.finishWrite(out);}catch(Exception e){if(out!=null)state.failWrite(out);throw e;}
    }
    public synchronized JSONObject workspace() throws Exception {JSONObject root=read();return new JSONObject().put("workspace",root.opt("workspace")).put("revision",root.getLong("revision"));}
    public synchronized JSONObject files() throws Exception {return new JSONObject().put("files",read().getJSONArray("files"));}
    public synchronized JSONObject save(String json,long expected) throws Exception {
        if(json.length()>3_000_000)throw new IOException("Workspace is too large.");JSONObject workspace=new JSONObject(json);validateWorkspace(workspace);JSONObject root=read();
        if(expected!=root.getLong("revision"))throw new IOException("Your workspace changed. Refresh before saving again.");
        Set<String> projects=ids(workspace.getJSONArray("projects"));JSONArray fs=root.getJSONArray("files");
        for(int i=0;i<fs.length();i++)if(!projects.contains(fs.getJSONObject(i).getString("projectId")))throw new IOException("Move or remove project documents before removing a project.");
        root.put("workspace",workspace).put("revision",expected+1);write(root);return workspace();
    }
    public static void validateWorkspace(JSONObject w) throws Exception {
        text(w,"builderName",2,100);text(w,"city",0,100);if(!"cloud".equals(w.getString("storage")))throw new IOException("Unsupported workspace format.");
        JSONArray ps=w.getJSONArray("projects"),bs=w.getJSONArray("buyers"),ts=w.getJSONArray("payments");
        if(ps.length()>200||bs.length()>3000||ts.length()>10000)throw new IOException("Workspace record limit exceeded.");
        Set<String> projectIds=ids(ps),buyerIds=ids(bs);ids(ts);Map<String,String> buyerProject=new HashMap<>();
        for(int i=0;i<ps.length();i++){JSONObject p=ps.getJSONObject(i);text(p,"name",2,100);text(p,"location",0,120);text(p,"state",0,80);text(p,"notes",0,2000);money(p,"budget");int year=p.getInt("year"),progress=p.getInt("progress");if(p.getDouble("year")!=year||p.getDouble("progress")!=progress||year<1990||year>2100||progress<0||progress>100)throw new IOException("Invalid project year or progress.");choice(p,"type",Arrays.asList("Residential","Commercial","Villa","Plotted development"));choice(p,"status",Arrays.asList("Planning","In progress","Completed","On hold"));date(p,"createdAt",false);JSONArray rs=p.getJSONArray("requirements");if(rs.length()>60)throw new IOException("Too many checklist items.");ids(rs);for(int r=0;r<rs.length();r++){text(rs.getJSONObject(r),"label",1,200);choice(rs.getJSONObject(r),"status",Arrays.asList("Pending","Uploaded","Not applicable"));}}
        for(int i=0;i<bs.length();i++){JSONObject b=bs.getJSONObject(i);text(b,"name",2,100);text(b,"unit",1,60);text(b,"phone",0,25);text(b,"notes",0,1000);money(b,"saleValue");date(b,"date",false);String p=b.getString("projectId");if(!projectIds.contains(p))throw new IOException("Buyer project is missing.");buyerProject.put(b.getString("id"),p);}
        for(int i=0;i<ts.length();i++){JSONObject t=ts.getJSONObject(i);text(t,"party",2,100);text(t,"category",1,80);text(t,"reference",0,100);text(t,"notes",0,1000);choice(t,"method",Arrays.asList("Bank transfer","UPI","Cheque","Cash"));date(t,"date",false);date(t,"dueDate",true);String p=t.getString("projectId"),direction=t.getString("direction");if(!projectIds.contains(p))throw new IOException("Payment project is missing.");if(money(t,"paid").compareTo(money(t,"amount"))>0)throw new IOException("Paid amount exceeds bill total.");if(!Arrays.asList("expense","receipt").contains(direction))throw new IOException("Invalid payment direction.");if(direction.equals("receipt")&&(!buyerIds.contains(t.getString("buyerId"))||!p.equals(buyerProject.get(t.getString("buyerId")))))throw new IOException("Collection buyer does not belong to this project.");}
    }
    private static void choice(JSONObject o,String k,List<String> allowed) throws Exception {if(!allowed.contains(o.getString(k)))throw new IOException("Invalid "+k+".");}
    private static void text(JSONObject o,String k,int min,int max) throws Exception {String s=o.getString(k);if(s.trim().length()<min||s.length()>max)throw new IOException("Invalid "+k+".");}
    private static BigDecimal money(JSONObject o,String key) throws Exception {BigDecimal n=new BigDecimal(o.get(key).toString());if(n.signum()<0||n.compareTo(new BigDecimal("1000000000000"))>0||n.stripTrailingZeros().scale()>2)throw new IOException("Invalid money value.");return n;}
    private static void date(JSONObject o,String key,boolean optional) throws Exception {String s=o.getString(key);if(optional&&s.isEmpty())return;if(!s.matches("\\d{4}-\\d{2}-\\d{2}")||!LocalDate.parse(s).toString().equals(s))throw new IOException("Invalid date.");}
    private static Set<String> ids(JSONArray rows) throws Exception {Set<String> result=new HashSet<>();for(int i=0;i<rows.length();i++){String id=rows.getJSONObject(i).getString("id");if(!id.matches("[A-Za-z0-9_-]{1,100}")||!result.add(id))throw new IOException("Invalid or duplicate record ID.");}return result;}
    private static byte[] readBounded(InputStream in,long max) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];long count=0;int n;while((n=in.read(buffer))!=-1){count+=n;if(count>max)throw new IOException("File exceeds the size limit.");out.write(buffer,0,n);}return out.toByteArray();}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
    private boolean projectExists(JSONObject root,String project) throws Exception {return !root.isNull("workspace")&&ids(root.getJSONObject("workspace").getJSONArray("projects")).contains(project);}
    public synchronized JSONObject importDocument(Uri uri,JSONObject request) throws Exception {
        JSONObject root=read();String project=request.getString("projectId"),folder=request.getString("folder"),type=request.getString("documentType");
        if(!projectExists(root,project)||!FOLDERS.contains(folder)||type.length()>100)throw new IOException("Choose a valid project and folder.");
        JSONArray rows=root.getJSONArray("files");if(rows.length()>=5000)throw new IOException("Document limit reached.");long total=0;for(int i=0;i<rows.length();i++)total+=rows.getJSONObject(i).getLong("size");
        String id=UUID.randomUUID().toString(),name="Document";try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
        name=name.replaceAll("[\\p{Cntrl}/\\\\]","_");if(name.length()>200)name=name.substring(0,200);String mime=context.getContentResolver().getType(uri);if(mime==null||!mime.matches("[A-Za-z0-9.+_-]+/[A-Za-z0-9.+_-]+"))mime="application/octet-stream";
        File file=new File(documents,id);MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;
        try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)){if(in==null)throw new IOException("Cannot read selected file.");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){size+=n;if(size>MAX_FILE||size+total>MAX_TOTAL)throw new IOException("Limit: 20 MB per file and 250 MB in this mobile version.");out.write(buffer,0,n);digest.update(buffer,0,n);}}catch(Exception e){file.delete();throw e;}
        if(size==0){file.delete();throw new IOException("The selected file is empty.");}
        JSONObject item=new JSONObject().put("id",id).put("projectId",project).put("folder",folder).put("documentType",type).put("name",name).put("mime",mime).put("size",size).put("sha256",hex(digest.digest())).put("createdAt",java.time.Instant.now().toString());
        rows.put(item);root.put("files",rows);try{write(root);}catch(Exception e){file.delete();throw e;}return item;
    }
    public synchronized JSONObject document(String id) throws Exception {if(!id.matches("[A-Fa-f0-9-]{36}"))throw new FileNotFoundException();JSONArray rows=read().getJSONArray("files");for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getString("id").equals(id))return rows.getJSONObject(i);throw new FileNotFoundException("Document not found.");}
    public synchronized File documentFile(String id) throws Exception {document(id);File f=new File(documents,id);if(!f.isFile())throw new FileNotFoundException();return f;}
    public synchronized void exportBackup(OutputStream output) throws Exception {
        JSONObject root=read();JSONArray rows=root.getJSONArray("files");try(ZipOutputStream zip=new ZipOutputStream(output)){zip.putNextEntry(new ZipEntry("state.json"));zip.write(root.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();byte[] buffer=new byte[8192];for(int i=0;i<rows.length();i++){String id=rows.getJSONObject(i).getString("id");zip.putNextEntry(new ZipEntry("documents/"+id));try(InputStream in=new FileInputStream(documentFile(id))){int n;while((n=in.read(buffer))!=-1)zip.write(buffer,0,n);}zip.closeEntry();}}
    }
    public synchronized void restoreBackup(InputStream input) throws Exception {
        File staging=new File(context.getCacheDir(),"restore-"+UUID.randomUUID());if(!staging.mkdirs())throw new IOException("Cannot prepare backup.");JSONObject restored=null;Set<String> names=new HashSet<>();List<File> added=new ArrayList<>();
        try{
            long total=0;try(ZipInputStream zip=new ZipInputStream(input)){ZipEntry entry;int entries=0;byte[] buffer=new byte[8192];while((entry=zip.getNextEntry())!=null){if(++entries>5001||entry.isDirectory()||!names.add(entry.getName()))throw new IOException("Invalid backup entries.");String name=entry.getName();if(name.equals("state.json")){byte[] bytes=readBounded(zip,3_000_000);total+=bytes.length;restored=new JSONObject(new String(bytes,StandardCharsets.UTF_8));}else if(name.matches("documents/[A-Fa-f0-9-]{36}")){File target=new File(staging,name.substring(10));long size=0;try(OutputStream out=new FileOutputStream(target)){int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>MAX_FILE||total>MAX_TOTAL+3_000_000)throw new IOException("Backup size limit exceeded.");out.write(buffer,0,n);}}}else throw new IOException("Unsupported backup entry.");zip.closeEntry();}}
            if(restored==null||restored.getInt("formatVersion")!=1)throw new IOException("Unsupported backup format.");
            if(!restored.isNull("workspace"))validateWorkspace(restored.getJSONObject("workspace"));JSONArray rows=restored.getJSONArray("files");if(names.size()!=rows.length()+1)throw new IOException("Backup document list does not match its files.");Set<String> projects=restored.isNull("workspace")?Collections.emptySet():ids(restored.getJSONObject("workspace").getJSONArray("projects"));Set<String> oldIds=new HashSet<>();
            for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String oldId=row.getString("id");if(!oldId.matches("[A-Fa-f0-9-]{36}")||!oldIds.add(oldId)||!names.contains("documents/"+oldId)||!projects.contains(row.getString("projectId"))||!FOLDERS.contains(row.getString("folder")))throw new IOException("Invalid backup document.");text(row,"name",1,200);text(row,"documentType",1,100);String mime=row.getString("mime");if(!mime.matches("[A-Za-z0-9.+_-]+/[A-Za-z0-9.+_-]+"))throw new IOException("Invalid file type.");File src=new File(staging,oldId);if(src.length()!=row.getLong("size"))throw new IOException("Backup file size mismatch.");MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(src)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}if(!hex(digest.digest()).equals(row.getString("sha256")))throw new IOException("Backup file integrity check failed.");String newId=UUID.randomUUID().toString();File dest=new File(documents,newId);try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}added.add(dest);row.put("id",newId);}
            JSONArray previous=read().getJSONArray("files");restored.put("revision",read().getLong("revision")+1);write(restored);added.clear();for(int i=0;i<previous.length();i++)new File(documents,previous.getJSONObject(i).getString("id")).delete();
        }finally{for(File f:added)f.delete();File[] temp=staging.listFiles();if(temp!=null)for(File f:temp)f.delete();staging.delete();}
    }
    public synchronized void wipe() throws Exception {state.delete();File[] all=documents.listFiles();if(all!=null)for(File f:all)if(!f.delete())throw new IOException("Some files could not be removed.");KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);ks.deleteEntry(keyAlias);}
}
