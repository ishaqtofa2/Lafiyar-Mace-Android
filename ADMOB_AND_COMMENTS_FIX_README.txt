LAFIYAR MACE - ADMOB IDS AND COMMENTS PATCH

ADMOB IDs IN APP CODE
1. Open app/src/main/java/com/maganimata/app/Config.java
2. Edit ADMOB_BANNER_ID, ADMOB_NATIVE_ID, ADMOB_INTERSTITIAL_ID,
   ADMOB_REWARDED_ID, ADMOB_REWARDED_INTERSTITIAL_ID and ADMOB_APP_OPEN_ID.
3. Use a DIFFERENT Ad Unit ID for each ad format. The supplied Banner ID and
   Interstitial ID were retained. Native/Rewarded/Rewarded Interstitial/App Open
   are blank because no separate IDs for those formats were supplied.
4. The Ad Unit IDs in AdController now come only from Config.java; remote
   settings can no longer overwrite them. The server still controls the on/off
   switches for ads.
5. The AdMob App ID is configured in BOTH Config.java and
   app/src/main/AndroidManifest.xml. If the App ID changes, update the manifest
   value for com.google.android.gms.ads.APPLICATION_ID too.
6. Rebuild and reinstall the APK after changing IDs. Use Google test ad IDs
   during development; live ads may not fill every request.

COMMENTS
- PostActivity's comment composer now validates empty/oversized text, disables
  the submit button while sending, prevents accidental double submission, and
  displays the real HTTP/API error message when the request fails.
- The Android source ZIP provided for this task does not contain the PHP backend.
  The app calls GET/POST https://gyaranaure.com.ng/magani/api/comments.php.
  For comments to persist and work for all users, that server endpoint must exist
  and support GET ?post_id=..., POST post_id/content, reply action + parent_id,
  and action=like_comment. If this endpoint is missing or its database schema
  differs, the app-side patch alone cannot create server-side comments; include
  the PHP API/backend project to fix that part completely.

BUILD NOTE
Gradle build could not be completed in this environment because Gradle 9.3.0
was not cached and internet access to services.gradle.org was unavailable.
