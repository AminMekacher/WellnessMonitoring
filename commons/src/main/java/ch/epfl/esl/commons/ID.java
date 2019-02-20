package ch.epfl.esl.commons;

/**
 * Created by orlandic on 02/03/2018.
 * All channel IDs, etc. needed for data transfer between classes
 */

public class ID {
    public static final String TEST_CHANNEL_ID = "TestReminder";
    public static final String FIREBASE_CHANNEL_ID = "FirebaseNotification";
    public static final int tuesdayNotificationID = 1;
    public static final int thursdayNotificationRepeatID = 2;
    public static final int fridayNotificationTuesdayID = 3;
    public static final int beCareNotificationID = 4;
    public static final String testReminderAction = "ch.epfl.esl.studentactivitymonitoring.TEST_REMINDER";
    public static final String stopWorkoutReminderAction = "ch.epfl.esl.studentactivitymonitoring.STOP_WORKOUT";

    public static final String WATCH_DATA_PATH = "/watchData";
    public static final String WATCH_DATA_KEY = "watchData";

    public static final String START_ORTHO_PATH = "/start-ortho-test";
    public static final String START_ORTHO_KEY = "start-ortho-test";

    public static final String STOP_ORTHO_PATH = "/stop-ortho-test";
    public static final String STOP_ORTHO_KEY = "stop-ortho-test";

    public static final String heartRateArrayID = "heartRateArray";
    public static final String timeStampArrayID = "timestampArray";
    public static final String accuracyArrayID = "accuracyArray";
    public static final String workoutDurationID = "workoutDuration";

    public static boolean wasTestDoneToday = false;
    public static int userID;
}
