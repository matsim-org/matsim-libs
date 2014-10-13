package playground.dziemke.analysis;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.dziemke.utils.ShapeReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 * 
 */
public class TripAnalyzer {
	public static void main(String[] args) {
	    // Parameters
		boolean onlyInterior = false;		//int
		boolean onlyBerlinBased = true;		//ber	usually varied for analysis
		boolean distanceFilter = true;		//dist		usually varied for analysis
		double maxDistance = 100;
		double minDistance = 0;
		Integer planningAreaId = 11000000;
		
		String runId = "run_145f";
		//String runId = "run_20";
	    String usedIteration = "150";
	    //String usedIteration = "100";
	    
//	    int maxBinDuration = 120;
//	    int binWidthDuration = 5;
//	    
//	    int maxBinTime = 23;
//	    int binWidthTime = 1;
//	    
//	    int maxBinDistance = 60;
//	    int binWidthDistance = 5;
//	    	    
//	    int maxBinSpeed = 60;
//	    int binWidthSpeed = 5;
	    
	    int maxBinDuration = 120;
	    int binWidthDuration = 1;
	    
	    int maxBinTime = 23;
	    int binWidthTime = 1;
	    
	    int maxBinDistance = 60;
	    int binWidthDistance = 1;
	    	    
	    int maxBinSpeed = 60;
	    int binWidthSpeed = 1;
	    
	    
	    // Input and output files
	    //String networkFile = "D:/Workspace/container/demand/input/iv_counts/network.xml";
	    String networkFile = "D:/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	    //String eventsFile = "D:/Workspace/container/demand/output/beeline/" + runId + "/ITERS/it." + usedIteration + "/" 
		//		+ runId + "." + usedIteration + ".events.xml.gz";
	    String eventsFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + usedIteration + "/" 
				+ runId + "." + usedIteration + ".events.xml.gz";
	    // String eventsFile = "D:/Workspace/container/examples/equil/output/" + runId + "/ITERS/it.200/" + runId + ".200.events.xml.gz";
	    //String configFile = "D:/Workspace/container/demand/output/" + runId + "/" + runId + ".output_config.xml.gz";
	    //String outputDirectory = "D:/Workspace/container/demand/output/beeline/" + runId + "/analysis";
	    String outputDirectory = "D:/Workspace/container/demand/output/" + runId + "/analysis";
	    
	    String shapeFileBerlin = "D:/Workspace/container/demand/input/shapefiles/Berlin_DHDN_GK4.shp";
	    Map<Integer, Geometry> zoneGeometries = ShapeReader.read(shapeFileBerlin, "NR");
	    Geometry berlinGeometry = zoneGeometries.get(planningAreaId);

	    Integer usedIt = Integer.parseInt(usedIteration);
	    if (!usedIt.equals(150)) {
	    	outputDirectory = outputDirectory + "_" + usedIteration;
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
	    
	    	    	    
	    // get network, which is needed to calculate distances
//	    Config config = ConfigUtils.loadConfig(configFile);
//    	Scenario scenario = ScenarioUtils.loadScenario(config);
//    	Network network = scenario.getNetwork();
	    Config config = ConfigUtils.createConfig();
	    Scenario scenario = ScenarioUtils.createScenario(config);
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
	    networkReader.readFile(networkFile);
	    Network network = scenario.getNetwork();
	    
    	
    	// create objects
    	int tripCounter = 0;
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
	    //TODO
	    
	    Map <Integer, Double> averageTripSpeedBeelineMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsBeeline = 0.;
	    double tripCounterSpeedBeeline = 0.;

	    int numberOfTripsWithNoCalculableSpeed = 0;
	    
	    Map <Id, Double> distanceRoutedMap = new TreeMap <Id, Double>();
	    Map <Id, Double> distanceBeelineMap = new TreeMap <Id, Double>();
	    
	    
	    // do calculations
	    for (Trip trip : handler.getTrips().values()) {
	    	if(trip.getTripComplete()) {
	    		boolean considerTrip = false;
	    		
	    		
	    		// get coordinates of links
	    		Id departureLinkId = trip.getDepartureLinkId();
	    		Id arrivalLinkId = trip.getArrivalLinkId();
	    		
	    		Link departureLink = network.getLinks().get(departureLinkId);
	    		Link arrivalLink = network.getLinks().get(arrivalLinkId);
	    		
	    		double arrivalCoordX = arrivalLink.getCoord().getX();
	    		double arrivalCoordY = arrivalLink.getCoord().getY();
	    		double departureCoordX = departureLink.getCoord().getX();
	    		double departureCoordY = departureLink.getCoord().getY();
	    		
	    		
	    		// calculate (beeline) distance
	    		double horizontalDistanceInMeter = (Math.abs(departureCoordX - arrivalCoordX)) / 1000;
	    		double verticalDistanceInMeter = (Math.abs(departureCoordY - arrivalCoordY)) / 1000;
	    		
	    		double tripDistanceBeeline = Math.sqrt(horizontalDistanceInMeter * horizontalDistanceInMeter 
	    				+ verticalDistanceInMeter * verticalDistanceInMeter);
	    		
	    		
	    		// create points
	    		Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
    			Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);
	    		
    			
	    		// choose if trip will be considered
	    		if (onlyInterior == true) {
	    			if (berlinGeometry.contains(arrivalLocation) && berlinGeometry.contains(departureLocation)) {
	    				considerTrip = true;
	    			}
	    		} else if (onlyBerlinBased == true) {
	    			if (berlinGeometry.contains(arrivalLocation) || berlinGeometry.contains(departureLocation)) {
	    				considerTrip = true;
	    			}
	    		} else {
	    			considerTrip = true;
	    		}
	    		
	    		if (distanceFilter == true && tripDistanceBeeline >= maxDistance) {
	    			considerTrip = false;
	    		}
	    		
//	    		if (distanceFilter == true && tripDistanceBeeline <= minDistance) {
//	    			considerTrip = false;
//	    		}
	    		
	    		
	    		if (considerTrip == true) {
		    		tripCounter++;
		    		
		    		// calculate travel times and store them in a map
		    		// trip.getArrivalTime() / trip.getDepartureTime() yields values in seconds!
		    		// trip.get...Time() delivers value in seconds!
		    		double departureTimeInSeconds = trip.getDepartureTime();
		    		double arrivalTimeInSeconds = trip.getArrivalTime();
		    		double tripDurationInSeconds = arrivalTimeInSeconds - departureTimeInSeconds;
		    		double tripDurationInMinutes = tripDurationInSeconds / 60.;
		    		double tripDurationInHours = tripDurationInMinutes / 60.;
		    		//addToMapIntegerKey(tripDurationMap, tripDurationInMinutes, 5, 120);
		    		addToMapIntegerKey(tripDurationMap, tripDurationInMinutes, binWidthDuration, maxBinDuration);
		    		aggregateTripDuration = aggregateTripDuration + tripDurationInMinutes;	 
	
		    		
		    		// store departure times in a map
		    		double departureTimeInHours = departureTimeInSeconds / 3600.;
		    		addToMapIntegerKey(departureTimeMap, departureTimeInHours, binWidthTime, maxBinTime);
		    		
		    		
		    		// store activities in a map
		    		String activityType = trip.getActivityStartActType();
					addToMapStringKey(activityTypeMap, activityType);
		
					
					// calculate (routed) distances and and store them in a map
					double tripDistanceMeter = 0.;
					for (int i = 0; i < trip.getLinks().size(); i++) {
						Id linkId = trip.getLinks().get(i);
						Link link = network.getLinks().get(linkId);
						double length = link.getLength();
						tripDistanceMeter = tripDistanceMeter + length;
					}
					double tripDistanceRouted = tripDistanceMeter / 1000.;
					
					
					// store (routed) distances  in a map
					addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted, binWidthDistance, maxBinDistance);
		    		aggregateTripDistanceRouted = aggregateTripDistanceRouted + tripDistanceRouted;
		    		distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);
	
		    		
		    		// store (beeline) distances in a map
		    		addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline, binWidthDistance, maxBinDistance);
		    		aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + tripDistanceBeeline;
		    		distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline);
	
		    		
		    		// calculate speeds and and store them in a map
		    		if (tripDurationInHours > 0.) {
		    			//System.out.println("trip distance is " + tripDistance + " and time is " + timeInHours);
			    		double averageTripSpeedRouted = tripDistanceRouted / tripDurationInHours;
			    		//addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, 5, 60);
			    		addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, binWidthSpeed, maxBinSpeed);
			    		aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted;
			    		
			    		double averageTripSpeedBeeline = tripDistanceBeeline / tripDurationInHours;
			    		//addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, 5, 60);
			    		addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, binWidthSpeed, maxBinSpeed);
			    		aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline;
			    		tripCounterSpeedBeeline = tripCounterSpeedBeeline + 1.;
		    		} else {
		    			numberOfTripsWithNoCalculableSpeed++;
		    		}
	    		}
			} else {
	    		System.err.println("Trip is not complete!");
	    		tripCounterIncomplete++;
	    		// Until now, the only case where incomplete trips happen is when agents are removed according to "removeStuckVehicles = true"
	    		// Since a removed agent can at most have one incomplete trip (this incomplete trip is exactly the event when he is removed)
	    		// the number of incomplete trips should be equal to the number of removed agents
	    	}
	    }
	    
	    double averageTripDuration = aggregateTripDuration / tripCounter;
	    double averageTripDistanceRouted = aggregateTripDistanceRouted / tripCounter;
	    double averageTripDistanceBeeline = aggregateTripDistanceBeeline / tripCounter;
	    double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / tripCounter;
	    //double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / tripCounter;
	    double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / tripCounterSpeedBeeline;
	    
	    
	    // write results to files
	    new File(outputDirectory).mkdir();
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime, tripCounter, averageTripDuration);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", tripCounter);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance, tripCounter, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed, tripCounter, averageOfAverageTripSpeedsRouted);
	    //writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed, tripCounter, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed, tripCounterSpeedBeeline, averageOfAverageTripSpeedsBeeline);
	    
	    
	    //------------------------------------------------------------------------------------------------------------
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed, tripCounterSpeedBeeline, averageOfAverageTripSpeedsBeeline);
	    //------------------------------------------------------------------------------------------------------------
	    
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", tripCounter);
		
	    
    	// return number of trips that have no previous activity
	    System.out.println("Number of trips that have no previous activity is: " + handler.getNoPreviousEndOfActivityCounter());
	    
	    
	    // return number of trips that have no calculable speed
	    System.out.println("Number of trips that have no calculable speed is: " + numberOfTripsWithNoCalculableSpeed);
	    
	    
	    // return number of incomplete trips (i.e. number of removed agents)
	    System.out.println("Number of incomplete trips (i.e. number of removed agents) is: " + tripCounterIncomplete);
	    
	    
	    // for the sake of couriosity also return number of (complete) trips (not in any way related to number of agent)
	    System.out.println("Number of (complete) trips is: " + tripCounter);
	}


	private static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue,
			int binWidth, int limitOfLastBin) {
		double inputValueBin = inputValue / binWidth;
		int floorOfLastBin = limitOfLastBin / binWidth;
//		// Math.floor returns next lower integer number (but as a double value)
//		int floorOfValue = (int)Math.floor(inputValueBin);
//		if (floorOfValue < 0) {
//			System.err.println("Lower end of bin may not be smaller than zero!");
//		}
//		
//		if (floorOfValue >= floorOfLastBin) {
//			floorOfValue = floorOfLastBin;
//		}
//		
//		if (!map.containsKey(floorOfValue)) {
//			map.put(floorOfValue, 1.);
//		} else {
//			double value = map.get(floorOfValue);
//			value++;
//			map.put(floorOfValue, value);
//		}
		
		// Math.ceil returns the higher integer number (but as a double value)
		int ceilOfValue = (int)Math.ceil(inputValueBin);
		if (ceilOfValue < 0) {
			System.err.println("Lower end of bin may not be smaller than zero!");
		}
				
//		if (ceilOfValue >= floorOfLastBin) {
//			ceilOfValue = floorOfLastBin;
//		}
						
		if (!map.containsKey(ceilOfValue)) {
			map.put(ceilOfValue, 1.);
		} else {
			double value = map.get(ceilOfValue);
			value++;
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