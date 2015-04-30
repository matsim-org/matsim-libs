package playground.sergioo.hits2012;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Person {

	//Enumerations
	public static enum Column {
	
		INCOMPLETE(37, "AL"),
		NO_ELIGIBLE_UNDER_4(38, "AM"),
		NO_ELIGIBLE_REASON(40, "AO"),
		AGE(41, "AP"),
		TYPE_NATIONALITY(42, "AQ"),
		GENDER(44, "AS"),
		HAS_CAR(45, "AT"),
		HAS_BIKE(46, "AU"),
		HAS_VAN_BUS(47, "AV"),
		HAS_LICENSE(48, "AW"),
		HAS_MOBILITY(49, "AX"),
		AIDS(50, "AY"),
		AIDS_OTHER(51, "AZ"),
		EMPLOYMENT(52, "BA"),
		EDUCATION(53, "BB"),
		SCHOOL_NAME(54, "BC"),
		SCHOOL_POSTAL_CODE(55, "BD"),
		OCCUPATION(56,"BE"),
		INDUSTRY(57,"BF"),
		FIXED_WORK_PLACE(59,"BH"),
		WORK_HOURS(60,"BI"),
		INCOME(61,"BJ"),
		DATE(62, "BK"),
		SURVEY_DAY(63,"BL"),
		START_HOME(64,"BM"),
		FIRST_ACTIVITY(66,"BO"),
		NO_TRIP_REASON(68,"BQ"),
		NO_TRIP_REASON_OTHER(69,"BR"),
		LAST_TIME_TRIP(70,"BS"),
		FACTOR(106, "DC");
	
		//Attributes
		public int column;
		public String columnName;
	
		//Constructors
		private Column(int column, String columnName) {
			this.column = column;
			this.columnName = columnName;
		}
	
	}
	public enum Role {
		MAIN, PARTNER, YOUNGER, OLDER
	}
	public static enum Day {
		Mon,Tue,Wed,Thur,Fri,Sat,Sun;
	}

	//Classes
	public static class Interval {
	
		//Attributes
		protected final int upperLimit;
		protected final int lowerLimit;
	
		//Constructors
		public Interval(int upperLimit, int lowerLimit) {
			this.upperLimit = upperLimit;
			this.lowerLimit = lowerLimit;
		}
		
		//Methods
		public int getUpperLimit() {
			return upperLimit;
		}
		public int getLowerLimit() {
			return lowerLimit;
		}
		public int getCenter() {
			return (upperLimit+lowerLimit)/2;
		}
	
	}
	public static class AgeInterval extends Interval {
	
		//Constructors
		public AgeInterval(String limits) {
			super(limits.startsWith("85")?85:new Integer(limits.split(" ")[0].split("-")[0]), limits.startsWith("85")?100:new Integer(limits.split(" ")[0].split("-")[1]));
		}
	
		public AgeInterval(int upperLimit, int lowerLimit) {
			super(upperLimit, lowerLimit);
		}
	
		public AgeInterval() {
			super(0, 0);
		}
	
	}
	public static class IncomeInterval extends Interval {
	
		//Constructors
		public IncomeInterval(String limits) {
			super(limits.startsWith("$8000")?8000:new Integer(limits.replaceAll("\\$","").split("-")[0]), limits.startsWith("$8000")?20000:new Integer(limits.replaceAll("\\$","").split("-")[1]));
		}
	
		public IncomeInterval(int upperLimit, int lowerLimit) {
			super(upperLimit, lowerLimit);
		}
	
		public IncomeInterval() {
			super(0, 0);
		}
	
	}

	//Constants
	public static Map<String, String> SCHOOLS = new HashMap<String, String>();

	//Attributes
	private final String id;
	private final SortedMap<String, Trip> trips = new TreeMap<String, Trip>();
	private String noEligibleReason;
	private final AgeInterval ageInterval;
	private final String typeNationality;
	private final String gender;
	private final boolean hasCar;
	private final boolean hasBike;
	private final boolean hasVanBus;
	private final boolean hasLicence;
	private final boolean hasMobility;
	private final String aids;
	private final String aidsOther;
	private final String employment;
	private final String education;
	private final String school;
	private final String occupation;
	private final String industry;
	private final String workPlacePostalCode;
	private final String workHours;
	private IncomeInterval incomeInterval;
	private final Date date;
	private final Day surveyDay;
	private final boolean startHome;
	private final String firstActivity;
	private final String noTripReason;
	private final String noTripReasonOther;
	private final String lastTimeTrip;
	private final double factor;
	private Role role;
	
	//Constructors
	public Person(String id, String noEligibleReason, double factor) {
		this(id, null, null, null, false, false, false, false, false, null, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, null, factor);
		this.noEligibleReason = noEligibleReason;
	}

	public Person(String id, AgeInterval ageInterval,
			String typeNationality, String gender, boolean hasCar,
			boolean hasBike, boolean hasVanBus, boolean hasLicence,
			boolean hasMobility, String aids, String aidsOther,
			String employment, String education, String school,
			String occupation, String industry, String workPlacePostalCode,
			String workHours, IncomeInterval incomeInterval, Date date,
			Day surveyDay, boolean startHome, String firstActivity,
			String noTripReason, String noTripReasonOther,
			String lastTimeTrip, double factor) {
		super();
		this.id = id;
		this.ageInterval = ageInterval;
		this.typeNationality = typeNationality;
		this.gender = gender;
		this.hasCar = hasCar;
		this.hasBike = hasBike;
		this.hasVanBus = hasVanBus;
		this.hasLicence = hasLicence;
		this.hasMobility = hasMobility;
		this.aids = aids;
		this.aidsOther = aidsOther;
		this.employment = employment;
		this.education = education;
		this.school = school;
		this.occupation = occupation;
		this.industry = industry;
		this.workPlacePostalCode = workPlacePostalCode;
		this.workHours = workHours;
		this.incomeInterval = incomeInterval;
		this.date = date;
		this.surveyDay = surveyDay;
		this.startHome = startHome;
		this.firstActivity = firstActivity;
		this.noTripReason = noTripReason;
		this.noTripReasonOther = noTripReasonOther;
		this.lastTimeTrip = lastTimeTrip;
		this.factor = factor;
	}


	//Methods
	public String getId() {
		return id;
	}
	public Trip getTrip(String id) {
		return trips.get(id);
	}
	public void addTrip(Trip trip) {
		trips.put(trip.getId(), trip);
	}
	public SortedMap<String, Trip> getTrips() {
		return trips;
	}
	public String getNoEligibleReason() {
		return noEligibleReason;
	}
	public AgeInterval getAgeInterval() {
		return ageInterval;
	}
	public String getTypeNationality() {
		return typeNationality;
	}
	public String getGender() {
		return gender;
	}
	public boolean hasCar() {
		return hasCar;
	}
	public boolean hasBike() {
		return hasBike;
	}
	public boolean hasVanBus() {
		return hasVanBus;
	}
	public boolean hasLicence() {
		return hasLicence;
	}
	public boolean hasMobility() {
		return hasMobility;
	}
	public String getAids() {
		return aids.equals("Others")?aidsOther:aids;
	}
	public String getEmployment() {
		return employment;
	}
	public String getEducation() {
		return education;
	}
	public String getSchool() {
		return school;
	}
	public String getOccupation() {
		return occupation;
	}
	public String getIndustry() {
		return industry;
	}
	public String getWorkPlacePostalCode() {
		return workPlacePostalCode;
	}
	public String getWorkHours() {
		return workHours;
	}
	public IncomeInterval getIncomeInterval() {
		return incomeInterval;
	}
	public int getMainIncome(Household household) {
		if(role==Role.MAIN)
			return incomeInterval.getCenter();
		else
			for(Person person:household.getPersons().values())
				if(person.getRole()==Role.MAIN)
					return incomeInterval.getCenter()+person.getIncomeInterval().getCenter()/2;
		return incomeInterval.getCenter();
	}
	public Date getDate() {
		return date;
	}
	public Day getSurveyDay() {
		return surveyDay;
	}
	public boolean isStartHome() {
		return startHome;
	}
	public String getFirstActivity() {
		return firstActivity;
	}
	public String getNoTripReason() {
		return noTripReason.equals("Others")?noTripReasonOther:noTripReason;
	}
	public String getLastTimeTrip() {
		return lastTimeTrip;
	}
	public double getFactor() {
		return factor;
	}
	public String[] getActivityChain() {
		String[] chain = new String[trips.size()+1];
		chain[0] = firstActivity;
		int i=1;
		for(Trip trip:trips.values())
			chain[i++] = trip.getPurpose();
		return chain;
	}
	public void setIncomeInterval(IncomeInterval incomeInterval) {
		this.incomeInterval = incomeInterval;
	}
	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}

}
