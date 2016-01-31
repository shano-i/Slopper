package ishida.slopper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import java.util.Arrays;
import android.provider.Settings.System;

public class Face_Detection extends AppCompatActivity {

    private static final String TAG = Face_Detection.class.getName();
    private TextureView mTextureView;
    private Size mPreviewSize;
    private CameraDevice mCamera;
    private CameraCharacteristics mCameraInfo;
    private CaptureRequest.Builder mPreviewBuilder;
    private Rect mCameraViewRect;
    private Context mContext = this;
    private MyCountDownTimer cdt;
    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face__detection);
        mTextureView = (TextureView)findViewById(R.id.textureView);
        mp = MediaPlayer.create(this, R.raw.pinponpanpon);
        cdt = new MyCountDownTimer(10500, 1000);
        cdt.start();
        System.putInt(getContentResolver(), System.SCREEN_OFF_TIMEOUT, 120000);

    }

    @Override
    protected void onResume(){
        super.onResume();
        if(mTextureView.isAvailable()) {
            CameraInit();
        }else{
            mTextureView.setSurfaceTextureListener(mSurfacetextureListener);
        }
    }

    @Override
    protected void onPause(){
        CameraDeinit();
        super.onPause();
    }

    private void CameraInit(){
        CameraManager cameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);

        try{
            for(String cameraId : cameraManager.getCameraIdList()){
                mCameraInfo = cameraManager.getCameraCharacteristics(cameraId);
                int facing = mCameraInfo.get(CameraCharacteristics.LENS_FACING);
                if(facing != CameraCharacteristics.LENS_FACING_FRONT) continue;
                StreamConfigurationMap map = mCameraInfo.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                mCameraViewRect = mCameraInfo.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                Log.i(TAG, "Camera Rect:" + mCameraViewRect.toString());
                cameraManager.openCamera(cameraId, mCameraDeviceStateCallback, null);
            }
        } catch(CameraAccessException e) {
            e.printStackTrace();
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    private void CameraDeinit(){
        if(mCamera != null){
            mCamera.close();
            mCamera = null;
        }
    }

    private void CameraCreatePreviewSession(){
        if(mCamera == null || !mTextureView.isAvailable() || mPreviewSize == null) return;
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if(texture == null) return;
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);
        try {
            mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }catch(CameraAccessException e){
            e.printStackTrace();
            return;
        }
        mPreviewBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE);
        mPreviewBuilder.addTarget(surface);
        try {
            mCamera.createCaptureSession(Arrays.asList(surface), mCameraCaptureSessionStateCallback, null);
        }catch(CameraAccessException e){
            e.printStackTrace();
            return;
        }
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback(){
        @Override
        public void onOpened(CameraDevice camera){
            mCamera = camera;
            CameraCreatePreviewSession();
        }
        @Override
        public void onDisconnected(CameraDevice camera){
            camera.close();
            mCamera = null;
        }
        @Override
        public void onError(CameraDevice camera, int error){
            camera.close();
            mCamera = null;
            Log.e(TAG, "CameraError:" + error);
        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback(){
        @Override
        public void onConfigured(CameraCaptureSession session){
            if(mCamera == null) return;
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            Handler backgroundHandler = new Handler(thread.getLooper());

            try {
                session.setRepeatingRequest(mPreviewBuilder.build(), mCameraCaptureSessionCaptureCallback, backgroundHandler);
            }catch(CameraAccessException e){
                e.printStackTrace();
            }
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session){
            Log.e(TAG, "CameraError:onConfigureFailed");
        }
    };
    private CameraCaptureSession.CaptureCallback mCameraCaptureSessionCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);


            if (faces != null && mode != null && faces.length != 0) {

                if(!mp.isPlaying()) {
                    mp.start();
                    Log.d(TAG, "Play");
                    cdt.cancel();
                    /*Intent intent = new Intent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setClassName("ishida.slopper", "ishida.slopper.MainActivity");
                    startActivity(intent);*/
                    //finish();
                    System.putInt(getContentResolver(), System.SCREEN_OFF_TIMEOUT, 10);
                    moveTaskToBack(true);
                }
                Log.e("tag", "faces : " + faces.length + " , mode : " + mode);
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            process(result);
        }
    };

    private TextureView.SurfaceTextureListener mSurfacetextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            CameraInit();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    public class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            //カウントダウン終了後の処理
            cdt.cancel();
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setClassName("ishida.slopper", "ishida.slopper.MainActivity");
            startActivity(intent);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }
    }
}