package com.android.hairyd;


public class NativeFunc {
    static {
        System.loadLibrary("NativeModule");
    }

    public native String getStringFromNative();

    public native void FindFeatures(long addrGray, long addrRgba,int seek );

    public native void getDisparity(long left, long right,int seek );

    public native void drawPoint(long addrRgba, float x,float y);

}
