package com.example.dialertheme;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.telecom.TelecomManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final String[] PERMS = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG};

    private AppTheme t;
    private FrameLayout content;
    private final TextView[] tabViews = new TextView[4];
    private int tab = 0;
    private String dialNumber = "";
    private String pendingCall;
    private TextView dialDisplay;

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private boolean has(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        t = AppTheme.current(this);
        Ui.styleWindow(this, t);
        if (b != null) {
            tab = b.getInt("tab", 0);
            dialNumber = b.getString("dial", "");
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(t.bg);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = Ui.text(this, "مخاطبین و تماس", 20, t.primary);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(16), dp(14), dp(16), dp(8));
        root.addView(title);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setBackgroundColor(t.surface);
        String[] names = {"مخاطبین", "شماره‌گیر", "تماس‌ها", "تنظیمات"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView tv = Ui.text(this, names[i], 14, t.text2);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, dp(14), 0, dp(14));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTab(idx);
                }
            });
            bar.addView(tv, new LinearLayout.LayoutParams(0, WRAP, 1f));
            tabViews[i] = tv;
        }
        root.addView(bar);
        setContentView(root);

        requestMissingPermissions();
        handleIntent(getIntent());
        showTab(tab);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
        out.putString("dial", dialNumber);
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleIntent(i);
        showTab(tab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null && tab == 3) showTab(3);
    }

    private void handleIntent(Intent i) {
        if (i == null || i.getBooleanExtra("handled", false)) return;
        Uri d = i.getData();
        if (d != null && "tel".equals(d.getScheme())) {
            dialNumber = Uri.decode(d.getSchemeSpecificPart());
            tab = 1;
            i.putExtra("handled", true);
        }
    }

    private void requestMissingPermissions() {
        List<String> need = new ArrayList<>();
        for (String p : PERMS) if (!has(p)) need.add(p);
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 1);
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] results) {
        super.onRequestPermissionsResult(rc, perms, results);
        showTab(tab);
        if (pendingCall != null && has(Manifest.permission.CALL_PHONE)) {
            String n = pendingCall;
            pendingCall = null;
            placeCall(n);
        }
    }

    private void showTab(int i) {
        tab = i;
        content.removeAllViews();
        View v;
        switch (i) {
            case 0: v = buildContacts(); break;
            case 1: v = buildKeypad(); break;
            case 2: v = buildRecents(); break;
            default: v = buildSettings(); break;
        }
        content.addView(v, new FrameLayout.LayoutParams(MATCH, MATCH));
        for (int k = 0; k < 4; k++) {
            tabViews[k].setTextColor(k == i ? t.primary : t.text2);
            tabViews[k].setTypeface(null, k == i ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    // ---------------------------------------------------------------- actions

    private void placeCall(String number) {
        if (TextUtils.isEmpty(number)) return;
        if (!has(Manifest.permission.CALL_PHONE)) {
            pendingCall = number;
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number.trim(), null)));
        } catch (Exception e) {
            Toast.makeText(this, "برقراری تماس ممکن نشد", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSms(String number) {
        try {
            startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))));
        } catch (Exception e) {
            Toast.makeText(this, "برنامه پیامک پیدا نشد", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDefaultDialer() {
        TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
        return tm != null && getPackageName().equals(tm.getDefaultDialerPackage());
    }

    private void requestDefaultDialer() {
        try {
            Intent i;
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager rm = (RoleManager) getSystemService(ROLE_SERVICE);
                i = rm.createRequestRoleIntent(RoleManager.ROLE_DIALER);
            } else {
                i = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
                i.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
            }
            startActivityForResult(i, 2);
        } catch (Exception e) {
            Toast.makeText(this, "این گوشی اجازه تغییر شماره‌گیر را از اینجا نمی‌دهد", Toast.LENGTH_LONG).show();
        }
    }

    private View message(String text, String btn, View.OnClickListener l) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        TextView tv = Ui.text(this, text, 15, t.text2);
        tv.setGravity(Gravity.CENTER);
        box.addView(tv);
        if (btn != null) {
            TextView b = Ui.pill(this, btn, t.primary, t.onPrimary);
            b.setOnClickListener(l);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(WRAP, WRAP);
            p.topMargin = dp(16);
            box.addView(b, p);
        }
        return box;
    }

    // --------------------------------------------------------------- contacts

    private View buildContacts() {
        if (!has(Manifest.permission.READ_CONTACTS)) {
            return message("برای نمایش مخاطبین، اجازه دسترسی لازم است.", "اجازه دادن",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestMissingPermissions();
                        }
                    });
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        EditText search = new EditText(this);
        search.setHint("جستجو در مخاطبین");
        search.setHintTextColor(t.text2);
        search.setTextColor(t.text);
        search.setSingleLine(true);
        search.setBackground(Ui.rect(this, t.surface, 24));
        search.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(MATCH, WRAP);
        sp.setMargins(dp(12), dp(4), dp(12), dp(8));
        box.addView(search, sp);

        ListView lv = new ListView(this);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        box.addView(lv, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        final ContactAdapter ad = new ContactAdapter();
        lv.setAdapter(ad);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                ContactUtil.Contact c = (ContactUtil.Contact) ad.getItem(pos);
                placeCall(c.number);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ContactUtil.Contact> l = ContactUtil.loadAll(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ad.setData(l);
                    }
                });
            }
        }).start();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                ad.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable e) {
            }
        });
        return box;
    }

    private static class ContactHolder {
        TextView avatar, name, number, sms;
    }

    private class ContactAdapter extends BaseAdapter {
        private List<ContactUtil.Contact> all = new ArrayList<>();
        private List<ContactUtil.Contact> shown = new ArrayList<>();
        private String q = "";

        void setData(List<ContactUtil.Contact> l) {
            all = l;
            filter(q);
        }

        void filter(String query) {
            q = query == null ? "" : query.trim().toLowerCase();
            String qd = q.replaceAll("[^0-9+]", "");
            List<ContactUtil.Contact> out = new ArrayList<>();
            for (ContactUtil.Contact c : all) {
                boolean ok = q.isEmpty()
                        || c.name.toLowerCase().contains(q)
                        || (!qd.isEmpty() && c.number.replaceAll("[^0-9+]", "").contains(qd));
                if (ok) out.add(c);
            }
            shown = out;
            notifyDataSetChanged();
        }

        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            ContactHolder h;
            if (cv == null) {
                h = new ContactHolder();
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(8), dp(16), dp(8));

                h.avatar = Ui.text(MainActivity.this, "", 18, t.onPrimary);
                h.avatar.setGravity(Gravity.CENTER);
                h.avatar.setBackground(Ui.circle(t.primary));
                row.addView(h.avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));

                LinearLayout col = new LinearLayout(MainActivity.this);
                col.setOrientation(LinearLayout.VERTICAL);
                h.name = Ui.text(MainActivity.this, "", 16, t.text);
                h.name.setSingleLine(true);
                h.name.setEllipsize(TextUtils.TruncateAt.END);
                h.number = Ui.text(MainActivity.this, "", 13, t.text2);
                h.number.setSingleLine(true);
                h.number.setTextDirection(View.TEXT_DIRECTION_LTR);
                col.addView(h.name);
                col.addView(h.number);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, WRAP, 1f);
                cp.setMarginStart(dp(12));
                row.addView(col, cp);

                h.sms = Ui.text(MainActivity.this, "✉", 20, t.primary);
                h.sms.setPadding(dp(12), dp(8), dp(12), dp(8));
                h.sms.setFocusable(false);
                row.addView(h.sms);

                row.setTag(h);
                cv = row;
            } else {
                h = (ContactHolder) cv.getTag();
            }
            final ContactUtil.Contact c = shown.get(pos);
            h.avatar.setText(c.name.isEmpty() ? "#" : c.name.substring(0, 1).toUpperCase());
            h.name.setText(c.name);
            h.number.setText(c.number);
            h.sms.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendSms(c.number);
                }
            });
            return cv;
        }
    }

    // ----------------------------------------------------------------- keypad

    private void updateDial() {
        if (dialDisplay != null) dialDisplay.setText(dialNumber);
    }

    private View keyView(final String k) {
        TextView v = Ui.text(this, k, 28, t.text);
        v.setGravity(Gravity.CENTER);
        v.setBackground(Ui.circle(t.surface));
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialNumber += k;
                updateDial();
            }
        });
        if (k.equals("0")) {
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    dialNumber += "+";
                    updateDial();
                    return true;
                }
            });
        }
        return v;
    }

    private LinearLayout.LayoutParams keyLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(64), dp(64));
        p.setMargins(dp(8), dp(6), dp(8), dp(6));
        return p;
    }

    private View buildKeypad() {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        box.setPadding(dp(16), dp(8), dp(16), dp(16));

        dialDisplay = Ui.text(this, dialNumber, 32, t.text);
        dialDisplay.setGravity(Gravity.CENTER);
        dialDisplay.setSingleLine(true);
        dialDisplay.setEllipsize(TextUtils.TruncateAt.START);
        dialDisplay.setMinHeight(dp(60));
        box.addView(dialDisplay, new LinearLayout.LayoutParams(MATCH, WRAP));

        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"*", "0", "#"}};
        for (String[] rowKeys : keys) {
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER);
            for (String k : rowKeys) r.addView(keyView(k), keyLp());
            box.addView(r);
        }

        LinearLayout last = new LinearLayout(this);
        last.setGravity(Gravity.CENTER);
        last.addView(new View(this), keyLp());

        TextView call = Ui.text(this, "📞", 28, Color.WHITE);
        call.setGravity(Gravity.CENTER);
        call.setBackground(Ui.circle(AppTheme.GREEN));
        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeCall(dialNumber);
            }
        });
        last.addView(call, keyLp());

        TextView back = Ui.text(this, "←", 28, t.text2);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dialNumber.isEmpty()) {
                    dialNumber = dialNumber.substring(0, dialNumber.length() - 1);
                    updateDial();
                }
            }
        });
        back.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialNumber = "";
                updateDial();
                return true;
            }
        });
        last.addView(back, keyLp());
        box.addView(last);

        sv.addView(box, new ScrollView.LayoutParams(MATCH, WRAP));
        return sv;
    }

    // ---------------------------------------------------------------- recents

    private static class Recent {
        String number, name;
        int type;
        long date, duration;
    }

    private static class RecentHolder {
        TextView icon, title, sub;
    }

    private static String tail(String n) {
        String d = n.replaceAll("[^0-9]", "");
        return d.length() > 9 ? d.substring(d.length() - 9) : d;
    }

    private View buildRecents() {
        if (!has(Manifest.permission.READ_CALL_LOG)) {
            return message("برای نمایش تاریخچه تماس‌ها، اجازه دسترسی لازم است.", "اجازه دادن",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestMissingPermissions();
                        }
                    });
        }
        ListView lv = new ListView(this);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        final RecentAdapter ad = new RecentAdapter();
        lv.setAdapter(ad);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Recent r = (Recent) ad.getItem(pos);
                placeCall(r.number);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Recent> out = new ArrayList<>();
                Map<String, String> names = new HashMap<>();
                if (has(Manifest.permission.READ_CONTACTS)) {
                    for (ContactUtil.Contact c : ContactUtil.loadAll(MainActivity.this)) {
                        names.put(tail(c.number), c.name);
                    }
                }
                Cursor cur = null;
                try {
                    cur = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                            new String[]{CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                                    CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION},
                            null, null, CallLog.Calls.DATE + " DESC");
                    while (cur != null && cur.moveToNext() && out.size() < 200) {
                        Recent r = new Recent();
                        r.number = cur.getString(0);
                        String cn = cur.getString(1);
                        r.type = cur.getInt(2);
                        r.date = cur.getLong(3);
                        r.duration = cur.getLong(4);
                        if (!TextUtils.isEmpty(cn)) r.name = cn;
                        else if (!TextUtils.isEmpty(r.number)) r.name = names.get(tail(r.number));
                        out.add(r);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (cur != null) cur.close();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ad.setData(out);
                    }
                });
            }
        }).start();
        return lv;
    }

    private class RecentAdapter extends BaseAdapter {
        private List<Recent> data = new ArrayList<>();
        private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

        void setData(List<Recent> l) {
            data = l;
            notifyDataSetChanged();
        }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int i) { return data.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            RecentHolder h;
            if (cv == null) {
                h = new RecentHolder();
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(10), dp(16), dp(10));

                h.icon = Ui.text(MainActivity.this, "", 22, t.text);
                h.icon.setGravity(Gravity.CENTER);
                row.addView(h.icon, new LinearLayout.LayoutParams(dp(36), WRAP));

                LinearLayout col = new LinearLayout(MainActivity.this);
                col.setOrientation(LinearLayout.VERTICAL);
                h.title = Ui.text(MainActivity.this, "", 16, t.text);
                h.title.setSingleLine(true);
                h.title.setEllipsize(TextUtils.TruncateAt.END);
                h.sub = Ui.text(MainActivity.this, "", 13, t.text2);
                h.sub.setSingleLine(true);
                col.addView(h.title);
                col.addView(h.sub);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, WRAP, 1f);
                cp.setMarginStart(dp(8));
                row.addView(col, cp);

                row.setTag(h);
                cv = row;
            } else {
                h = (RecentHolder) cv.getTag();
            }
            Recent r = data.get(pos);
            boolean missed = r.type == 3 || r.type == 5;
            String title = !TextUtils.isEmpty(r.name) ? r.name
                    : (!TextUtils.isEmpty(r.number) ? r.number : "ناشناس");
            String typeLabel;
            switch (r.type) {
                case 1: typeLabel = "ورودی"; break;
                case 2: typeLabel = "خروجی"; break;
                case 3: typeLabel = "از دست رفته"; break;
                case 5: typeLabel = "رد شده"; break;
                default: typeLabel = "";
            }
            String sub = typeLabel + " • " + fmt.format(new Date(r.date));
            if (r.duration > 0) {
                sub += String.format(Locale.US, " • %d:%02d", r.duration / 60, r.duration % 60);
            }
            h.icon.setText(r.type == 2 ? "↗" : "↙");
            h.icon.setTextColor(missed ? AppTheme.RED : (r.type == 2 ? t.primary : AppTheme.GREEN));
            h.title.setText(title);
            h.title.setTextColor(missed ? AppTheme.RED : t.text);
            h.sub.setText(sub);
            return cv;
        }
    }

    // --------------------------------------------------------------- settings

    private TextView header(String s) {
        TextView tv = Ui.text(this, s, 16, t.primary);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(20), 0, dp(8));
        return tv;
    }

    private View buildSettings() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), 0, dp(16), dp(24));

        box.addView(header("شماره‌گیر پیش‌فرض"));
        boolean def = isDefaultDialer();
        TextView st = Ui.text(this, def
                ? "این برنامه شماره‌گیر پیش‌فرض است ✓ صفحه تماس ورودی و خروجی با تم شما نمایش داده می‌شود."
                : "برای اینکه صفحه تماس ورودی و خروجی هم تم شما را داشته باشد، این برنامه باید شماره‌گیر پیش‌فرض باشد.",
                14, t.text2);
        box.addView(st);
        if (!def) {
            TextView b = Ui.pill(this, "تنظیم به‌عنوان شماره‌گیر پیش‌فرض", t.primary, t.onPrimary);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestDefaultDialer();
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
            p.topMargin = dp(10);
            box.addView(b, p);
        }

        boolean missingPerm = false;
        for (String p : PERMS) if (!has(p)) missingPerm = true;
        if (missingPerm) {
            TextView b = Ui.pill(this, "درخواست اجازه‌های لازم", t.surface, t.text);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestMissingPermissions();
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
            p.topMargin = dp(10);
            box.addView(b, p);
        }

        box.addView(header("تم رنگی"));
        int cur = AppTheme.index(this);
        for (int i = 0; i < AppTheme.ALL.length; i++) {
            final int idx = i;
            AppTheme th = AppTheme.ALL[i];
            boolean sel = cur == i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable bg = Ui.rect(this, th.bg, 14);
            bg.setStroke(dp(sel ? 3 : 1), sel ? th.primary : th.text2);
            row.setBackground(bg);

            TextView nm = Ui.text(this, th.name + (sel ? "  ✓" : ""), 16, th.text);
            row.addView(nm, new LinearLayout.LayoutParams(0, WRAP, 1f));
            int[] sw = {th.surface, th.primary, th.text2};
            for (int col : sw) {
                View d = new View(this);
                d.setBackground(Ui.circle(col));
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(22), dp(22));
                dl.setMarginStart(dp(6));
                row.addView(d, dl);
            }
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppTheme.setCurrent(MainActivity.this, idx);
                    recreate();
                }
            });
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(MATCH, WRAP);
            rp.bottomMargin = dp(8);
            box.addView(row, rp);
        }

        box.addView(header("اندازه متن"));
        LinearLayout sizes = new LinearLayout(this);
        String[] labels = {"کوچک", "متوسط", "بزرگ", "خیلی بزرگ"};
        int curSize = AppTheme.sizeIndex(this);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean sel = curSize == i;
            TextView chip = Ui.pill(this, labels[i],
                    sel ? t.primary : t.surface, sel ? t.onPrimary : t.text);
            chip.setPadding(dp(4), dp(10), dp(4), dp(10));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppTheme.setSize(MainActivity.this, idx);
                    recreate();
                }
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            cp.setMargins(dp(3), 0, dp(3), 0);
            sizes.addView(chip, cp);
        }
        box.addView(sizes, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView tip = Ui.text(this,
                "برای برگشتن به شماره‌گیر قبلی: تنظیمات اندروید ← برنامه‌ها ← برنامه‌های پیش‌فرض ← تلفن.",
                13, t.text2);
        tip.setPadding(0, dp(24), 0, 0);
        box.addView(tip);

        sv.addView(box, new ScrollView.LayoutParams(MATCH, WRAP));
        return sv;
    }
}
