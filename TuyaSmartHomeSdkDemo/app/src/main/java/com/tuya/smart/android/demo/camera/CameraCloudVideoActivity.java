package com.tuya.smart.android.demo.camera;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.tuya.smart.android.camera.sdk.TuyaIPCSdk;
import com.tuya.smart.android.camera.sdk.api.ITuyaIPCCore;
import com.tuya.smart.android.demo.R;
import com.tuya.smart.android.demo.base.utils.MessageUtil;
import com.tuya.smart.camera.camerasdk.typlayer.callback.AbsP2pCameraListener;
import com.tuya.smart.camera.camerasdk.typlayer.callback.IRegistorIOTCListener;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationCallBack;
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationDelegateCallBack;
import com.tuya.smart.camera.ipccamerasdk.msgvideo.ITYCloudVideo;
import com.tuya.smart.camera.ipccamerasdk.msgvideo.TYCloudVideoPlayer;
import com.tuya.smart.camera.middleware.widget.TuyaCameraView;

public class CameraCloudVideoActivity extends AppCompatActivity {

    private final int OPERATE_SUCCESS = 1;
    private final int OPERATE_FAIL = 0;
    private final int MSG_CLOUD_VIDEO_DEVICE = 1000;

    private ProgressBar mProgressBar;
    private TuyaCameraView mCameraView;

    private ITYCloudVideo mcloudCamera;
    private String playUrl;
    private String encryptKey;
    private int playDuration;
    private String cachePath;
    private String mDevId;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CLOUD_VIDEO_DEVICE:
                    startplay();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void startplay() {
        mcloudCamera.playVideo(playUrl, 0, encryptKey, new OperationCallBack() {
            @Override
            public void onSuccess(int i, int i1, String s, Object o) {
                Log.d("mcloudCamera", "onsuccess");
            }

            @Override
            public void onFailure(int i, int i1, int i2, Object o) {

            }
        }, new OperationCallBack() {
            @Override
            public void onSuccess(int i, int i1, String s, Object o) {
                Log.d("mcloudCamera", "finish onsuccess");
            }

            @Override
            public void onFailure(int i, int i1, int i2, Object o) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_cloud_video);
        initData();
        initview();
        initCloudCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mcloudCamera != null) {
            mcloudCamera.removeOnDelegateP2PCameraListener();
        }
    }

    private void initData() {
        playUrl = getIntent().getStringExtra("playUrl");
        encryptKey = getIntent().getStringExtra("encryptKey");
        playDuration = getIntent().getIntExtra("playDuration", 0);
        mDevId = getIntent().getStringExtra("devId");
        cachePath = getApplication().getCacheDir().getPath();

    }

    private void initCloudCamera() {
        mcloudCamera = new TYCloudVideoPlayer();
        mcloudCamera.registerP2PCameraListener(new AbsP2pCameraListener() {
            @Override
            public void receiveFrameDataForMediaCodec(int i, byte[] bytes, int i1, int i2, byte[] bytes1, boolean b, int i3) {
                super.receiveFrameDataForMediaCodec(i, bytes, i1, i2, bytes1, b, i3);
            }
        });
        mcloudCamera.generateCloudCameraView((IRegistorIOTCListener) mCameraView.createdView());
        mcloudCamera.createCloudDevice(cachePath, mDevId, new OperationDelegateCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data) {
                mHandler.sendMessage(MessageUtil.getMessage(MSG_CLOUD_VIDEO_DEVICE, OPERATE_SUCCESS));
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode) {

            }
        });

    }

    private void initview() {
        mProgressBar = findViewById(R.id.camera_cloud_video_progressbar);
        mCameraView = findViewById(R.id.camera_cloud_video_view);
        int p2PType = -1;
        ITuyaIPCCore cameraInstance = TuyaIPCSdk.getCameraInstance();
        if (cameraInstance != null) {
            p2PType = cameraInstance.getP2PType(mDevId);
        }
        mCameraView.createVideoView(p2PType);
    }

}
