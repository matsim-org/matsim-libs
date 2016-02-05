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
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.dziemke.utils.ShapeReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 * 
 */
public class CemdapBasedTripAnalyzer {
	public static void main(String[] args) {
	    
		// Parameters
		String runId = "run_168a";
//		String runId = "run791";
		String usedIteration = "300"; // most frequently used value: 150
//		String usedIteration = "600";
		
		// TODO why was that necessary?
		Integer planningAreaId = 11000000;
		
		boolean onlyCar = false; // "car"; new, should be used for runs with ChangeLedModes enabled
		
		boolean onlyInterior = false; // "int"
		
		boolean onlyBerlinBased = true; // "ber"; usually varied for analysis
		
		boolean distanceFilter = true; // "dist"; usually varied for analysis
		//double minDistance = 0;
		double maxDistance = 100;
		
		boolean onlyWorkTrips = false; // "work"
				
		boolean ageFilter = false; // "age"
		Integer minAge = 80;
		Integer maxAge = 119;	
		
		int maxBinDuration = 120;
	    int binWidthDuration = 1;
	    
	    int maxBinTime = 23;
	    int binWidthTime = 1;
	    
	    int maxBinDistance = 60;
	    int binWidthDistance = 1;
	    	    
	    int maxBinSpeed = 60;
	    int binWidthSpeed = 1;
	    
	    
	    // Input and output files
//	    String networkFile = "/Users/dominik/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	    String networkFile = "../../../../Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
//	    String networkFile = "D:/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
//	    String networkFile = "D:/Workspace/runs-svn/"  + runId + "/counts_network_merged.xml_cl.xml.gz";
	    
//	    String eventsFile = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/ITERS/it." + usedIteration + "/" 
//	    String eventsFile = "D:/Workspace/runs-svn/" + runId + "/output_rerun/ITERS/it." + usedIteration + "/" 
//	    		+ runId + "." + usedIteration + ".events.txt.gz";
//	    String eventsFile = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + "/" 
//				+ runId + "." + usedIteration + ".events.xml.gz";
	    String eventsFile = "../../../../Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + "/" 
//	    String eventsFile = "/Users/dominik/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + "/" 
				+ runId + "." + usedIteration + ".events.xml.gz";

//	    String cemdapPersonFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/19/persons1.dat"; // wrong!!!
//	    String cemdapPersonFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/18/persons1.dat";
//	    String cemdapPersonFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/21/persons1.dat";
	    
//	    String cemdapPersonFile = "../../../../Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/21/persons1.dat";
	    String cemdapPersonFile = "../../../../Workspace/shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/21/persons1.dat";
	    
//	    String cemdapPersonFile = "/Users/dominik/Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/21/persons1.dat";
	    
	    //String outputDirectory = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/analysis";
//	    String outputDirectory = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";
	    String outputDirectory = "../../../../Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";
//	    String outputDirectory = "/Users/dominik/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";
//	    String outputDirectory = "D:/Workspace/runs-svn/other/" + runId + "/analysis";
	    
//	    String shapeFileBerlin = "D:/Workspace/data/cemdapMatsimCadyts/input/shapefiles/Berlin_DHDN_GK4.shp";
	    
//	    String shapeFileBerlin = "../../../../Workspace/data/cemdapMatsimCadyts/input/shapefiles/Berlin_DHDN_GK4.shp";
	    String shapeFileBerlin = "../../../../Workspace/shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Berlin_DHDN_GK4.shp";
	    
//	    String shapeFileBerlin = "/Users/dominik/Workspace/data/cemdapMatsimCadyts/input/shapefiles/Berlin_DHDN_GK4.shp";
	    Map<Integer, Geometry> zoneGeometries = ShapeReader.read(shapeFileBerlin, "NR");
	    Geometry berlinGeometry = zoneGeometries.get(planningAreaId);
	    
	       
	    // Systematic output naming
	    Integer iteration4Analysis = Integer.parseInt(usedIteration);
	    if (!iteration4Analysis.equals(150)) {
	    	outputDirectory = outputDirectory + "_" + usedIteration;
	    }
	    
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
		
		
		// new 2015-12-09 for doublecheck
		outputDirectory = outputDirectory + "_2";
		// ----
		
		
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
	    CemdapPersonFileReader cemdapPersonFileReader = new CemdapPersonFileReader();
	    if (ageFilter == true) {
	    	// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
	    	// so far age is the only one
		    // parse person file
		 	
		 	cemdapPersonFileReader.parse(cemdapPersonFile);
	    }
	 	// --------------------------------------------------------------------------------------------------
	    
	    	    	    
	    // get network, which is needed to calculate distances
	    Config config = ConfigUtils.createConfig();
	    Scenario scenario = ScenarioUtils.createScenario(config);
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	    networkReader.readFile(networkFile);
	    Network network = scenario.getNetwork();
	    
    	
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
	    	if(trip.getTripComplete()) {
	    		boolean considerTrip = false;
	    		
	    		
	    		// get coordinates of links
	    		Id<Link> departureLinkId = trip.getDepartureLinkId();
	    		Id<Link> arrivalLinkId = trip.getArrivalLinkId();
	    		
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
    			// Note: Check of "interior"/"berlinBased" has to come first since this sets the "considerTrip"
    			// variable to true.
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
	    		
	    		// --------------------------------------------------------------------------------------------------
//				if (!trip.getMode().equals("car") && !trip.getMode().equals("pt")) {
//					throw new RuntimeException("In current implementation leg mode must either be car or pt");
//				}
				
				if (onlyCar == true) {
					if (!trip.getMode().equals("car")) {
						considerTrip = false;
					}
				}
			    // --------------------------------------------------------------------------------------------------
	    		
	    		if (distanceFilter == true && tripDistanceBeeline >= maxDistance) {
	    			considerTrip = false;
	    		}
	    		
//	    		if (distanceFilter == true && tripDistanceBeeline <= minDistance) {
//	    			considerTrip = false;
//	    		}
	    		
	    		//--------------------------------------------------------------------------------------------------------------------
	    		if (onlyWorkTrips == true) {
		    		boolean doesWorkTrip = false;
		    		if (trip.getActivityEndActType().equals("work")) {
		    			doesWorkTrip = true;	    			
		    		}
		    		
					if (doesWorkTrip == true) { // can be varied
		    			considerTrip = false;
		    		}
		    	}
				//--------------------------------------------------------------------------------------------------------------------
	    		
	    		// write person activity attributes
//	    		if (trip.getActivityEndActType().equals("work")) {
//	    			personActivityAttributes.putAttribute(trip.getDriverId(), "hasWorkActivity", true);
//	    		}
	    		// TODO The plan was to claculated activity-chain frequencies here...
	    		
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
	    		
	    		
	    		
	    		if (considerTrip == true) {
		    		tripCounter++;
		    		
		    		// calculate travel times and store them in a map
		    		// trip.getArrivalTime() / trip.getDepartureTime() yields values in seconds!
		    		double departureTimeInSeconds = trip.getDepartureTime();
		    		double arrivalTimeInSeconds = trip.getArrivalTime();
		    		double tripDurationInSeconds = arrivalTimeInSeconds - departureTimeInSeconds;
		    		double tripDurationInMinutes = tripDurationInSeconds / 60.;
		    		double tripDurationInHours = tripDurationInMinutes / 60.;
		    		//addToMapIntegerKey(tripDurationMap, tripDurationInMinutes, 5, 120);
		    		addToMapIntegerKey(tripDurationMap, tripDurationInMinutes, binWidthDuration, maxBinDuration, 1.);
		    		aggregateTripDuration = aggregateTripDuration + tripDurationInMinutes;	 
	
		    		
		    		// store departure times in a map
		    		double departureTimeInHours = departureTimeInSeconds / 3600.;
		    		addToMapIntegerKey(departureTimeMap, departureTimeInHours, binWidthTime, maxBinTime, 1.);
		    		
		    		
		    		// store activities in a map
		    		String activityType = trip.getActivityStartActType();
					addToMapStringKey(activityTypeMap, activityType);
		
					
					// calculate (routed) distances and and store them in a map
					double tripDistanceMeter = 0.;
					for (int i = 0; i < trip.getLinks().size(); i++) {
						Id<Link> linkId = trip.getLinks().get(i);
						Link link = network.getLinks().get(linkId);
						double length = link.getLength();
						tripDistanceMeter = tripDistanceMeter + length;
					}
					// TODO here, the distances from activity to link and link to activity are missing!
					double tripDistanceRouted = tripDistanceMeter / 1000.;
					
					
					// store (routed) distances  in a map
					addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted, binWidthDistance, maxBinDistance, 1.);
		    		aggregateTripDistanceRouted = aggregateTripDistanceRouted + tripDistanceRouted;
		    		distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);
	
		    		
		    		// store (beeline) distances in a map
		    		addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline, binWidthDistance, maxBinDistance, 1.);
		    		aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + tripDistanceBeeline;
		    		distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline);
	
		    		
		    		// calculate speeds and and store them in a map
		    		if (tripDurationInHours > 0.) {
		    			//System.out.println("trip distance is " + tripDistance + " and time is " + timeInHours);
			    		double averageTripSpeedRouted = tripDistanceRouted / tripDurationInHours;
			    		addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, binWidthSpeed, maxBinSpeed, 1.);
			    		aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted;
			    		
			    		double averageTripSpeedBeeline = tripDistanceBeeline / tripDurationInHours;
			    		addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, binWidthSpeed, maxBinSpeed, 1.);
			    		aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline;
			    		
			    		tripCounterSpeed++;
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
	    double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / tripCounterSpeed;
	    double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / tripCounterSpeed;
	    
	    
	    otherInformationMap.put("Number of trips that have no previous activity", handler.getNoPreviousEndOfActivityCounter());
	    otherInformationMap.put("Number of trips that have no calculable speed", numberOfTripsWithNoCalculableSpeed);
	    otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", tripCounterIncomplete);
	    otherInformationMap.put("Number of (complete) trips", tripCounter);
	 
	    
	    // write results to files
	    new File(outputDirectory).mkdir();
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime, tripCounter, averageTripDuration);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", tripCounter);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance, tripCounter, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed, tripCounterSpeed, averageOfAverageTripSpeedsRouted);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", tripCounter);
	}


//	private static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue, int binWidth, int limitOfLastBin) {
//		double inputValueBin = inputValue / binWidth;
//		int floorOfLastBin = limitOfLastBin / binWidth;
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
//	}

	
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