package playground.dziemke.analysis.general.srv;

/**
 * @author gthunig on 30.03.2017.
 */
public class SrvTripUtils {

    static final String HOUSEHOLD_ID = "HHNR";
    static final String PERSON_ID = "PNR";
    static final String TRIP_ID = "WNR";
    // activity end corresponds to start of trip
    static final String DEPARTURE_ZONE_ID = "START_TEILBEZIRK2";
    static final String ACTIVITY_END_ACT_TYPE = "V_START_ZWECK";
    static final String ACTIVITY_START_ACT_TYPE = "V_ZWECK";
    static final String USE_WALK = "V_FUSS";
    static final String USE_BIKE = "V_RAD";
    static final String USE_RIDE = "V_MOP";
    static final String USE_BUS = "V_BUS";
    static final String USE_TRAM = "V_STRAB";
    static final String USE_UNDERG = "V_UBAHN";
    static final String USE_SBAHN = "V_SBAHN";
    static final String USE_LOCAL_TRAIN = "V_NZUG";
    static final String USE_LONG_DIST_TRAIN = "V_FZUG";
    static final String USE_HOUSEHOLD_CAR = "V_HHPKW_F";
    static final String USE_OTHER_CAR = "V_ANDPKW_F";
    static final String USE_HOUSEHOLD_CAR_POOL = "V_HHPKW_MF";
    static final String USE_OTHER_CAR_POOL = "V_ANDPKW_MF";
    static final String ARRIVAL_ZONE_ID = "ZIEL_TEILBEZIRK2";
    // departure time corresponds to "beginn" of trip
    static final String DEPARTURE_TIME_MIN = "V_BEGINN"; // in min (Ahrens2010SrVDatenaufbereitung, p.47)
    static final String ARRIVAL_TIME_MIN = "V_ANKUNFT"; // in min (Ahrens2010SrVDatenaufbereitung, p.47)
    static final String DISTANCE_BEELINE_KM = "V_LAENGE"; // in km (Ahrens2010SrVDatenaufbereitung, p.47)
//    private static final String MODE = "E_HVM";
    static final String MODE = "E_HVM4";
    static final String DURATION_MIN = "E_DAUER"; // in min (Ahrens2010SrVDatenaufbereitung, p.48)
    static final String SPEED_KM_H = "E_GESCHW"; // in km/h (Ahrens2010SrVDatenaufbereitung, p.48)
    static final String DISTANCE_ROUTED_FASTEST_KM = "E_LAENGE_SCHNELL"; // in km (Ahrens2010SrVDatenaufbereitung, p.48)
    static final String DISTANCE_ROUTED_SHORTEST_KM = "E_LAENGE_KUERZ"; // in km (Ahrens2010SrVDatenaufbereitung, p.49)
    static final String WEIGHT = "GEWICHT_W";

}
