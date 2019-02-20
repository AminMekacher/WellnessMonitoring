package ch.epfl.esl.commons;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by Lara on 3/20/2018.
 * User object to store user ids in room
 */

@Entity
public class User {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo
    public int userID;
}
