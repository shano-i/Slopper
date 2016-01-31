package ishida.slopper;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Random;

import static java.lang.Math.abs;

public class HrSActivity extends WearableActivity{
    private final String TAG = HrSActivity.class.getName();
    private TextView mTextHeart;
    private SensorManager mSensorManager;
    private Vibrator vib;
    private GoogleApiClient client;
    private Sensor heart;
    private String mNode;
    private int count;
    private double a = 5.0;
    private double tmp = 60.0;
    private Button send;

    final SensorEventListener mHeartListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Log.d(TAG, "Heart Rate : " + event.values[0]);
            if(mTextHeart != null){
                mTextHeart.setText(String.valueOf(event.values[0]));
                if(event.values[0] != 0 && event.values[0] <= 80 && (tmp-event.values[0]) >= a){
                    Log.i(TAG,"Sleep Status Changed : "+ (event.values[0] - tmp));
                    Wearable.MessageApi.sendMessage(client, mNode, "Timer Start!", null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public  void onResult(MessageApi.SendMessageResult result){
                            if(!result.getStatus().isSuccess()){
                                Log.d(TAG,"ERROR failed to send message" + result.getStatus());
                            }
                        }
                    });
                    vib.vibrate(500);
                } else if(event.values[0] != 0 && tmp != event.values[0] && (tmp-event.values[0]) <= a){
                    Log.i(TAG, "Sleep Status not Changed : " + (event.values[0] - tmp));
                    //vib.vibrate(200);//デバッグ用につき，後ほどコメントアウト
                }
            }
            tmp = event.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "onAccuracyChanged :" + accuracy);
        }
    };

    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_hr_s);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mTextHeart = (TextView)findViewById(R.id.textView);
        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        send = (Button) findViewById(R.id.sendbutton);
        client = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Wearable.NodeApi.getConnectedNodes(client).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                            @Override
                            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                                if(nodes.getNodes().size() > 0){
                                    mNode = nodes.getNodes().get(0).getId();
                                }
                            }
                        });
                        Log.d("MyFragment", "onConnected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d("MyFragment", "onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d("MyFragment", "onConnectionFailed");
                    }
                })
                .build();
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Wearable.MessageApi.sendMessage(client, mNode, "Timer Start!", null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public  void onResult(MessageApi.SendMessageResult result){
                        if(!result.getStatus().isSuccess()){
                            Log.d(TAG,"ERROR failed to send message" + result.getStatus());
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        heart = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(mHeartListener, heart, mSensorManager.SENSOR_DELAY_FASTEST);
        client.connect();
    }

    @Override
    protected void onPause(){
        super.onPause();;
        mSensorManager.unregisterListener(mHeartListener);
        client.disconnect();
    }
}