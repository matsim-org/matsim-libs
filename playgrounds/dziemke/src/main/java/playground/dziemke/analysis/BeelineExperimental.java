package playground.dziemke.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * Older version of analysis.TripAnalyzer.java
 * Kept since you can compare different versions of calculating beelines with this
 */
public class BeelineExperimental {
	public static void main(String[] args) {
	    String runId = "run_104";
		String eventsFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it.150/" + runId + ".150.events.xml.gz";
		String configFile = "D:/Workspace/container/demand/output/" + runId + "/" + runId + ".output_config.xml.gz";
		String outputFileBase = "D:/Workspace/container/demand/output/" + runId + "/beeline/";
		
		
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
	    Config config = ConfigUtils.loadConfig(configFile);
    	Scenario scenario = ScenarioUtils.loadScenario(config);
    	Network network = scenario.getNetwork();
      	
    	
    	// create variables
    	int tripCounter = 0;
		
		Map <Integer, Integer> tripDistanceRoutedMap = new TreeMap <Integer, Integer>();
		double aggregateTripDistanceRouted = 0.;
		
		Map <Integer, Integer> tripDistanceBeelineFromNodeMap = new TreeMap <Integer, Integer>();
		double aggregateTripDistanceBeelineFromNode = 0.;
		
		Map <Integer, Integer> tripDistanceBeelineToNodeMap = new TreeMap <Integer, Integer>();
		double aggregateTripDistanceBeelineToNode = 0.;
		
		Map <Integer, Integer> tripDistanceBeelineCenterMap = new TreeMap <Integer, Integer>();
		double aggregateTripDistanceBeelineCenter = 0.;
		
		Map <Integer, Integer> tripDistanceBeelineLinkMap = new TreeMap <Integer, Integer>();
		double aggregateTripDistanceBeelineLink = 0.;

	    Map <Id, Double> distanceRoutedMap = new TreeMap <Id, Double>();
	    Map <Id, Double> distanceFromNodeMap = new TreeMap <Id, Double>();
	    Map <Id, Double> distanceToNodeMap = new TreeMap <Id, Double>();
	    Map <Id, Double> distanceCenterMap = new TreeMap <Id, Double>();
	    Map <Id, Double> distanceLinkMap = new TreeMap <Id, Double>();
	  
	    
	    // do calculations
	    for (Trip trip : handler.getTrips().values()) {
	    	if(trip.getTripComplete()) {
	    		tripCounter++;
				
				// calculate routed distances and and store them in a map
				double tripDistanceMeter = 0.;
				for (int i = 0; i < trip.getLinks().size(); i++) {
					Id linkId = trip.getLinks().get(i);
					Link link = network.getLinks().get(linkId);
					double length = link.getLength();
					tripDistanceMeter = tripDistanceMeter + length;
				}
				double tripDistanceRouted = tripDistanceMeter / 1000.;
				addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted);
	    		aggregateTripDistanceRouted = aggregateTripDistanceRouted + tripDistanceRouted;
	    		distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);
	    		
	    		// calculate different beeline distances and store them in maps
	    		Link departureLink = network.getLinks().get(trip.getDepartureLinkId());
	    		Link arrivalLink = network.getLinks().get(trip.getArrivalLinkId());
	    		
	    		Coord departureCoordFromNode = departureLink.getFromNode().getCoord();
	    		Coord departureCoordToNode = departureLink.getToNode().getCoord();
	    		Coord arrivalCoordFromNode = arrivalLink.getFromNode().getCoord();
	    		Coord arrivalCoordToNode = arrivalLink.getToNode().getCoord();
	    		
	    		double departureCoordXFromNode = departureCoordFromNode.getX();
	    		double departureCoordYFromNode = departureCoordFromNode.getY();
	    		double departureCoordXToNode = departureCoordToNode.getX();
	    		double departureCoordYToNode = departureCoordToNode.getY();
	    		double departureCoordXCenter = (departureCoordXFromNode + departureCoordXToNode) / 2;
	    		double departureCoordYCenter = (departureCoordYFromNode + departureCoordYToNode) / 2;
	    		double departureCoordXLink = departureLink.getCoord().getX();
	    		double departureCoordYLink = departureLink.getCoord().getY();
	    		double arrivalCoordXFromNode = arrivalCoordFromNode.getX();
	    		double arrivalCoordYFromNode = arrivalCoordFromNode.getY();
	    		double arrivalCoordXToNode = arrivalCoordToNode.getX();
	    		double arrivalCoordYToNode = arrivalCoordToNode.getY();
	    		double arrivalCoordXCenter = (arrivalCoordXFromNode + arrivalCoordXToNode) / 2;
	    		double arrivalCoordYCenter = (arrivalCoordYFromNode + arrivalCoordYToNode) / 2;
	    		double arrivalCoordXLink = arrivalLink.getCoord().getX();
	    		double arrivalCoordYLink = arrivalLink.getCoord().getY();
		    	
	    		double horizontalDistanceFromNode = (Math.abs(departureCoordXFromNode - arrivalCoordXFromNode)) / 1000;
	    		double verticalDistanceFromNode = (Math.abs(departureCoordYFromNode - arrivalCoordYFromNode)) / 1000;
	    		double horizontalDistanceToNode = (Math.abs(departureCoordXToNode - arrivalCoordXToNode)) / 1000;
	    		double verticalDistanceToNode = (Math.abs(departureCoordYToNode - arrivalCoordYToNode)) / 1000;
	    		double horizontalDistanceCenter = (Math.abs(departureCoordXCenter - arrivalCoordXCenter)) / 1000;
	    		double verticalDistanceCenter = (Math.abs(departureCoordYCenter - arrivalCoordYCenter)) / 1000;
	    		double horizontalDistanceLink = (Math.abs(departureCoordXLink - arrivalCoordXLink)) / 1000;
	    		double verticalDistanceLink = (Math.abs(departureCoordYLink - arrivalCoordYLink)) / 1000;
	    		
	    		double distanceFromNode = Math.sqrt(horizontalDistanceFromNode * horizontalDistanceFromNode
	    				+ verticalDistanceFromNode * verticalDistanceFromNode);
	    		double distanceToNode = Math.sqrt(horizontalDistanceToNode * horizontalDistanceToNode
	    				+ verticalDistanceToNode * verticalDistanceToNode);
	    		double distanceCenter = Math.sqrt(horizontalDistanceCenter * horizontalDistanceCenter
	    				+ verticalDistanceCenter * verticalDistanceCenter);
	    		double distanceLink = Math.sqrt(horizontalDistanceLink * horizontalDistanceLink	+ verticalDistanceLink * verticalDistanceLink);
	    		
	    		addToMapIntegerKey(tripDistanceBeelineFromNodeMap, distanceFromNode);
	    		aggregateTripDistanceBeelineFromNode = aggregateTripDistanceBeelineFromNode + distanceFromNode;
	    		addToMapIntegerKey(tripDistanceBeelineToNodeMap, distanceToNode);
	    		aggregateTripDistanceBeelineToNode = aggregateTripDistanceBeelineToNode + distanceToNode;
	    		addToMapIntegerKey(tripDistanceBeelineCenterMap, distanceCenter);
	    		aggregateTripDistanceBeelineCenter = aggregateTripDistanceBeelineCenter + distanceCenter;
	    		addToMapIntegerKey(tripDistanceBeelineLinkMap, distanceLink);
	    		aggregateTripDistanceBeelineLink = aggregateTripDistanceBeelineLink + distanceLink;
	    		
	    		distanceFromNodeMap.put(trip.getTripId(), distanceFromNode);
	    		distanceToNodeMap.put(trip.getTripId(), distanceToNode);
	    		distanceCenterMap.put(trip.getTripId(), distanceCenter);
	    		distanceLinkMap.put(trip.getTripId(), distanceLink);
			} else {
	    		System.err.println("Trip is not complete!");
	    	}
	    }
	    
//	    double averageTime = aggregateTime / tripCounter;
	    double averageTripDistanceRouted = aggregateTripDistanceRouted / tripCounter;
	    double averageTripDistanceBeelineFromNode = aggregateTripDistanceBeelineFromNode / tripCounter;
	    double averageTripDistanceBeelineToNode = aggregateTripDistanceBeelineToNode / tripCounter;
	    double averageTripDistanceBeelineCenter = aggregateTripDistanceBeelineCenter / tripCounter;
	    double averageTripDistanceBeelineLink = aggregateTripDistanceBeelineLink / tripCounter;
//	    double averageOfAverageTripSpeeds = aggregateOfAverageTripSpeeds / tripCounter;
	    
	    
	    // write results to files
	    writeToFileIntegerKey(tripDistanceRoutedMap, outputFileBase + "tripDistanceRouted.txt", tripCounter, averageTripDistanceRouted);
	    writeToFileIntegerKey(tripDistanceBeelineFromNodeMap, outputFileBase + "tripDistanceBeelineFromNode.txt", tripCounter, averageTripDistanceBeelineFromNode);
	    writeToFileIntegerKey(tripDistanceBeelineToNodeMap, outputFileBase + "tripDistanceBeelineToNode.txt", tripCounter, averageTripDistanceBeelineToNode);
	    writeToFileIntegerKey(tripDistanceBeelineCenterMap, outputFileBase + "tripDistanceBeelineCenter.txt", tripCounter, averageTripDistanceBeelineCenter);
	    writeToFileIntegerKey(tripDistanceBeelineLinkMap, outputFileBase + "tripDistanceBeelineLink.txt", tripCounter, averageTripDistanceBeelineLink);

	    writeToFileIdKey(distanceRoutedMap, distanceFromNodeMap, distanceToNodeMap, distanceCenterMap, distanceLinkMap, 
	    		outputFileBase + "beeline.txt", tripCounter);

	    
    	// return number of trips that have no previous activity
	    System.out.println("Number of trips that have no previous activity is: " + handler.getNoPreviousEndOfActivityCounter());
	    
	    
	    // return number of trips that have no calculable speed
//	    System.out.println("Number of trips that have no calculabkle speed is: " + numberOfTripsWithNoCalculableSpeed);
	}


	private static void addToMapIntegerKey(Map <Integer, Integer> map, double inputValue) {
		double inputValueDividedByFive = inputValue / 5;
		// Math.floor returns next lower integer number (but as a double value)
		int lowerEndOfBin = (int)Math.floor(inputValueDividedByFive);
		if (lowerEndOfBin < 0) {
			System.err.println("Lower end of bin may not be smaller than zero!");
		}
		
		if (lowerEndOfBin >= 24) {
			lowerEndOfBin = 24;
		}
		
		if (!map.containsKey(lowerEndOfBin)) {
			map.put(lowerEndOfBin, 1);
		} else {
			int value = map.get(lowerEndOfBin);
			value++;
			map.put(lowerEndOfBin, value);
		}
	}
	
	
	private static void writeToFileIntegerKey(Map<Integer, Integer> map, String outputFile, int tripCounter, double average) {
		BufferedWriter bufferedWriter = null;
		
		try {
            File output = new File(outputFile);
    		FileWriter fileWriter = new FileWriter(output);
    		bufferedWriter = new BufferedWriter(fileWriter);
    		
    		int writeCounter = 0;
    		
    		for (int key : map.keySet()) {
    			int binCaption = key * 5;
    			int value = map.get(key);
    			bufferedWriter.write(binCaption + "+" + "\t" + value);
    			writeCounter = writeCounter + value;
    			bufferedWriter.newLine();
    		}
    		bufferedWriter.write("Average = " + "\t" + average);
			bufferedWriter.newLine();
			bufferedWriter.write("Sum = " + "\t" + writeCounter);
    		
    		if (writeCounter != tripCounter) {
    			System.err.println("Number of trips in " + outputFile + " is not equal to number of all trips!");
    		}
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		System.out.println("Analysis file " + outputFile + " written.");
	}
	
	
	private static void writeToFileIdKey(Map<Id, Double> mapRouted, Map<Id, Double> mapFrom, Map<Id, Double> mapTo,
			Map<Id, Double> mapCenter, Map<Id, Double> map, String outputFile, int tripCounter) {
		BufferedWriter bufferedWriter = null;
		
		try {
	        File output = new File(outputFile);
			FileWriter fileWriter = new FileWriter(output);
			bufferedWriter = new BufferedWriter(fileWriter);
		
			int mapEntryCounter = 0;
			int counterFromNode = 0;
			int counterToNode = 0;
			int counterCenter = 0;
			int counterLink = 0;
			double minDistanceRouted = Double.POSITIVE_INFINITY;
			double maxDistanceRouted = Double.NEGATIVE_INFINITY;
			double minDistanceBeelineFromNode = Double.POSITIVE_INFINITY;
			double maxDistanceBeelineFromNode = Double.NEGATIVE_INFINITY;
			double minDistanceBeelineToNode = Double.POSITIVE_INFINITY;
			double maxDistanceBeelineToNode = Double.NEGATIVE_INFINITY;
			double minDistanceBeelineCenter = Double.POSITIVE_INFINITY;
			double maxDistanceBeelineCenter = Double.NEGATIVE_INFINITY;
			double minDistanceBeelineLink = Double.POSITIVE_INFINITY;
			double maxDistanceBeelineLink = Double.NEGATIVE_INFINITY;
			
			double minRatioBeelineFromNode = Double.POSITIVE_INFINITY;
			double maxRatioBeelineFromNode = Double.NEGATIVE_INFINITY;
			double aggregateRatioRoutedBeelineFromNode = 0.;
			double minRatioBeelineToNode = Double.POSITIVE_INFINITY;
			double maxRatioBeelineToNode = Double.NEGATIVE_INFINITY;
			double aggregateRatioRoutedBeelineToNode = 0.;
			double minRatioBeelineCenter = Double.POSITIVE_INFINITY;
			double maxRatioBeelineCenter = Double.NEGATIVE_INFINITY;
			double aggregateRatioRoutedBeelineCenter = 0.;
			double minRatioBeelineLink = Double.POSITIVE_INFINITY;
			double maxRatioBeelineLink = Double.NEGATIVE_INFINITY;
			double aggregateRatioRoutedBeelineLink = 0.;
    		
    		for (Id tripId : mapRouted.keySet()) {
    			double distanceRouted = mapRouted.get(tripId);
    			double distanceBeelineFromNode = mapFrom.get(tripId);
    			double distanceBeelineToNode = mapTo.get(tripId);
    			double distanceBeelineCenter = mapCenter.get(tripId);
    			double distanceBeelineLink = map.get(tripId);
    			
    			double ratioRoutedBeelineFromNode = distanceRouted / distanceBeelineFromNode;
    			double ratioRoutedBeelineToNode = distanceRouted / distanceBeelineToNode;
    			double ratioRoutedBeelineCenter = distanceRouted / distanceBeelineCenter;
    			double ratioRoutedBeelineLink = distanceRouted / distanceBeelineLink;
    			    			
    			bufferedWriter.write(tripId + "\t" + distanceRouted + "\t" + distanceBeelineFromNode + "\t" + ratioRoutedBeelineFromNode + "\t"
    					+ distanceBeelineToNode + "\t" + ratioRoutedBeelineToNode + "\t" + distanceBeelineCenter + "\t" + ratioRoutedBeelineCenter + "\t"
    					+ distanceBeelineLink + "\t" + ratioRoutedBeelineLink);
    			mapEntryCounter++;
    			bufferedWriter.newLine();
    			
    			if (distanceRouted < minDistanceRouted) { minDistanceRouted = distanceRouted; }
    			if (distanceRouted > maxDistanceRouted) { maxDistanceRouted = distanceRouted; }
    			if (distanceBeelineFromNode < minDistanceBeelineFromNode) { minDistanceBeelineFromNode = distanceBeelineFromNode; }
    			if (distanceBeelineFromNode > maxDistanceBeelineFromNode) { maxDistanceBeelineFromNode = distanceBeelineFromNode; }
    			if (distanceBeelineToNode < minDistanceBeelineToNode) { minDistanceBeelineToNode = distanceBeelineToNode; }
    			if (distanceBeelineToNode > maxDistanceBeelineToNode) { maxDistanceBeelineToNode = distanceBeelineToNode; }
    			if (distanceBeelineCenter < minDistanceBeelineCenter) { minDistanceBeelineCenter = distanceBeelineCenter; }
    			if (distanceBeelineCenter > maxDistanceBeelineCenter) { maxDistanceBeelineCenter = distanceBeelineCenter; }
    			if (distanceBeelineLink < minDistanceBeelineLink) { minDistanceBeelineLink = distanceBeelineLink; }
    			if (distanceBeelineLink > maxDistanceBeelineLink) { maxDistanceBeelineLink = distanceBeelineLink; }
    			
    			if (distanceBeelineFromNode > 1 && distanceRouted > 1) {
    				aggregateRatioRoutedBeelineFromNode = aggregateRatioRoutedBeelineFromNode + ratioRoutedBeelineFromNode;
    				counterFromNode++;
    				if (ratioRoutedBeelineFromNode < minRatioBeelineFromNode) { minRatioBeelineFromNode = ratioRoutedBeelineFromNode; }
    				if (ratioRoutedBeelineFromNode > maxRatioBeelineFromNode) { maxRatioBeelineFromNode = ratioRoutedBeelineFromNode; }
    			}
    			
    			if (distanceBeelineToNode > 1 && distanceRouted > 1) {
    				aggregateRatioRoutedBeelineToNode = aggregateRatioRoutedBeelineToNode + ratioRoutedBeelineToNode;
    				counterToNode++;
    				if (ratioRoutedBeelineToNode < minRatioBeelineToNode) { minRatioBeelineToNode = ratioRoutedBeelineToNode; }
    				if (ratioRoutedBeelineToNode > maxRatioBeelineToNode) { maxRatioBeelineToNode = ratioRoutedBeelineToNode; }
    			}
    			
    			if (distanceBeelineCenter > 1 && distanceRouted > 1) {
    				aggregateRatioRoutedBeelineCenter = aggregateRatioRoutedBeelineCenter + ratioRoutedBeelineCenter;
    				counterCenter++;
    				if (ratioRoutedBeelineCenter < minRatioBeelineCenter) { minRatioBeelineCenter = ratioRoutedBeelineCenter; }
    				if (ratioRoutedBeelineCenter > maxRatioBeelineCenter) { maxRatioBeelineCenter = ratioRoutedBeelineCenter; }
    			}
    			
    			if (distanceBeelineLink > 1 && distanceRouted > 1) {
    				aggregateRatioRoutedBeelineLink = aggregateRatioRoutedBeelineLink + ratioRoutedBeelineLink;
    				counterLink++;
    				if (ratioRoutedBeelineLink < minRatioBeelineLink) { minRatioBeelineLink = ratioRoutedBeelineLink; }
    				if (ratioRoutedBeelineLink > maxRatioBeelineLink) { maxRatioBeelineLink = ratioRoutedBeelineLink; }
    			}
     		}
    		bufferedWriter.write("Number of map entries = " + "\t" + mapEntryCounter);
    		
    		if (mapEntryCounter != tripCounter) {
    			System.err.println("Number of map entries in " + outputFile + " is not equal to number of trips!");
    		}
    		
    		double averageRatioRoutedBeelineFromNode = aggregateRatioRoutedBeelineFromNode /counterFromNode;
    		double averageRatioRoutedBeelineToNode = aggregateRatioRoutedBeelineToNode / counterToNode;
    		double averageRatioRoutedBeelineCenter = aggregateRatioRoutedBeelineCenter / counterCenter;
    		double averageRatioRoutedBeelineLink = aggregateRatioRoutedBeelineLink / counterLink;
    		
    		System.out.println("Minimum routed distance is = " + minDistanceRouted);
    		System.out.println("Maximum routed distance is = " + maxDistanceRouted);
    		System.out.println("Minimum beeline distance (based on from nodes) is = " + minDistanceBeelineFromNode);
    		System.out.println("Maximum beeline distance (based on from nodes) is = " + maxDistanceBeelineFromNode);
    		System.out.println("Minimum beeline distance (based on to nodes) is = " + minDistanceBeelineToNode);
    		System.out.println("Maximum beeline distance (based on to nodes) is = " + maxDistanceBeelineToNode);
    		System.out.println("Minimum beeline distance (based on link center) is = " + minDistanceBeelineCenter);
    		System.out.println("Maximum beeline distance (based on link center) is = " + maxDistanceBeelineCenter);
    		System.out.println("Minimum beeline distance (based on link) is = " + minDistanceBeelineLink);
    		System.out.println("Maximum beeline distance (based on link) is = " + maxDistanceBeelineLink);
    		System.out.println("Minimum ratio routed/beeline distance (based on from nodes) is = " + minRatioBeelineFromNode);
    		System.out.println("Average ratio routed/beeline distance (based on from nodes) is = " + averageRatioRoutedBeelineFromNode);
    		System.out.println("Maximum ratio routed/beeline distance (based on from nodes) is = " + maxRatioBeelineFromNode);
    		System.out.println("Minimum ratio routed/beeline distance (based on to nodes) is = " + minRatioBeelineToNode);
    		System.out.println("Average ratio routed/beeline distance (based on to nodes) is = " + averageRatioRoutedBeelineToNode);
    		System.out.println("Maximum ratio routed/beeline distance (based on to nodes) is = " + maxRatioBeelineToNode);
    		System.out.println("Minimum ratio routed/beeline distance (based on link center) is = " + minRatioBeelineCenter);
    		System.out.println("Average ratio routed/beeline distance (based on link center) is = " + averageRatioRoutedBeelineCenter);
    		System.out.println("Maximum ratio routed/beeline distance (based on link center) is = " + maxRatioBeelineCenter);
    		System.out.println("Minimum ratio routed/beeline distance (based on link) is = " + minRatioBeelineLink);
    		System.out.println("Average ratio routed/beeline distance (based on link) is = " + averageRatioRoutedBeelineLink);
    		System.out.println("Maximum ratio routed/beeline distance (based on link) is = " + maxRatioBeelineLink);
	    } catch (FileNotFoundException ex) {
	        ex.printStackTrace();
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        try {
	            if (bufferedWriter != null) {
	                bufferedWriter.flush();
	                bufferedWriter.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
		System.out.println("Analysis file " + outputFile + " written.");
	}
}