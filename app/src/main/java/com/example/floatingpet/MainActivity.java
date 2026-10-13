package com.example.floatingpet;
import android.app.*;import android.os.*;import android.content.*;import android.provider.Settings;import android.view.*;import android.widget.*;
public class MainActivity extends Activity {
 android.content.SharedPreferences prefs;LinearLayout layout;
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 void refresh(){if(PetAccessibilityService.instance!=null)PetAccessibilityService.instance.refresh();}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("pet",MODE_PRIVATE);layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(20),dp(22),dp(20),dp(16));ScrollView scroll=new ScrollView(this);scroll.addView(layout);setContentView(scroll);
 TextView title=new TextView(this);title.setText("말랑 키보드 펫");title.setTextSize(25);layout.addView(title);
 TextView note=new TextView(this);note.setText("1. 아래 버튼으로 접근성 설정을 열어 ‘말랑 키보드 펫’을 켜주세요.\n2. 다른 앱 위에 캐릭터가 표시됩니다. 드래그로 이동할 수 있어요.\n3. 글자 내용은 수집하거나 저장하지 않습니다. 앱에 따라 입력 반응이 감지되지 않을 수 있어요.");note.setPadding(0,dp(12),0,dp(12));layout.addView(note);
 Button settings=new Button(this);settings.setText("접근성 설정 열기");settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));layout.addView(settings);
 toggle("캐릭터 표시", "visible",true);toggle("키 입력에 반응", "react",true);toggle("좌우 반전", "flip",false);
 seek("크기", "size",50,20,100,"");seek("발 복귀 속도", "delay",150,50,600,"ms");
 TextView end=new TextView(this);end.setText("사진을 교체하려면 Android Studio 프로젝트의 app/src/main/res/drawable-nodpi 안에 idle.jpg, left.jpg, right.jpg를 같은 이름으로 바꾼 뒤 앱을 다시 빌드해야 합니다.\n\n접근성 서비스는 일부 기기에서 배터리 관리에 의해 중단될 수 있습니다.");end.setPadding(0,dp(24),0,0);layout.addView(end);
 }
 void toggle(String label,String key,boolean def){Switch s=new Switch(this);s.setText(label);s.setTextSize(17);s.setPadding(0,dp(12),0,dp(12));s.setChecked(prefs.getBoolean(key,def));s.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean(key,on).apply();refresh();});layout.addView(s);}
 void seek(String label,String key,int def,int min,int max,String unit){TextView t=new TextView(this);t.setPadding(0,dp(16),0,0);t.setTextSize(17);int value=prefs.getInt(key,def);t.setText(label+": "+value+unit);layout.addView(t);SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(value-min);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){if(!user)return;int v=p+min;t.setText(label+": "+v+unit);prefs.edit().putInt(key,v).apply();refresh();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});layout.addView(s);}
}
