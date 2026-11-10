package com.maganimata.app;

import android.app.Activity;
import com.google.android.ump.*;

public class ConsentManager {
    public interface Ready { void run(boolean canRequestAds); }
    public static void request(Activity activity, Ready ready){
        ConsentInformation info=UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params=new ConsentRequestParameters.Builder().build();
        info.requestConsentInfoUpdate(activity,params,
            ()->UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity,error->ready.run(info.canRequestAds())),
            error->ready.run(info.canRequestAds())
        );
    }
}
