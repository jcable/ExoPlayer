package com.google.android.exoplayer2.text.ssa;

import android.content.Intent;
import android.text.Layout;
import android.util.ArrayMap;
import android.util.Log;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.SimpleSubtitleDecoder;
import com.google.android.exoplayer2.util.LongArray;
import com.google.android.exoplayer2.util.ParsableByteArray;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.attr.data;
import static android.R.attr.format;
import static android.R.attr.key;
import static android.R.attr.lines;
import static android.R.attr.text;
import static android.R.attr.textAlignment;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static com.google.android.exoplayer2.text.Cue.DIMEN_UNSET;
import static com.google.android.exoplayer2.text.Cue.TYPE_UNSET;

/**
 * Created by cablej01 on 26/12/2016.
 */

public class SSADecoder extends SimpleSubtitleDecoder {
    private static final String TAG = "SSADecoder";
    private static String defaultDialogueFormat = "Start, ReadOrder, Layer, Style, Name, MarginL, MarginR, MarginV, Effect, Text";
    private static String defaultStyleFormat = "Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding";
    private String[] dialogueFormat;
    private String[] styleFormat;
    private SSASubtitle subtitles = new SSASubtitle();
    private Map<String,Style> styles = new HashMap<>();

    public SSADecoder() {
        super("SSADecoder");
        dialogueFormat = parseKeys(defaultDialogueFormat);
        styleFormat = parseKeys(defaultStyleFormat);
    }

    /**
     * Decodes data into a {@link SSASubtitle}.
     *
     * @param bytes An array holding the data to be decoded, starting at position 0.
     * @param length The size of the data to be decoded.
     * @return The decoded {@link SSASubtitle}.
     */
    @Override
    protected SSASubtitle decode(byte[] bytes, int length) {
        ParsableByteArray data = new ParsableByteArray(bytes, length);
        String currentLine;
        while ((currentLine = data.readLine()) != null) {
            if (currentLine.length() == 0) {
                // Skip blank lines.
                continue;
            }
            Log.i(TAG, currentLine);

            if (currentLine.equals("[Script Info]")) {
                parseInfo(data);
                continue;
            }
            else if (currentLine.equals("[V4+ Styles]")) {
                parseStyles(styles, data);
                continue;
            }
            else if (currentLine.equals("[V4 Styles]")) {
                parseStyles(styles, data);
                continue;
            }
            else if (currentLine.equals("[Events]")) {
                while(true) {
                    currentLine = data.readLine();
                    if(currentLine==null)
                        break;
                    Log.i(TAG, currentLine);
                    if(!currentLine.contains(":"))
                        break;
                    String p[] = currentLine.split(":",2);
                    if(p[0].equals("Format")) {
                        dialogueFormat = parseKeys(p[1]);
                    }
                    else if(p[0].equals("Dialogue")) {
                        addEvent(p[1]);
                    }
                }
            }
            else if (currentLine.matches("^Dialogue:.*$")) {
                addEvent(currentLine.split(":",2)[1]);
            }
        }

        return subtitles;
    }

    private void addEvent(String event) {
        Map<String,String> ev = parseLine(dialogueFormat, event.trim());
        int readOrder = Integer.parseInt(ev.get("readorder"));
        long start = parseTimecode(ev.get("start"));
        int marginL = Integer.parseInt(ev.get("marginl"));
        int marginR = Integer.parseInt(ev.get("marginr"));
        int marginV = Integer.parseInt(ev.get("marginv"));
        String styleName = ev.get("style");
        Style style = styles.get(styleName);
        if(style == null) {
            Log.e(TAG, "null style");
        }
        else {
            if(marginL==0) marginL = style.getMarginL();
            if(marginR==0) marginR = style.getMarginR();
            if(marginV==0) marginV = style.getMarginV();
        }
        int layer = Integer.parseInt(ev.get("layer"));
        String effect = ev.get("effect");
        String text = ev.get("text").replaceAll("\\\\N", "\n");
        String simpleText = text.replaceAll("\\{[^{]*\\}", "");
        Layout.Alignment textAlignment = null;
        float line = Cue.DIMEN_UNSET;
        int lineType = Cue.TYPE_UNSET;
        int lineAnchor = Cue.TYPE_UNSET;
        int position = Cue.TYPE_UNSET;
        int positionAnchor = Cue.TYPE_UNSET;
        int size = Cue.TYPE_UNSET;
        Cue cue = new Cue(simpleText);
        //Cue cue = new  Cue(simpleText, textAlignment, line, lineType, lineAnchor,  position, positionAnchor, size);
        subtitles.add(readOrder, cue, start);
    }

    private static void parseInfo(ParsableByteArray data) {
    }

    private void parseStyles(Map<String, Style> styles, ParsableByteArray data) {
        while(true) {
            String line = data.readLine();
            if(line==null)
                break;
            Log.i(TAG, line);
            if(!line.contains(":"))
                break;
            String p[] = line.split(":",2);
            if(p[0].equals("Format")) {
                styleFormat = parseKeys(p[1]);
            }
            else if(p[0].equals("Style")) {
                Style s = new Style(parseLine(styleFormat, p[1]));
                styles.put(s.getName(), s);
            }
        }
    }

    private String[] parseKeys(String format) {
        String keys[] = format.split(", *");
        String r[] = new String[keys.length];
        for(int i=0; i<r.length; i++) {
            r[i] = keys[i].trim().toLowerCase();
        }
        return r;
    }

    private static Map<String,String> parseLine(String[] keys, String event) {
        Map<String,String> result = new HashMap<>();
        String fields[] = event.split(", *", keys.length);
        for(int i=0; i<keys.length; i++) {
            String k = keys[i];
            String v = fields[i].trim();
            result.put(k, v);
        }
        return result;
    }

    public static String formatTimeCode(long tc_us) {
        long seconds = tc_us / 1000000;
        long us = tc_us - 1000000*seconds;
        long minutes = seconds / 60;
        seconds -= 60 * minutes;
        long hours = minutes / 60;
        minutes -= 60*hours;
        double sec = seconds + ((float)us)/1000000.0;
        return String.format("%01d:%02d:%06.3f", hours, minutes, sec);
    }

    private static long parseTimecode(String time) {
        String p[] = time.split(":");
        long hours = Long.parseLong(p[0]);
        long minutes = Long.parseLong(p[1]);
        float seconds = Float.parseFloat(p[2]);
        float us = 1000000*seconds;
        long lus = ((long)us);
        return lus + 1000000 * (60 * (minutes + 60 * hours));
    }
}
