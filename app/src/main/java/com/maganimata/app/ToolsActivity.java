package com.maganimata.app;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class ToolsActivity extends Activity {
    // Theme Palette from Reference Image
    private final int COLOR_PRIMARY_PINK = Color.parseColor("#E91E75");  // #E91E75 Pink
    private final int COLOR_DEEP_PLUM = Color.parseColor("#8E2458");     // #8E2458 Deep Plum
    private final int COLOR_LIGHT_PINK = Color.parseColor("#FFF0F6");    // #FFF0F6 Light Pink
    private final int COLOR_HERO_BG = Color.parseColor("#FFE5F0");      // #FFE5F0 Hero Pink
    private final int COLOR_TEXT_MAIN = Color.parseColor("#30202C");     // #30202C Dark Main Text
    private final int COLOR_TEXT_MUTED = Color.parseColor("#786A74");    // #786A74 Muted Text
    private final int COLOR_BORDER = Color.parseColor("#F5E0EB");        // Soft border

    // Prediction Card Colors
    private final int COLOR_FERTILE_BG = Color.parseColor("#E3F7E9");    // Soft Green
    private final int COLOR_FERTILE_TEXT = Color.parseColor("#1B5E20");
    private final int COLOR_OVU_BG = Color.parseColor("#EEE8FF");        // Soft Lavender
    private final int COLOR_OVU_TEXT = Color.parseColor("#4A148C");
    private final int COLOR_PHASE_BG = Color.parseColor("#FFF5D6");      // Soft Yellow
    private final int COLOR_PHASE_TEXT = Color.parseColor("#E65100");

    private LinearLayout root;
    private String token = "";
    private SharedPreferences trackerStore;
    private final List<Animator> activeAnimators = new ArrayList<>();
    private int activeTab = 0; // 0=Period, 1=Ovulation, 2=Pregnancy, 3=Baby Growth, 4=Others
    private Calendar currentCalMonth = Calendar.getInstance();
    private Date selectedCalendarDate = new Date();

    private int dp(float x) {
        return (int) (x * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView t(String s, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        return v;
    }

    private GradientDrawable bg(int c, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        return g;
    }

    private GradientDrawable bgGradient(int startColor, int endColor, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{startColor, endColor});
        g.setCornerRadius(dp(r));
        return g;
    }

    private GradientDrawable strokeBg(int fillColor, int strokeColor, int strokeWidthDp, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fillColor);
        g.setStroke(dp(strokeWidthDp), strokeColor);
        g.setCornerRadius(dp(r));
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
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        token = getSharedPreferences("magani_prefs", MODE_PRIVATE).getString("auth_token", "");
        trackerStore = getSharedPreferences("lafiyar_mace_trackers_store", MODE_PRIVATE);
        build();
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    private void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // Section A: Top Navigation Header
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(10), dp(16), dp(10));
        top.setBackgroundColor(Color.WHITE);

        TextView back = t("←", 26, COLOR_PRIMARY_PINK, true);
        back.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));
        back.setOnClickListener(v -> finish());
        animateTouch(back);

        TextView title = t("Lafiyar Mace", 22, COLOR_DEEP_PLUM, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.lafiyar_mace_logo);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        avatar.setPadding(dp(2), dp(2), dp(2), dp(2));
        avatar.setBackground(strokeBg(COLOR_LIGHT_PINK, COLOR_PRIMARY_PINK, 1, 20));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));

        root.addView(top);

        ScrollView mainScroller = new ScrollView(this);
        mainScroller.setFillViewport(true);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(10), dp(14), dp(28));
        mainScroller.addView(c);
        root.addView(mainScroller, new LinearLayout.LayoutParams(-1, 0, 1));

        // Section B: Tracker Navigation Tabs
        c.addView(createTrackerTabs());

        // Dynamic Dashboard Body Based on Active Tab
        if (activeTab == 0) {
            buildPeriodDashboard(c);
        } else if (activeTab == 1) {
            buildOvulationDashboard(c);
        } else if (activeTab == 2) {
            buildPregnancyDashboard(c);
        } else if (activeTab == 3) {
            buildBabyGrowthDashboard(c);
        } else {
            buildOthersDashboard(c);
        }

        setContentView(root);
    }

    // Section B Layout: Tabs
    private View createTrackerTabs() {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dp(4), 0, dp(14));

        String[] tabLabels = {"Period Tracker", "Ovulation Tracker", "Pregnancy Tracker", "Baby Growth", "Others"};
        String[] tabIcons = {"🩸", "🌸", "🤰", "📊", "🔲"};

        for (int i = 0; i < tabLabels.length; i++) {
            final int index = i;
            boolean isActive = (activeTab == index);

            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.HORIZONTAL);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(14), dp(10), dp(14), dp(10));

            if (isActive) {
                tab.setBackground(bg(COLOR_PRIMARY_PINK, 22));
            } else {
                tab.setBackground(strokeBg(COLOR_LIGHT_PINK, COLOR_BORDER, 1, 22));
            }

            TextView iconTv = t(tabIcons[i], 16, isActive ? Color.WHITE : COLOR_PRIMARY_PINK, false);
            TextView textTv = t(tabLabels[i], 13, isActive ? Color.WHITE : COLOR_TEXT_MAIN, true);
            textTv.setPadding(dp(6), 0, 0, 0);

            tab.addView(iconTv);
            tab.addView(textTv);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(42));
            lp.setMargins(0, 0, dp(8), 0);
            container.addView(tab, lp);

            tab.setOnClickListener(v -> {
                activeTab = index;
                build();
            });
            animateTouch(tab);
        }
        hsv.addView(container);
        return hsv;
    }

    // --- TAB 0: PERIOD TRACKER DASHBOARD ---
    private void buildPeriodDashboard(LinearLayout container) {
        String savedStart = trackerStore.getString("period_start", today());
        int savedCycle = trackerStore.getInt("cycle_length", 28);
        int savedDuration = trackerStore.getInt("period_duration", 5);
        boolean reminderOn = trackerStore.getBoolean("period_reminder", true);

        Date startDate;
        try {
            startDate = parse(savedStart);
        } catch (Exception e) {
            startDate = new Date();
        }

        Date nextPeriod = addDays(startDate, savedCycle);
        Date ovuDate = addDays(nextPeriod, -14);
        Date fertileStart = addDays(ovuDate, -5);
        Date fertileEnd = addDays(ovuDate, 1);

        int daysToNext = daysBetween(new Date(), nextPeriod);
        int daysToOvu = daysBetween(new Date(), ovuDate);
        int currentCycleDay = Math.max(1, Math.min(savedCycle, daysBetween(startDate, new Date()) + 1));

        // Section C: Hero Card
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(14), dp(18));
        hero.setBackground(strokeBg(COLOR_HERO_BG, COLOR_BORDER, 1, 24));
        hero.setElevation(dp(2));

        LinearLayout heroLeft = new LinearLayout(this);
        heroLeft.setOrientation(LinearLayout.VERTICAL);

        TextView heroTitle = t("Period Tracker", 22, COLOR_DEEP_PLUM, true);
        TextView heroDesc = t("Bi diddigin al'adarki, gano lokacin al'ada ta gaba, ovulation da fertile window. Kula da lafiyarki kullum.", 12, COLOR_TEXT_MUTED, false);
        heroDesc.setLineSpacing(dp(3), 1.0f);
        heroDesc.setPadding(0, dp(6), 0, dp(12));

        TextView howBtn = t("💡 Yadda yake aiki?  ›", 13, COLOR_PRIMARY_PINK, true);
        howBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        howBtn.setBackground(bg(Color.WHITE, 16));
        howBtn.setOnClickListener(v -> showManual());
        animateTouch(howBtn);

        heroLeft.addView(heroTitle);
        heroLeft.addView(heroDesc);
        heroLeft.addView(howBtn, new LinearLayout.LayoutParams(-2, -2));

        hero.addView(heroLeft, new LinearLayout.LayoutParams(0, -2, 1.2f));

        ImageView heroImg = new ImageView(this);
        heroImg.setImageResource(R.drawable.header_woman);
        heroImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        heroImg.setPadding(dp(4), dp(4), dp(4), dp(4));
        heroImg.setBackground(bg(Color.WHITE, 20));

        hero.addView(heroImg, new LinearLayout.LayoutParams(dp(90), dp(110)));
        addBreathingAnimation(heroImg);

        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
        heroLp.setMargins(0, 0, 0, dp(14));
        container.addView(hero, heroLp);

        // Section D: Quick Information Cards (2x2 Grid)
        LinearLayout qGrid = new LinearLayout(this);
        qGrid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        row1.addView(createQuickCard("📅", "Ranar al'ada ta baya", fmtShort(startDate), true, v -> editPeriodStartDialog()), new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(createQuickCard("🕒", "Tsawon cycle", savedCycle + " Kwana", true, v -> editCycleDialog()), new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        row2.addView(createQuickCard("🩸", "Tsawon kwanakin al'ada", savedDuration + " Kwana", true, v -> editDurationDialog()), new LinearLayout.LayoutParams(0, -2, 1));

        // Reminder Switch Card
        LinearLayout remCard = new LinearLayout(this);
        remCard.setOrientation(LinearLayout.VERTICAL);
        remCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        remCard.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 18));

        LinearLayout remTop = new LinearLayout(this);
        remTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView rIcon = t("⚙️", 18, COLOR_PRIMARY_PINK, false);
        TextView rTitle = t("Saita Reminder", 12, COLOR_TEXT_MUTED, true);
        rTitle.setPadding(dp(6), 0, 0, 0);
        remTop.addView(rIcon);
        remTop.addView(rTitle);
        remCard.addView(remTop);

        Switch sw = new Switch(this);
        sw.setChecked(reminderOn);
        sw.setOnCheckedChangeListener((cb, checked) -> {
            trackerStore.edit().putBoolean("period_reminder", checked).apply();
            toast(checked ? "An kunna sanarwar reminder." : "An kashe sanarwar reminder.");
        });
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(-2, -2);
        swLp.setMargins(0, dp(4), 0, 0);
        remCard.addView(sw, swLp);

        LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(0, -2, 1);
        rcLp.setMargins(dp(6), 0, 0, 0);
        row2.addView(remCard, rcLp);

        qGrid.addView(row1);
        LinearLayout.LayoutParams r2Lp = new LinearLayout.LayoutParams(-1, -2);
        r2Lp.setMargins(0, dp(8), 0, 0);
        qGrid.addView(row2, r2Lp);

        LinearLayout.LayoutParams qgLp = new LinearLayout.LayoutParams(-1, -2);
        qgLp.setMargins(0, 0, 0, dp(14));
        container.addView(qGrid, qgLp);

        // Section E: Prediction Cards (4 Color-coded Cards)
        LinearLayout predGrid = new LinearLayout(this);
        predGrid.setOrientation(LinearLayout.HORIZONTAL);

        predGrid.addView(createPredCard("Ranar al'ada ta gaba", fmtShort(nextPeriod), daysToNext <= 0 ? "Yau ko gobe" : "Rana " + daysToNext + " daga yanzu", COLOR_LIGHT_PINK, COLOR_PRIMARY_PINK), new LinearLayout.LayoutParams(0, -2, 1));
        predGrid.addView(createPredCard("Lokacin Ovulation", fmtShort(ovuDate), daysToOvu <= 0 ? "A halin yanzu" : "Rana " + daysToOvu + " daga yanzu", COLOR_OVU_BG, COLOR_OVU_TEXT), new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout predGrid2 = new LinearLayout(this);
        predGrid2.setOrientation(LinearLayout.HORIZONTAL);

        predGrid2.addView(createPredCard("Fertile Window", fmtShort(fertileStart) + " – " + fmtShort(fertileEnd), "Mafi yiwuwa ɗaukar ciki", COLOR_FERTILE_BG, COLOR_FERTILE_TEXT), new LinearLayout.LayoutParams(0, -2, 1));

        String phaseName = currentCycleDay <= savedDuration ? "Period Phase" : (currentCycleDay <= (savedCycle - 14) ? "Follicular Phase" : "Luteal Phase");
        predGrid2.addView(createPredCard("Yanzu kina cikin", phaseName, "Rana ta " + currentCycleDay + " daga " + savedCycle, COLOR_PHASE_BG, COLOR_PHASE_TEXT), new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout predBox = new LinearLayout(this);
        predBox.setOrientation(LinearLayout.VERTICAL);
        predBox.addView(predGrid);

        LinearLayout.LayoutParams pg2Lp = new LinearLayout.LayoutParams(-1, -2);
        pg2Lp.setMargins(0, dp(8), 0, 0);
        predBox.addView(predGrid2, pg2Lp);

        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(-1, -2);
        pbLp.setMargins(0, 0, 0, dp(14));
        container.addView(predBox, pbLp);

        // Section F: Calendar Section
        container.addView(createCalendarView(startDate, ovuDate, fertileStart, fertileEnd, savedDuration));

        // Section G: Daily Guidance Card
        container.addView(createDailyGuidanceCard(startDate, savedDuration, ovuDate, fertileStart, fertileEnd));

        // Section H: Main Action Button
        TextView mainActionBtn = t("+ Shigar da Ranar Al'ada", 16, Color.WHITE, true);
        mainActionBtn.setGravity(Gravity.CENTER);
        mainActionBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mainActionBtn.setBackground(bg(COLOR_PRIMARY_PINK, 20));
        mainActionBtn.setElevation(dp(3));
        mainActionBtn.setOnClickListener(v -> periodDialog());
        animateTouch(mainActionBtn);

        LinearLayout.LayoutParams mabLp = new LinearLayout.LayoutParams(-1, -2);
        mabLp.setMargins(0, dp(14), 0, dp(16));
        container.addView(mainActionBtn, mabLp);

        // Section I: Secondary Feature Cards
        LinearLayout secGrid1 = new LinearLayout(this);
        secGrid1.setOrientation(LinearLayout.HORIZONTAL);
        secGrid1.addView(createSecondaryCard("📖", "Neman Tarihi", "Duba cikakken history dinsu", v -> showPeriodHistoryModal()), new LinearLayout.LayoutParams(0, -2, 1));
        secGrid1.addView(createSecondaryCard("📊", "Rahoto & Chart", "Gani da fahimtar pattern", v -> showCycleReportModal()), new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout secGrid2 = new LinearLayout(this);
        secGrid2.setOrientation(LinearLayout.HORIZONTAL);
        secGrid2.addView(createSecondaryCard("🔔", "Saita Reminder", "Karbi sanarwa lokacin da ya kusa", v -> editReminderSettingsModal()), new LinearLayout.LayoutParams(0, -2, 1));
        secGrid2.addView(createSecondaryCard("⚙️", "Saitunan Tracker", "Gyara cycle da lokacin al'ada", v -> periodDialog()), new LinearLayout.LayoutParams(0, -2, 1));

        container.addView(secGrid1);
        LinearLayout.LayoutParams sg2Lp = new LinearLayout.LayoutParams(-1, -2);
        sg2Lp.setMargins(0, dp(8), 0, dp(18));
        container.addView(secGrid2, sg2Lp);

        // Section J: Health Education Articles Section
        container.addView(createHealthEducationSection());
    }

    private View createQuickCard(String icon, String title, String val, boolean editable, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 18));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView iTv = t(icon, 18, COLOR_PRIMARY_PINK, false);
        TextView tTv = t(title, 11, COLOR_TEXT_MUTED, true);
        tTv.setPadding(dp(6), 0, 0, 0);
        top.addView(iTv);
        top.addView(tTv, new LinearLayout.LayoutParams(0, -2, 1));

        if (editable) {
            TextView editIcon = t("✏️", 11, COLOR_PRIMARY_PINK, false);
            top.addView(editIcon);
        }
        card.addView(top);

        TextView valTv = t(val, 15, COLOR_PRIMARY_PINK, true);
        valTv.setPadding(0, dp(6), 0, 0);
        card.addView(valTv);

        if (l != null) {
            card.setOnClickListener(l);
            animateTouch(card);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(lp);
        return card;
    }

    private View createPredCard(String label, String val, String sub, int bgColor, int textColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(bg(bgColor, 18));

        TextView lTv = t(label, 11, textColor, true);
        TextView vTv = t(val, 15, textColor, true);
        vTv.setPadding(0, dp(4), 0, dp(2));

        TextView sTv = t(sub, 10, textColor, false);

        card.addView(lTv);
        card.addView(vTv);
        card.addView(sTv);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(lp);
        return card;
    }

    private View createSecondaryCard(String icon, String title, String sub, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 18));

        TextView iTv = t(icon, 22, COLOR_PRIMARY_PINK, false);
        iTv.setGravity(Gravity.CENTER);
        iTv.setBackground(bg(COLOR_LIGHT_PINK, 14));
        card.addView(iTv, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), 0, dp(4), 0);

        TextView tTv = t(title, 13, COLOR_TEXT_MAIN, true);
        TextView sTv = t(sub, 10, COLOR_TEXT_MUTED, false);
        box.addView(tTv);
        box.addView(sTv);

        card.addView(box, new LinearLayout.LayoutParams(0, -2, 1));

        TextView arr = t("›", 22, COLOR_PRIMARY_PINK, true);
        card.addView(arr);

        card.setOnClickListener(l);
        animateTouch(card);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(lp);
        return card;
    }

    // Section F: Calendar View Component
    private View createCalendarView(Date periodStart, Date ovuDate, Date fertileStart, Date fertileEnd, int periodDuration) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 22));

        // Month Navigation Row
        LinearLayout monthNav = new LinearLayout(this);
        monthNav.setGravity(Gravity.CENTER_VERTICAL);

        TextView prevBtn = t("‹", 24, COLOR_PRIMARY_PINK, true);
        prevBtn.setPadding(dp(8), 0, dp(8), 0);
        prevBtn.setOnClickListener(v -> {
            currentCalMonth.add(Calendar.MONTH, -1);
            build();
        });
        animateTouch(prevBtn);

        TextView monthTitle = t(new SimpleDateFormat("MMMM yyyy", Locale.US).format(currentCalMonth.getTime()), 16, COLOR_TEXT_MAIN, true);
        monthTitle.setGravity(Gravity.CENTER);

        TextView nextBtn = t("›", 24, COLOR_PRIMARY_PINK, true);
        nextBtn.setPadding(dp(8), 0, dp(8), 0);
        nextBtn.setOnClickListener(v -> {
            currentCalMonth.add(Calendar.MONTH, 1);
            build();
        });
        animateTouch(nextBtn);

        monthNav.addView(prevBtn);
        monthNav.addView(monthTitle, new LinearLayout.LayoutParams(0, -2, 1));
        monthNav.addView(nextBtn);

        card.addView(monthNav);

        // Legend Row
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(0, dp(8), 0, dp(10));

        legend.addView(t("● Al'ada  ", 10, COLOR_PRIMARY_PINK, true));
        legend.addView(t("● Ovulation  ", 10, COLOR_OVU_TEXT, true));
        legend.addView(t("● Fertile Window  ", 10, COLOR_FERTILE_TEXT, true));
        legend.addView(t("● Yau", 10, COLOR_TEXT_MAIN, true));

        card.addView(legend);

        // Weekday Headers
        LinearLayout weekHeader = new LinearLayout(this);
        String[] days = {"Lah", "Tal", "Arb", "Alh", "Jum", "Asb", "Lah"};
        for (String d : days) {
            TextView cell = t(d, 11, COLOR_TEXT_MUTED, true);
            cell.setGravity(Gravity.CENTER);
            weekHeader.addView(cell, new LinearLayout.LayoutParams(0, dp(26), 1));
        }
        card.addView(weekHeader);

        // Days Grid
        Calendar cal = (Calendar) currentCalMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int offset = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayNum = 1;

        for (int r = 0; r < 6 && dayNum <= maxDays; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int col = 0; col < 7; col++) {
                TextView cell = t("", 13, COLOR_TEXT_MAIN, false);
                cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(34), 1);

                if ((r > 0 || col >= offset) && dayNum <= maxDays) {
                    final int currentDay = dayNum;
                    Calendar cellCal = (Calendar) cal.clone();
                    cellCal.set(Calendar.DAY_OF_MONTH, currentDay);
                    Date cellDate = cellCal.getTime();
                    String label = String.valueOf(currentDay);

                    boolean isPeriod = daysBetween(periodStart, cellDate) >= 0 && daysBetween(periodStart, cellDate) < periodDuration;
                    boolean isOvu = daysBetween(ovuDate, cellDate) == 0;
                    boolean isFertile = !cellDate.before(fertileStart) && !cellDate.after(fertileEnd);
                    boolean isToday = daysBetween(new Date(), cellDate) == 0;

                    if (isToday) {
                        cell.setText(label);
                        cell.setTextColor(Color.WHITE);
                        cell.setTypeface(null, Typeface.BOLD);
                        cell.setBackground(bg(COLOR_PRIMARY_PINK, 30));
                    } else if (isOvu) {
                        cell.setText(label);
                        cell.setTextColor(COLOR_OVU_TEXT);
                        cell.setTypeface(null, Typeface.BOLD);
                        cell.setBackground(bg(COLOR_OVU_BG, 30));
                    } else if (isPeriod) {
                        cell.setText(label);
                        cell.setTextColor(COLOR_PRIMARY_PINK);
                        cell.setTypeface(null, Typeface.BOLD);
                        cell.setBackground(bg(COLOR_LIGHT_PINK, 30));
                    } else if (isFertile) {
                        cell.setText(label);
                        cell.setTextColor(COLOR_FERTILE_TEXT);
                        cell.setBackground(bg(COLOR_FERTILE_BG, 30));
                    } else {
                        cell.setText(label);
                    }

                    cell.setOnClickListener(v -> {
                        selectedCalendarDate = cellDate;
                        build();
                    });

                    dayNum++;
                }
                row.addView(cell, cp);
            }
            card.addView(row);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);
        return card;
    }

    // Section G: Daily Guidance Card
    private View createDailyGuidanceCard(Date periodStart, int periodDuration, Date ovuDate, Date fertileStart, Date fertileEnd) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(strokeBg(COLOR_LIGHT_PINK, COLOR_BORDER, 1, 20));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView mBadge = t(new SimpleDateFormat("MMM", Locale.US).format(selectedCalendarDate), 11, COLOR_PRIMARY_PINK, true);
        mBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
        mBadge.setBackground(bg(Color.WHITE, 10));

        TextView dayNum = t(new SimpleDateFormat("dd", Locale.US).format(selectedCalendarDate), 20, COLOR_DEEP_PLUM, true);
        dayNum.setPadding(dp(8), 0, dp(10), 0);

        boolean isPeriodDay = daysBetween(periodStart, selectedCalendarDate) >= 0 && daysBetween(periodStart, selectedCalendarDate) < periodDuration;
        boolean isFertile = !selectedCalendarDate.before(fertileStart) && !selectedCalendarDate.after(fertileEnd);

        String titleText = isPeriodDay ? "Rana ta " + (daysBetween(periodStart, selectedCalendarDate) + 1) + " na al'ada" : (isFertile ? "Ranakun Samun Ciki" : "Rana ta Yau");
        String subText = isPeriodDay ? "Jini mai matsakaici • Sha ruwa sosai" : "Mafi yiwuwa daukar ciki";

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.addView(t(titleText, 15, COLOR_TEXT_MAIN, true));
        textGroup.addView(t(subText, 11, COLOR_TEXT_MUTED, false));

        topRow.addView(mBadge);
        topRow.addView(dayNum);
        topRow.addView(textGroup, new LinearLayout.LayoutParams(0, -2, 1));

        card.addView(topRow);

        View div = new View(this);
        div.setBackgroundColor(COLOR_BORDER);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(-1, dp(1));
        dLp.setMargins(0, dp(10), 0, dp(10));
        card.addView(div, dLp);

        card.addView(t("💧 Wannan rana tana cikin kwanakin lura da lafiyarki.", 11, COLOR_TEXT_MUTED, false));
        card.addView(t("❤️ Ka ci abinci mai gina jiki, ka sha ruwa sosai.", 11, COLOR_TEXT_MUTED, false));
        card.addView(t("🌙 Ka huta idan kin ji gajiya ko zafin mara.", 11, COLOR_TEXT_MUTED, false));
        card.addView(t("➕ Idan zafin mara ya yi tsanani, tuntubi ma'aikacin lafiya.", 11, COLOR_PRIMARY_PINK, true));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(lp);
        return card;
    }

    // Section J: Health Education Cards
    private View createHealthEducationSection() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(t("Karin Bayanan da za su iya taimaka", 14, COLOR_DEEP_PLUM, true), new LinearLayout.LayoutParams(0, -2, 1));

        TextView allBtn = t("Duba duk  →", 12, COLOR_PRIMARY_PINK, true);
        allBtn.setOnClickListener(v -> showManual());
        top.addView(allBtn);

        box.addView(top);

        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dp(10), 0, dp(10));

        container.addView(createArticleCard("Mece ce Al'ada?", "Ilimin zagayowar jinin al'ada da lafiyar mata.", v -> showManual()));
        container.addView(createArticleCard("Menene Ovulation?", "Fahimtar lokacin fitar kwai da haihuwa.", v -> showManual()));
        container.addView(createArticleCard("Kididdige Fertile Window", "Hanyoyin gano kwanakin samun ciki.", v -> showManual()));
        container.addView(createArticleCard("Rage Zafin Mara", "Shawarwarin rage murdawar ciki.", v -> showManual()));

        hsv.addView(container);
        box.addView(hsv);
        return box;
    }

    private View createArticleCard(String title, String desc, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 18));

        ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.header_woman);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setBackground(bg(COLOR_LIGHT_PINK, 12));
        card.addView(img, new LinearLayout.LayoutParams(dp(130), dp(80)));

        TextView tTv = t(title, 13, COLOR_TEXT_MAIN, true);
        tTv.setPadding(0, dp(6), 0, dp(2));
        TextView kTv = t("Karin bayani  ›", 11, COLOR_PRIMARY_PINK, true);

        card.addView(tTv);
        card.addView(kTv);

        card.setOnClickListener(l);
        animateTouch(card);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(145), -2);
        lp.setMargins(0, 0, dp(10), 0);
        card.setLayoutParams(lp);
        return card;
    }

    // --- TAB 1: OVULATION DASHBOARD ---
    private void buildOvulationDashboard(LinearLayout container) {
        String savedStart = trackerStore.getString("period_start", today());
        int savedCycle = trackerStore.getInt("cycle_length", 28);

        Date start;
        try {
            start = parse(savedStart);
        } catch (Exception e) {
            start = new Date();
        }

        Date ovu = addDays(start, savedCycle - 14);
        Date fs = addDays(ovu, -5);
        Date fe = addDays(ovu, 1);
        int cycleDay = Math.max(1, Math.min(savedCycle, daysBetween(start, new Date()) + 1));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(bgGradient(COLOR_DEEP_PLUM, COLOR_PRIMARY_PINK, 22));

        TextView hTitle = t("Lokacin Ovulation • Ovulation Tracker", 20, Color.WHITE, true);
        TextView hSub = t("Ki duba kiyasin lokacin haihuwa da kwanakin da ake iya samun ciki cikin sauki.", 12, 0xFFF8DDEA, false);
        hSub.setPadding(0, dp(4), 0, dp(12));

        hero.addView(hTitle);
        hero.addView(hSub);

        TextView actionBtn = t("Duba Kwanakin Ovulation  ›", 14, COLOR_DEEP_PLUM, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
        actionBtn.setBackground(bg(Color.WHITE, 16));
        actionBtn.setOnClickListener(v -> ovulationDialog());
        animateTouch(actionBtn);

        hero.addView(actionBtn, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(0, 0, 0, dp(14));
        container.addView(hero, hLp);

        showFertilityResultInline(container, start, ovu, fs, fe, cycleDay, savedCycle);
    }

    private void showFertilityResultInline(LinearLayout parent, Date start, Date ovu, Date fs, Date fe, int cycleDay, int cycleLength) {
        LinearLayout panel = box();
        panel.setPadding(dp(16), dp(14), dp(16), dp(16));
        panel.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 20));

        TextView ov = t("🌸 LOKACIN FITAR ƘWAI (OVULATION)\n" + fmt(ovu), 18, COLOR_DEEP_PLUM, true);
        ov.setPadding(dp(14), dp(14), dp(14), dp(14));
        ov.setBackground(bg(COLOR_OVU_BG, 14));
        panel.addView(ov, new LinearLayout.LayoutParams(-1, -2));

        TextView fertile = t("RANAKUN SAMUN CIKI (FERTILE WINDOW)\n" + fmt(fs) + "  –  " + fmt(fe), 16, COLOR_TEXT_MAIN, true);
        fertile.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(10), 0, dp(10));
        fertile.setBackground(bg(COLOR_FERTILE_BG, 14));
        panel.addView(fertile, fp);

        int percent = (int) Math.max(0, Math.min(100, Math.round(cycleDay * 100f / Math.max(1, cycleLength))));
        TextView progressLabel = t("Ci gaban zagayowa: Rana " + cycleDay + " ciki " + cycleLength + "  •  " + percent + "%", 14, COLOR_TEXT_MAIN, true);
        panel.addView(progressLabel);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(percent);
        bar.setProgressTintList(ColorStateList.valueOf(COLOR_PRIMARY_PINK));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFE5DCE2));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(12));
        bp.setMargins(0, dp(6), 0, dp(14));
        panel.addView(bar, bp);

        TextView safetyNote = t("Ranakun fitar ƙwai ƙiyastawa ne ba tabbacin hana ciki ko samun ciki ba. Ovulation timing can vary between cycles.", 11, COLOR_TEXT_MUTED, false);
        safetyNote.setPadding(dp(12), dp(10), dp(12), dp(10));
        safetyNote.setBackground(bg(0xFFFFEAF3, 12));
        panel.addView(safetyNote);

        parent.addView(panel);
    }

    // --- TAB 2: PREGNANCY DASHBOARD ---
    private void buildPregnancyDashboard(LinearLayout container) {
        String pDate = trackerStore.getString("preg_date", today());
        String mode = trackerStore.getString("preg_mode", "lmp");

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(bgGradient(COLOR_DEEP_PLUM, COLOR_PRIMARY_PINK, 22));

        TextView hTitle = t("Bibiyar Ciki • Pregnancy Tracker", 20, Color.WHITE, true);
        TextView hSub = t("Ki bibiyi makonnin cikinki, fahimtar ci gaban jariri da kiyasin ranar haihuwa.", 12, 0xFFF8DDEA, false);
        hSub.setPadding(0, dp(4), 0, dp(12));

        hero.addView(hTitle);
        hero.addView(hSub);

        TextView actionBtn = t("Saita Bayanan Ciki  ›", 14, COLOR_DEEP_PLUM, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
        actionBtn.setBackground(bg(Color.WHITE, 16));
        actionBtn.setOnClickListener(v -> pregnancyDialog());
        animateTouch(actionBtn);

        hero.addView(actionBtn, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(0, 0, 0, dp(14));
        container.addView(hero, hLp);

        try {
            Date entered = parse(pDate);
            Date lmpDate, dueDate;
            if (mode.equals("due")) {
                dueDate = entered;
                lmpDate = addDays(dueDate, -280);
            } else {
                lmpDate = entered;
                dueDate = addDays(lmpDate, 280);
            }

            int totalDays = Math.max(0, daysBetween(lmpDate, new Date()));
            int weeks = Math.min(42, totalDays / 7);
            int daysRem = totalDays % 7;

            LinearLayout infoCard = new LinearLayout(this);
            infoCard.setOrientation(LinearLayout.VERTICAL);
            infoCard.setPadding(dp(16), dp(16), dp(16), dp(16));
            infoCard.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 20));

            TextView wTv = t("👶 MAKON CIKI A YAU\nMako " + weeks + " da kwanaki " + daysRem, 18, COLOR_DEEP_PLUM, true);
            wTv.setPadding(dp(14), dp(14), dp(14), dp(14));
            wTv.setBackground(bg(COLOR_LIGHT_PINK, 14));
            infoCard.addView(wTv);

            TextView dTv = t("📅 RANAR HAIHUWA DA AKE TSAMMANI\n" + fmt(dueDate), 16, COLOR_TEXT_MAIN, true);
            dTv.setPadding(dp(14), dp(14), dp(14), dp(14));
            LinearLayout.LayoutParams dtLp = new LinearLayout.LayoutParams(-1, -2);
            dtLp.setMargins(0, dp(10), 0, dp(10));
            dTv.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 14));
            infoCard.addView(dTv, dtLp);

            String trimester = weeks <= 12 ? "Zangon Farko (Weeks 1–12)" : (weeks <= 27 ? "Zangon Biyu (Weeks 13–27)" : "Zangon Uku (Weeks 28–40+)");
            infoCard.addView(t("● " + trimester, 13, COLOR_PRIMARY_PINK, true));

            container.addView(infoCard);
        } catch (Exception e) {
            container.addView(t("Shigar da ranar al'ada ko due date domin duba ciki.", 13, COLOR_TEXT_MUTED, false));
        }
    }

    // --- TAB 3: BABY GROWTH DASHBOARD ---
    private void buildBabyGrowthDashboard(LinearLayout container) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(bgGradient(COLOR_DEEP_PLUM, COLOR_PRIMARY_PINK, 22));

        TextView hTitle = t("Baby Growth • Girman Jariri", 20, Color.WHITE, true);
        TextView hSub = t("Bibiyi girman jariri, nauyi, da matakan ci gaban lafiya.", 12, 0xFFF8DDEA, false);
        hSub.setPadding(0, dp(4), 0, dp(12));

        hero.addView(hTitle);
        hero.addView(hSub);

        TextView actionBtn = t("+ Yi Rikodin Girma  ›", 14, COLOR_DEEP_PLUM, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
        actionBtn.setBackground(bg(Color.WHITE, 16));
        actionBtn.setOnClickListener(v -> toast("Za ki iya adana bayanan jaririnki a sashen kulawar kai."));
        animateTouch(actionBtn);

        hero.addView(actionBtn, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(0, 0, 0, dp(14));
        container.addView(hero, hLp);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 20));

        card.addView(t("🍼 Matakan Ci Gaban Jariri", 16, COLOR_DEEP_PLUM, true));
        card.addView(t("• Mako 1–4: Bacci mai kyau da shan nono.\n• Mako 5–12: Motsa hannaye da fahimtar muryar uwa.\n• Mako 13+: Murmushi da bin mutane da kallo.", 12, COLOR_TEXT_MUTED, false));

        container.addView(card);
    }

    // --- TAB 4: OTHERS DASHBOARD ---
    private void buildOthersDashboard(LinearLayout container) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(bgGradient(COLOR_DEEP_PLUM, COLOR_PRIMARY_PINK, 22));

        TextView hTitle = t("Sauran Ayyuka • Other Services", 20, Color.WHITE, true);
        TextView hSub = t("Nemi shawarar likita ko bincika kayan lafiya a kasuwarmu.", 12, 0xFFF8DDEA, false);
        hSub.setPadding(0, dp(4), 0, dp(12));

        hero.addView(hTitle);
        hero.addView(hSub);

        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
        hLp.setMargins(0, 0, 0, dp(14));
        container.addView(hero, hLp);

        addSimpleToolCard(container, "🤖", "Tambayi AI Assistant / Ask AI", "Tambayi AI duk wata tambaya game da lafiyar mata.", v -> startActivity(new Intent(this, AiActivity.class)));
        addSimpleToolCard(container, "👩‍⚕️", "Ask a Doctor / Tambayi Likita", "Send a question to the app's health support team and view replies.", v -> questionsDialog());
        addSimpleToolCard(container, "🛍", "Marketplace / Kasuwa", "Browse products posted by the administrator.", v -> marketplace());
        addSimpleToolCard(container, "📘", "Jagorar Tools / Tools Manual", "Read full manual in Hausa and English.", v -> showManual());
    }

    private void addSimpleToolCard(LinearLayout container, String icon, String title, String sub, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), dp(12), dp(12), dp(12));
        r.setBackground(bg(Color.WHITE, 16));

        TextView i = t(icon, 24, COLOR_PRIMARY_PINK, true);
        i.setGravity(Gravity.CENTER);
        r.addView(i, new LinearLayout.LayoutParams(dp(42), dp(46)));

        LinearLayout x = new LinearLayout(this);
        x.setOrientation(LinearLayout.VERTICAL);
        x.setPadding(dp(10), 0, dp(4), 0);

        x.addView(t(title, 15, COLOR_TEXT_MAIN, true));
        TextView s = t(sub, 12, COLOR_TEXT_MUTED, false);
        s.setPadding(0, dp(2), 0, 0);
        x.addView(s);

        r.addView(x, new LinearLayout.LayoutParams(0, -2, 1));
        TextView a = t("›", 26, COLOR_PRIMARY_PINK, false);
        a.setGravity(Gravity.CENTER);
        r.addView(a, new LinearLayout.LayoutParams(dp(30), dp(46)));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(10));
        container.addView(r, p);

        r.setOnClickListener(l);
        animateTouch(r);
    }

    private void savePeriodHistory(String start, String end, int cycle) {
        try {
            String existing = trackerStore.getString("period_history_json", "[]");
            JSONArray arr = new JSONArray(existing);
            JSONObject obj = new JSONObject();
            obj.put("start", start);
            obj.put("end", end);
            obj.put("cycle", cycle);
            obj.put("created_at", today());
            arr.put(obj);
            trackerStore.edit().putString("period_history_json", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // --- MODALS & INPUT DIALOGS ---
    private void editPeriodStartDialog() {
        LinearLayout l = box();
        EditText startInput = dateInputCardField(l, "Ranar Farko Ta Al'ada / Period Start Date", "Zaɓi ranar");
        startInput.setText(trackerStore.getString("period_start", today()));

        Dialog d = createProDialog("📅", "Gyara Ranar Al'ada", "Update period start date", l);
        TextView saveBtn = t("Ajiye / Save", 15, Color.WHITE, true);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
        saveBtn.setBackground(bg(COLOR_PRIMARY_PINK, 16));
        l.addView(saveBtn, new LinearLayout.LayoutParams(-1, -2));

        saveBtn.setOnClickListener(v -> {
            String val = startInput.getText().toString().trim();
            if (!val.isEmpty()) {
                trackerStore.edit().putString("period_start", val).apply();
                saveTracker("period", val, trackerStore.getInt("cycle_length", 28), "{}", null);
                d.dismiss();
                build();
                toast("An sabunta ranar al'ada!");
            }
        });
        d.show();
    }

    private void editCycleDialog() {
        LinearLayout l = box();
        EditText cInput = inputCardField(l, "Tsawon Cycle (Kwanaki)", "e.g. 28", InputType.TYPE_CLASS_NUMBER);
        cInput.setText(String.valueOf(trackerStore.getInt("cycle_length", 28)));

        Dialog d = createProDialog("🕒", "Gyara Tsawon Cycle", "Update average cycle length in days", l);
        TextView saveBtn = t("Ajiye / Save", 15, Color.WHITE, true);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
        saveBtn.setBackground(bg(COLOR_PRIMARY_PINK, 16));
        l.addView(saveBtn, new LinearLayout.LayoutParams(-1, -2));

        saveBtn.setOnClickListener(v -> {
            try {
                int c = Integer.parseInt(cInput.getText().toString().trim());
                trackerStore.edit().putInt("cycle_length", Math.max(20, Math.min(60, c))).apply();
                d.dismiss();
                build();
                toast("An sabunta tsawon cycle!");
            } catch (Exception ignored) {
            }
        });
        d.show();
    }

    private void editDurationDialog() {
        LinearLayout l = box();
        EditText dInput = inputCardField(l, "Tsawon Kwanakin Al'ada", "e.g. 5", InputType.TYPE_CLASS_NUMBER);
        dInput.setText(String.valueOf(trackerStore.getInt("period_duration", 5)));

        Dialog d = createProDialog("🩸", "Gyara Kwanakin Al'ada", "Update period duration in days", l);
        TextView saveBtn = t("Ajiye / Save", 15, Color.WHITE, true);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setPadding(dp(14), dp(12), dp(14), dp(12));
        saveBtn.setBackground(bg(COLOR_PRIMARY_PINK, 16));
        l.addView(saveBtn, new LinearLayout.LayoutParams(-1, -2));

        saveBtn.setOnClickListener(v -> {
            try {
                int dur = Integer.parseInt(dInput.getText().toString().trim());
                trackerStore.edit().putInt("period_duration", Math.max(2, Math.min(10, dur))).apply();
                d.dismiss();
                build();
                toast("An sabunta kwanakin al'ada!");
            } catch (Exception ignored) {
            }
        });
        d.show();
    }

    private void showPeriodHistoryModal() {
        LinearLayout l = box();
        String raw = trackerStore.getString("period_history_json", "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            if (arr.length() == 0) {
                l.addView(t("Babu tarihin al'adar da aka yi rikodin tukuna.", 13, COLOR_TEXT_MUTED, false));
            } else {
                for (int i = arr.length() - 1; i >= 0; i--) {
                    JSONObject obj = arr.getJSONObject(i);
                    LinearLayout item = new LinearLayout(this);
                    item.setOrientation(LinearLayout.VERTICAL);
                    item.setPadding(dp(12), dp(10), dp(12), dp(10));
                    item.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 14));

                    item.addView(t("📅 Starts: " + obj.optString("start"), 13, COLOR_TEXT_MAIN, true));
                    item.addView(t("Cycle: " + obj.optInt("cycle") + " days • End: " + obj.optString("end", "-"), 11, COLOR_TEXT_MUTED, false));

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                    lp.setMargins(0, 0, 0, dp(8));
                    l.addView(item, lp);
                }
            }
        } catch (Exception e) {
            l.addView(t("Kasa loda tarihi.", 13, COLOR_TEXT_MUTED, false));
        }

        Dialog d = createProDialog("📖", "Tarihin Al'ada / History", "Logged menstrual period history", l);
        d.show();
    }

    private void showCycleReportModal() {
        LinearLayout l = box();
        int cycle = trackerStore.getInt("cycle_length", 28);
        int dur = trackerStore.getInt("period_duration", 5);

        l.addView(t("📊 RAHOTO DA FAHIMTAR PATTERN", 16, COLOR_DEEP_PLUM, true));
        l.addView(t("• Matsakaicin tsawon cycle: " + cycle + " Kwana\n• Matsakaicin kwanakin al'ada: " + dur + " Kwana\n• Yanayin zagayowa: Regular Cycle (Al'ada mai ka'ida)\n\nWannan bayanin yana taimakawa wajen kiyaye lafiyar mahaifa.", 13, COLOR_TEXT_MAIN, false));

        Dialog d = createProDialog("📊", "Rahoto & Chart", "Summary of menstrual patterns", l);
        d.show();
    }

    private void editReminderSettingsModal() {
        LinearLayout l = box();
        boolean reminderOn = trackerStore.getBoolean("period_reminder", true);

        Switch sw = new Switch(this);
        sw.setText("Kunna Reminder na Al'ada");
        sw.setTextColor(COLOR_TEXT_MAIN);
        sw.setChecked(reminderOn);
        sw.setOnCheckedChangeListener((cb, checked) -> {
            trackerStore.edit().putBoolean("period_reminder", checked).apply();
            toast(checked ? "An kunna reminder." : "An kashe reminder.");
        });

        l.addView(sw);
        Dialog d = createProDialog("🔔", "Saita Reminder", "Configure period notification reminders", l);
        d.show();
    }

    private EditText inputCardField(LinearLayout container, String labelText, String hintText, int inputType) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

        TextView lbl = t(labelText, 13, COLOR_DEEP_PLUM, true);
        lbl.setPadding(0, 0, 0, dp(6));
        card.addView(lbl);

        EditText e = new EditText(this);
        e.setHint(hintText);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setTextSize(15);
        e.setTextColor(COLOR_TEXT_MAIN);
        e.setHintTextColor(0xFF9A8F97);
        if (inputType != 0) e.setInputType(inputType);
        e.setBackground(bg(COLOR_LIGHT_PINK, 12));

        card.addView(e, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(12));
        container.addView(card, cp);
        return e;
    }

    private EditText dateInputCardField(LinearLayout container, String labelText, String hintText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

        TextView lbl = t(labelText, 13, COLOR_DEEP_PLUM, true);
        lbl.setPadding(0, 0, 0, dp(6));
        card.addView(lbl);

        EditText e = new EditText(this);
        e.setHint(hintText + " (Taba domin zabar rana)");
        e.setFocusable(false);
        e.setClickable(true);
        e.setInputType(0);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setTextSize(15);
        e.setTextColor(COLOR_TEXT_MAIN);
        e.setHintTextColor(0xFF9A8F97);
        e.setBackground(bg(COLOR_LIGHT_PINK, 12));
        e.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_my_calendar, 0);

        e.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                Date current = parse(e.getText().toString().trim());
                cal.setTime(current);
            } catch (Exception ignored) {
            }
            DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, day) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.clear();
                chosen.set(Calendar.YEAR, y);
                chosen.set(Calendar.MONTH, m);
                chosen.set(Calendar.DAY_OF_MONTH, day);
                e.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(chosen.getTime()));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });

        card.addView(e, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(12));
        container.addView(card, cp);
        return e;
    }

    private Dialog createProDialog(String icon, String hausaTitle, String subText, View contentView) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(bg(COLOR_LIGHT_PINK, 24));

        // Header Card
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(16), dp(18), dp(16));
        header.setBackground(bgGradient(COLOR_DEEP_PLUM, COLOR_PRIMARY_PINK, 24));

        TextView iconTv = t(icon, 26, COLOR_DEEP_PLUM, false);
        iconTv.setGravity(Gravity.CENTER);
        iconTv.setBackground(bg(Color.WHITE, 16));
        header.addView(iconTv, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, dp(8), 0);

        TextView titleView = t(hausaTitle, 18, Color.WHITE, true);
        TextView subView = t(subText, 11, 0xFFF8DDEA, false);
        titleBox.addView(titleView);
        titleBox.addView(subView);

        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView closeBtn = t("✕", 20, Color.WHITE, true);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        header.addView(closeBtn);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        animateTouch(closeBtn);

        layout.addView(header);

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.addView(contentView);
        layout.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.86f);
            dialog.getWindow().setLayout(w, h);
        }
        return dialog;
    }

    private LinearLayout box() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        return l;
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private Date parse(String s) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s);
    }

    private String fmt(Date d) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(d);
    }

    private String fmtShort(Date d) {
        return new SimpleDateFormat("dd MMM", Locale.US).format(d);
    }

    private Date addDays(Date d, int n) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_YEAR, n);
        return c.getTime();
    }

    private int daysBetween(Date a, Date b) {
        return (int) ((b.getTime() - a.getTime()) / (24 * 60 * 60 * 1000L));
    }

    // --- PERIOD TRACKER DIALOG ---
    private void periodDialog() {
        LinearLayout l = box();

        String savedStart = trackerStore.getString("period_start", today());
        int savedCycle = trackerStore.getInt("cycle_length", 28);
        String savedEnd = trackerStore.getString("period_end", "");

        EditText startInput = dateInputCardField(l, "Ranar Farko Ta Al'ada / Period Start Date", "Zaɓi ranar farko");
        startInput.setText(savedStart);

        EditText endInput = dateInputCardField(l, "Ranar Ƙarshe (Optional) / End Date", "Zaɓi ranar ƙarshe idan an sani");
        endInput.setText(savedEnd);

        EditText cycleInput = inputCardField(l, "Tsawon Zagayowar Al'ada / Cycle Length (Days)", "Kwanaki (e.g. 28)", InputType.TYPE_CLASS_NUMBER);
        cycleInput.setText(String.valueOf(savedCycle));

        TextView tipNote = t("Lura: Zagayowar al'ada ta cika kasancewa tsakanin kwanaki 21 zuwa 35. Cycle timing varies between women.", 11, COLOR_TEXT_MUTED, false);
        tipNote.setPadding(dp(12), dp(10), dp(12), dp(10));
        tipNote.setBackground(bg(0xFFFFF0F5, 12));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, 0, 0, dp(14));
        l.addView(tipNote, np);

        TextView actionBtn = t("Ƙiyasta da Ajiye / Calculate & Save", 15, Color.WHITE, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(14), dp(14), dp(14));
        actionBtn.setBackground(bgGradient(COLOR_PRIMARY_PINK, COLOR_DEEP_PLUM, 16));
        actionBtn.setMinimumHeight(dp(48));
        l.addView(actionBtn, new LinearLayout.LayoutParams(-1, -2));

        Dialog d = createProDialog("📅", "Bibiyar Al'ada", "Track your menstrual cycle & next period", l);

        actionBtn.setOnClickListener(v -> {
            try {
                String startDate = startInput.getText().toString().trim();
                String endDate = endInput.getText().toString().trim();
                int cycleLen = Math.max(20, Math.min(60, Integer.parseInt(cycleInput.getText().toString().trim())));

                Date start = parse(startDate);
                Date next = addDays(start, cycleLen);
                Date ovu = addDays(next, -14);
                Date fertileStart = addDays(ovu, -5);
                Date fertileEnd = addDays(ovu, 1);

                trackerStore.edit()
                        .putString("period_start", startDate)
                        .putString("period_end", endDate)
                        .putInt("cycle_length", cycleLen)
                        .apply();

                savePeriodHistory(startDate, endDate, cycleLen);
                saveTracker("period", startDate, cycleLen, "{\"end\":\"" + endDate + "\"}", null);

                d.dismiss();
                build();

                showResultCard("Mai Bibiyar Al'ada • Period Tracker",
                        "Sakamakon Hasashen Zagayowar Al'ada",
                        "Al'adarki ta gaba na iya farawa a ranar: " + fmt(next),
                        "Matafiyar zagayowar al'ada: kwanaki " + cycleLen,
                        "Lokacin fitar kwai da aka kiyasta: " + fmt(ovu) + " (Ranakun haihuwa: " + fmtShort(fertileStart) + " – " + fmtShort(fertileEnd) + ")",
                        "Wadannan ranaku kimantawa ne bisa bayanan da kika shigar. Ba tabbacin likita bane.");
            } catch (Exception e) {
                toast("Shigar da ingantacciyar rana da lamba.");
            }
        });
        animateTouch(actionBtn);

        d.show();
    }

    // --- OVULATION TRACKER DIALOG ---
    private void ovulationDialog() {
        String savedStart = trackerStore.getString("period_start", today());
        int savedCycle = trackerStore.getInt("cycle_length", 28);

        LinearLayout l = box();

        EditText startInput = dateInputCardField(l, "Ranar Farko Ta Al'ada / Period Start Date", "Ranar fara al'ada");
        startInput.setText(savedStart);

        EditText cycleInput = inputCardField(l, "Tsawon Zagayowar Al'ada / Cycle Length", "Kwanaki (e.g. 28)", InputType.TYPE_CLASS_NUMBER);
        cycleInput.setText(String.valueOf(savedCycle));

        TextView safetyNotice = t("⚠️ MUHIMMIN ISARWA / MEDICAL SAFETY NOTICE:\nSakamakon fitar ƙwai ƙiyastawa ne kawai. Kada a yi amfani da wannan a matsayin ingantacciyar hanyar hana daukar ciki.", 11, 0xFF991B1B, true);
        safetyNotice.setPadding(dp(12), dp(10), dp(12), dp(10));
        safetyNotice.setBackground(bg(0xFFFEE2E2, 12));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, 0, 0, dp(14));
        l.addView(safetyNotice, np);

        TextView actionBtn = t("Duba Kwanakin Fitar Kwai / Calculate Ovulation", 15, Color.WHITE, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(14), dp(14), dp(14));
        actionBtn.setBackground(bgGradient(COLOR_PRIMARY_PINK, COLOR_DEEP_PLUM, 16));
        actionBtn.setMinimumHeight(dp(48));
        l.addView(actionBtn, new LinearLayout.LayoutParams(-1, -2));

        Dialog d = createProDialog("🌸", "Lokacin Ovulation", "Estimate ovulation date & fertile window", l);

        actionBtn.setOnClickListener(v -> {
            try {
                String startDate = startInput.getText().toString().trim();
                int cycleLen = Math.max(20, Math.min(60, Integer.parseInt(cycleInput.getText().toString().trim())));

                Date start = parse(startDate);
                Date ovu = addDays(start, cycleLen - 14);
                Date fs = addDays(ovu, -5);
                Date fe = addDays(ovu, 1);

                trackerStore.edit()
                        .putString("period_start", startDate)
                        .putInt("cycle_length", cycleLen)
                        .apply();

                saveTracker("ovulation", startDate, cycleLen, "{}", null);
                d.dismiss();
                build();

                int cycleDay = Math.max(1, Math.min(cycleLen, daysBetween(start, new Date()) + 1));
                showFertilityResult(start, ovu, fs, fe, cycleDay, cycleLen);
            } catch (Exception e) {
                toast("Tabbatar da shigar da ingantattun bayanai.");
            }
        });
        animateTouch(actionBtn);

        d.show();
    }

    // --- PREGNANCY TRACKER DIALOG ---
    private void pregnancyDialog() {
        LinearLayout l = box();

        String savedDate = trackerStore.getString("preg_date", today());
        String savedMode = trackerStore.getString("preg_mode", "lmp");

        LinearLayout modeCard = new LinearLayout(this);
        modeCard.setOrientation(LinearLayout.VERTICAL);
        modeCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        modeCard.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

        TextView modeLbl = t("Hanyar Ƙiyasta Ciki / Calculation Mode", 13, COLOR_DEEP_PLUM, true);
        modeLbl.setPadding(0, 0, 0, dp(6));
        modeCard.addView(modeLbl);

        RadioGroup modeGroup = new RadioGroup(this);
        modeGroup.setOrientation(RadioGroup.VERTICAL);

        RadioButton rLmp = new RadioButton(this);
        rLmp.setText("Ranar al'ada ta karshe (LMP)");
        rLmp.setTextColor(COLOR_TEXT_MAIN);

        RadioButton rDue = new RadioButton(this);
        rDue.setText("Ranar haihuwa da likita ya tabbatar (Due Date)");
        rDue.setTextColor(COLOR_TEXT_MAIN);

        modeGroup.addView(rLmp);
        modeGroup.addView(rDue);
        if (savedMode.equals("due")) rDue.setChecked(true);
        else rLmp.setChecked(true);

        modeCard.addView(modeGroup);

        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(-1, -2);
        mcLp.setMargins(0, 0, 0, dp(12));
        l.addView(modeCard, mcLp);

        EditText dateInputView = dateInputCardField(l, "Rana / Selected Date", "Zaɓi ranar da ta dace");
        dateInputView.setText(savedDate);

        TextView actionBtn = t("Bibiyi Cikina / Track Pregnancy", 15, Color.WHITE, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(14), dp(14), dp(14));
        actionBtn.setBackground(bgGradient(COLOR_PRIMARY_PINK, COLOR_DEEP_PLUM, 16));
        actionBtn.setMinimumHeight(dp(48));
        l.addView(actionBtn, new LinearLayout.LayoutParams(-1, -2));

        Dialog d = createProDialog("👶", "Bibiyar Ciki", "Follow pregnancy week by week & due date", l);

        actionBtn.setOnClickListener(v -> {
            try {
                String inputDate = dateInputView.getText().toString().trim();
                boolean isDueMode = rDue.isChecked();
                String mode = isDueMode ? "due" : "lmp";

                Date entered = parse(inputDate);
                Date lmpDate, dueDate;
                if (isDueMode) {
                    dueDate = entered;
                    lmpDate = addDays(dueDate, -280);
                } else {
                    lmpDate = entered;
                    dueDate = addDays(lmpDate, 280);
                }

                int totalDays = Math.max(0, daysBetween(lmpDate, new Date()));
                int weeks = Math.min(42, totalDays / 7);
                int daysRem = totalDays % 7;

                trackerStore.edit()
                        .putString("preg_date", inputDate)
                        .putString("preg_mode", mode)
                        .apply();

                saveTracker("pregnancy", inputDate, 0, "{\"mode\":\"" + mode + "\"}", null);
                d.dismiss();
                build();

                String trimesterInfo;
                if (weeks <= 12) {
                    trimesterInfo = "Zangon Farko (First Trimester • Weeks 1–12): Jariri yana saurin ginuwa.";
                } else if (weeks <= 27) {
                    trimesterInfo = "Zangon Biyu (Second Trimester • Weeks 13–27): Motsin jariri na kara bayyana.";
                } else {
                    trimesterInfo = "Zangon Uku (Third Trimester • Weeks 28–40+): Shirye-shiryen haihuwa.";
                }

                showResultCard("Bibiyar Ciki • Pregnancy Tracker",
                        "Kiyasin Makonnin Ciki da Ranar Haihuwa",
                        "Kina iya kasancewa a mako na: " + weeks + " da kwanaki " + daysRem,
                        "Ranar haihuwa da ake tsammani (Due Date): " + fmt(dueDate),
                        trimesterInfo,
                        "Ranar haihuwa ƙiyastawa ce ba tabbaci na ƙarshe ba. Tuntubi ma'aikacin lafiya domin duba ci gaban ciki.");
            } catch (Exception e) {
                toast("Tabbatar da shigar da ranar da ta dace.");
            }
        });
        animateTouch(actionBtn);

        d.show();
    }

    // --- SYMPTOMS TRACKER DIALOG ---
    private void symptomsDialog() {
        LinearLayout l = box();

        String[] symptomsList = {
                "🩸 Abdominal pain / Ciwon mara",
                "🤕 Headache / Ciwon kai",
                "😴 Fatigue / Gajiya",
                "🎭 Mood changes / Sauyin yanayi",
                "🤢 Nausea / Tashin zuciya",
                "🦴 Back pain / Ciwon baya",
                "⚡ Cramps / Murɗawan ciki",
                "✏️ Other / Wata alama"
        };

        LinearLayout symCard = new LinearLayout(this);
        symCard.setOrientation(LinearLayout.VERTICAL);
        symCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        symCard.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

        TextView symLbl = t("Zaɓi Alama / Select Symptom", 13, COLOR_DEEP_PLUM, true);
        symLbl.setPadding(0, 0, 0, dp(6));
        symCard.addView(symLbl);

        Spinner symSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, symptomsList);
        symSpinner.setAdapter(adapter);
        symSpinner.setBackground(bg(COLOR_LIGHT_PINK, 12));
        symSpinner.setPadding(dp(10), dp(10), dp(10), dp(10));
        symCard.addView(symSpinner, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(-1, -2);
        scLp.setMargins(0, 0, 0, dp(12));
        l.addView(symCard, scLp);

        EditText customSymInput = inputCardField(l, "Sunan Alama (If 'Other' is selected)", "Rubuta alama a nan", 0);

        LinearLayout sevCard = new LinearLayout(this);
        sevCard.setOrientation(LinearLayout.VERTICAL);
        sevCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        sevCard.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

        TextView sevLabel = t("Tsananin Alama / Severity", 13, COLOR_DEEP_PLUM, true);
        sevLabel.setPadding(0, 0, 0, dp(6));
        sevCard.addView(sevLabel);

        RadioGroup sevGroup = new RadioGroup(this);
        sevGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rMild = new RadioButton(this);
        rMild.setText("Ɗan kaɗan / Mild");
        rMild.setTextColor(COLOR_TEXT_MAIN);

        RadioButton rMod = new RadioButton(this);
        rMod.setText("Matsakaici");
        rMod.setTextColor(COLOR_TEXT_MAIN);

        RadioButton rSev = new RadioButton(this);
        rSev.setText("Mai tsanani");
        rSev.setTextColor(COLOR_TEXT_MAIN);

        sevGroup.addView(rMild);
        sevGroup.addView(rMod);
        sevGroup.addView(rSev);
        rMild.setChecked(true);
        sevCard.addView(sevGroup);

        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(-1, -2);
        svLp.setMargins(0, 0, 0, dp(12));
        l.addView(sevCard, svLp);

        EditText logDateInput = dateInputCardField(l, "Ranar da Aka Ji Alama / Date", "Rana");
        logDateInput.setText(today());

        EditText notesInput = inputCardField(l, "Ƙarin Bayani / Optional Notes", "Mene ne kika ji?", 0);

        TextView actionBtn = t("Ajiye Alama / Save Symptom", 15, Color.WHITE, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(14), dp(14), dp(14));
        actionBtn.setBackground(bgGradient(COLOR_PRIMARY_PINK, COLOR_DEEP_PLUM, 16));
        actionBtn.setMinimumHeight(dp(48));
        l.addView(actionBtn, new LinearLayout.LayoutParams(-1, -2));

        Dialog d = createProDialog("💓", "Bibiyar Alamomi", "Record & track physical symptoms & changes", l);

        actionBtn.setOnClickListener(v -> {
            String selectedSym = symSpinner.getSelectedItem().toString();
            if (selectedSym.contains("Other") && !customSymInput.getText().toString().trim().isEmpty()) {
                selectedSym = customSymInput.getText().toString().trim();
            }
            String severityStr = rSev.isChecked() ? "Mai tsanani / Severe" : (rMod.isChecked() ? "Matsakaici / Moderate" : "Ɗan kaɗan / Mild");
            String dateStr = logDateInput.getText().toString().trim();
            String noteStr = notesInput.getText().toString().trim();

            saveSymptomLocal(selectedSym, severityStr, dateStr, noteStr);

            Map<String, String> m = new HashMap<>();
            m.put("symptom", selectedSym);
            m.put("severity", severityStr);
            m.put("note", noteStr);
            m.put("log_date", dateStr);
            Api.post("symptoms.php", m, token, new Api.Callback() {
                public void ok(String b) {
                    runOnUiThread(() -> toast("An ajiye alama cikin nasara / Symptom saved."));
                }

                public void err(Exception e) {
                    runOnUiThread(() -> toast("An ajiye a cikin waya (Offline saved)."));
                }
            });

            d.dismiss();
            build();

            if (rSev.isChecked()) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ SHAWARAR KULA WA / MEDICAL ALERT")
                        .setMessage("Kin yi rikodin alama mai tsanani (" + selectedSym + "). Idan kina jin zafi mai tsanani ko jinin da bai tsaya ba, nemi kulawar ma'aikacin lafiya ko asibiti maza-maza.\n\nYou recorded a severe symptom. Please consult a healthcare professional immediately if symptoms persist or worsen.")
                        .setPositiveButton("Na Tabbatar / OK", null)
                        .show();
            } else {
                toast("An ajiye alamarku! / Symptom logged successfully.");
            }
        });
        animateTouch(actionBtn);

        d.show();
    }

    private void saveSymptomLocal(String sym, String sev, String date, String note) {
        try {
            String existing = trackerStore.getString("symptoms_json", "[]");
            JSONArray arr = new JSONArray(existing);
            JSONObject obj = new JSONObject();
            obj.put("symptom", sym);
            obj.put("severity", sev);
            obj.put("date", date);
            obj.put("note", note);
            arr.put(obj);
            trackerStore.edit().putString("symptoms_json", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // --- TOOLS MANUAL DIALOG ---
    private void showManual() {
        LinearLayout l = box();

        TextView intro = t("LAFIYAR MACE – TRACKERS MANUAL", 18, COLOR_DEEP_PLUM, true);
        intro.setPadding(0, 0, 0, dp(12));
        l.addView(intro);

        // HAUSA SECTION
        TextView hausaHeading = t("BAYANI A HAUSA", 16, COLOR_PRIMARY_PINK, true);
        hausaHeading.setPadding(0, dp(4), 0, dp(6));
        l.addView(hausaHeading);

        TextView hausaBody = t(
                "1. PERIOD TRACKER (Mai Bibiyar Jinin Al'ada)\n" +
                        "• Amfaninsa: Yana taimaka wa mace ta bibiyi zagayowar jinin al'adarta, sanin tsawon lokacin al'ada da samun hasashen ranar da al'ada ta gaba za ta fara.\n" +
                        "• Yadda ake amfani da shi: Ki shigar da ranar da jinin al'adarki ya fara da ranar da ya ƙare. Ki ci gaba da sabunta bayanan kowane wata domin samun hasashe mafi dacewa.\n\n" +

                        "2. OVULATION TRACKER (Mai Bibiyar Lokacin Fitar Ƙwai)\n" +
                        "• Amfaninsa: Yana taimaka wa mace ta fahimci ranakun da fitar ƙwai zai iya faruwa da lokacin da yiwuwar samun ciki ta fi yawa.\n" +
                        "• Yadda ake amfani da shi: Ki shigar da bayanan jinin al'adarki, musamman ranar da ya fara da tsawon zagayowar al'adarki idan kin sani. Tracker zai yi amfani da bayanan wajen hasashen lokacin fitar ƙwai da ranakun da yiwuwar samun ciki ta fi yawa.\n\n" +

                        "3. PREGNANCY TRACKER (Mai Bibiyar Ciki)\n" +
                        "• Amfaninsa: Yana taimaka wa mai ciki ta bibiyi makonnin cikinta, fahimtar ci gaban jariri da sanin muhimman matakan kula da lafiya yayin ciki.\n" +
                        "• Yadda ake amfani da shi: Ki shigar da ranar farko ta jinin al'adarki na ƙarshe ko ranar haihuwa da aka ƙiyasta idan an tabbatar da ita. Tracker zai taimaka wajen ƙiyasta makonnin ciki da ranar haihuwa da ake tsammani. Ki sabunta bayanai idan an ba ki sabon ƙiyasin ranar haihuwa a asibiti.\n\n" +

                        "4. SYMPTOMS TRACKER (Mai Bibiyar Alamomin Jiki)\n" +
                        "• Amfaninsa: Yana taimaka wa mace ta rubuta alamomin da take ji, kamar ciwon mara, ciwon kai, gajiya, sauyin yanayi da sauran sauye-sauyen jiki.\n" +
                        "• Yadda ake amfani da shi: Ki zaɓi alamomin da kike ji, ki rubuta lokacin da suka fara, tsaninsu da tsawon lokacin da suka ɗauka. Ki riƙa sabunta bayanan domin taimaka miki lura da tsarin alamomin jikinki da bayyana su ga ma'aikacin lafiya idan ya zama dole.",
                13, COLOR_TEXT_MAIN, false);
        hausaBody.setLineSpacing(dp(4), 1.0f);
        hausaBody.setPadding(0, 0, 0, dp(16));
        l.addView(hausaBody);

        // ENGLISH SECTION
        TextView engHeading = t("ENGLISH VERSION", 16, COLOR_PRIMARY_PINK, true);
        engHeading.setPadding(0, dp(6), 0, dp(6));
        l.addView(engHeading);

        TextView engBody = t(
                "1. PERIOD TRACKER\n" +
                        "• Benefits: Helps women track their menstrual cycles, understand the length of their periods, and estimate when their next period may begin.\n" +
                        "• How to Use It: Enter the date your period starts and the date it ends. Continue updating your information every month to help improve future predictions.\n\n" +

                        "2. OVULATION TRACKER\n" +
                        "• Benefits: Helps women estimate when ovulation may occur and identify the days when the likelihood of becoming pregnant may be higher.\n" +
                        "• How to Use It: Enter your menstrual information, especially the first day of your last period and your usual cycle length, if known. The tracker uses this information to estimate your possible ovulation date and fertile window.\n\n" +

                        "3. PREGNANCY TRACKER\n" +
                        "• Benefits: Helps pregnant women follow their pregnancy week by week, understand their baby's development, and learn about important health and wellness milestones during pregnancy.\n" +
                        "• How to Use It: Enter the first day of your last menstrual period or your estimated due date if it has been confirmed by a healthcare professional. The tracker will help estimate how many weeks pregnant you may be and your expected due date. Update your information if your healthcare professional provides a revised due date.\n\n" +

                        "4. SYMPTOMS TRACKER\n" +
                        "• Benefits: Helps women record physical symptoms and changes they experience, such as abdominal pain, headaches, fatigue, mood changes, and other bodily changes.\n" +
                        "• How to Use It: Select the symptoms you experience and record when they started, how severe they were, and how long they lasted. Keep your records updated to help you identify patterns and explain your symptoms to a healthcare professional when necessary.",
                13, COLOR_TEXT_MAIN, false);
        engBody.setLineSpacing(dp(4), 1.0f);
        engBody.setPadding(0, 0, 0, dp(16));
        l.addView(engBody);

        // DISCLAIMER
        TextView warning = t(
                "MUHIMMIN BAYANI / IMPORTANT INFORMATION\n" +
                        "Sakamakon trackers hasashe ne bisa bayanan da aka shigar, ba tabbacin ainihin ranar fitar ƙwai, samun ciki ko ranar haihuwa ba. Idan kina da wata alama mai tsanani ko damuwa game da lafiyarki, ki nemi kulawar ma'aikacin lafiya.\n\n" +
                        "Tracker results are estimates based on the information entered. They cannot guarantee the exact date of ovulation, whether pregnancy will occur, or the actual date of delivery. If you experience severe symptoms or have concerns about your health, seek advice from a qualified healthcare professional.\n\n" +
                        "LAFIYAR MACE — Ilimi, Bibiyar Lafiya da Kulawar Kai a Kowane Mataki na Rayuwa.",
                12, COLOR_DEEP_PLUM, true);
        warning.setLineSpacing(dp(4), 1.0f);
        warning.setPadding(dp(12), dp(12), dp(12), dp(12));
        warning.setBackground(bg(0xFFFFEAF3, 14));
        l.addView(warning);

        Dialog d = createProDialog("📘", "Jagorar Tools / Tools Manual", "Complete guide for all health trackers", l);
        d.show();
    }

    private void showFertilityResult(Date start, Date ovu, Date fs, Date fe, int cycleDay, int cycleLength) {
        showFertilityResultInline(box(), start, ovu, fs, fe, cycleDay, cycleLength);
        showResultCard("Lokacin Ovulation • Ovulation Tracker",
                "Sakamakon Kimanta Ranar Fitar Ƙwai",
                "Ranar fitar kwai da aka kiyasta: " + fmt(ovu),
                "Lokacin samun ciki (Fertile Window): " + fmt(fs) + " – " + fmt(fe),
                "Matafiyar zagayowa: Rana " + cycleDay + " ciki " + cycleLength,
                "Wadannan ranaku kimantawa ne bisa bayanan da kika shigar.");
    }

    // --- RESULT CARD & ADMOB REWARDED DOWNLOAD ---
    private View lastResultCard;
    private String lastResultTitle = "";
    private final ArrayList<String> lastResultLines = new ArrayList<>();

    private void showResultCard(String title, String tagline, String a, String b, String c, String disclaimer) {
        lastResultTitle = title;
        lastResultLines.clear();
        lastResultLines.add(a);
        lastResultLines.add(b);
        lastResultLines.add(c);
        lastResultLines.add(disclaimer);

        LinearLayout outer = box();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(strokeBg(0xFFFFF0F7, COLOR_BORDER, 1, 20));
        card.setElevation(dp(3));

        card.addView(t("♡  LAFIYAR MACE", 12, COLOR_PRIMARY_PINK, true));

        TextView h = t(title, 20, COLOR_TEXT_MAIN, true);
        h.setPadding(0, dp(10), 0, dp(4));
        card.addView(h);

        card.addView(t(tagline, 13, COLOR_TEXT_MUTED, false));

        View line = new View(this);
        line.setBackgroundColor(COLOR_PRIMARY_PINK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(2));
        lp.setMargins(0, dp(12), 0, dp(12));
        card.addView(line, lp);

        for (int i = 0; i < 3; i++) {
            TextView row = t("✦  " + lastResultLines.get(i), 14, COLOR_TEXT_MAIN, true);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackground(bg(Color.WHITE, 12));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.setMargins(0, 0, 0, dp(8));
            card.addView(row, rp);
        }

        TextView disclaimerView = t(lastResultLines.get(3), 11, COLOR_TEXT_MUTED, false);
        disclaimerView.setPadding(dp(2), dp(6), dp(2), dp(4));
        card.addView(disclaimerView);

        TextView footer = t("Lafiyar Mace • Women’s health, explained with care", 11, COLOR_DEEP_PLUM, true);
        footer.setPadding(0, dp(10), 0, 0);
        card.addView(footer);

        outer.addView(card);
        lastResultCard = card;

        TextView download = t("⬇  Ajiye Hoton Sakamako / Download Result Image", 14, Color.WHITE, true);
        download.setGravity(Gravity.CENTER);
        download.setPadding(dp(12), dp(14), dp(12), dp(14));
        download.setBackground(bgGradient(COLOR_PRIMARY_PINK, COLOR_DEEP_PLUM, 14));
        download.setMinimumHeight(dp(48));
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(-1, -2);
        dp1.setMargins(0, dp(12), 0, 0);
        outer.addView(download, dp1);

        download.setOnClickListener(v -> downloadAfterReward(download));
        animateTouch(download);

        Dialog d = createProDialog("✦", "Sakamakonku / Your Result", "Generated tracking summary", outer);
        d.show();
    }

    private void downloadAfterReward(TextView button) {
        button.setEnabled(false);
        button.setText("Lodi tallan sakamako… / Loading ad…");
        MobileAds.initialize(this, status -> {
        });
        RewardedAd.load(this, Config.ADMOB_REWARDED_ID, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("⬇  Ajiye Hoton Sakamako / Download Result Image");
                    ad.show(ToolsActivity.this, item -> {
                        runOnUiThread(() -> saveResultCardImage());
                    });
                });
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("Sake gwadawa / Try again");
                    toast("Tallan bai shirya ba. Da fatan za a sake gwadawa daga baya.");
                });
            }
        });
    }

    private void saveResultCardImage() {
        if (lastResultCard == null) {
            toast("Babu sakamakon da za a ajiye.");
            return;
        }
        try {
            int width = Math.max(lastResultCard.getWidth(), dp(320));
            lastResultCard.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int height = lastResultCard.getMeasuredHeight();
            lastResultCard.layout(0, 0, width, height);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            lastResultCard.draw(canvas);

            String filename = "LafiyarMace_" + lastResultTitle.replaceAll("[^A-Za-z0-9]+", "_") + "_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= 29) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LafiyarMace");
            }
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Could not create image file");

            new Thread(() -> {
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null) throw new Exception("Could not open image file");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                    bitmap.recycle();
                    runOnUiThread(() -> toast("An ajiye hoton a Pictures/LafiyarMace / Saved successfully."));
                } catch (Exception e) {
                    runOnUiThread(() -> toast("Kasa ajiye hoto. Da fatan a sake gwadawa."));
                }
            }).start();
        } catch (Exception e) {
            toast("Kasa ajiye hoto. Da fatan a sake gwadawa.");
        }
    }

    // --- PRESERVED API SERVICES ---
    private boolean needLogin() {
        if (token.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Incewar Akanti / Account Required")
                    .setMessage("Shigar da akanti yana taimakawa wajen ajiye bayanan ku a dindindin.\nPlease register or login to sync your tracker data online.")
                    .setNegativeButton("Daga baya / Cancel", null)
                    .setPositiveButton("SHIGA / LOGIN", (d, w) -> {
                        startActivity(new Intent(this, AuthActivity.class));
                    }).show();
            return true;
        }
        return false;
    }

    private void questionsDialog() {
        if (needLogin()) return;
        LinearLayout l = box();

        EditText q = inputCardField(l, "Tambayarki / Your Question", "Rubuta tambayarki a fili...", 0);
        q.setMinLines(4);
        q.setGravity(Gravity.TOP);
        q.setSingleLine(false);

        TextView actionBtn = t("Aikata / Send Question", 15, Color.WHITE, true);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(dp(14), dp(14), dp(14), dp(14));
        actionBtn.setBackground(bgGradient(COLOR_PRIMARY_PINK, COLOR_DEEP_PLUM, 16));
        actionBtn.setMinimumHeight(dp(48));
        l.addView(actionBtn, new LinearLayout.LayoutParams(-1, -2));

        Dialog d = createProDialog("👩‍⚕️", "Ask a Doctor", "Send question to health support team", l);

        actionBtn.setOnClickListener(v -> {
            Map<String, String> m = new HashMap<>();
            m.put("question", q.getText().toString().trim());
            Api.post("questions.php", m, token, new Api.Callback() {
                public void ok(String b) {
                    runOnUiThread(() -> {
                        d.dismiss();
                        loadQuestions();
                    });
                }

                public void err(Exception e) {
                    runOnUiThread(() -> toast("Kasa aika tambaya. / Unable to send."));
                }
            });
        });
        animateTouch(actionBtn);

        d.show();
    }

    private void loadQuestions() {
        Api.get("questions.php", token, new Api.Callback() {
            public void ok(String b) {
                runOnUiThread(() -> {
                    try {
                        JSONArray a = new JSONObject(b).optJSONArray("questions");
                        StringBuilder s = new StringBuilder();
                        if (a != null) {
                            for (int i = 0; i < a.length(); i++) {
                                JSONObject q = a.getJSONObject(i);
                                s.append("Q: ").append(q.optString("question")).append("\n");
                                String ans = q.optString("answer", "");
                                s.append(ans.isEmpty() ? "Status: " + q.optString("status") : "Answer: " + ans).append("\n\n");
                            }
                        }
                        new AlertDialog.Builder(ToolsActivity.this)
                                .setTitle("Tambayoyina / My Doctor Questions")
                                .setMessage(s.length() == 0 ? "Babu tambayoyi tukuna / No questions yet." : s.toString())
                                .setPositiveButton("A'a / OK", null)
                                .show();
                    } catch (Exception e) {
                        toast("Kasa karanta tambayoyi.");
                    }
                });
            }

            public void err(Exception e) {
                runOnUiThread(() -> toast("Kasa loda tambayoyi."));
            }
        });
    }

    private void marketplace() {
        Api.get("marketplace.php", new Api.Callback() {
            public void ok(String b) {
                runOnUiThread(() -> {
                    try {
                        JSONArray a = new JSONObject(b).optJSONArray("products");
                        LinearLayout l = box();
                        ScrollView sc = new ScrollView(ToolsActivity.this);
                        sc.addView(l);
                        if (a != null) {
                            for (int i = 0; i < a.length(); i++) {
                                JSONObject p = a.getJSONObject(i);
                                LinearLayout card = new LinearLayout(ToolsActivity.this);
                                card.setOrientation(LinearLayout.VERTICAL);
                                card.setPadding(dp(12), dp(12), dp(12), dp(12));
                                card.setBackground(strokeBg(Color.WHITE, COLOR_BORDER, 1, 16));

                                ImageView im = new ImageView(ToolsActivity.this);
                                im.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                GradientDrawable ph = bg(COLOR_LIGHT_PINK, 12);
                                im.setBackground(ph);
                                String u = Api.absoluteImageUrl(p.optString("image_url", ""));
                                if (!u.isEmpty()) Glide.with(ToolsActivity.this).load(u).placeholder(ph).into(im);

                                card.addView(im, new LinearLayout.LayoutParams(-1, dp(170)));
                                card.addView(t(p.optString("title"), 17, COLOR_TEXT_MAIN, true));
                                card.addView(t("₦" + p.optString("price"), 16, COLOR_PRIMARY_PINK, true));
                                TextView ds = t(p.optString("description"), 12, COLOR_TEXT_MUTED, false);
                                ds.setPadding(0, dp(4), 0, dp(4));
                                card.addView(ds);
                                card.addView(t("Tuntubi / Contact: " + p.optString("contact", ""), 12, COLOR_TEXT_MAIN, true));

                                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                                cp.setMargins(0, 0, 0, dp(12));
                                l.addView(card, cp);
                            }
                        }
                        Dialog d = createProDialog("🛍", "Marketplace / Kasuwa", "Browse products posted by administrator", sc);
                        d.show();
                    } catch (Exception e) {
                        toast("Kasa loda kasuwa.");
                    }
                });
            }

            public void err(Exception e) {
                runOnUiThread(() -> toast("Kasa loda kasuwa."));
            }
        });
    }

    private void saveTracker(String type, String date, int cycle, String payload, Runnable done) {
        if (token.isEmpty()) {
            if (done != null) done.run();
            return;
        }
        Map<String, String> m = new HashMap<>();
        m.put("action", "save");
        m.put("type", type);
        m.put("date", date);
        m.put("cycle_length", String.valueOf(cycle));
        m.put("payload", payload);
        Api.post("trackers.php", m, token, new Api.Callback() {
            public void ok(String b) {
                if (done != null) done.run();
            }

            public void err(Exception e) {
                if (done != null) done.run();
            }
        });
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
