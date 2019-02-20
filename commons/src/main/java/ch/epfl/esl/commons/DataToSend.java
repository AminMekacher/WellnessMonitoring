package ch.epfl.esl.commons;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Lara on 2/23/2018.
 * Data to send to be.care platform
 */

public class DataToSend {

    public static String sessionType;


    public static int IDs = 0;
    public static int protocolID = 2;
    public static String protcolVersion = "1";
    public static int userID = ID.userID;


    //Start and end of overall data collection
    public static String startDate;
    public static String endDate;

    //Start and end dates of individual pieces
    public static String startSupine;
    public static String endSupine;
    public static String startStanding;
    public static String endStanding;

    //Array of heart rate values obtained from polar belt during orthostatic test
    public static ArrayList<Float> standingHeartRateArray;
    public static ArrayList<Float> supineHeartRateArray;

    public static String averageStandingHR; //need to be sent as strings
    public static String averageSupineHR;

    //Array of RR intervals obtainted from polar belt during orthostatic test
    public static ArrayList<Integer> standingRRIntervalArray;
    public static ArrayList<Integer> supineRRIntervalArray;

    //Timestamps of standing and supine orthostatic test data
    public static ArrayList<Long> standingTimestamps;
    public static ArrayList<Long> sittingTimestamps;

    //List of answers to the questions in the questionnaire
    public static ArrayList<String> answersToQuestionnaireArray;
    public static String averageResponse; //needs to be sent as string

    public void setSupine(String sessionTypeLocal, String startDateLocal, String startSupineLocal, String endSupineLocal,
                          ArrayList<Float> supineHeartRateArrayLocal, ArrayList<Integer> supineRRIntervalArrayLocal,
                          ArrayList<Long> sittingTimestampsLocal, String averageSupineHRLocal) {
        sessionType = sessionTypeLocal;
        startDate = startDateLocal;
        endDate = endSupineLocal;
        startSupine = startSupineLocal;
        endSupine = endSupineLocal;
        supineHeartRateArray = supineHeartRateArrayLocal;
        supineRRIntervalArray = supineRRIntervalArrayLocal;
        sittingTimestamps = sittingTimestampsLocal;
        averageSupineHR = averageSupineHRLocal;
    }

    public void setStanding(String startDateLocal, String startSupineLocal, String endSupineLocal,
                            String startStandingLocal, String endStandingLocal,
                            ArrayList<Float> supineHeartRateArrayLocal, ArrayList<Integer> supineRRIntervalArrayLocal,
                            ArrayList<Float> standingHeartRateArrayLocal, ArrayList<Integer> standingRRIntervalArrayLocal,
                            ArrayList<Long> sittingTimestampsLocal, ArrayList<Long> standingTimestampsLocal, String averageSupineHRLocal,
                            String averageStandingHRLocal) {
        startDate = startDateLocal;
        startSupine = startSupineLocal;
        startStanding = startStandingLocal;
        endStanding = endStandingLocal;
        endSupine = endSupineLocal;
        supineHeartRateArray = supineHeartRateArrayLocal;
        standingHeartRateArray = standingHeartRateArrayLocal;
        supineRRIntervalArray = supineRRIntervalArrayLocal;
        standingRRIntervalArray = standingRRIntervalArrayLocal;
        sittingTimestamps = sittingTimestampsLocal;
        standingTimestamps = standingTimestampsLocal;
        averageSupineHR = averageSupineHRLocal;
        averageStandingHR = averageStandingHRLocal;

    }

    public void setQuestionnaire(String sessionTypeLocal, ArrayList<String> answersToQuestionnaireArrayLocal, String averageResponseLocal, String endDateLocal){
        sessionType = sessionTypeLocal;
        answersToQuestionnaireArray = answersToQuestionnaireArrayLocal;
        averageResponse = averageResponseLocal;
        endDate = endDateLocal;
    }

    public void setOnlyQuestionnaire(String sessionTypeLocal, String startDateLocal, String endDateLocal, ArrayList<String> answersToQuestionnaireArrayLocal, String averageResponseLocal) {
        sessionType = sessionTypeLocal;
        startDate = startDateLocal;
        endDate = endDateLocal;
        startSupine = startDateLocal;
        endSupine = endDateLocal;
        answersToQuestionnaireArray = answersToQuestionnaireArrayLocal;
        averageResponse = averageResponseLocal;
    }

    public void clear(){
        sessionType = "";
        startDate = "";
        endDate = "";
        startSupine = "";
        startStanding = "";
        endStanding = "";
        endSupine = "";
        supineHeartRateArray = new ArrayList<Float>();
        standingHeartRateArray = new ArrayList<Float>();
        supineRRIntervalArray = new ArrayList<Integer>();
        standingRRIntervalArray = new ArrayList<Integer>();
        sittingTimestamps = new ArrayList<Long>();
        standingTimestamps = new ArrayList<Long>();
        averageSupineHR = "";
        averageStandingHR = "";
        answersToQuestionnaireArray  = new ArrayList<String>();;
        averageResponse = "";
    }

    //Set only questionnaire
    public JSONObject createJSONObjectQuestionnaire() {


        JSONObject supineGlobalData = new JSONObject();
        try {
            supineGlobalData.put(BeCare.GlobalData.AverageQuestionResponse, averageResponse);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject recordPhaseSupine = new JSONObject();
        try {
            recordPhaseSupine.put(BeCare.ID.startDate, startSupine);
            recordPhaseSupine.put(BeCare.ID.endDate, endSupine);
            recordPhaseSupine.put(BeCare.ID.globalData,supineGlobalData);
            JSONArray answers = new JSONArray(answersToQuestionnaireArray);
            recordPhaseSupine.put(BeCare.ID.responses, answers);
            recordPhaseSupine.put(BeCare.ID.dataSets, new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        JSONArray recordPhases = new JSONArray();
        recordPhases.put(recordPhaseSupine);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BeCare.ID.endDate,endDate);
            jsonObject.put(BeCare.ID.id,IDs);
            jsonObject.put(BeCare.ID.globalData, new JSONObject());
            jsonObject.put("userId",userID);
            jsonObject.put(BeCare.ID.protocolId, protocolID);
            jsonObject.put(BeCare.ID.protocolVersion, protcolVersion);
            jsonObject.put(BeCare.ID.sessionType, sessionType);
            jsonObject.put(BeCare.ID.startDate, startDate);
            jsonObject.put(BeCare.ID.recordPhases, recordPhases);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    //Send only supine data
    public JSONObject createJSONObjectSupine() {
        JSONArray sittingTimestampsArray = new JSONArray(sittingTimestamps);
        JSONObject dataSetSupineHR = new JSONObject();
        try {
            dataSetSupineHR.put(BeCare.ID.dataType,BeCare.DataType.HR);
            dataSetSupineHR.put(BeCare.ID.timestamps,sittingTimestampsArray);
            JSONArray supineHeartRate = new JSONArray(supineHeartRateArray);
            dataSetSupineHR.put(BeCare.ID.values,supineHeartRate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject dataSetSupineRR = new JSONObject();
        try {
            dataSetSupineRR.put(BeCare.ID.dataType,BeCare.DataType.RR);
            dataSetSupineRR.put(BeCare.ID.timestamps,sittingTimestampsArray);
            JSONArray supineRRIntervals = new JSONArray(supineRRIntervalArray);
            dataSetSupineRR.put(BeCare.ID.values,supineRRIntervals);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray dataSetsSupine = new JSONArray();
        dataSetsSupine.put(dataSetSupineHR);
        dataSetsSupine.put(dataSetSupineRR);

        JSONObject supineGlobalData = new JSONObject();
        try {
            supineGlobalData.put(BeCare.GlobalData.AverageHR, averageSupineHR);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject recordPhaseSupine = new JSONObject();
        try {
            recordPhaseSupine.put(BeCare.ID.startDate, startSupine);
            recordPhaseSupine.put(BeCare.ID.endDate, endSupine);
            recordPhaseSupine.put(BeCare.ID.globalData,supineGlobalData);
            JSONArray answers = new JSONArray(answersToQuestionnaireArray);
            recordPhaseSupine.put(BeCare.ID.responses, answers);
            recordPhaseSupine.put(BeCare.ID.dataSets, dataSetsSupine);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        JSONArray recordPhases = new JSONArray();
        recordPhases.put(recordPhaseSupine);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BeCare.ID.endDate,endDate);
            jsonObject.put(BeCare.ID.id,IDs);
            jsonObject.put(BeCare.ID.globalData, new JSONObject());
            jsonObject.put("userId",userID);
            jsonObject.put(BeCare.ID.protocolId, protocolID);
            jsonObject.put(BeCare.ID.protocolVersion, protcolVersion);
            jsonObject.put(BeCare.ID.sessionType, sessionType);
            jsonObject.put(BeCare.ID.startDate, startDate);
            jsonObject.put(BeCare.ID.recordPhases, recordPhases);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    //send the results of the orthostatic test session to be.care
    public JSONObject createJSONObjectWhole() {
        JSONArray sittingTimestampsArray = new JSONArray(sittingTimestamps);
        JSONObject dataSetSupineHR = new JSONObject();
        try {
            dataSetSupineHR.put(BeCare.ID.dataType,BeCare.DataType.HR);
            dataSetSupineHR.put(BeCare.ID.timestamps,sittingTimestampsArray);
            JSONArray supineHeartRate = new JSONArray(supineHeartRateArray);
            dataSetSupineHR.put(BeCare.ID.values,supineHeartRate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject dataSetSupineRR = new JSONObject();
        try {
            dataSetSupineRR.put(BeCare.ID.dataType,BeCare.DataType.RR);
            dataSetSupineRR.put(BeCare.ID.timestamps,sittingTimestampsArray);
            JSONArray supineRRIntervals = new JSONArray(supineRRIntervalArray);
            dataSetSupineRR.put(BeCare.ID.values,supineRRIntervals);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray dataSetsSupine = new JSONArray();
        dataSetsSupine.put(dataSetSupineHR);
        dataSetsSupine.put(dataSetSupineRR);

        JSONObject supineGlobalData = new JSONObject();
        try {
            supineGlobalData.put(BeCare.GlobalData.AverageHR, averageSupineHR);
            supineGlobalData.put(BeCare.GlobalData.AverageQuestionResponse, averageResponse);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject recordPhaseSupine = new JSONObject();
        try {
            recordPhaseSupine.put(BeCare.ID.startDate, startSupine);
            recordPhaseSupine.put(BeCare.ID.endDate, endSupine);
            recordPhaseSupine.put(BeCare.ID.globalData,supineGlobalData);
            JSONArray answers = new JSONArray(answersToQuestionnaireArray);
            recordPhaseSupine.put(BeCare.ID.responses, answers);
            recordPhaseSupine.put(BeCare.ID.dataSets, dataSetsSupine);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray standingTimestampsArray = new JSONArray(standingTimestamps);
        JSONObject dataSetStandingHR = new JSONObject();
        try {
            dataSetStandingHR.put(BeCare.ID.dataType, BeCare.DataType.HR);
            dataSetStandingHR.put(BeCare.ID.timestamps, standingTimestampsArray);
            JSONArray standingHR = new JSONArray(standingHeartRateArray);
            dataSetStandingHR.put(BeCare.ID.values, standingHR);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject dataSetStandingRR = new JSONObject();
        try {
            dataSetStandingRR.put(BeCare.ID.dataType, BeCare.DataType.RR);
            dataSetStandingRR.put(BeCare.ID.timestamps, standingTimestampsArray);
            JSONArray standingRR = new JSONArray(standingRRIntervalArray);
            dataSetStandingRR.put(BeCare.ID.values, standingRR);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject standingGlobalData = new JSONObject();
        try {
            standingGlobalData.put(BeCare.GlobalData.AverageHR, averageStandingHR);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        JSONArray dataSetsStanding = new JSONArray();
        dataSetsStanding.put(dataSetStandingHR);
        dataSetsStanding.put(dataSetStandingRR);

        JSONObject recordPhaseStanding = new JSONObject();
        try {
            recordPhaseStanding.put(BeCare.ID.startDate, startStanding);
            recordPhaseStanding.put(BeCare.ID.endDate, endStanding);
            recordPhaseStanding.put(BeCare.ID.globalData, standingGlobalData);
            recordPhaseStanding.put(BeCare.ID.responses, new JSONArray());
            recordPhaseStanding.put(BeCare.ID.dataSets, dataSetsStanding);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray recordPhases = new JSONArray();
        if (sessionType.equals(BeCare.SessionType.energy)) {
            recordPhases.put(recordPhaseSupine);
            recordPhases.put(recordPhaseStanding);
        } else if (sessionType.equals(BeCare.SessionType.questionnaire)) {
            recordPhases.put(recordPhaseSupine);
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BeCare.ID.endDate,endDate);
            jsonObject.put(BeCare.ID.id,IDs);
            jsonObject.put(BeCare.ID.globalData, new JSONObject());
            jsonObject.put("userId",userID);
            jsonObject.put(BeCare.ID.protocolId, protocolID);
            jsonObject.put(BeCare.ID.protocolVersion, protcolVersion);
            jsonObject.put(BeCare.ID.sessionType, sessionType);
            jsonObject.put(BeCare.ID.startDate, startDate);
            jsonObject.put(BeCare.ID.recordPhases, recordPhases);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


}
