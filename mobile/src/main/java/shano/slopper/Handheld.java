package shano.slopper;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;



public class Handheld extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {
    private static final String TAG = R.class.getName();
    private TextView data;
    private GoogleApiClient client;



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

        if(messageEvent.getPath().equals("/path")){
            final String message = new String(messageEvent.getData());

            Log.d(TAG, "Message path received is :" + messageEvent.getPath());
            Log.d(TAG, "Message received is :" + message);
            data.setText(message);
        }
    }

}


