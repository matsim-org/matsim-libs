package playground.dziemke.analysis;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

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

	/* Parameters */
	private static final String runId = "run_190";
	private static final String usedIteration = "300"; // most frequently used value: 150
	private static final String cemdapPersonsFileId = "21";
	
	private static final Integer planningAreaId = 11000000; // 11000000 = Berlin

	private static final boolean onlyCar = false; // "car"; new, should be used for runs with ChangeLegMode enabled
	private static final boolean onlyInterior = true; // "int"
	private static final boolean onlyBerlinBased = false; // "ber"; usually varied for analysis
	private static final boolean distanceFilter = true; // "dist"; usually varied for analysis
	// private static final double double minDistance = 0;
	private static final double maxDistance_km = 100; // most frequently used value: 150

	private static final boolean onlyWorkTrips = false; // "work"

	private static final boolean ageFilter = false; // "age"
	private static final Integer minAge = 80;
	private static final Integer maxAge = 119;	

	private static final int maxBinDuration_min = 120;
	private static final int binWidthDuration_min = 1;

	private static final int maxBinTime_h = 23;
	private static final int binWidthTime_h = 1;

	private static final int maxBinDistance_km = 60;
	private static final int binWidthDistance_km = 1;

	private static final int maxBinSpeed_kmh = 60;
	private static final int binWidthSpeed_kmh = 1;


	/* Input and output */
	private static final String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	private static final String eventsFile = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + 
			"/" + runId + "." + usedIteration + ".events.xml.gz";
	private static final String cemdapPersonFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + 
			cemdapPersonsFileId + "/persons1.dat";
	private static final String planningAreaShapeFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Berlin_DHDN_GK4.shp";
	private static String outputDirectory = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";

	
	/* Variables to store objects */
	private static Network network;
	private static Geometry planningAreaGeometry;
	private static CemdapPersonFileReader cemdapPersonFileReader;

	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		Map<Integer, Geometry> zoneGeometries = ShapeReader.read(planningAreaShapeFile, "NR");
		planningAreaGeometry = zoneGeometries.get(planningAreaId);

	    // Systematic output naming
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
//		outputDirectory = outputDirectory + "_2"; // in case used for double-check
				
		
		// Create an EventsManager instance (MATSim infrastructure)
	    EventsManager eventsManager = EventsUtils.createEventsManager();
	    TripHandler handler = new TripHandler();
	    eventsManager.addHandler(handler);
	 
	    // Connect a file reader to the EventsManager and read in the event file
	    MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	    reader.readFile(eventsFile);
	    System.out.println("Events file read!");
	    
	    // check if all trips have been completed; if so, result will be zero
	    int numberOfIncompleteTrips = 0;
	    for (Trip trip : handler.getTrips().values()) {
	    	if(!trip.getTripComplete()) { numberOfIncompleteTrips++; }
	    }
	    System.out.println(numberOfIncompleteTrips + " trips are incomplete.");
	    
	    
	    // --------------------------------------------------------------------------------------------------
	    cemdapPersonFileReader = new CemdapPersonFileReader();
	    if (ageFilter == true) {
	    	// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
	    	// so far age is the only one
		    // parse person file
		 	
		 	cemdapPersonFileReader.parse(cemdapPersonFile);
	    }
	 	// --------------------------------------------------------------------------------------------------
	    
	    	    	    
	    /* Get network, which is needed to calculate distances */
	    network = NetworkUtils.createNetwork();
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
	    networkReader.readFile(networkFile);
    	
    	// create objects
    	int tripCounter = 0;
    	int tripCounterSpeed = 0;
    	int tripCounterIncomplete = 0;
    	
    	Map <Integer, Double> tripDurationMap = new TreeMap <Integer, Double>();
	    double aggregateTripDuration = 0.;
	    
	    Map <Integer, Double> departureTimeMap = new TreeMap <Integer, Double>();
	    
	    Map <String, Double> activityTypeMap = new TreeMap <String, Double>();
	    
	    Map <Integer, Double> tripDistanceRoutedMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceRouted = 0.;
		
		Map <Integer, Double> tripDistanceBeelineMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceBeeline = 0.;
	    
		Map <Integer, Double> averageTripSpeedRoutedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsRouted = 0.;
	    
	    Map <Integer, Double> averageTripSpeedBeelineMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsBeeline = 0.;
	    

	    int numberOfTripsWithNoCalculableSpeed = 0;
	    
	    Map <Id<Trip>, Double> distanceRoutedMap = new TreeMap <Id<Trip>, Double>();
	    Map <Id<Trip>, Double> distanceBeelineMap = new TreeMap <Id<Trip>, Double>();
	    
	    Map <String, Integer> otherInformationMap = new TreeMap <String, Integer>();
	    
	    // --------------------------------------------------------------------------------------------------
	    ObjectAttributes personActivityAttributes = new ObjectAttributes();
	    // --------------------------------------------------------------------------------------------------
	    
	    
	    // do calculations
	    for (Trip trip : handler.getTrips().values()) {
	    	if(!trip.getTripComplete()) {
	    		System.err.println("Trip is not complete!");
	    		tripCounterIncomplete++;
	    		/* The only case where incomplete trips occur is when agents are removed according to "removeStuckVehicles = true"
	    		 * Since a removed agent can at most have one incomplete trip (the one when he is removed), the number of
	    		 * incomplete trips should be equal to the number of removed agents
	    		 */
	    		continue;
	    	}
	    	

	    	if (decideIfConsiderTrip(trip) == true) {
	    		tripCounter++;

	    		// calculate travel times and store them in a map
	    		// trip.getArrivalTime() / trip.getDepartureTime() yields values in seconds!
	    		double departureTime_s = trip.getDepartureTime();
	    		double arrivalTime_s = trip.getArrivalTime();
	    		double tripDuration_s = arrivalTime_s - departureTime_s;
	    		double tripDuration_min = tripDuration_s / 60.;
	    		double tripDuration_h = tripDuration_min / 60.;
	    		addToMapIntegerKey(tripDurationMap, tripDuration_min, binWidthDuration_min, maxBinDuration_min, 1.);
	    		aggregateTripDuration = aggregateTripDuration + tripDuration_min;	 

	    		// store departure times in a map
	    		double departureTime_h = departureTime_s / 3600.;
	    		addToMapIntegerKey(departureTimeMap, departureTime_h, binWidthTime_h, maxBinTime_h, 1.);

	    		// store activities in a map
	    		String activityType = trip.getActivityStartActType();
	    		addToMapStringKey(activityTypeMap, activityType);

	    		// calculate (routed) distances and and store them in a map
	    		double tripDistance_m = 0.;
	    		for (int i = 0; i < trip.getLinks().size(); i++) {
	    			Id<Link> linkId = trip.getLinks().get(i);
	    			Link link = network.getLinks().get(linkId);
	    			double length_m = link.getLength();
	    			tripDistance_m = tripDistance_m + length_m;
	    		}
	    		// TODO here, the distances from activity to link and link to activity are missing!
	    		double tripDistanceRouted = tripDistance_m / 1000.;

	    		// store (routed) distances  in a map
	    		addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted, binWidthDistance_km, maxBinDistance_km, 1.);
	    		aggregateTripDistanceRouted = aggregateTripDistanceRouted + tripDistanceRouted;
	    		distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);

	    		// store (beeline) distances in a map
	    		double tripDistanceBeeline = trip.getBeelineDistance(network);
	    		addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline, binWidthDistance_km, maxBinDistance_km, 1.);
	    		aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + tripDistanceBeeline;
	    		distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline);

	    		// calculate speeds and and store them in a map
	    		if (tripDuration_h > 0.) {
	    			//System.out.println("trip distance is " + tripDistance + " and time is " + timeInHours);
	    			double averageTripSpeedRouted = tripDistanceRouted / tripDuration_h;
	    			addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, binWidthSpeed_kmh, maxBinSpeed_kmh, 1.);
	    			aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted;

	    			double averageTripSpeedBeeline = tripDistanceBeeline / tripDuration_h;
	    			addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, binWidthSpeed_kmh, maxBinSpeed_kmh, 1.);
	    			aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline;

	    			tripCounterSpeed++;
	    		} else {
	    			numberOfTripsWithNoCalculableSpeed++;
	    		}
	    	}
	    }
	    
	    double averageTripDuration = aggregateTripDuration / tripCounter;
	    double averageTripDistanceRouted = aggregateTripDistanceRouted / tripCounter;
	    double averageTripDistanceBeeline = aggregateTripDistanceBeeline / tripCounter;
	    double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / tripCounterSpeed;
	    double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / tripCounterSpeed;
	    
	    
	    otherInformationMap.put("Number of trips that have no previous activity", handler.getNoPreviousEndOfActivityCounter());
	    otherInformationMap.put("Number of trips that have no calculable speed", numberOfTripsWithNoCalculableSpeed);
	    otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", tripCounterIncomplete);
	    otherInformationMap.put("Number of (complete) trips", tripCounter);
	 
	    
	    // write results to files
	    new File(outputDirectory).mkdir();
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, tripCounter, averageTripDuration);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", tripCounter);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, tripCounter, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed_kmh, tripCounterSpeed, averageOfAverageTripSpeedsRouted);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_kmh, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_kmh, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", tripCounter);
	}
	
	
	@SuppressWarnings("unused")
	private static boolean decideIfConsiderTrip(Trip trip){
    	boolean considerTrip = true;

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
    			considerTrip = false;
    		}
    	}
    	if (onlyInterior == true) {
    		if (!planningAreaGeometry.contains(arrivalLocation) || !planningAreaGeometry.contains(departureLocation)) {
    			considerTrip = false;
    		}
    	}
//		if (!trip.getMode().equals("car") && !trip.getMode().equals("pt")) {
//			throw new RuntimeException("In current implementation leg mode must either be car or pt");
//		}
    	if (onlyCar == true) {
    		if (!trip.getMode().equals("car")) {
    			considerTrip = false;
    		}
    	}
    	if (distanceFilter == true && trip.getBeelineDistance(network) >= maxDistance_km) {
    		considerTrip = false;
    	}
//    	if (distanceFilter == true && trip.getBeelineDistance(network) <= minDistance) {
//    		considerTrip = false;
//    	}
    	if (onlyWorkTrips == true) {
    		if (trip.getActivityEndActType().equals("work")) {
    			considerTrip = false;
    		}
    	}
    	
    	// TODO The plan was to calculate activity-chain frequencies here. Needs to be done somewhere else
    	// write person activity attributes
//    	if (trip.getActivityEndActType().equals("work")) {
//    		personActivityAttributes.putAttribute(trip.getDriverId(), "hasWorkActivity", true);
//    	}

    	// --------------------------------------------------------------------------------------------------
    	// PERSON-SPECIFIC ATTRIBUTES
    	if (ageFilter == true) {
    		// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
    		// so far age is the only one
    		String personId = trip.getPersonId().toString();
    		int age = (int) cemdapPersonFileReader.getPersonAttributes().getAttribute(personId, "age");

    		if (age < minAge) {
    			considerTrip = false;
    		}
    		if (age > maxAge) {
    			considerTrip = false;
    		}
    	}
    	// --------------------------------------------------------------------------------------------------

    	return considerTrip;
	}

	
	private static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue, int binWidth, int limitOfLastBin, double weight) {
		double inputValueBin = inputValue / binWidth;
		int ceilOfLastBin = limitOfLastBin / binWidth;		
		// Math.ceil returns the higher integer number (but as a double value)
		int ceilOfValue = (int)Math.ceil(inputValueBin);
		if (ceilOfValue < 0) {
			new RuntimeException("Lower end of bin may not be smaller than zero!");
		}
				
		if (ceilOfValue >= ceilOfLastBin) {
			ceilOfValue = ceilOfLastBin;
		}
						
		if (!map.containsKey(ceilOfValue)) {
			map.put(ceilOfValue, weight);
		} else {
			double value = map.get(ceilOfValue);
			value = value + weight;
			map.put(ceilOfValue, value);
		}			
	}
	
	
	private static void addToMapStringKey(Map <String, Double> map, String caption) {
		if (!map.containsKey(caption)) {
			map.put(caption, 1.);
		} else {
			double value = map.get(caption);
			value++;
			map.put(caption, value);
		}
	}	
}