package com.example.floatingpet;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.*;
import android.graphics.PixelFormat;
import android.view.*;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Looper;

public class PetAccessibilityService extends AccessibilityService {
 static PetAccessibilityService instance;
 private WindowManager wm; private ImageView pet; private WindowManager.LayoutParams lp;
 private final Handler handler = new Handler(Looper.getMainLooper());
 private int index=0; private Runnable reset;
 @Override public void onServiceConnected(){super.onServiceConnected();instance=this;showPet();}
 private android.content.SharedPreferences prefs(){return getSharedPreferences("pet",MODE_PRIVATE);}
 private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 void showPet(){if(pet!=null)return;wm=(WindowManager)getSystemService(WINDOW_SERVICE);pet=new ImageView(this);pet.setImageResource(R.drawable.idle);pet.setScaleType(ImageView.ScaleType.FIT_CENTER);
 lp=new WindowManager.LayoutParams(dp(prefs().getInt("size",50)*3),dp(prefs().getInt("size",50)*3),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP|Gravity.START;lp.x=prefs().getInt("x",50);lp.y=prefs().getInt("y",250);pet.setScaleX(prefs().getBoolean("flip",false)?-1f:1f);pet.setVisibility(prefs().getBoolean("visible",true)?View.VISIBLE:View.GONE);
 pet.setOnTouchListener(new View.OnTouchListener(){float downX,downY;int startX,startY;public boolean onTouch(View v,android.view.MotionEvent e){switch(e.getAction()){case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=lp.x;startY=lp.y;return true;case MotionEvent.ACTION_MOVE:lp.x=startX+(int)(e.getRawX()-downX);lp.y=startY+(int)(e.getRawY()-downY);wm.updateViewLayout(pet,lp);return true;case MotionEvent.ACTION_UP:prefs().edit().putInt("x",lp.x).putInt("y",lp.y).apply();return true;}return false;}});wm.addView(pet,lp);}
 void refresh(){if(pet==null)return;int sz=dp(prefs().getInt("size",50)*3);lp.width=sz;lp.height=sz;pet.setScaleX(prefs().getBoolean("flip",false)?-1f:1f);pet.setVisibility(prefs().getBoolean("visible",true)?View.VISIBLE:View.GONE);wm.updateViewLayout(pet,lp);}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){if(pet==null||!prefs().getBoolean("react",true)||!prefs().getBoolean("visible",true))return;if(e.getEventType()!=AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)return;index++;pet.setImageResource(index%2==0?R.drawable.left:R.drawable.right);if(reset!=null)handler.removeCallbacks(reset);reset=()->{if(pet!=null)pet.setImageResource(R.drawable.idle);};handler.postDelayed(reset,prefs().getInt("delay",150));}
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){if(reset!=null)handler.removeCallbacks(reset);if(pet!=null&&wm!=null)wm.removeView(pet);pet=null;instance=null;super.onDestroy();}
}
