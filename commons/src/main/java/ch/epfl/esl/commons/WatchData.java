package ch.epfl.esl.commons;

import java.util.ArrayList;

/**
 * Created by Lara on 3/7/2018.
 * Data collected on the watch
 */

public class WatchData {
    public static ArrayList<Float> heartRateArray = new ArrayList<Float>();
    public static ArrayList<Long> timestampArray = new ArrayList<Long>();

    public void clear() {
        heartRateArray.clear();
        timestampArray.clear();
    }
}
