package com.example.dialertheme;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.VideoProfile;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class InCallActivity extends Activity {

    private AppTheme t;
    private TextView avatar, nameTv, numTv, statusTv;
    private LinearLayout controls;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean finishScheduled = false;

    private final CallManager.Listener listener = new CallManager.Listener() {
        @Override
        public void onChanged() {
            refresh();
        }
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Call c = CallManager.primary();
            if (c != null) statusTv.setText(statusText(c));
            handler.postDelayed(this, 1000);
        }
    };

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        t = AppTheme.current(this);
        Ui.styleWindow(this, t);

        if (CallManager.primary() == null) {
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(t.bg);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        avatar = Ui.text(this, "", 44, t.onPrimary);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.circle(t.primary));
        root.addView(avatar, new LinearLayout.LayoutParams(dp(120), dp(120)));

        nameTv = Ui.text(this, "", 28, t.text);
        nameTv.setTypeface(null, Typeface.BOLD);
        nameTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        np.topMargin = dp(20);
        root.addView(nameTv, np);

        numTv = Ui.text(this, "", 16, t.text2);
        numTv.setGravity(Gravity.CENTER);
        numTv.setTextDirection(View.TEXT_DIRECTION_LTR);
        root.addView(numTv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusTv = Ui.text(this, "", 18, t.primary);
        statusTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(10);
        root.addView(statusTv, sp);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER);
        root.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        CallManager.addListener(listener);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (statusTv != null) {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        CallManager.removeListener(listener);
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void syncAudio() {
        CallService svc = CallManager.service;
        if (svc == null) return;
        CallAudioState as = svc.getCallAudioState();
        if (as != null) {
            CallManager.muted = as.isMuted();
            CallManager.route = as.getRoute();
        }
    }

    private void refresh() {
        if (statusTv == null) return;
        final Call c = CallManager.primary();
        if (c == null) {
            statusTv.setText("تماس پایان یافت");
            controls.removeAllViews();
            if (!finishScheduled) {
                finishScheduled = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 800);
            }
            return;
        }
        syncAudio();
        String title = CallManager.title(this, c);
        String number = CallManager.number(c);
        nameTv.setText(title);
        numTv.setText((number != null && !number.equals(title)) ? number : "");
        avatar.setText(initial(title));
        statusTv.setText(statusText(c));
        buildControls(c);
    }

    private String initial(String title) {
        if (TextUtils.isEmpty(title)) return "#";
        char ch = title.charAt(0);
        return Character.isLetter(ch) ? String.valueOf(ch).toUpperCase() : "#";
    }

    private String statusText(Call c) {
        switch (c.getState()) {
            case Call.STATE_RINGING:
                return "تماس ورودی";
            case Call.STATE_DIALING:
            case Call.STATE_CONNECTING:
                return "در حال برقراری تماس…";
            case Call.STATE_HOLDING:
                return "در انتظار";
            case Call.STATE_ACTIVE:
                long ct = c.getDetails().getConnectTimeMillis();
                long sec = ct > 0 ? (System.currentTimeMillis() - ct) / 1000 : 0;
                if (sec < 0) sec = 0;
                return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
            case Call.STATE_DISCONNECTED:
                return "پایان تماس";
            default:
                return "…";
        }
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER);
        r.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(24);
        r.setLayoutParams(p);
        return r;
    }

    private View circleButton(String symbol, String label, int bg, int fg, View.OnClickListener l) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView c = Ui.text(this, symbol, 26, fg);
        c.setGravity(Gravity.CENTER);
        c.setBackground(Ui.circle(bg));
        c.setOnClickListener(l);
        col.addView(c, new LinearLayout.LayoutParams(dp(68), dp(68)));
        TextView lb = Ui.text(this, label, 13, t.text2);
        lb.setGravity(Gravity.CENTER);
        lb.setPadding(0, dp(6), 0, 0);
        col.addView(lb);
        return col;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private void buildControls(final Call c) {
        controls.removeAllViews();
        int s = c.getState();

        if (s == Call.STATE_RINGING) {
            LinearLayout r = row();
            r.addView(circleButton("✕", "رد", AppTheme.RED, Color.WHITE, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    c.reject(false, null);
                }
            }), weight());
            r.addView(circleButton("📞", "پاسخ", AppTheme.GREEN, Color.WHITE, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    c.answer(VideoProfile.STATE_AUDIO_ONLY);
                }
            }), weight());
            controls.addView(r);
            return;
        }

        boolean muted = CallManager.muted;
        boolean spk = CallManager.route == CallAudioState.ROUTE_SPEAKER;
        boolean hold = s == Call.STATE_HOLDING;

        LinearLayout r = row();
        r.addView(circleButton("🔇", "بی‌صدا",
                muted ? t.primary : t.surface, muted ? t.onPrimary : t.text,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CallService svc = CallManager.service;
                        if (svc != null) svc.setMuted(!CallManager.muted);
                    }
                }), weight());
        r.addView(circleButton("🔊", "بلندگو",
                spk ? t.primary : t.surface, spk ? t.onPrimary : t.text,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CallService svc = CallManager.service;
                        if (svc != null) {
                            boolean on = CallManager.route == CallAudioState.ROUTE_SPEAKER;
                            svc.setAudioRoute(on ? CallAudioState.ROUTE_WIRED_OR_EARPIECE
                                    : CallAudioState.ROUTE_SPEAKER);
                        }
                    }
                }), weight());
        if (s == Call.STATE_ACTIVE || s == Call.STATE_HOLDING) {
            r.addView(circleButton("❚❚", "نگه‌داشتن",
                    hold ? t.primary : t.surface, hold ? t.onPrimary : t.text,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (c.getState() == Call.STATE_HOLDING) c.unhold();
                            else c.hold();
                        }
                    }), weight());
        }
        controls.addView(r);

        LinearLayout end = row();
        end.addView(circleButton("✕", "پایان تماس", AppTheme.RED, Color.WHITE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        c.disconnect();
                    }
                }), weight());
        controls.addView(end);
    }
}
