package com.maganimata.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;

import org.json.JSONObject;

public class AdController {
    private final Activity activity;
    private final SharedPreferences prefs;

    private boolean master, admobEnabled, metaEnabled, bannerEnabled, nativeEnabled;
    private boolean interstitialEnabled, rewardedEnabled, rewardedInterstitialEnabled, appOpenEnabled;

    private boolean homeBannerEnabled, homeNativeEnabled;
    private boolean articleBannerEnabled, articleNativeEnabled;

    private String admobBanner, admobNative, admobInterstitial, admobRewarded, admobRewardedInterstitial, admobAppOpen;
    private String metaBanner, metaInterstitial;

    private int articleBannerFrequency = 3;
    private int articleNativeFrequency = 5;
    private int articleBannerMax = 1;
    private int articleNativeMax = 3;
    private int homeBannerMax = 1;
    private int homeNativeMax = 3;
    private int interstitialFrequency = 5;
    private long interstitialCooldownMs = 10 * 60 * 1000L;

    private int opened = 0;
    private long lastInterstitialMs = 0L;
    private InterstitialAd interstitial;
    private RewardedAd rewarded;
    private RewardedInterstitialAd rewardedInterstitial;
    private boolean appOpenShowing = false;

    public AdController(Activity activity) {
        this.activity = activity;
        prefs = activity.getSharedPreferences("lafiyar_mace_ads", Activity.MODE_PRIVATE);
        lastInterstitialMs = prefs.getLong("last_interstitial_ms", 0L);
    }

    public void apply(JSONObject s) {
        master = "1".equals(s.optString("ads_enabled", "1"));
        admobEnabled = "1".equals(s.optString("admob_enabled", "1"));
        metaEnabled = "1".equals(s.optString("meta_enabled", "1"));
        bannerEnabled = "1".equals(s.optString("banner_enabled", "1"));
        nativeEnabled = "1".equals(s.optString("native_enabled", "1"));
        interstitialEnabled = "1".equals(s.optString("interstitial_enabled", "1"));
        rewardedEnabled = "1".equals(s.optString("rewarded_enabled", "1"));
        rewardedInterstitialEnabled = "1".equals(s.optString("rewarded_interstitial_enabled", "0"));
        appOpenEnabled = "1".equals(s.optString("app_open_enabled", "1"));

        homeBannerEnabled = "1".equals(s.optString("home_banner_enabled", "1"));
        homeNativeEnabled = "1".equals(s.optString("home_native_enabled", "1"));
        articleBannerEnabled = "1".equals(s.optString("article_banner_enabled", "1"));
        articleNativeEnabled = "1".equals(s.optString("article_native_enabled", "1"));

        // Ad Unit IDs intentionally come ONLY from Config.java. Server settings must not
        // overwrite code IDs, so you can change IDs in Android Studio and rebuild the APK.
        admobBanner = Config.ADMOB_BANNER_ID == null ? "" : Config.ADMOB_BANNER_ID.trim();
        admobNative = Config.ADMOB_NATIVE_ID == null ? "" : Config.ADMOB_NATIVE_ID.trim();
        admobInterstitial = Config.ADMOB_INTERSTITIAL_ID == null ? "" : Config.ADMOB_INTERSTITIAL_ID.trim();
        admobRewarded = Config.ADMOB_REWARDED_ID == null ? "" : Config.ADMOB_REWARDED_ID.trim();
        admobRewardedInterstitial = Config.ADMOB_REWARDED_INTERSTITIAL_ID == null ? "" : Config.ADMOB_REWARDED_INTERSTITIAL_ID.trim();
        admobAppOpen = Config.ADMOB_APP_OPEN_ID == null ? "" : Config.ADMOB_APP_OPEN_ID.trim();
        metaBanner = s.optString("meta_banner_placement_id", "");
        metaInterstitial = s.optString("meta_interstitial_placement_id", "");

        articleBannerFrequency = positiveInt(s, "article_banner_frequency", 3, 1, 20);
        articleNativeFrequency = positiveInt(s, "article_native_frequency", 5, 1, 20);
        articleBannerMax = positiveInt(s, "article_banner_max", 1, 0, 5);
        articleNativeMax = positiveInt(s, "article_native_max", 3, 0, 3); if (articleNativeEnabled && articleNativeMax > 0) articleNativeMax = 3;
        homeBannerMax = positiveInt(s, "home_banner_max", 1, 0, 2);
        homeNativeMax = positiveInt(s, "home_native_max", 3, 0, 3); if (homeNativeEnabled && homeNativeMax > 0) homeNativeMax = 3;
        interstitialFrequency = positiveInt(s, "interstitial_frequency", 5, 2, 50);
        int cooldownMin = positiveInt(s, "interstitial_cooldown_minutes", 10, 1, 120);
        interstitialCooldownMs = cooldownMin * 60_000L;

        if (master && admobEnabled) {
            loadInterstitial();
            if (rewardedEnabled) loadRewarded();
            if (rewardedInterstitialEnabled) loadRewardedInterstitial();
        }
        if (master && metaEnabled) {
            try { AudienceNetworkAds.initialize(activity); } catch (Exception ignored) {}
        }
    }

    private int positiveInt(JSONObject s, String key, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(s.optString(key, String.valueOf(fallback))))); }
        catch (Exception ignored) { return fallback; }
    }

    private String value(JSONObject s, String key, String fallback) {
        String v = s.optString(key, "").trim();
        return v.isEmpty() ? fallback : v;
    }

    // Generic page banner: one call = one banner. Callers decide the page placement.
    public void showBanner(LinearLayout parent) {
        showBannerInternal(parent);
    }

    public void showHomeBanner(LinearLayout parent) {
        if (homeBannerMax <= 0 || !homeBannerEnabled) return;
        showBannerInternal(parent);
    }

    public void showHomeNative(LinearLayout parent) {
        if (!master || !homeNativeEnabled || homeNativeMax <= 0) return;
        showNativeWithBannerFallback(parent);
    }

    public void showArticleBanner(LinearLayout parent) {
        if (!articleBannerEnabled || articleBannerMax <= 0) return;
        showBannerInternal(parent);
    }

    public void showArticleNative(LinearLayout parent) {
        if (!master || !nativeEnabled || !articleNativeEnabled || articleNativeMax <= 0) return;
        showNativeWithBannerFallback(parent);
    }

    public int getArticleBannerFrequency() { return articleBannerFrequency; }
    public int getArticleNativeFrequency() { return articleNativeFrequency; }
    public int getArticleAdFrequency() {
        if (isArticleBannerEnabled() && isArticleNativeEnabled()) return Math.min(articleBannerFrequency, articleNativeFrequency);
        if (isArticleBannerEnabled()) return articleBannerFrequency;
        if (isArticleNativeEnabled()) return articleNativeFrequency;
        return 999;
    }
    public boolean isArticleBannerEnabled() { return master && bannerEnabled && articleBannerEnabled && articleBannerMax > 0; }
    public boolean isArticleNativeEnabled() { return master && nativeEnabled && articleNativeEnabled && articleNativeMax > 0; }

    private void showBannerInternal(LinearLayout parent) {
        if (!master || !bannerEnabled) return;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56));
        lp.setMargins(0, dp(6), 0, dp(12));
        if (admobEnabled && !admobBanner.isEmpty()) {
            AdView ad = new AdView(activity);
            ad.setAdSize(AdSize.BANNER);
            ad.setAdUnitId(admobBanner);
            ad.setAdListener(new AdListener() {
                @Override public void onAdFailedToLoad(LoadAdError error) {
                    if (metaEnabled && !metaBanner.isEmpty()) addMetaBanner(parent, lp);
                }
            });
            parent.addView(ad, lp);
            ad.loadAd(new AdRequest.Builder().build());
        } else if (metaEnabled && !metaBanner.isEmpty()) {
            addMetaBanner(parent, lp);
        }
    }

    private void addMetaBanner(LinearLayout parent, LinearLayout.LayoutParams lp) {
        com.facebook.ads.AdView meta = new com.facebook.ads.AdView(activity, metaBanner, com.facebook.ads.AdSize.BANNER_HEIGHT_50);
        parent.addView(meta, lp);
        meta.loadAd(meta.buildLoadAdConfig().build());
    }

    private void showNativeWithBannerFallback(LinearLayout parent) {
        if (!master || !nativeEnabled) return;
        if (!admobEnabled || admobNative.isEmpty()) {
            showBannerInternal(parent);
            return;
        }
        final NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_native, parent, false);
        final boolean[] failed = {false};
        AdLoader loader = new AdLoader.Builder(activity, admobNative)
                .forNativeAd(nativeAd -> populateNative(nativeAd, adView))
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .withAdListener(new AdListener() {
                    @Override public void onAdFailedToLoad(LoadAdError error) {
                        if (failed[0]) return;
                        failed[0] = true;
                        parent.removeView(adView);
                        showBannerInternal(parent);
                    }
                })
                .build();
        parent.addView(adView, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        loader.loadAd(new AdRequest.Builder().build());
    }

    public void showNative(LinearLayout parent) {
        if (!master || !nativeEnabled || !admobEnabled || admobNative.isEmpty()) return;
        final NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_native, parent, false);
        AdLoader loader = new AdLoader.Builder(activity, admobNative)
                .forNativeAd(nativeAd -> populateNative(nativeAd, adView))
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .withAdListener(new AdListener() {})
                .build();
        parent.addView(adView, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        loader.loadAd(new AdRequest.Builder().build());
    }

    private void populateNative(NativeAd ad, NativeAdView view) {
        android.widget.TextView headline = view.findViewById(R.id.ad_headline);
        android.widget.TextView body = view.findViewById(R.id.ad_body);
        android.widget.TextView cta = view.findViewById(R.id.ad_cta);
        android.widget.TextView advertiser = view.findViewById(R.id.ad_advertiser);
        android.widget.ImageView icon = view.findViewById(R.id.ad_icon);
        MediaView media = view.findViewById(R.id.ad_media);
        headline.setText(ad.getHeadline());
        view.setHeadlineView(headline);
        if (ad.getBody() != null) { body.setText(ad.getBody()); body.setVisibility(android.view.View.VISIBLE); view.setBodyView(body); }
        else body.setVisibility(android.view.View.GONE);
        if (ad.getCallToAction() != null) { cta.setText(ad.getCallToAction()); cta.setVisibility(android.view.View.VISIBLE); view.setCallToActionView(cta); }
        else cta.setVisibility(android.view.View.GONE);
        if (ad.getAdvertiser() != null) { advertiser.setText(ad.getAdvertiser()); advertiser.setVisibility(android.view.View.VISIBLE); view.setAdvertiserView(advertiser); }
        else advertiser.setVisibility(android.view.View.GONE);
        if (ad.getIcon() != null) { icon.setImageDrawable(ad.getIcon().getDrawable()); icon.setVisibility(android.view.View.VISIBLE); view.setIconView(icon); }
        else icon.setVisibility(android.view.View.GONE);
        if (ad.getMediaContent() != null) { media.setMediaContent(ad.getMediaContent()); media.setVisibility(android.view.View.VISIBLE); view.setMediaView(media); }
        else media.setVisibility(android.view.View.GONE);
        view.setNativeAd(ad);
    }

    public void onPostOpened() {
        if (!master || !interstitialEnabled) return;
        opened++;
        long now = System.currentTimeMillis();
        if (opened < interstitialFrequency) return;
        if (now - lastInterstitialMs < interstitialCooldownMs) return;
        opened = 0;
        lastInterstitialMs = now;
        prefs.edit().putLong("last_interstitial_ms", now).apply();
        showInterstitial();
    }

    public void showInterstitial() {
        if (!master || !interstitialEnabled) return;
        if (interstitial != null) {
            InterstitialAd ad = interstitial;
            interstitial = null;
            ad.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                @Override public void onAdDismissedFullScreenContent() { loadInterstitial(); }
                @Override public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError e) { loadInterstitial(); }
            });
            ad.show(activity);
            return;
        }
        loadInterstitial();
    }

    private void loadInterstitial() {
        if (!admobEnabled || admobInterstitial.isEmpty()) return;
        InterstitialAd.load(activity, admobInterstitial, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(InterstitialAd ad) { interstitial = ad; }
            @Override public void onAdFailedToLoad(LoadAdError error) { interstitial = null; showMetaInterstitialFallback(); }
        });
    }

    private void showMetaInterstitialFallback() {
        if (!master || !metaEnabled || metaInterstitial.isEmpty()) return;
        com.facebook.ads.InterstitialAd ad = new com.facebook.ads.InterstitialAd(activity, metaInterstitial);
        com.facebook.ads.InterstitialAdListener listener = new com.facebook.ads.InterstitialAdListener() {
            public void onInterstitialDisplayed(com.facebook.ads.Ad a) {}
            public void onInterstitialDismissed(com.facebook.ads.Ad a) { ad.destroy(); }
            public void onError(com.facebook.ads.Ad a, com.facebook.ads.AdError e) { ad.destroy(); }
            public void onAdLoaded(com.facebook.ads.Ad a) { ad.show(); }
            public void onAdClicked(com.facebook.ads.Ad a) {}
            public void onLoggingImpression(com.facebook.ads.Ad a) {}
        };
        ad.loadAd(ad.buildLoadAdConfig().withAdListener(listener).build());
    }

    private void loadRewarded() {
        if (admobRewarded == null || admobRewarded.isEmpty()) return;
        RewardedAd.load(activity, admobRewarded, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) { rewarded = ad; }
            @Override public void onAdFailedToLoad(LoadAdError error) { rewarded = null; }
        });
    }

    public boolean isRewardedEnabled() { return master && admobEnabled && rewardedEnabled; }

    public boolean showRewarded(final RewardCallback callback) {
        if (!isRewardedEnabled() || rewarded == null) return false;
        RewardedAd ad = rewarded;
        rewarded = null;
        ad.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { loadRewarded(); }
            @Override public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError e) { loadRewarded(); }
        });
        ad.show(activity, rewardItem -> callback.onReward(rewardItem));
        return true;
    }

    public void showRewardedWhenReady(final RewardCallback callback, final Runnable unavailable) {
        if (!isRewardedEnabled()) { unavailable.run(); return; }
        if (showRewarded(callback)) return;
        loadRewarded();
        final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        final long deadline = android.os.SystemClock.uptimeMillis() + 8000L;
        final Runnable poll = new Runnable() {
            @Override public void run() {
                if (showRewarded(callback)) return;
                if (android.os.SystemClock.uptimeMillis() >= deadline) { unavailable.run(); return; }
                h.postDelayed(this, 250L);
            }
        };
        h.post(poll);
    }

    private void loadRewardedInterstitial() {
        if (admobRewardedInterstitial == null || admobRewardedInterstitial.isEmpty()) return;
        RewardedInterstitialAd.load(activity, admobRewardedInterstitial, new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
            @Override public void onAdLoaded(RewardedInterstitialAd ad) { rewardedInterstitial = ad; }
            @Override public void onAdFailedToLoad(LoadAdError error) { rewardedInterstitial = null; }
        });
    }

    public boolean showRewardedInterstitial(final RewardCallback callback) {
        if (!master || !rewardedInterstitialEnabled || rewardedInterstitial == null) return false;
        RewardedInterstitialAd ad = rewardedInterstitial;
        rewardedInterstitial = null;
        ad.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { loadRewardedInterstitial(); }
            @Override public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError e) { loadRewardedInterstitial(); }
        });
        ad.show(activity, rewardItem -> callback.onReward(rewardItem));
        return true;
    }

    public void showAppOpen() {
        if (!master || !appOpenEnabled || !admobEnabled || admobAppOpen.isEmpty() || appOpenShowing) return;
        AppOpenAd.load(activity, admobAppOpen, new AdRequest.Builder().build(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override public void onAdLoaded(AppOpenAd ad) {
                appOpenShowing = true;
                ad.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() { appOpenShowing = false; }
                    @Override public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError e) { appOpenShowing = false; }
                });
                ad.show(activity);
            }
            @Override public void onAdFailedToLoad(LoadAdError error) { appOpenShowing = false; }
        });
    }

    public interface RewardCallback { void onReward(RewardItem reward); }
    private int dp(int x) { return (int)(x * activity.getResources().getDisplayMetrics().density + .5f); }
}
