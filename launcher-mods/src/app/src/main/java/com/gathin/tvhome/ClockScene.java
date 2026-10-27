package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The clock screensaver's scene (layout dream_clock): the thin clock and date on black over a slow ambient
 * glow. Every minute the clock drifts to a new spot and the glow shifts colour, so nothing burns in.
 * Shared by ClockDream (the system screensaver) and ClockActivity (Home left idle).
 *
 * With clock_wallpaper on it becomes a picture frame: Home's rotating wallpapers fill the screen behind the
 * clock and cross-fade to the next one every {@link #FRAME_MS}. One picture decodes at a time, at screen size
 * in RGB_565, and the one it replaces is recycled after the fade, so a long idle stays within a frame or two.
 */
final class ClockScene {

    private static final long MOVE_MS = 60_000L;
    /** Picture frame: a new wallpaper every ~22 s, cross-faded over 1.2 s. */
    private static final long FRAME_MS = 22_000L;
    private static final int FRAME_FADE_MS = 1200;
    /** The glow's colour change: 16 steps over 8 s. Animated per frame it redrew the full-screen gradient ~480 times. */
    private static final int GLOW_STEPS = 16;
    private static final long GLOW_STEP_MS = 500L;
    /** Deep, low-brightness glow colours; the glow is drawn at partial alpha on black. */
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View content;
    private final ImageView ambient;
    private final GradientDrawable glow;
    private int colorIndex;
    private int glowColor;
    /** clock_wallpaper or clock_glow false: something else (a picture, or plain black) replaced the coloured glow. */
    private boolean wallpaperShown;
    /** clock_wallpaper: the pictures to rotate through (one for a single-image theme), their darkening, and the
     *  resources to draw them with. */
    private List<File> wallpapers;
    private int wallpaperIdx = -1;
    private int wallpaperShade;
    private Resources wallpaperRes;
    /** The picture currently on screen; recycled once the next one has faded in. */
    private Bitmap shownBmp;
    /** Bumped by stop(): a decode started before it finds a different number and drops its result. Main thread only. */
    private int wallpaperGeneration;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            shiftGlow();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    /** Picture frame: advance to the next wallpaper. Separate from the clock's minute tick so photos change faster. */
    private final Runnable frameTick = new Runnable() {
        @Override
        public void run() {
            showNextPhoto();
            handler.postDelayed(this, FRAME_MS);
        }
    };

    /** root is the inflated dream_clock layout. */
    ClockScene(View root) {
        content = root.findViewById(R.id.dreamContent);
        ambient = root.findViewById(R.id.dreamAmbient);
        glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                glow.setGradientRadius((r - l) * 0.55f));
        colorIndex = random.nextInt(PALETTE.length);
        setGlow(PALETTE[colorIndex]);
    }

    void start() {
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, MOVE_MS);
        if (wallpapers != null && !wallpapers.isEmpty() && !wallpaperShown) {
            showNextPhoto();                                   // the first picture, right away
            if (wallpapers.size() > 1) {
                handler.removeCallbacks(frameTick);
                handler.postDelayed(frameTick, FRAME_MS);      // then rotate
            }
        }
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);   // the tick, the frame tick and any glow steps still queued
        content.animate().cancel();
        wallpaperGeneration++;   // a decode still running must not attach its picture to a stopped scene
        if (shownBmp != null) {
            shownBmp.recycle();
            shownBmp = null;
        }
    }

    /**
     * clock_wallpaper: put Home's wallpapers (the theme's picture, or its whole rotating collection) behind the
     * clock, darkened by clock_wallpaper_dim, in place of the glow. Decoded off the main thread at screen size in
     * RGB_565 (about 1.3 MB at 720p), like Home's own; until the first one lands, or if none can be read, the glow
     * stays. Call before start(), which shows the first picture and begins the rotation.
     */
    void useHomeWallpaper(Context ctx) {
        Config cfg;
        try {
            cfg = Config.load(ctx);
        } catch (RuntimeException e) {
            return;
        }
        if (!cfg.clockWallpaper) {
            // clock_glow false: plain black behind the clock (OLED); the clock itself still moves every minute.
            if (!cfg.clockGlow) {
                wallpaperShown = true;   // also stops the glow's colour cycle
                ambient.setBackground(null);
            }
            return;
        }
        List<File> list = new ArrayList<>();
        if (!cfg.wallpapers.isEmpty()) list.addAll(cfg.wallpapers);
        else if (cfg.wallpaper != null) list.add(cfg.wallpaper);
        if (list.isEmpty()) return;
        Collections.shuffle(list);       // a different order each time the screensaver opens
        wallpapers = list;
        wallpaperShade = (cfg.clockWallpaperDim * 255 / 100) << 24;
        wallpaperRes = ctx.getResources();
    }

    /** Decode the next wallpaper off the main thread, then cross-fade it in. */
    private void showNextPhoto() {
        if (wallpapers == null || wallpapers.isEmpty()) return;
        wallpaperIdx = (wallpaperIdx + 1) % wallpapers.size();
        File file = wallpapers.get(wallpaperIdx);
        int shade = wallpaperShade;
        Resources res = wallpaperRes;
        DisplayMetrics dm = res.getDisplayMetrics();
        int w = dm.widthPixels, h = dm.heightPixels;
        int generation = wallpaperGeneration;
        new Thread(() -> {
            Bitmap bmp = decode(file, w, h);
            if (bmp == null) return;
            handler.post(() -> {
                if (generation != wallpaperGeneration) {
                    bmp.recycle();   // stopped meanwhile: this picture was never attached
                    return;
                }
                crossfadeTo(res, bmp, shade);
            });
        }, "frame-photo").start();
    }

    /** Swap the picture under the clock, fading from the one on screen. centerCrop fills the screen like Home. */
    private void crossfadeTo(Resources res, Bitmap bmp, int shade) {
        Drawable next = new LayerDrawable(new Drawable[]{
                new BitmapDrawable(res, bmp), new ColorDrawable(shade)});
        Drawable current = ambient.getDrawable();
        ambient.setBackground(null);   // drop the glow; the picture takes over
        if (current == null) {
            ambient.setImageDrawable(next);
        } else {
            TransitionDrawable fade = new TransitionDrawable(new Drawable[]{current, next});
            fade.setCrossFadeEnabled(true);
            ambient.setImageDrawable(fade);
            fade.startTransition(FRAME_FADE_MS);
        }
        wallpaperShown = true;
        Bitmap old = shownBmp;
        shownBmp = bmp;
        // Recycle the picture we just replaced, but only once it has finished fading out.
        if (old != null) handler.postDelayed(old::recycle, FRAME_FADE_MS + 300L);
    }

    /** WallpaperPlayer's decode: sampled and scaled down to cover the screen, never bigger than 4K. */
    private static Bitmap decode(File file, int screenW, int screenH) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            int width = options.outWidth, height = options.outHeight;
            if (width <= 0 || height <= 0) return null;
            int sample = WallpaperSize.sampleSize(width, height, screenW, screenH);
            if (!WallpaperSize.accepted(width / sample, height / sample)) return null;
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            int[] scale = WallpaperSize.coverScale(width / sample, height / sample, screenW, screenH);
            if (scale != null) {
                options.inScaled = true;
                options.inDensity = scale[0];
                options.inTargetDensity = scale[1];
            }
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getPath(), options);
        } catch (Exception | OutOfMemoryError failure) {
            return null;
        }
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(900).withLayer().withEndAction(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).withLayer().start();
        }).start();
    }

    /** A random spot that keeps the whole clock on screen, with the glow centred behind it. */
    private void placeClock() {
        View parent = (View) content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        float x = maxX * (0.1f + 0.8f * random.nextFloat());
        float y = maxY * (0.1f + 0.8f * random.nextFloat());
        content.setX(x);
        content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0) {
            glow.setGradientCenter((x + content.getWidth() / 2f) / parent.getWidth(),
                    (y + content.getHeight() / 2f) / parent.getHeight());
        }
    }

    private void shiftGlow() {
        if (wallpaperShown) return;
        int from = glowColor;
        colorIndex = (colorIndex + 1) % PALETTE.length;
        int to = PALETTE[colorIndex];
        ArgbEvaluator mix = new ArgbEvaluator();
        for (int i = 1; i <= GLOW_STEPS; i++) {
            float f = (float) i / GLOW_STEPS;
            handler.postDelayed(() -> setGlow((int) mix.evaluate(f, from, to)), i * GLOW_STEP_MS);
        }
    }

    private void setGlow(int color) {
        glowColor = color;
        int c = color & 0x00FFFFFF;
        glow.setColors(new int[]{0x40000000 | c, 0x14000000 | c, 0x00000000 | c});
    }
}
