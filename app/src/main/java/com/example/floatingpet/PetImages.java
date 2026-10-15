package com.example.floatingpet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class PetImages {
    private static final String[] NAMES = {"idle", "left", "right"};
    private static final int[] DEFAULTS = {
        R.drawable.idle,
        R.drawable.left,
        R.drawable.right
    };

    private PetImages() {}

    private static File file(Context context, int slot) {
        return new File(context.getFilesDir(),
                "pet_image_" + NAMES[slot] + ".png");
    }

    public static boolean custom(Context context, int slot) {
        return file(context, slot).isFile();
    }

    public static void reset(Context context, int slot) {
        file(context, slot).delete();
    }

    public static void apply(Context context, ImageView view, int slot) {
        File f = file(context, slot);
        if (f.isFile()) {
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bitmap != null) {
                view.setImageBitmap(bitmap);
                return;
            }
        }
        view.setImageResource(DEFAULTS[slot]);
    }

    public static void save(Context context,
                            android.net.Uri uri,
                            int slot) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        try (InputStream in =
                     context.getContentResolver().openInputStream(uri)) {
            if (in == null)
                throw new java.io.IOException("사진을 열 수 없습니다.");
            BitmapFactory.decodeStream(in, null, bounds);
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0)
            throw new java.io.IOException("지원하지 않는 이미지입니다.");

        int sample = 1;
        while (Math.max(bounds.outWidth / sample,
                        bounds.outHeight / sample) > 1024)
            sample *= 2;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap decoded;

        try (InputStream in =
                     context.getContentResolver().openInputStream(uri)) {
            if (in == null)
                throw new java.io.IOException("사진을 열 수 없습니다.");
            decoded = BitmapFactory.decodeStream(in, null, options);
        }

        if (decoded == null)
            throw new java.io.IOException("이미지 처리에 실패했습니다.");

        File temp = new File(context.getFilesDir(),
                "pet_image_temp.png");

        try {
            try (FileOutputStream out = new FileOutputStream(temp)) {
                if (!decoded.compress(Bitmap.CompressFormat.PNG, 100, out))
                    throw new java.io.IOException("저장 실패");
            }

            File target = file(context, slot);
            if (!temp.renameTo(target)) {
                try (java.io.FileInputStream in =
                             new java.io.FileInputStream(temp);
                     FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int count;
                    while ((count = in.read(buf)) != -1)
                        out.write(buf, 0, count);
                }
            }
        } finally {
            decoded.recycle();
            temp.delete();
        }
    }
}
