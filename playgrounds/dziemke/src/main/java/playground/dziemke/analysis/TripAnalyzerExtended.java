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
public class TripAnalyzerExtended {
	public static final Logger log = Logger.getLogger(TripAnalyzerExtended.class);
	
	/* Parameters */
	private static final String runId = "run_146c";
	private static final String usedIteration = "150"; // most frequently used value: 150
	private static final String cemdapPersonsInputFileId = "21"; // check if this number corresponds correctly to the runId
	
	private static final Integer planningAreaId = 11000000; // 11000000 = Berlin

	private static final boolean onlyCar = false; // "car"; should be used for runs with ChangeLegMode enabled
	private static final boolean onlyInterior = false; // "int"
	private static final boolean onlyBerlinBased = true; // "ber"; usually varied for analysis
	
	private static final boolean distanceFilter = true; // "dist"; usually varied for analysis
	// private static final double double minDistance = 0;
	private static final double maxDistance_km = 100;

	private static final boolean onlyWorkTrips = false; // "work"

	private static final boolean ageFilter = false; // "age"
	private static final Integer minAge = 80; // typically "x0"
	private static final Integer maxAge = 119; // typically "x9"; highest number ususally chosen is 119

	private static final int binWidthDuration_min = 1;
	private static final int binWidthTime_h = 1;
	private static final int binWidthDistance_km = 1;
	private static final int binWidthSpeed_km_h = 1;

	/* Input and output */
	private static final String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	private static final String eventsFile = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + 
			"/" + runId + "." + usedIteration + ".events.xml.gz";
	private static final String cemdapPersonsInputFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + 
			cemdapPersonsInputFileId + "/persons1.dat";
	private static final String planningAreaShapeFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Berlin_DHDN_GK4.shp";
	private static String outputDirectory = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";
	
	private static String gnuplotScriptName = "plot_rel_path_run.gnu";

	/* Variables to store objects */
	private static Geometry planningAreaGeometry;
	private static ObjectAttributes cemdapPersonAttributes;

	/* Variables to store information */
	private static double aggregateWeightOfConsideredTrips = 0;
	private static int numberOfInIncompleteTrips = 0;
    
	private static Map<Id<Trip>, Double> distanceRoutedMap = new TreeMap<>();
	private static Map<Id<Trip>, Double> distanceBeelineMap = new TreeMap<>();
    
	private static Map<String, Double> otherInformationMap = new TreeMap<>();

	
	public static void main(String[] args) {
		double aggregateWeightOfTripsWithNonNegativeTimesAndDurations = 0;
		double numberOfTripsWithCalculableSpeedBeeline = 0;
		double numberOfTripsWithCalculableSpeedRouted = 0;
		
		adaptOutputDirectory();
	    
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
	    
	    Map<Integer, Geometry> zoneGeometries = ShapeReader.read(planningAreaShapeFile, "NR");
		planningAreaGeometry = zoneGeometries.get(planningAreaId);	    
	    
		AnalysisFileWriter writer = new AnalysisFileWriter();

		if (ageFilter == true) {
	    	// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
	    	CemdapPersonInputFileReader cemdapPersonInputFileReader = new CemdapPersonInputFileReader();
		 	cemdapPersonInputFileReader.parse(cemdapPersonsInputFile);
		 	cemdapPersonAttributes = cemdapPersonInputFileReader.getPersonAttributes();
	    }
	    
	    List<Trip> trips = createListOfValidTrip(tripHandler.getTrips(), network);
	    
	    /* Do calculations and write-out*/
	    aggregateWeightOfTripsWithNonNegativeTimesAndDurations = TripAnalyzerBasic.countTripsWithNonNegativeTimesAndDurations(trips);
	    
	    Map <Integer, Double> tripDurationMap = TripAnalyzerBasic.createTripDurationMap(trips, binWidthDuration_min);
	    double averageTripDuration = TripAnalyzerBasic.calculateAverageTripDuration_min(trips);
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, aggregateWeightOfConsideredTrips, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, aggregateWeightOfConsideredTrips, averageTripDuration);

	    Map <Integer, Double> departureTimeMap = TripAnalyzerBasic.createDepartureTimeMap(trips, binWidthTime_h);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, aggregateWeightOfConsideredTrips, Double.NaN);
	    	    
	    Map<String, Double> activityTypeMap = TripAnalyzerBasic.createActivityTypeMap(trips);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", aggregateWeightOfConsideredTrips);
	    
	    Map<Integer, Double> tripDistanceBeelineMap = TripAnalyzerBasic.createTripDistanceBeelineMap(trips, binWidthDistance_km, network);
		double averageTripDistanceBeeline_km = TripAnalyzerBasic.calculateAverageTripDistanceBeeline_km(trips, network);
		writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceBeeline_km);
		writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceBeeline_km);
	    
	    Map<Integer, Double> tripDistanceRoutedMap = TripAnalyzerBasic.createTripDistanceRoutedMap(trips, binWidthDistance_km, network);
	    double averageTripDistanceRouted_km = TripAnalyzerBasic.calculateAverageTripDistanceRouted_km(trips, network);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, aggregateWeightOfConsideredTrips, averageTripDistanceRouted_km);
	    
	    numberOfTripsWithCalculableSpeedBeeline = TripAnalyzerBasic.countTripsWithCalculableSpeedBeeline(trips, network);
	    Map<Integer, Double> averageTripSpeedBeelineMap = TripAnalyzerBasic.createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h, network);
		double averageOfAverageTripSpeedsBeeline_km_h = TripAnalyzerBasic.calculateAverageOfAverageTripSpeedsBeeline_km_h(trips, network);
		writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);
		writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeedBeeline, averageOfAverageTripSpeedsBeeline_km_h);
		
		numberOfTripsWithCalculableSpeedRouted = TripAnalyzerBasic.countTripsWithCalculableSpeedRouted(trips, network);
		Map<Integer, Double> averageTripSpeedRoutedMap = TripAnalyzerBasic.createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h, network);
		double averageOfAverageTripSpeedsRouted_km_h = TripAnalyzerBasic.calculateAverageOfAverageTripSpeedsRouted_km_h(trips, network);
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
	    	    
	    log.info(numberOfInIncompleteTrips + " trips are incomplete.");
	    
	    
	    /* Create gnuplot graphics */
//	    String gnuplotScriptName = "plot_rel_path_run.gnu";
	    String pathToSpecificAnalysisDir = outputDirectory;
		String relativePathToGnuplotScript = "../../../../shared-svn/projects/cemdapMatsimCadyts/analysis/gnuplot/" + gnuplotScriptName;
		String argument1= "wd_wt_carp_dist";

		GnuplotUtils.runGnuplotScript(pathToSpecificAnalysisDir, relativePathToGnuplotScript, argument1);
	}
	
	
	static void doBeelineCaluclations(List<Trip> trips, int binWidthDistance_km, Network network) {
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
		outputDirectory = outputDirectory + "_9"; // TODO in case used for double-check
		
		/* Create directory */
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
			aggregateWeightOfConsideredTrips++;
		}
		
		return trips;
	}
}