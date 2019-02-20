package ch.epfl.esl.commons;

/**
 * Created by orlandic on 05/03/2018.
 */

public class BeCare {

    public static final String baseURL = "https://api.becare.me/api/";
    //"https://dev.becare.me/api/"; for development
    //"https://api.becare.me/api/"; for students
    public class SessionType {
        public static final String questionnaire = "CHECKUP_QUESTIONNARY";
        public static final String energy = "CHECKUP_ENERGY";
        public static final String other = "OTHER";
    }

    public class DataType {
        public static final String HR  = "HR";
        public static final String RR = "RR";
    }

    public class GlobalData {
        public static final String AverageHR = "HR_AVG";
        public static final String AverageQuestionResponse = "RESPONSES_AVG";
    }

    public class ID {
        public static final String id = "id";
        public static final String startDate = "startDate";
        public static final String endDate = "endDate";
        public static final String userId = "userId";
        public static final String protocolId = "protocolId";
        public static final String protocolVersion = "protocolVersion";
        public static final String sessionType  = "sessionType";
        public static final String recordPhases = "recordPhases";
        public static final String dataSets = "dataSets";
        public static final String dataType = "dataType";
        public static final String values = "values";
        public static final String timestamps = "timestamps";
        public static final String responses = "responses";
        public static final String globalData = "globalData";
    }

}
