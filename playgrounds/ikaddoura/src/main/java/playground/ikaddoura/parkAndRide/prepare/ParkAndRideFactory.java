/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.ikaddoura.parkAndRide.prepare;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class ParkAndRideFactory {

	private static final Logger log = Logger.getLogger(ParkAndRideFactory.class);
	private TextFileWriter writer = new TextFileWriter();
	private Map<Id, Node> id2nextCarLinkToNode = new HashMap<Id, Node>();
	private List<Id> insertedIDs = new ArrayList<Id>();
	
	private ScenarioImpl scenario;
	private double extensionRadius;
	private int maxSearchSteps;
	private String outputPath;
	
	private boolean useInputFile = false;
	private String prInputDataFile;
	private Map<Id<ParkAndRideFacility>, PRInputData> id2prInputData = new HashMap<>();

	private boolean useScheduleFile = false;
	private int constantPRcapacity;
	private String filterType = null;

	public ParkAndRideFactory(ScenarioImpl scenario, double extensionRadius, int maxSearchSteps, String outputPath) {
		this.scenario = scenario;
		this.extensionRadius = extensionRadius;
		this.maxSearchSteps = maxSearchSteps;
		this.outputPath = outputPath;
	}

	public void setUseInputFile(boolean b, String prInputData) {
		this.useInputFile = b;
		this.prInputDataFile = prInputData;
	}

	public void setUseScheduleFile(boolean b, String filterType, int constantPRcapacity) {
		this.useScheduleFile = b;
		this.filterType = filterType;
		this.constantPRcapacity = constantPRcapacity;
	}

	public void setId2prCarLinkToNode() {
		
		if (this.useInputFile == true){
			log.info("Using the input file to create park-and-ride facilities...");

			PRInputDataReader reader = new PRInputDataReader();
			this.id2prInputData = reader.getId2prInputData(this.prInputDataFile);
			
			this.setId2prCarLinkToNode_fromInputFile(this.id2prInputData);
			
		} 
		
		if (this.useScheduleFile == true) {
			log.info("Using the transit schedule to create park-and-ride facilities...");
			
			if (this.filterType.equals("berlin")) { // filters transit stops by name
				Map<Id<TransitStopFacility>, TransitStopFacility> id2transitStopFacility = this.getFilteredTransitStops_byName();
				this.setId2prCarLinkToNode_fromScheduleFile(id2transitStopFacility);
			
			} else if (this.filterType.equals("allTransitStops")) { // adds park-and-ride facilities to all transit stops
				Map<Id<TransitStopFacility>, TransitStopFacility> id2transitStopFacility = this.scenario.getTransitSchedule().getFacilities();
				this.setId2prCarLinkToNode_fromScheduleFile(id2transitStopFacility);
			
			} else {
				throw new RuntimeException("Filter type. "+ this.filterType + " unknown. Aborting...");
			}
		}
	}
		
	private Node getBelongingCarLink(Coord coord) {
		Node nextCarLinkToNode = null;
		boolean nodeFound = false;
		
		// returns the next carLinkToNode if coordinates belong to a carLinkToNode
    	for (Link link : this.scenario.getNetwork().getLinks().values()) {
			if (!nodeFound) {
				if (link.getAllowedModes().contains(TransportMode.car)){
					if (link.getToNode().getCoord().equals(coord)) {
						nextCarLinkToNode = link.getToNode();
						nodeFound = true;
					}
				} else {
					// not a carLink
				}
			} else {
				// next carLink already found
			}
    	}
		return nextCarLinkToNode;
	}

	private Node findNextCarLink(double searchRadius, Coord coord) {
		Node nextCarLinkToNode = null;
		boolean nodeFound = false;
//		log.info("SearchRadius: " + searchRadius + " around Coordinates " + coord.toString());
		for (Link link : scenario.getNetwork().getLinks().values()){
			if (!nodeFound){	
				if (link.getAllowedModes().contains(TransportMode.car)){
					
					double linkToNodeX = link.getToNode().getCoord().getX();
					double linkToNodeY = link.getToNode().getCoord().getY();
					double xDiff = Math.abs(linkToNodeX - coord.getX());
					double yDiff = Math.abs(linkToNodeY - coord.getY());
					double hyp = Math.sqrt(Math.pow(xDiff, 2)+Math.pow(yDiff, 2));
					
					if (hyp <= searchRadius){
						log.info("Car-Link " + link.getId() + " (toNode:"+link.getToNode().getCoord()+") is within search radius " + searchRadius + " around coordinates " + coord.toString() + ".");
						nextCarLinkToNode = link.getToNode();
						nodeFound = true;
					} else {
//						System.out.println("Der Link "+link.getId()+" liegt NICHT im Suchradius um die TransitStopCoordinates "+coord.toString()+".");
					}
				} else {
					// not a carLink
				}
			} else {
				// next carLink already found
			}
		}
		return nextCarLinkToNode;
	}

	private Map<Id<TransitStopFacility>, TransitStopFacility> getFilteredTransitStops_byName() {
		Map<Id<TransitStopFacility>, TransitStopFacility> id2transitStops = new HashMap<Id<TransitStopFacility>, TransitStopFacility>();	
		List<TransitStopFacility> stopsfilteredOut = new ArrayList<TransitStopFacility>();

		for (TransitStopFacility stop : this.scenario.getTransitSchedule().getFacilities().values()){
			
			String name = null;
			if (stop.getName() == null){
				name = stop.getId().toString();
			} else {
				name = stop.getName().toString();
			}
			log.info("TransitStop " + stop.getId() + " / " + name + " / "+ stop.getCoord());
			
			if (name.contains("B ") || name.contains("T-Hst ") || name.contains("N ") || name.contains("T-Est ") || name.contains("T+B ") ) {
				// Transit Stop seems to be a bus or tram stop
				stopsfilteredOut.add(stop);
				log.info("Transit Stop name contains B, T-Hst, N, T-Est or T+B --> no Park'n'Ride facility created.");
			} else {
				if (name.contains("U ") || name.contains("S ") || name.contains("Berlin ") || name.contains("Berlin-")){
					id2transitStops.put(stop.getId(), stop);
				} else {
					log.info("Transit Stop name doesn't contain S, U, Berlin or Berlin-");
					stopsfilteredOut.add(stop);
				}
			}
		}
				
		String path = this.outputPath + "/infoFiles/";
		File directory = new File(path);
		directory.mkdirs();
		this.writer.writeInfo(stopsfilteredOut, path + "info_stops_filteredOutByName.txt");
		
		return id2transitStops;
	}
	
	private Map<Id, TransitStopFacility> getFilteredTransitStops_byCoordinates() {
		Map<Id, TransitStopFacility> id2transitStops = new HashMap<Id, TransitStopFacility>();	
		List<TransitStopFacility> stopsfilteredOut = new ArrayList<TransitStopFacility>();

		for (TransitStopFacility stop : this.scenario.getTransitSchedule().getFacilities().values()){
			
			String name = null;
			if (stop.getName() == null){
				name = stop.getId().toString();
			} else {
				name = stop.getName().toString();
			}
			log.info("TransitStop " + stop.getId() + " / " + name + " / "+ stop.getCoord());
			
			double xMax = 4598530.0; // Warschauer Straße
			double xMin = 4588834.0418; // Richard-Wagner-Platz
			double yMax = 5823013.0; // Nordbahnhof
			double yMin = 5817773.842; // Platz der Luftbrücke
			
			if (stop.getCoord().getX() <= xMax && stop.getCoord().getX() >= xMin && stop.getCoord().getY() <= yMax && stop.getCoord().getY() >= yMin) {
				// within rectangle
				stopsfilteredOut.add(stop);
			} else {
				id2transitStops.put(stop.getId(), stop);
			}
		}
				
		String path = this.outputPath + "/infoFiles/";
		File directory = new File(path);
		directory.mkdirs();
		this.writer.writeInfo(stopsfilteredOut, path + "info_stops_filteredOutByName.txt");
		
		return id2transitStops;
	}
	
	private void setId2prCarLinkToNode_fromInputFile(Map<Id<ParkAndRideFacility>, PRInputData> id2prInputData){
				
		List<Id> idsNoNodeFound = new ArrayList<Id>();
		
        for (Id id : id2prInputData.keySet()){
        	Node nextCarLinkToNode = null;
        	boolean nodeFound = false;
        	
			nextCarLinkToNode = getBelongingCarLink(id2prInputData.get(id).getCoord());
			if (nextCarLinkToNode == null) {
				// continue and search for nextCarLink by increasing the search space
			} else {
	        	nodeFound = true;
				if (insertedIDs.contains(nextCarLinkToNode.getId())){
				// don't insert twice
				} else {
					this.id2nextCarLinkToNode.put(id, nextCarLinkToNode);
					insertedIDs.add(nextCarLinkToNode.getId());
				}
			}
        	
        	if(!nodeFound) {
        		log.info("Coordinates don't belong to a carLink. Searching for closest carLink...");
        		
        		double searchRadius = 0; 
				for (int n = 0; n <= maxSearchSteps; n++){
					if (!nodeFound){
						searchRadius = searchRadius + extensionRadius;
						nextCarLinkToNode = findNextCarLink(searchRadius, id2prInputData.get(id).getCoord());
						if (nextCarLinkToNode == null) {
							// continue and increase search space
						} else {
				        	nodeFound = true;
							if (insertedIDs.contains(nextCarLinkToNode.getId())){
							// don't insert twice
							} else {
								this.id2nextCarLinkToNode.put(id, nextCarLinkToNode);
								insertedIDs.add(nextCarLinkToNode.getId());
							}
						}
					}
					if (n == maxSearchSteps && !nodeFound) {
						log.warn("No car-Link found! Either increase max. search steps or extension radius!");
						idsNoNodeFound.add(id);
					}
				}
        	}	
        }
        
        if (idsNoNodeFound.isEmpty()){
			log.info("For all TransitStopFacilities a car-Link was found.");
		} else {
			String path = this.outputPath + "/infoFiles/";
			File directory = new File(path);
			directory.mkdirs();
			this.writer.writeInfoIDs(idsNoNodeFound, path + "info_IDs_noCarLinkFound.txt");
		}
	}

	private void setId2prCarLinkToNode_fromScheduleFile(Map<Id<TransitStopFacility>, TransitStopFacility> id2transitStopFacility) {
		
		List<TransitStopFacility> stopsWithoutPRFacility = new ArrayList<TransitStopFacility>();

		log.info("Creating park-and-ride facilities...");
		
		for (TransitStopFacility stop : id2transitStopFacility.values()){
			boolean nodeFound = false;
		
			if (stop.getId() == null || stop.getLinkId() == null){
				log.warn("Transit stop is expected to have an Id. Skipping transit stop...");
			} else {
				String name;
				if (stop.getName() == null){
					log.warn("Transit stop facility has no name.");
					name = "n.a.";
				} else {
					name = stop.getName();
				}				
				log.info("Transit stop id: " + stop.getId().toString() + " / Transit stop name: " + name);
				
				Coord coordinates = null;
				if (stop.getLinkId() == null) {
					log.info("Transit stop " + name + " has no reference link Id. Using the coordinates of the transti stop...");
					coordinates = stop.getCoord();
				} else {
					log.info("Transit stop " + name + " has a reference link Id. Using the coordinates of the toNode...");
					Id stopLinkId = stop.getLinkId();
					Link stopLink = scenario.getNetwork().getLinks().get(stopLinkId);
					coordinates = stopLink.getToNode().getCoord();
				}
				
				Node nextCarLinkToNode = null;
	        	
				nextCarLinkToNode = getBelongingCarLink(coordinates);
				if (nextCarLinkToNode == null) {
					// continue and search for nextCarLink by increasing the search space
				} else {
		        	nodeFound = true;
					if (insertedIDs.contains(stop.getId())){
					// don't insert twice for a transit stop
					} else {
						this.id2nextCarLinkToNode.put(stop.getId(), nextCarLinkToNode);
						insertedIDs.add(stop.getId());
					}
				}
	        	
	        	if(!nodeFound) {
	        		log.info("Transit stop coordinates not on a carLink. Searching for closest carLink...");
	        		
	        		double searchRadius = 0; 
					for (int n = 0; n <= maxSearchSteps; n++){
						if (!nodeFound){
							searchRadius = searchRadius + extensionRadius;
							nextCarLinkToNode = findNextCarLink(searchRadius, coordinates);
							if (nextCarLinkToNode == null) {
								// continue and increase search space
							} else {
					        	nodeFound = true;
								if (insertedIDs.contains(stop.getId())){
								// don't insert twice
								} else {
									this.id2nextCarLinkToNode.put(stop.getId(), nextCarLinkToNode);
									insertedIDs.add(stop.getId());
								}
							}
						}
						if (n == maxSearchSteps && !nodeFound) {
							log.warn("No car-Link found! Either increase max. search steps or extension radius!");
							stopsWithoutPRFacility.add(stop);
						}
					}
	        	}	 						
			}
		}
		
		if (stopsWithoutPRFacility.isEmpty()){
			log.info("For all Transit stop facilities a car-Link was found.");
		} else {
			String path = this.outputPath + "/infoFiles/";
			File directory = new File(path);
			directory.mkdirs();
			this.writer.writeInfo(stopsWithoutPRFacility, path + "info_stops_noCarLinkFound.txt");	
		}
	}

	public void createPRLinks(PRFacilityCreator prFacilityCreator) {
		if (this.useInputFile == true) {
			int i = 0;
			for (Id id : this.id2nextCarLinkToNode.keySet()){
				Id<ParkAndRideFacility> prFacilitiyId = Id.create(i, ParkAndRideFacility.class);
				prFacilityCreator.createPRFacility(prFacilitiyId, this.id2nextCarLinkToNode.get(id), this.id2prInputData.get(id).getStopName(), this.id2prInputData.get(id).getCapacity());
				i++;
			}
		}
		
		if (this.useScheduleFile == true) {
			int i = 0;
			for (Id id : this.id2nextCarLinkToNode.keySet()){
				Id<ParkAndRideFacility> prFacilitiyId = Id.create(i, ParkAndRideFacility.class);
				String name;
				if (this.scenario.getTransitSchedule().getFacilities().get(id).getName() == null) {
					name = id.toString(); // using the Id instead of the name
				} else {
					name = this.scenario.getTransitSchedule().getFacilities().get(id).getName();
				}
				prFacilityCreator.createPRFacility(prFacilitiyId, this.id2nextCarLinkToNode.get(id), name, constantPRcapacity);
				i++;
			}
		}
	}

	public void writeNetwork(String output) {
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		networkWriter.write(output);	
	}

	public void writePrFacilities(PRFacilityCreator prFacilityCreator, String output) {
		TextFileWriter writer = new TextFileWriter();	
		writer.write(prFacilityCreator.getParkAndRideFacilities(), output);
	}
}
