package com.google.android.exoplayer2.text.ssa;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.Subtitle;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.LongArray;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by cablej01 on 26/12/2016.
 */

public class SSASubtitle implements Subtitle {

    private List<Cue> cues = new ArrayList<>();
    private List<Long> cueTimesUs = new ArrayList<>();


    public SSASubtitle() {
        super();
    }

    public void add(int pos, Cue cue, long cueTimeUs) {
        cues.add(pos, cue);
        cueTimesUs.add(pos, cueTimeUs);
    }

    @Override
    public int getNextEventTimeIndex(long timeUs) {
        int index = Util.binarySearchCeil(cueTimesUs, timeUs, false, false);
        return index < cueTimesUs.size() ? index : C.INDEX_UNSET;
    }

    @Override
    public int getEventTimeCount() {
        return cueTimesUs.size();
    }

    @Override
    public long getEventTime(int index) {
        Assertions.checkArgument(index >= 0);
        Assertions.checkArgument(index < cueTimesUs.size());
        return cueTimesUs.get(index);
    }

    @Override
    public List<Cue> getCues(long timeUs) {
        Log.i("getCues", String.format("%d", timeUs));
        int index = Util.binarySearchFloor(cueTimesUs, timeUs, true, false);
        if (index == -1 || cues.get(index) == null) {
            // timeUs is earlier than the start of the first cue, or we have an empty cue.
            return Collections.emptyList();
        } else {
            return Collections.singletonList(cues.get(index));
        }
    }
}
