package com.example.fgbtracker;

import android.util.Log;

import com.example.fgbtracker.ui.home.HomeFragment;
import com.example.fgbtracker.ui.video.VideoFeedView;

import dji.sdk.camera.VideoFeeder;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.sdkmanager.LiveVideoResolution;

public class LiveSteamVideo {
    public static final String TAG = LiveSteamVideo.class.getName();
    private int lastBitRate;
    private String mliveShowUrl;
    private LiveStreamManager mStreamMananger;
    private VideoFeedView primaryVideoFeed;

    public LiveSteamVideo(String prefVideoIp, String port, VideoFeedView videoView) {
        mliveShowUrl= "rtmp://"+prefVideoIp+":"+port+"/show/h264";
        primaryVideoFeed = videoView;
        setup();
    }

    void setup()
    {
        ;
    }

    public int startLiveShow() {
        Log.d(TAG, "Start Live Show on URL:"+mliveShowUrl);
        if (!isLiveStreamManagerOn()) {
            return -1;
        }
        if (mStreamMananger.isStreaming()) {
            Log.d(TAG, "already started!");
            return -2;
        }
        mStreamMananger.setAudioStreamingEnabled(false);
        mStreamMananger.setVideoEncodingEnabled(true);
        mStreamMananger.setVideoSource(LiveStreamManager.LiveStreamVideoSource.Primary);
        if(primaryVideoFeed != null)
            primaryVideoFeed.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(),true);
        else
            Log.d(TAG, "primaryVideoFeed == null");
        Log.d(TAG, "Start Live Show thread");
        new Thread() {
            @Override
            public void run() {
                mStreamMananger.setLiveUrl(mliveShowUrl);
                int result = mStreamMananger.startStream();
                //DJISDKManager.getInstance().getLiveStreamManager().setStartTime();

                Log.d(TAG, "startLive:" + result +
                        "\n isVideoStreamSpeedConfigurable:" + DJISDKManager.getInstance().getLiveStreamManager().isVideoStreamSpeedConfigurable() +
                        "\n isLiveAudioEnabled:" + DJISDKManager.getInstance().getLiveStreamManager().isLiveAudioEnabled());
            }
        }.start();
        return 0;
    }

    public void stopLiveShow() {
        if (!isLiveStreamManagerOn()) {
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().stopStream();
        Log.d(TAG, "Stop Live Show");
    }

    public void getInfo() {
        if (!isLiveStreamManagerOn()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Video BitRate:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoBitRate()).append(" kpbs\n");
        sb.append("Audio BitRate:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveAudioBitRate()).append(" kpbs\n");
        sb.append("Video FPS:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoFps()).append("\n");
        sb.append("Video Cache size:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoCacheSize()).append(" frame");
        sb.append("Video Resolution:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoResolution());

        Log.d(TAG, sb.toString());
    }

    private boolean isLiveStreamManagerOn() {
        if (mStreamMananger == null)
            mStreamMananger = DJISDKManager.getInstance().getLiveStreamManager();
        if (mStreamMananger == null) {
            Log.d(TAG, "No live stream manager!");
            return false;
        }
        return true;
    }

    private void setBitRate(int bitRate) {
        lastBitRate = bitRate;
        DJISDKManager.getInstance().getLiveStreamManager().setLiveVideoBitRate(bitRate);
    }
    private float getFrameRate() {
        return DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoFps();
    }

    private void setResolution(LiveVideoResolution res) {
        DJISDKManager.getInstance().getLiveStreamManager().setLiveVideoResolution(res);
    }
}
