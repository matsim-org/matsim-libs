package playground.dziemke.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.dziemke.utils.ShapeReader;


/**
 * @author dziemke
 */
public class TripAnalyzer_V2 {
	public static final Logger log = Logger.getLogger(TripAnalyzer_V2.class) ;
	
	/* Parameters */
	private static final String runId = "run_168a";
	private static final String usedIteration = "300"; // most frequently used value: 150
	private static final String cemdapPersonsInputFileId = "21"; // check if this number corresponds correctly to the runId
	
	private static final Integer planningAreaId = 11000000; // 11000000 = Berlin

	private static final boolean onlyCar = false; // "car"; new, should be used for runs with ChangeLegMode enabled
	private static final boolean onlyInterior = false; // "int"
	private static final boolean onlyBerlinBased = true; // "ber"; usually varied for analysis
	private static final boolean distanceFilter = true; // "dist"; usually varied for analysis
	// private static final double double minDistance = 0;
	private static final double maxDistance_km = 100; // most frequently used value: 150

	private static final boolean onlyWorkTrips = false; // "work"

	private static final boolean ageFilter = false; // "age"
	private static final Integer minAge = 80; // typically "x0"
	private static final Integer maxAge = 119; // typically "x9"; higehst number ususally chosen is 119

	private static final int maxBinDuration_min = 120;
	private static final int binWidthDuration_min = 1;

	private static final int maxBinTime_h = 23;
	private static final int binWidthTime_h = 1;

	private static final int maxBinDistance_km = 60;
	private static final int binWidthDistance_km = 1;

	private static final int maxBinSpeed_km_h = 60;
	private static final int binWidthSpeed_km_h = 1;


	/* Input and output */
	private static final String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	private static final String eventsFile = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + 
			"/" + runId + "." + usedIteration + ".events.xml.gz";
	private static final String cemdapPersonsInputFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + 
			cemdapPersonsInputFileId + "/persons1.dat";
	private static final String planningAreaShapeFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Berlin_DHDN_GK4.shp";
	private static String outputDirectory = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";

	
	/* Variables to store objects */
	private static Network network;
	private static Geometry planningAreaGeometry;
	private static ObjectAttributes cemdapPersonAttributes;

	
	/* Variables to store information */
	private static int numberOfConsideredTrips = 0;
	private static int numberOfInIncompleteTrips = 0;
	private static int numberOfTripsWithCalculabeSpeed = 0;
    private static int numberOfTripsWithNoCalculableSpeed = 0;
    
	private static Map<Id<Trip>, Double> distanceRoutedMap = new TreeMap<>();
	private static Map<Id<Trip>, Double> distanceBeelineMap = new TreeMap<>();
    
	private static Map<String, Integer> otherInformationMap = new TreeMap<>();

	
	public static void main(String[] args) {
		adaptOutputDirectory();
	    
		/* Events infrastructure and reading the events file */
	    EventsManager eventsManager = EventsUtils.createEventsManager();
	    TripHandler tripHandler = new TripHandler();
	    eventsManager.addHandler(tripHandler);
	    MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
	    eventsReader.readFile(eventsFile);
	    log.info("Events file read!");
	    
	    	       
	    /* Get network, which is needed to calculate distances */
	    network = NetworkUtils.createNetwork();
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
	    networkReader.readFile(networkFile);
	    
	    Map<Integer, Geometry> zoneGeometries = ShapeReader.read(planningAreaShapeFile, "NR");
		planningAreaGeometry = zoneGeometries.get(planningAreaId);	    
	    
		AnalysisFileWriter writer = new AnalysisFileWriter();

		if (ageFilter == true) {
	    	// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
	    	CemdapPersonInputFileReader cemdapPersonInputFileReader = new CemdapPersonInputFileReader();
		 	cemdapPersonInputFileReader.parse(cemdapPersonsInputFile);
		 	cemdapPersonAttributes = cemdapPersonInputFileReader.getPersonAttributes();
	    }
	    
	    List<Trip> trips = createListOfValidTrip(tripHandler.getTrips());
	    
	    /* Do calculations and write-out*/
	    Map <Integer, Double> tripDurationMap = createTripDurationMap(trips, binWidthDuration_min, maxBinDuration_min);
	    double averageTripDuration = calculateAverageTripDuration_min(trips);
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, numberOfConsideredTrips, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, numberOfConsideredTrips, averageTripDuration);
	    
	    Map <Integer, Double> departureTimeMap = createDepartureTimeMap(trips, binWidthTime_h, maxBinTime_h);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, numberOfConsideredTrips, averageTripDuration);
	    	    
	    Map<String, Double> activityTypeMap = createActivityTypeMap(trips, binWidthTime_h, maxBinTime_h);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", numberOfConsideredTrips);
	    
	    Map<Integer, Double> tripDistanceRoutedMap = createTripDistanceRoutedMap(trips, binWidthDistance_km, maxBinDistance_km);
	    double averageTripDistanceRouted_km = calculateAverageTripDistanceRouted_km(trips);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, numberOfConsideredTrips, averageTripDistanceRouted_km);
	    
	    Map<Integer, Double> tripDistanceBeelineMap = createTripDistanceBeelineMap(trips, binWidthDistance_km, maxBinDistance_km);
		double averageTripDistanceBeeline_km = calculateAverageTripDistanceBeeline_km(trips);
		writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, numberOfConsideredTrips, averageTripDistanceBeeline_km);
		writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, numberOfConsideredTrips, averageTripDistanceBeeline_km);
		
		Map<Integer, Double> averageTripSpeedRoutedMap = createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h, maxBinSpeed_km_h);
		double averageOfAverageTripSpeedsRouted_km_h = calculateAverageOfAverageTripSpeedsRouted_km_h(trips);
		writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed_km_h, numberOfTripsWithCalculabeSpeed, averageOfAverageTripSpeedsRouted_km_h);
		
		Map<Integer, Double> averageTripSpeedBeelineMap = createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h, maxBinSpeed_km_h);
		double averageOfAverageTripSpeedsBeeline_km_h = calculateAverageOfAverageTripSpeedsBeeline_km_h(trips);
		writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, numberOfTripsWithCalculabeSpeed, averageOfAverageTripSpeedsBeeline_km_h);
		writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, numberOfTripsWithCalculabeSpeed, averageOfAverageTripSpeedsBeeline_km_h);

		/* Other information */
	    otherInformationMap.put("Number of trips that have no previous activity", tripHandler.getNoPreviousEndOfActivityCounter());
	    otherInformationMap.put("Number of trips that have no calculable speed", numberOfTripsWithNoCalculableSpeed);
	    otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", numberOfInIncompleteTrips);
	    otherInformationMap.put("Number of (complete) trips", numberOfConsideredTrips);
	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", numberOfConsideredTrips);
	    	    
	    log.info(numberOfInIncompleteTrips + " trips are incomplete.");
	}
	
	private static Map<Integer, Double> createTripDurationMap(List<Trip> trips, int binWidthDuration_min, int maxBinDuration_min) {
    	Map<Integer, Double> tripDurationMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDuration_min = trip.getDurationByCalculation_s() / 60.;
    		double tripWeight = trip.getWeight();
    		AnalysisUtils.addToMapIntegerKey(tripDurationMap, tripDuration_min, binWidthDuration_min, maxBinDuration_min, tripWeight);
    	}
    	return tripDurationMap;
    }
	
	private static double calculateAverageTripDuration_min(List<Trip> trips) {
		double sumOfAllDurations_min = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
    		sumOfAllDurations_min += trip.getDurationByCalculation_s() / 60.;
    		sumOfAllWeights += trip.getWeight();
    	}
		return sumOfAllDurations_min / sumOfAllWeights;
	}
	
	private static Map<Integer, Double> createDepartureTimeMap(List<Trip> trips, int binWidthTime_h, int maxBinTime_h) {
    	Map<Integer, Double> departureTimeMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double departureTime_h = trip.getDepartureTime_s() / 3600.;
    		double tripWeight = trip.getWeight();
    		AnalysisUtils.addToMapIntegerKey(departureTimeMap, departureTime_h, binWidthTime_h, maxBinTime_h, tripWeight);
    	}
    	return departureTimeMap;
    }
	
	private static Map<String, Double> createActivityTypeMap(List<Trip> trips, int binWidthTime_h, int maxBinTime_h) {
    	Map<String, Double> activityTypeMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		String activityType = trip.getActivityStartActType();
    		double tripWeight = trip.getWeight();
    		AnalysisUtils.addToMapStringKey(activityTypeMap, activityType, tripWeight);
    	}
    	return activityTypeMap;
    }
	
	private static Map<Integer, Double> createTripDistanceRoutedMap(List<Trip> trips, int binWidthDistance_km, int maxBinDistance_km) {
    	Map<Integer, Double> tripDistanceRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDistanceRouted_km = trip.getDistanceRoutedByCalculation_m(network) / 1000.;
    		double tripWeight = trip.getWeight();
    		AnalysisUtils.addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted_km, binWidthDistance_km, maxBinDistance_km, tripWeight);
    		
    		distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted_km); // TODO eventually remove this
    	}
    	return tripDistanceRoutedMap;
    }
	
	private static double calculateAverageTripDistanceRouted_km(List<Trip> trips) {
		double sumOfAllDistancesRouted_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
    		sumOfAllDistancesRouted_km += (trip.getDistanceRoutedByCalculation_m(network) / 1000.);
    		sumOfAllWeights += trip.getWeight();
    	}
		return sumOfAllDistancesRouted_km / sumOfAllWeights;
	}
	
	private static Map<Integer, Double> createTripDistanceBeelineMap(List<Trip> trips, int binWidthDistance_km, int maxBinDistance_km) {
    	Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDistanceBeeline_km = trip.getDistanceBeelineByCalculation_m(network) / 1000.;
    		double tripWeight = trip.getWeight();
    		AnalysisUtils.addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, maxBinDistance_km, tripWeight);
    		
    		distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline_km); // TODO eventually remove this
    	}
    	return tripDistanceBeelineMap;
    }
	
	private static double calculateAverageTripDistanceBeeline_km(List<Trip> trips) {
		double sumOfAllDistancesBeeline_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
    		sumOfAllDistancesBeeline_km += (trip.getDistanceBeelineByCalculation_m(network) / 1000.);
    		sumOfAllWeights += trip.getWeight();
    	}
		return sumOfAllDistancesBeeline_km / sumOfAllWeights;
	}
	
	private static Map<Integer, Double> createAverageTripSpeedRoutedMap(List<Trip> trips, int binWidthSpeed_km_h, int maxBinSpeed_km_h) {
    	Map<Integer, Double> averageTripSpeedRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
    		if (tripDuration_h > 0.) {
	    		double tripDistanceRouted_km = trip.getDistanceRoutedByCalculation_m(network) / 1000.;
	    		double tripWeight = trip.getWeight();
	    		AnalysisUtils.addToMapIntegerKey(averageTripSpeedRoutedMap, (tripDistanceRouted_km / tripDuration_h), binWidthSpeed_km_h, maxBinSpeed_km_h, tripWeight);
    		}
    	}
    	return averageTripSpeedRoutedMap;
    }
	
	private static double calculateAverageOfAverageTripSpeedsRouted_km_h(List<Trip> trips) {
		double sumOfAllAverageSpeedsRouted_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
			if (tripDuration_h > 0.) {
				double tripDistanceRouted_km = (trip.getDistanceRoutedByCalculation_m(network) / 1000.);
				sumOfAllAverageSpeedsRouted_km_h += (tripDistanceRouted_km / tripDuration_h);
				sumOfAllWeights += trip.getWeight();
			}
    	}
		return sumOfAllAverageSpeedsRouted_km_h / sumOfAllWeights;
	}
	
	private static Map<Integer, Double> createAverageTripSpeedBeelineMap(List<Trip> trips, int binWidthSpeed_km_h, int maxBinSpeed_km_h) {
    	Map<Integer, Double> averageTripSpeedBeelineMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
    		if (tripDuration_h > 0.) {
	    		double tripDistanceBeeline_km = trip.getDistanceBeelineByCalculation_m(network) / 1000.;
	    		double tripWeight = trip.getWeight();
	    		AnalysisUtils.addToMapIntegerKey(averageTripSpeedBeelineMap, (tripDistanceBeeline_km / tripDuration_h), binWidthSpeed_km_h, maxBinSpeed_km_h, tripWeight);
	    		numberOfTripsWithCalculabeSpeed++;
    		} else {
    			numberOfTripsWithNoCalculableSpeed++;
    		}
    	}
    	return averageTripSpeedBeelineMap;
    }
	
	private static double calculateAverageOfAverageTripSpeedsBeeline_km_h(List<Trip> trips) {
		double sumOfAllAverageSpeedsBeeline_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
			if (tripDuration_h > 0.) {
				double tripDistanceBeeline_km = (trip.getDistanceBeelineByCalculation_m(network) / 1000.);
				sumOfAllAverageSpeedsBeeline_km_h += (tripDistanceBeeline_km / tripDuration_h);
				sumOfAllWeights += trip.getWeight();
			}
    	}
		return sumOfAllAverageSpeedsBeeline_km_h / sumOfAllWeights;
	}
	
	
	@SuppressWarnings("all")
	private static void adaptOutputDirectory() {
		outputDirectory = outputDirectory + "_" + usedIteration;
	    if (onlyCar == true) {
			outputDirectory = outputDirectory + "_car";
		}
	    if (onlyInterior == true) {
			outputDirectory = outputDirectory + "_int";
	    }
		if (onlyBerlinBased == true) {
			outputDirectory = outputDirectory + "_ber";
		}
		if (distanceFilter == true) {
			outputDirectory = outputDirectory + "_dist";
		}
		if (onlyWorkTrips == true) {
			outputDirectory = outputDirectory + "_work";
		}
		if (ageFilter == true) {
			outputDirectory = outputDirectory + "_age_" + minAge.toString();
			outputDirectory = outputDirectory + "_" + maxAge.toString();
		}
		outputDirectory = outputDirectory + "_3"; // TODO in case used for double-check
		
		/* Create directory */
		new File(outputDirectory).mkdir();
	}
	
	
	@SuppressWarnings("unused")
	private static List<Trip> createListOfValidTrip(Map<Id<Trip>, Trip> tripMap) {
		List<Trip> trips = new LinkedList<>();
		
		for (Trip trip : tripMap.values()) {

			// get coordinates of links
			Id<Link> departureLinkId = trip.getDepartureLinkId();
			Id<Link> arrivalLinkId = trip.getArrivalLinkId();
			//
			Link departureLink = network.getLinks().get(departureLinkId);
			Link arrivalLink = network.getLinks().get(arrivalLinkId);

			// TODO use coords of toNode instead of center coord of link
			double arrivalCoordX = arrivalLink.getCoord().getX();
			double arrivalCoordY = arrivalLink.getCoord().getY();
			double departureCoordX = departureLink.getCoord().getX();
			double departureCoordY = departureLink.getCoord().getY();

			// create points
			Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
			Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);

			// choose if trip will be considered
			if (onlyBerlinBased == true) {
				if (!planningAreaGeometry.contains(arrivalLocation) && !planningAreaGeometry.contains(departureLocation)) {
					continue;
				}
			}
			if (onlyInterior == true) {
				if (!planningAreaGeometry.contains(arrivalLocation) || !planningAreaGeometry.contains(departureLocation)) {
					continue;
				}
			}
//			if (!trip.getMode().equals("car") && !trip.getMode().equals("pt")) {
//				throw new RuntimeException("In current implementation leg mode must either be car or pt");
//			}
			if (onlyCar == true) {
				if (!trip.getMode().equals("car")) {
					continue;
				}
			}
			if (distanceFilter == true && (trip.getDistanceBeelineByCalculation_m(network) / 1000.) >= maxDistance_km) {
				continue;
			}
//			if (distanceFilter == true && (trip.getBeelineDistance(network) / 1000.) <= minDistance) {
//    			continue;
//    		}
			if (onlyWorkTrips == true) {
				if (trip.getActivityEndActType().equals("work")) {
					continue;
				}
			}

			// TODO The plan was to calculate activity-chain frequencies here. Needs to be done somewhere else
			// write person activity attributes
//   	 	if (trip.getActivityEndActType().equals("work")) {
//   	 		personActivityAttributes.putAttribute(trip.getDriverId(), "hasWorkActivity", true);
//    		}

			/* Person-specific attributes */
			if (ageFilter == true) {
				// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
				// so far age is the only one
				String personId = trip.getPersonId().toString();
				int age = (int) cemdapPersonAttributes.getAttribute(personId, "age");

				if (age < minAge) {
					continue;
				}
				if (age > maxAge) {
					continue;
				}
			}

			/* The only case where incomplete trips occur is when agents are removed according to "removeStuckVehicles = true"
			 * Since a removed agent can at most have one incomplete trip (the one when he is removed), the number of
			 * incomplete trips should be equal to the number of removed agents */
			if(!trip.getTripComplete()) {
				System.err.println("Trip is not complete!");
				numberOfInIncompleteTrips++;
				continue;
			}	    	

			/* Only trips that fullfill all checked criteria are added; otherwise that loop would have been "continued" already */
			trips.add(trip);
			numberOfConsideredTrips++;
		}
		
		return trips;
	}
}