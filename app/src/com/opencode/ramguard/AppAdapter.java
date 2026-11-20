package com.opencode.ramguard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.List;

public class AppAdapter extends BaseAdapter {
    public static class Item {
        public final String label;
        public final String pkg;
        public final Drawable icon;
        public boolean checked;
        public Item(String label, String pkg, Drawable icon, boolean checked) {
            this.label = label; this.pkg = pkg; this.icon = icon; this.checked = checked;
        }
    }

    private final Context ctx;
    private final List<Item> items;
    private final boolean multi;

    public AppAdapter(Context ctx, List<Item> items, boolean multi) {
        this.ctx = ctx; this.items = items; this.multi = multi;
    }

    @Override public int getCount() { return items.size(); }
    @Override public Object getItem(int pos) { return items.get(pos); }
    @Override public long getItemId(int pos) { return pos; }

    @Override public View getView(int pos, View convert, ViewGroup parent) {
        View v = convert;
        if (v == null) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));

            ImageView iv = new ImageView(ctx);
            iv.setId(1);
            iv.setAdjustViewBounds(true);
            iv.setMaxWidth(dp(40));
            iv.setMaxHeight(dp(40));
            iv.setPadding(0, 0, dp(12), 0);
            row.addView(iv);

            LinearLayout texts = new LinearLayout(ctx);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView label = new TextView(ctx);
            label.setId(2);
            label.setTextColor(Color.parseColor("#F2F4FF"));
            label.setTextSize(15);
            TextView pkg = new TextView(ctx);
            pkg.setId(3);
            pkg.setTextColor(Color.parseColor("#9AA3B2"));
            pkg.setTextSize(11);
            texts.addView(label);
            texts.addView(pkg);
            row.addView(texts, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            if (multi) {
                CheckBox cb = new CheckBox(ctx);
                cb.setId(4);
                cb.setClickable(false);
                cb.setFocusable(false);
                row.addView(cb);
            }
            v = row;
        }

        final Item it = items.get(pos);
        ((ImageView) v.findViewById(1)).setImageDrawable(it.icon);
        ((TextView) v.findViewById(2)).setText(it.label);
        ((TextView) v.findViewById(3)).setText(it.pkg);
        if (multi) {
            ((CheckBox) v.findViewById(4)).setChecked(it.checked);
            v.setOnClickListener(x -> {
                it.checked = !it.checked;
                notifyDataSetChanged();
            });
        }
        return v;
    }

    private int dp(int value) {
        return (int) (value * ctx.getResources().getDisplayMetrics().density);
    }
}
