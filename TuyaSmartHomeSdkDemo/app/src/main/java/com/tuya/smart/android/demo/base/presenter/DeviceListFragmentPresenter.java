package com.tuya.smart.android.demo.base.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.tuya.smart.android.base.event.NetWorkStatusEvent;
import com.tuya.smart.android.base.event.NetWorkStatusEventModel;
import com.tuya.smart.android.base.utils.PreferencesUtil;
import com.tuya.smart.android.camera.sdk.TuyaIPCSdk;
import com.tuya.smart.android.camera.sdk.api.ITuyaIPCCore;
import com.tuya.smart.android.common.utils.L;
import com.tuya.smart.android.demo.R;
import com.tuya.smart.android.demo.base.activity.BrowserActivity;
import com.tuya.smart.android.demo.base.app.Constant;
import com.tuya.smart.android.demo.base.fragment.DeviceListFragment;
import com.tuya.smart.android.demo.base.utils.ActivityUtils;
import com.tuya.smart.android.demo.base.utils.DialogUtil;
import com.tuya.smart.android.demo.base.utils.ProgressUtil;
import com.tuya.smart.android.demo.base.utils.ToastUtil;
import com.tuya.smart.android.demo.base.view.IDeviceListFragmentView;
import com.tuya.smart.android.demo.camera.CameraPanelActivity;
import com.tuya.smart.android.demo.config.AddDeviceTypeActivity;
import com.tuya.smart.android.demo.config.CommonConfig;
import com.tuya.smart.android.demo.device.CommonDeviceDebugActivity;
import com.tuya.smart.android.demo.device.SwitchActivity;
import com.tuya.smart.android.demo.device.common.CommonDeviceDebugPresenter;
import com.tuya.smart.android.demo.shortcut.ShortcutDeviceActivity;
import com.tuya.smart.android.mvp.presenter.BasePresenter;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.home.sdk.api.ITuyaHomeStatusListener;
import com.tuya.smart.home.sdk.bean.HomeBean;
import com.tuya.smart.home.sdk.callback.ITuyaGetHomeListCallback;
import com.tuya.smart.home.sdk.callback.ITuyaHomeResultCallback;
import com.tuya.smart.sdk.TuyaSdk;
import com.tuya.smart.sdk.api.IResultCallback;
import com.tuya.smart.sdk.bean.DeviceBean;

import java.util.List;
import java.util.Map;

import static com.tuya.smart.android.demo.camera.utils.Constants.INTENT_P2P_TYPE;

/**
 * Created by letian on 15/6/1.
 */
public class DeviceListFragmentPresenter extends BasePresenter implements NetWorkStatusEvent {

    private static final String TAG = "DeviceListFragmentPresenter";
    private static final int WHAT_JUMP_GROUP_PAGE = 10212;
    protected Activity mActivity;
    protected IDeviceListFragmentView mView;

    public DeviceListFragmentPresenter(DeviceListFragment fragment, IDeviceListFragmentView view) {
        mActivity = fragment.getActivity();
        mView = view;
        TuyaSdk.getEventBus().register(this);
        Constant.HOME_ID = PreferencesUtil.getLong("homeId", Constant.HOME_ID);

    }

    public void getData() {
        mView.loadStart();
        getDataFromServer();
    }

    private void showDevIsNotOnlineTip(final DeviceBean deviceBean) {
        final boolean isShared = deviceBean.isShare;
        DialogUtil.customDialog(mActivity, mActivity.getString(R.string.title_device_offline), mActivity.getString(R.string.content_device_offline),
                mActivity.getString(isShared ? R.string.ty_offline_delete_share : R.string.cancel_connect),
                mActivity.getString(R.string.right_button_device_offline), mActivity.getString(R.string.left_button_device_offline), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                if (isShared) {
//                                    //跳转到删除共享
//                                    Intent intent = new Intent(mActivity, SharedActivity.class);
//                                    intent.putExtra(SharedActivity.CURRENT_TAB, SharedActivity.TAB_RECEIVED);
//                                    mActivity.startActivity(intent);
                                } else {
                                    DialogUtil.simpleConfirmDialog(mActivity, mActivity.getString(R.string.device_confirm_remove), new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                                unBindDevice(deviceBean);
                                            }
                                        }
                                    });
                                }
                                break;
                            case DialogInterface.BUTTON_NEUTRAL:
//                                //重置说明
                                Intent intent = new Intent(mActivity, BrowserActivity.class);
                                intent.putExtra(BrowserActivity.EXTRA_LOGIN, false);
                                intent.putExtra(BrowserActivity.EXTRA_REFRESH, true);
                                intent.putExtra(BrowserActivity.EXTRA_TOOLBAR, true);
                                intent.putExtra(BrowserActivity.EXTRA_TITLE, mActivity.getString(R.string.left_button_device_offline));
                                intent.putExtra(BrowserActivity.EXTRA_URI, CommonConfig.RESET_URL);
                                mActivity.startActivity(intent);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                }).show();

    }

    protected void onItemClick(DeviceBean devBean) {
        if (devBean == null) {
            ToastUtil.showToast(mActivity, R.string.no_device_found);
            return;
        }
        if (devBean.getProductId().equals("4eAeY1i5sUPJ8m8d")) {
            Intent intent = new Intent(mActivity, SwitchActivity.class);
            intent.putExtra(SwitchActivity.INTENT_DEVID, devBean.getDevId());
            mActivity.startActivity(intent);
        } else if (TuyaIPCSdk.getCameraInstance() != null && TuyaIPCSdk.getCameraInstance().isIPCDevice(devBean.devId)) {
            Intent intent = new Intent(mActivity, CameraPanelActivity.class);
            intent.putExtra(CommonDeviceDebugPresenter.INTENT_DEVID, devBean.getDevId());
            Map<String, Object> map = devBean.getSkills();
            int p2pType = -1;
            ITuyaIPCCore cameraInstance = TuyaIPCSdk.getCameraInstance();
            if (cameraInstance != null) {
                p2pType = cameraInstance.getP2PType(devBean.getDevId());
            }
            intent.putExtra(INTENT_P2P_TYPE, p2pType);
            intent.putExtra(CommonDeviceDebugPresenter.INTENT_DEVID, devBean.getDevId());
            mActivity.startActivity(intent);
        } else {
            gotoDeviceCommonActivity(devBean);
        }

    }

    private void gotoDeviceCommonActivity(DeviceBean devBean) {
        Intent intent = new Intent(mActivity, CommonDeviceDebugActivity.class);
        intent.putExtra(CommonDeviceDebugPresenter.INTENT_DEVID, devBean.getDevId());
        mActivity.startActivity(intent);
    }

    public void gotoShortcutActivity() {
        Intent intent = new Intent(mActivity, ShortcutDeviceActivity.class);
        mActivity.startActivity(intent);
    }

    public void getDataFromServer() {
        TuyaHomeSdk.getHomeManagerInstance().queryHomeList(new ITuyaGetHomeListCallback() {
            @Override
            public void onSuccess(List<HomeBean> homeBeans) {
                if (homeBeans.size() == 0) {
                    mView.gotoCreateHome();
                    return;
                }
                final long homeId = homeBeans.get(0).getHomeId();
                Constant.HOME_ID = homeId;
                PreferencesUtil.set("homeId", Constant.HOME_ID);
                TuyaHomeSdk.newHomeInstance(homeId).getHomeDetail(new ITuyaHomeResultCallback() {
                    @Override
                    public void onSuccess(HomeBean bean) {

                        updateDeviceData(bean.getDeviceList());
                    }

                    @Override
                    public void onError(String errorCode, String errorMsg) {

                    }
                });
                TuyaHomeSdk.newHomeInstance(homeId).registerHomeStatusListener(new ITuyaHomeStatusListener() {
                    @Override
                    public void onDeviceAdded(String devId) {

                    }

                    @Override
                    public void onDeviceRemoved(String devId) {

                    }

                    @Override
                    public void onGroupAdded(long groupId) {

                    }

                    @Override
                    public void onGroupRemoved(long groupId) {

                    }

                    @Override
                    public void onMeshAdded(String meshId) {
                        L.d(TAG, "onMeshAdded: " + meshId);
                    }


                });

            }

            @Override
            public void onError(String errorCode, String error) {
                TuyaHomeSdk.newHomeInstance(Constant.HOME_ID).getHomeLocalCache(new ITuyaHomeResultCallback() {
                    @Override
                    public void onSuccess(HomeBean bean) {
                        L.d(TAG, com.alibaba.fastjson.JSONObject.toJSONString(bean));
                        if (mActivity != null) {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDeviceData(bean.getDeviceList());
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String errorCode, String errorMsg) {

                    }
                });
            }
        });
    }

    public void gotoAddDevice() {
        ActivityUtils.gotoActivity(mActivity, AddDeviceTypeActivity.class, ActivityUtils.ANIMATE_SLIDE_TOP_FROM_BOTTOM, false);
    }

    //添加设备
    public void addDevice() {
        final WifiManager mWifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!mWifiManager.isWifiEnabled()) {
            DialogUtil.simpleConfirmDialog(mActivity, mActivity.getString(R.string.open_wifi), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            mWifiManager.setWifiEnabled(true);
                            gotoAddDevice();
                            break;
                    }
                }
            });
        } else {
            gotoAddDevice();
        }
    }


    public void onDeviceClick(DeviceBean deviceBean) {
        if (!deviceBean.getIsOnline()) {
            showDevIsNotOnlineTip(deviceBean);
            return;
        }
        onItemClick(deviceBean);
    }

    public boolean onDeviceLongClick(final DeviceBean deviceBean) {
        if (deviceBean.getIsShare()) {
            return false;
        }
        DialogUtil.simpleConfirmDialog(mActivity, mActivity.getString(R.string.device_confirm_remove), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    unBindDevice(deviceBean);
                }
            }
        });
        return true;
    }

    /**
     * 移除网关
     */
    private void unBindDevice(final DeviceBean deviceBean) {
        ProgressUtil.showLoading(mActivity, R.string.loading);
        TuyaHomeSdk.newDeviceInstance(deviceBean.getDevId()).removeDevice(new IResultCallback() {
            @Override
            public void onError(String s, String s1) {
                ProgressUtil.hideLoading();
                ToastUtil.showToast(mActivity, s1);
            }

            @Override
            public void onSuccess() {
                ProgressUtil.hideLoading();
            }
        });

    }

    private void updateDeviceData(List<DeviceBean> list) {
        if (list.size() == 0) {
            mView.showBackgroundView();
        } else {
            mView.hideBackgroundView();
            mView.updateDeviceData(list);
            mView.loadFinish();
        }
    }

    @Override
    public void onEvent(NetWorkStatusEventModel eventModel) {
        netStatusCheck(eventModel.isAvailable());
    }

    public boolean netStatusCheck(boolean isNetOk) {
        networkTip(isNetOk, R.string.ty_no_net_info);
        return true;
    }

    private void networkTip(boolean networkok, int tipRes) {
        if (networkok) {
            mView.hideNetWorkTipView();
        } else {
            mView.showNetWorkTipView(tipRes);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        TuyaSdk.getEventBus().unregister(this);
    }

}
