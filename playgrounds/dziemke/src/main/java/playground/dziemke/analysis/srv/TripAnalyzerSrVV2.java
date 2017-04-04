package playground.dziemke.analysis.srv;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.dziemke.analysis.AnalysisFileWriter;
import playground.dziemke.analysis.AnalysisUtils;
import playground.dziemke.analysis.Trip;
import playground.dziemke.analysis.TripAnalyzerV2Basic;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author dziemke
 */
public class TripAnalyzerSrVV2 {
	public static final Logger log = Logger.getLogger(TripAnalyzerSrVV2.class);
	
	/* Parameters */
	private static final boolean useWeights = true;			//wt
	private static final boolean onlyCar = false;			//car
	private static final boolean onlyCarAndCarPool = true;	//carp
	private static final boolean onlyHomeAndWork = false;	//hw
	
	private static final boolean distanceFilter = true;		//dist
	private static final double minDistance_km = 0; // TODO switch back off
	private static final double maxDistance_km = 100;
	
	private static final boolean ageFilter = false;
	private static final Integer minAge = 80; // typically "x0"
	private static final Integer maxAge = 119; // typically "x9"; highest number ususally chosen is 119	

	private static final int binWidthDuration_min = 1;
	private static final int binWidthTime_h = 1;
	private static final int binWidthDistance_km = 1;
	private static final int binWidthSpeed_km_h = 1;

	/* Input and output */
	private static final String INPUT_TRIPS_FILE = "../../../shared-svn/projects/cemdapMatsimCadyts/analysis/srv/input/W2008_Berlin_Weekday.dat";
	private static final String INPUT_PERSONS_FILE = "../../../shared-svn/projects/cemdapMatsimCadyts/analysis/srv/input/P2008_Berlin2.dat";

	private static final String INPUT_NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
//	private static finalString shapeFile = "/Users/dominik/Workspace/data/srv/input/RBS_OD_STG_1412/RBS_OD_STG_1412.shp";

	private static final String OUTPUT_DIRECTORY = "../../../shared-svn/projects/cemdapMatsimCadyts/analysis/srv/output/wd_new";

	/* Variables to store information */
	private static double aggregatedWeightOfConsideredTrips = 0;
//	private static int numberOfInIncompleteTrips = 0;

	private static Map<String, Double> otherInformationMap = new TreeMap<>();

	
	public static void main(String[] args) {
		analyze(INPUT_TRIPS_FILE, INPUT_PERSONS_FILE, INPUT_NETWORK_FILE, OUTPUT_DIRECTORY);
	}

	public static void analyze(String inputTripsFile, String inputPersonsFile, String inputNetworkFile, String outputDirectory) {
		double aggregatedWeightOfTripsWithNonNegativeTimesAndDurations;
		double aggregatedWeightOfTripsWithNonNegativeDistanceBeelineSurvey;
		double aggregatedWeightOfTripsWithNonNegativeDistanceRoutedSurvey;
		double aggregatedWeightOfTripsWithCalculableSpeedBeelineSurvey;
		double aggregatedWeightOfTripsWithCalculableSpeedRoutedSurvey;

		//
		String fromCRS = "EPSG:31468"; // GK4
		String toCRS = "EPSG:31468"; // GK4
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);
		//

		outputDirectory = adaptOutputDirectory(outputDirectory);

		/* Get network, which is needed to calculate distances */
		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		networkReader.readFile(inputNetworkFile);

	    /* Parse trip file */
		log.info("Parsing " + inputTripsFile + ".");
		SrV2008TripParser tripParser = new SrV2008TripParser();
		tripParser.parse(inputTripsFile);
		log.info("Finished parsing trips.");

		/* Parse person file */
		log.info("Parsing " + inputPersonsFile + ".");
		SrV2008PersonParser personParser = new SrV2008PersonParser();
		personParser.parse(inputPersonsFile);
		log.info("Finished parsing persons.");

		AnalysisFileWriter writer = new AnalysisFileWriter();

		List<Trip> trips = createListOfValidTrip(tripParser.getTrips(), personParser);

	    /* Do calculations and write-out*/
		// reliant on variable "V_ANKUNFT": -9 = no data, -10 = implausible
		// and on variable "V_BEGINN": -9 = no data, -10 = implausible
		aggregatedWeightOfTripsWithNonNegativeTimesAndDurations = TripAnalyzerV2Basic.countTripsWithNonNegativeTimesAndDurations(trips);
		Map <Integer, Double> tripDurationMap = TripAnalyzerV2Basic.createTripDurationMap(trips, binWidthDuration_min);
		double averageTripDuration = TripAnalyzerV2Basic.calculateAverageTripDuration_min(trips);
		writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, aggregatedWeightOfTripsWithNonNegativeTimesAndDurations, averageTripDuration);
		writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, aggregatedWeightOfTripsWithNonNegativeTimesAndDurations, averageTripDuration);

		Map <Integer, Double> departureTimeMap = TripAnalyzerV2Basic.createDepartureTimeMap(trips, binWidthTime_h);
		writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, aggregatedWeightOfTripsWithNonNegativeTimesAndDurations, Double.NaN);

		// reliant on variable "V_ZWECK": -9 = no data
		// "V_ZWECK" - end of trip = start of activity
		Map<String, Double> activityTypeMap = TripAnalyzerV2Basic.createActivityTypeMap(trips);
		writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", aggregatedWeightOfConsideredTrips);

		// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
		aggregatedWeightOfTripsWithNonNegativeDistanceBeelineSurvey = countTripsWithNonnegativeDistanceBeelineInSurvey(trips);
		Map<Integer, Double> tripDistanceBeelineMap = createTripDistanceBeelineMap(trips, binWidthDistance_km); // SrV-specific, distances given by survey
		double averageTripDistanceBeeline_km = calculateAverageTripDistanceBeeline_km(trips); // SrV-specific, distances given by survey
		writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, aggregatedWeightOfTripsWithNonNegativeDistanceBeelineSurvey, averageTripDistanceBeeline_km);
		writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, aggregatedWeightOfTripsWithNonNegativeDistanceBeelineSurvey, averageTripDistanceBeeline_km);

		// In SrV, a routed distance (according to some software) is already given
		// reliant on SrV variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
		aggregatedWeightOfTripsWithNonNegativeDistanceRoutedSurvey = countTripsWithNonnegativeDistanceRoutedInSurvey(trips);
		Map<Integer, Double> tripDistanceRoutedMap = createTripDistanceRoutedMap(trips, binWidthDistance_km); // SrV-specific, distances given by survey
		double averageTripDistanceRouted_km = calculateAverageTripDistanceRouted_km(trips); // SrV-specific, distances given by survey
		writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, aggregatedWeightOfTripsWithNonNegativeDistanceRoutedSurvey, averageTripDistanceRouted_km);

		// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
		aggregatedWeightOfTripsWithCalculableSpeedBeelineSurvey = countTripsWithCalculableSpeedBeelineInSurvey(trips); // SrV-specific, distances given by survey
		Map<Integer, Double> averageTripSpeedBeelineMap = createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h); // SrV-specific, distances given by survey
		double averageOfAverageTripSpeedsBeeline_km_h = calculateAverageOfAverageTripSpeedsBeeline_km_h(trips); // SrV-specific, distances given by survey
		writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, aggregatedWeightOfTripsWithCalculableSpeedBeelineSurvey, averageOfAverageTripSpeedsBeeline_km_h);
		writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, aggregatedWeightOfTripsWithCalculableSpeedBeelineSurvey, averageOfAverageTripSpeedsBeeline_km_h);

		// reliant to SrV variable variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
		aggregatedWeightOfTripsWithCalculableSpeedRoutedSurvey = countTripsWithCalculableSpeedRoutedInSurvey(trips); // SrV-specific, distances given by survey
		Map<Integer, Double> averageTripSpeedRoutedMap = createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h); // SrV-specific, distances given by survey
		double averageOfAverageTripSpeedsRouted_km_h = calculateAverageOfAverageTripSpeedsRouted_km_h(trips); // SrV-specific, distances given by survey
		writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed_km_h, aggregatedWeightOfTripsWithCalculableSpeedRoutedSurvey, averageOfAverageTripSpeedsRouted_km_h);

		/* Other information */
//	    otherInformationMap.put("Number of trips that have no previous activity", tripHandler.getNoPreviousEndOfActivityCounter()); // TODO reactivate
		otherInformationMap.put("Aggregated weight of trips that have negative times or durations", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithNonNegativeTimesAndDurations);
		otherInformationMap.put("Aggregated weight of trips that have negative distance (beeline, from survey)", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithNonNegativeDistanceBeelineSurvey);
		otherInformationMap.put("Aggregated weight of trips that have negative distance (routed, from survey)", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithNonNegativeDistanceRoutedSurvey);
		otherInformationMap.put("Aggregated weight of trips that have no calculable speed (beeline)", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithCalculableSpeedBeelineSurvey);
		otherInformationMap.put("Aggregated weight of trips that have no calculable speed (routed)", aggregatedWeightOfConsideredTrips - aggregatedWeightOfTripsWithCalculableSpeedRoutedSurvey);
//		otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", (double) numberOfInIncompleteTrips);
		otherInformationMap.put("Aggregated weight of (complete) trips", aggregatedWeightOfConsideredTrips);
		writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");

//		log.info(numberOfInIncompleteTrips + " trips are incomplete.");

	    /* Create plans and events */
		TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap = createPersonTripsMap(trips);
		SrV2PlansAndEventsConverter.convert(personTripsMap, //network, 
				ct, outputDirectory + "/");
	}
	
	/* SrV-specific calculations using distances given by survey instead of own calculated distances */
	private static double countTripsWithNonnegativeDistanceBeelineInSurvey(List<Trip> trips) {
		double counter = 0.;
		for (Trip trip : trips) {
			if (trip.getDistanceBeelineFromSurvey_m() >= 0) {
				counter += trip.getWeight();
			}
		}
		return counter;
	}	
	
	private static Map<Integer, Double> createTripDistanceBeelineMap(List<Trip> trips, int binWidthDistance_km) {
		Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
		for (Trip trip : trips) {
			if (trip.getDistanceBeelineFromSurvey_m() >= 0) {
				double tripDistanceBeeline_km = (trip.getDistanceBeelineFromSurvey_m() / 1000.);
				AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, trip.getWeight());
			}
		}
		return tripDistanceBeelineMap;
	}

	private static double calculateAverageTripDistanceBeeline_km(List<Trip> trips) {
		double sumOfAllDistancesBeeline_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			if (trip.getDistanceBeelineFromSurvey_m() >= 0) {
				sumOfAllDistancesBeeline_km += (trip.getDistanceBeelineFromSurvey_m() / 1000. * trip.getWeight());
				sumOfAllWeights += trip.getWeight();
			}
		}
		return sumOfAllDistancesBeeline_km / sumOfAllWeights;
	}
	
	private static double countTripsWithNonnegativeDistanceRoutedInSurvey(List<Trip> trips) {
		double counter = 0.;
		for (Trip trip : trips) {
			if (trip.getDistanceRoutedShortestFromSurvey_m() >= 0) {
				counter += trip.getWeight();
			}
		}
		return counter;
	}
	
	private static Map<Integer, Double> createTripDistanceRoutedMap(List<Trip> trips, int binWidthDistance_km) {
    	Map<Integer, Double> tripDistanceRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		if (trip.getDistanceRoutedShortestFromSurvey_m() >= 0) {
    			double tripDistanceRouted_km = (trip.getDistanceRoutedShortestFromSurvey_m() / 1000.);
    			AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceRoutedMap, tripDistanceRouted_km, binWidthDistance_km, trip.getWeight());
    		}
    	}
    	return tripDistanceRoutedMap;
    }
	
	private static double calculateAverageTripDistanceRouted_km(List<Trip> trips) {
		double sumOfAllDistancesRouted_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			if (trip.getDistanceRoutedShortestFromSurvey_m() >= 0) {
				sumOfAllDistancesRouted_km += (trip.getDistanceRoutedShortestFromSurvey_m() / 1000. * trip.getWeight());
				sumOfAllWeights += trip.getWeight();
			}
		}
		return sumOfAllDistancesRouted_km / sumOfAllWeights;
	}
	
	private static double countTripsWithCalculableSpeedBeelineInSurvey(List<Trip> trips) {
		double counter = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
			double tripDistanceBeeline_km = (trip.getDistanceBeelineFromSurvey_m() / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
	    		counter += trip.getWeight();
			}
		}
		return counter;
	}

	private static Map<Integer, Double> createAverageTripSpeedBeelineMap(List<Trip> trips, int binWidthSpeed_km_h) {
		Map<Integer, Double> averageTripSpeedBeelineMap = new TreeMap<>();
		for (Trip trip : trips) {
			double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
			double tripDistanceBeeline_km = (trip.getDistanceBeelineFromSurvey_m() / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
	    		AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedBeelineMap, (tripDistanceBeeline_km / tripDuration_h), binWidthSpeed_km_h, trip.getWeight());
			}
		}
		return averageTripSpeedBeelineMap;
	}

	private static double calculateAverageOfAverageTripSpeedsBeeline_km_h(List<Trip> trips) {
		double sumOfAllAverageSpeedsBeeline_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
			double tripDistanceBeeline_km = (trip.getDistanceBeelineFromSurvey_m() / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
				sumOfAllAverageSpeedsBeeline_km_h += (tripDistanceBeeline_km / tripDuration_h * trip.getWeight());
				sumOfAllWeights += trip.getWeight();
			}
		}
		return sumOfAllAverageSpeedsBeeline_km_h / sumOfAllWeights;
	}

	private static double countTripsWithCalculableSpeedRoutedInSurvey(List<Trip> trips) {
		double counter = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
			double tripDistanceBeeline_km = (trip.getDistanceRoutedShortestFromSurvey_m() / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
	    		counter += trip.getWeight();
			}
		}
		return counter;
	}

	private static Map<Integer, Double> createAverageTripSpeedRoutedMap(List<Trip> trips, int binWidthSpeed_km_h) {
    	Map<Integer, Double> averageTripSpeedRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
    		double tripDistanceRouted_km = (trip.getDistanceRoutedShortestFromSurvey_m() / 1000.);
    		if ((tripDuration_h > 0.) && (tripDistanceRouted_km >= 0.)) {
	    		AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedRoutedMap, (tripDistanceRouted_km / tripDuration_h), binWidthSpeed_km_h, trip.getWeight());
    		}
    	}
    	return averageTripSpeedRoutedMap;
    }

	private static double calculateAverageOfAverageTripSpeedsRouted_km_h(List<Trip> trips) {
		double sumOfAllAverageSpeedsRouted_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
			double tripDistanceRouted_km = (trip.getDistanceRoutedShortestFromSurvey_m() / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceRouted_km >= 0.)) {
				sumOfAllAverageSpeedsRouted_km_h += (tripDistanceRouted_km / tripDuration_h * trip.getWeight());
				sumOfAllWeights += trip.getWeight();
			}
    	}
		return sumOfAllAverageSpeedsRouted_km_h / sumOfAllWeights;
	}

	private static TreeMap<Id<Person>, TreeMap<Double, Trip>> createPersonTripsMap(List<Trip> trips) {
		TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap = new TreeMap<>();
		
		for (Trip trip : trips) {
			String personId = trip.getPersonId().toString();
			Id<Person> idPerson = Id.create(personId, Person.class);
			
			if (!personTripsMap.containsKey(idPerson)) {
				personTripsMap.put(idPerson, new TreeMap<>());
			}
		
			double departureTime_s = trip.getDepartureTime_s();
			if (personTripsMap.get(idPerson).containsKey(departureTime_s)) {
				throw new RuntimeException("Person may not have two activites ending at the exact same time.");
			} else {
				personTripsMap.get(idPerson).put(departureTime_s, trip);
			}
		}
		return personTripsMap;
	}
	/* SrV-specific calculations -- End */
	
	@SuppressWarnings("all")
	private static String adaptOutputDirectory(String outputDirectory) {
		if (useWeights == true) {
			outputDirectory = outputDirectory + "_wt";
		}
		if (onlyCar == true) {
			outputDirectory = outputDirectory + "_car";
		}
		if (onlyCarAndCarPool == true) {
			outputDirectory = outputDirectory + "_carp";
		}
		if (onlyCar == false && onlyCarAndCarPool == false) {
			outputDirectory = outputDirectory + "_all";
		}
		if (distanceFilter == true) {
			outputDirectory = outputDirectory + "_dist";
		}
		if (onlyHomeAndWork == true) {
			outputDirectory = outputDirectory + "_hw";
		}
		if (ageFilter == true) {
			outputDirectory = outputDirectory + "_" + minAge.toString();
			outputDirectory = outputDirectory + "_" + maxAge.toString();
		}

		/* Create directory */
		new File(outputDirectory).mkdir();
		return outputDirectory;
	}
	
	@SuppressWarnings("all")
	private static List<Trip> createListOfValidTrip(Map<Id<Trip>, Trip> tripMap, SrV2008PersonParser personParser) {
		List<Trip> trips = new LinkedList<>();
		for (Trip trip : tripMap.values()) {
	    	// mode of transport and activity type
	    	// reliant on variable "V_HHPKW_F": 0/1
	    	int useHouseholdCar = trip.getUseHouseholdCar();
	    	// reliant on variable "V_ANDPKW_F": 0/1
	    	int useOtherCar = trip.getUseOtherCar();
	    	// reliant on variable "V_HHPKW_MF": 0/1
	    	int useHouseholdCarPool = trip.getUseHouseholdCarPool();
	    	// reliant on variable "V_ANDPKW_MF": 0/1
	    	int useOtherCarPool = trip.getUseOtherCarPool();

	    	String activityEndActType = trip.getActivityTypeBeforeTrip();
	    	String activityStartActType = trip.getActivityTypeAfterTrip();

	    	if (onlyHomeAndWork == true) {
	    		if (!(activityEndActType.equals("home") && activityStartActType.equals("work")) && !(activityEndActType.equals("work") && activityStartActType.equals("home"))) {
	    			continue;
	    		}
	    	}

	    	if (onlyCar == true) {
	    		if (!(useHouseholdCar == 1) && !(useOtherCar == 1)) {		 
	    			continue;
	    		}
	    	}
	    	if (onlyCarAndCarPool == true) {
	    		if (!(useHouseholdCar == 1) && !(useOtherCar == 1) && !(useHouseholdCarPool == 1) && !(useOtherCarPool == 1)) {		 
	    			continue;
	    		}
	    	} 
	
	    	double tripDistanceBeeline_km = trip.getDistanceBeelineFromSurvey_m() / 1000.;
	    	if (distanceFilter == true && tripDistanceBeeline_km >= maxDistance_km) {
	    		continue;
	    	}
	    	if (distanceFilter == true && tripDistanceBeeline_km <= minDistance_km) { // TODO switch back off
	    		continue;
	    	}

	    	// age
	    	String personId = trip.getPersonId().toString();
	    	
	    	if (ageFilter == true) {
	    		int age = (int) personParser.getPersonAttributes().getAttribute(personId, "age");
	    		if (age < minAge) {
	    			continue;
	    		}
	    		if (age > maxAge) {
	    			continue;
	    		}
	    	}
	    	
	    	// activity times and durations
	    	if ((trip.getArrivalTime_s() < 0) || (trip.getDepartureTime_s() < 0) //|| (trip.getDurationByCalculation_s() < 0)
	    			) {
	    		continue;
	    	}	    	

	    	// make weighting correct
	    	double weight;
    		if (useWeights == true) {
    			weight = trip.getWeight();
    		} else {
    			weight = 1.;
    		}
			trip.setWeight(weight);

			/* Only trips that fullfill all checked criteria are added; otherwise that loop would have been "continued" already */
			trips.add(trip);
			aggregatedWeightOfConsideredTrips += weight;
		}
		
		return trips;
	}
}