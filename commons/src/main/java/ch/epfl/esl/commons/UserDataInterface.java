package ch.epfl.esl.commons;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by Lara on 3/20/2018.
 * Interface between user id and database
 */

@Dao
public interface UserDataInterface {

    @Query("SELECT userID FROM User")
    int getLastValue();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertSensorData(User user);

    @Query("DELETE FROM User")
    void deleteAll();
}
