package shano.slopper;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Handheld extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {
    private static final String TAG = R.class.getName();
    private TextView data;
    private GoogleApiClient client;

    public static String getNowDate(){
        final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handheld);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        data = (TextView) findViewById(R.id.textView1);
        this.client = new GoogleApiClient.Builder(this).addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult){
                        Log.d(TAG, "onConnectionFailed:" + connectionResult.toString());
                    }
                })
                .build();
        this.client.connect();


    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        Wearable.MessageApi.addListener(client, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if(messageEvent.getPath().equals("/path")) {
            final String message = new String(messageEvent.getData());

            Log.d(TAG, "Message path received is :" + messageEvent.getPath());
            Log.d(TAG, "Message received is :" + message);
            data.setText(message);

            try{
                String path = Environment.getDataDirectory().getPath();
                FileOutputStream outputStream = openFileOutput(getNowDate() + ".csv", MODE_APPEND);
                PrintWriter writer = new PrintWriter(outputStream);
                writer.println(message);
                Log.d(TAG, "csv is created where:" + path + "/" + getNowDate() + ".csv");
                //writer.flush();
                writer.close();
                outputStream.close();

            }catch (FileNotFoundException e){
                e.printStackTrace();
                Log.i(TAG,"file not exist");
            }catch(IOException e){
                e.printStackTrace();
                Log.i(TAG, "csv file can't create");
            }
        }
    }
}


