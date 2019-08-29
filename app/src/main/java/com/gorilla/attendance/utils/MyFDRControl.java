package com.gorilla.attendance.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.gorillatechnology.fdrcontrol.CameraPreview;
import com.gorillatechnology.fdrcontrol.FDRControl;
import com.gorillatechnology.fdrcontrol.FDRServiceClient;
import com.gorillatechnology.fdrcontrol.FDRServiceClient.FRResponseListener;
import com.gorillatechnology.fdrcontrol.FDRServiceClient.FRResult;
import com.gorillatechnology.fdrcontrol.FDRServiceClient.FaceEventResponseListener;
import com.gorillatechnology.fdrcontrol.FDRServiceClient.ResponseListener;
import com.gorillatechnology.fdrcontrol.FaceEvent;
import com.gorillatechnology.fdrcontrol.FaceSelectionGrid;
import com.gorillatechnology.fdrcontrol.FaceView;
import com.gorillatechnology.fdrcontrol.IODImage;
import com.gorillatechnology.fdrcontrol.IODObject;
import com.gorillatechnology.fdrcontrol.IODObjectSet;
import com.gorillatechnology.fdrcontrol.IODObjectSet.Statistics;
import com.gorillatechnology.fdrcontrol.R.raw;
import com.gorillatechnology.videosource.RTSPClient;
import com.gorillatechnology.videosource.RTSPClient.StatusCallback;
import gorilla.iod.IntelligentObjectDetector;
import gorilla.iod.IntelligentObjectDetector.FR_Suitable_Type;
import gorilla.iod.IntelligentObjectDetector.Face_Occlusion_Type;
import gorilla.iod.IntelligentObjectDetector.IOD_CAMERA_DIRECTION;
import gorilla.iod.IntelligentObjectDetector.IOD_FUNC_CODE;
import gorilla.iod.IntelligentObjectDetector.IOD_FUNC_ENABLE_STATUS;
import gorilla.iod.IntelligentObjectDetector.IOD_SRC_IMG_FORMAT;
import gorilla.iod.IntelligentObjectDetector.Liveness_Type;
import gorilla.iod.IntelligentObjectDetector.Object_Detector_Type;
import gorilla.iod.IntelligentObjectDetector.Type_IODInfo;
import gorilla.iod.IntelligentObjectDetector.Type_LivenessMotion;
import gorilla.iod.IntelligentObjectDetector.Type_LivenessMotionRequest;
import gorilla.iod.IntelligentObjectDetector.Type_RectInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Author: Tsung Hsuan, Lai
 * Created on: 2019/8/27
 * Description:
 */
@SuppressLint({"ViewConstructor", "DefaultLocale", "LogNotTimber"})
public class MyFDRControl extends RelativeLayout {
    private static final String TAG = "FDRControl";
    private static final double extendedFaceRatio = 1.5D;
    private static int maxFaceWidthForFDRService = 160;
    private static FDRControl.CaptureMode captureMode;
    private static final int enrollSamplingPeriod = 500;
    private static final int enrollMaxImageNumber = 10;
    private static final int recognizeSamplingPeriod = 1000;
    private static final int recognizeMaxImageNumber = 1;
    private static final int identifyRequestPeriod = 5000;
    private static Boolean bIsTriggerAgain;
    private static final String defIODImageLogPath = "/Gorilla/FDRControl/IODImageLog";
    private Boolean isTouchFocus = true;
    private FDRControl.Mode opMode;
    private FDRControl.FaceSource faceSource;
    private CameraPreview cameraPreview;
    private RTSPClient rtspClient;
    private FaceView faceView;
    private FaceSelectionGrid faceSelectionGrid;
    private Bitmap faceBitmap;
    private ByteBuffer rgbBuffer;
    Thread vaThread;
    private String libPath;
    Thread setObjThread;
    private int maxObjNumber;
    private int detectionLevel;
    private int scanningLevel;
    private int delayTimeMS;
    private static final double defMinObjWidthRatioOfShortEdge = 0.16666666666666666D;
    private static final double defMaxObjWidthRatioOfShortEdge = 0.6666666666666666D;
    private int minObjWidth;
    private int maxObjWidth;
    private int bestMinObjWidth;
    private int bestMaxObjWidth;
    private Rect bestObjLoc;
    private boolean isAutoSelectLargestFace;
    private boolean isIODGlassesEnable;
    private boolean isIODGenderEnable;
    private boolean isIODLivenessEnable;
    private boolean isIODFRSuitableEnable;
    private boolean isIODOcclusionEnable;
    private boolean isEnableLivenessMotion;
    private int genderLevel;
    private boolean isIODImageLogEnable;
    private String logPath;
    private boolean isDoIOD;
    private int imageWidth;
    private int imageHeight;
    private int imageOrientation;
    private int numberOfFaceDetected;
    private IntelligentObjectDetector faceDetector;
    private Type_IODInfo[] faceDetected;
    private Type_LivenessMotion[] livenessMotionInfo;
    private Type_LivenessMotionRequest[] livenessMotionRequest;
    private int faceDetectMode;
    private SparseArray<IODTriggerInfo> iodTriggerTimeArray;
    private IODObjectSet enrollObjectSet;
    private int selectedObjId;
    private String sourceId;
    private int regLevel;
    private Boolean isStartIdentify;
    private IODObjectSet recognizeObjectSet;
    private Integer identifyFlag;
    private int randomId;
    private boolean exit;
    private ImageRawData waitingPreviewData;
    private ImageRawData previewData;
    private ImageRawData rtspPreview;
    private boolean isHideFace;
    private float bitmapScaleRatioX;
    private float bitmapScaleRatioY;
    private boolean firstCameraStart;
    private cameraPreviewCallback camPreviewCallback;
    private int livenessRequestNum;
    FDRServiceClient fdrClient;
    IODTriggerResponseListener iodTrigerRespListener;
    FDRControlResponseListener fdrCtrlRespListener;
    RTSPStatusListener rtspStatusListener;
    IODLivenessMotionResponseListener iodLivenessMotionRespListener;
    private PreviewCallback previewCallback;
    private StatusCallback rtspStatusCallback;
    private com.gorillatechnology.videosource.RTSPClient.PreviewCallback rtspPreviewCallback;
    private AutoFocusCallback autoFocusCallback;
    private FaceEventResponseListener faceEventListener;
    private Runnable doIdentifyTarget;
    private FRResponseListener frListenerForCtrl;

    public MyFDRControl(Context context, FDRControl.Mode mode, FDRControl.FaceSource source) {
        super(context);
        this.opMode = FDRControl.Mode.IOD;
        this.faceSource = FDRControl.FaceSource.CAMERA;
        this.cameraPreview = null;
        this.rtspClient = null;
        this.faceView = null;
        this.faceSelectionGrid = null;
        this.faceBitmap = null;
        this.rgbBuffer = null;
        this.vaThread = null;
        this.libPath = null;
        this.setObjThread = null;
        this.maxObjNumber = 10;
        this.detectionLevel = 3;
        this.scanningLevel = 3;
        this.delayTimeMS = 500;
        this.minObjWidth = 0;
        this.maxObjWidth = 0;
        this.bestMinObjWidth = 0;
        this.bestMaxObjWidth = 0;
        this.bestObjLoc = null;
        this.isAutoSelectLargestFace = true;
        this.isIODGlassesEnable = false;
        this.isIODGenderEnable = false;
        this.isIODLivenessEnable = false;
        this.isIODFRSuitableEnable = false;
        this.isIODOcclusionEnable = false;
        this.isEnableLivenessMotion = false;
        this.genderLevel = 0;
        this.isIODImageLogEnable = false;
        this.logPath = null;
        this.isDoIOD = false;
        this.imageWidth = 0;
        this.imageHeight = 0;
        this.imageOrientation = 0;
        this.numberOfFaceDetected = 0;
        this.faceDetector = null;
        this.faceDetected = null;
        this.livenessMotionInfo = null;
        this.livenessMotionRequest = null;
        this.faceDetectMode = 1;
        this.iodTriggerTimeArray = new SparseArray();
        this.enrollObjectSet = new IODObjectSet(500, 10, captureMode);
        this.selectedObjId = 0;
        this.sourceId = "";
        this.regLevel = 3;
        this.isStartIdentify = false;
        this.recognizeObjectSet = new IODObjectSet(1000, 1, captureMode);
        this.identifyFlag = 0;
        this.randomId = 0;
        this.exit = false;
        this.waitingPreviewData = new ImageRawData();
        this.previewData = new ImageRawData();
        this.rtspPreview = new ImageRawData();
        this.isHideFace = false;
        this.bitmapScaleRatioX = 0.0F;
        this.bitmapScaleRatioY = 0.0F;
        this.firstCameraStart = false;
        this.camPreviewCallback = null;
        this.livenessRequestNum = 3;
        this.fdrClient = FDRServiceClient.instance();
        this.iodTrigerRespListener = null;
        this.fdrCtrlRespListener = null;
        this.rtspStatusListener = null;
        this.iodLivenessMotionRespListener = null;
        this.previewCallback = new NamelessClass_7();
        this.rtspStatusCallback = new NamelessClass_6();
        this.rtspPreviewCallback = new NamelessClass_5();
        this.autoFocusCallback = new NamelessClass_4();
        this.faceEventListener = new NamelessClass_3();
        this.doIdentifyTarget = new NamelessClass_2();
        this.frListenerForCtrl = new NamelessClass_1();
        this.init(mode, source, 0, 10, (String)null);
    }

    public MyFDRControl(Context context, FDRControl.Mode mode, FDRControl.FaceSource source, int cameraId) {
        super(context);
        this.opMode = FDRControl.Mode.IOD;
        this.faceSource = FDRControl.FaceSource.CAMERA;
        this.cameraPreview = null;
        this.rtspClient = null;
        this.faceView = null;
        this.faceSelectionGrid = null;
        this.faceBitmap = null;
        this.rgbBuffer = null;
        this.vaThread = null;
        this.libPath = null;
        this.setObjThread = null;
        this.maxObjNumber = 10;
        this.detectionLevel = 3;
        this.scanningLevel = 3;
        this.delayTimeMS = 500;
        this.minObjWidth = 0;
        this.maxObjWidth = 0;
        this.bestMinObjWidth = 0;
        this.bestMaxObjWidth = 0;
        this.bestObjLoc = null;
        this.isAutoSelectLargestFace = true;
        this.isIODGlassesEnable = false;
        this.isIODGenderEnable = false;
        this.isIODLivenessEnable = false;
        this.isIODFRSuitableEnable = false;
        this.isIODOcclusionEnable = false;
        this.isEnableLivenessMotion = false;
        this.genderLevel = 0;
        this.isIODImageLogEnable = false;
        this.logPath = null;
        this.isDoIOD = false;
        this.imageWidth = 0;
        this.imageHeight = 0;
        this.imageOrientation = 0;
        this.numberOfFaceDetected = 0;
        this.faceDetector = null;
        this.faceDetected = null;
        this.livenessMotionInfo = null;
        this.livenessMotionRequest = null;
        this.faceDetectMode = 1;
        this.iodTriggerTimeArray = new SparseArray();
        this.enrollObjectSet = new IODObjectSet(500, 10, captureMode);
        this.selectedObjId = 0;
        this.sourceId = "";
        this.regLevel = 3;
        this.isStartIdentify = false;
        this.recognizeObjectSet = new IODObjectSet(1000, 1, captureMode);
        this.identifyFlag = 0;
        this.randomId = 0;
        this.exit = false;
        this.waitingPreviewData = new ImageRawData();
        this.previewData = new ImageRawData();
        this.rtspPreview = new ImageRawData();
        this.isHideFace = false;
        this.bitmapScaleRatioX = 0.0F;
        this.bitmapScaleRatioY = 0.0F;
        this.firstCameraStart = false;
        this.camPreviewCallback = null;
        this.livenessRequestNum = 3;
        this.fdrClient = FDRServiceClient.instance();
        this.iodTrigerRespListener = null;
        this.fdrCtrlRespListener = null;
        this.rtspStatusListener = null;
        this.iodLivenessMotionRespListener = null;
        this.previewCallback = new NamelessClass_7();
        this.rtspStatusCallback = new NamelessClass_6();
        this.rtspPreviewCallback = new NamelessClass_5();
        this.autoFocusCallback = new NamelessClass_4();
        this.faceEventListener = new NamelessClass_3();
        this.doIdentifyTarget = new NamelessClass_2();
        this.frListenerForCtrl = new NamelessClass_1();
        this.init(mode, source, cameraId, 0, (String)null);
    }

    public MyFDRControl(Context context, FDRControl.Mode mode, FDRControl.FaceSource source, int cameraId, int maxObjNumber) {
        super(context);
        this.opMode = FDRControl.Mode.IOD;
        this.faceSource = FDRControl.FaceSource.CAMERA;
        this.cameraPreview = null;
        this.rtspClient = null;
        this.faceView = null;
        this.faceSelectionGrid = null;
        this.faceBitmap = null;
        this.rgbBuffer = null;
        this.vaThread = null;
        this.libPath = null;
        this.setObjThread = null;
        this.maxObjNumber = 10;
        this.detectionLevel = 3;
        this.scanningLevel = 3;
        this.delayTimeMS = 500;
        this.minObjWidth = 0;
        this.maxObjWidth = 0;
        this.bestMinObjWidth = 0;
        this.bestMaxObjWidth = 0;
        this.bestObjLoc = null;
        this.isAutoSelectLargestFace = true;
        this.isIODGlassesEnable = false;
        this.isIODGenderEnable = false;
        this.isIODLivenessEnable = false;
        this.isIODFRSuitableEnable = false;
        this.isIODOcclusionEnable = false;
        this.isEnableLivenessMotion = false;
        this.genderLevel = 0;
        this.isIODImageLogEnable = false;
        this.logPath = null;
        this.isDoIOD = false;
        this.imageWidth = 0;
        this.imageHeight = 0;
        this.imageOrientation = 0;
        this.numberOfFaceDetected = 0;
        this.faceDetector = null;
        this.faceDetected = null;
        this.livenessMotionInfo = null;
        this.livenessMotionRequest = null;
        this.faceDetectMode = 1;
        this.iodTriggerTimeArray = new SparseArray();
        this.enrollObjectSet = new IODObjectSet(500, 10, captureMode);
        this.selectedObjId = 0;
        this.sourceId = "";
        this.regLevel = 3;
        this.isStartIdentify = false;
        this.recognizeObjectSet = new IODObjectSet(1000, 1, captureMode);
        this.identifyFlag = 0;
        this.randomId = 0;
        this.exit = false;
        this.waitingPreviewData = new ImageRawData();
        this.previewData = new ImageRawData();
        this.rtspPreview = new ImageRawData();
        this.isHideFace = false;
        this.bitmapScaleRatioX = 0.0F;
        this.bitmapScaleRatioY = 0.0F;
        this.firstCameraStart = false;
        this.camPreviewCallback = null;
        this.livenessRequestNum = 3;
        this.fdrClient = FDRServiceClient.instance();
        this.iodTrigerRespListener = null;
        this.fdrCtrlRespListener = null;
        this.rtspStatusListener = null;
        this.iodLivenessMotionRespListener = null;
        this.previewCallback = new NamelessClass_7();
        this.rtspStatusCallback = new NamelessClass_6();
        this.rtspPreviewCallback = new NamelessClass_5();
        this.autoFocusCallback = new NamelessClass_4();
        this.faceEventListener = new NamelessClass_3();
        this.doIdentifyTarget = new NamelessClass_2();
        this.frListenerForCtrl = new NamelessClass_1();
        this.init(mode, source, cameraId, maxObjNumber, (String)null);
    }

    public MyFDRControl(Context context, FDRControl.Mode mode, FDRControl.FaceSource source, int cameraId, int maxObjNumber, String libPath) {
        super(context);
        this.opMode = FDRControl.Mode.IOD;
        this.faceSource = FDRControl.FaceSource.CAMERA;
        this.cameraPreview = null;
        this.rtspClient = null;
        this.faceView = null;
        this.faceSelectionGrid = null;
        this.faceBitmap = null;
        this.rgbBuffer = null;
        this.vaThread = null;
        this.libPath = null;
        this.setObjThread = null;
        this.maxObjNumber = 10;
        this.detectionLevel = 3;
        this.scanningLevel = 3;
        this.delayTimeMS = 500;
        this.minObjWidth = 0;
        this.maxObjWidth = 0;
        this.bestMinObjWidth = 0;
        this.bestMaxObjWidth = 0;
        this.bestObjLoc = null;
        this.isAutoSelectLargestFace = true;
        this.isIODGlassesEnable = false;
        this.isIODGenderEnable = false;
        this.isIODLivenessEnable = false;
        this.isIODFRSuitableEnable = false;
        this.isIODOcclusionEnable = false;
        this.isEnableLivenessMotion = false;
        this.genderLevel = 0;
        this.isIODImageLogEnable = false;
        this.logPath = null;
        this.isDoIOD = false;
        this.imageWidth = 0;
        this.imageHeight = 0;
        this.imageOrientation = 0;
        this.numberOfFaceDetected = 0;
        this.faceDetector = null;
        this.faceDetected = null;
        this.livenessMotionInfo = null;
        this.livenessMotionRequest = null;
        this.faceDetectMode = 1;
        this.iodTriggerTimeArray = new SparseArray();
        this.enrollObjectSet = new IODObjectSet(500, 10, captureMode);
        this.selectedObjId = 0;
        this.sourceId = "";
        this.regLevel = 3;
        this.isStartIdentify = false;
        this.recognizeObjectSet = new IODObjectSet(1000, 1, captureMode);
        this.identifyFlag = 0;
        this.randomId = 0;
        this.exit = false;
        this.waitingPreviewData = new ImageRawData();
        this.previewData = new ImageRawData();
        this.rtspPreview = new ImageRawData();
        this.isHideFace = false;
        this.bitmapScaleRatioX = 0.0F;
        this.bitmapScaleRatioY = 0.0F;
        this.firstCameraStart = false;
        this.camPreviewCallback = null;
        this.livenessRequestNum = 3;
        this.fdrClient = FDRServiceClient.instance();
        this.iodTrigerRespListener = null;
        this.fdrCtrlRespListener = null;
        this.rtspStatusListener = null;
        this.iodLivenessMotionRespListener = null;
        this.previewCallback = new NamelessClass_7();
        this.rtspStatusCallback = new NamelessClass_6();
        this.rtspPreviewCallback = new NamelessClass_5();
        this.autoFocusCallback = new NamelessClass_4();
        this.faceEventListener = new NamelessClass_3();
        this.doIdentifyTarget = new NamelessClass_2();
        this.frListenerForCtrl = new NamelessClass_1();
        this.init(mode, source, cameraId, maxObjNumber, libPath);
    }

    public MyFDRControl(Context context) {
        super(context);
    }

    public MyFDRControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyFDRControl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyFDRControl(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean setLivenessMotionRequestNum(int num) {
        if (this.faceDetector == null) {
            return false;
        } else {
            this.faceDetector.setLivenessMotionRequestNum(num);
            return true;
        }
    }

    public boolean resetLivenessMotion(int objID) {
        if (this.faceDetector == null) {
            return false;
        } else {
            this.faceDetector.resetLivenessMotion(objID);
            return true;
        }
    }

    public boolean setLivenessMotionRequest(int objID, int[] motionRequest) {
        if (this.faceDetector == null) {
            return false;
        } else {
            this.faceDetector.setLivenessMotionRequest(objID, motionRequest);
            return true;
        }
    }

    private void init(FDRControl.Mode mode, FDRControl.FaceSource source, int cameraId, int maxObjNumber, String libPath) {
        this.faceDetectMode = 1;
        this.opMode = mode;
        this.faceSource = source;
        this.libPath = libPath;
        this.setBackgroundColor(-16777216);
        if (this.faceSource == FDRControl.FaceSource.CAMERA || this.faceSource == FDRControl.FaceSource.RTSP) {
            this.faceView = new FaceView(this.getContext());
            this.faceView.setZOrderMediaOverlay(true);
            this.addView(this.faceView);
        }

        if (this.faceSource == FDRControl.FaceSource.CAMERA) {
            this.cameraPreview = new CameraPreview(this.getContext(), cameraId, this.previewCallback, this.autoFocusCallback);
            this.addView(this.cameraPreview);
            this.firstCameraStart = true;
        } else if (this.faceSource == FDRControl.FaceSource.RTSP) {
            this.rtspClient = new RTSPClient();
            this.rtspClient.setStatusCallback(this.rtspStatusCallback);
            this.rtspClient.setPreviewCallback(this.rtspPreviewCallback);
        }

        if (maxObjNumber > 0) {
            this.maxObjNumber = maxObjNumber;
        }

        this.faceSelectionGrid = new FaceSelectionGrid(this.getContext());
        this.faceSelectionGrid.setVisibility(View.GONE);
        this.addView(this.faceSelectionGrid);
        this.faceSelectionGrid.initView();
        this.createIODBinFiles();
        if (this.faceSource == FDRControl.FaceSource.EVENT) {
            this.fdrClient.setFaceEventResponseListener(this.faceEventListener);
            this.faceSelectionGrid.setVisibility(View.VISIBLE);
        } else {
            this.fdrClient.setFRResponseListener(this.frListenerForCtrl);
        }

        Runnable r = new VARunnable();
        this.vaThread = new Thread(r);
        this.vaThread.start();
    }

    private static float getDensity(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.density;
    }

    private static int convertDpToPixel(int dp, Context context) {
        int px = Math.round((float)dp * getDensity(context));
        return px;
    }

    private static float convertPixelToDp(float px, Context context) {
        float dp = px / getDensity(context);
        return dp;
    }

    private Rect getPixelRect(float left, float top, float right, float bottom) {
        int leftPixel = convertDpToPixel(Math.round(left), this.getContext());
        int topPixel = convertDpToPixel(Math.round(top), this.getContext());
        int rightPixel = convertDpToPixel(Math.round(right), this.getContext());
        int bottomPixel = convertDpToPixel(Math.round(bottom), this.getContext());
        Rect rect = new Rect(leftPixel, topPixel, rightPixel, bottomPixel);
        return rect;
    }

    public void hideFaceView(boolean isHide) {
        this.isHideFace = isHide;
        if (this.faceView != null) {
            this.faceView.isHideCanvas(isHide);
        }

    }

    public void drawText(String text) {
        if (this.faceView != null) {
            this.faceView.drawText(text);
        }

    }

    public void drawDetectRect(float left, float top, float right, float bottom, Paint paint, boolean bIsDraw) {
        if (this.cameraPreview != null) {
            this.cameraPreview.drawDetectRect(this.getPixelRect(left, top, right, bottom), paint, bIsDraw);
        }

    }

    public void setBestObjLoc(float left, float top, float right, float bottom) {
        Runnable r = new SetBestObjRunnable(left, top, right, bottom);
        this.setObjThread = new Thread(r);
        this.setObjThread.start();
    }

    private void setBestObj(float left, float top, float right, float bottom, double cameraScaleX, double cameraScaleY) {
        this.bestObjLoc = this.getPixelRect(left, top, right, bottom);
        this.bestObjLoc.left = (int)((double)this.bestObjLoc.left * cameraScaleX);
        this.bestObjLoc.top = (int)((double)this.bestObjLoc.top * cameraScaleY);
        this.bestObjLoc.right = (int)((double)this.bestObjLoc.right * cameraScaleX);
        this.bestObjLoc.bottom = (int)((double)this.bestObjLoc.bottom * cameraScaleY);
    }

    private void createIODBinFiles() {
        this.rawResourceToFileIfNotExist(raw.a, "a.bin");
        this.rawResourceToFileIfNotExist(raw.b, "b.bin");
        this.rawResourceToFileIfNotExist(raw.c, "c.txt");
        this.rawResourceToFileIfNotExist(raw.d, "d.txt");
        this.rawResourceToFileIfNotExist(raw.e, "e.bin");
        this.rawResourceToFileIfNotExist(raw.f, "f.txt");
    }

    private void rawResourceToFileIfNotExist(int resId, String fileName) {
        String path = this.getBinFilePath();
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, fileName);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = this.getResources().openRawResource(resId);
            outputStream = new FileOutputStream(file);
            byte[] bytes = new byte[inputStream.available()];

            int read;
            while((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException var22) {
            Log.e("FDRControl", var22.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException var21) {
                    var21.printStackTrace();
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException var20) {
                    var20.printStackTrace();
                }
            }

        }

    }

    private String getBinFilePath() {
        String appDir = this.getContext().getFilesDir().toString();
        return String.format("%s/Bin", appDir);
    }

    public void setCameraPreviewCallback(cameraPreviewCallback callback) {
        this.camPreviewCallback = callback;
    }

    private void doFaceDetect(ImageRawData rawImg) {
        if (this.faceDetector == null || this.imageWidth != rawImg.width || this.imageHeight != rawImg.height || this.imageOrientation != rawImg.degree) {
            if (this.faceDetector != null) {
                this.faceDetector.free();
                this.faceDetector = null;
            }

            Random rand = new Random();
            this.randomId = rand.nextInt(100) * 100;
            this.enrollObjectSet.clear();
            this.recognizeObjectSet.clear();
            this.selectedObjId = 0;
            boolean isMirror = false;
            if (this.cameraPreview != null) {
                isMirror = this.cameraPreview.isFacingFront();
            }

            IOD_CAMERA_DIRECTION direction = degreeToIODCamDirt(rawImg.degree, isMirror);
            this.faceDetector = new IntelligentObjectDetector(this.getBinFilePath(), Object_Detector_Type.GORILLA_IOD_FACE_DETECTOR, rawImg.width, rawImg.height, rawImg.format, direction, this.maxObjNumber, this.libPath);
            int shortEdge = Math.min(rawImg.width, rawImg.height);
            if (this.maxObjWidth == 0) {
                this.maxObjWidth = (int)((double)shortEdge * 0.6666666666666666D);
            }

            if (this.minObjWidth == 0) {
                this.minObjWidth = (int)((double)shortEdge * 0.16666666666666666D);
            }

            this.faceDetector.setParam(this.minObjWidth, this.maxObjWidth, this.detectionLevel, this.scanningLevel);
            IOD_FUNC_ENABLE_STATUS isGenderEnable = this.isIODGenderEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_FEATURE_GENDER, isGenderEnable);
            this.faceDetector.setGenderDetectionLevel(this.genderLevel);
            this.faceDetector.setFaceDetectionMode(this.faceDetectMode);
            IOD_FUNC_ENABLE_STATUS isGlassesEnable = this.isIODGlassesEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_FEATURE_GLASSES, isGlassesEnable);
            IOD_FUNC_ENABLE_STATUS isLivenessEnable = this.isIODLivenessEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_LIVENESS_DETECT, isLivenessEnable);
            IOD_FUNC_ENABLE_STATUS isFRSuitableEnable = this.isIODFRSuitableEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_FR_SUITABLE, isFRSuitableEnable);
            IOD_FUNC_ENABLE_STATUS isIODOcclusionEnable = this.isIODOcclusionEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_OCCLUSION, isIODOcclusionEnable);
            IOD_FUNC_ENABLE_STATUS isLivenessMotionEnable = this.isEnableLivenessMotion ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_LIVENESS_MOTION, isLivenessMotionEnable);
            if (this.faceDetected == null) {
                this.faceDetected = new Type_IODInfo[this.maxObjNumber];

                for(int i = 0; i < this.maxObjNumber; ++i) {
                    this.faceDetected[i] = new Type_IODInfo();
                }
            }

            this.imageWidth = rawImg.width;
            this.imageHeight = rawImg.height;
            this.imageOrientation = rawImg.degree;
        }

        if (rawImg.data != null) {
            this.numberOfFaceDetected = this.faceDetector.exec(rawImg.data, this.faceDetected);
        } else {
            Log.i("FDRControl", String.format("rawImg.data = null."));
        }

        Log.i("FDRControl", String.format("%d faces detected.", this.numberOfFaceDetected));
    }

    private static IOD_CAMERA_DIRECTION degreeToIODCamDirt(int degree, boolean mirror) {
        IOD_CAMERA_DIRECTION dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_0;
        if (mirror) {
            if (degree == 0) {
                dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_0_MIRROR;
            } else if (degree == 90) {
                dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_90_MIRROR;
            } else if (degree == 180) {
                dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_180_MIRROR;
            } else if (degree == 270) {
                dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_270_MIRROR;
            }
        } else if (degree == 90) {
            dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_90;
        } else if (degree == 180) {
            dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_180;
        } else if (degree == 270) {
            dirt = IOD_CAMERA_DIRECTION.IOD_CAMERA_DEGREE_270;
        }

        return dirt;
    }

    public void setFaceListParam(int samplingNum, int samplingTime, FDRControl.CaptureMode samplingMode, int maxImageWidth) {
        maxFaceWidthForFDRService = maxImageWidth;
        captureMode = samplingMode;
        this.enrollObjectSet.clear();
        this.recognizeObjectSet.clear();
        this.enrollObjectSet = new IODObjectSet(samplingTime, samplingNum, captureMode);
        this.recognizeObjectSet = new IODObjectSet(samplingTime, samplingNum, captureMode);
    }

    public void setFocusMode(FDRControl.FocusMode focusMode, boolean touchFocus) {
        this.isTouchFocus = touchFocus;
        if (this.cameraPreview != null) {
            switch(focusMode) {
                case AUTO:
                    this.cameraPreview.setFocusMode("auto");
                    break;
                case INFINITY:
                    this.cameraPreview.setFocusMode("infinity");
                    break;
                case MACRO:
                    this.cameraPreview.setFocusMode("macro");
                    break;
                case FIXED:
                    this.cameraPreview.setFocusMode("fixed");
                    break;
                case EDOF:
                    this.cameraPreview.setFocusMode("edof");
                    break;
                case CONTINUOUS_VIDEO:
                    this.cameraPreview.setFocusMode("continuous-video");
                    break;
                case CONTINUOUS_PICTURE:
                    this.cameraPreview.setFocusMode("continuous-picture");
            }
        }

    }

    public FDRControl.FocusMode getFocusMode() {
        if (this.cameraPreview != null) {
            String focusMode = this.cameraPreview.getFocusMode();
            if (focusMode == "auto") {
                return FDRControl.FocusMode.AUTO;
            } else if (focusMode == "infinity") {
                return FDRControl.FocusMode.INFINITY;
            } else if (focusMode == "macro") {
                return FDRControl.FocusMode.MACRO;
            } else if (focusMode == "fixed") {
                return FDRControl.FocusMode.FIXED;
            } else if (focusMode == "edof") {
                return FDRControl.FocusMode.EDOF;
            } else if (focusMode == "continuous-video") {
                return FDRControl.FocusMode.CONTINUOUS_VIDEO;
            } else if (focusMode == "continuous-picture") {
                return FDRControl.FocusMode.CONTINUOUS_PICTURE;
            } else {
                Log.i("FDRControl", focusMode + "not identified by FDRControl.");
                return null;
            }
        } else {
            Log.i("FDRControl", "cameraPreview has not been initialized.");
            return null;
        }
    }

    public String GetMotion(int nMotion) {
        switch(nMotion) {
            case 0:
                return "Unknown";
            case 1:
                return "Front";
            case 2:
                return "Left";
            case 3:
                return "Up";
            case 4:
                return "Right";
            case 5:
                return "Down";
            case 6:
                return "Open Mouth";
            case 7:
                return "Blink Eye";
            default:
                return "";
        }
    }

    public String GetFaceStatus(int nStatus) {
        switch(nStatus) {
            case 0:
                return "Not Ready";
            case 1:
                return "Ready";
            case 2:
                return "Checked";
            default:
                return "";
        }
    }

    public String GetLivenessResult(int nStatus) {
        switch(nStatus) {
            case 0:
                return "No Result";
            case 1:
                return "Spoof";
            case 2:
                return "Unknown";
            case 3:
                return "Real";
            default:
                return "";
        }
    }

    public void updateFaceView(ImageRawData rawImg) {
        int bitmapWidth = 0;
        int bitmapHeight = 0;
        this.faceView.setVisibility(View.VISIBLE);
        if (rawImg.degree != 90 && rawImg.degree != 270) {
            bitmapWidth = rawImg.width;
            bitmapHeight = rawImg.height;
        } else {
            bitmapWidth = rawImg.height;
            bitmapHeight = rawImg.width;
        }

        boolean newBitmap = false;
        if (this.faceBitmap == null || this.faceBitmap.getWidth() != bitmapWidth || this.faceBitmap.getHeight() != bitmapHeight) {
            if (this.faceBitmap != null) {
                this.faceBitmap.recycle();
                this.faceBitmap = null;
            }

            Log.i("FDRControl", String.format("video size: %d * %d", rawImg.width, rawImg.height));
            this.faceBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);
            this.faceBitmap.setHasAlpha(false);
            if (rawImg.format == IOD_SRC_IMG_FORMAT.IOD_SRC_IMG_FORMAT_YUV) {
                this.rgbBuffer = ByteBuffer.allocate(this.faceBitmap.getByteCount());
            }

            newBitmap = true;
        }

        int i;
        if (newBitmap || this.faceView.getWidth() == 0 || this.faceView.getHeight() == 0) {
            i = 0;
            int faceViewHeight = 0;
            if (this.cameraPreview != null) {
                i = this.cameraPreview.getWidth();
                faceViewHeight = this.cameraPreview.getHeight();
            } else {
                View parentView = (View)this.getParent();
                if (parentView != null) {
                    double dispAspect = (double)parentView.getWidth() / (double)parentView.getHeight();
                    double videoAspect = (double)rawImg.width / (double)rawImg.height;
                    if (videoAspect < dispAspect) {
                        i = (int)((double)((float)parentView.getHeight()) * videoAspect);
                        faceViewHeight = parentView.getHeight();
                    } else {
                        i = parentView.getWidth();
                        faceViewHeight = (int)((double)((float)parentView.getWidth()) / videoAspect);
                    }

                    Log.d("FDRControl", String.format("parentView size: %d * %d,videoAspect:%1.2f", parentView.getWidth(), parentView.getHeight(), videoAspect));
                } else {
                    Log.d("FDRControl", String.format("parentView null"));
                }
            }

            this.faceView.setSize(i, faceViewHeight);
            this.bitmapScaleRatioX = (float)this.faceView.getCanvasWidth() / (float)bitmapWidth;
            this.bitmapScaleRatioY = (float)this.faceView.getCanvasHeight() / (float)bitmapHeight;
            this.faceView.setVisibility(View.INVISIBLE);
            Log.d("FDRControl", String.format("faceView size: %d * %d", i, faceViewHeight));
        }

        if (rawImg.format == IOD_SRC_IMG_FORMAT.IOD_SRC_IMG_FORMAT_YUV) {
            byte[] bytes = this.rgbBuffer.array();
            Type_RectInfo rect = new Type_RectInfo(0, 0, bitmapWidth, bitmapHeight);
            boolean isMirror = false;
            if (this.cameraPreview != null) {
                isMirror = this.cameraPreview.isFacingFront();
            }

            IOD_CAMERA_DIRECTION direction = degreeToIODCamDirt(rawImg.degree, isMirror);
            IntelligentObjectDetector.convertYUVtoRGB(rawImg.data, rawImg.width, rawImg.height, direction, rect, bytes, this.faceBitmap.getRowBytes(), 4);
        } else {
            this.rgbBuffer = ByteBuffer.wrap(rawImg.data);
        }

        this.rgbBuffer.rewind();
        this.faceBitmap.copyPixelsFromBuffer(this.rgbBuffer);
        if (this.isEnableLivenessMotion && this.faceDetector != null) {
            for(i = 0; i < this.numberOfFaceDetected; ++i) {
                Type_IODInfo face = this.faceDetected[i];
                this.livenessMotionInfo = new Type_LivenessMotion[1];
                this.livenessMotionInfo[0] = new Type_LivenessMotion();
                int[] livenessMotionRequest = new int[20];
                this.faceDetector.execLivenessMotion(face.objId, this.livenessMotionInfo, livenessMotionRequest);
                if (this.iodLivenessMotionRespListener != null) {
                    this.iodLivenessMotionRespListener.onIODLivenessMotionResponse(face.objId, this.livenessMotionInfo[0], livenessMotionRequest);
                }
            }
        }

        if (!this.isHideFace) {
            this.faceView.draw(this.faceBitmap, this.faceDetected, this.numberOfFaceDetected, this.selectedObjId);
        }

        for (i = 0; i < this.numberOfFaceDetected; ++i) {
            boolean isNeedTrigger = false;
            TriggerType triggerType = TriggerType.NONE;
            Type_IODInfo face = this.faceDetected[i];
            IODTriggerInfo triggerInfo;
            if (face.triggerState == 1) {
                triggerInfo = new IODTriggerInfo();
                triggerInfo.trigger = TriggerType.OCCUR;
                this.iodTriggerTimeArray.put(face.objId, triggerInfo);
                triggerType = TriggerType.OCCUR;
                isNeedTrigger = true;
            } else if (face.triggerState == 2) {
                this.iodTriggerTimeArray.remove(face.objId);
                triggerType = TriggerType.CLEAR;
                isNeedTrigger = true;
            } else {
                triggerInfo = (IODTriggerInfo)this.iodTriggerTimeArray.get(face.objId);
                if (triggerInfo != null) {
                    synchronized(bIsTriggerAgain) {
                        if (triggerInfo.trigger != TriggerType.DETERMINED || bIsTriggerAgain) {
                            int scaleFaceX = Math.round((float)face.x * this.bitmapScaleRatioX);
                            int scaleFaceY = Math.round((float)face.y * this.bitmapScaleRatioY);
                            int scaleFaceW = Math.round((float)face.width * this.bitmapScaleRatioX);
                            int scaleFaceH = Math.round((float)face.height * this.bitmapScaleRatioY);
                            int sizeStatus = this.checkObjectSize(scaleFaceW, scaleFaceH);
                            int locationStatus = this.checkObjectLocation(new Rect(scaleFaceX, scaleFaceY, scaleFaceX + scaleFaceW, scaleFaceY + scaleFaceH));
                            if (sizeStatus != 0) {
                                if (sizeStatus == -1) {
                                    triggerType = TriggerType.TOO_FAR;
                                } else if (sizeStatus == 1) {
                                    triggerType = TriggerType.TOO_CLOSE;
                                }

                                triggerInfo.readyTimestamp = 0L;
                                bIsTriggerAgain = true;
                            } else if (locationStatus != 0) {
                                if (locationStatus == -1) {
                                    triggerType = TriggerType.TOO_LEFT;
                                } else if (locationStatus == 1) {
                                    triggerType = TriggerType.TOO_RIGHT;
                                } else if (locationStatus == -2) {
                                    triggerType = TriggerType.TOO_HIGH;
                                } else if (locationStatus == 2) {
                                    triggerType = TriggerType.TOO_LOW;
                                }

                                triggerInfo.readyTimestamp = 0L;
                                bIsTriggerAgain = true;
                            } else {
                                triggerType = TriggerType.READY;
                                if (triggerInfo.readyTimestamp != 0L) {
                                    long peroid = System.currentTimeMillis() - triggerInfo.readyTimestamp;
                                    if (peroid >= (long)this.delayTimeMS) {
                                        triggerType = TriggerType.DETERMINED;
                                    }
                                } else {
                                    triggerInfo.readyTimestamp = System.currentTimeMillis();
                                }
                            }

                            if (triggerInfo.trigger != triggerType && triggerInfo.trigger != TriggerType.DETERMINED || bIsTriggerAgain) {
                                triggerInfo.trigger = triggerType;
                                isNeedTrigger = true;
                                if (bIsTriggerAgain) {
                                    bIsTriggerAgain = false;
                                }

                                if (triggerType == TriggerType.DETERMINED) {
                                    this.writeIODImageLog(face);
                                    this.iodTriggerTimeArray.remove(face.objId);
                                    triggerInfo.readyTimestamp = 0L;
                                    triggerInfo.trigger = TriggerType.DETERMINED;
                                    this.iodTriggerTimeArray.put(face.objId, triggerInfo);
                                }
                            }
                        }
                    }
                }
            }

            if (isNeedTrigger) {
                synchronized(this.recognizeObjectSet) {
                    this.recognizeObjectSet.setIODObjectIsDetermined(face.objId, triggerType == TriggerType.DETERMINED);
                }

                if (this.iodTrigerRespListener != null) {
                    this.iodTrigerRespListener.onIODTriggerResponse(triggerType, face);
                }
            }
        }

    }

    private int checkObjectSize(int width, int height) {
        int sizeStatus = 0;
        int longEdge = Math.max(width, height);
        if (this.bestMinObjWidth > 0 && longEdge < this.bestMinObjWidth) {
            sizeStatus = -1;
        } else if (this.bestMaxObjWidth > 0 && longEdge > this.bestMaxObjWidth) {
            sizeStatus = 1;
        }

        return sizeStatus;
    }

    private void SetToast(String text, int msec) {
        final Toast toast = Toast.makeText(this.getContext(), text, Toast.LENGTH_SHORT);
        toast.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                toast.cancel();
            }
        }, (long)msec);
    }

    private int checkObjectLocation(Rect obj) {
        int locStatus = 0;
        if (this.bestObjLoc != null && this.bestObjLoc.width() > 0 && this.bestObjLoc.height() > 0) {
            if (obj.left < this.bestObjLoc.left && obj.right < this.bestObjLoc.right) {
                locStatus = -1;
            } else if (obj.left > this.bestObjLoc.left && obj.right > this.bestObjLoc.right) {
                locStatus = 1;
            } else if (obj.top < this.bestObjLoc.top && obj.bottom < this.bestObjLoc.bottom) {
                locStatus = -2;
            } else if (obj.top > this.bestObjLoc.top && obj.bottom > this.bestObjLoc.bottom) {
                locStatus = 2;
            }
        }

        return locStatus;
    }

    private void writeIODImageLog(Type_IODInfo face) {
        if (this.isIODImageLogEnable) {
            Bitmap bitmap = null;
            Rect extRect = this.extendedFaceRect(face.x, face.y, face.width, face.height);
            if (this.faceBitmap != null && extRect.width() > 0 && extRect.height() > 0) {
                bitmap = Bitmap.createBitmap(this.faceBitmap, extRect.left, extRect.top, extRect.width(), extRect.height());
            }

            if (bitmap != null) {
                String dateFmtString = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.US)).format(Calendar.getInstance().getTime());
                String fileName = String.format("%s-%03d-%d-%d.JPG", dateFmtString, face.objId, face.gender, face.glasses);
                File dir = new File(this.logPath);
                File file = new File(dir, fileName);
                if (file.exists()) {
                    file.delete();
                }

                try {
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(CompressFormat.JPEG, 90, out);
                    out.flush();
                    out.close();
                } catch (Exception var9) {
                    var9.printStackTrace();
                    return;
                }
            }
        }

    }

    private void updateObjectSet() {
        boolean isSelectedObjExist = false;
        int maxSizeObjId = 0;
        int maxObjSize = 0;

        for(int i = 0; i < this.numberOfFaceDetected; ++i) {
            Type_IODInfo face = this.faceDetected[i];
            if (face.triggerState != 0 && face.triggerState != 1) {
                if (face.triggerState == 2) {
                    if (this.opMode == FDRControl.Mode.ENROLL) {
                        synchronized(this.enrollObjectSet) {
                            this.enrollObjectSet.removeIODObjectById(face.objId);
                        }
                    } else if (this.opMode == FDRControl.Mode.RECOGNIZE || this.opMode == FDRControl.Mode.IOD) {
                        synchronized(this.recognizeObjectSet) {
                            this.recognizeObjectSet.removeIODObjectById(face.objId);
                        }
                    }
                }
            } else if (this.faceBitmap != null) {
                if (face.objId == this.selectedObjId) {
                    isSelectedObjExist = true;
                }

                int size = face.width * face.height;
                if (size > maxObjSize) {
                    maxObjSize = size;
                    maxSizeObjId = face.objId;
                }

                IODImage iodImage = new IODImage();
                iodImage.qualityScore = face.qualityScore;
                iodImage.poseScore = face.poseScore;
                iodImage.gender = face.gender;
                iodImage.glasses = face.glasses;
                if (face.liveness_result == 0) {
                    iodImage.liveness = Liveness_Type.IOD_LIVENESS_NO_RESULT;
                } else if (face.liveness_result == 1) {
                    iodImage.liveness = Liveness_Type.IOD_LIVENESS_SPOOF;
                } else if (face.liveness_result == 2) {
                    iodImage.liveness = Liveness_Type.IOD_LIVENESS_UNKNOWN;
                } else if (face.liveness_result == 3) {
                    iodImage.liveness = Liveness_Type.IOD_LIVENESS_REAL;
                }

                if (face.occlusionType == 0) {
                    iodImage.occlusionType = Face_Occlusion_Type.GORILLA_FACE_OCCLUSION_UNKNOWN;
                } else if (face.occlusionType == 1) {
                    iodImage.occlusionType = Face_Occlusion_Type.GORILLA_FACE_OCCLUSION_NONE;
                } else if (face.occlusionType == 2) {
                    iodImage.occlusionType = Face_Occlusion_Type.GORILLA_FACE_OCCLUSION_DETECTED;
                }

                if (face.frSuitableType == 0) {
                    iodImage.frSuitableType = FR_Suitable_Type.GORILLA_FR_SUITABLE_UNKNOWN;
                } else if (face.frSuitableType == 1) {
                    iodImage.frSuitableType = FR_Suitable_Type.GORILLA_FR_SUITABLE_OK;
                } else if (face.frSuitableType == 2) {
                    iodImage.frSuitableType = FR_Suitable_Type.GORILLA_FR_NOT_SUITABLE;
                }

                Rect extRect = this.extendedFaceRect(face.x, face.y, face.width, face.height);
                if (extRect.width() > 0 && extRect.height() > 0) {
                    int roiX = face.x - extRect.left;
                    int roiY = face.y - extRect.top;
                    iodImage.roi = new Rect(roiX, roiY, roiX + face.width, roiY + face.height);
                    iodImage.bitmap = null;
                    if (this.opMode == FDRControl.Mode.IOD) {
                        synchronized(this.recognizeObjectSet) {
                            this.recognizeObjectSet.addIODImage(face.objId, iodImage);
                        }
                    } else {
                        iodImage.bitmap = Bitmap.createBitmap(this.faceBitmap, extRect.left, extRect.top, extRect.width(), extRect.height());
                        IODImage iodImageDS = new IODImage(iodImage, maxFaceWidthForFDRService);
                        if (this.opMode == FDRControl.Mode.ENROLL) {
                            synchronized(this.enrollObjectSet) {
                                this.enrollObjectSet.addIODImage(face.objId, iodImageDS);
                            }
                        } else if (this.opMode == FDRControl.Mode.RECOGNIZE) {
                            synchronized(this.recognizeObjectSet) {
                                this.recognizeObjectSet.addIODImage(face.objId, iodImageDS);
                            }
                        }
                    }
                }
            }
        }

        if (!isSelectedObjExist) {
            this.selectedObjId = 0;
        }

        if ((this.selectedObjId == 0 || this.isAutoSelectLargestFace) && maxSizeObjId != 0) {
            this.selectedObjId = maxSizeObjId;
        }

    }

    private Rect extendedFaceRect(int x, int y, int width, int height) {
        Rect extRect = new Rect();
        if (this.faceBitmap != null) {
            int left = x - (int)((double)width * 1.5D);
            int top = y - (int)((double)height * 1.5D);
            int right = x + (int)((double)width * 2.5D);
            int bottom = y + (int)((double)height * 2.5D);
            extRect.left = left >= 0 ? left : 0;
            extRect.top = top >= 0 ? top : 0;
            extRect.right = right <= this.faceBitmap.getWidth() ? right : this.faceBitmap.getWidth();
            extRect.bottom = bottom <= this.faceBitmap.getHeight() ? bottom : this.faceBitmap.getHeight();
        }

        return extRect;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.cameraPreview != null && event.getAction() == 0) {
            Point ptInView = new Point((int)event.getX(), (int)event.getY());
            if (this.cameraPreview.isPointOfParentViewInCameraImage(ptInView)) {
                Point ptInRotatedCamera = this.cameraPreview.pointTransFromParentViewToRotatedCameraImage(ptInView);
                if (this.isTouchFocus) {
                    this.cameraPreview.autoFocus(ptInRotatedCamera, 200);
                }

                if (!this.isAutoSelectLargestFace) {
                    this.selectObject(ptInRotatedCamera);
                }
            }
        }

        return super.onTouchEvent(event);
    }

    private void selectObject(Point pt) {
        for(int i = 0; i < this.numberOfFaceDetected; ++i) {
            Type_IODInfo face = this.faceDetected[i];
            Rect rect = new Rect(face.x, face.y, face.x + face.width, face.y + face.height);
            if (this.isPointInRect(pt, rect)) {
                this.selectedObjId = face.objId;
                break;
            }
        }

    }

    private boolean isPointInRect(Point pt, Rect rect) {
        boolean ret = false;
        if (pt.x >= rect.left && pt.x <= rect.right && pt.y >= rect.top && pt.y <= rect.bottom) {
            ret = true;
        }

        return ret;
    }

    public void setIODParam(int detectionLevel, int scanningLevel, int delayTimeMS) {
        this.detectionLevel = detectionLevel;
        this.scanningLevel = scanningLevel;
        this.delayTimeMS = delayTimeMS;
        if (this.faceDetector != null) {
            this.faceDetector.setParam(this.minObjWidth, this.maxObjWidth, detectionLevel, scanningLevel);
        }

    }

    public void setIODObjWidth(int minObjWidth, int maxObjWidth, int bestMinObjWidth, int bestMaxObjWidth) {
        this.minObjWidth = minObjWidth;
        this.maxObjWidth = maxObjWidth;
        this.bestMinObjWidth = bestMinObjWidth;
        this.bestMaxObjWidth = bestMaxObjWidth;
        if (this.faceDetector != null && minObjWidth > 0 && maxObjWidth > 0) {
            this.faceDetector.setParam(minObjWidth, maxObjWidth, this.detectionLevel, this.scanningLevel);
        }

    }

    public void setIODDetectMode(int detectMode) {
        this.faceDetectMode = detectMode;
        if (this.faceDetector != null) {
            this.faceDetector.setFaceDetectionMode(detectMode);
        }

    }

    public void setIODGenderEnable(boolean enable, int detectionLevel) {
        this.isIODGenderEnable = enable;
        this.genderLevel = detectionLevel;
        if (this.faceDetector != null) {
            IOD_FUNC_ENABLE_STATUS isEnable = this.isIODGenderEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_FEATURE_GENDER, isEnable);
            this.faceDetector.setGenderDetectionLevel(this.genderLevel);
        }

    }

    public void setIODGlassesEnable(boolean enable) {
        this.isIODGlassesEnable = enable;
        if (this.faceDetector != null) {
            IOD_FUNC_ENABLE_STATUS isEnable = this.isIODGlassesEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_FEATURE_GLASSES, isEnable);
        }

    }

    public void setIODLivenessEnable(boolean enable) {
        this.isIODLivenessEnable = enable;
        if (this.faceDetector != null) {
            IOD_FUNC_ENABLE_STATUS isEnable = this.isIODLivenessEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_LIVENESS_DETECT, isEnable);
        }

    }

    public void setIODFRSuitableEnable(boolean enable) {
        this.isIODFRSuitableEnable = enable;
        if (this.faceDetector != null) {
            IOD_FUNC_ENABLE_STATUS isEnable = this.isIODFRSuitableEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_FR_SUITABLE, isEnable);
        }

    }

    public void setIODOcclusionEnable(boolean enable) {
        this.isIODOcclusionEnable = enable;
        if (this.faceDetector != null) {
            IOD_FUNC_ENABLE_STATUS isEnable = this.isIODOcclusionEnable ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_OCCLUSION, isEnable);
        }

    }

    public void setLivenessMotionEnable(boolean enable) {
        this.isEnableLivenessMotion = enable;
        if (this.faceDetector != null) {
            IOD_FUNC_ENABLE_STATUS isEnable = this.isEnableLivenessMotion ? IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_ENABLE : IOD_FUNC_ENABLE_STATUS.IOD_FUNC_SET_DISABLE;
            this.faceDetector.setFunctionEnable(IOD_FUNC_CODE.GORILLA_IOD_LIVENESS_MOTION, isEnable);
        }

    }

    public void setIODImageLogEnable(boolean enable, String logPath) {
        this.isIODImageLogEnable = enable;
        this.logPath = logPath;
        if (this.isIODImageLogEnable) {
            if (this.logPath == null) {
                String root = Environment.getExternalStorageDirectory().toString();
                this.logPath = root + "/Gorilla/FDRControl/IODImageLog";
            }

            File dir = new File(this.logPath);
            dir.mkdirs();
        }

    }

    public boolean switchMode(FDRControl.Mode mode) {
        Log.i("FDRControl", String.format("switchMode:%s", mode));
        if (this.opMode == FDRControl.Mode.ENROLL) {
            this.cancelFaceSelection();
        }

        this.opMode = mode;
        synchronized(this.enrollObjectSet) {
            this.enrollObjectSet.clear();
        }

        synchronized(this.recognizeObjectSet) {
            this.recognizeObjectSet.clear();
            return true;
        }
    }

    public boolean releaseCamera(int cameraId) {
        Log.i("FDRControl", String.format("releaseCamera:%d", cameraId));
        if (this.cameraPreview != null) {
            this.cameraPreview.releaseCamera();
            return true;
        } else {
            return false;
        }
    }

    public boolean reInitCamera(int cameraId) {
        Log.i("FDRControl", String.format("reInitCamera:%d", cameraId));
        if (this.cameraPreview != null) {
            this.cameraPreview.setCameraId(cameraId);
            return this.cameraPreview.reInitCamera();
        } else {
            return false;
        }
    }

    public int getNumberOfCameras() {
        return this.cameraPreview != null ? this.cameraPreview.getNumberOfCameras() : 0;
    }

    public boolean switchCamera(int cameraId) {
        Log.i("FDRControl", String.format("switchCamera:%d", cameraId));
        return this.cameraPreview != null ? this.cameraPreview.switchCamera(cameraId) : false;
    }

    public void setIODTriggerResponseListener(IODTriggerResponseListener listener) {
        this.iodTrigerRespListener = listener;
    }

    public void setFDRServiceResponseListener(ResponseListener listener) {
        this.fdrClient.setResponseListener(listener);
    }

    public void setFDRControlResponseListener(FDRControlResponseListener listener) {
        this.fdrCtrlRespListener = listener;
    }

    public void setRTSPStatusListener(RTSPStatusListener listener) {
        this.rtspStatusListener = listener;
    }

    public void setIODLivenessMotionResponseListener(IODLivenessMotionResponseListener listener) {
        this.iodLivenessMotionRespListener = listener;
    }

    public boolean isTargetSelected() {
        return this.selectedObjId != 0;
    }

    public void connectRTSP(String url, String username, String password) {
        if (this.rtspClient != null) {
            Log.i("FDRControl", "[connectRTSP]");
            this.rtspClient.open(url, username, password);
        }

    }

    public void disconnectRTSP() {
        if (this.rtspClient != null) {
            this.rtspClient.stop();
            this.rtspClient.close();
        }

    }

    public void playRTSP() {
        if (this.rtspClient != null) {
            Log.i("FDRControl", "[playRTSP]");
            this.rtspClient.play();
        }

    }

    public void loginFDRService(String ip, int port, String username, String password) {
        this.fdrClient.login(String.format("%s:%d", ip, port), username, password);
    }

    public void logoutFDRService() {
        if (this.fdrClient.isLogin()) {
            this.fdrClient.logout();
        }

    }

    public boolean isFDRServiceLogin() {
        return this.fdrClient.isLogin();
    }

    public void setTargetInfo(String targetId, String targetInfo) {
        this.fdrClient.setTargetInfo(targetId, targetInfo);
    }

    public void getTargetInfo(String targetId) {
        this.fdrClient.getTargetInfo(targetId);
    }

    public void deleteTarget(String targetId, String userId) {
        this.fdrClient.deleteTarget(targetId, userId);
    }

    public boolean startFaceSelection() {
        boolean ret = false;
        if (this.opMode == FDRControl.Mode.ENROLL && this.faceSource != FDRControl.FaceSource.EVENT && this.selectedObjId != 0) {
            IODObject object = null;
            synchronized(this.enrollObjectSet) {
                object = this.enrollObjectSet.getIODObjectById(this.selectedObjId);
            }

            if (object != null) {
                this.faceSelectionGrid.clearObjectImageList();
                this.faceSelectionGrid.addObjectImageList(object.getObjectImageList(), maxFaceWidthForFDRService);
                this.faceSelectionGrid.setVisibility(View.VISIBLE);
                this.stopFaceDetection();
                ret = true;
            }
        }

        return ret;
    }

    public void cancelFaceSelection() {
        if (this.faceSource != FDRControl.FaceSource.EVENT) {
            this.faceSelectionGrid.clearObjectImageList();
            this.faceSelectionGrid.setVisibility(View.GONE);
            this.startFaceDetection();
        }

    }

    public boolean searchEventFace(Date lastTime, int maxNum) {
        boolean ret = false;
        if (this.faceSource == FDRControl.FaceSource.EVENT) {
            this.faceSelectionGrid.clearObjectImageList();
            this.fdrClient.searchFaceEvents(lastTime, maxNum);
        }

        return ret;
    }

    public void clearEventFace() {
        if (this.faceSource == FDRControl.FaceSource.EVENT) {
            this.faceSelectionGrid.clearObjectImageList();
        }

    }

    public Bitmap getBestSelectedFaceBitmap() {
        return this.faceSelectionGrid.getBestSelectedFaceBitmap();
    }

    public boolean enrollTarget(String targetId) {
        boolean ret = false;
        int bufferLength = this.faceSelectionGrid.getSelectedObjectImagesBufferLength();
        if (bufferLength > 0) {
            ByteBuffer imageBuf = ByteBuffer.allocate(bufferLength);
            int imageNum = this.faceSelectionGrid.getSelectedObjectImagesBuffer(imageBuf);
            imageBuf.rewind();
            this.fdrClient.enrollTarget(targetId, imageNum, imageBuf);
            ret = true;
        }

        this.cancelFaceSelection();
        this.clearEventFace();
        return ret;
    }

    public int getSelectedFaceNumber() {
        return this.faceSelectionGrid.getSelectedFaceNumber();
    }

    public int getEnrolledFaceList(List<byte[]> pngList, List<String> iniList) {
        int ret = this.faceSelectionGrid.getSelectedObjectPngList(pngList, iniList);
        this.cancelFaceSelection();
        this.clearEventFace();
        Log.i("FDRControl", String.format("getEnrolledFaceList - ret:%d", ret));
        return ret;
    }

    public void finalizeTemplates() {
        this.fdrClient.finalizeTemplates();
    }

    public boolean getTargetIdbyCustomId(String customId) {
        boolean ret = false;
        if (customId != null) {
            this.fdrClient.getTargetIdbyCustomId(customId);
            ret = true;
        }

        return ret;
    }

    public void startIdentify(String sourceId, int regLevel) {
        if (this.opMode == FDRControl.Mode.RECOGNIZE) {
            this.isStartIdentify = true;
            synchronized(this.recognizeObjectSet) {
                this.recognizeObjectSet.clear();
            }

            this.sourceId = sourceId;
            this.regLevel = regLevel;
            synchronized(this.identifyFlag) {
                if (this.identifyFlag == 0) {
                    this.postDelayed(this.doIdentifyTarget, 5000L);
                }
            }
        }

    }

    public void stopIdentify() {
        if (this.opMode == FDRControl.Mode.RECOGNIZE) {
            this.isStartIdentify = false;
            synchronized(this.recognizeObjectSet) {
                this.recognizeObjectSet.clear();
            }
        }

    }

    public boolean identifyTarget(String sourceId, int regLevel) {
        boolean ret = false;
        if (this.selectedObjId != 0) {
            ret = this.identifyTargetByObjId(sourceId, regLevel, true, this.selectedObjId);
        } else {
            Log.e("FDRControl", "No selected object for verify.");
        }

        return ret;
    }

    private void identifyAllTargets(String sourceId, int regLevel, boolean isSingleMode) {
        synchronized(this.identifyFlag) {
            for(int i = this.recognizeObjectSet.getSize() - 1; i >= 0; --i) {
                IODObject object = this.recognizeObjectSet.getIODObject(i);
                if (object != null) {
                    int objId = object.getId();
                    this.identifyTargetByObjId(sourceId, regLevel, isSingleMode, objId);
                }
            }

        }
    }

    private boolean identifyTargetByObjId(String sourceId, int regLevel, boolean isSingleMode, int objId) {
        boolean ret = false;
        synchronized(this.recognizeObjectSet) {
            IODObject object = this.recognizeObjectSet.getIODObjectById(objId);
            if (object != null) {
                int imageNum = 0;
                int bufferLength = object.getObjectImagesBufferLength();
                if (bufferLength > 0) {
                    ByteBuffer imageBuf = ByteBuffer.allocate(bufferLength);
                    imageBuf.rewind();
                    imageNum = object.getObjectImagesBuffer(imageBuf);
                    int frId = isSingleMode ? 0 : object.getId() + this.randomId;
                    this.fdrClient.identifyTarget(sourceId, frId, regLevel, imageNum, imageBuf);
                    ret = true;
                    Integer var12 = this.identifyFlag;
                    Integer var13 = this.identifyFlag = this.identifyFlag + 1;
                }

                Log.i("FDRControl", String.format("Identify object %d, image num: %d", object.getId(), imageNum));
                this.recognizeObjectSet.removeIODObjectById(objId);
                if (objId == this.selectedObjId) {
                    this.selectedObjId = 0;
                }
            }

            return ret;
        }
    }

    public boolean verifyTarget(String sourceId, String targetId, int regLevel) {
        boolean ret = false;
        if (this.selectedObjId != 0) {
            synchronized(this.recognizeObjectSet) {
                IODObject object = this.recognizeObjectSet.getIODObjectById(this.selectedObjId);
                if (object != null) {
                    int imageNum = 0;
                    int bufferLength = object.getObjectImagesBufferLength();
                    if (bufferLength > 0) {
                        ByteBuffer imageBuf = ByteBuffer.allocate(bufferLength);
                        imageBuf.rewind();
                        imageNum = object.getObjectImagesBuffer(imageBuf);
                        this.fdrClient.verifyTarget(sourceId, targetId, regLevel, imageNum, imageBuf);
                        ret = true;
                    }

                    Log.i("FDRControl", String.format("Identify object %d, image num: %d", object.getId(), imageNum));
                    this.recognizeObjectSet.removeIODObjectById(this.selectedObjId);
                }

                this.selectedObjId = 0;
            }
        } else {
            Log.e("FDRControl", "No selected object for verify.");
        }

        return ret;
    }

    public int getRecognizedFaceList(List<byte[]> pngList, List<String> iniList, FDRControl.IODfeature iodfeature) {
        int ret = 0;
        synchronized(bIsTriggerAgain) {
            bIsTriggerAgain = true;
            if (this.selectedObjId != 0) {
                synchronized(this.recognizeObjectSet) {
                    IODObject object = this.recognizeObjectSet.getIODObjectById(this.selectedObjId);
                    if (object != null) {
                        iodfeature.liveness = object.liveness;
                        iodfeature.frSuitableType = object.frSuitableType;
                        iodfeature.occlusionType = object.occlusionType;
                        ret = object.getObjectPngList(pngList, iniList);
                        this.recognizeObjectSet.removeIODObjectById(this.selectedObjId);
                    }

                    this.selectedObjId = 0;
                }
            }

            Log.i("FDRControl", String.format("getRecognizedFaceList - ret:%d", ret));
            return ret;
        }
    }

    public AdCategory getCurrentAdSuggestion() {
        AdCategory ad = AdCategory.NONE;
        Statistics stat = this.recognizeObjectSet.getStatistics();
        if (stat.determinedNum != 0) {
            if (stat.maleNum > stat.femaleNum) {
                ad = AdCategory.MALE;
            } else if (stat.femaleNum > stat.maleNum) {
                ad = AdCategory.FEMALE;
            } else {
                ad = AdCategory.GENERAL;
            }
        }

        return ad;
    }

    public void autoSelectLargestFace(boolean enable) {
        this.isAutoSelectLargestFace = enable;
    }

    public void startFaceDetection() {
        this.isDoIOD = true;
        this.faceView.setVisibility(View.VISIBLE);
    }

    public void stopFaceDetection() {
        this.waitingPreviewData.data = null;
        this.previewData.data = null;
        this.isDoIOD = false;
        this.faceView.setVisibility(View.GONE);
    }

    public int getMinExposure() {
        return this.cameraPreview != null ? this.cameraPreview.getMinExposure() : 0;
    }

    public float getExposureStep() {
        return this.cameraPreview != null ? this.cameraPreview.getExposureStep() : 0.0F;
    }

    public int getMaxExposure() {
        return this.cameraPreview != null ? this.cameraPreview.getMaxExposure() : 0;
    }

    public int getExposure() {
        return this.cameraPreview != null ? this.cameraPreview.getExposure() : 0;
    }

    public boolean setExposure(int exposure) {
        return this.cameraPreview != null ? this.cameraPreview.setExposure(exposure) : false;
    }

    public boolean lockExposure(boolean isLock) {
        return this.cameraPreview != null ? this.cameraPreview.lockExposure(isLock) : false;
    }

    public void setCameraVisibility(int cameraVisibility) {
        if (this.cameraPreview != null) {
            this.cameraPreview.setVisibility(cameraVisibility);
        }

    }

    public void setFaceVisibility(int faceVisibility) {
        if (this.faceView != null) {
            this.faceView.setVisibility(faceVisibility);
        }

    }

    public void startCamera() {
        this.isDoIOD = true;
        this.firstCameraStart = true;
        if (this.cameraPreview != null) {
            this.cameraPreview.startCamera();
        }

        this.faceView.setIsBlindMode(false);
    }

    public void stopCamera() {
        this.firstCameraStart = false;
        this.isDoIOD = false;
        this.waitingPreviewData.data = null;
        this.previewData.data = null;
        if (this.cameraPreview != null) {
            this.cameraPreview.stopCamera();
        }

        this.faceView.setIsBlindMode(true);
    }

    public void release() {
        Log.d("FDRControl", String.format("fdrCtrl release"));
        this.stopIdentify();
        this.removeView(this.cameraPreview);

        try {
            this.exit = true;
            this.vaThread.join();
            this.setObjThread.join();
        } catch (InterruptedException var2) {
            Log.e("FDRControl", var2.toString());
        }

        if (this.cameraPreview != null) {
            this.cameraPreview.release();
            this.cameraPreview = null;
            this.firstCameraStart = false;
        }

        if (this.rtspClient != null) {
            this.rtspClient.release();
            this.rtspClient = null;
        }

        if (this.faceDetector != null) {
            Log.i("FDRControl", "free IOD");
            this.faceDetector.free();
            this.faceDetector = null;
        }

        this.enrollObjectSet.clear();
        this.recognizeObjectSet.clear();
        this.selectedObjId = 0;
    }

    public ImageRawData getRtspPreview() {
        return MyFDRControl.this.rtspPreview;
    }

    static {
        captureMode = FDRControl.CaptureMode.ByQuality;
        bIsTriggerAgain = false;
    }

    public static enum AdCategory {
        NONE,
        GENERAL,
        MALE,
        FEMALE;

        private AdCategory() {
        }
    }

    private class UpdateFaceViewRunnable implements Runnable {
        ImageRawData rawImg = null;

        public UpdateFaceViewRunnable(ImageRawData rawImg) {
            this.rawImg = rawImg;
        }

        public void run() {
            if (this.rawImg.data != null) {
                MyFDRControl.this.updateFaceView(this.rawImg);
            }

            synchronized(this) {
                this.notify();
            }
        }
    }

    private class VARunnable implements Runnable {
        private VARunnable() {
        }

        public void run() {
            Log.i("FDRControl", "VARunnable start");

            while(true) {
                while(!MyFDRControl.this.exit) {
                    if (!MyFDRControl.this.isDoIOD) {
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException var8) {
                            var8.printStackTrace();
                        }
                    } else {
                        boolean isImageUpdated = false;
                        synchronized(MyFDRControl.this.waitingPreviewData) {
                            if (MyFDRControl.this.waitingPreviewData.data != null && MyFDRControl.this.isDoIOD && (MyFDRControl.this.previewData.data == null || !Arrays.equals(MyFDRControl.this.waitingPreviewData.data, MyFDRControl.this.previewData.data))) {
                                MyFDRControl.this.previewData = MyFDRControl.this.waitingPreviewData.clone();
                                isImageUpdated = true;
                            }
                        }

                        if (isImageUpdated) {
                            long time0 = System.currentTimeMillis();
                            MyFDRControl.this.doFaceDetect(MyFDRControl.this.previewData);
                            UpdateFaceViewRunnable faceViewRunable = MyFDRControl.this.new UpdateFaceViewRunnable(MyFDRControl.this.previewData);
                            synchronized(faceViewRunable) {
                                ((Activity) MyFDRControl.this.getContext()).runOnUiThread(faceViewRunable);

                                try {
                                    faceViewRunable.wait(100L);
                                } catch (Exception var10) {
                                    Log.e("FDRControl", var10.getMessage());
                                }
                            }

                            long time2 = System.currentTimeMillis();
                            MyFDRControl.this.updateObjectSet();
                        } else {
                            try {
                                Thread.sleep(10L);
                            } catch (InterruptedException var9) {
                                var9.printStackTrace();
                            }
                        }
                    }
                }

                Log.i("FDRControl", "VARunnable end");
                return;
            }
        }
    }

    private class OnRTSPStatusRunnable implements Runnable {
        int status = 0;
        int error = 0;

        public OnRTSPStatusRunnable(int status, int error) {
            this.status = status;
            this.error = error;
        }

        public void run() {
            if (MyFDRControl.this.rtspStatusListener != null) {
                if (this.error != 0) {
                    MyFDRControl.this.rtspStatusListener.onError(this.error);
                } else if (this.status == 0) {
                    MyFDRControl.this.rtspStatusListener.onClosed();
                } else if (this.status == 1) {
                    MyFDRControl.this.rtspStatusListener.onStoped();
                } else if (this.status == 2) {
                    MyFDRControl.this.rtspStatusListener.onConnecting();
                } else if (this.status == 3) {
                    MyFDRControl.this.rtspStatusListener.onPlaying();
                } else if (this.status == 4) {
                    MyFDRControl.this.rtspStatusListener.onWaitRetry();
                }
            }

        }
    }

    public interface cameraPreviewCallback {
        void onCameraPreview();

        void onCameraStarted();
    }

    private class SetBestObjRunnable implements Runnable {
        float bestLeft;
        float bestTop;
        float bestRight;
        float bestBottom;
        double cameraScaleX;
        double cameraScaleY;
        int waitCounter = 0;

        public SetBestObjRunnable(float left, float top, float right, float bottom) {
            this.bestLeft = left;
            this.bestTop = top;
            this.bestRight = right;
            this.bestBottom = bottom;
        }

        public void run() {
            if (MyFDRControl.this.cameraPreview != null) {
                while(true) {
                    if (!MyFDRControl.this.cameraPreview.IsScaled()) {
                        try {
                            Thread.sleep(100L);
                            ++this.waitCounter;
                        } catch (InterruptedException var4) {
                            var4.printStackTrace();
                        }

                        if (this.waitCounter <= 5) {
                            continue;
                        }
                    }

                    this.cameraScaleX = MyFDRControl.this.cameraPreview.getRatioX();
                    this.cameraScaleY = MyFDRControl.this.cameraPreview.getRatioY();
                    MyFDRControl.this.setBestObj(this.bestLeft, this.bestTop, this.bestRight, this.bestBottom, this.cameraScaleX, this.cameraScaleY);
                    break;
                }
            }

            synchronized(this) {
                this.notify();
            }
        }
    }

    public interface IODLivenessMotionResponseListener {
        void onIODLivenessMotionResponse(int var1, Type_LivenessMotion var2, int[] var3);
    }

    public interface RTSPStatusListener {
        void onClosed();

        void onStoped();

        void onConnecting();

        void onPlaying();

        void onWaitRetry();

        void onError(int var1);
    }

    public interface FDRControlResponseListener {
        void onSearchFaceEventsResponse(int var1, int var2, Date var3);
    }

    public interface IODTriggerResponseListener {
        void onIODTriggerResponse(TriggerType var1, Type_IODInfo var2);
    }

    public static enum TriggerType {
        NONE,
        OCCUR,
        TOO_FAR,
        TOO_CLOSE,
        TOO_LEFT,
        TOO_RIGHT,
        TOO_HIGH,
        TOO_LOW,
        READY,
        DETERMINED,
        CLEAR;

        private TriggerType() {
        }
    }

    public class ImageRawData {
        public byte[] data;
        public int width;
        public int height;
        public int degree;
        public IOD_SRC_IMG_FORMAT format;

        private ImageRawData() {
            this.data = null;
            this.width = 0;
            this.height = 0;
            this.degree = 0;
            this.format = IOD_SRC_IMG_FORMAT.IOD_SRC_IMG_FORMAT_YUV;
        }

        public ImageRawData clone() {
            ImageRawData clone = MyFDRControl.this.new ImageRawData();
            if (this.data != null) {
                clone.data = (byte[])this.data.clone();
                clone.width = this.width;
                clone.height = this.height;
                clone.degree = this.degree;
                clone.format = this.format;
            }

            return clone;
        }
    }

    private class IODTriggerInfo {
        public TriggerType trigger;
        public long readyTimestamp;

        private IODTriggerInfo() {
            this.trigger = TriggerType.NONE;
            this.readyTimestamp = 0L;
        }
    }

    class NamelessClass_7 implements PreviewCallback {
        NamelessClass_7() {
        }

        public void onPreviewFrame(byte[] data, Camera camera) {
            if (MyFDRControl.this.camPreviewCallback != null) {
                MyFDRControl.this.camPreviewCallback.onCameraPreview();
                if (MyFDRControl.this.firstCameraStart) {
                    MyFDRControl.this.camPreviewCallback.onCameraStarted();
                    MyFDRControl.this.firstCameraStart = false;
                }
            }

            Parameters parameters = camera.getParameters();
            Size cameraSize = parameters.getPreviewSize();
            int degree = MyFDRControl.this.cameraPreview.getDisplayOrientation();
            synchronized(MyFDRControl.this.waitingPreviewData) {
                if (MyFDRControl.this.isDoIOD) {
                    MyFDRControl.this.waitingPreviewData.data = data;
                    MyFDRControl.this.waitingPreviewData.width = cameraSize.width;
                    MyFDRControl.this.waitingPreviewData.height = cameraSize.height;
                    MyFDRControl.this.waitingPreviewData.degree = degree;
                    MyFDRControl.this.waitingPreviewData.format = IOD_SRC_IMG_FORMAT.IOD_SRC_IMG_FORMAT_YUV;
                }

            }
        }
    }

    class NamelessClass_6 implements StatusCallback {
        NamelessClass_6() {
        }

        public void onStatus(int status, int error) {
            ((Activity) MyFDRControl.this.getContext()).runOnUiThread(MyFDRControl.this.new OnRTSPStatusRunnable(status, error));
        }
    }

    class NamelessClass_5 implements com.gorillatechnology.videosource.RTSPClient.PreviewCallback {
        NamelessClass_5() {
        }

        public void onPreviewFrame(byte[] data, int width, int height, long timestamp) {
            synchronized(MyFDRControl.this.waitingPreviewData) {
                if (MyFDRControl.this.isDoIOD) {
                    MyFDRControl.this.waitingPreviewData.data = data;
                    MyFDRControl.this.waitingPreviewData.width = width;
                    MyFDRControl.this.waitingPreviewData.height = height;
                    MyFDRControl.this.waitingPreviewData.degree = 0;
                    MyFDRControl.this.waitingPreviewData.format = IOD_SRC_IMG_FORMAT.IOD_SRC_IMG_FORMAT_RGBA;
                } else {
                    MyFDRControl.this.rtspPreview.data = data;
                    MyFDRControl.this.rtspPreview.width = width;
                    MyFDRControl.this.rtspPreview.height = height;
                    MyFDRControl.this.rtspPreview.degree = 0;
                    MyFDRControl.this.rtspPreview.format = IOD_SRC_IMG_FORMAT.IOD_SRC_IMG_FORMAT_RGBA;
                    UpdateFaceViewRunnable faceViewRunable = MyFDRControl.this.new UpdateFaceViewRunnable(MyFDRControl.this.rtspPreview);
                    synchronized(faceViewRunable) {
                        ((Activity) MyFDRControl.this.getContext()).runOnUiThread(faceViewRunable);

                        try {
                            faceViewRunable.wait(100L);
                        } catch (Exception var12) {
                            Log.e("FDRControl", var12.getMessage());
                        }
                    }
                }

            }
        }

        public void onPreviewBitmap(Bitmap bitmap, long timestamp) {
        }
    }

    class NamelessClass_4 implements AutoFocusCallback {
        NamelessClass_4() {
        }

        public void onAutoFocus(boolean success, Camera camera) {
            Log.i("FDRControl", String.format("focus success: %b.", success));
        }
    }

    class NamelessClass_3 implements FaceEventResponseListener {
        NamelessClass_3() {
        }

        public void onSearchFaceEventsResponse(int respCode, ArrayList<FaceEvent> eventList) {
            int faceNum = 0;
            Date firstTime = new Date();
            if (respCode == 0 && eventList != null) {
                faceNum = eventList.size();

                for(int i = 0; i < faceNum; ++i) {
                    FaceEvent event = (FaceEvent)eventList.get(i);
                    MyFDRControl.this.fdrClient.getFaceEventImage(event.mId);
                    if (event.mTime.compareTo(firstTime) < 0) {
                        firstTime = event.mTime;
                    }
                }
            }

            if (MyFDRControl.this.fdrCtrlRespListener != null) {
                MyFDRControl.this.fdrCtrlRespListener.onSearchFaceEventsResponse(respCode, faceNum, firstTime);
            }

        }

        public void onGetFaceEventImageResponse(int respCode, Bitmap bitmap) {
            if (respCode == 0 && bitmap != null) {
                IODImage image = new IODImage();
                image.bitmap = bitmap;
                image.roi.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                MyFDRControl.this.faceSelectionGrid.addObjectImage(image, 0);
            }

        }
    }

    class NamelessClass_2 implements Runnable {
        NamelessClass_2() {
        }

        public void run() {
            MyFDRControl.this.identifyAllTargets(MyFDRControl.this.sourceId, MyFDRControl.this.regLevel, false);
            synchronized(MyFDRControl.this.identifyFlag) {
                if (MyFDRControl.this.identifyFlag == 0) {
                    MyFDRControl.this.postDelayed(MyFDRControl.this.doIdentifyTarget, 5000L);
                }

            }
        }
    }

    class NamelessClass_1 implements FRResponseListener {
        NamelessClass_1() {
        }

        public void onIdentifyTargetResponse(int respCode, FRResult frResult) {
            synchronized(MyFDRControl.this.identifyFlag) {
                Integer var4 = MyFDRControl.this.identifyFlag;
                Integer var5 = MyFDRControl.this.identifyFlag = MyFDRControl.this.identifyFlag - 1;
                if (MyFDRControl.this.isStartIdentify && MyFDRControl.this.identifyFlag == 0) {
                    MyFDRControl.this.postDelayed(MyFDRControl.this.doIdentifyTarget, 5000L);
                }
            }

            if (frResult != null) {
                Log.i("FDRControl", String.format("onIdentifyTargetResponse - obj:%d, target:%s, trigger:%s", frResult.objectId, frResult.targetId, frResult.trigger));
            }

        }

        public void onVerifyTargetResponse(int respCode, FRResult frResult) {
        }
    }
}
