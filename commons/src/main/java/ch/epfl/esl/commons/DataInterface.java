package ch.epfl.esl.commons;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by Lara on 3/7/2018.
 * Interface between HR data and sqlite database
 */

@Dao
public interface DataInterface {
    @Query("SELECT * FROM HeartRateData ORDER BY timestamp")
    List<HeartRateData> getLastValues();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertSensorData(HeartRateData heartRateData);

    @Query("DELETE FROM HeartRateData")
    void deleteAll();

}
