package playground.acmarmol.matsim2030.microcensus2010;


/**
* 
* Holds MZ2010 answers.
* The purpose of this class is to allow easy editing of parsers coding.
* 
*
* @author acmarmol
* 
*/

public class MZConstants{
	
	//general 
		public static final String NO_ANSWER = "no answer";
		public static final String NOT_KNOWN = "not known";
		public static final String UNSPECIFIED = "unspecified";
		public static final String OTHER = "other";
		public static final String YES = "yes";
		public static final String NO = "no";

		
		public static final String TYPE = "type";
		
		public static final String SWISS_CODE = "8100";
	
		
	//ziel person
		public static final String AGE = "age";
		//gender
		public static final String GENDER = "gender";
		public static final String FEMALE = "f";
		public static final String MALE = "m";
		//transport mode availability
		public static final String ALWAYS = "always";
		public static final String ARRANGEMENT = "by arrengement";
		public static final String NEVER = "never";
		//employment status
		public static final String INDEPENDENT = "independent";
		public static final String MITARBEITENDES = "Mitarbeitendes Familienmitglied";
		public static final String EMPLOYEE = "employee";
		public static final String TRAINEE = "apprentice-trainee";
		public static final String UNEMPLOYED = "unemployed";
		public static final String AUSBILDUNG = "education or training";
		public static final String RETIRED = "retired";
		public static final String DISABLED = "disabled";
		public static final String HOUSEWIFE_HOUSEHUSBAND = "housewife/househusband";
		public static final String OTHER_INACTIVE = "other inactive";
		//wege information
		public static final String TOTAL_TRIPS_INLAND = "total weges inland";
		public static final String TOTAL_TRIPS_DISTANCE = "total trips distance";
		public static final String TOTAL_TRIPS_DURATION = "total trips duration";
		
	//vehicles
		public static final String YEAR_REGISTRATION = "year of registration";
		public static final String MONTH_REGISTRATION = "month of registration";
		//fuel type:
		public static final String FUEL_TYPE = "fuel type";
		public static final String BENZIN = "benzin";
		public static final String DIESEL = "diesel";
		public static final String HYBRIDE85GAS = "hybridE85Gas";
		public static final String JUST_FOR_CAR = "only available for car mode";

	//months
		public static final String JANUARY = "january";
		public static final String FEBRUARY = "february";
		public static final String MARCH = "march";
		public static final String APRIL = "april";
		public static final String MAY = "may";
		public static final String JUNE = "june";
		public static final String JULY = "july";
		public static final String AUGUST = "august";
		public static final String SEPTEMBER = "september";
		public static final String OCTOBER = "october";
		public static final String NOVEMBER = "november";
		public static final String DECEMBER = "december";
		
	//days
		public static final String MONDAY = "monday";
		public static final String TUESDAY = "tuesday";
		public static final String WEDNESDAY = "wednesday";
		public static final String THURSDAY = "thursday";
		public static final String FRIDAY = "friday";
		public static final String SATURDAY = "saturday";
		public static final String SUNDAY= "sunday";
		
	//trip purposes/activities
		//directly from MZ
		public static final String HOME = "home";
		public static final String CHANGE = "Change, change of transport, car park";
		public static final String WORK = "work";
		public static final String EDUCATION = "education";
		public static final String SHOPPING = "shopping";
		public static final String ERRANDS = "errands and use of services";
		public static final String BUSINESS = "business";
		public static final String DIENSTFAHRT = "dienstfahrt";
		public static final String LEISURE= "leisure";
		public static final String ACCOMPANYING_CHILDREN = "accompanying (children)";
		public static final String ACCOMPANYING_NOT_CHILDREN = "accompanying (not children)";
		public static final String FOREIGN_PROPERTY = "foreign property";
		public static final String OVERNIGHT = "overnight away";
		public static final String PSEUDOETAPPE = "pseudoetappe";
		//created during cross border handling
		public static final String AIRPORT = "airport";
		public static final String BORDER = "border";
		public static final String ABROAD_TELEPORT = "abroad_teleport";
		
	//transport modes
		public static final String PLANE = "plane";
		public static final String TRAIN = "train";
		public static final String POSTAUTO = "postauto";
		public static final String SHIP = "ship";
		public static final String TRAM = "tram";
		public static final String BUS = "bus";
		public static final String SONSTINGER_OEV = "sonstigerOeV";
		public static final String REISECAR = "reisecar";
		public static final String CAR = "car";
		public static final String TRUCK = "truck";
		public static final String TAXI = "taxi";
		public static final String MOTORCYCLE = "motorcycle";
		public static final String MOFA = "mofa";
		public static final String BYCICLE = "bycicle";
		public static final String WALK = "walk";
		public static final String SKATEBOARD = "skateboard";
		
		public static final String KLEINMOTORRAD = "small motorcycle";
		public static final String MOTORRAD_FAHRER = "motorcycle-driver";
		public static final String MOTORRAD_MITFAHRER = "motorcycle-not-driver";
		public static final String CAR_FAHRER = "car-driver";
		public static final String CAR_MITFAHRER = "car-not-driver";
		public static final String CABLE_CAR = "cable car";
		public static final String BORDER_CROSSING = "border crossing";
		
	//legs attributes
		public static final String START_COUNTRY = "start country";
		public static final String END_COUNTRY = "end country";
		public static final String DEPARTURE = "departure";
		public static final String ARRIVAL = "arrival";
		public static final String PRINCIPAL_MODE = "principal mode";
		public static final String NUMBER_STAGES = "number of stages";
		public static final String STAGE = "stage";
		
	//other activities
		public static final String NO_OTHER = "no other activity";
		public static final String REGISTERED_UNEMPLOYED = " registered as unemployed";
		public static final String LOOKING_JOB = "not registered, but looking for a job";
		public static final String UNEMPLOYED_NO_OTHER = "unemployed/no other"; 
		
	
	//attribute names
		//zielpersonen parser
		public static final String PERSON_WEIGHT = "person weight";
		public static final String HOUSEHOLD_SIZE = "household size";
		public static final String HOUSEHOLD_INCOME= "household income";
		public static final String DAY_OF_WEEK = "day of week";
		public static final String EMPLOYMENT_STATUS = "work: employment status";
		public static final String LEVEL_EMPLOYMENT = "work: level of employment";
		public static final String OTHER_ACTIVITY1 = "other activity-1";
		public static final String OTHER_ACTIVITY2 = "other activity-2";
		public static final String OTHER_ACTIVITY3 = "other activity-3";
		public static final String WORK_LOCATION_COORD = "work: location coord";
		public static final String DRIVING_LICENCE = "driving licence";
		public static final String CAR_AVAILABILITY = "availability: car";
		public static final String MOTORCYCLE_AVAILABILITY = "availability: motorcycle";
		public static final String SMALL_MOTORCYCLE_AVAILABILITY = "availability: small motorcycle";
		public static final String MOFA_AVAILABILITY = "availability: mofa";
		public static final String BICYCLE_AVAILABILITY = "availability: bicycle";
		public static final String CAR_SHARING_MEMBERSHIP = "car sharing membership";
		public static final String ABBO_HT = "abonnement: Halbtax";
		public static final String ABBO_GA1 = "abonnement: GA first class";
		public static final String ABBO_GA2 = "abonnement: GA second class";
		public static final String ABBO_VERBUND = "abonnement: Verbund";
		public static final String ABBO_STRECKEN = "abonnement: Stecken";
		public static final String ABBO_GLEIS7 = "abonnement: Gleis 7";

		//household parse
		public static final String HOUSEHOLD_NUMBER = "household number";
		public static final String HOUSEHOLD_WEIGHT = "household weight";
		public static final String MUNICIPALITY = "municipality";
		public static final String REGION = "region";
		public static final String CANTON = "canton";
		public static final String COORD = "coord";
		public static final String TOTAL_CARS = "total cars";
		public static final String TOTAL_MOTORCYCLES = "total motorcycles";
		public static final String TOTAL_SMALL_MOTORCYCLES = "total small motorcycles";
		public static final String TOTAL_MOFAS = "total mofas";
		public static final String TOTAL_BYCICLES = "total bicycles";
		
		
		

		
		
		
	
	
	

}
