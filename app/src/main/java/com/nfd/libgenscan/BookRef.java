package com.nfd.libgenscan;
import android.app.DownloadManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.select.Elements.*;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;

import java.net.HttpURLConnection;
/**
 * @author Alexander Ronsse-Tucherov
 * @version 2016-08-26.
 *          Wrapper to hold a reference to some book; provided for options for future expansion (e.g. UPC manipulation)
 */
class BookRef {
    private static ArrayList<BookRef> bookList = new ArrayList<>(); //currently dead, but likely will help with history
    private List<BarcodeFormat> allowedFormats = Arrays.asList(BarcodeFormat.ISBN10, BarcodeFormat.ISBN13);

    private String id;
    private BarcodeFormat format;
    private boolean opened;
    private MainActivity parent;


    public BookRef(String id, BarcodeFormat format, boolean opened, MainActivity parent) throws IllegalArgumentException {


        if (!isAllowed(format)) {
            throw new IllegalArgumentException("Format not supported");
        }
        this.id = id;
        this.format = format;
        this.opened = opened;
        this.parent = parent;
    }

    public boolean isAllowed(BarcodeFormat b) {
        return this.allowedFormats.contains(b);
    }

    public String getId() {
        return id;
    }

    public boolean isOpened() {
        return opened;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    static void addToList(BookRef b) {
        bookList.add(b);
    }



//    public String googleBooks() {
//
//
//        return;
//    }




    //TODO: add more libraries, possibly ways of handling other types of barcodes (search by UPC?)
    public void searchBook(){
        opened = true;
        Document doc = null;
        String uri ="";
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            doc = Jsoup.connect("http://libgen.io/search.php?req=" + id +
                    "&lg_topic=libgen&open=0&view=simple&res=25&phrase=1&column=identifier").get();

        Elements el = doc.select("td").select("tbody").select("tr").select("td");
        String test = el.first().select("a").attr("href");
        String md5 = test.substring(test.indexOf("=")+1);
        uri = "http://libgen.io/get.php?md5=" + md5;

        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        parent.fire(browserIntent);

    }


}
