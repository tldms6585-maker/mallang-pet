
package com.example.floatingpet;

import android.app.*;
import android.os.*;
import android.content.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {

    android.content.SharedPreferences prefs;
    LinearLayout layout;

    ImageView[] previews = new ImageView[3];

    int dp(int x) {
        return (int) (x * getResources()
                .getDisplayMetrics().density + .5f);
    }

    void refresh() {
        if (PetAccessibilityService.instance != null) {
            PetAccessibilityService.instance.refresh();
        }
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        prefs = getSharedPreferences("pet", MODE_PRIVATE);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
                dp(20), dp(22), dp(20), dp(16)
        );

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("말랑 키보드 펫");
        title.setTextSize(25);
        layout.addView(title);

        TextView note = new TextView(this);
        note.setText(
                "1. 아래 버튼으로 접근성 설정을 열어 " +
                "‘말랑 키보드 펫’을 켜주세요.\n" +
                "2. 다른 앱 위에 캐릭터가 표시됩니다. " +
                "드래그로 이동할 수 있어요.\n" +
                "3. 글자 내용은 수집하거나 저장하지 않습니다. " +
                "앱에 따라 입력 반응이 감지되지 않을 수 있어요."
        );
        note.setPadding(0, dp(12), 0, dp(12));
        layout.addView(note);

        Button settings = new Button(this);
        settings.setText("접근성 설정 열기");
        settings.setOnClickListener(v ->
                startActivity(new Intent(
                        Settings.ACTION_ACCESSIBILITY_SETTINGS
                ))
        );
        layout.addView(settings);

        toggle("캐릭터 표시", "visible", true);
        toggle("키 입력에 반응", "react", true);
        toggle("좌우 반전", "flip", false);

        seek("크기", "size", 50, 20, 100, "");
        seek("발 복귀 속도", "delay", 150, 50, 600, "ms");

        // 사진 3장 변경 메뉴
        addPhotoSettings();

        TextView end = new TextView(this);
        end.setText(
                "사진은 앱 내부에 저장되며 다른 앱으로 " +
                "전송하지 않습니다. 앱 데이터를 삭제하거나 " +
                "앱을 제거하면 선택한 사진도 삭제됩니다.\n\n" +
                "접근성 서비스는 일부 기기에서 배터리 " +
                "관리에 의해 중단될 수 있습니다."
        );
        end.setPadding(0, dp(24), 0, 0);
        layout.addView(end);
    }

    void toggle(String label, String key, boolean def) {

        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(17);
        s.setPadding(0, dp(12), 0, dp(12));
        s.setChecked(prefs.getBoolean(key, def));

        s.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(key, on).apply();
            refresh();
        });

        layout.addView(s);
    }

    void seek(
            String label,
            String key,
            int def,
            int min,
            int max,
            String unit
    ) {

        TextView t = new TextView(this);
        t.setPadding(0, dp(16), 0, 0);
        t.setTextSize(17);

        int value = prefs.getInt(key, def);
        t.setText(label + ": " + value + unit);
        layout.addView(t);

        SeekBar s = new SeekBar(this);
        s.setMax(max - min);
        s.setProgress(value - min);

        s.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    public void onProgressChanged(
                            SeekBar b,
                            int p,
                            boolean user
                    ) {
                        if (!user) return;

                        int v = p + min;
                        t.setText(label + ": " + v + unit);

                        prefs.edit()
                                .putInt(key, v)
                                .apply();

                        refresh();
                    }

                    public void onStartTrackingTouch(
                            SeekBar b
                    ) {}

                    public void onStopTrackingTouch(
                            SeekBar b
                    ) {}
                }
        );

        layout.addView(s);
    }

    // 사진 변경 메뉴
    void addPhotoSettings() {

        TextView heading = new TextView(this);
        heading.setText("캐릭터 사진 변경");
        heading.setTextSize(21);
        heading.setPadding(0, dp(22), 0, dp(8));
        layout.addView(heading);

        TextView description = new TextView(this);
        description.setText(
                "각 사진을 눌러 갤러리에서 교체하세요. " +
                "PNG 투명 배경도 지원합니다."
        );
        layout.addView(description);

        String[] names = {
                "기본 사진 (대기)",
                "왼발 사진",
                "오른발 사진"
        };

        for (int i = 0; i < 3; i++) {

            final int slot = i;

            TextView label = new TextView(this);
            label.setText(names[i]);
            label.setTextSize(17);
            label.setPadding(
                    0, dp(16), 0, dp(4)
            );
            layout.addView(label);

            // 현재 사진 미리보기
            ImageView preview = new ImageView(this);

            preview.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            dp(140), dp(140)
                    )
            );

            preview.setScaleType(
                    ImageView.ScaleType.FIT_CENTER
            );

            previews[i] = preview;

            PetImages.apply(this, preview, i);
            layout.addView(preview);

            // 사진 선택 버튼
            Button choose = new Button(this);
            choose.setText("사진 선택 / 변경");

            choose.setOnClickListener(v -> {

                Intent intent = new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

                intent.addCategory(
                        Intent.CATEGORY_OPENABLE
                );

                intent.setType("image/*");

                startActivityForResult(
                        intent, 100 + slot
                );
            });

            layout.addView(choose);

            // 기본 사진 복원 버튼
            Button reset = new Button(this);
            reset.setText("기본 사진으로 복원");

            reset.setOnClickListener(v -> {

                PetImages.reset(this, slot);

                PetImages.apply(
                        this,
                        previews[slot],
                        slot
                );

                refresh();
            });

            layout.addView(reset);
        }
    }

    // 선택한 사진 저장
    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode >= 100 &&
                requestCode <= 102 &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            int slot = requestCode - 100;

            try {

                PetImages.save(
                        this,
                        data.getData(),
                        slot
                );

                PetImages.apply(
                        this,
                        previews[slot],
                        slot
                );

                refresh();

                Toast.makeText(
                        this,
                        "사진이 변경되었습니다.",
                        Toast.LENGTH_SHORT
                ).show();

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "사진을 불러오지 못했습니다: " +
                                e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
