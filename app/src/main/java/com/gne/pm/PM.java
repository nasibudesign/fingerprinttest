package com.gne.pm;


public class PM {
    static {

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            System.loadLibrary("fingerPm_7");
//        } else {
//            System.loadLibrary("fingerPm_5");
//        }
        System.loadLibrary("fingerPm");
    }

    static public native int powerOn();

    static public native int powerOff();


}









































