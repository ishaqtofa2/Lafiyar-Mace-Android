MAGANI MATA v1.3 — Android Project

What was updated:
- English UI throughout the Android app.
- Feminine healthcare color palette and women's-health heart/cross icon.
- Home / Categories / Saved / Settings bottom navigation.
- Fixed API image URL handling and landscape article images.
- Server-driven AdMob settings.
- Banner, Native, Interstitial, Rewarded, Rewarded Interstitial and App Open AdMob support.
- Meta Audience Network banner/interstitial fallback support; Meta native/rewarded can be served through AdMob mediation after the Meta adapter is configured in AdMob.
- Ads can be enabled/disabled from the backend settings.
- Interstitial frequency is server controlled.

Testing IDs currently included:
AdMob App ID: ca-app-pub-3940256099942544~3347511713
Banner: ca-app-pub-3940256099942544/9214589741
Native: ca-app-pub-3940256099942544/2247696110
Interstitial: ca-app-pub-3940256099942544/1033173712
Rewarded: ca-app-pub-3940256099942544/5224354917
Rewarded Interstitial: ca-app-pub-3940256099942544/5354046379
App Open: ca-app-pub-3940256099942544/9257395921

Backend API:
https://gyaranaure.com.ng/magani/api/

Before production:
1. Replace Google test IDs with your real AdMob IDs.
2. Configure Meta Audience Network in AdMob mediation and enter the real placement/app values in the backend dashboard.
3. Replace the database password in backend/config/config.php with the real server password. Never publish database credentials in the app.
4. Run database_migrate_ads.sql if the existing database does not yet have the new ad settings keys.
5. Build a release APK/AAB and test ads on a real device using the platform's test mode.

Build note:
The source was updated here, but this environment could not download Gradle 9.3.0 because outbound network access is unavailable. Open the project in Android Studio and let Gradle sync normally before building.
