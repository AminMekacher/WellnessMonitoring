package ch.epfl.esl.commons;

/**
 * Created by Lara on 4/18/2018.
 */

public class ActivitySegment {
    public long startTime;
    public long endTime;
    public int activityType;

    public ActivitySegment(long start, long end, int type) {
        startTime = start;
        endTime = end;
        activityType = type;
    }

}
