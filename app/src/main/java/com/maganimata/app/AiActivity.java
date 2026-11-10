package com.maganimata.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class AiActivity extends Activity {
    // Premium Color System
    private final int COLOR_PRIMARY = Color.rgb(201, 67, 128);      // #C94380 Soft Pink
    private final int COLOR_PRIMARY_DARK = Color.rgb(112, 42, 80); // #702A50 Plum
    private final int COLOR_DARK = Color.rgb(48, 32, 44);          // #30202C Dark Charcoal Text
    private final int COLOR_MUTED = Color.rgb(112, 101, 110);      // #70656E Medium Gray
    private final int COLOR_BG = Color.rgb(255, 248, 252);         // #FFF8FC Light Pinkish BG
    private final int COLOR_BORDER = Color.rgb(243, 226, 236);     // #F3E2EC Light Pink Border

    private LinearLayout chatContainer;
    private ScrollView chatScrollView;
    private EditText inputField;
    private TextView sendButton;
    private View activeLoadingBubble = null;
    private boolean isRequesting = false;
    private String token = "";

    private int dp(float x) {
        return (int) (x * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }

    private GradientDrawable bg(int color, float cornerRadius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(cornerRadius));
        return g;
    }

    private GradientDrawable strokeBg(int fillColor, int strokeColor, int strokeWidthDp, float cornerRadius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fillColor);
        g.setStroke(dp(strokeWidthDp), strokeColor);
        g.setCornerRadius(dp(cornerRadius));
        return g;
    }

    private GradientDrawable gradientBg(int startColor, int endColor, float cornerRadius) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{startColor, endColor});
        g.setCornerRadius(dp(cornerRadius));
        return g;
    }

    private void animateTouch(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        token = getSharedPreferences("magani_prefs", MODE_PRIVATE).getString("auth_token", "");
        buildShell();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackground(gradientBg(COLOR_PRIMARY_DARK, COLOR_PRIMARY, 0));
        header.setElevation(dp(4));

        TextView backBtn = text("←", 26, Color.WHITE, true);
        backBtn.setGravity(Gravity.CENTER);
        header.addView(backBtn, new LinearLayout.LayoutParams(dp(42), dp(44)));
        backBtn.setOnClickListener(v -> finish());
        animateTouch(backBtn);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(10), 0, 0, 0);

        TextView title = text("Tambayi AI • Ask AI Assistant", 20, Color.WHITE, true);
        TextView subtitle = text("Tambaye shi duk wata tambaya game da lafiyar mata", 11, 0xFFF8DDEA, false);
        titleBox.addView(title);
        titleBox.addView(subtitle);

        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header);

        // Chat Messages Stream
        chatScrollView = new ScrollView(this);
        chatScrollView.setFillViewport(true);

        chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatContainer.setPadding(dp(14), dp(12), dp(14), dp(16));

        chatScrollView.addView(chatContainer, new ScrollView.LayoutParams(-1, -2));
        root.addView(chatScrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        // 1. Premium Promotional Header Banner
        addHeaderBanner();

        // 2. Initial Medical Disclaimer Banner
        addDisclaimerBanner();

        // 3. Initial AI Greeting Bubble
        addAiBubble("Sannu! Ni ce AI Assistant ta Lafiyar Mace 🤖.\nZa ki iya tambaya ta ko wace irin tambaya game da al'ada, ciki, fitar ƙwai, da kula da lafiyar mata a Harshen Hausa ko Turanci.\n\nYaya zan taimake ki yau?");

        // Suggested Prompt Chips
        root.addView(createSuggestedChips());

        // Bottom Input Bar
        root.addView(createInputBar());

        setContentView(root);
    }

    private void addHeaderBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(16), dp(14), dp(16), dp(14));
        banner.setBackground(gradientBg(COLOR_PRIMARY_DARK, COLOR_PRIMARY, 18));
        banner.setElevation(dp(3));

        LinearLayout copyBox = new LinearLayout(this);
        copyBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = text("Tambayi Lafiyar Mace AI", 17, Color.WHITE, true);
        TextView subtitle = text("Samun bayanan lafiya cikin Hausa da Turanci.", 12, 0xFFF8DDEA, true);
        subtitle.setPadding(0, dp(2), 0, dp(2));

        TextView body = text("Tambayi AI game da al'ada, ciki, ovulation da kula da lafiyar mata.", 11, 0xFFF3D0E2, false);
        body.setLineSpacing(dp(2), 1.0f);

        TextView btn = text("Fara Tambaya  ➔", 11, COLOR_PRIMARY_DARK, true);
        btn.setPadding(dp(12), dp(6), dp(12), dp(6));
        btn.setBackground(bg(Color.WHITE, 14));
        btn.setOnClickListener(v -> {
            if (inputField != null) {
                inputField.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        animateTouch(btn);

        copyBox.addView(title);
        copyBox.addView(subtitle);
        copyBox.addView(body);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, -2);
        btnLp.setMargins(0, dp(8), 0, 0);
        copyBox.addView(btn, btnLp);

        banner.addView(copyBox, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.lafiyar_mace_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("Lafiyar Mace logo");

        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        logoLp.setMargins(dp(10), 0, 0, 0);
        banner.addView(logo, logoLp);

        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(-1, -2);
        bannerLp.setMargins(0, 0, 0, dp(12));
        chatContainer.addView(banner, bannerLp);
    }

    private void addDisclaimerBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(dp(14), dp(12), dp(14), dp(12));
        banner.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

        TextView title = text("💡 ILIMI DA KULA WA / HEALTH DISCLAIMER", 12, COLOR_PRIMARY_DARK, true);
        TextView body = text("Wannan AI yana bayar da ilimin lafiya ne kawai. Ba ya mayurbin gwajin likita ko asibiti. Idan kina da alama mai tsanani ko zafi marar jurewa, tuntubi likita ko asibiti maza-maza.", 11, COLOR_MUTED, false);
        body.setLineSpacing(dp(3), 1.0f);
        body.setPadding(0, dp(4), 0, 0);

        banner.addView(title);
        banner.addView(body);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(14));
        chatContainer.addView(banner, lp);
    }

    private View createSuggestedChips() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(6), dp(14), dp(6));

        String[] prompts = {
                "🌸 Menene alamomin fitar kwai?",
                "🩸 Yadda ake rage zafin mara?",
                "🤰 Yaya ake gano kwanakin ciki?",
                "🩺 Kula da lafiyar jiki yayin al'ada"
        };

        for (String p : prompts) {
            TextView chip = text(p, 12, COLOR_PRIMARY_DARK, true);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

            chip.setOnClickListener(v -> {
                inputField.setText(p.replaceAll("^[🌸🩸🤰🩺]\\s*", ""));
                submitQuestion();
            });
            animateTouch(chip);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, dp(8), 0);
            row.addView(chip, lp);
        }

        hsv.addView(row);
        return hsv;
    }

    private View createInputBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(10), dp(12), dp(12));
        bar.setBackgroundColor(Color.WHITE);
        bar.setElevation(dp(8));

        inputField = new EditText(this);
        inputField.setHint("Rubuta tambayarki a nan... / Ask AI...");
        inputField.setHintTextColor(0xFF8C818A);
        inputField.setTextColor(COLOR_DARK);
        inputField.setTextSize(14);
        inputField.setPadding(dp(14), dp(12), dp(14), dp(12));
        inputField.setMaxLines(3);
        inputField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputField.setBackground(strokeBg(COLOR_BG, COLOR_BORDER, 1, 20));

        bar.addView(inputField, new LinearLayout.LayoutParams(0, -2, 1));

        sendButton = text("Aika ➢", 13, Color.WHITE, true);
        sendButton.setGravity(Gravity.CENTER);
        sendButton.setPadding(dp(14), dp(12), dp(14), dp(12));
        sendButton.setBackground(gradientBg(COLOR_PRIMARY, COLOR_PRIMARY_DARK, 20));

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, dp(46));
        btnLp.setMargins(dp(8), 0, 0, 0);
        bar.addView(sendButton, btnLp);

        sendButton.setOnClickListener(v -> submitQuestion());
        animateTouch(sendButton);

        return bar;
    }

    private void submitQuestion() {
        if (isRequesting) return;
        final String query = inputField.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Da fatan za a rubuta tambayarka tukunna / Please enter a question.", Toast.LENGTH_SHORT).show();
            return;
        }

        inputField.setText("");
        addUserBubble(query);
        showLoadingBubble();

        isRequesting = true;
        sendButton.setEnabled(false);
        sendButton.setAlpha(0.6f);

        Map<String, String> m = new HashMap<>();
        m.put("prompt", query);
        m.put("question", query);

        Api.post("../ask_ai.php", m, token, new Api.Callback() {
            @Override
            public void ok(String body) {
                runOnUiThread(() -> {
                    isRequesting = false;
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                    removeLoadingBubble();

                    try {
                        String cleanJson = body == null ? "" : body.trim();
                        int firstBrace = cleanJson.indexOf('{');
                        int lastBrace = cleanJson.lastIndexOf('}');
                        if (firstBrace >= 0 && lastBrace > firstBrace) {
                            cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
                        }

                        JSONObject r = new JSONObject(cleanJson);
                        if (r.optBoolean("ok", true) && (r.has("answer") || r.has("response"))) {
                            String reply = r.optString("answer", r.optString("response", "An amsa tambayarki cikin nasara."));
                            addAiBubble(reply);
                        } else {
                            String err = r.optString("error", "An samu matsala wajen gano amsa.");
                            err = err.replaceAll("<[^>]*>", "").trim();
                            addErrorBubble(err);
                            inputField.setText(query);
                        }
                    } catch (Exception e) {
                        addErrorBubble("Kasa karanta amsar AI. Da fatan a sake gwadawa.");
                        inputField.setText(query);
                    }
                });
            }

            @Override
            public void err(Exception e) {
                runOnUiThread(() -> {
                    isRequesting = false;
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                    removeLoadingBubble();

                    String detail = e == null || e.getMessage() == null ? "Check your internet connection." : e.getMessage();
                    detail = detail.replaceAll("<[^>]*>", "").trim();
                    String detailLower = detail.toLowerCase(java.util.Locale.US);

                    if (detailLower.contains("timeout") || detailLower.contains("timed out")) {
                        detail = "Lokacin amsawa ya wuce (Timeout). Da fatan za a sake gwadawa ko kuma a taƙaita tambayarki.";
                    } else if (detail.contains("404")) {
                        detail = "Sabar AI tana samun gyara (HTTP 404). Da fatan za a sake gwadawa daga baya.";
                    }
                    addErrorBubble("An samu kuskure: " + detail);
                    inputField.setText(query);
                });
            }
        });
    }

    private void addUserBubble(String message) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.END);
        row.setPadding(0, dp(4), 0, dp(6));

        TextView bubble = text(message, 14, Color.WHITE, false);
        bubble.setLineSpacing(dp(3), 1.0f);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setBackground(gradientBg(COLOR_PRIMARY, COLOR_PRIMARY_DARK, 18));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = Gravity.END;
        row.addView(bubble, lp);

        chatContainer.addView(row);
        scrollToBottom();
    }

    private void addAiBubble(String message) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.START);
        row.setPadding(0, dp(4), 0, dp(6));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 18));

        TextView header = text("🤖 Lafiyar Mace AI", 12, COLOR_PRIMARY_DARK, true);
        header.setPadding(0, 0, 0, dp(6));

        TextView bubble = text(message, 14, COLOR_DARK, false);
        bubble.setLineSpacing(dp(4), 1.05f);

        card.addView(header);
        card.addView(bubble);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = Gravity.START;
        row.addView(card, lp);

        chatContainer.addView(row);
        scrollToBottom();
    }

    private void addErrorBubble(String errorMessage) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.START);
        row.setPadding(0, dp(4), 0, dp(6));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(bg(0xFFFEE2E2, 16));

        TextView header = text("⚠️ Kuskure / Error", 12, 0xFF991B1B, true);
        TextView body = text(errorMessage, 13, 0xFF7F1D1D, false);
        body.setLineSpacing(dp(3), 1.0f);

        card.addView(header);
        card.addView(body);

        row.addView(card);
        chatContainer.addView(row);
        scrollToBottom();
    }

    private void showLoadingBubble() {
        if (activeLoadingBubble != null) return;

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.START);
        row.setPadding(0, dp(4), 0, dp(6));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 18));

        ProgressBar pb = new ProgressBar(this);
        pb.setIndeterminate(true);
        card.addView(pb, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView tv = text(" AI tana tunani... / Thinking...", 13, COLOR_PRIMARY_DARK, true);
        tv.setPadding(dp(8), 0, 0, 0);
        card.addView(tv);

        row.addView(card);
        chatContainer.addView(row);
        activeLoadingBubble = row;
        scrollToBottom();
    }

    private void removeLoadingBubble() {
        if (activeLoadingBubble != null) {
            chatContainer.removeView(activeLoadingBubble);
            activeLoadingBubble = null;
        }
    }

    private void scrollToBottom() {
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
