package com.maganimata.app;

public final class Config {
    private Config() {}

    public static final String API_BASE_URL = "https://gyaranaure.com.ng/magani/api/";
    public static final String WEB_BASE_URL = "https://gyaranaure.com.ng/magani/";

    // EDIT THESE VALUES DIRECTLY IN CODE, then rebuild the APK.
    // App ID uses ~ ; each Ad Unit ID uses / . Never reuse one unit ID across formats.
    public static final String ADMOB_APP_ID = "ca-app-pub-7286881844270614~1304241671";
    public static final String ADMOB_BANNER_ID = "ca-app-pub-7286881844270614/2840684624";
    // Paste the separate Native Ad Unit ID from AdMob. Leave empty until you have one.
    public static final String ADMOB_NATIVE_ID = "ca-app-pub-7286881844270614/1115024047";
    public static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-7286881844270614/1192561035";
    // Paste separate Rewarded / Rewarded Interstitial / App Open IDs if you use those formats.
    public static final String ADMOB_REWARDED_ID = "ca-app-pub-7286881844270614/1780315018";
    public static final String ADMOB_REWARDED_INTERSTITIAL_ID = "ca-app-pub-7286881844270614/1898814640";
    public static final String ADMOB_APP_OPEN_ID = "ca-app-pub-7286881844270614/5848475978";
}
