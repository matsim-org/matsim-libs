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
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author Ihab
 *
 */
public class ParkAndRideFactory {

	private boolean useInputFile = false;
	private String filterType = null;
	private ScenarioImpl scenario;
	private double extensionRadius;
	private int maxSearchSteps;
	private String outputPath;
	private SortedMap<Id, PRCarLinkToNode> id2prCarLinkToNode = new TreeMap<Id, PRCarLinkToNode>();
	private TextFileWriter writer = new TextFileWriter();

	public ParkAndRideFactory(ScenarioImpl scenario, double extensionRadius, int maxSearchSteps, String outputPath) {
		this.scenario = scenario;
		this.extensionRadius = extensionRadius;
		this.maxSearchSteps = maxSearchSteps;
		this.outputPath = outputPath;
	}

	public void loadPrInputData(String prInputData) {

		// read in TextFile ...
		
	}

	public void setUseInputFile(boolean b, String prInputData) {
		this.useInputFile = b;
	}

	public void setFilterType(String filterType) {
		this.filterType = filterType;
	}

	public void setId2prCarLinkToNode() {
		
		if (this.useInputFile == true){ // adds park-and-ride facilities at the closest carLink of the given coordinates
			
			// ...
			
		} else if (this.filterType.equals("berlin")) { // filters transit stops by name
			Map<Id, TransitStopFacility> id2transitStopFacility = getFilteredTransitStops(); 
			this.setId2prCarLinkToNode(id2transitStopFacility);
			
		} else if (this.filterType.equals("allTransitStops")) { // adds park-and-ride facilities to all transit stops
			Map<Id, TransitStopFacility> id2transitStopFacility = this.scenario.getTransitSchedule().getFacilities();
			this.setId2prCarLinkToNode(id2transitStopFacility);
		}
	}
		
	private Map<Id, TransitStopFacility> getFilteredTransitStops() {
		Map<Id, TransitStopFacility> id2transitStops = new HashMap<Id, TransitStopFacility>();	
		List<TransitStopFacility> stopsfilteredOut = new ArrayList<TransitStopFacility>();

		for (TransitStopFacility stop : this.scenario.getTransitSchedule().getFacilities().values()){
			
			String name = null;
			if (stop.getName() == null){
				name = stop.getId().toString();
			} else {
				name = stop.getName().toString();
			}
			System.out.println("__________________________________________________________");
			System.out.println("TransitStop " + stop.getId() + " / " + name + " / "+ stop.getCoord());
			
			// ---------------------------------------------------
//			double xMax = 4598530.0; // Warschauer Straße
//			double xMin = 4588834.0418; // Richard-Wagner-Platz
//			double yMax = 5823013.0; // Nordbahnhof
//			double yMin = 5817773.842; // Platz der Luftbrücke
//			if (stop.getCoord().getX() <= xMax && stop.getCoord().getX() >= xMin && stop.getCoord().getY() <= yMax && stop.getCoord().getY() >= yMin) {
//				// within rectangle
//				System.out.println("TransitStop is within rectangle. --> Not creating Park'n'Ride Facility.");
//			}
			// ---------------------------------------------------
			
			if (name.contains("B ") || name.contains("T-Hst ") || name.contains("N ") || name.contains("T-Est ") || name.contains("T+B ") ) {
				// Transit Stop seems to be a bus or tram stop
				stopsfilteredOut.add(stop);
				System.out.println("Transit Stop name contains B, T-Hst, N, T-Est or T+B --> no Park'n'Ride facility created.");
			}
			
			else {
				if (name.contains("U ") || name.contains("S ") || name.contains("Berlin ") || name.contains("Berlin-")){
					id2transitStops.put(stop.getId(), stop);
					System.out.println("Creating Park'n'Ride Facility for TransitStop " + stop.getId() + "...");
				} else {
					System.out.println("Transit Stop name doesn't contain S, U, Berlin or Berlin-");
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

	private void setId2prCarLinkToNode(Map<Id, TransitStopFacility> id2transitStopFacility) {
		
		Map<TransitStopFacility, Link> transitStop2nearestCarLink = new HashMap<TransitStopFacility, Link>();
		List<TransitStopFacility> stopsWithoutPRFacility = new ArrayList<TransitStopFacility>();

		boolean hasPRnode = false;
		
		for (TransitStopFacility stop : id2transitStopFacility.values()){
			
			String name = null;
			if (stop.getName() == null){
				name = stop.getId().toString();
			} else {
				name = stop.getName().toString();
			}
			System.out.println("__________________________________________________________");
			System.out.println("TransitStop " + stop.getId() + " / " + name + " / "+ stop.getCoord());
			
			System.out.println("Creating Park'n'Ride Facility for TransitStop " + stop.getId() + "...");
		
			if (stop.getLinkId() != null){
				Id stopLinkId = stop.getLinkId();
				Link stopLink = scenario.getNetwork().getLinks().get(stopLinkId);
		
				if (stopLink.getAllowedModes().contains(TransportMode.car)){
					transitStop2nearestCarLink.put(stop, stopLink);
					System.out.println("TransitStop " + stop.getId() + " is on car-Link " + stopLink.getId().toString() + " (" + stopLink.getToNode().getCoord().toString() + ").");
				
					if (this.id2prCarLinkToNode.get(stopLink.getToNode().getId()) == null){
						PRCarLinkToNode prCarLinkToNode = new PRCarLinkToNode();
						prCarLinkToNode.setNodeId(stopLink.getToNode().getId());
						prCarLinkToNode.setNode(stopLink.getToNode());
						prCarLinkToNode.setStopName(name);
						this.id2prCarLinkToNode.put(stopLink.getToNode().getId(), prCarLinkToNode);
						hasPRnode = true;
					}
				}
			}
			
			if (!hasPRnode) {
				System.out.println("TransitStopFacility " + stop.getId() + " is not on a car-Link.");
				System.out.println("Searching for closest car-Link");
				double searchRadius = 0; 
				for (int n = 0; n <= maxSearchSteps; n++){
					if (!hasPRnode){
						searchRadius = searchRadius + extensionRadius;
						for (Link link : scenario.getNetwork().getLinks().values()){
							if (!hasPRnode){
								if (link.getAllowedModes().contains(TransportMode.car)){
									double linkToNodeX = link.getToNode().getCoord().getX();
									double linkToNodeY = link.getToNode().getCoord().getY();
									double xDiff = Math.abs(linkToNodeX - stop.getCoord().getX());
									double yDiff = Math.abs(linkToNodeY - stop.getCoord().getY());
									double hyp = Math.sqrt(Math.pow(xDiff, 2)+Math.pow(yDiff, 2));
									if (hyp <= searchRadius){
										System.out.println("Der Link " + link.getId() + " ("+link.getToNode().getCoord()+") liegt im Suchradius um TransitStop " + stop.getId() + ".");
										transitStop2nearestCarLink.put(stop, link);
										if (this.id2prCarLinkToNode.get(link.getToNode().getId()) == null){
											PRCarLinkToNode prCarLinkToNode = new PRCarLinkToNode();
											prCarLinkToNode.setNodeId(link.getToNode().getId());
											prCarLinkToNode.setNode(link.getToNode());
											prCarLinkToNode.setStopName(name);
											this.id2prCarLinkToNode.put(link.getToNode().getId(), prCarLinkToNode);
											hasPRnode = true;
										}
										hasPRnode = true;
									}
									else {
										System.out.print(".");
//										System.out.println("Der Link "+link.getId()+" liegt NICHT im Suchradius um die TransitStopCoordinates "+coord.toString()+".");
									}
								}
							}
						}
					}
							
					if (n==maxSearchSteps && !hasPRnode){
						System.out.println("No car-Link found! Either increase max. search steps or extension radius!");
						stopsWithoutPRFacility.add(stop);
					}
				} 						
			} else {
				// has already PR facility
			}
		}
		
		if (stopsWithoutPRFacility.isEmpty()){
			System.out.println("For all TransitStopFacilities a car-Link was found.");
		}
		
		String path = this.outputPath + "/infoFiles/";
		File directory = new File(path);
		directory.mkdirs();
		this.writer.writeInfo(stopsWithoutPRFacility, path + "info_stops_noCarLinkFound.txt");	
	}

	public Map<Id, PRCarLinkToNode> getId2prCarLinkToNode() {
		return this.id2prCarLinkToNode;
	}
}
