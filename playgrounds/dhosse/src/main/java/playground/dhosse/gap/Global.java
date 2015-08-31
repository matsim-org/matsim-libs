package playground.dhosse.gap;

public class Global {
	
	public static final String runID = "run6";
	
	//the activity types used in the gap scenario
	public static enum ActType{
		home,
		work,
		education,
		shop,
		leisure,
		other
	};
	
	//subpopulation classes
	public static final String USER_GROUP = "usrGroup";
	public static final String GP_CAR = "GP_CAR";
	public static final String GP_LICENSE = "GP_LICENSE";
	public static final String GP_NO_CAR = "GP_NO_CAR";
	public static final String COMMUTER = "COMMUTER";
	
	//age classes
	public static final String AGE = "AGE";
	public static final String CHILD = "CHILD";
	public static final String ADULT = "ADULT";
	public static final String PENSIONER = "PENSIONER";
	
	//sex classes
	public static final String SEX = "SEX";
	public static final String MALE = "MALE";
	public static final String FEMALE = "FEMALE";
	
	//employment
	public static final String EMPLOYMENT = "EMPLOYEMENT";
	public static final String EMPLOYED = "EMPLOYED";
	public static final String NOT_EMPLOYED = "NOT_EMPLOYED";
	
	//car availability
	public static final String CAR_AVAILABILITY = "CAR_AVAILABILITY";
	public static final String CAR_AVAIL = "CAR_AVAIL";
	public static final String NO_CAR = "NO_CAR";
	
	//driving license
	public static final String LICENSE = "LICENSE";
	public static final String HAS_LICENSE = "HAS_LICENSE";
	public static final String NO_LICENSE = "NO_LICENSE";
	
	//status of residence
	public static final String RESIDENCE = "RESIDENCE";
	public static final String INHABITANT = "INHABITANT";
	
	public static final String CAR_OPTION = "CAR_OPTION";
	
}
