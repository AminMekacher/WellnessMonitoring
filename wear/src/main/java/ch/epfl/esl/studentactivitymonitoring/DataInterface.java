package ch.epfl.esl.studentactivitymonitoring;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.RoomWarnings;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.IGNORE;

/**
 * Created by Lara on 3/7/2018.
 */

@Dao
public interface DataInterface {
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM HeartRateData ORDER BY timestamp")
    List<HeartRateData> getLastValues();

    @Insert(onConflict = IGNORE)
    long insertSensorData(HeartRateData heartRateData);

    @Query("DELETE FROM HeartRateData")
    void deleteAll();

}
