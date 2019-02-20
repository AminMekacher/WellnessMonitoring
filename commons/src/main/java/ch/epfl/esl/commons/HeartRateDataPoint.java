package ch.epfl.esl.commons;

/**
 * Created by Lara on 4/18/2018.
 */

public class HeartRateDataPoint {
    public long timeStamp;
    public float value;

    public HeartRateDataPoint(float Value, long timestamp) {
        timeStamp = timestamp;
        value = Value;
    }
}
