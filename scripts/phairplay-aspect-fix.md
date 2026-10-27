# PhairPlay cast aspect fix (Allwinner HY300 Ultra / H726)

How the horizontal stretch when mirroring an iPhone/iPad to PhairPlay (and iMirror) on this projector was traced and fixed. AirScreen does not stretch; the two free receivers do.

## Symptom

An AirPlay screen-mirror from a portrait iPhone shows the picture stretched wide (icons look fat, a 19.5:9 phone fills a wider-than-correct area). Both PhairPlay and iMirror are affected identically; AirScreen is not.

## Root cause

The Allwinner hardware H.264 decoder (`c2.allwinner.avc.decoder`, `libawcodec2_vndk.so` + cedarc) scales the decoded frame down toward the 720p panel when the source is taller than 720, but it applies **different horizontal and vertical scale-down ratios** (`nHorizonScaleDownRatio != nVerticalScaleDownRatio`) and then reports a **distorted crop rectangle** through MediaCodec. For a 498x1080 (19.5:9) mirror stream the logs show:

```
VdecComponent: C2StreamPictureSizeInfo width = 498 height = 1080   # true size (from the SPS)
CCodecConfig:  raw.crop.width = 484  raw.crop.height = 712          # distorted crop, ratio 0.68 not 0.46
CCodecBuffers: Rect crop(0, 0, 483, 711)
```

PhairPlay's `VideoDecoder.publishOutputSize()` trusts the MediaCodec crop rectangle and overwrites the correct SPS-parsed size (498x1080, from `initialize()`) with the distorted crop (484x712). The aspect-fit then renders the portrait content at ratio ~0.68 instead of ~0.46, i.e. horizontally stretched. iMirror has the same bug; AirScreen uses a different render path and avoids it.

The vendor aspect/overscan control (`ITvServer.setPQValue(21, mode)`) was ruled out: setting FULL vs 16:9 vs AUTO produced identical output, because it governs the hardware video plane (HDMI-in etc.), not PhairPlay's GPU/TextureView path. See the "HDMI-in" note below.

## The fix

Stop `publishOutputSize()` from overwriting the size with the decoder's crop on this device; keep the true SPS-parsed size from `initialize()`. The aspect-fit transform then maps the (internally rescaled) surface to a rectangle of the true aspect, restoring correct proportions.

In the PhairPlay source this is a guard in `app/src/main/kotlin/com/phairplay/airplay/VideoDecoder.kt`. As a binary patch to the released APK it is a one-instruction smali change: an early `return-void` at the top of `publishOutputSize`.

## Applying it to the released APK (build locally; do not redistribute)

PhairPlay is third-party software. Do not host or share the patched APK; build it yourself from the official release.

```sh
# 1. Decode (full decode so resources rebuild cleanly; a resources-skipped
#    build (-r) left the app crashing on launch).
apktool d -f PhairPlay-<version>-GoogleTV.apk -o pp

# 2. Edit  pp/smali/com/phairplay/airplay/VideoDecoder.smali
#    In method  publishOutputSize(Landroid/media/MediaFormat;)V ,
#    immediately after the line  ".locals 5"  insert a single line:
#        return-void
#    (The rest of the method becomes unreachable, which is the intent.)

# 3. Rebuild, zip-align, sign with your own debug key, reinstall.
apktool b pp -o pp.apk
zipalign -p -f 4 pp.apk pp-aligned.apk
apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android \
  --ks-key-alias androiddebugkey --key-pass pass:android pp-aligned.apk
adb uninstall com.phairplay.googletv       # release is signed with a different key
adb install --abi armeabi-v7a pp-aligned.apk
```

The real fix belongs upstream (a crop sanity-check: ignore the MediaCodec crop when its aspect differs sharply from the coded/SPS aspect). This was reported to the project; see the link in the repo README.

## Related: HDMI-in aspect (separate control)

The Allwinner TvServer exposes a per-source overscan/aspect mode via `ITvServer.setPQValue(21, mode)`, where `EnumOverScanScreenMode` is `0=FULL 1=16X9 2=4X3 3=CINEMA 4=ZOOM 5=AUTO 6=DOT_BY_DOT`. It affects the hardware video path (for example an HDMI-in source), not PhairPlay's TextureView path, so it is unrelated to the mirror stretch above. [`SetAspect.java`](SetAspect.java) calls it by reflection through `app_process` (no vendor stubs needed at build time; `MagcubicOS.apk` supplies them at runtime) for cases where an HDMI source shows the wrong aspect:

```sh
# build: javac -cp android.jar -> d8 -> aspect.jar (dex)
adb push aspect.jar /data/local/tmp/
adb shell su -c 'CLASSPATH=/data/local/tmp/aspect.jar:/product/app/MagcubicOS/MagcubicOS.apk \
  app_process32 /system/bin SetAspect 5'   # 5 = AUTO; "get" to read the current value
```
