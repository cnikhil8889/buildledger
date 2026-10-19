package com.buildledger.app;
import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import org.json.JSONObject;
import java.io.FileNotFoundException;

/** Only explicitly granted, read-only document URIs leave the app sandbox. */
public final class DocumentProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private String id(Uri uri) throws FileNotFoundException {if(uri.getPathSegments().size()!=1)throw new FileNotFoundException();return uri.getLastPathSegment();}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Read-only document");
        try{return ParcelFileDescriptor.open(LedgerStore.get(getContext()).documentFile(id(uri)),ParcelFileDescriptor.MODE_READ_ONLY);}catch(Exception e){throw new FileNotFoundException("Document not found");}
    }
    @Override public String getType(Uri uri){try{return LedgerStore.get(getContext()).document(id(uri)).getString("mime");}catch(Exception e){return "application/octet-stream";}}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        try{JSONObject file=LedgerStore.get(getContext()).document(id(uri));String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor result=new MatrixCursor(columns);Object[] values=new Object[columns.length];for(int i=0;i<columns.length;i++){if(columns[i].equals(OpenableColumns.DISPLAY_NAME))values[i]=file.getString("name");else if(columns[i].equals(OpenableColumns.SIZE))values[i]=file.getLong("size");}result.addRow(values);return result;}catch(Exception e){return null;}
    }
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("Read only");}
    @Override public int update(Uri uri,ContentValues values,String s,String[] a){throw new UnsupportedOperationException("Read only");}
    @Override public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException("Read only");}
}
