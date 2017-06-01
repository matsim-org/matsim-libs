package playground.dziemke.analysis;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.dziemke.utils.ShapeReader;

/**
 * @author dziemke
 */
public class TripAnalyzerV2Extended {
	public static final Logger LOG = Logger.getLogger(TripAnalyzerV2Extended.class);
	
	// Parameters
	private static String runId = "be_124"; // <----------
	private static String iterationForAnalysis = "300";
	private static final String cemdapPersonsInputFileId = "21"; // Check if this number corresponds correctly to the runId

	private static boolean onlyAnalyzeTripsWithMode = true; // <----------
	private static final String mode = TransportMode.car;
	
	private static final boolean onlyAnalyzeTripInteriorOfArea = false; // formerly results labelled as "int"
	private static boolean onlyAnalyzeTripsStartingOrEndingInArea = true; // formerly results labelled as "ber" (Berlin-based) <----------
	private static final Integer areaId = 11000000; // 11000000 = Berlin; Set a shapefile that contains this area correctly!
	
	private static boolean onlyAnalyzeTripsInDistanceRange = true; // "dist"; usually varied for analysis // <----------
	private static final double minDistance_km = 0;
	private static final double maxDistance_km = 100;

	private static final boolean onlyAnalyzeTripsWithActivityTypeBeforeTrip = false;
	private static final String activityTypeBeforeTrip = "shopping";
	private static final boolean onlyAnalyzeTripsWithActivityTypeAfterTrip = false;
	private static final String activityTypeAfterTrip = "work";

	private static final boolean onlyAnalyzeTripsDoneByPeopleInAgeRange = false; // "age"; this requires setting a CEMDAP file
	private static final Integer minAge = 80; // typically "x0"
	private static final Integer maxAge = 119; // typically "x9"; highest number usually chosen is 119

	private static final int binWidthDuration_min = 1;
	private static final int binWidthTime_h = 1;
	private static final int binWidthDistance_km = 1;
	private static final int binWidthSpeed_km_h = 1;

	private static String gnuplotScriptName = "plot_rel_path_run.gnu";

	// Input and output
	private static String networkFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network_shortIds.xml.gz"; // <----------
	private static String eventsFile = "../../../runs-svn/berlin_scenario_2016/" + runId + "/" + runId + ".output_events.xml.gz";
	private static String cemdapPersonsInputFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + cemdapPersonsInputFileId + "/persons1.dat"; // TODO
	private static String areaShapeFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
	private static String outputDirectory = "../../../runs-svn/berlin_scenario_2016/" + runId + "/analysis";

	// Variables to store objects
	private static Geometry areaGeometry;
	private static ObjectAttributes cemdapPersonAttributes;

	// Variables to store information
	private static double aggregateWeightOfConsideredTrips = 0;
	private static int numberOfInIncompleteTrips = 0;
    
	private static Map<Id<Trip>, Double> distanceRoutedMap = new TreeMap<>();
	private static Map<Id<Trip>, Double> distanceBeelineMap = new TreeMap<>();
    
	private static Map<String, Double> otherInformationMap = new TreeMap<>();
	
	public static void main(String[] args) {
		if (args.length != 0) {
			networkFile = args[0];
			eventsFile = args[1];
			areaShapeFile = args[2];
			outputDirectory = args[3];
			runId = args[4];
			iterationForAnalysis = args[5];
			onlyAnalyzeTripsWithMode = Boolean.valueOf(args[6]);
			onlyAnalyzeTripsStartingOrEndingInArea = Boolean.valueOf(args[7]);
			onlyAnalyzeTripsInDistanceRange = Boolean.valueOf(args[8]);
			gnuplotScriptName = null;
		}
		run();
	}

	private static void run() {
		double aggregateWeightOfTripsWithNonNegativeTimesAndDurations;
		double numberOfTripsWithCalculableSpeedBeeline;
		double numberOfTripsWithCalculableSpeedRouted;

		adaptOutputDirectory();

		/* Events infrastructure and reading the events file */
		EventsManager eventsManager = EventsUtils.createEventsManager();
		TripHandler tripHandler = new TripHandler();
		eventsManager.addHandler(tripHandler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(eventsFile);
		LOG.info("Events file read!");

	    /* Get network, which is needed to calculate distances */
		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		networkReader.readFile(networkFile);

		Map<Integer, Geometry> zoneGeometries = ShapeReader.read(areaShapeFile, "NR");
		areaGeometry = zoneGeometries.get(areaId);

		AnalysisFileWriter writer = new AnalysisFileWriter();

		if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
			// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
			CemdapPersonInputFileReader cemdapPersonInputFileReader = new CemdapPersonInputFileReader();
			cemdapPersonInputFileReader.parse(cemdapPersonsInputFile);
			cemdapPersonAttributes = cemdapPersonInputFileReader.getPersonAttributes();
		}

		List<Trip> trips = createListOfValidTrip(tripHandler.getTrips(), network);

	    /* Do calculations and write-out*/
		aggregateWeightOfTripsWithNonNegativeTimesAndDurations = TripAnalyzerV2Basic.countTripsWithNonNegativeTimesAndDurations(trips);

		Map <Integer, Double> tripDurationMap = TripAnalyzerV2Basic.createTripDurationMap(trips, binWidthDuration_min);
		double averageTripDuration = TripAnalyzerV2Basic.calculateAverageTripDuration_min(trips);
		writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, aggregateWeightOfConsideredTrips, averageTripDuration);
		writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, aggregateWeightOfConsideredTrips, averageTripDuration);

		Map <Integer, Double> departureTimeMap = TripAnalyzerV2Basic.createDepartureTimeMap(trips, binWidthTime_h);
		writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, aggregateWeightOfConsideredTrips, Double.NaN);

		Map<String, Double> activityTypeMap = TripAnalyzerV2Basic.createActivityTypeMap(trips);
		writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", aggregateWeightOfConsideredTrips);

		Map<Integer, Double> tripDistanceBeelineMap = TripAnalyzerV2Basic.createTripDistanceBeelineMap(trips, binWidthDistance_km, network);
		double averageTripDistanceBeeline_km = TripAnalyzerV2Basic.calculateAverageTripDistanceBeeline_km(trips, network);
		writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceBeeline_km);
		writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceBeeline_km);

		Map<Integer, Double> tripDistanceRoutedMap = TripAnalyzerV2Basic.createTripDistanceRoutedMap(trips, binWidthDistance_km, network);
		double averageTripDistanceRouted_km = TripAnalyzerV2Basic.calculateAverageTripDistanceRouted_km(trips, network);
		writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceRouted_km);

		numberOfTripsWithCalculableSpeedBeeline = TripAnalyzerV2Basic.countTripsWithCalculableSpeedBeeline(trips, network);
		Map<Integer, Double> averageTripSpeedBeelineMap = TripAnalyzerV2Basic.createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h, network);
		double averageOfAverageTripSpeedsBeeline_km_h = TripAnalyzerV2Basic.calculateAverageOfAverageTripSpeedsBeeline_km_h(trips, network);
		writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);
		writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);

		numberOfTripsWithCalculableSpeedRouted = TripAnalyzerV2Basic.countTripsWithCalculableSpeedRouted(trips, network);
		Map<Integer, Double> averageTripSpeedRoutedMap = TripAnalyzerV2Basic.createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h, network);
		double averageOfAverageTripSpeedsRouted_km_h = TripAnalyzerV2Basic.calculateAverageOfAverageTripSpeedsRouted_km_h(trips, network);
		writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedRouted, averageOfAverageTripSpeedsRouted_km_h);

		/* Other information */
		otherInformationMap.put("Number of trips that have no previous activity", (double) tripHandler.getNoPreviousEndOfActivityCounter());
		otherInformationMap.put("Number of trips that have no negative times or durations", aggregateWeightOfConsideredTrips - aggregateWeightOfTripsWithNonNegativeTimesAndDurations);
		otherInformationMap.put("Number of trips that have no calculable speed beeline", aggregateWeightOfConsideredTrips - numberOfTripsWithCalculableSpeedBeeline);
		otherInformationMap.put("Number of trips that have no calculable speed routed", aggregateWeightOfConsideredTrips - numberOfTripsWithCalculableSpeedRouted);
		otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", (double) numberOfInIncompleteTrips);
		otherInformationMap.put("Number of (complete) trips", aggregateWeightOfConsideredTrips);
		writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");

		// write a routed distance vs. beeline distance comparison file
		doBeelineCaluclations(trips, binWidthDistance_km, network);
		writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", aggregateWeightOfConsideredTrips);

		LOG.info(numberOfInIncompleteTrips + " trips are incomplete.");


		if (gnuplotScriptName != null) {
			runGnuplot(outputDirectory, gnuplotScriptName);
		}

	}

	private static void runGnuplot(String outputDirectory, String gnuplotScriptName) {
		/* Create gnuplot graphics */
//	    	String gnuplotScriptName = "plot_rel_path_run.gnu";
		String relativePathToGnuplotScript = "../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/gnuplot/" + gnuplotScriptName;
		String argument1= "wd_wt_carp_dist";

		GnuplotUtils.runGnuplotScript(outputDirectory, relativePathToGnuplotScript, argument1);
	}

	private static void doBeelineCaluclations(List<Trip> trips, int binWidthDistance_km, Network network) {
		Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
		for (Trip trip : trips) {
			double tripDistanceRouted_km = trip.getDistanceRoutedByCalculation_m(network) / 1000.;
			double tripDistanceBeeline_km = trip.getDistanceBeelineByCalculation_m(network) / 1000.;
			double tripWeight = trip.getWeight();
			AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, //maxBinDistance_km, 
					tripWeight);
			
			distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline_km); // TODO eventually remove this
			distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted_km); // TODO eventually remove this
		}
	}
	
	
	@SuppressWarnings("all")
	private static void adaptOutputDirectory() {
		outputDirectory = outputDirectory + "_" + iterationForAnalysis;
	    if (onlyAnalyzeTripsWithMode) {
			outputDirectory = outputDirectory + "_" + mode;
		}
	    if (onlyAnalyzeTripInteriorOfArea) {
			outputDirectory = outputDirectory + "_inside-" + areaId;
	    }
		if (onlyAnalyzeTripsStartingOrEndingInArea) {
			outputDirectory = outputDirectory + "_soe-in-" + areaId;
		}
		if (onlyAnalyzeTripsInDistanceRange) {
//			outputDirectory = outputDirectory + "_dist-" + maxDistance_km;
			outputDirectory = outputDirectory + "_dist-" + minDistance_km + "-" + maxDistance_km;
		}
		if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
			outputDirectory = outputDirectory + "_act-bef-" + activityTypeBeforeTrip;
		}
		if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
			outputDirectory = outputDirectory + "_act-aft-" + activityTypeAfterTrip;
		}
		if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
			outputDirectory = outputDirectory + "_age-" + minAge.toString() + "-" + maxAge.toString();
		}
		new File(outputDirectory).mkdir();
	}
	
	
	@SuppressWarnings("all")
	private static List<Trip> createListOfValidTrip(Map<Id<Trip>, Trip> tripMap, Network network) {
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

			// Choose if trip will be considered
			if (onlyAnalyzeTripsStartingOrEndingInArea) {
				if (!areaGeometry.contains(arrivalLocation) && !areaGeometry.contains(departureLocation)) {
					continue;
				}
			}
			if (onlyAnalyzeTripInteriorOfArea) {
				if (onlyAnalyzeTripsStartingOrEndingInArea) {
					Log.warn("onlyAnalyzeTripInteriorOfArea and onlyAnalyzeTripsStartingOrEndingInArea activated at the same time!");
				}
				if (!areaGeometry.contains(arrivalLocation) || !areaGeometry.contains(departureLocation)) {
					continue;
				}
			}
			if (onlyAnalyzeTripsWithMode) {
				if (!trip.getMode().equals(mode)) {
					continue;
				}
			}
			if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeelineByCalculation_m(network) / 1000.) > maxDistance_km) {
				continue;
			}
			if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeelineByCalculation_m(network) / 1000.) < minDistance_km) {
    			continue;
    		}
			if (onlyAnalyzeTripsWithActivityTypeBeforeTrip && onlyAnalyzeTripsWithActivityTypeAfterTrip) {
				Log.warn("onlyAnalyzeTripsWithActivityTypeBeforeTrip and onlyAnalyzeTripsWithActivityTypeAfterTrip activated at the same time."
						+ "This may lead to results that are hard to interpret: rather not use these options simultaneously.");
			}
			if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
				if (!trip.getActivityTypeBeforeTrip().equals(activityTypeBeforeTrip)) {
					continue;
				}
			}
			if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
				if (!trip.getActivityTypeAfterTrip().equals(activityTypeAfterTrip)) {
					continue;
				}
			}

			// TODO The plan was to calculate activity-chain frequencies here. Needs to be done somewhere else
			// write person activity attributes
//   	 	if (trip.getActivityEndActType().equals("work")) {
//   	 		personActivityAttributes.putAttribute(trip.getDriverId(), "hasWorkActivity", true);
//    		}

			// Person-specific attributes
			if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
				// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
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
			aggregateWeightOfConsideredTrips++;
		}
		
		return trips;
	}
}