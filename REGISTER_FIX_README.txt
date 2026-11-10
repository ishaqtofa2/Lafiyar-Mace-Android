Lafiyar Mace - Register/API diagnostic fix

Updated:
- Api.java now follows redirects, sends a User-Agent, has slightly longer timeouts, and reports HTTP status + server response instead of hiding the cause.
- AuthActivity.java now shows the actual API/network error during registration/login/reset.
- Config.java was not changed because the correct production API hostname must be confirmed from the live backend.

IMPORTANT:
The Android ZIP does not contain the PHP backend/auth.php. The app currently points to:
https://gyaranaure.com.ng/magani/api/

If registration still shows a DNS/SSL/HTTP error, the PHP backend URL/server must be checked. The new message will show the exact error so it can be fixed rather than guessing.
