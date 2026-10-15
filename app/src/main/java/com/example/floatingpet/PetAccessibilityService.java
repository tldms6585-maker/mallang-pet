package com.example.floatingpet;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.app.KeyguardManager;
import android.os.PowerManager;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.*;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;

import java.util.List;

public class PetAccessibilityService extends AccessibilityService {

    static PetAccessibilityService instance;

    private WindowManager wm;
    private ImageView pet;
    private WindowManager.LayoutParams lp;

    private final Handler handler =
        new Handler(Looper.getMainLooper());

    private int index = 0;
    private int currentSlot = 0;
    private Runnable reset;

    private boolean receiverRegistered = false;
    private boolean videoMode = false;

    private final Runnable checkWindows = new Runnable() {
        @Override
        public void run() {
            checkVideoMode();
        }
    };

    private SharedPreferences prefs() {
        return getSharedPreferences("pet", MODE_PRIVATE);
    }

    private int dp(float x) {
        return (int) (
            x * getResources().getDisplayMetrics().density + .5f
        );
    }

    private boolean screenUnlocked() {

        PowerManager pm =
            (PowerManager) getSystemService(POWER_SERVICE);

        KeyguardManager km =
            (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        return pm != null
            && pm.isInteractive()
            && km != null
            && !km.isKeyguardLocked();
    }

    private boolean shouldShowPet() {
        return prefs().getBoolean("visible", true)
            && screenUnlocked()
            && !videoMode;
    }

    private void updatePetVisibility() {

        if (pet == null) return;

        int visibility = shouldShowPet()
            ? View.VISIBLE
            : View.GONE;

        if (pet.getVisibility() != visibility) {
            pet.setVisibility(visibility);
        }
    }

    private boolean isVideoApp(String pkg) {

        if (pkg == null) return false;

        return pkg.equals("com.google.android.youtube")
            || pkg.equals("com.android.chrome")
            || pkg.equals("com.wavve.android");
    }

    private boolean isFullscreenWindow(
        AccessibilityWindowInfo window
    ) {

        if (window == null) return false;

        Rect bounds = new Rect();
        window.getBoundsInScreen(bounds);

        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        return bounds.width() >= width * 0.98f
            && bounds.height() >= height * 0.95f;
    }

    private void scheduleVideoCheck() {
        handler.removeCallbacks(checkWindows);
        handler.postDelayed(checkWindows, 150);
    }

    private void checkVideoMode() {

        boolean detected = false;

        try {

            List<AccessibilityWindowInfo> windows =
                getWindows();

            if (windows != null) {

                for (AccessibilityWindowInfo window : windows) {

                    if (window == null) continue;

                    if (window.getType() !=
                        AccessibilityWindowInfo.TYPE_APPLICATION) {
                        continue;
                    }

                    CharSequence title = window.getTitle();

                    String titleText =
                        title == null ? "" : title.toString();

                    Rect bounds = new Rect();
                    window.getBoundsInScreen(bounds);

                    DisplayMetrics metrics =
                        new DisplayMetrics();

                    wm.getDefaultDisplay()
                        .getRealMetrics(metrics);

                    boolean smallWindow =
                        bounds.width() < metrics.widthPixels * 0.8f
                        && bounds.height() < metrics.heightPixels * 0.8f;

                    boolean pipCandidate =
                        smallWindow
                        && window.getType() ==
                            AccessibilityWindowInfo.TYPE_APPLICATION;

                    if (pipCandidate) {
                        // 작은 앱 창만으로 영상 PIP인지
                        // 확정할 수 없어 자동으로 숨기지 않음
                    }

                    if (window.isActive()) {

                        android.view.accessibility.AccessibilityNodeInfo
                            root = window.getRoot();

                        String pkg = "";

                        if (root != null) {

                            CharSequence p = root.getPackageName();

                            if (p != null) {
                                pkg = p.toString();
                            }

                            root.recycle();
                        }

                        if (isVideoApp(pkg)
                            && isFullscreenWindow(window)) {

                            detected = true;
                            break;
                        }
                    }
                }
            }

        } catch (Exception ignored) {
            return;
        }

        videoMode = detected;
        updatePetVisibility();
    }

    private final BroadcastReceiver screenReceiver =
        new BroadcastReceiver() {

            @Override
            public void onReceive(
                Context context,
                Intent intent
            ) {

                String action = intent.getAction();

                if (Intent.ACTION_SCREEN_OFF.equals(action)) {

                    if (pet != null) {
                        pet.setVisibility(View.GONE);
                    }

                } else if (
                    Intent.ACTION_SCREEN_ON.equals(action)
                    || Intent.ACTION_USER_PRESENT.equals(action)
                ) {

                    updatePetVisibility();
                    scheduleVideoCheck();
                }
            }
        };

    @Override
    public void onServiceConnected() {

        super.onServiceConnected();

        instance = this;

        AccessibilityServiceInfo info = getServiceInfo();

        if (info != null) {

            info.eventTypes |=
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;

            info.flags |=
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

            setServiceInfo(info);
        }

        showPet();

        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(screenReceiver, filter);
        receiverRegistered = true;

        updatePetVisibility();
        scheduleVideoCheck();
    }

    void showPet() {

        if (pet != null) return;

        wm = (WindowManager)
            getSystemService(WINDOW_SERVICE);

        pet = new ImageView(this);

        PetImages.apply(this, pet, 0);

        pet.setScaleType(
            ImageView.ScaleType.FIT_CENTER
        );

        lp = new WindowManager.LayoutParams(
            dp(prefs().getInt("size", 50) * 3),
            dp(prefs().getInt("size", 50) * 3),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );

        lp.gravity = Gravity.TOP | Gravity.START;

        lp.x = prefs().getInt("x", 50);
        lp.y = prefs().getInt("y", 250);

        pet.setScaleX(
            prefs().getBoolean("flip", false) ? -1f : 1f
        );

        pet.setVisibility(
            shouldShowPet() ? View.VISIBLE : View.GONE
        );

        pet.setOnTouchListener(
            new View.OnTouchListener() {

                float downX, downY;
                int startX, startY;

                @Override
                public boolean onTouch(
                    View v,
                    MotionEvent e
                ) {

                    switch (e.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            downX = e.getRawX();
                            downY = e.getRawY();

                            startX = lp.x;
                            startY = lp.y;

                            return true;

                        case MotionEvent.ACTION_MOVE:

                            lp.x = startX +
                                (int)(e.getRawX() - downX);

                            lp.y = startY +
                                (int)(e.getRawY() - downY);

                            wm.updateViewLayout(pet, lp);

                            return true;

                        case MotionEvent.ACTION_UP:

                            prefs().edit()
                                .putInt("x", lp.x)
                                .putInt("y", lp.y)
                                .apply();

                            return true;
                    }

                    return false;
                }
            }
        );

        wm.addView(pet, lp);
    }

    void refresh() {

        if (pet == null) return;

        int sz = dp(
            prefs().getInt("size", 50) * 3
        );

        lp.width = sz;
        lp.height = sz;

        pet.setScaleX(
            prefs().getBoolean("flip", false) ? -1f : 1f
        );

        PetImages.apply(this, pet, currentSlot);
        updatePetVisibility();

        wm.updateViewLayout(pet, lp);
    }

    @Override
    public void onAccessibilityEvent(
        AccessibilityEvent e
    ) {

        if (e == null) return;

        int type = e.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED
            || type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            scheduleVideoCheck();
        }

        if (pet == null
            || !prefs().getBoolean("react", true)
            || !shouldShowPet()) {
            return;
        }

        if (type !=
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return;
        }

        index++;

        currentSlot = index % 2 == 0 ? 1 : 2;
        PetImages.apply(this, pet, currentSlot);

        if (reset != null) {
            handler.removeCallbacks(reset);
        }

        reset = () -> {
            if (pet != null) {
                currentSlot = 0;
                PetImages.apply(this, pet, 0);
            }
        };

        handler.postDelayed(
            reset,
            prefs().getInt("delay", 150)
        );
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacks(checkWindows);

        if (receiverRegistered) {
            unregisterReceiver(screenReceiver);
            receiverRegistered = false;
        }

        if (reset != null) {
            handler.removeCallbacks(reset);
        }

        if (pet != null && wm != null) {
            wm.removeView(pet);
        }

        pet = null;
        instance = null;

        super.onDestroy();
    }
}
