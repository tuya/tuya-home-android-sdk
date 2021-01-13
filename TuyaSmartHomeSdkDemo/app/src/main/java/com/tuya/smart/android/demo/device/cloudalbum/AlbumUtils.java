package com.tuya.smart.android.demo.device.cloudalbum;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.tuya.smart.android.common.utils.L;

import java.io.File;

/**
 * ProjectName: TuyaSmartHomeSdkDemo
 * Description:
 * CreateDate: 2021/1/13 11:05 AM
 * <p>
 *
 * @author: <a href="mailto:liming.huang@tuya.com">colin</a>
 * @version: 3.24.5
 * @since: 3.24.5
 */
public class AlbumUtils {
    private static final String TAG = "AlbumUtils";

    private AlbumUtils() {
    }

    public static void selectPictures(Activity activity, int reqCode) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        activity.startActivityForResult(Intent.createChooser(intent, "Select Pictures"), reqCode);
    }


    public static void selectMovie(Activity activity, Uri data, int reqCode) {
        // Intent intent = new Intent(Intent.ACTION_PICK, data);
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(data, "video/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        activity.startActivityForResult(intent, reqCode);
    }

    /**
     * 安卓4.4以后是不能把文件的真实路径直接给别的应用的，所以返回的uri是经过封装的.
     *
     * @param context
     * @param uri
     * @return
     */
    @Nullable
    public static String getPath(final Context context, final Uri uri) {
        final boolean isOverKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isOverKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                // 外部存储空间
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory().getAbsolutePath().concat(File.separator).concat(split[1]);
                } else {
                    return "/storage/".concat(type).concat(File.separator).concat(split[1]);
                }
            }
            // DownloadsProvider, ex: content://com.android.providers.downloads.documents/document/446
            // ex: content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Fu%3D1186508270%2C1917318453%26fm%3D26%26gp%3D0.jpg
            else if (isDownloadsDocument(uri)) {
                // 下载目录
                final String docId = DocumentsContract.getDocumentId(uri);
                if (docId.startsWith("raw:")) {
                    return docId.replaceFirst("raw:", "");
                }
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(docId));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider, ex: content://com.android.providers.media.documents/document/image%3A17924
            else if (isMediaDocument(uri)) {
                // 图片，影音档案
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                String selectionColumn = "_id";
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    selectionColumn = MediaStore.Images.Media._ID;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    selectionColumn = MediaStore.Video.Media._ID;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    selectionColumn = MediaStore.Audio.Media._ID;
                } else {
                    return null;
                }

                final String selection = selectionColumn + "=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general), ex: content://media/external/images/media/309
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(context, uri, null, null);
        }
        // File, ex: file:///storage/emulated/0/DCIM/Camera/20141027_132114.jpg
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @Nullable
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            L.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
