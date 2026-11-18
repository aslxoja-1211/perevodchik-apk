package com.ovozli.tarjimon;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
import java.util.Locale;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivityTTS";
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private android.media.MediaPlayer mediaPlayer = null;

    private String pendingText = null;
    private String pendingLang = null;
    private float pendingRate = 1.0f;
    private float pendingPitch = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Configure WebView settings for media & JavaScript bridge
        try {
            WebView webView = getBridge().getWebView();
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                settings.setAllowFileAccess(true);
                settings.setAllowContentAccess(true);
                settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                webView.clearCache(true);

                webView.addJavascriptInterface(new NativeTTSInterface(), "AndroidNativeTTS");
                Log.d(TAG, "AndroidNativeTTS JavaScript Interface added successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure WebView settings", e);
        }

        // 2. Initialize native Android TextToSpeech engine
        initTTS();
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                Log.d(TAG, "Native Android TextToSpeech initialized successfully");

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        runOnUiThread(() -> {
                            try {
                                WebView webView = getBridge().getWebView();
                                if (webView != null) {
                                    webView.evaluateJavascript("if(window.onTTSStart) window.onTTSStart();", null);
                                }
                            } catch (Exception ignored) {}
                        });
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            try {
                                WebView webView = getBridge().getWebView();
                                if (webView != null) {
                                    webView.evaluateJavascript("if(window.onTTSEnd) window.onTTSEnd();", null);
                                }
                            } catch (Exception ignored) {}
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            try {
                                WebView webView = getBridge().getWebView();
                                if (webView != null) {
                                    webView.evaluateJavascript("if(window.onTTSError) window.onTTSError();", null);
                                }
                            } catch (Exception ignored) {}
                        });
                    }
                });

                // If any speech was requested before TTS finished initializing, speak it now
                if (pendingText != null) {
                    speakNative(pendingText, pendingLang, pendingRate, pendingPitch);
                    pendingText = null;
                }
            } else {
                Log.e(TAG, "Native TextToSpeech initialization failed with status: " + status);
            }
        });
    }

    private void speakNative(String text, String langCode, float rate, float pitch) {
        if (!ttsReady || tts == null || text == null || text.trim().isEmpty()) {
            return;
        }

        // Ensure volume is audible
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                if (currentVol == 0 && maxVol > 0) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(1, maxVol / 2), 0);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not adjust audio volume", e);
        }

        Locale locale;
        String cleanLang = langCode == null ? "ru" : langCode.toLowerCase().trim();
        switch (cleanLang) {
            case "ru":
            case "ru-ru":
                locale = new Locale("ru", "RU");
                break;
            case "tr":
            case "tr-tr":
                locale = new Locale("tr", "TR");
                break;
            case "en":
            case "en-us":
                locale = Locale.US;
                break;
            case "zh":
            case "zh-cn":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "uz":
            case "uz-uz":
            default:
                locale = new Locale("uz", "UZ");
                int res = tts.isLanguageAvailable(locale);
                if (res != TextToSpeech.LANG_AVAILABLE && res != TextToSpeech.LANG_COUNTRY_AVAILABLE && res != TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                    boolean foundUzVoice = false;
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && tts.getVoices() != null) {
                            for (android.speech.tts.Voice v : tts.getVoices()) {
                                if (v.getLocale() != null && "uz".equalsIgnoreCase(v.getLocale().getLanguage())) {
                                    tts.setVoice(v);
                                    locale = v.getLocale();
                                    foundUzVoice = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    if (!foundUzVoice) {
                        locale = new Locale("tr", "TR");
                    }
                }
                break;
        }

        try {
            int res = tts.isLanguageAvailable(locale);
            if (res != TextToSpeech.LANG_AVAILABLE && res != TextToSpeech.LANG_COUNTRY_AVAILABLE && res != TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                if ("uz".equalsIgnoreCase(locale.getLanguage())) {
                    Locale trLocale = new Locale("tr", "TR");
                    if (tts.isLanguageAvailable(trLocale) >= 0) {
                        locale = trLocale;
                    } else if (tts.isLanguageAvailable(new Locale("ru", "RU")) >= 0) {
                        locale = new Locale("ru", "RU");
                    } else {
                        locale = Locale.getDefault();
                    }
                } else {
                    Locale defaultLoc = Locale.getDefault();
                    if (tts.isLanguageAvailable(defaultLoc) >= 0) {
                        locale = defaultLoc;
                    }
                }
            }
            tts.setLanguage(locale);
            tts.setSpeechRate(rate > 0 ? rate : 1.0f);
            tts.setPitch(pitch > 0 ? pitch : 1.0f);

            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utt_" + System.currentTimeMillis());
            Log.d(TAG, "Speaking native text (" + locale.getLanguage() + "): " + text);
        } catch (Exception e) {
            Log.e(TAG, "Error during native TTS speak", e);
        }
    }

    public void notifyTTSStart() {
        runOnUiThread(() -> {
            try {
                WebView webView = getBridge().getWebView();
                if (webView != null) {
                    webView.evaluateJavascript("if(window.onTTSStart) window.onTTSStart();", null);
                }
            } catch (Exception ignored) {}
        });
    }

    public void notifyTTSEnd() {
        runOnUiThread(() -> {
            try {
                WebView webView = getBridge().getWebView();
                if (webView != null) {
                    webView.evaluateJavascript("if(window.onTTSEnd) window.onTTSEnd();", null);
                }
            } catch (Exception ignored) {}
        });
    }

    public void playNativeUrl(String url) {
        playNativeUrl(url, null, null);
    }

    public void playNativeUrl(String url, String fallbackText, String langCode) {
        if (url == null || url.trim().isEmpty()) {
            if (fallbackText != null && !fallbackText.trim().isEmpty()) {
                speakNative(fallbackText, langCode, 1.0f, 1.0f);
            }
            return;
        }
        runOnUiThread(() -> {
            try {
                if (mediaPlayer != null) {
                    try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
                    mediaPlayer = null;
                }
                mediaPlayer = new android.media.MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
                    headers.put("Referer", "https://translate.google.com/");
                    mediaPlayer.setDataSource(MainActivity.this, android.net.Uri.parse(url), headers);
                } else {
                    mediaPlayer.setDataSource(url);
                }
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    notifyTTSStart();
                });
                mediaPlayer.setOnCompletionListener(mp -> {
                    notifyTTSEnd();
                    try { mp.release(); } catch (Exception ignored) {}
                    mediaPlayer = null;
                });
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.w(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                    notifyTTSEnd();
                    try { mp.release(); } catch (Exception ignored) {}
                    mediaPlayer = null;
                    if (fallbackText != null && !fallbackText.trim().isEmpty()) {
                        speakNative(fallbackText, langCode, 1.0f, 1.0f);
                    }
                    return true;
                });
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.e(TAG, "playNativeUrl error", e);
                notifyTTSEnd();
                if (fallbackText != null && !fallbackText.trim().isEmpty()) {
                    speakNative(fallbackText, langCode, 1.0f, 1.0f);
                }
            }
        });
    }

    public void playNativeBase64(String base64Audio) {
        playNativeBase64(base64Audio, null, null);
    }

    public void playNativeBase64(String base64Audio, String fallbackText, String langCode) {
        if (base64Audio == null || base64Audio.trim().isEmpty()) {
            if (fallbackText != null && !fallbackText.trim().isEmpty()) {
                speakNative(fallbackText, langCode, 1.0f, 1.0f);
            }
            return;
        }
        try {
            // Strip data: prefix if present
            String cleanB64 = base64Audio;
            int commaIdx = cleanB64.indexOf(",");
            if (commaIdx != -1) {
                cleanB64 = cleanB64.substring(commaIdx + 1);
            }
            byte[] bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT);
            java.io.File tempFile = new java.io.File(getCacheDir(), "tts_current.wav");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile, false);
            fos.write(bytes);
            fos.close();
            playNativeUrl(tempFile.getAbsolutePath(), fallbackText, langCode);
        } catch (Exception e) {
            Log.e(TAG, "playNativeBase64 error", e);
            if (fallbackText != null && !fallbackText.trim().isEmpty()) {
                speakNative(fallbackText, langCode, 1.0f, 1.0f);
            }
        }
    }

    public class NativeTTSInterface {
        @JavascriptInterface
        public boolean isAvailable() {
            return true;
        }

        @JavascriptInterface
        public void playNativeBase64(String base64Wav) {
            MainActivity.this.playNativeBase64(base64Wav, null, null);
        }

        @JavascriptInterface
        public void playNativeBase64(String base64Wav, String fallbackText, String langCode) {
            MainActivity.this.playNativeBase64(base64Wav, fallbackText, langCode);
        }

        @JavascriptInterface
        public void playNativeUrl(String url) {
            MainActivity.this.playNativeUrl(url, null, null);
        }

        @JavascriptInterface
        public void playNativeUrl(String url, String fallbackText, String langCode) {
            MainActivity.this.playNativeUrl(url, fallbackText, langCode);
        }

        @JavascriptInterface
        public void speak(String text, String langCode, float rate, float pitch) {
            if (!ttsReady || tts == null) {
                Log.w(TAG, "TTS not ready yet, queuing pending request");
                pendingText = text;
                pendingLang = langCode;
                pendingRate = rate;
                pendingPitch = pitch;
                return;
            }
            speakNative(text, langCode, rate, pitch);
        }

        @JavascriptInterface
        public void vibrate(int durationMs) {
            try {
                int ms = Math.max(8, Math.min(120, durationMs));
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        v.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(ms);
                    }
                }
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void stop() {
            if (mediaPlayer != null) {
                try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
                mediaPlayer = null;
            }
            if (ttsReady && tts != null) {
                tts.stop();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
