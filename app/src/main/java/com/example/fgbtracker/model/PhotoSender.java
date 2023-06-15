package com.example.fgbtracker.model;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import android.os.Message;

import androidx.annotation.NonNull;

import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager.FileListState;
import dji.sdk.media.MediaManager;
import com.example.fgbtracker.LiveSteamVideo;
import com.example.fgbtracker.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoSender implements MediaManager.FileListStateListener {
    private static final String TAG = PhotoSender.class.getName();
    private final MediaManager mediaManager;
    private final Camera mCamera;
    private int photoCounter;
    private List<MediaFile> fileList = new ArrayList<>();;
    File destDir;

    public PhotoSender(String pref_photo_host, String pref_photo_port, Camera camera) {
        Log.d(TAG, "PhotoSender");
        photoCounter = 0;
        mCamera = camera;
        mediaManager = mCamera.getMediaManager();
        destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/MediaManagerDemo/");
        mediaManager.addUpdateFileListStateListener(this);
        mCamera.setMediaFileCallback(new MediaFile.Callback() {
            @Override
            public void onNewFile(@NonNull MediaFile mediaFile) {
                Log.d(TAG,"New photo generated: " + mediaFile.getFileName());
                // Set camera to download mode
                downloadFile(mediaFile);
                /*
                mCamera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error == null) {
                            Log.d(TAG, "Set cameraMode success");
                            downloadFile(mediaFile);
                        } else {
                            Log.e(TAG, "Set cameraMode failed: " + error.getDescription());
                        }
                    }
                });*/
            }
        });
    }

    private void downloadFile(MediaFile mediaFile) {
        mediaFile.fetchFileData(destDir, null, new DownloadListener<String>() {
            @Override
            public void onFailure(DJIError error) {
                Log.e(TAG, "Download File Failed" + error.getDescription());
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {
                int tmpProgress = (int) (1.0 * current / total * 100);
                Log.d(TAG,"download progress: "+tmpProgress);
            }

            @Override
            public void onRealtimeDataUpdate(byte[] bytes, long l, boolean b) {
                ;
            }

            @Override
            public void onStart() {
                ;
            }

            @Override
            public void onSuccess(String filePath) {
                Log.d(TAG,"Download File Success" + ":" + filePath);
                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null)
                            Log.d(TAG, "setupCamera(photo): "+djiError.getDescription());
                        else
                            photoCounter += 1;
                    }
                });
            }
        });
    }

    @Override
    public void onFileListStateChange(FileListState fileListState) {
        Log.d(TAG, "onFileListStateChange: "+fileListState.toString());
        if(fileListState.compareTo(FileListState.INCOMPLETE) == 0) {
            fileList = mediaManager.getSDCardFileListSnapshot();
            for (int i = 0; i < fileList.size(); i++) {
                Log.d(TAG, "["+i+"]"+fileList.get(i).getFileName());
            }
        }
    }

    public void takePicture() {
        mCamera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (null == djiError) {
                    Log.d(TAG, "takePicture: " + photoCounter);
                } else {
                    Log.d(TAG,"takePicture failed: " + djiError.getDescription());
                    mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    //mCamera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null)
                                Log.d(TAG, "setupCamera(photo): "+djiError.getDescription());
                            else
                                photoCounter += 1;
                        }
                    });

                }
            }
        });
    }
}
