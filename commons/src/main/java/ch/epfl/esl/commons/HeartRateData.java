package ch.epfl.esl.commons;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;

import java.util.ArrayList;

/**
 * Created by Lara on 3/7/2018.
 * A data object of HR values saved in Room (from watch-monitored workouts)
 */

@Entity
public class HeartRateData {

    public final static int HEART_RATE = 0;

    @PrimaryKey(autoGenerate = true)
    public int uid;
    @ColumnInfo
    public long timestamp;
    @ColumnInfo
    public ArrayList<Float> value;

}

//convert floats into sqlite-readable type
class ConverterArrayToString {

    @TypeConverter
    public static String StringfromFloatArray(ArrayList<Float> values) {
        String outputString = "";
        for (Float value: values) {
            outputString += (String.valueOf(value) + ":");
        }
        return outputString;
    }

    @TypeConverter
    public static ArrayList<Float> StringToFloatArray(String values) {
        ArrayList<Float> outputFloatArray = new ArrayList<Float>();
        String[] strings = values.split(":");
        for (String string : strings){
            if (!string.isEmpty()) {
                outputFloatArray.add(Float.valueOf(string));
            }
        }
        return outputFloatArray;
    }
}

