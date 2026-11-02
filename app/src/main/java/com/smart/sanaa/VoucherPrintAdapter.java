package com.smart.sanaa;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;

/** Android system printing of confirmed vouchers. No WebView is used. */
public final class VoucherPrintAdapter extends PrintDocumentAdapter {
    private final List<String> usernames;
    private final String profile;
    public VoucherPrintAdapter(List<String> confirmed,String selectedProfile){
        usernames=new ArrayList<>(confirmed);
        profile=selectedProfile;
    }
    @Override public void onLayout(PrintAttributes oldAttributes,PrintAttributes newAttributes,
                                   CancellationSignal cancellation,LayoutResultCallback callback,Bundle extras){
        if(cancellation.isCanceled()){callback.onLayoutCancelled();return;}
        int pages=(usernames.size()+23)/24;
        PrintDocumentInfo document=new PrintDocumentInfo.Builder("Smart-Sanaa-Vouchers.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages).build();
        callback.onLayoutFinished(document,true);
    }
    @Override public void onWrite(PageRange[] ranges,ParcelFileDescriptor destination,
                                  CancellationSignal cancellation,WriteResultCallback callback){
        if(cancellation.isCanceled()){callback.onWriteCancelled();return;}
        try(FileOutputStream output=new FileOutputStream(destination.getFileDescriptor())){
            VoucherPdf.write(output,usernames,profile);
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        }catch(Exception error){callback.onWriteFailed(error.getMessage());}
    }
}
