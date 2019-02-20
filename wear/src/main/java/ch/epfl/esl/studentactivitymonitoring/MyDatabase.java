package ch.epfl.esl.studentactivitymonitoring;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

/**
 * Created by Lara on 3/7/2018.
 */

@Database(entities = {HeartRateData.class}, version = 2, exportSchema = false)
@TypeConverters({ConverterArrayToString.class})
public abstract class MyDatabase extends RoomDatabase {
    public static MyDatabase INSTANCE;
    public abstract DataInterface sensorDataDao();
    public static synchronized MyDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE =
                    Room.databaseBuilder(context.getApplicationContext(),
                            MyDatabase.class, "HeartRateDB").fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }
    public static void destroyInstance() {INSTANCE = null;}
}

