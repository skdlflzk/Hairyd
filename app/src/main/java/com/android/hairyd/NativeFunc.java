package com.android.hairyd;


public class NativeFunc {
    static {
        System.loadLibrary("NativeModule");
    }

    public native String getStringFromNative();

    public native void FindFeatures(long addrGray, long addrRgba,int seek );

    public native void getDisparity(long left, long right,int seek );

    public native void drawPoint(long addrRgba, float x,float y);

    public native double[] AAMfitting(long addrGray, long addrRgba,double[] datum, double[] average );

    public native long getGradient(long addrGray);

    public native boolean IsIntersect(double ax, double ay,double bx,double by,double cx,double cy,double x,double y) ;

}
