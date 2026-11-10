package com.maganimata.app;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.gms.ads.MobileAds;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    // Premium Color System
    private final int PRIMARY = Color.rgb(217, 60, 134);      // #D93C86 Primary Pink
    private final int PRIMARY_DARK = Color.rgb(135, 54, 111);  // #87366F Deep Plum
    private final int DARK = Color.rgb(48, 32, 44);           // #30202C Dark Charcoal Text
    private final int MUTED = Color.rgb(118, 107, 117);       // #766B75 Muted Text
    private final int BG = Color.rgb(255, 242, 248);          // #FFF2F8 Light Pink BG
    private final int BORDER = Color.rgb(245, 224, 235);      // #F5E0EB Soft Border

    private LinearLayout content;
    private TextView pageTitle, pageSubtitle;
    private EditText search;
    private final List<JSONObject> allPosts = new ArrayList<>();
    private final List<JSONObject> marketplaceProducts = new ArrayList<>();
    private final Set<Integer> savedIds = new LinkedHashSet<>();
    private SharedPreferences prefs;
    private AdController ads;
    private String activeCategory = "All";
    private String currentQuery = "";
    private boolean settingsLoaded = false;
    private boolean appOpenRequested = false;

    // Loading & Empty state flags
    private boolean postsLoading = false;
    private boolean postsLoaded = false;

    private final List<JSONObject> sliders = new ArrayList<>();
    private final Handler sliderHandler = new Handler();
    private int sliderIndex = 0;
    private final Handler marketplaceHandler = new Handler();
    private int marketplaceIndex = 0;
    private final List<Animator> activeAnimators = new ArrayList<>();

    private String dailyTipTitle = "Daily Health Tip";
    private String dailyTipText = "Drink enough water daily to keep your body healthy and your skin glowing.";
    private String dailyTipLink = "";
    private TextView notificationBadge;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }

    private TextView sectionTitle(String value) {
        TextView t = text(value, 18, DARK, true);
        t.setPadding(0, dp(6), 0, dp(8));
        return t;
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

    @SuppressLint("ClickableViewAccessibility")
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

    private boolean areAnimationsEnabled() {
        try {
            float duration = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            return duration > 0;
        } catch (Exception e) {
            return true;
        }
    }

    private void addBreathingAnimation(View target) {
        if (!areAnimationsEnabled()) return;
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(target, "scaleX", 1.0f, 1.06f, 1.0f);
        pulseX.setDuration(2200);
        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseX.setRepeatMode(ValueAnimator.RESTART);

        ObjectAnimator pulseY = ObjectAnimator.ofFloat(target, "scaleY", 1.0f, 1.06f, 1.0f);
        pulseY.setDuration(2200);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatMode(ValueAnimator.RESTART);

        pulseX.start();
        pulseY.start();
        activeAnimators.add(pulseX);
        activeAnimators.add(pulseY);
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("magani_prefs", MODE_PRIVATE);
        loadSaved();
        MobileAds.initialize(this, status -> {
        });
        ads = new AdController(this);
        buildShell();
        loadAdSettings();
        loadSliders();
        loadDailyTip();
        loadMarketplaceProducts();
        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null) {
            loadNotificationCount(notificationBadge);
        }
        if ("All".equals(activeCategory) && currentQuery.isEmpty()) {
            showHome();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacksAndMessages(null);
        marketplaceHandler.removeCallbacksAndMessages(null);
        for (Animator a : activeAnimators) {
            if (a != null && a.isRunning()) a.cancel();
        }
        activeAnimators.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Animator a : activeAnimators) {
            if (a != null) a.cancel();
        }
        activeAnimators.clear();
    }

    private void loadNotificationCount(final TextView directBadge) {
        String token = prefs == null ? "" : prefs.getString("auth_token", "");
        if (token.isEmpty()) {
            if (directBadge != null) directBadge.setVisibility(View.GONE);
            return;
        }
        Api.get("notifications.php", token, new Api.Callback() {
            public void ok(String body) {
                runOnUiThread(() -> {
                    try {
                        int count = new JSONObject(body).optInt("unread_count", 0);
                        if (directBadge != null) {
                            directBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                            directBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                        }
                    } catch (Exception ignored) {
                    }
                });
            }

            public void err(Exception e) {
                if (directBadge != null) runOnUiThread(() -> directBadge.setVisibility(View.GONE));
            }
        });
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(14), dp(8), dp(14), dp(10));
        header.setBackground(gradientBg(PRIMARY_DARK, PRIMARY, 0));
        header.setElevation(dp(4));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView menu = text("☰", 24, Color.WHITE, true);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("Open menu");
        titleRow.addView(menu, new LinearLayout.LayoutParams(dp(38), dp(42)));
        menu.setOnClickListener(v -> showSidebar(menu));
        animateTouch(menu);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.lafiyar_mace_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("Lafiyar Mace logo");
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        logoParams.setMargins(0, 0, dp(6), 0);
        titleRow.addView(logo, logoParams);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView brandTitle = text("Lafiyar Mace", 20, Color.WHITE, true);
        TextView brandSub = text("Promoting Women's Health Through Education and Awareness", 10, 0xFFF8DDEA, false);
        brand.addView(brandTitle);
        brand.addView(brandSub);
        pageTitle = brandTitle;
        pageSubtitle = brandSub;
        titleRow.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        FrameLayout notificationWrap = new FrameLayout(this);
        TextView notification = text("🔔", 20, Color.WHITE, false);
        notification.setGravity(Gravity.CENTER);
        notificationWrap.addView(notification, new FrameLayout.LayoutParams(dp(40), dp(40)));
        TextView badge = text("", 9, Color.WHITE, true);
        notificationBadge = badge;
        badge.setGravity(Gravity.CENTER);
        badge.setMinWidth(dp(18));
        badge.setPadding(dp(4), 0, dp(4), 0);
        GradientDrawable badgeBg = bg(0xFFE53935, 12);
        badge.setBackground(badgeBg);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP | Gravity.END);
        notificationWrap.addView(badge, bp);
        notification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        animateTouch(notification);

        titleRow.addView(notificationWrap, new LinearLayout.LayoutParams(dp(40), dp(42)));
        loadNotificationCount(badge);

        TextView refresh = text("↻", 22, Color.WHITE, false);
        refresh.setGravity(Gravity.CENTER);
        refresh.setOnClickListener(v -> {
            loadAdSettings();
            loadSliders();
            loadDailyTip();
            loadMarketplaceProducts();
            loadPosts();
            loadNotificationCount(badge);
        });
        animateTouch(refresh);
        titleRow.addView(refresh, new LinearLayout.LayoutParams(dp(36), dp(40)));
        header.addView(titleRow);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search health articles, topics...");
        search.setTextSize(13);
        search.setTextColor(DARK);
        search.setHintTextColor(0xFF8C818A);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(8));
        search.setBackground(bg(Color.WHITE, 24));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(42));
        sp.setMargins(0, dp(6), 0, 0);
        header.addView(search, sp);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            public void onTextChanged(CharSequence s, int st, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase();
                showHome();
            }

            public void afterTextChanged(Editable e) {
            }
        });
        root.addView(header, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Main Content Area with Floating Ask AI Button Overlay
        FrameLayout mainContainer = new FrameLayout(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(80)); // Bottom padding so content scrolls above FAB
        scroll.addView(content);

        mainContainer.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        // TASK 2: FLOATING "ASK AI" BUTTON (HIGH PRIORITY)
        LinearLayout askAiFab = new LinearLayout(this);
        askAiFab.setOrientation(LinearLayout.HORIZONTAL);
        askAiFab.setGravity(Gravity.CENTER_VERTICAL);
        askAiFab.setPadding(dp(16), dp(10), dp(18), dp(10));
        askAiFab.setBackground(gradientBg(PRIMARY_DARK, PRIMARY, 28));
        askAiFab.setElevation(dp(6));

        TextView fabIcon = text("🤖", 20, Color.WHITE, false);
        TextView fabText = text("Ask AI", 13, Color.WHITE, true);
        fabText.setPadding(dp(8), 0, 0, 0);

        askAiFab.addView(fabIcon);
        askAiFab.addView(fabText);

        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(-2, dp(48));
        fabLp.gravity = Gravity.BOTTOM | Gravity.END;
        fabLp.setMargins(0, 0, dp(16), dp(16));
        mainContainer.addView(askAiFab, fabLp);

        askAiFab.setOnClickListener(v -> startActivity(new Intent(this, AiActivity.class)));
        animateTouch(askAiFab);
        addBreathingAnimation(askAiFab);

        root.addView(mainContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        // Bottom Navigation Bar
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(6), dp(8), dp(6));
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(dp(12));

        addNavButton(nav, "⌂", "Home", v -> showHome());
        addNavButton(nav, "✚", "Health tools", v -> startActivity(new Intent(this, ToolsActivity.class)));
        addNavButton(nav, "☰", "More", v -> showSidebar(nav));
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
    }

    private void addNavButton(LinearLayout nav, String icon, String label, View.OnClickListener click) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(5), dp(3), dp(5), dp(3));

        TextView i = text(icon, 20, PRIMARY, false);
        i.setGravity(Gravity.CENTER);
        TextView t = text(label, 11, MUTED, true);
        t.setGravity(Gravity.CENTER);

        item.addView(i, new LinearLayout.LayoutParams(-1, dp(28)));
        item.addView(t, new LinearLayout.LayoutParams(-1, dp(20)));

        item.setOnClickListener(click);
        animateTouch(item);

        nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void loadAdSettings() {
        Api.get("settings.php", new Api.Callback() {
            public void ok(String body) {
                runOnUiThread(() -> {
                    try {
                        JSONObject r = new JSONObject(body);
                        JSONObject settings = r.optJSONObject("settings");
                        ads.apply(settings != null ? settings : r);
                        settingsLoaded = true;
                        if ("All".equals(activeCategory) && currentQuery.isEmpty()) {
                            showHome();
                        }
                        if (!appOpenRequested) {
                            appOpenRequested = true;
                            ads.showAppOpen();
                        }
                    } catch (Exception ex) {
                        android.util.Log.e("LafiyarMaceAds", "Could not parse ad settings", ex);
                    }
                });
            }

            public void err(Exception e) {
                runOnUiThread(() -> {
                    ads.apply(new JSONObject());
                    settingsLoaded = true;
                    if ("All".equals(activeCategory) && currentQuery.isEmpty()) showHome();
                    if (!appOpenRequested) {
                        appOpenRequested = true;
                        ads.showAppOpen();
                    }
                });
            }
        });
    }

    private void loadSliders() {
        Api.get("sliders.php", new Api.Callback() {
            public void ok(String body) {
                runOnUiThread(() -> {
                    try {
                        JSONObject r = new JSONObject(body);
                        JSONArray arr = r.optJSONArray("sliders");
                        sliders.clear();
                        if (arr != null) for (int i = 0; i < arr.length(); i++) sliders.add(arr.getJSONObject(i));
                        sliderIndex = 0;
                        showHome();
                    } catch (Exception ignored) {
                    }
                });
            }

            public void err(Exception e) {
                runOnUiThread(() -> {
                    sliders.clear();
                    showHome();
                });
            }
        });
    }

    private void loadMarketplaceProducts() {
        Api.get("marketplace.php", new Api.Callback() {
            public void ok(String body) {
                runOnUiThread(() -> {
                    try {
                        JSONObject r = new JSONObject(body);
                        JSONArray arr = r.optJSONArray("products");
                        marketplaceProducts.clear();
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                marketplaceProducts.add(arr.getJSONObject(i));
                            }
                        }
                        if ("All".equals(activeCategory) && currentQuery.isEmpty()) {
                            showHome();
                        }
                    } catch (Exception ignored) {
                    }
                });
            }

            public void err(Exception e) {
            }
        });
    }

    private void showSlider() {
        if (sliders.isEmpty()) return;
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setPadding(0, 0, 0, dp(6));
        hsv.addView(track, new HorizontalScrollView.LayoutParams(-2, ViewGroup.LayoutParams.WRAP_CONTENT));

        int width = getResources().getDisplayMetrics().widthPixels - dp(28);
        for (int i = 0; i < sliders.size(); i++) {
            JSONObject item = sliders.get(i);
            final String link = item.optString("link_url", "");
            final String image = Api.absoluteImageUrl(item.optString("image_url", ""));
            final String title = item.optString("title", "");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(6), dp(6), dp(6), dp(6));
            card.setBackground(strokeBg(Color.WHITE, BORDER, 1, 18));
            card.setElevation(dp(2));

            ImageView img = new ImageView(this);
            img.setAdjustViewBounds(true);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setMaxHeight(dp(180));
            GradientDrawable ph = bg(0xFFF1E7EE, 14);
            img.setBackground(ph);

            if (!image.isEmpty()) {
                com.bumptech.glide.Glide.with(this).load(image).placeholder(ph).error(ph).into(img);
            }
            card.addView(img, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (!title.isEmpty()) {
                TextView tt = text(title, 14, DARK, true);
                tt.setMaxLines(2);
                tt.setPadding(dp(6), dp(7), dp(6), dp(4));
                card.addView(tt);
            }

            card.setOnClickListener(v -> {
                if (!link.isEmpty()) try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                } catch (Exception ignored) {
                }
            });
            animateTouch(card);

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, dp(10), 0);
            track.addView(card, cp);
        }
        content.addView(hsv, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView hint = text("Sponsored", 10, MUTED, false);
        hint.setGravity(Gravity.END);
        hint.setPadding(0, 0, dp(5), dp(4));
        content.addView(hint);
        scheduleSlider(hsv, width);
    }

    private void scheduleSlider(HorizontalScrollView hsv, int width) {
        sliderHandler.removeCallbacksAndMessages(null);
        if (sliders.size() < 2) return;
        sliderHandler.postDelayed(new Runnable() {
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                sliderIndex = (sliderIndex + 1) % sliders.size();
                hsv.smoothScrollTo(sliderIndex * width + sliderIndex * dp(10), 0);
                sliderHandler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    private void showMarketplaceCarousel() {
        if (marketplaceProducts.isEmpty()) return;

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = sectionTitle("Kayayyaki daga Marketplace");
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView seeAll = text("Duba duka  →", 12, PRIMARY, true);
        seeAll.setGravity(Gravity.CENTER_VERTICAL);
        seeAll.setOnClickListener(v -> startActivity(new Intent(this, ToolsActivity.class)));
        heading.addView(seeAll, new LinearLayout.LayoutParams(dp(92), dp(45)));

        content.addView(heading);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setPadding(0, dp(4), 0, dp(12));
        hsv.addView(track, new HorizontalScrollView.LayoutParams(-2, ViewGroup.LayoutParams.WRAP_CONTENT));

        int cardWidth = dp(150);

        for (int i = 0; i < marketplaceProducts.size(); i++) {
            JSONObject p = marketplaceProducts.get(i);
            final String pTitle = p.optString("title", "Product");
            final String pPrice = p.optString("price", "");
            final String pImg = Api.absoluteImageUrl(p.optString("image_url", ""));
            final String pDesc = p.optString("description", "");
            final String pContact = p.optString("contact", "");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(8), dp(8), dp(8), dp(10));
            card.setBackground(strokeBg(Color.WHITE, BORDER, 1, 18));
            card.setElevation(dp(2));

            ImageView img = new ImageView(this);
            img.setAdjustViewBounds(true);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setMaxHeight(dp(130));
            GradientDrawable ph = bg(0xFFF3EAF0, 14);
            img.setBackground(ph);

            if (!pImg.isEmpty()) {
                com.bumptech.glide.Glide.with(this).load(pImg).placeholder(ph).error(ph).into(img);
            }
            card.addView(img, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView name = text(pTitle, 13, DARK, true);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setPadding(dp(4), dp(6), dp(4), 0);
            card.addView(name);

            if (!pPrice.isEmpty()) {
                String priceFormatted = pPrice.startsWith("₦") ? pPrice : "₦" + pPrice;
                TextView priceTv = text(priceFormatted, 13, PRIMARY, true);
                priceTv.setPadding(dp(4), dp(2), dp(4), dp(2));
                card.addView(priceTv);
            }

            TextView action = text("Duba Kayan  ›", 11, PRIMARY, true);
            action.setPadding(dp(4), dp(2), dp(4), 0);
            card.addView(action);

            card.setOnClickListener(v -> openProductDetailsDialog(pTitle, pPrice, pDesc, pContact, pImg));
            animateTouch(card);

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, dp(10), 0);
            track.addView(card, cp);
        }

        content.addView(hsv, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        scheduleMarketplaceSlider(hsv, cardWidth);
    }

    private void scheduleMarketplaceSlider(HorizontalScrollView hsv, int itemWidth) {
        marketplaceHandler.removeCallbacksAndMessages(null);
        if (marketplaceProducts.size() < 2) return;
        marketplaceHandler.postDelayed(new Runnable() {
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                marketplaceIndex = (marketplaceIndex + 1) % marketplaceProducts.size();
                hsv.smoothScrollTo(marketplaceIndex * itemWidth + marketplaceIndex * dp(10), 0);
                marketplaceHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }

    private void openProductDetailsDialog(String name, String price, String desc, String contact, String imgUrl) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        if (!imgUrl.isEmpty()) {
            ImageView im = new ImageView(this);
            im.setAdjustViewBounds(true);
            im.setScaleType(ImageView.ScaleType.FIT_CENTER);
            im.setMaxHeight(dp(220));
            GradientDrawable ph = bg(0xFFF3EAF0, 14);
            im.setBackground(ph);
            com.bumptech.glide.Glide.with(this).load(imgUrl).placeholder(ph).error(ph).into(im);
            card.addView(im, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        TextView n = text(name, 18, DARK, true);
        n.setPadding(0, dp(10), 0, dp(4));
        card.addView(n);

        if (!price.isEmpty()) {
            String pf = price.startsWith("₦") ? price : "₦" + price;
            TextView p = text(pf, 16, PRIMARY, true);
            p.setPadding(0, 0, 0, dp(8));
            card.addView(p);
        }

        if (!desc.isEmpty()) {
            TextView d = text(desc, 13, MUTED, false);
            d.setPadding(0, 0, 0, dp(8));
            card.addView(d);
        }

        if (!contact.isEmpty()) {
            TextView c = text("Contact / Tuntubi: " + contact, 13, DARK, true);
            c.setPadding(0, 0, 0, dp(8));
            card.addView(c);
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Marketplace Product")
                .setView(card)
                .setPositiveButton("Open Marketplace / Duba Kasuwa", (dialog, which) -> startActivity(new Intent(this, ToolsActivity.class)))
                .setNegativeButton("Close / Rufe", null)
                .show();
    }

    // TASK 1: FIX "NO ARTICLES FOUND" ROOT CAUSE & NETWORK ERROR HANDLING
    private boolean postsError = false;
    private String postsErrorMessage = "";

    private void loadPosts() {
        postsLoading = true;
        postsError = false;
        postsErrorMessage = "";
        showHome();

        Api.get("posts.php", new Api.Callback() {
            public void ok(String body) {
                runOnUiThread(() -> {
                    postsLoading = false;
                    try {
                        String clean = body == null ? "" : body.trim();
                        int firstBrace = clean.indexOf('{');
                        int lastBrace = clean.lastIndexOf('}');
                        int firstBracket = clean.indexOf('[');
                        int lastBracket = clean.lastIndexOf(']');

                        JSONArray arr = null;

                        if (firstBracket >= 0 && (firstBrace < 0 || firstBracket < firstBrace)) {
                            if (lastBracket > firstBracket) {
                                String jsonArrayStr = clean.substring(firstBracket, lastBracket + 1);
                                arr = new JSONArray(jsonArrayStr);
                            }
                        } else if (firstBrace >= 0 && lastBrace > firstBrace) {
                            String jsonObjectStr = clean.substring(firstBrace, lastBrace + 1);
                            JSONObject r = new JSONObject(jsonObjectStr);
                            arr = r.optJSONArray("posts");
                            if (arr == null) arr = r.optJSONArray("data");
                            if (arr == null) arr = r.optJSONArray("articles");
                            if (arr == null) arr = r.optJSONArray("rows");
                        }

                        allPosts.clear();
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                allPosts.add(arr.getJSONObject(i));
                            }
                            postsLoaded = true;
                            postsError = false;
                        } else {
                            postsLoaded = true;
                            postsError = false;
                        }
                        showHome();
                    } catch (Exception e) {
                        postsLoaded = false;
                        postsError = true;
                        String detail = e.getMessage() == null ? "JSON parse error" : e.getMessage();
                        postsErrorMessage = "An samu kuskure wajen karanta bayanan sabar: " + detail;
                        showHome();
                    }
                });
            }

            public void err(Exception e) {
                runOnUiThread(() -> {
                    postsLoading = false;
                    postsLoaded = false;
                    postsError = true;
                    String detail = e == null || e.getMessage() == null ? "Check your internet connection." : e.getMessage();
                    postsErrorMessage = "An samu matsalar haɗawa da sabar: " + detail;
                    showHome();
                });
            }
        });
    }

    private void loadDailyTip() {
        Api.get("daily_tip.php", new Api.Callback() {
            public void ok(String body) {
                runOnUiThread(() -> {
                    try {
                        JSONObject r = new JSONObject(body);
                        JSONObject tip = r.optJSONObject("tip");
                        if (tip != null) {
                            dailyTipTitle = tip.optString("title", "Daily Health Tip");
                            dailyTipText = tip.optString("content", "Drink enough water daily to keep your body healthy and your skin glowing.");
                            dailyTipLink = tip.optString("link_url", "");
                        }
                        showHome();
                    } catch (Exception ignored) {
                    }
                });
            }

            public void err(Exception e) {
            }
        });
    }

    private void showHome() {
        pageTitle.setText("Lafiyar Mace");
        pageSubtitle.setText("Promoting Women's Health Through Education and Awareness");
        content.removeAllViews();

        // Category shortcuts
        LinearLayout catScrollRow = new LinearLayout(this);
        catScrollRow.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView catScroll = new HorizontalScrollView(this);
        catScroll.setHorizontalScrollBarEnabled(false);
        catScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinkedHashSet<String> cats = new LinkedHashSet<>();
        for (JSONObject o : allPosts) {
            String c = o.optString("category", "General").trim();
            if (!c.isEmpty()) cats.add(c);
        }
        String[] icons = {"♥", "♀", "✿", "●", "⚕", "•••"};
        int idx = 0;
        for (String cat : cats) {
            if (idx >= 5) break;
            final String selected = cat;
            boolean isSelected = activeCategory.equalsIgnoreCase(selected);

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(10), dp(7), dp(10), dp(6));
            chip.setBackground(isSelected ? bg(PRIMARY, 17) : bg(0xFFFFF0F7, 17));

            TextView ci = text(icons[idx % icons.length], 20, isSelected ? Color.WHITE : PRIMARY, true);
            ci.setGravity(Gravity.CENTER);
            TextView ct = text(cat, 10, isSelected ? Color.WHITE : DARK, true);
            ct.setGravity(Gravity.CENTER);
            ct.setMaxLines(2);

            chip.addView(ci, new LinearLayout.LayoutParams(dp(62), dp(28)));
            chip.addView(ct, new LinearLayout.LayoutParams(dp(74), dp(30)));

            chip.setOnClickListener(v -> {
                activeCategory = selected;
                showHome();
            });
            animateTouch(chip);

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(88), dp(74));
            cp.setMargins(0, 0, dp(8), 0);
            catScrollRow.addView(chip, cp);
            idx++;
        }

        LinearLayout more = new LinearLayout(this);
        more.setOrientation(LinearLayout.VERTICAL);
        more.setGravity(Gravity.CENTER);
        more.setPadding(dp(10), dp(7), dp(10), dp(6));
        more.setBackground(bg(0xFFFFF0F7, 17));

        TextView mi = text("•••", 20, PRIMARY, true);
        mi.setGravity(Gravity.CENTER);
        TextView mt = text("More\nCategories", 10, DARK, true);
        mt.setGravity(Gravity.CENTER);

        more.addView(mi, new LinearLayout.LayoutParams(dp(62), dp(28)));
        more.addView(mt, new LinearLayout.LayoutParams(dp(74), dp(30)));
        more.setOnClickListener(v -> showCategories());
        animateTouch(more);

        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(88), dp(74));
        catScrollRow.addView(more, mp);
        catScroll.addView(catScrollRow, new HorizontalScrollView.LayoutParams(-2, dp(78)));
        content.addView(catScroll);

        showSlider();

        // TASK 4: Daily health tip card
        LinearLayout tip = new LinearLayout(this);
        tip.setGravity(Gravity.CENTER_VERTICAL);
        tip.setPadding(dp(14), dp(12), dp(12), dp(12));
        tip.setBackground(strokeBg(0xFFFFEAF3, BORDER, 1, 18));

        TextView bulb = text("💡", 24, PRIMARY, true);
        bulb.setGravity(Gravity.CENTER);
        tip.addView(bulb, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout tipCopy = new LinearLayout(this);
        tipCopy.setOrientation(LinearLayout.VERTICAL);
        TextView tipTitle = text(dailyTipTitle, 14, PRIMARY_DARK, true);
        TextView tipText = text(dailyTipText, 12, MUTED, false);
        tipText.setMaxLines(3);
        tipText.setLineSpacing(dp(2), 1.0f);

        tipCopy.addView(tipTitle);
        tipCopy.addView(tipText);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, -2, 1);
        tcp.setMargins(dp(8), 0, dp(6), 0);
        tip.addView(tipCopy, tcp);

        TextView see = text("Duba Karin Bayani", 11, Color.WHITE, true);
        see.setGravity(Gravity.CENTER);
        see.setPadding(dp(10), dp(8), dp(10), dp(8));
        see.setBackground(gradientBg(PRIMARY_DARK, PRIMARY, 18));
        see.setOnClickListener(v -> {
            if (!dailyTipLink.isEmpty()) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(dailyTipLink)));
                } catch (Exception ignored) {
                }
            } else {
                startActivity(new Intent(this, ToolsActivity.class));
            }
        });
        animateTouch(see);

        tip.addView(see, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, dp(10), 0, dp(14));
        content.addView(tip, tp);

        // TASK 1: Marketplace Product Carousel section on Home Page
        showMarketplaceCarousel();

        if (settingsLoaded) ads.showHomeBanner(content);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = sectionTitle(activeCategory.equals("All") ? "Bayanan Lafiya na Karshe" : activeCategory);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView all = text("View All  →", 12, PRIMARY, true);
        all.setGravity(Gravity.CENTER_VERTICAL);
        all.setOnClickListener(v -> {
            activeCategory = "All";
            currentQuery = "";
            search.setText("");
            showHome();
        });
        heading.addView(all, new LinearLayout.LayoutParams(dp(92), dp(45)));
        content.addView(heading);

        // TASK 1 & 10: Handling Loading, Empty, Error, and Success States Gracefully
        if (postsLoading && !postsLoaded) {
            LinearLayout loadingBox = new LinearLayout(this);
            loadingBox.setOrientation(LinearLayout.VERTICAL);
            loadingBox.setGravity(Gravity.CENTER);
            loadingBox.setPadding(dp(16), dp(32), dp(16), dp(32));

            ProgressBar pb = new ProgressBar(this);
            loadingBox.addView(pb, new LinearLayout.LayoutParams(dp(36), dp(36)));

            TextView loadTv = text("Ana lodi bayanan lafiya...", 13, MUTED, true);
            loadTv.setPadding(0, dp(10), 0, 0);
            loadingBox.addView(loadTv);

            content.addView(loadingBox);
            return;
        }

        if (postsError && !postsLoaded) {
            LinearLayout errorBox = new LinearLayout(this);
            errorBox.setOrientation(LinearLayout.VERTICAL);
            errorBox.setGravity(Gravity.CENTER);
            errorBox.setPadding(dp(16), dp(24), dp(16), dp(24));
            errorBox.setBackground(strokeBg(Color.WHITE, BORDER, 1, 20));

            TextView iconTv = text("⚠️", 32, PRIMARY, false);
            errorBox.addView(iconTv);

            TextView e = text("An Samu Matsalar Lodi / Network Error", 14, DARK, true);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(8), 0, dp(4));
            errorBox.addView(e);

            TextView subE = text(postsErrorMessage.isEmpty() ? "Da fatan za a duba haɗin intanet dinki sannan ki sake gwadawa." : postsErrorMessage, 12, MUTED, false);
            subE.setGravity(Gravity.CENTER);
            subE.setPadding(0, 0, 0, dp(12));
            errorBox.addView(subE);

            TextView retryBtn = text("↻ Gwada Sake Lodawa", 12, Color.WHITE, true);
            retryBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
            retryBtn.setBackground(gradientBg(PRIMARY_DARK, PRIMARY, 16));
            retryBtn.setOnClickListener(v -> {
                activeCategory = "All";
                currentQuery = "";
                search.setText("");
                loadPosts();
            });
            animateTouch(retryBtn);
            errorBox.addView(retryBtn, new LinearLayout.LayoutParams(-2, -2));

            LinearLayout.LayoutParams ebLp = new LinearLayout.LayoutParams(-1, -2);
            ebLp.setMargins(0, dp(10), 0, dp(16));
            content.addView(errorBox, ebLp);
            return;
        }

        // Latest Health Information Posts Rendering
        LinearLayout firstRow = null;
        int shown = 0;
        int homeNativeShown = 0;

        for (JSONObject o : allPosts) {
            if (!matches(o)) continue;

            if (shown < 2) {
                if (shown == 0) {
                    firstRow = new LinearLayout(this);
                    firstRow.setOrientation(LinearLayout.HORIZONTAL);
                    firstRow.setGravity(Gravity.TOP);

                    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rp.setMargins(0, 0, 0, dp(10));
                    content.addView(firstRow, rp);
                }
                addHomePostCard(firstRow, o);
            } else {
                addHomePostCardSingle(o);
            }

            shown++;

            if (shown > 0 && shown % 5 == 0 && homeNativeShown < 3 && settingsLoaded) {
                ads.showHomeNative(content);
                homeNativeShown++;
            }
        }

        // Empty State: Displayed ONLY when posts loading is complete AND no posts match filter
        if (shown == 0 && postsLoaded) {
            LinearLayout emptyBox = new LinearLayout(this);
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(dp(16), dp(24), dp(16), dp(24));
            emptyBox.setBackground(strokeBg(Color.WHITE, BORDER, 1, 20));

            TextView iconTv = text("📖", 32, PRIMARY, false);
            emptyBox.addView(iconTv);

            TextView e = text("Ba a samu bayanan lafiya ba a yanzu.", 14, DARK, true);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(8), 0, dp(4));
            emptyBox.addView(e);

            TextView subE = text("Gwada bincika wani taken lafiya ko danna sake lodawa.", 12, MUTED, false);
            subE.setGravity(Gravity.CENTER);
            subE.setPadding(0, 0, 0, dp(12));
            emptyBox.addView(subE);

            TextView retryBtn = text("↻ Gwada Sake Lodawa", 12, Color.WHITE, true);
            retryBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
            retryBtn.setBackground(gradientBg(PRIMARY_DARK, PRIMARY, 16));
            retryBtn.setOnClickListener(v -> {
                activeCategory = "All";
                currentQuery = "";
                search.setText("");
                loadPosts();
            });
            animateTouch(retryBtn);
            emptyBox.addView(retryBtn, new LinearLayout.LayoutParams(-2, -2));

            LinearLayout.LayoutParams ebLp = new LinearLayout.LayoutParams(-1, -2);
            ebLp.setMargins(0, dp(10), 0, dp(16));
            content.addView(emptyBox, ebLp);
        }
    }

    private boolean matches(JSONObject o) {
        String cat = o.optString("category", "General");
        if (!activeCategory.equals("All") && !cat.equalsIgnoreCase(activeCategory)) return false;
        if (currentQuery.isEmpty()) return true;
        String hay = (o.optString("title", "") + " " + o.optString("excerpt", "") + " " + o.optString("content", "") + " " + cat).toLowerCase(Locale.US);
        return hay.contains(currentQuery);
    }

    private void addHomePostCard(LinearLayout row, JSONObject o) {
        final String title = o.optString("title", "Health article");
        final String category = o.optString("category", "General");
        final String excerpt = o.optString("excerpt", "");
        final String image = Api.absoluteImageUrl(o.optString("image_url", o.optString("image", "")));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(6), dp(6), dp(6), dp(8));
        card.setBackground(strokeBg(Color.WHITE, BORDER, 1, 18));
        card.setElevation(dp(1));

        ImageView img = new ImageView(this);
        img.setAdjustViewBounds(true);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setMaxHeight(dp(150));
        GradientDrawable ph = bg(0xFFF3EAF0, 14);
        img.setBackground(ph);

        if (!image.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(image).placeholder(ph).error(ph).into(img);
        }
        card.addView(img, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView cat = text(category, 10, PRIMARY, true);
        cat.setPadding(dp(4), dp(7), dp(4), 0);
        cat.setMaxLines(1);
        card.addView(cat);

        TextView tv = text(title, 14, DARK, true);
        tv.setMaxLines(3);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setPadding(dp(4), dp(3), dp(4), 0);
        card.addView(tv);

        TextView desc = text(excerpt, 11, MUTED, false);
        desc.setMaxLines(2);
        desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        desc.setPadding(dp(4), dp(4), dp(4), dp(3));
        card.addView(desc);

        TextView dt = text(o.optString("display_date", o.optString("created_at", "")), 10, MUTED, false);
        dt.setPadding(dp(4), 0, dp(4), dp(2));
        card.addView(dt);

        TextView read = text("Karanta  ›", 11, PRIMARY, true);
        read.setPadding(dp(4), 0, dp(4), 0);
        card.addView(read, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        cp.setMargins(0, 0, dp(6), 0);
        row.addView(card, cp);

        card.setOnClickListener(v -> openArticle(o));
        animateTouch(card);
    }

    private void addHomePostCardSingle(JSONObject o) {
        final String title = o.optString("title", "Health article");
        final String category = o.optString("category", "General");
        final String excerpt = o.optString("excerpt", "");
        final String image = Api.absoluteImageUrl(o.optString("image_url", o.optString("image", "")));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(10));
        card.setBackground(strokeBg(Color.WHITE, BORDER, 1, 18));
        card.setElevation(dp(1));

        ImageView img = new ImageView(this);
        img.setAdjustViewBounds(true);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setMaxHeight(dp(450));
        GradientDrawable ph = bg(0xFFF3EAF0, 14);
        img.setBackground(ph);

        if (!image.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(image).placeholder(ph).error(ph).into(img);
        }

        card.addView(img, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView cat = text(category, 11, PRIMARY, true);
        cat.setPadding(dp(5), dp(9), dp(5), 0);
        cat.setMaxLines(1);
        card.addView(cat);

        TextView tv = text(title, 17, DARK, true);
        tv.setMaxLines(3);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setPadding(dp(5), dp(4), dp(5), 0);
        card.addView(tv);

        TextView desc = text(excerpt, 13, MUTED, false);
        desc.setMaxLines(3);
        desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        desc.setPadding(dp(5), dp(6), dp(5), dp(4));
        card.addView(desc);

        TextView dt = text(o.optString("display_date", o.optString("created_at", "")), 11, MUTED, false);
        dt.setPadding(dp(5), 0, dp(5), dp(3));
        card.addView(dt);

        TextView read = text("Karanta labarin  ›", 12, PRIMARY, true);
        read.setGravity(Gravity.CENTER_VERTICAL);
        read.setPadding(dp(5), 0, dp(5), 0);
        card.addView(read, new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(12));
        content.addView(card, cp);

        card.setOnClickListener(v -> openArticle(o));
        animateTouch(card);
    }

    private void addPostCard(JSONObject o) {
        final int id = o.optInt("id", 0);
        final String title = o.optString("title", "Health article");
        final String category = o.optString("category", "General");
        final String excerpt = o.optString("excerpt", "");
        final String image = Api.absoluteImageUrl(o.optString("image_url", o.optString("image", "")));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(9), dp(9), dp(9), dp(9));
        card.setBackground(strokeBg(Color.WHITE, BORDER, 1, 18));
        card.setElevation(dp(2));

        ImageView img = new ImageView(this);
        img.setAdjustViewBounds(true);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setMaxHeight(dp(450));
        GradientDrawable ph = bg(0xFFF1E7EE, 14);
        img.setBackground(ph);

        if (!image.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(image).placeholder(ph).error(ph).into(img);
        }
        card.addView(img, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView cat = text(category.toUpperCase(Locale.US), 11, PRIMARY, true);
        cat.setPadding(dp(5), dp(10), dp(5), dp(3));
        card.addView(cat);

        TextView tv = text(title, 18, DARK, true);
        tv.setMaxLines(3);
        card.addView(tv);

        TextView desc = text(excerpt, 14, MUTED, false);
        desc.setMaxLines(3);
        desc.setPadding(0, dp(6), 0, dp(4));
        card.addView(desc);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        TextView read = text("Karanta labarin  ›", 13, PRIMARY, true);
        bottom.addView(read, new LinearLayout.LayoutParams(0, dp(36), 1));

        TextView star = text(savedIds.contains(id) ? "★" : "☆", 27, PRIMARY, false);
        star.setGravity(Gravity.CENTER);
        bottom.addView(star, new LinearLayout.LayoutParams(dp(48), dp(42)));
        card.addView(bottom);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(14));
        content.addView(card, cp);

        star.setOnClickListener(v -> {
            toggleSaved(id);
            star.setText(savedIds.contains(id) ? "★" : "☆");
        });
        card.setOnClickListener(v -> openArticle(o));
        animateTouch(card);
    }

    private void showCategories() {
        pageTitle.setText("Categories");
        pageSubtitle.setText("Browse women's health topics");
        content.removeAllViews();
        if (settingsLoaded) ads.showBanner(content);
        content.addView(sectionTitle("Health categories"));

        LinkedHashSet<String> cats = new LinkedHashSet<>();
        cats.add("All");
        for (JSONObject o : allPosts) cats.add(o.optString("category", "General"));

        for (String cat : cats) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), 0, dp(12), 0);
            row.setBackground(strokeBg(Color.WHITE, BORDER, 1, 16));

            TextView name = text(cat, 16, DARK, true);
            row.addView(name, new LinearLayout.LayoutParams(0, dp(58), 1));

            TextView arrow = text("→", 20, PRIMARY, true);
            row.addView(arrow, new LinearLayout.LayoutParams(dp(45), dp(58)));

            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(58));
            rp.setMargins(0, 0, 0, dp(10));
            content.addView(row, rp);

            row.setOnClickListener(v -> {
                activeCategory = cat;
                showHome();
            });
            animateTouch(row);
        }
    }

    private void showSaved() {
        pageTitle.setText("Saved Articles");
        pageSubtitle.setText("Your bookmarked health information");
        content.removeAllViews();
        if (settingsLoaded) ads.showBanner(content);
        content.addView(sectionTitle("Your saved articles"));
        int count = 0;

        for (JSONObject o : allPosts) {
            if (savedIds.contains(o.optInt("id", 0))) {
                addPostCard(o);
                count++;
            }
        }
        if (count == 0) {
            TextView e = text("You have not saved any articles yet. Tap the star to save an article.", 15, MUTED, false);
            e.setPadding(dp(8), dp(25), dp(8), dp(25));
            content.addView(e);
        }
    }

    private void showSidebar(View anchor) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(14));
        panel.setBackgroundColor(Color.WHITE);

        LinearLayout brandCard = new LinearLayout(this);
        brandCard.setOrientation(LinearLayout.VERTICAL);
        brandCard.setGravity(Gravity.CENTER_HORIZONTAL);
        brandCard.setPadding(dp(12), dp(18), dp(12), dp(18));
        brandCard.setBackground(gradientBg(PRIMARY_DARK, PRIMARY, 18));

        ImageView drawerLogo = new ImageView(this);
        drawerLogo.setImageResource(R.drawable.lafiyar_mace_logo);
        drawerLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        drawerLogo.setContentDescription("Lafiyar Mace logo");
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(58), dp(58));
        logoLp.bottomMargin = dp(8);
        brandCard.addView(drawerLogo, logoLp);

        TextView heading = text("Lafiyar Mace", 21, Color.WHITE, true);
        heading.setGravity(Gravity.CENTER);
        brandCard.addView(heading);

        TextView slogan = text("Promoting Women's Health Through Education and Awareness", 12, Color.WHITE, false);
        slogan.setGravity(Gravity.CENTER);
        slogan.setLineSpacing(dp(2), 1.0f);
        slogan.setPadding(dp(4), dp(6), dp(4), 0);
        brandCard.addView(slogan);
        panel.addView(brandCard, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView section = text("MENU", 11, MUTED, true);
        section.setPadding(dp(10), dp(18), dp(8), dp(8));
        panel.addView(section);

        addDrawerItem(panel, "⌂   Home", () -> showHome());
        addDrawerItem(panel, "🤖   Tambayi AI", () -> startActivity(new Intent(this, AiActivity.class)));
        addDrawerItem(panel, "▦   Categories", () -> showCategories());
        addDrawerItem(panel, "♡   Saved articles", () -> showSaved());
        addDrawerItem(panel, "✚   Health tools", () -> startActivity(new Intent(this, ToolsActivity.class)));
        addDrawerItem(panel, "◉   Profile", () -> openProfile());
        addDrawerItem(panel, "⚙   Settings", () -> showSettings());

        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
        dividerLp.setMargins(dp(4), dp(10), dp(4), dp(10));
        panel.addView(divider, dividerLp);
        addDrawerItem(panel, "⇥   Log out", () -> logoutUser());

        TextView footer = text("Women's health education and awareness.", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8), dp(16), dp(8), dp(4));
        panel.addView(footer);

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.addView(panel);

        FrameLayout fullScreenLayer = new FrameLayout(this);
        fullScreenLayer.setBackgroundColor(Color.WHITE);
        fullScreenLayer.setClickable(true);
        fullScreenLayer.setFocusable(true);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START | Gravity.TOP);
        fullScreenLayer.addView(scroller, panelParams);

        final PopupWindow popup = new PopupWindow(fullScreenLayer, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popup.setElevation(dp(18));
        popup.setOutsideTouchable(false);
        popup.setClippingEnabled(true);
        fullScreenLayer.setOnClickListener(v -> popup.dismiss());
        scroller.setOnClickListener(v -> {
        });

        for (int i = 0; i < panel.getChildCount(); i++) {
            View child = panel.getChildAt(i);
            if (child.getTag() instanceof Runnable) {
                Runnable action = (Runnable) child.getTag();
                child.setOnClickListener(v -> {
                    popup.dismiss();
                    action.run();
                });
            }
        }
        popup.showAtLocation(findViewById(android.R.id.content), Gravity.START | Gravity.TOP, 0, 0);
    }

    private void addDrawerItem(LinearLayout panel, String label, Runnable action) {
        TextView item = text(label, 15, DARK, true);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), 0, dp(10), 0);
        item.setBackground(bg(Color.WHITE, 12));
        item.setTag(action);
        item.setMinHeight(dp(48));
        item.setContentDescription(label.replace("   ", " "));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.setMargins(0, dp(2), 0, dp(2));
        panel.addView(item, lp);
    }

    private void openProfile() {
        String token = prefs.getString("auth_token", "");
        if (token.isEmpty()) {
            startActivity(new Intent(this, AuthActivity.class));
            return;
        }
        String name = prefs.getString("user_name", "User");
        String email = prefs.getString("user_email", "");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        TextView avatar = text("👤", 38, PRIMARY, true);
        avatar.setGravity(Gravity.CENTER);
        card.addView(avatar, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView n = text(name, 20, DARK, true);
        n.setGravity(Gravity.CENTER);
        card.addView(n);
        TextView e = text(email, 14, MUTED, false);
        e.setGravity(Gravity.CENTER);
        e.setPadding(0, dp(5), 0, dp(8));
        card.addView(e);
        new android.app.AlertDialog.Builder(this).setTitle("My Profile / Bayanan Asusu").setView(card).setPositiveButton("Close", null).show();
    }

    private void logoutUser() {
        String token = prefs.getString("auth_token", "");
        if (token.isEmpty()) {
            startActivity(new Intent(this, AuthActivity.class));
            return;
        }
        new android.app.AlertDialog.Builder(this).setTitle("Log out?").setMessage("Do you want to log out of Lafiyar Mace?").setNegativeButton("Cancel", null).setPositiveButton("Log out", (d, w) -> {
            java.util.Map<String, String> m = new java.util.HashMap<>();
            m.put("action", "logout");
            Api.post("auth.php", m, token, new Api.Callback() {
                public void ok(String b) {
                }

                public void err(Exception e) {
                }
            });
            prefs.edit().remove("auth_token").remove("user_id").remove("user_name").remove("user_email").apply();
            startActivity(new Intent(this, AuthActivity.class));
        }).show();
    }

    private void showSettings() {
        pageTitle.setText("Settings");
        pageSubtitle.setText("App preferences and information");
        content.removeAllViews();
        if (settingsLoaded) ads.showBanner(content);
        content.addView(sectionTitle("App Settings"));
        addSetting("Language", "English", false, null);
        addSetting("Saved articles", savedIds.size() + " saved", false, null);
        addSetting("Refresh content", "Get the latest articles from the server", true, v -> {
            loadAdSettings();
            loadPosts();
            loadMarketplaceProducts();
        });
        addSetting("Share the app", "Share Lafiyar Mace with friends and family", true, v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "Lafiyar Mace — Women's Health Information");
            startActivity(Intent.createChooser(i, "Share Lafiyar Mace"));
        });
        addSetting("About Lafiyar Mace", "Educational health information for women. This app does not replace advice from a qualified healthcare professional.", false, null);
        addSetting("Version", "1.0", false, null);
    }

    private void addSetting(String title, String subtitle, boolean clickable, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackground(strokeBg(Color.WHITE, BORDER, 1, 16));

        TextView t = text(title, 16, DARK, true);
        TextView s = text(subtitle, 13, MUTED, false);
        s.setPadding(0, dp(4), 0, 0);

        row.addView(t);
        row.addView(s);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dp(10));
        content.addView(row, p);
        if (clickable && listener != null) {
            row.setOnClickListener(listener);
            animateTouch(row);
        }
    }

    private void openArticle(JSONObject o) {
        if (ads != null && ads.isRewardedEnabled()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Continue Reading")
                    .setMessage("Watch a short rewarded ad to support Lafiyar Mace, or continue without an ad.")
                    .setNegativeButton("CONTINUE WITHOUT AD", (dialog, which) -> openArticleNow(o, true))
                    .setPositiveButton("WATCH AD", (dialog, which) ->
                            ads.showRewardedWhenReady(
                                    reward -> openArticleNow(o, false),
                                    () -> openArticleNow(o, true)
                            ))
                    .setOnCancelListener(dialog -> openArticleNow(o, true))
                    .show();
            return;
        }
        openArticleNow(o, true);
    }

    private void openArticleNow(JSONObject o) {
        openArticleNow(o, true);
    }

    private void openArticleNow(JSONObject o, boolean countForInterstitial) {
        Intent i = new Intent(this, PostActivity.class);
        i.putExtra("id", o.optInt("id", 0));
        i.putExtra("title", o.optString("title", "Health article"));
        i.putExtra("content", o.optString("content", ""));
        i.putExtra("category", o.optString("category", "General"));
        i.putExtra("image", Api.absoluteImageUrl(o.optString("image_url", o.optString("image", ""))));
        i.putExtra("date", o.optString("display_date", o.optString("created_at", o.optString("date", ""))));
        if (countForInterstitial) ads.onPostOpened();
        startActivity(i);
    }

    private void loadSaved() {
        String raw = prefs.getString("saved", "");
        if (raw.isEmpty()) return;
        for (String s : raw.split(","))
            try {
                savedIds.add(Integer.parseInt(s));
            } catch (Exception ignored) {
            }
    }

    private void toggleSaved(int id) {
        if (savedIds.contains(id)) savedIds.remove(id);
        else savedIds.add(id);
        StringBuilder b = new StringBuilder();
        for (Integer x : savedIds) {
            if (b.length() > 0) b.append(',');
            b.append(x);
        }
        prefs.edit().putString("saved", b.toString()).apply();
    }
}
