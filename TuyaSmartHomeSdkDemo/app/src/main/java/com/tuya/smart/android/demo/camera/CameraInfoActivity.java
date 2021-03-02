package com.tuya.smart.android.demo.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.tuya.smart.android.camera.sdk.TuyaIPCSdk;
import com.tuya.smart.android.camera.sdk.api.ICameraConfigInfo;
import com.tuya.smart.android.camera.sdk.api.ITuyaIPCCore;
import com.tuya.smart.android.demo.R;
import com.tuya.smart.android.demo.camera.adapter.CameraInfoAdapter;
import com.tuya.smart.android.demo.camera.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class CameraInfoActivity extends AppCompatActivity {

    private String mDevId;
    private List<String> mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_info);
        mDevId = getIntent().getStringExtra(Constants.INTENT_DEV_ID);
        initData();
        initView();
    }

    private void initData() {
        ITuyaIPCCore cameraInstance = TuyaIPCSdk.getCameraInstance();
        if (cameraInstance != null) {
            mData = new ArrayList<>();
            mData.add("is Low Power: " + cameraInstance.isLowPowerDevice(mDevId));
            ICameraConfigInfo cameraConfig = cameraInstance.getCameraConfig(mDevId);
            if (cameraConfig != null) {
                mData.add("video num: " + cameraConfig.getVideoNum());
                mData.add("default definition num: " + cameraConfig.getDefaultDefinition());
                mData.add("is support speaker: " + cameraConfig.isSupportSpeaker());
                mData.add("is support pick up: " + cameraConfig.isSupportPickup());
                mData.add("is support talk back mode up: " + cameraConfig.isSupportChangeTalkBackMode());
                mData.add("default talk back mode: " + cameraConfig.getDefaultTalkBackMode());
                mData.add("support playback speed: " + list2String(cameraConfig.getSupportPlaySpeedList()));
                mData.add("raw data json: " + cameraConfig.getRawDataJsonStr());
            }
        }
    }

    private String list2String(List<Integer> list) {
        if (list != null && !list.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < list.size();i ++) {
                stringBuilder.append(list.get(i).toString());
                if (i < list.size() - 1) {
                    stringBuilder.append(", ");
                }
            }
            return stringBuilder.toString();
        } else {
            return "";
        }
    }

    private void initView() {
        if (mData != null) {
            RecyclerView ry = findViewById(R.id.camera_info_ry);
            ry.setLayoutManager(new LinearLayoutManager(this));
            CameraInfoAdapter cameraInfoAdapter = new CameraInfoAdapter(mData);
            ry.setAdapter(cameraInfoAdapter);
            cameraInfoAdapter.notifyDataSetChanged();
        }
    }
}