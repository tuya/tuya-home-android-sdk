package com.tuya.smart.android.demo.device.cloudalbum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.List;

/**
 * ProjectName: TuyaSmartHomeSdkDemo
 * Description:
 * CreateDate: 2021/1/11 8:12 PM
 * <p>
 *
 * @author: <a href="mailto:liming.huang@tuya.com">colin</a>
 * @version: 3.24.5
 * @since: 3.24.5
 */
public interface IAlbumDeviceApi {
    List<View> getUploadIndicatorViews(Context context);

    void gotoAlbumToSelect(Activity activity);

    void onDestroy();
}
