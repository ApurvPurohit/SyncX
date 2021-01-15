package com.example.android.syncx;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.SyncX.R;
import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class QRCodeScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView scannerView;
    String resultText = "";
    public String qrTempID;
    public String typeofSensor;
    private ProgressDialog progressBar;
    private int progressBarStatus = 0;
    private Handler progressBarbHandler = new Handler();
    AlertDialog alertDialog = null;

    SharedPreferences sharedpref;// 0 - for private mode
    public SharedPreferences.Editor cache;


    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        scannerView = new ZXingScannerView(this); /* Initialize object */
        setContentView(scannerView); /* Set the ScannerView as a content of current activity */
    }


    @Override
    public void onResume() {
        super.onResume();
        /* Asking user to allow access of camera */
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scannerView.setResultHandler(this); /* Set handler for ZXingScannerView */
            scannerView.startCamera(); /* Start camera */
        } else {
            ActivityCompat.requestPermissions(QRCodeScanActivity.this, new
                    String[]{Manifest.permission.CAMERA}, 1024);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        scannerView.stopCamera(); /* Stop camera */
    }

    @Override
    public void handleResult(Result scanResult) {

        ToneGenerator toneNotification = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100); /* Setting beep sound */

        toneNotification.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        resultText = scanResult.getText(); /* Retrieving text from QR Code */
        JSONObject obj = null;
//        Bundle bundle = new Bundle();
//        bundle.putString("key", resultText);
//        Fragment fobj = new Fragment1();
//        fobj.setArguments(bundle);
//        getSupportFragmentManager().beginTransaction().replace(R.id.linearLayout, fobj).commit();
        finish();
    }
    private void showMessage(String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setCancelable(true);
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        // Must call show() prior to fetching text view
        TextView messageView = (TextView) alertDialog.findViewById(android.R.id.message);
        messageView.setGravity(Gravity.CENTER);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

}

