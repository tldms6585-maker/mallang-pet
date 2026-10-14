
package com.example.floatingpet;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.app.KeyguardManager;
import android.os.PowerManager;
import android.content.*;
import android.graphics.PixelFormat;
import android.view.*;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Looper;

public class PetAccessibilityService extends AccessibilityService {

    static PetAccessibilityService instance;

    private WindowManager wm;
    private ImageView pet;
    private WindowManager.LayoutParams lp;

    private final Handler handler =
        new Handler(Looper.getMainLooper());

    private int index = 0;
    private Runnable reset;

    private boolean receiverRegistered = false;

    private SharedPreferences prefs() {
        return getSharedPreferences("pet", MODE_PRIVATE);
    }

    private int dp(float x) {
        return (int) (
            x * getResources().getDisplayMetrics().density + .5f
        );
    }

    // 화면이 켜져 있고 잠금이 해제된 경우만 표시
    private boolean shouldShowPet() {

        PowerManager pm =
            (PowerManager) getSystemService(POWER_SERVICE);

        KeyguardManager km =
            (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        boolean screenOn =
            pm != null && pm.isInteractive();

        boolean unlocked =
            km != null && !km.isKeyguardLocked();

        boolean visible =
            prefs().getBoolean("visible", true);

        return screenOn && unlocked && visible;
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

    // 화면 상태 변경 시에만 실행
    private final BroadcastReceiver screenReceiver =
        new BroadcastReceiver() {

            @Override
            public void onReceive(
                Context context,
                Intent intent
            ) {

                String action = intent.getAction();

                if (action == null) return;

                switch (action) {

                    case Intent.ACTION_SCREEN_OFF:
                        // 화면이 꺼지면 즉시 숨김
                        if (pet != null) {
                            pet.setVisibility(View.GONE);
                        }
                        break;

                    case Intent.ACTION_SCREEN_ON:
                        // 화면이 켜져도 잠겨 있으면 숨김
                        updatePetVisibility();
                        break;

                    case Intent.ACTION_USER_PRESENT:
                        // 잠금 해제 후 다시 표시
                        updatePetVisibility();
                        break;
                }
            }
        };

    @Override
    public void onServiceConnected() {

        super.onServiceConnected();

        instance = this;

        showPet();

        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(screenReceiver, filter);
        receiverRegistered = true;

        updatePetVisibility();
    }

    void showPet() {

        if (pet != null) return;

        wm = (WindowManager)
            getSystemService(WINDOW_SERVICE);

        pet = new ImageView(this);

        pet.setImageResource(R.drawable.idle);

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

        updatePetVisibility();

        wm.updateViewLayout(pet, lp);
    }

    @Override
    public void onAccessibilityEvent(
        AccessibilityEvent e
    ) {

        if (pet == null ||
            !prefs().getBoolean("react", true) ||
            !shouldShowPet()) {
            return;
        }

        if (e.getEventType() !=
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return;
        }

        index++;

        pet.setImageResource(
            index % 2 == 0
                ? R.drawable.left
                : R.drawable.right
        );

        if (reset != null) {
            handler.removeCallbacks(reset);
        }

        reset = () -> {
            if (pet != null) {
                pet.setImageResource(R.drawable.idle);
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
