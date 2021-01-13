package com.tuya.smart.android.demo.device;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.tuya.smart.android.common.utils.TuyaUtil;
import com.tuya.smart.android.demo.R;
import com.tuya.smart.android.demo.base.activity.BaseActivity;
import com.tuya.smart.android.demo.base.utils.DialogUtil;
import com.tuya.smart.android.demo.device.cloudalbum.IAlbumDeviceApi;
import com.tuya.smart.android.demo.device.common.CommonDeviceDebugPresenter;
import com.tuya.smart.android.device.bean.SchemaBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by letian on 16/8/26.
 */
public class CommonDeviceDebugActivity extends BaseActivity implements ICommonDeviceDebugView {
    private static final String TAG = "CommonDeviceDebugActivity";
    private CommonDebugDeviceAdapter mCommonDebugDeviceAdapter;
    @BindView(R.id.lv_dp_list)
    public ListView mDpListView;
    private CommonDeviceDebugPresenter mPresenter;

    @BindView(R.id.test_log)
    public TextView mLogView;

    @BindView(R.id.test_scroll)
    public ScrollView testScroll;

    @BindView(R.id.v_off_line)
    public View mOffLineView;

    @BindView(R.id.network_tip)
    public TextView mOffLineTip;
    private Unbinder mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_common_debug);
        mBind = ButterKnife.bind(this);
        initToolbar();
        initMenu();
        initPresenter();
        initTitle();
        initAdapter();
        initCustom();
    }

    private void initCustom() {
        if (mPresenter.isCloudAlbumDevice()) {
            ViewStub stub = findViewById(R.id.custom_operator);
            View inflated = stub.inflate();
            IAlbumDeviceApi albumDevice;
            if (inflated instanceof ViewGroup && (albumDevice = mPresenter.getAlbumDeviceApi()) != null) {
                List<View> views = albumDevice.getUploadIndicatorViews(this);
                for (View view : views) {
                    ((ViewGroup) inflated).addView(view);
                }
            }
        }
    }

    private void initPresenter() {
        mPresenter = new CommonDeviceDebugPresenter(this, this);
    }


    private void initMenu() {
        setDisplayHomeAsUpEnabled();
        setMenu(R.menu.toolbar_top_smart_device, new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_test_mode:
                        break;
                    case R.id.action_rename:
                        mPresenter.renameDevice();
                        break;
                    case R.id.action_close:
                        finish();
                        break;
                    case R.id.action_check_update:
                        break;
                    case R.id.action_resume_factory_reset:
                        mPresenter.resetFactory();
                        break;
                    case R.id.action_unconnect:
                        mPresenter.removeDevice();
                        break;
                }
                return false;
            }
        });
    }

    private void initTitle() {
        setTitle(mPresenter.getTitle());

    }

    private void initAdapter() {
        mCommonDebugDeviceAdapter = new CommonDebugDeviceAdapter(this, mPresenter.getDevBean(), R.layout.list_view_common_debug, new ArrayList<SchemaBean>());
        mCommonDebugDeviceAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.onClick(view);
            }
        });
        mCommonDebugDeviceAdapter.setCheckChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mPresenter.onCheckedChanged(compoundButton, b);
            }
        });
        mDpListView.setAdapter(mCommonDebugDeviceAdapter);
        mCommonDebugDeviceAdapter.setData(mPresenter.getSchemaList());

        mOffLineView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
    }

    @Override
    public void updateView(String dpStr) {
        JSONObject jsonObject = JSONObject.parseObject(dpStr);
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            mCommonDebugDeviceAdapter.updateViewData(entry.getKey(), entry.getValue());
        }

        mCommonDebugDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void logError(String error) {
        showMessage(Color.parseColor("#D0021B"), error);
    }

    private void showMessage(final int color, final String log) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SpannableStringBuilder style = new SpannableStringBuilder(log);
                style.setSpan(new ForegroundColorSpan(color), 0, log.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mLogView.append(style);
                mLogView.append("\n");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        View lastChild = testScroll.getChildAt(testScroll.getChildCount() - 1);
                        int bottom = lastChild.getBottom() + testScroll.getPaddingBottom();
                        int sy = testScroll.getScrollY();
                        int sh = testScroll.getHeight();
                        int delta = bottom - (sy + sh);
                        //                        scrollView.smoothScrollTo(0, bottom);

                        testScroll.fullScroll(ScrollView.FOCUS_DOWN);
                        //                        ObjectAnimator.ofInt(testScroll, "scrollY", delta, bottom).setDuration(500).start();

                    }
                }, 100);

            }
        });
    }

    private Handler mHandler = new Handler();

    @Override
    public void logSuccess(final String log) {
        showMessage(Color.parseColor("#5fb336"), log);
    }

    public void scroll2Bottom() {
        // 内层高度超过外层
        int offset = mLogView.getMeasuredHeight()
                - testScroll.getMeasuredHeight();
        if (offset < 0) {
            offset = 0;
        }
        testScroll.scrollTo(0, offset);
    }

    @Override
    public void logSuccess(DpLogBean logBean) {
        String msg = "\n";
        msg += "send success";
        msg += "\n";
        msg += getString(R.string.message_request) + " " + logBean.getDpSend();
        msg += "\n";
        msg += getString(R.string.time_dp_start) + " " + TuyaUtil.formatDate(logBean.getTimeStart(), "yyyy-MM-dd hh:mm:ss");
        msg += "\n";
        msg += getString(R.string.time_dp_end) + " " + TuyaUtil.formatDate(logBean.getTimeEnd(), "yyyy-MM-dd hh:mm:ss");
        msg += "\n";
        msg += getString(R.string.time_dp_spend) + " " + (logBean.getTimeEnd() - logBean.getTimeStart()) + "ms";
        msg += "\n";
        msg += getString(R.string.message_receive) + " " + logBean.getDpReturn();
        msg += "\n";
        logSuccess(msg);
    }

    @Override
    public void logError(DpLogBean logBean) {
        String msg = "\n";
        msg += "send failure";
        msg += "\n";
        msg += getString(R.string.message_request) + " " + logBean.getDpSend();
        msg += "\n";
        msg += getString(R.string.time_dp_start) + " " + TuyaUtil.formatDate(logBean.getTimeStart(), "yyyy-MM-dd hh:mm:ss");
        msg += "\n";
        msg += getString(R.string.time_dp_end) + " " + TuyaUtil.formatDate(logBean.getTimeEnd(), "yyyy-MM-dd hh:mm:ss");
        msg += "\n";
        msg += getString(R.string.time_dp_spend) + " " + (logBean.getTimeEnd() - logBean.getTimeStart()) + "ms";
        msg += "\n";
        if (!TextUtils.isEmpty(logBean.getDpReturn())) {
            msg += getString(R.string.message_receive) + " " + logBean.getDpReturn();
            msg += "\n";
        }
        msg += getString(R.string.failure_reason) + " " + logBean.getErrorMsg();
        msg += "\n";
        logError(msg);
    }


    @OnClick(R.id.tv_clear)
    public void clearMsg() {
        mLogView.setText("");
    }

    @Override
    public void logDpReport(String dpStr) {
        dpStr = getString(R.string.data_report) + "  " + dpStr;
        final String finalDpStr = dpStr;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SpannableStringBuilder style = new SpannableStringBuilder(finalDpStr);
                style.setSpan(new ForegroundColorSpan(Color.parseColor("#303030")), 0, finalDpStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mLogView.append(style);
                mLogView.append("\n");


            }
        });
        scroll2Bottom();
    }

    @Override
    public void deviceRemoved() {
        DialogUtil.simpleSmartDialog(this, R.string.device_has_unbinded, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
    }

    @Override
    public void deviceOnlineStatusChanged(boolean online) {
        if (online) {
            setViewGone(mOffLineView);
        } else {
            setViewVisible(mOffLineView);
            mOffLineTip.setText(getString(R.string.device_offLine));
        }
    }

    @Override
    public void onNetworkStatusChanged(boolean status) {
        if (status) {
            setViewGone(mOffLineView);
        } else {
            setViewVisible(mOffLineView);
            mOffLineTip.setText(getString(R.string.device_network_error));
        }
    }

    @Override
    public void devInfoUpdate() {
        initTitle();
    }

    @Override
    public void updateTitle(String titleName) {
        setTitle(titleName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBind.unbind();
        mPresenter.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IAlbumDeviceApi albumDevice = mPresenter.getAlbumDeviceApi();
        if (albumDevice instanceof ActivityCompat.PermissionCompatDelegate) {
            ((ActivityCompat.PermissionCompatDelegate) albumDevice).onActivityResult(this, requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        IAlbumDeviceApi albumDevice = mPresenter.getAlbumDeviceApi();
        if (albumDevice instanceof ActivityCompat.OnRequestPermissionsResultCallback) {
            ((ActivityCompat.OnRequestPermissionsResultCallback) albumDevice).onRequestPermissionsResult(requestCode
                    , permissions, grantResults);
        }
    }
}
