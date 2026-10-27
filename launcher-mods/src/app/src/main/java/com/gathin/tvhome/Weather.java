package com.gathin.tvhome;

import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Current weather for a fixed point from Open-Meteo (open-meteo.com): free, no API key, HTTPS. Dependency-free
 * and called off the main thread; a failure just returns null and the header keeps its last reading.
 */
final class Weather {

    private static final String TAG = "Weather";

    private Weather() {}

    /** One reading: the temperature in whole degrees and a single glyph for the sky. */
    static final class Now {
        final int tempC;
        final String glyph;
        Now(int tempC, String glyph) {
            this.tempC = tempC;
            this.glyph = glyph;
        }
    }

    static Now fetch(double lat, double lon) {
        HttpURLConnection c = null;
        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + lat
                    + "&longitude=" + lon + "&current=temperature_2m,weather_code&timezone=auto");
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "tvhome");
            if (c.getResponseCode() != 200) return null;
            JSONObject current = new JSONObject(readAll(c.getInputStream())).getJSONObject("current");
            int temp = (int) Math.round(current.getDouble("temperature_2m"));
            return new Now(temp, glyphFor(current.optInt("weather_code", -1)));
        } catch (Exception e) {
            Log.d(TAG, "fetch failed: " + e);
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        try (InputStream src = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = src.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    /** WMO weather interpretation code → one glyph. Plain sun/cloud/umbrella/snow symbols render without an emoji font. */
    private static String glyphFor(int code) {
        if (code == 0) return "☀";                         // ☀ clear
        if (code == 1 || code == 2) return "⛅";            // ⛅ mainly clear / partly cloudy
        if (code == 3) return "☁";                         // ☁ overcast
        if (code == 45 || code == 48) return "☁";          // fog
        if (code >= 51 && code <= 67) return "☂";          // ☂ drizzle / rain
        if (code >= 71 && code <= 77) return "❄";          // ❄ snow
        if (code >= 80 && code <= 82) return "☂";          // rain showers
        if (code >= 85 && code <= 86) return "❄";          // snow showers
        if (code >= 95) return "⚡";                        // ⚡ thunderstorm
        return "☀";
    }
}
