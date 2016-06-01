package playground.dziemke.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;

/**
 * @author dziemke
 */
public class TripAnalyzerBasic {
	public static final Logger log = Logger.getLogger(TripAnalyzerBasic.class);
	
	/* Parameters */
	private static final String RUN_ID = "run_168a";
	private static final String USED_ITERATION = "300"; // most frequently used value: 150

	private static final int binWidthDuration_min = 1;
	private static final int binWidthTime_h = 1;
	private static final int binWidthDistance_km = 1;
	private static final int binWidthSpeed_km_h = 1;

	/* Input and output */
	private static String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	private static String eventsFile = "../../../runs-svn/cemdapMatsimCadyts/" + RUN_ID + "/ITERS/it." + USED_ITERATION +
			"/" + RUN_ID + "." + USED_ITERATION + ".events.xml.gz";
	private static String outputDirectory = "../../../runs-svn/cemdapMatsimCadyts/" + RUN_ID + "/test_analysis";

	/* Variables to store information */
	private static double aggregateWeightOfConsideredTrips = 0;
	private static int numberOfInIncompleteTrips = 0;
    
	private static Map<String, Double> otherInformationMap = new TreeMap<>();

	
	public static void main(String[] args) {

		if (args.length != 0 && args.length != 3) {
			throw new RuntimeException("Number of arguments is wrong.");
		} else if (args.length == 3) {
			networkFile = args[0];
			eventsFile = args[1];
			outputDirectory = args[2];
		}

		new File(outputDirectory).mkdir();
		
		double numberOfTripsWithCalculableSpeedBeeline = 0;
		double numberOfTripsWithCalculableSpeedRouted = 0;
	    
	    /* Events infrastructure and reading the events file */
	    EventsManager eventsManager = EventsUtils.createEventsManager();
	    TripHandler tripHandler = new TripHandler();
	    eventsManager.addHandler(tripHandler);
	    MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
	    eventsReader.readFile(eventsFile);
	    log.info("Events file read!");
	     
	    /* Get network, which is needed to calculate distances */
	    Network network = NetworkUtils.createNetwork();
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
	    networkReader.readFile(networkFile);    
	    
		AnalysisFileWriter writer = new AnalysisFileWriter();
		
		List<Trip> trips = createListOfValidTrip(tripHandler.getTrips());
	    
	    /* Do calculations and write-out*/
	    Map <Integer, Double> tripDurationMap = createTripDurationMap(trips, binWidthDuration_min);
	    double averageTripDuration = calculateAverageTripDuration_min(trips);
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, aggregateWeightOfConsideredTrips, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, aggregateWeightOfConsideredTrips, averageTripDuration);
	    
	    Map <Integer, Double> departureTimeMap = createDepartureTimeMap(trips, binWidthTime_h);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, aggregateWeightOfConsideredTrips, Double.NaN);
	    	    
	    Map<String, Double> activityTypeMap = createActivityTypeMap(trips);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", aggregateWeightOfConsideredTrips);
	    
	    Map<Integer, Double> tripDistanceBeelineMap = createTripDistanceBeelineMap(trips, binWidthDistance_km, network);
		double averageTripDistanceBeeline_km = calculateAverageTripDistanceBeeline_km(trips, network);
		writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceBeeline_km);
		writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceBeeline_km);
	    
	    Map<Integer, Double> tripDistanceRoutedMap = createTripDistanceRoutedMap(trips, binWidthDistance_km, network);
	    double averageTripDistanceRouted_km = calculateAverageTripDistanceRouted_km(trips, network);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceRouted_km);
	    
	    numberOfTripsWithCalculableSpeedBeeline = TripAnalyzerBasic.countTripsWithCalculableSpeedBeeline(trips, network);
	    Map<Integer, Double> averageTripSpeedBeelineMap = createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h, network);
		double averageOfAverageTripSpeedsBeeline_km_h = calculateAverageOfAverageTripSpeedsBeeline_km_h(trips, network);
		writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);
		writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);
		
		numberOfTripsWithCalculableSpeedRouted = TripAnalyzerBasic.countTripsWithCalculableSpeedRouted(trips, network);
		Map<Integer, Double> averageTripSpeedRoutedMap = createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h, network);
		double averageOfAverageTripSpeedsRouted_km_h = calculateAverageOfAverageTripSpeedsRouted_km_h(trips, network);
		writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedRouted, averageOfAverageTripSpeedsRouted_km_h);

		/* Other information */
	    otherInformationMap.put("Aggregated weight of trips that have no previous activity", (double) tripHandler.getNoPreviousEndOfActivityCounter());
	    otherInformationMap.put("Aggregated weight of trips that have no calculable speed beeline", aggregateWeightOfConsideredTrips - numberOfTripsWithCalculableSpeedBeeline);
	    otherInformationMap.put("Aggregated weight of trips that have no calculable speed routed", aggregateWeightOfConsideredTrips - numberOfTripsWithCalculableSpeedRouted);
	    otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", (double) numberOfInIncompleteTrips);
	    otherInformationMap.put("Aggregated weight of (complete) trips", aggregateWeightOfConsideredTrips);
	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    	    	    
	    log.info(numberOfInIncompleteTrips + " trips are incomplete.");
	}
	
	public static Map<Integer, Double> createTripDurationMap(List<Trip> trips, int binWidthDuration_min) {
    	Map<Integer, Double> tripDurationMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDurationByCalculation_s() >= 0)) {
    			double tripDuration_min = (trip.getDurationByCalculation_s() / 60.);
    			AnalysisUtils.addToMapIntegerKeyCeiling(tripDurationMap, tripDuration_min, binWidthDuration_min, trip.getWeight());
    		}
    	}
    	return tripDurationMap;
    }
	
	public static double calculateAverageTripDuration_min(List<Trip> trips) {
		double sumOfAllDurations_min = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDurationByCalculation_s() >= 0)) {
				sumOfAllDurations_min += (trip.getDurationByCalculation_s() / 60. * trip.getWeight());
				sumOfAllWeights += trip.getWeight();
			}
    	}
		return sumOfAllDurations_min / sumOfAllWeights;
	}
	
	public static double countTripsWithNonNegativeTimesAndDurations(List<Trip> trips) {
		double counter = 0;
		for (Trip trip : trips) {
			if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDurationByCalculation_s() >= 0)) {
//				counter++;
				counter += trip.getWeight();
			}
		}
		return counter;
	}
	
	public static Map<Integer, Double> createDepartureTimeMap(List<Trip> trips, int binWidthTime_h) {
    	Map<Integer, Double> departureTimeMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		if ((trip.getArrivalTime_s() >= 0) && (trip.getDepartureTime_s() >= 0) && (trip.getDurationByCalculation_s() >= 0)) {
        		double departureTime_h = (trip.getDepartureTime_s() / 3600.);
        		/* Note: Here, "floor" is used instead of "ceiling". A departure at 6:43 should go into the 6.a.m. bin. */
    			AnalysisUtils.addToMapIntegerKeyFloor(departureTimeMap, departureTime_h, binWidthTime_h, trip.getWeight());
    		}
    	}
    	return departureTimeMap;
    }
	
	public static Map<String, Double> createActivityTypeMap(List<Trip> trips) {
    	Map<String, Double> activityTypeMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		String activityType = trip.getActivityStartActType();
    		AnalysisUtils.addToMapStringKey(activityTypeMap, activityType, trip.getWeight());
    	}
    	return activityTypeMap;
    }
	
	public static Map<Integer, Double> createTripDistanceBeelineMap(List<Trip> trips, int binWidthDistance_km, Network network) {
		Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
		for (Trip trip : trips) {
			double tripDistanceBeeline_km = (trip.getDistanceBeelineByCalculation_m(network) / 1000.);
			AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, trip.getWeight());
		}
		return tripDistanceBeelineMap;
	}

	public static double calculateAverageTripDistanceBeeline_km(List<Trip> trips, Network network) {
		double sumOfAllDistancesBeeline_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			sumOfAllDistancesBeeline_km += (trip.getDistanceBeelineByCalculation_m(network) / 1000. * trip.getWeight());
			sumOfAllWeights += trip.getWeight();
		}
		return sumOfAllDistancesBeeline_km / sumOfAllWeights;
	}

	public static Map<Integer, Double> createTripDistanceRoutedMap(List<Trip> trips, int binWidthDistance_km, Network network) {
    	Map<Integer, Double> tripDistanceRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDistanceRouted_km = (trip.getDistanceRoutedByCalculation_m(network) / 1000.);
    		AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceRoutedMap, tripDistanceRouted_km, binWidthDistance_km, trip.getWeight());
    	}
    	return tripDistanceRoutedMap;
    }
	
	public static double calculateAverageTripDistanceRouted_km(List<Trip> trips, Network network) {
		double sumOfAllDistancesRouted_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
    		sumOfAllDistancesRouted_km += (trip.getDistanceRoutedByCalculation_m(network) / 1000. * trip.getWeight());
    		sumOfAllWeights += trip.getWeight();
    	}
		return sumOfAllDistancesRouted_km / sumOfAllWeights;
	}
	
	public static int countTripsWithCalculableSpeedBeeline(List<Trip> trips, Network network) {
		int counter = 0;
		for (Trip trip : trips) {
			if ((trip.getDurationByCalculation_s() > 0.) && (trip.getDistanceBeelineByCalculation_m(network)) >= 0.) {
	    		counter++;
			}
		}
		return counter;
	}
	
	public static Map<Integer, Double> createAverageTripSpeedBeelineMap(List<Trip> trips, int binWidthSpeed_km_h, Network network) {
		Map<Integer, Double> averageTripSpeedBeelineMap = new TreeMap<>();
		for (Trip trip : trips) {
			double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
			double tripDistanceBeeline_km = (trip.getDistanceBeelineByCalculation_m(network) / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
	    		AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedBeelineMap, (tripDistanceBeeline_km / tripDuration_h), binWidthSpeed_km_h, trip.getWeight());
			}
		}
		return averageTripSpeedBeelineMap;
	}

	public static double calculateAverageOfAverageTripSpeedsBeeline_km_h(List<Trip> trips, Network network) {
		double sumOfAllAverageSpeedsBeeline_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
			double tripDistanceBeeline_km = (trip.getDistanceBeelineByCalculation_m(network) / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceBeeline_km >= 0.)) {
				sumOfAllAverageSpeedsBeeline_km_h += (tripDistanceBeeline_km / tripDuration_h * trip.getWeight());
				sumOfAllWeights += trip.getWeight();
			}
		}
		return sumOfAllAverageSpeedsBeeline_km_h / sumOfAllWeights;
	}
	
	public static int countTripsWithCalculableSpeedRouted(List<Trip> trips, Network network) {
		int counter = 0;
		for (Trip trip : trips) {
			if ((trip.getDurationByCalculation_s() > 0.) && (trip.getDistanceRoutedByCalculation_m(network)) >= 0.) {
	    		counter++;
			}
		}
		return counter;
	}

	public static Map<Integer, Double> createAverageTripSpeedRoutedMap(List<Trip> trips, int binWidthSpeed_km_h, Network network) {
    	Map<Integer, Double> averageTripSpeedRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
    		double tripDistanceRouted_km = (trip.getDistanceRoutedByCalculation_m(network) / 1000.);
    		if ((tripDuration_h > 0.) && (tripDistanceRouted_km >= 0.)) {
	    		AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedRoutedMap, (tripDistanceRouted_km / tripDuration_h), binWidthSpeed_km_h, trip.getWeight());
    		}
    	}
    	return averageTripSpeedRoutedMap;
    }
	
	public static double calculateAverageOfAverageTripSpeedsRouted_km_h(List<Trip> trips, Network network) {
		double sumOfAllAverageSpeedsRouted_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = (trip.getDurationByCalculation_s() / 3600.);
			double tripDistanceRouted_km = (trip.getDistanceRoutedByCalculation_m(network) / 1000.);
			if ((tripDuration_h > 0.) && (tripDistanceRouted_km >= 0.)) {
				sumOfAllAverageSpeedsRouted_km_h += (tripDistanceRouted_km / tripDuration_h * trip.getWeight());
				sumOfAllWeights += trip.getWeight();
			}
    	}
		return sumOfAllAverageSpeedsRouted_km_h / sumOfAllWeights;
	}
	
	
	private static List<Trip> createListOfValidTrip(Map<Id<Trip>, Trip> tripMap) {
		List<Trip> trips = new LinkedList<>();
		for (Trip trip : tripMap.values()) {
			trips.add(trip);
			aggregateWeightOfConsideredTrips++;
		}
		return trips;
	}
}