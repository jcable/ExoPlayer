package com.google.android.exoplayer2.text.ssa;

import android.content.Intent;
import android.text.Layout;
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
import static android.R.attr.text;
import static android.R.attr.textAlignment;
import static com.google.android.exoplayer2.text.Cue.DIMEN_UNSET;
import static com.google.android.exoplayer2.text.Cue.TYPE_UNSET;

/**
 * Created by cablej01 on 26/12/2016.
 */

public class SSADecoder extends SimpleSubtitleDecoder {
    private static final String TAG = "SSADecoder";
    private String format = "Start, ReadOrder, Layer, Style, Name, MarginL, MarginR, MarginV, Effect, Text";
    private SSASubtitle subtitles = new SSASubtitle();

    public SSADecoder() {
        super("SSADecoder");
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
            if (currentLine.equals("[V4+ Styles]")) {
                parseStyles(data);
                continue;
            }
            if (currentLine.equals("[V4 Styles]")) {
                parseStyles(data);
                continue;
            }
            if (currentLine.equals("[Events]")) {
                parseEvents(format, data, subtitles);
            }
            if (currentLine.matches("^Dialogue:.*$")) {
                addEvent(subtitles, format, currentLine.split(":",2)[1]);
            }
        }

        return subtitles;
    }

    private static void parseInfo(ParsableByteArray data) {
        Map<String,List<String>> info = parseLines(data);
    }

    private static void parseStyles(ParsableByteArray data) {
        Map<String,List<String>> styles = parseLines(data);
    }

    //0,0,Watamote-internal/narrator,Serinuma,0000,0000,0000,,The prince should be with the princess.
    //ReadOrder, Layer, Style, Name, MarginL, MarginR, MarginV, Effect, Text
    private static void parseEvents(String format, ParsableByteArray data, SSASubtitle subtitles) {
        Map<String, List<String>> events = parseLines(data);
        if (events.containsKey("Format")) {
            format = events.get("Format").get(0);
        }
        for(String event : events.get("Dialogue")) {
            addEvent(subtitles, format, event);
        }
    }

    private static void addEvent(SSASubtitle subtitles, String format, String event) {
        Map<String,String> ev = parseEvent(format, event.trim());
        int readOrder = Integer.parseInt(ev.get("ReadOrder"));
        long start = parseTimecode(ev.get("Start"));
        int marginL = Integer.parseInt(ev.get("MarginL"));
        int marginR = Integer.parseInt(ev.get("MarginR"));
        int marginV = Integer.parseInt(ev.get("MarginV"));
        int layer = Integer.parseInt(ev.get("Layer"));
        String style = ev.get("Style");
        String effect = ev.get("Effect");
        String text = ev.get("Text");
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

    private static Map<String,String> parseEvent(String format, String event) {
        String keys[] = format.split(", *");
        Map<String,String> result = new HashMap<>();
        String fields[] = event.split(", *", keys.length);
        for(int i=0; i<keys.length; i++) {
            String k = keys[i].trim();
            String v = fields[i];
            result.put(k, v);
        }
        return result;
    }

    private static Map<String,List<String>> parseLines(ParsableByteArray data) {
        Map<String,List<String>> result = new HashMap<String, List<String>>();
        while(true) {
            String line = data.readLine();
            if(line==null)
                break;
            Log.i(TAG, line);
            if(!line.contains(":"))
                break;
            String p[] = line.split(":",2);
            if(result.containsKey(p[0])) {
                result.get(p[0]).add(p[1]);
            }
            else {
                List<String> l = new ArrayList<String>();
                l.add(p[1]);
                result.put(p[0], l);
            }
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
        return String.format("%02d:%02d:%02d.%d", hours, minutes, seconds, us);
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
