package org.rainbowlittlecat.app.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Text to speech
 */
public class VoiceOutput {
    private TextToSpeech ladyGoogle;

    public VoiceOutput(Context context) {
        ladyGoogle = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                Locale locale = Locale.getDefault();

                if (locale.equals(Locale.TAIWAN))
                    ladyGoogle.setLanguage(Locale.TAIWAN);
                else
                    ladyGoogle.setLanguage(Locale.ENGLISH);
            }
        });
    }

    public void speak(String toSay) {
        if (!toSay.equals(""))
            ladyGoogle.speak(toSay, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * call this in onDestroy()
     */
    public void shutdown() {
        if (ladyGoogle != null) {
            ladyGoogle.stop();
            ladyGoogle.shutdown();
        }
    }
}
