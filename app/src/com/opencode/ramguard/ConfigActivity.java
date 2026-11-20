package com.opencode.ramguard;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ConfigActivity extends Activity {
    private GuardConfig cfg = new GuardConfig();
    private TextView tvTarget, tvInterval, tvKills, tvChips, tvSkipScope, tvSkipMode;
    private boolean showSystem = false;

    private static final java.util.Set<String> CRITICAL = new java.util.HashSet<>(java.util.Arrays.asList(
        "com.google.android.gms", "app.revanced.android.gms", "com.google.android.play.games",
        "com.topjohnwu.magisk", "org.lsposed.manager", "org.adaway", "org.fdroid.fdroid",
        "com.opencode.rewardskip", "xmnh.soulfrog", "com.coderstory.toolkit",
        "ru.ljflcuer.fkmzwcfgi", "inc.whew.android.fakegapps", "com.opencode.ramguard"
    ));

    private static final String BG      = "#0F1117";
    private static final String CARD    = "#1C2030";
    private static final String ACCENT  = "#6C7BFF";
    private static final String MUTED   = "#9AA3B2";
    private static final String WHITE   = "#F2F4FF";
    private static final String GREEN   = "#22C55E";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        cfg.load();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(BG));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p, p, p);

        root.addView(header());
        root.addView(card("TARGET GAME", "The app that triggers memory cleanup", "CHANGE GAME", v -> pickGame(), tvTarget = valueView(18, WHITE)));
        root.addView(card("CHECK INTERVAL", "How often the guard checks the foreground app", "CHANGE", v -> pickInterval(), tvInterval = valueView(18, WHITE)));
        root.addView(card("BACKGROUND APPS", "Force-stopped when the game opens", "EDIT LIST", v -> editKills(), tvKills = valueView(18, WHITE)));
        root.addView(chipsCard());
        root.addView(skipRewardCard());
        root.addView(skipModeCard());
        root.addView(saveButton());

        TextView hint = new TextView(this);
        hint.setText("Changes apply automatically — the daemon reloads the config.");
        hint.setTextColor(Color.parseColor(MUTED));
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(14), 0, dp(10));
        root.addView(hint);

        scroll.addView(root);
        setContentView(scroll);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); cfg.load(); refresh(); }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private TextView valueView(int size, String color) {
        TextView t = new TextView(this);
        t.setTextSize(size);
        t.setTextColor(Color.parseColor(color));
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return t;
    }

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#3B4DFF"), Color.parseColor("#8B5CF6")});
        gd.setCornerRadius(dp(22));
        h.setBackground(gd);
        h.setPadding(dp(22), dp(22), dp(22), dp(22));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView status = new TextView(this);
        status.setText("●  SERVICE ACTIVE");
        status.setTextColor(Color.WHITE);
        status.setTextSize(11);
        status.setLetterSpacing(1.2f);
        top.addView(status, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button more = new Button(this);
        more.setText("⋮");
        more.setTextColor(Color.WHITE);
        more.setTextSize(20);
        more.setBackgroundColor(Color.TRANSPARENT);
        more.setPadding(0, 0, 0, 0);
        more.setOnClickListener(v -> {
            android.widget.PopupMenu pm = new android.widget.PopupMenu(this, v);
            pm.getMenu().add(0, 1, 0, showSystem
                    ? "Hide system & critical apps" : "Show system & critical apps");
            pm.setOnMenuItemClickListener(item -> {
                showSystem = !showSystem;
                refresh();
                Toast.makeText(this, showSystem ? "Showing all apps" : "Showing user apps only",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
            pm.show();
        });
        top.addView(more);
        h.addView(top);

        TextView title = new TextView(this);
        title.setText("RamGuard");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(6), 0, 0);
        h.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Game memory saver · keep your game alive");
        sub.setTextColor(Color.parseColor("#E0E7FF"));
        sub.setTextSize(13);
        h.addView(sub);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(18);
        h.setLayoutParams(lp);
        return h;
    }

    private View card(String label, String desc, String action, View.OnClickListener click, TextView value) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(CARD));
        gd.setCornerRadius(dp(18));
        c.setBackground(gd);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(Color.parseColor(ACCENT));
        l.setTextSize(11);
        l.setTypeface(null, Typeface.BOLD);
        l.setLetterSpacing(1f);
        c.addView(l);

        value.setPadding(0, dp(6), 0, 0);
        c.addView(value);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor(MUTED));
        d.setTextSize(11);
        c.addView(d);

        Button b = new Button(this);
        b.setText(action);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2A3050"));
        bg.setCornerRadius(dp(10));
        b.setBackground(bg);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(12);
        b.setLayoutParams(bp);
        c.addView(b);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(12);
        c.setLayoutParams(cp);
        return c;
    }

    private View chipsCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(CARD));
        gd.setCornerRadius(dp(18));
        c.setBackground(gd);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView l = new TextView(this);
        l.setText("CURRENT KILL LIST");
        l.setTextColor(Color.parseColor(ACCENT));
        l.setTextSize(11);
        l.setTypeface(null, Typeface.BOLD);
        l.setLetterSpacing(1f);
        c.addView(l);

        tvChips = new TextView(this);
        tvChips.setTextColor(Color.parseColor(MUTED));
        tvChips.setTextSize(12);
        tvChips.setPadding(0, dp(8), 0, 0);
        c.addView(tvChips);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(12);
        c.setLayoutParams(cp);
        return c;
    }

    private View skipRewardCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(CARD));
        gd.setCornerRadius(dp(18));
        c.setBackground(gd);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView l = new TextView(this);
        l.setText("REWARDSKIP · GAME SCOPE");
        l.setTextColor(Color.parseColor(ACCENT));
        l.setTextSize(11);
        l.setTypeface(null, Typeface.BOLD);
        l.setLetterSpacing(1f);
        c.addView(l);

        tvSkipScope = new TextView(this);
        tvSkipScope.setTextColor(Color.parseColor(MUTED));
        tvSkipScope.setTextSize(12);
        tvSkipScope.setPadding(0, dp(8), 0, 0);
        c.addView(tvSkipScope);

        Button b = new Button(this);
        b.setText("ADD GAME TO REWARDSKIP");
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2A3050"));
        bg.setCornerRadius(dp(10));
        b.setBackground(bg);
        b.setOnClickListener(v -> pickSkipGame());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(12);
        b.setLayoutParams(bp);
        c.addView(b);

        Button rm = new Button(this);
        rm.setText("REMOVE GAME FROM REWARDSKIP");
        rm.setTextColor(Color.WHITE);
        rm.setTextSize(12);
        rm.setAllCaps(true);
        GradientDrawable rbg = new GradientDrawable();
        rbg.setColor(Color.parseColor("#3A2540"));
        rbg.setCornerRadius(dp(10));
        rm.setBackground(rbg);
        rm.setOnClickListener(v -> pickSkipGameRemove());
        rm.setLayoutParams(bp);
        c.addView(rm);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(12);
        c.setLayoutParams(cp);
        return c;
    }

    private void loadSkipScope() {
        java.io.File f = new java.io.File("/data/data/com.opencode.ramguard/files/rewardskip_scope.txt");
        if (!f.exists()) { tvSkipScope.setText("RewardSkip not detected"); return; }
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
            if (lines.isEmpty()) { tvSkipScope.setText("No games in scope"); return; }
            String status = lines.get(0);
            if (status.contains("NOT_INSTALLED")) { tvSkipScope.setText("RewardSkip not detected"); return; }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < lines.size(); i++) {
                String p = lines.get(i).trim();
                if (!p.isEmpty()) sb.append("• ").append(p).append("\n");
            }
            tvSkipScope.setText(sb.length() == 0 ? "No games in scope" : sb.toString().trim());
        } catch (Exception e) {
            tvSkipScope.setText("Could not read scope");
        }
    }

    private void pickSkipGame() {
        final List<AppAdapter.Item> p = installedApps();
        if (p.isEmpty()) { Toast.makeText(this, "No apps found", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this)
            .setTitle("Add game to RewardSkip")
            .setAdapter(new AppAdapter(this, p, false), (d, w) -> {
                String pkg = p.get(w).pkg;
                try {
                    java.io.FileOutputStream out = openFileOutput("scope_request.txt", MODE_PRIVATE);
                    out.write(pkg.getBytes("UTF-8"));
                    out.close();
                    Toast.makeText(this, "Requested: " + p.get(w).label, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                d.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private String skipMode() {
        try {
            java.io.File f = new java.io.File("/data/data/com.opencode.ramguard/files/skip_mode.txt");
            if (f.exists()) {
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
                String s = r.readLine(); r.close();
                if (s != null && s.trim().equals("delayed")) return "DELAYED (~5s, lower detection)";
            }
        } catch (Exception ignore) {}
        return "INSTANT (skip ad completely)";
    }

    private View skipModeCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(CARD));
        gd.setCornerRadius(dp(18));
        c.setBackground(gd);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView l = new TextView(this);
        l.setText("REWARDSKIP MODE");
        l.setTextColor(Color.parseColor(ACCENT));
        l.setTextSize(11);
        l.setTypeface(null, Typeface.BOLD);
        l.setLetterSpacing(1f);
        c.addView(l);

        tvSkipMode = new TextView(this);
        tvSkipMode.setTextColor(Color.parseColor(WHITE));
        tvSkipMode.setTextSize(14);
        tvSkipMode.setPadding(0, dp(8), 0, 0);
        c.addView(tvSkipMode);

        Button b = new Button(this);
        b.setText("SWITCH MODE");
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2A3050"));
        bg.setCornerRadius(dp(10));
        b.setBackground(bg);
        b.setOnClickListener(v -> {
            String cur = skipMode();
            String next = cur.startsWith("DELAYED") ? "instant" : "delayed";
            try {
                java.io.FileOutputStream out = openFileOutput("skip_mode.txt", MODE_PRIVATE);
                out.write(next.getBytes("UTF-8")); out.close();
                tvSkipMode.setText(skipMode());
                Toast.makeText(this, "Mode: " + next + " — restart the game to apply",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(12);
        b.setLayoutParams(bp);
        c.addView(b);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dp(12);
        c.setLayoutParams(cp);
        return c;
    }

    private void pickSkipGameRemove() {
        java.io.File f = new java.io.File("/data/data/com.opencode.ramguard/files/rewardskip_scope.txt");
        final java.util.List<String> pkgs = new java.util.ArrayList<>();
        try {
            for (String line : java.nio.file.Files.readAllLines(f.toPath())) {
                if (line.startsWith("STATUS=")) continue;
                if (!line.trim().isEmpty()) pkgs.add(line.trim());
            }
        } catch (Exception ignore) {}
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "No games in RewardSkip scope", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] arr = pkgs.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
            .setTitle("Remove game from RewardSkip")
            .setSingleChoiceItems(arr, -1, (d, w) -> {
                String pkg = pkgs.get(w);
                try {
                    java.io.FileOutputStream out = openFileOutput("scope_remove.txt", MODE_PRIVATE);
                    out.write(pkg.getBytes("UTF-8"));
                    out.close();
                    Toast.makeText(this, "Removing: " + pkg, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                d.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private View saveButton() {
        Button b = new Button(this);
        b.setText("SAVE SETTINGS");
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(null, Typeface.BOLD);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#3B4DFF"), Color.parseColor("#7C3AED")});
        gd.setCornerRadius(dp(14));
        b.setBackground(gd);
        b.setOnClickListener(v -> {
            cfg.save();
            Toast.makeText(this, "Saved — applied automatically", Toast.LENGTH_SHORT).show();
        });
        return b;
    }

    private void refresh() {
        tvTarget.setText(cfg.target);
        tvInterval.setText(cfg.interval + " seconds");
        tvKills.setText(cfg.kills.size() + " app(s)");
        if (cfg.kills.isEmpty()) {
            tvChips.setText("None — tap EDIT LIST to add apps");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String k : cfg.kills) sb.append("• ").append(k).append("\n");
            tvChips.setText(sb.toString().trim());
        }
        loadSkipScope();
        tvSkipMode.setText(skipMode());
    }

    private List<AppAdapter.Item> installedApps() {
        List<AppAdapter.Item> out = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (PackageInfo pi : pm.getInstalledPackages(0)) {
            ApplicationInfo ai = pi.applicationInfo;
            if (ai == null) continue;
            if (!showSystem) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (CRITICAL.contains(pi.packageName)) continue;
            }
            String label = pi.packageName;
            try { label = pm.getApplicationLabel(ai).toString(); } catch (Exception ignore) {}
            out.add(new AppAdapter.Item(label, pi.packageName, pm.getApplicationIcon(ai),
                    cfg.kills.contains(pi.packageName)));
        }
        Collections.sort(out, (a, b) -> {
            int c = a.label.compareToIgnoreCase(b.label);
            return c != 0 ? c : a.pkg.compareToIgnoreCase(b.pkg);
        });
        return out;
    }

    private void pickGame() {
        final List<AppAdapter.Item> p = installedApps();
        if (p.isEmpty()) { Toast.makeText(this, "No apps found", Toast.LENGTH_SHORT).show(); return; }
        AppAdapter adapter = new AppAdapter(this, p, false);
        new AlertDialog.Builder(this)
            .setTitle("Select game to guard")
            .setAdapter(adapter, (d, w) -> {
                cfg.target = p.get(w).pkg;
                refresh();
                d.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void pickInterval() {
        final int[] v = {1, 2, 3, 5, 10};
        new AlertDialog.Builder(this).setTitle("Check interval")
            .setSingleChoiceItems(new CharSequence[]{"1 s", "2 s", "3 s", "5 s", "10 s"},
                Math.max(0, Arrays.binarySearch(v, cfg.interval)),
                (d, w) -> { cfg.interval = v[w]; refresh(); d.dismiss(); })
            .setNegativeButton("Cancel", null).show();
    }

    private void editKills() {
        final List<AppAdapter.Item> p = installedApps();
        if (p.isEmpty()) { Toast.makeText(this, "No apps found", Toast.LENGTH_SHORT).show(); return; }
        AppAdapter adapter = new AppAdapter(this, p, true);
        new AlertDialog.Builder(this)
            .setTitle("Apps to close when game opens")
            .setAdapter(adapter, null)
            .setPositiveButton("OK", (d, w) -> {
                cfg.kills.clear();
                for (AppAdapter.Item it : p) if (it.checked) cfg.kills.add(it.pkg);
                refresh();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
