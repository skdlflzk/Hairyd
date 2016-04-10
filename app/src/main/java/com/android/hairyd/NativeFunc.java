package com.android.hairyd;


public class NativeFunc {
    static {
        System.loadLibrary("NativeModule");
    }

    public native String getStringFromNative();

    public native void FindFeatures(long addrGray, long addrRgba);
}
