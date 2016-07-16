package playground.dziemke.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 * 
 */
public class NetworkAnalyzer {
	public static void main(String[] args) {
	    // Input file
	    String networkFile = "D:/Workspace/container/demand/input/iv_counts/network.xml";  
	   
	    
	    // Get network, which is needed to calculate distances
	    Config config = ConfigUtils.createConfig();
	    Scenario scenario = ScenarioUtils.createScenario(config);
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	    networkReader.readFile(networkFile);
	    Network network = scenario.getNetwork();
	    
	    
	    // Create needed objects
	    int numberOfNodes = 0;
	    int numberOfLinks = 0;
	    
	    Map<Double, Integer> capacityMap = new HashMap<Double, Integer>();
	    
	    int capacity0To1000 = 0;
	    int capacity1000To2000 = 0;
	    int capacity2000To3000 = 0;
	    int capacity3000To4000 = 0;
	    int capacity4000To5000 = 0;
	    int capacity5000ToInf = 0;
	    	    
	    Map<Double, Integer> freeSpeedMap = new HashMap<Double, Integer>();
	    Map<Double, Integer> numberOfLanesMap = new HashMap<Double, Integer>();
	    
	    int linksWithMoreThanOneMode = 0;
	    
	    double maxLength = Integer.MIN_VALUE;
	    double minLength = Integer.MAX_VALUE;
	    
	    
	    // Do calculations
	    for (Node node : network.getNodes().values()) {
	    	numberOfNodes++;
	    }
	     
	    
	    for (Link link : network.getLinks().values()) {
	    	numberOfLinks++;
	    	
	    	double capacity = link.getCapacity();
	    	if (!capacityMap.containsKey(capacity)) {
	    		capacityMap.put(capacity, 1);
	    	} else {
	    		int quantityCapacity = capacityMap.get(capacity);
	    		capacityMap.put(capacity, quantityCapacity + 1);
	    	}
	    	if (capacity < 1000) {capacity0To1000++;}
	    	else if (capacity >= 1000 && capacity < 2000) {capacity1000To2000++;}
	    	else if (capacity >= 2000 && capacity < 3000) {capacity2000To3000++;}
	    	else if (capacity >= 3000 && capacity < 4000) {capacity3000To4000++;}
	    	else if (capacity >= 4000 && capacity < 5000) {capacity4000To5000++;}
	    	else if (capacity >= 5000) {capacity5000ToInf++;}
	    	else {System.err.println("Error!");}
	    	
	    	double freeSpeed = link.getFreespeed();
	    	if (!freeSpeedMap.containsKey(freeSpeed)) {
	    		freeSpeedMap.put(freeSpeed, 1);
	    	} else {
	    		int quantityFreeSpeed = freeSpeedMap.get(freeSpeed);
	    		freeSpeedMap.put(freeSpeed, quantityFreeSpeed + 1);
	    	}
	    	
	    	double numberOfLanes = link.getNumberOfLanes();
	    	if (!numberOfLanesMap.containsKey(numberOfLanes)) {
	    		numberOfLanesMap.put(numberOfLanes, 1);
	    	} else {
	    		int quantityNumberOfLanes = numberOfLanesMap.get(numberOfLanes);
	    		numberOfLanesMap.put(numberOfLanes, quantityNumberOfLanes + 1);
	    	}
	    		    	
	    	if (link.getAllowedModes().size() > 1) {
	    		linksWithMoreThanOneMode++;
	    	}
	    	
	    	double length = link.getLength();
	    	if (length > maxLength) {
	    		maxLength = length;
	    	}
	    	if (length < minLength) {
	    		minLength = length;
	    	}
	    }
	    
	    
	    // Write information to console
	    System.out.println("Number of Nodes: " + numberOfNodes);
	    System.out.println("Number of Links: " + numberOfLinks);
	    
	    for (Double capacity : capacityMap.keySet()) {
	    	System.out.println(capacityMap.get(capacity) + " links have a capacity of " + capacity + ".");
	    }
	    
	    System.out.println(capacity0To1000 + " links have a capacity of less than 1000.");
	    System.out.println(capacity1000To2000 + " links have a capacity of at least 1000 and less than 2000.");
	    System.out.println(capacity2000To3000 + " links have a capacity of at least 2000 and less than 3000.");
	    System.out.println(capacity3000To4000 + " links have a capacity of at least 3000 and less than 4000.");
	    System.out.println(capacity4000To5000 + " links have a capacity of at least 4000 and less than 5000.");
	    System.out.println(capacity5000ToInf + " links have a capacity of at least 5000.");
	    
	    for (Double freeSpeed : freeSpeedMap.keySet()) {
	    	System.out.println(freeSpeedMap.get(freeSpeed) + " links have a free speed of " + freeSpeed + " / " + freeSpeed * 3.6 + ".");
	    }
	    
	    for (Double numberOfLanes : numberOfLanesMap.keySet()) {
	    	System.out.println(numberOfLanesMap.get(numberOfLanes) + " links have " + numberOfLanes + " lanes.");
	    }
    
	    System.out.println(linksWithMoreThanOneMode + " links allow more than one mode.");
	    
	    System.out.println("Maximum link length is: " + maxLength);
	    System.out.println("Minimum link length is: " + minLength);  
	}
}