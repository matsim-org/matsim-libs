package playground.sergioo.hits2012;

/*import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;*/
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

//import javax.swing.JFrame;






import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;

import playground.sergioo.hits2012.Person.Day;
import playground.sergioo.hits2012.Person.IncomeInterval;
import playground.sergioo.hits2012.Trip.Purpose;
import playground.sergioo.hits2012.stages.CycleStage;
import playground.sergioo.hits2012.stages.OtherBusStage;
import playground.sergioo.hits2012.stages.PublicBusStage;
import playground.sergioo.hits2012.stages.MRTStage;
import playground.sergioo.hits2012.stages.MotorDriverStage;
import playground.sergioo.hits2012.stages.MotorStage;
import playground.sergioo.hits2012.stages.StationStage;
import playground.sergioo.hits2012.stages.TaxiStage;

public class HitsReader {
	
	private final static Map<String, String> placeTypesMap = new HashMap<String, String>();
	
	private final static Map<String, String> purposesMap = new HashMap<String, String>();
	
	public static void main(String[] args) throws IOException, ParseException {
		Map<String, Household> households = readHits(args[0]);
		PrintWriter printer = new PrintWriter("./data/locs.csv");
		for(Entry<String, Coord> loc:Location.SINGAPORE_COORDS_MAP.entrySet())
			printer.println(loc.getKey()+","+loc.getValue().getX()+","+loc.getValue().getY());
		printer.close();
		System.out.println("Households: "+households.size());
		int numPersons = 0;
		for(Household household:households.values())
			numPersons += household.getPersons().size();
		System.out.println("Travellers: " + numPersons);
		numPersons = 0;
		for(Household household:households.values())
			numPersons += household.getPersonsNoTraveling().size();
		System.out.println("No travelling: " + numPersons);
		numPersons = 0;
		for(Household household:households.values())
			numPersons += household.getPersonsNoComplete().size();
		System.out.println("Incomplete: " + numPersons);
		numPersons = 0;
		for(Household household:households.values())
			numPersons += household.getPersonsNoSurvey().size();
		System.out.println("No survey: " + numPersons);
		int[] numDaysPerson = new int[Day.values().length];
		double[] numDaysPersonW = new double[Day.values().length];
		int[] numDaysTrip = new int[Day.values().length];
		double[] numDaysTripW = new double[Day.values().length];
		int[] numDaysStage = new int[Day.values().length];
		for(Household household:households.values())
			for(Person person:household.getPersons().values()) {
				int index = person.getSurveyDay().ordinal();
				numDaysPerson[index]++;
				numDaysPersonW[index]+=person.getFactor();
				numDaysTrip[index]+=person.getTrips().size();
				for(Trip trip:person.getTrips().values()) {
					numDaysTripW[index]+=trip.getFactor();
					numDaysStage[index]+=trip.getStages().size();
				}
			}
		for(Day day:Day.values()) {
			System.out.println(day.name()+": "+numDaysPerson[day.ordinal()]+" people, "+numDaysTrip[day.ordinal()]+" trips, "+numDaysStage[day.ordinal()]+" stages.");
			System.out.println(day.name()+": "+numDaysPersonW[day.ordinal()]+" people, "+numDaysTripW[day.ordinal()]+" trips.");
		}
	}
	
	/*public static void main(String[] args) throws IOException, ParseException {
		Map<String, Household> households = readHits(args[0]);
		PrintWriter printWriter = new PrintWriter("./data/persons.txt");
		for(Household household:households.values())
			for(Person person:household.getPersons().values())
				printWriter.println(person.getId()+","+household.getLocation().getCoord().getX()+","+household.getLocation().getCoord().getY()+","+person.getAgeInterval().getCenter()+","+(person.getIncomeInterval()==null?0:person.getIncomeInterval().getCenter())+","+person.getTrips().size());
		printWriter.close();
	}*/
	
	private static void init() {
		placeTypesMap.put("Place of worship", Trip.PlaceType.TEMPLE.text);
		placeTypesMap.put("Government related buildings", Trip.PlaceType.GOVERMENT.text);
		placeTypesMap.put("Petrol station", Trip.PlaceType.PETROL.text);
		placeTypesMap.put("Work related trips", Trip.PlaceType.WORK_RELATED.text);
		placeTypesMap.put("Market/food centre/restaurant", Trip.PlaceType.EAT.text);
		placeTypesMap.put("Beach area/ Water body", Trip.PlaceType.WATER.text);
		placeTypesMap.put("Civic & community institution", Trip.PlaceType.CIVIC.text);
		placeTypesMap.put("Entertainment", Trip.PlaceType.FUN.text);
		placeTypesMap.put("Shop/shopping centre", Trip.PlaceType.SHOP.text);
		placeTypesMap.put("Financial Institution", Trip.PlaceType.FINANTIAL.text);
		placeTypesMap.put("School", Trip.PlaceType.SCHOOL.text);
		placeTypesMap.put("Hosptial/polyclinic/specialist centre/nursing home", Trip.PlaceType.MEDICAL.text);
		placeTypesMap.put("Park/open space", Trip.PlaceType.PARK.text);
		placeTypesMap.put("Residential (other's home)", Trip.PlaceType.HOME_OTHER.text);
		placeTypesMap.put("Education Ctr (Student care/ Nursery/ Childcare/ Library)", Trip.PlaceType.EDU.text);
		placeTypesMap.put("Sports/recreation", Trip.PlaceType.REC.text);
		placeTypesMap.put("Sentosa/ Pulau Bukum/ Johore", Trip.PlaceType.ISLAND.text);
		placeTypesMap.put("Residential (your home)", Trip.PlaceType.HOME.text);
		placeTypesMap.put("Hotel/ Hostel", Trip.PlaceType.HOTEL.text);
		placeTypesMap.put("Land Checkpoint", Trip.PlaceType.CHECK.text);
		placeTypesMap.put("Industrial/factory/workshop premise", Trip.PlaceType.INDUSTRIAL.text);
		placeTypesMap.put("Transport facilities", Trip.PlaceType.TRANSPORT.text);
		placeTypesMap.put("Port", Trip.PlaceType.PORT.text);
		placeTypesMap.put("Airport", Trip.PlaceType.AIRPORT.text);
		placeTypesMap.put("SAF Camps/ Police Complex/ Civil Defence/ Fire stn", Trip.PlaceType.MILITARY.text);
		placeTypesMap.put("Office", Trip.PlaceType.OFFICE.text);
		purposesMap.put("Medical visit (self)", Trip.Purpose.MEDICAL.text);
		purposesMap.put("Work Related Trip", Trip.Purpose.WORK_FLEX.text);
		purposesMap.put("Shopping", Trip.Purpose.SHOP.text);
		purposesMap.put("Pick-up  Drop Off", Trip.Purpose.P_U_D_O.text);
		purposesMap.put("Entertainment- Social", Trip.Purpose.SOCIAL.text);
		purposesMap.put("Household activities (household chores personal care rest)", Trip.Purpose.HOME.text);
		purposesMap.put("NS", Trip.Purpose.NS.text);
		purposesMap.put("Working for paid employment", Trip.Purpose.WORK.text);
		purposesMap.put("Work-related (meetings sales etc)", Trip.Purpose.WORK_FLEX.text);
		purposesMap.put("Dining refreshment", Trip.Purpose.EAT.text);
		purposesMap.put("Education", Trip.Purpose.EDU.text);
		purposesMap.put("Accompanying someone", Trip.Purpose.ACCOMP.text);
		purposesMap.put("Recreation", Trip.Purpose.REC.text);
		purposesMap.put("Other Personal Business", Trip.Purpose.ERRANDS.text);
		purposesMap.put("Professional Driver", Trip.Purpose.DRIVE.text);
		purposesMap.put("Religious related matters", Trip.Purpose.RELIGION.text);
		try {
			BufferedReader reader = new BufferedReader(new FileReader("./data/locs.csv"));
            Location.SINGAPORE_COORDS_MAP = new HashMap<>();
            String line = reader.readLine();
            while(line != null){
                String[] split = line.split(",");
                Location.SINGAPORE_COORDS_MAP.put(split[0],new Coord(Double.parseDouble(split[1]),Double.parseDouble(split[2])));
                line = reader.readLine();
            }
            reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static Map<String, Household> readHits(String fileName) throws IOException, NumberFormatException, ParseException {
		init();
		Map<String, Household> households = new HashMap<String, Household>();
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		reader.readLine();
		String line = reader.readLine();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm");
		while(line!=null) {
			String[] lineParts = line.split(",");
			Household household = households.get(lineParts[1]);
			if(household==null) {
				String areaCode = lineParts[Location.Column.AREA_CODE.column];
				String area = Location.AREAS.get(areaCode);
				if(area==null) {
					area = lineParts[Location.Column.AREA_NAME.column];
					Location.AREAS.put(areaCode, area);
				}
				String postalCode = lineParts[Location.Column.POSTAL_CODE.column];
				Location location = Household.LOCATIONS.get(postalCode);
				if(location==null) {
					location = new Location(postalCode, areaCode);
					Household.LOCATIONS.put(postalCode, location);
				}
				location.addType(Trip.PlaceType.HOME.text);
				location.addPurpose(Trip.Purpose.HOME.text);
				String numBikes = lineParts[Household.Column.NUM_BIKES.column], numVehicles = lineParts[Household.Column.NUM_VEHICLES.column],
						factor = lineParts[Household.Column.FACTOR.column];
				household = new Household(lineParts[1], location, lineParts[Household.Column.DWELLING_TYPE.column],
						lineParts[Household.Column.DWELLING_8_TYPE.column], lineParts[Household.Column.ETHNIC.column],
						lineParts[Household.Column.ETHNIC_OTHERS.column], numBikes.isEmpty()?0:new Integer(numBikes),
						numVehicles.isEmpty()?0:new Integer(numVehicles), new Double(factor));
				households.put(lineParts[1], household);
			}
			Person person = household.getPerson(lineParts[2]);
			if(person==null) {
				String factor = lineParts[Person.Column.FACTOR.column];
				String reasonNoEligible = lineParts[Person.Column.NO_ELIGIBLE_REASON.column];
				if(lineParts[Person.Column.NO_ELIGIBLE_UNDER_4.column].contains("Yes "))
					reasonNoEligible = "Under 4";
				else if(lineParts[Person.Column.INCOMPLETE.column].contains("Yes"))
					reasonNoEligible = "Incomplete";
				if(!reasonNoEligible.isEmpty())
					person = new Person(lineParts[2], reasonNoEligible, new Double(factor));
				else {
					String schoolPostalCode = lineParts[Person.Column.SCHOOL_POSTAL_CODE.column];
					String school = "";
					if(!schoolPostalCode.trim().isEmpty()) {
						school = Person.SCHOOLS.get(schoolPostalCode);
						if(school==null) {
							school = lineParts[Person.Column.SCHOOL_NAME.column];
							Person.SCHOOLS.put(schoolPostalCode, school);
						}
					}
					String income = lineParts[Person.Column.INCOME.column];
					String sDay = lineParts[Person.Column.SURVEY_DAY.column];
					Day surveyDay = null;
					for(Day day:Day.values())
						if(day.name().equals(sDay)) {
							surveyDay = day;
							break;
						}
					person = new Person(lineParts[2], new Person.AgeInterval(lineParts[Person.Column.AGE.column]),
							lineParts[Person.Column.TYPE_NATIONALITY.column], lineParts[Person.Column.GENDER.column],
							lineParts[Person.Column.HAS_CAR.column].contains("Yes")?true:false,
							lineParts[Person.Column.HAS_BIKE.column].contains("Yes")?true:false,
							lineParts[Person.Column.HAS_VAN_BUS.column].contains("Yes")?true:false,
							lineParts[Person.Column.HAS_LICENSE.column].contains("No,")?true:false,
							lineParts[Person.Column.HAS_MOBILITY.column].contains("Yes")?true:false,
							lineParts[Person.Column.AIDS.column], lineParts[Person.Column.AIDS_OTHER.column],
							lineParts[Person.Column.EMPLOYMENT.column], lineParts[Person.Column.EDUCATION.column],
							school, lineParts[Person.Column.OCCUPATION.column], lineParts[Person.Column.INDUSTRY.column],
							lineParts[Person.Column.FIXED_WORK_PLACE.column], lineParts[Person.Column.WORK_HOURS.column],
							income.contains("Refused")?null:income.contains("No Income")?new IncomeInterval():new IncomeInterval(income),
							dateFormat.parse(lineParts[Person.Column.DATE.column]), surveyDay,
							lineParts[Person.Column.START_HOME.column].contains("Home")?true:false,
							purposesMap.get(lineParts[Person.Column.FIRST_ACTIVITY.column].replaceAll("^\"|\"$", "")), lineParts[Person.Column.NO_TRIP_REASON.column],
							lineParts[Person.Column.NO_TRIP_REASON_OTHER.column], lineParts[Person.Column.LAST_TIME_TRIP.column],
							new Double(factor));
				}
				household.addPerson(person);
			}
			if((person.getNoEligibleReason()==null || person.getNoEligibleReason().isEmpty())&&(person.getNoTripReason()==null || person.getNoTripReason().isEmpty())) {
				Trip trip = person.getTrip(lineParts[3]);
				if(trip==null) {
					String factor = lineParts[Trip.Column.FACTOR.column];
					String placeType = placeTypesMap.get(lineParts[Trip.Column.PLACE_TYPE.column]);
					Trip.PLACE_TYPES.add(placeType);
					String purpose = purposesMap.get(lineParts[Trip.Column.PURPOSE.column].replaceAll("^\"|\"$", ""));
					Trip.PURPOSES.add(purpose);
					String mode = lineParts[Trip.Column.MODE.column];
					Trip.MODES.add(mode);
					String startPostalCode = lineParts[Trip.Column.START_POSTAL_CODE.column];
					Location location = Household.LOCATIONS.get(startPostalCode);
					if(location==null) {
						location = new Location(startPostalCode);
						Household.LOCATIONS.put(startPostalCode, location);
					}
					String endPostalCode = lineParts[Trip.Column.END_POSTAL_CODE.column];
					location = Household.LOCATIONS.get(endPostalCode);
					if(location==null) {
						location = new Location(endPostalCode);
						Household.LOCATIONS.put(endPostalCode, location);
					}
					location.addType(placeType);
					if(!purpose.equals(Purpose.ACCOMP.text))
						location.addPurpose(purpose);
					trip = new Trip(lineParts[3], startPostalCode, endPostalCode,
							timeFormat.parse(lineParts[Trip.Column.START_TIME.column]),
							timeFormat.parse(lineParts[Trip.Column.END_TIME.column]),
							placeType, purpose, mode, new Double(factor));
					person.addTrip(trip);
				}
				if(trip.getMode().contains("Using motorised transport or cycle")) {
					Stage stage = null;
					String mode = lineParts[Stage.Column.MODE.column];
					Stage.Mode modeObject = Stage.Mode.getMode(mode);
					if(modeObject==Stage.Mode.CAR_DRIVER || modeObject==Stage.Mode.BIKE_RIDER || modeObject==Stage.Mode.VAN_DRIVER || modeObject==Stage.Mode.OTHER) {
						String parkType = lineParts[Stage.Column.PARK_TYPE.column];
						MotorDriverStage.PARK_TYPES.add(parkType);
						double erpCost = lineParts[Stage.Column.ERP_COST.column].equals("Don't Know")?-1:new Double(lineParts[Stage.Column.ERP_COST.column]);
						double parkCost = lineParts[Stage.Column.PARK_COST.column].equals("Don't Know")?-1:new Double(lineParts[Stage.Column.PARK_COST.column]);
						if(modeObject==Stage.Mode.OTHER) {
							mode = lineParts[Stage.Column.MODE_OTHERS.column];
							MotorDriverStage.OTHER_MODES.add(mode);
						}
						stage = new MotorDriverStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]),
								new Integer(lineParts[Stage.Column.NUM_PASSENGERS.column]), erpCost,
								lineParts[Stage.Column.ERP_REIMBURSMENT.column].equals("Yes"), parkCost,
								parkType, lineParts[Stage.Column.PARK_REIMBURSMENT.column].equals("Yes"));
					}
					else if(modeObject==Stage.Mode.CAR_PASSENGER || modeObject==Stage.Mode.BIKE_PASSENGER || modeObject==Stage.Mode.VAN_PASSENGER) {
						int numPassengers = modeObject==Stage.Mode.BIKE_PASSENGER?2:new Integer(lineParts[Stage.Column.NUM_PASSENGERS.column]);
						stage = new MotorStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]), numPassengers);
					}
					else if(modeObject==Stage.Mode.MRT) {
						String start = lineParts[Stage.Column.START.column];
						String end = lineParts[Stage.Column.END.column];
						MRTStage.STATIONS.add(start);
						MRTStage.STATIONS.add(end);
						stage = new MRTStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]),
								new Double(lineParts[Stage.Column.WAIT_TIME.column]), start, end,
								lineParts[Stage.Column.FIRST_TRANSFER.column],
								lineParts[Stage.Column.SECOND_TRANSFER.column], lineParts[Stage.Column.THIRD_TRANSFER.column]);
					}
					else if(modeObject==Stage.Mode.LRT) {
						String start = lineParts[Stage.Column.START.column];
						String end = lineParts[Stage.Column.END.column];
						StationStage.STATIONS.add(start);
						StationStage.STATIONS.add(end);
						stage = new StationStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]),
								new Double(lineParts[Stage.Column.WAIT_TIME.column]), start, end);
					}
					else if(modeObject==Stage.Mode.PUBLIC_BUS) {
						String lineBus = lineParts[Stage.Column.START.column];
						PublicBusStage.LINES.add(lineBus);
						stage = new PublicBusStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]),
								new Double(lineParts[Stage.Column.WAIT_TIME.column]), lineBus);
					}
					else if(modeObject==Stage.Mode.COMPANY_BUS || modeObject==Stage.Mode.SCHOOL_BUS || modeObject==Stage.Mode.SHUTTLE_BUS) {
						stage = new OtherBusStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]),
								new Double(lineParts[Stage.Column.WAIT_TIME.column]), modeObject.toString());
					}
					else if(modeObject==Stage.Mode.TAXI) {
						double taxiFare = lineParts[Stage.Column.TAXI_FARE.column].equals("Don't Know")?-1:new Double(lineParts[Stage.Column.TAXI_FARE.column]);
						stage = new TaxiStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]),
								new Double(lineParts[Stage.Column.WAIT_TIME.column]),
								new Integer(lineParts[Stage.Column.NUM_PASSENGERS.column]),
								taxiFare, lineParts[Stage.Column.TAXI_REIMBURSMENT.column].equals("Yes"));
					}
					else if(modeObject==Stage.Mode.CYCLE) {
						stage = new CycleStage(lineParts[4], mode, new Double(lineParts[Stage.Column.WALK_TIME.column]),
								new Double(lineParts[Stage.Column.IN_VEHICLE_TIME.column]),
								new Double(lineParts[Stage.Column.LAST_WALK_TIME.column]),
								lineParts[Stage.Column.CYCLE.column]);
					}
					if(stage!=null)
						trip.addStage(stage);
				}
			}
			line = reader.readLine();
		}
		reader.close();
		for(Household household:households.values())
			household.orderPeople();
		return households;
	}

}
