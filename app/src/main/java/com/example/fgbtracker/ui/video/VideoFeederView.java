package com.example.fgbtracker.ui.video;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.example.fgbtracker.MainActivity;
import com.example.fgbtracker.ui.PresentableView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import dji.common.airlink.PhysicalSource;
import dji.common.airlink.VideoFeedPriority;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.keysdk.AirLinkKey;
import dji.keysdk.KeyManager;
import dji.keysdk.ProductKey;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.SetCallback;
import dji.sdk.airlink.AirLink;
import dji.sdk.airlink.OcuSyncLink;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import fgbtracker.R;

/**
 * Class that manage live video feed from DJI products to the mobile device.
 * Also give the example of "getPrimaryVideoFeed" and "getSecondaryVideoFeed".
 */
public class VideoFeederView extends LinearLayout
        implements View.OnClickListener, PresentableView, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = VideoFeederView.class.getName();
    //private PopupNumberPicker popupNumberPicker = null;
    //private PopupNumberPickerDouble popupNumberPickerDouble = null;
    private static int[] INDEX_CHOSEN = { -1, -1, -1 };
    private Handler handler = new Handler(Looper.getMainLooper());
    private Context context;
    private VideoFeedView primaryVideoFeed;
    private VideoFeeder.PhysicalSourceListener sourceListener;
    private AirLinkKey extEnabledKey;
    private AirLinkKey lbBandwidthKey;
    private AirLinkKey hdmiBandwidthKey;
    private AirLinkKey mainCameraBandwidthKey;
    private AirLinkKey assignSourceToPrimaryChannelKey;
    private AirLinkKey primaryVideoBandwidthKey;
    private SetCallback setBandwidthCallback;
    private SetCallback setExtEnableCallback;
    private ActionCallback allocSourceCallback;
    private AirLink airLink;
    private View primaryCoverView;
    private String cameraListStr;

    public VideoFeederView(Context context) {
        super(context);
        this.context = context;
        init(context);
    }
    public VideoFeederView(Context context, AttributeSet attrs) {
        super(context);
        this.context = context;
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setClickable(true);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_video_feeder, this, true);

        initAirLink();
        initAllKeys();
        initUI();
        initCallbacks();
        setUpListeners();
    }

    private void initUI() {

        //primaryVideoFeedTitle = (TextView) findViewById(R.id.primary_video_feed_title);
        primaryVideoFeed = (VideoFeedView) findViewById(R.id.primary_video_feed);
        primaryCoverView = findViewById(R.id.primary_cover_view);
        primaryVideoFeed.setCoverView(primaryCoverView);
        disableAllButtons();
        initEXTSwitch();
    }

    private void initAirLink() {
        BaseProduct baseProduct = DJISDKManager.getInstance().getProduct();
        if (null != baseProduct && null != baseProduct.getAirLink()) {
            airLink = baseProduct.getAirLink();
        }
    }

    private void initAllKeys() {
        extEnabledKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.IS_EXT_VIDEO_INPUT_PORT_ENABLED);
        lbBandwidthKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT);
        hdmiBandwidthKey =
                AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_HDMI_VIDEO_INPUT_PORT);
        mainCameraBandwidthKey = AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LEFT_CAMERA);
        assignSourceToPrimaryChannelKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.ASSIGN_SOURCE_TO_PRIMARY_CHANNEL);
        primaryVideoBandwidthKey = AirLinkKey.createOcuSyncLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_PRIMARY_VIDEO);
    }

    private void initCallbacks() {
        setBandwidthCallback = new SetCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Set key value successfully");
                if (primaryVideoFeed != null) {
                    primaryVideoFeed.changeSourceResetKeyFrame();
                }
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                Log.e(TAG, "Failed to set: " + error.getDescription());
            }
        };

        setExtEnableCallback = new SetCallback() {
            @Override
            public void onSuccess() {
                updateExtSwitchValue(null);
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                updateExtSwitchValue(null);
            }
        };

        allocSourceCallback = new ActionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Perform action successfully");
            }

            @Override
            public void onFailure(@NonNull DJIError error) {
                Log.e(TAG, "Failed to action: " + error.getDescription());
            }
        };
    }

    private void initEXTSwitch() {
        updateExtSwitchValue(null);
    }

    private void updateExtSwitchValue(Object value) {
        if (value == null && KeyManager.getInstance() != null) {
            value = KeyManager.getInstance().getValue(extEnabledKey);
        }
        final Object switchValue = value;
        if (switchValue != null) {
            VideoFeederView.this.post(new Runnable() {
                @Override
                public void run() {
                    //enableSingle.setOnCheckedChangeListener(null);
                    //enableSingle.setChecked((Boolean) switchValue);
                    //enableSingle.setOnCheckedChangeListener(VideoFeederView.this);
                    enableExtButtons((Boolean) switchValue);
                }
            });
        }
    }

    private void enableExtButtons(boolean isExtEnabled) {
       // enableSingle.setEnabled(true);

        if (isExtEnabled) {
            ;
        } else {
            ;
        }
    }

    private void disableAllButtons() {
        //enableSingle.setEnabled(false);
       ;
    }

    private void setUpListeners() {
        sourceListener = new VideoFeeder.PhysicalSourceListener() {
            @Override
            public void onChange(VideoFeeder.VideoFeed videoFeed, PhysicalSource newPhysicalSource) {
                if (videoFeed == VideoFeeder.getInstance().getPrimaryVideoFeed()) {
                    String newText = "Primary Source: " + newPhysicalSource.toString();
                }
            }
        };

        setVideoFeederListeners(true);
    }

    private void tearDownListeners() {
        setVideoFeederListeners(false);
    }

    private void setVideoFeederListeners(boolean isOpen) {
        if (VideoFeeder.getInstance() == null) return;

        final BaseProduct product = DJISDKManager.getInstance().getProduct();
        updateM210SeriesButtons();
        updateM300Buttons();
        if (product != null) {
            VideoFeeder.VideoDataListener primaryVideoDataListener =
                    primaryVideoFeed.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
            if (isOpen) {
                String newText =
                        "Primary Source: " + VideoFeeder.getInstance().getPrimaryVideoFeed().getVideoSource().name();
                if (Helper.isMultiStreamPlatform()) {
                    String newTextFpv = "Secondary Source: " + VideoFeeder.getInstance()
                            .getSecondaryVideoFeed()
                            .getVideoSource()
                            .name();
                }
                VideoFeeder.getInstance().addPhysicalSourceListener(sourceListener);
            } else {
                VideoFeeder.getInstance().removePhysicalSourceListener(sourceListener);
                VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(primaryVideoDataListener);
            }
        }
    }

    private void updateM210SeriesButtons() {
        if (isM210SeriesTwoCameraConnected()) {

            VideoFeederView.this.post(new Runnable() {
                @Override
                public void run() {
                    disableAllButtons();
                }
            });
        }
    }

    private void updateM300Buttons() {
        if (Helper.isM300Product()) {
            VideoFeederView.this.post(new Runnable() {
                @Override
                public void run() {
                    disableAllButtons();
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        ;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        ;
    }

    private void onClickSingleSwitch(boolean checked) {
        KeyManager.getInstance().setValue(extEnabledKey, checked, setExtEnableCallback);
    }

    private void onClickDualSwitch(boolean checked) {
        KeyManager.getInstance().setValue(extEnabledKey, !checked, setExtEnableCallback);
    }

    private void onClickLBOnlyBtn() {
        KeyManager.getInstance().setValue(lbBandwidthKey, 1.0f, setBandwidthCallback);
    }

    private void onClickExtOnlyBtn() {

        KeyManager.getInstance().setValue(lbBandwidthKey, 0.0f, setBandwidthCallback);
    }

    private void onClickLBAndExtBtn() {
        KeyManager.getInstance().setValue(lbBandwidthKey, 0.5f, setBandwidthCallback);
    }

    private void onClickHDMIOnlyBtn() {
        KeyManager.getInstance().setValue(hdmiBandwidthKey, 1.0f, setBandwidthCallback);
    }

    private void onClickAVOnlyBtn() {
        KeyManager.getInstance().setValue(hdmiBandwidthKey, 0.0f, setBandwidthCallback);
    }

    private void onClickHDMIAndAVBtn() {
        KeyManager.getInstance().setValue(hdmiBandwidthKey, 0.5f, setBandwidthCallback);
    }

    private void onClickLeftAndFpvBtn() {
        if (airLink != null) {
            if (airLink.isOcuSyncLinkSupported()) {
                KeyManager.getInstance().performAction(assignSourceToPrimaryChannelKey, allocSourceCallback, PhysicalSource.LEFT_CAM, PhysicalSource.FPV_CAM);
                KeyManager.getInstance().setValue(primaryVideoBandwidthKey, 1.0f, setBandwidthCallback);

            } else {
                KeyManager.getInstance().setValue(lbBandwidthKey, 0.8f, null);
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 1.0f, setBandwidthCallback);
            }
        }
    }

    private void onClickRightAndFpvBtn() {
        if (airLink != null) {
            if (airLink.isOcuSyncLinkSupported()) {
                KeyManager.getInstance().performAction(assignSourceToPrimaryChannelKey, allocSourceCallback, PhysicalSource.RIGHT_CAM, PhysicalSource.FPV_CAM);
                KeyManager.getInstance().setValue(primaryVideoBandwidthKey, 0.0f, setBandwidthCallback);

            } else {
                KeyManager.getInstance().setValue(lbBandwidthKey, 0.8f, null);
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.0f, setBandwidthCallback);
            }
        }
    }

    private void onClickLeftAndRightBtn() {
        if (airLink != null) {
            if (airLink.isOcuSyncLinkSupported()) {
                KeyManager.getInstance().performAction(assignSourceToPrimaryChannelKey, allocSourceCallback, PhysicalSource.LEFT_CAM, PhysicalSource.RIGHT_CAM);
                KeyManager.getInstance().setValue(primaryVideoBandwidthKey, 0.5f, setBandwidthCallback);

            } else {
                KeyManager.getInstance().setValue(lbBandwidthKey, 1.0f, null);
                KeyManager.getInstance().setValue(mainCameraBandwidthKey, 0.5f, setBandwidthCallback);
            }
        }
    }

    private void onClickSetVideoSourceBtn() {
        if (Helper.isM300Product()) {
            if (airLink != null) {
                OcuSyncLink ocuSyncLink = airLink.getOcuSyncLink();
                if (ocuSyncLink != null) {

                    final PhysicalSource[] videoIndex = new PhysicalSource[]{
                            PhysicalSource.LEFT_CAM,
                            PhysicalSource.RIGHT_CAM,
                            PhysicalSource.TOP_CAM,
                            PhysicalSource.FPV_CAM
                    };

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            ocuSyncLink.assignSourceToPrimaryChannel(videoIndex[INDEX_CHOSEN[0]], videoIndex[INDEX_CHOSEN[1]], new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError error) {
                                    Log.d(TAG,"Set Video Source : " + (error != null ? error.getDescription() : "Success"));
                                }
                            });
                            resetIndex();
                        }
                    };

                    ArrayList<String> videos = new ArrayList<>(
                            Arrays.asList(
                                    PhysicalSource.LEFT_CAM.name(),
                                    PhysicalSource.RIGHT_CAM.name(),
                                    PhysicalSource.TOP_CAM.name(),
                                    PhysicalSource.FPV_CAM.name()
                            ));

                   // initPopupNumberPicker(videos, videos, r);
                }
            }
        }
    }

    private void onClickSetPrimaryPriorityBtn() {
        if (Helper.isM300Product()) {
            VideoFeeder.VideoFeed videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
            VideoFeedPriority[] priorityIndex = new VideoFeedPriority[]{
                    VideoFeedPriority.HIGH,
                    VideoFeedPriority.MEDIUM,
                    VideoFeedPriority.LOW
            };

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    videoFeed.setPriority(VideoFeedPriority.find(INDEX_CHOSEN[0]), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            Log.d(TAG,"Set Primary priority : " + (error != null ? error.getDescription() : "Success"));
                        }
                    });
                    resetIndex();
                }
            };
            ArrayList<String> priorities = new ArrayList<>(
                    Arrays.asList(
                            VideoFeedPriority.HIGH.name(),
                            VideoFeedPriority.MEDIUM.name(),
                            VideoFeedPriority.LOW.name()
                    ));
            //initPopupNumberPicker(priorities, r);
        }
    }

    private void onClickGetPrimaryPriorityBtn() {
        if (Helper.isM300Product()) {
            VideoFeeder.VideoFeed videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
            videoFeed.getPriority(new CommonCallbacks.CompletionCallbackWith<VideoFeedPriority>() {
                @Override
                public void onSuccess(VideoFeedPriority priority) {
                    Log.d(TAG,"Get Primary priority : " + priority);
                }

                @Override
                public void onFailure(DJIError error) {
                    Log.d(TAG,"Get Primary priority failed: " + error.getDescription());
                }
            });
        }
    }

    private boolean isM210SeriesTwoCameraConnected() {
        Object model = null;
        if (KeyManager.getInstance() != null) {
            model = KeyManager.getInstance().getValue(ProductKey.create(ProductKey.MODEL_NAME));
        }
        if (model != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null && (product instanceof Aircraft)) {
                List<Camera> cameraList = ((Aircraft) product).getCameras();
                if (cameraList != null) {
                    if (cameraListStr == null) {
                        cameraListStr = new String();
                    }
                    cameraListStr = "";
                    for (int i = 0; i < cameraList.size(); i++) {
                        Camera camera = cameraList.get(i);
                        cameraListStr += "Camera "
                                + i
                                + " is "
                                + camera.getDisplayName()
                                + " is connected "
                                + camera.isConnected()
                                + " camera component index is "
                                + +camera.getIndex()
                                + "\n";
                    }
                }
                if ((model == Model.MATRICE_210
                        || model == Model.MATRICE_210_RTK
                        || model == Model.MATRICE_210_V2
                        || model == Model.MATRICE_210_RTK_V2)) {
                    return (cameraList != null
                            && cameraList.size() == 2
                            && cameraList.get(0).isConnected()
                            && cameraList.get(1).isConnected());
                }
            }
        }

        return false;
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_video_feeder;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //DJISampleApplication.getEventBus().post(new MainActivity.RequestStartFullScreenEvent());
    }

    @Override
    protected void onDetachedFromWindow() {
        //DJISampleApplication.getEventBus().post(new MainActivity.RequestEndFullScreenEvent());
        tearDownListeners();
        super.onDetachedFromWindow();
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    public void resetIndex() {
        INDEX_CHOSEN = new int[3];
        INDEX_CHOSEN[0] = -1;
        INDEX_CHOSEN[1] = -1;
        INDEX_CHOSEN[2] = -1;
    }
/*
    private void initPopupNumberPicker(ArrayList<String> list, final Runnable r) {
        popupNumberPicker = new PopupNumberPicker(context, list, new PickerValueChangeListener() {

            @Override
            public void onValueChange(int pos1, int pos2) {
                popupNumberPicker.dismiss();
                popupNumberPicker = null;
                INDEX_CHOSEN[0] = pos1;
                handler.post(r);
            }
        }, 500, 200, 0);
        popupNumberPicker.showAtLocation(this, Gravity.CENTER, 0, 0);
    }

    private void initPopupNumberPicker(ArrayList<String> list1, ArrayList<String> list2, final Runnable r) {
        popupNumberPickerDouble =
                new PopupNumberPickerDouble(context, list1, null, list2, null, new PickerValueChangeListener() {

                    @Override
                    public void onValueChange(int pos1, int pos2) {
                        popupNumberPickerDouble.dismiss();
                        popupNumberPickerDouble = null;
                        INDEX_CHOSEN[0] = pos1;
                        INDEX_CHOSEN[1] = pos2;
                        handler.post(r);
                    }
                }, 500, 200, 0);

        popupNumberPickerDouble.showAtLocation(this, Gravity.CENTER, 0, 0);
    }*/
}