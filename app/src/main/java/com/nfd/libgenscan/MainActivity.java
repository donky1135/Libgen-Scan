package com.nfd.libgenscan;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.widget.Toast;

import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;
/* Scanning UI and main menu. This should always be the first thing the user sees on launch.
 * TODO: add history menu, autoscan option, and restore support for pre-Marshmallow if possible
 */
// TODO Need to add settings tab for preferences
//AppCompatActivity was actually causing crashes?
public class MainActivity extends Activity implements ZBarScannerView.ResultHandler {
    private ZBarScannerView mScannerView;
    private boolean autosearch = true; //TODO: add switching (new Activity, along with menu of bookrefs)
    public static final int PERMISSION_REQUEST_CAMERA = 1;
    public static final int PERMISSION_REQUEST_FILEZ = 2;

    //TODO: annotate s.t. < 23 are accepted
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZBarScannerView(this);    // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view

        // Request permission. This does it asynchronously so we have to wait for onRequestPermissionResult before
        // trying to open the camera.
        if (!haveCameraPermission())
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        if (!haveFilePermission())
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_FILEZ);
    }

    private boolean haveCameraPermission() {
        if (Build.VERSION.SDK_INT < 23)
            return true;
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean haveFilePermission(){
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // This is because the dialog was cancelled when we recreated the activity.
        if (permissions.length == 0 || grantResults.length == 0)
            return;

        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mScannerView.startCamera();
                } else {
                    finish();
                }
            }
            break;

//            case PERMISSION_REQUEST_FILEZ:{
//                if (grantResults[1] == PackageManager.PERMISSION_GRANTED){}
//                else
//                    finish();
//
//            }
//            break;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScannerView.stopCamera();
        mScannerView = null;
    }

    //intended to fire URL opener intents from searches
//    void fire(Intent i) {
//        startActivity(i);
//    }
//Currently useless, took out the browser
    @Override
    public void handleResult(Result rawResult) {
        try {
            BookRef b = new BookRef(rawResult.getContents(), rawResult.getBarcodeFormat(), false, this);
            BookRef.addToList(b);  //TODO: have some type of list feature to do mass downloads.  Given current implementation might not be feasible
            //remove; if set to auto-open, immediately call openers before throwing ref out
            // gotta figure out settings activities first
//            if (autosearch) {
//                b.searchBook();
//            }
            new AsyncDl().execute(b); //As said before it was just easiest to pass the object so parsing & downloading got off of the UI thread
            mScannerView.resumeCameraPreview(this);
        } catch (IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), "Barcode format not supported; try another book.",
                    Toast.LENGTH_SHORT).show();
            mScannerView.resumeCameraPreview(this);
        }

    }

    private class AsyncDl extends AsyncTask<BookRef, Void, Void> {
        protected Void doInBackground(BookRef... params){
            try {
                //Sleeping cause don't wanna be a dick to the people running these servers for "free"
                SystemClock.sleep(1000);
                //Found it was easiest to offload the web parsing asynchronously by just getting the BookRef object.
                //Gives me the two strings required, don't know if there is a more efficent way to do this
                String[] i = params[0].searchBook();
                String dlLink = i[0];
                String fileName = i[1];
                //Generates the download, and all enclosed inside of an asyn class, so you can keep on scanning no matter the downloads
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(dlLink))
                        .setTitle(fileName)
                        .setDescription("getting your book")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }
            catch (Exception e){
                //If for some reason the barcode scanner starts putting out shit, it won't crash
                System.out.println(e.toString()); //To be clear I'm not the worlds best programmer
            }
            return null;
        }
        protected void onPostExecute(){}
        protected void onPreExecute(){}
        protected void onProgressUpdate(Void... values) {}
    }
}

