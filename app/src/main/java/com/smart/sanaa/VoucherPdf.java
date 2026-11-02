package com.smart.sanaa;

import android.content.ContentResolver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** Exports a verified batch as a local PDF, without a WebView or network service. */
public final class VoucherPdf {
    private VoucherPdf() {}
    public static void write(ContentResolver resolver, Uri uri, List<String> users, String profile) throws IOException {
        try(OutputStream stream=resolver.openOutputStream(uri)){
            if(stream==null)throw new IOException("Cannot open selected document");
            write(stream,users,profile);
        }
    }
    public static void write(OutputStream destination, List<String> users, String profile) throws IOException {
        if (users == null || users.isEmpty()) throw new IOException("No confirmed vouchers to export");
        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        int row = 0, pageNumber = 0;
        PdfDocument.Page page = null;
        try {
            for (String username : users) {
                if (row == 0) {
                    if (page != null) pdf.finishPage(page);
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create());
                    Canvas canvas = page.getCanvas();
                    paint.setTextSize(19);
                    canvas.drawText("SMART SANAA - HOTSPOT VOUCHERS", 30, 46, paint);
                    paint.setTextSize(12);
                    canvas.drawText("Profile: " + profile, 30, 72, paint);
                }
                paint.setTextSize(13);
                page.getCanvas().drawText("User: " + username + "    Password: " + username, 30, 115 + row * 29, paint);
                row = (row + 1) % 24;
            }
            if (page != null) pdf.finishPage(page);
            pdf.writeTo(destination);
        } finally {
            pdf.close();
        }
    }
}
