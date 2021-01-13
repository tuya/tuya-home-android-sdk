package com.tuya.smart.android.demo.device.cloudalbum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.tuya.smart.android.common.utils.L;
import com.tuya.smart.android.demo.R;
import com.tuya.smart.android.demo.base.utils.CommonUtil;
import com.tuya.smart.android.demo.base.utils.DialogUtil;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuya.smart.uploader.api.IOssImageUploadApi;
import com.tuya.smart.uploader.api.IOssUploadPlugin;
import com.tuya.smart.uploader.api.IOssVideoUploadApi;
import com.tuya.smart.uploader.api.IUploadProcessCallback;
import com.tuya.smart.uploader.api.IVideoUploadProcessCallback;
import com.tuya.smart.uploader.api.bean.img.ImgBean;
import com.tuya.smart.uploader.api.bean.img.ImgsUploadCompleteBean;
import com.tuya.smart.uploader.api.bean.img.ImgsUploadProgressBean;
import com.tuya.smart.uploader.api.bean.video.VideoProcessTaskBean;
import com.tuya.smart.uploader.api.bean.video.VideoProgressBean;
import com.tuya.smart.uploader.api.bean.video.VideoUploadBean;
import com.tuya.smart.uploader.impl.OssUploadPlugin;
import com.tuya.smart.uploader.impl.utils.EventHelper;
import com.tuya.smart.utils.DensityUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * ProjectName: TuyaSmartHomeSdkDemo
 * Description:
 * CreateDate: 2021/1/11 8:31 PM
 * <p>
 *
 * @author: <a href="mailto:liming.huang@tuya.com">colin</a>
 * @version: 3.24.5
 * @since: 3.24.5
 */
public class CloudAlbumDeviceInteraction implements IAlbumDeviceApi, ActivityCompat.PermissionCompatDelegate,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CloudAlbumDeviceInteraction";
    private static final int REQUEST_READ_STORAGE = 15001;
    private static final int REQUEST_SELECT_MULTI_IMAGE = 16001;
    private static final int REQUEST_SELECT_VIDEO = 16002;

    private Activity mActivity;
    private DeviceBean mDeviceBean;
    private SelectType mType;
    @Nullable
    private IOssImageUploadApi mImageUploadApi;
    @Nullable
    private IOssVideoUploadApi mVideoUploadApi;

    public CloudAlbumDeviceInteraction(Activity activity, DeviceBean deviceBean) {
        mActivity = activity;
        mDeviceBean = deviceBean;
        IOssUploadPlugin plugin = new OssUploadPlugin(activity);
        if (plugin != null) {
            mImageUploadApi = plugin.imageUploader();
            mVideoUploadApi = plugin.videoUploader();
        }
    }

    @Override
    public List<View> getUploadIndicatorViews(Context context) {
        List<View> list = new ArrayList<>();
        int mg = DensityUtil.dip2px(context, 4);

        Button picture = new Button(context);
        picture.setText("Select Picture");
        picture.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_btn_code));
        LinearLayout.LayoutParams picLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        picLayoutParams.setMargins(mg, mg, mg, mg);
        picture.setLayoutParams(picLayoutParams);
        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mType = SelectType.IMAGE;
                gotoAlbumToSelect(mActivity);
            }
        });
        list.add(picture);

        Button video = new Button(context);
        video.setText("Select Movie");
        video.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_btn_code));
        LinearLayout.LayoutParams videoLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        videoLayoutParams.setMargins(mg, mg, mg, mg);
        video.setLayoutParams(videoLayoutParams);
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mType = SelectType.VIDEO;
                gotoAlbumToSelect(mActivity);
            }
        });

        list.add(video);
        return list;
    }

    @Override
    public void gotoAlbumToSelect(Activity activity) {
        requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_READ_STORAGE);
    }

    @Override
    public void onDestroy() {
        if (mImageUploadApi != null) {
            mImageUploadApi.onDestroy();
        }
        if (mVideoUploadApi != null) {
            mVideoUploadApi.onDestroy();
        }
    }

    // private void generalOpen(Activity activity) {
    //     Uri uri = mType.externalContentUri;
    //     Intent intent = new Intent(Intent.ACTION_PICK, uri);
    //     activity.startActivityForResult(intent, REQUEST_SELECT_MULTI_IMAGE);
    // }

    private void openAlbum(Activity activity) {
        if (activity == null) return;

        if (SelectType.IMAGE == mType) {
            AlbumUtils.selectPictures(activity, REQUEST_SELECT_MULTI_IMAGE);
        } else {
            AlbumUtils.selectMovie(activity, SelectType.VIDEO.externalContentUri, REQUEST_SELECT_VIDEO);
        }
    }

    @Override
    public boolean requestPermissions(@NonNull Activity activity, @NonNull String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_STORAGE && permissions != null && grantResults != null) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum(mActivity);
            } else {
                showPermissionDialog();
            }
        }
    }

    @Override
    public boolean onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
        L.e(TAG, "onActivityResult, reqCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == REQUEST_SELECT_MULTI_IMAGE) {
            if (resultCode == Activity.RESULT_OK && null != data) {
                List<ImgBean> fileList = new ArrayList<>();
                ClipData imageNames = data.getClipData();
                // 多选
                if (imageNames != null) {
                    for (int i = 0; i < imageNames.getItemCount(); i++) {
                        Uri imageUri = imageNames.getItemAt(i).getUri();
                        L.d(TAG, "original image uri: " + (imageUri != null ? imageUri.toString() : "null"));
                        if (imageUri == null) continue;

                        String path = AlbumUtils.getPath(activity, imageUri);
                        if (path == null) {
                            return false;
                        }

                        Uri newUri = Uri.parse(path);
                        if (newUri != null) {
                            activity.grantUriPermission(activity.getPackageName(), newUri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            L.i(TAG, String.format("Transfer Image[%d] uri: %s", i, newUri.toString()));
                            ImgBean bean = new ImgBean();
                            bean.setImageUri(newUri.toString());
                            bean.setTitle("No." + i + "-" + System.currentTimeMillis());
                            fileList.add(bean);
                        }
                    }
                }
                // 单选
                else {
                    Uri imageUri = data.getData();
                    L.d(TAG, "original image uri: " + (imageUri != null ? imageUri.toString() : "null"));
                    if (imageUri == null) return false;

                    String path = AlbumUtils.getPath(activity, imageUri);
                    if (path == null) {
                        return false;
                    }

                    Uri newUri = Uri.parse(path);
                    if (newUri != null) {
                        activity.grantUriPermission(activity.getPackageName(), newUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        L.i(TAG, String.format("Transfer Image uri: %s", newUri.toString()));
                        ImgBean bean = new ImgBean();
                        bean.setImageUri(newUri.toString());
                        bean.setTitle("Image-1-" + System.currentTimeMillis());
                        fileList.add(bean);
                    }
                }

                if (mImageUploadApi != null) {
                    mImageUploadApi.batchUploadToDevice(mDeviceBean.getDevId(), fileList, new IUploadProcessCallback<ImgsUploadProgressBean, ImgsUploadCompleteBean>() {
                        @Override
                        public void onProcessStart(String s) {
                            L.d(TAG, "onProcessStart, taskId: " + s);
                        }

                        @Override
                        public void onProgress(ImgsUploadProgressBean imgsUploadProgressBean) {
                            L.d(TAG,
                                    "onProgress, current: " + imgsUploadProgressBean.getCurrentImage() + ", fileId: " + imgsUploadProgressBean.getEncryptFileId());
                            // mock cancelBatchUpload when upload images more than 2
                            // if (!imgsUploadProgressBean.getComplete() && imgsUploadProgressBean.getUploaded() >= 2) {
                            //     L.e(TAG, "Image cancelBatchUpload");
                            //     mImageUploadApi.cancelBatchUpload(imgsUploadProgressBean.getBatchTaskId());
                            // }
                        }

                        @Override
                        public void onComplete(ImgsUploadCompleteBean imgsUploadCompleteBean) {
                            L.i(TAG,
                                    "onComplete, success: " + imgsUploadCompleteBean.getUploaded() + ", failed: " + imgsUploadCompleteBean.getFailed());
                        }
                    });
                }
            }
        } else if (requestCode == REQUEST_SELECT_VIDEO) {
            if (resultCode == Activity.RESULT_OK && null != data) {
                Uri videoUri = data.getData();
                L.d(TAG, "original video uri: " + (videoUri != null ? videoUri.toString() : "null"));
                if (videoUri == null) return false;

                Cursor cursor = null;
                String videoPath = null;
                int size = 0;
                int duration = 0;
                String title = "";
                try {
                    String[] filePathColumn = {SelectType.VIDEO.data, MediaStore.Video.Media.SIZE,
                            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.TITLE};
                    cursor = activity.getContentResolver().query(videoUri,
                            filePathColumn, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        // videoPath
                        int columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0]);
                        videoPath = cursor.getString(columnIndex);
                        L.d(TAG, "videoPath: " + videoPath);
                        // size
                        columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[1]);
                        size = cursor.getInt(columnIndex);
                        L.d(TAG, "videoSize: " + size);
                        // duration
                        columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[2]);
                        duration = cursor.getInt(columnIndex);
                        L.d(TAG, "videoDuration: " + duration);
                        // title
                        columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[3]);
                        title = cursor.getString(columnIndex);
                        L.d(TAG, "videoTitle: " + title);
                    }
                } catch (Exception e) {
                    L.e(TAG, e.getMessage(), e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                String path = AlbumUtils.getPath(activity, videoUri);
                if (path == null) {
                    return false;
                }

                Uri newUri = Uri.parse(path);
                if (newUri != null) {
                    activity.grantUriPermission(activity.getPackageName(), newUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    L.i(TAG, String.format("Transfer Video uri: %s", newUri.toString()));
                    VideoUploadBean bean = new VideoUploadBean();
                    bean.setVideoUri(newUri.toString());
                    bean.setName(title + "-" + System.currentTimeMillis());
                    bean.setDuration(duration);
                    bean.setSize(size);

                    if (mVideoUploadApi != null) {
                        mVideoUploadApi.uploadVideoToDevice(mDeviceBean.getDevId(), bean, new IVideoUploadProcessCallback<VideoProgressBean, Object>() {
                            @Override
                            public void onProgress(VideoProgressBean videoProgressBean) {
                                L.d(TAG, "onProgress, code: " + videoProgressBean.getCode() + ", extInfo: " +
                                        (videoProgressBean.getExtInfo() != null ? videoProgressBean.getExtInfo().toString() : "null"));
                                // mock cancelBatchTask when progress over 60%
                                // if (videoProgressBean.getCode() == EventHelper.CODE_PROGRESS &&
                                //         videoProgressBean.getExtInfo().containsKey("progress")) {
                                //     int progress = (Integer) videoProgressBean.getExtInfo().get("progress");
                                //     if (progress >= 60) {
                                //         L.e(TAG, "Video cancelBatchTask");
                                //         mVideoUploadApi.cancelBatchTask(videoProgressBean.taskId);
                                //     }
                                // }
                                if (videoProgressBean.getCode() == EventHelper.CODE_CANCEL) {
                                    L.i(TAG, "cancel success.");
                                }
                                if (videoProgressBean.getCode() == EventHelper.CODE_FAILED) {
                                    L.e(TAG, "video upload failed.");
                                }
                                if (videoProgressBean.getCode() == EventHelper.CODE_SUCCESS) {
                                    L.i(TAG, "then will call onComplete.");
                                }
                            }

                            @Override
                            public void onProcessStart(String s) {
                                L.d(TAG, "onProcessStart, taskId: " + s);
                            }

                            @Override
                            public void onComplete(Object o) {
                                L.i(TAG, "onComplete, result: " + (o != null ? o.toString() : "null"));
                            }

                            @Override
                            public void onProcessStart(VideoProcessTaskBean videoProcessTaskBean) {
                                L.d(TAG, "onProcessStart, taskId: " + videoProcessTaskBean.taskId);
                            }
                        });
                    }
                }
            }
        }
        return false;
    }

    @SuppressLint("StringFormatInvalid")
    private void showPermissionDialog() {
        if (mActivity == null) return;

        String tip = String.format(mActivity.getString(R.string.ty_set_read_external_permission),
                CommonUtil.getApplicationName(mActivity, mActivity.getPackageName()));
        DialogUtil.simpleSmartDialog(mActivity, tip, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 1) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
                    intent.setData(uri);
                    mActivity.startActivity(intent);
                }
            }
        });
    }

    public enum SelectType {
        VIDEO(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA),
        IMAGE(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA);

        Uri externalContentUri;
        String data;

        SelectType(Uri externalContentUri,
                   String data) {
            this.externalContentUri = externalContentUri;
            this.data = data;
        }
    }

}
