/* *********************************************************************** *
 * project: org.matsim.*
 * PRnodeFinder.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author Ihab
 *
 */
public class PRNodeSearch {
	private Map<TransitStopFacility, Link> transitStop2nearestCarLink = new HashMap<TransitStopFacility, Link>();

	private SortedMap<Id, PRCarLinkToNode> carLinkToNodes = new TreeMap<Id, PRCarLinkToNode>();
	private List<TransitStopFacility> stopsWithoutPRFacility = new ArrayList<TransitStopFacility>();
	private List<TransitStopFacility> stopsNotSUBerlin = new ArrayList<TransitStopFacility>();
	private final static Logger log = Logger.getLogger(PRNodeSearch.class);

	public void searchForCarLink(Scenario scenario, double extensionRadius, int maxSearchSteps) {
				
		boolean hasPRnode;
		for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()){
			
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
				this.stopsNotSUBerlin.add(stop);
				System.out.println("Transit Stop name contains B, T-Hst, N, T-Est or T+B --> no Park'n'Ride facility created.");
			}
			
			else {
				
				if (name.contains("U ") || name.contains("S ") || name.contains("Berlin ") || name.contains("Berlin-")){
					
					hasPRnode = false;
					
					System.out.println("Creating Park'n'Ride Facility for TransitStop " + stop.getId() + "...");
		
					if (stop.getLinkId() != null){
						Id stopLinkId = stop.getLinkId();
						Link stopLink = scenario.getNetwork().getLinks().get(stopLinkId);
		
						if (stopLink.getAllowedModes().contains(TransportMode.car)){
		
							this.transitStop2nearestCarLink.put(stop, stopLink);
							System.out.println("TransitStop " + stop.getId() + " is on car-Link " + stopLink.getId().toString() + " (" + stopLink.getToNode().getCoord().toString() + ").");
							if (this.carLinkToNodes.get(stopLink.getToNode().getId()) == null){
//							if (!this.carLinkToNodes.keySet().contains(stopLink.getToNode())){
								PRCarLinkToNode prCarLinkToNode = new PRCarLinkToNode();
								prCarLinkToNode.setNodeId(stopLink.getToNode().getId());
								prCarLinkToNode.setNode(stopLink.getToNode());
								prCarLinkToNode.setStopName(name);
								this.carLinkToNodes.put(stopLink.getToNode().getId(), prCarLinkToNode);
								hasPRnode = true;
							}
						}
					}
					
					if (!hasPRnode) {
						System.out.println("TransitStopFacility " + stop.getId() + " is not on a car-Link.");
						System.out.println("Searching for closest car-Link...");
						double searchRadius = 0; 
						for (int n = 0; n <= maxSearchSteps; n++){
							if (!hasPRnode){
								searchRadius = searchRadius + extensionRadius;
		//						System.out.println("Suchradius: " + searchRadius + " um die Koordinaten " + coord.toString() + ".");
		
								for (Link link : scenario.getNetwork().getLinks().values()){
									if (!hasPRnode){
										if (link.getAllowedModes().contains(TransportMode.car)){
		//									System.out.println("Der Link "+link.getId()+" ist ein car-Link.");
											double linkToNodeX = link.getToNode().getCoord().getX();
											double linkToNodeY = link.getToNode().getCoord().getY();
											double xDiff = Math.abs(linkToNodeX - stop.getCoord().getX());
											double yDiff = Math.abs(linkToNodeY - stop.getCoord().getY());
											double hyp = Math.sqrt(Math.pow(xDiff, 2)+Math.pow(yDiff, 2));
											if (hyp <= searchRadius){
												System.out.println("Der Link " + link.getId() + " ("+link.getToNode().getCoord()+") liegt im Suchradius um TransitStop " + stop.getId() + ".");
												this.transitStop2nearestCarLink.put(stop, link);
												if (this.carLinkToNodes.get(link.getToNode().getId()) == null){
													PRCarLinkToNode prCarLinkToNode = new PRCarLinkToNode();
													prCarLinkToNode.setNodeId(link.getToNode().getId());
													prCarLinkToNode.setNode(link.getToNode());
													prCarLinkToNode.setStopName(name);
													this.carLinkToNodes.put(link.getToNode().getId(), prCarLinkToNode);
													hasPRnode = true;
												}
												hasPRnode = true;
											}
											else {
		//										System.out.println("Der Link "+link.getId()+" liegt NICHT im Suchradius um die TransitStopCoordinates "+coord.toString()+".");
											}
										}
									}
								}
							}
							
							if (n==maxSearchSteps && !hasPRnode){
								log.warn("No car-Link found! Either increase max. search steps or extension radius!");
								this.stopsWithoutPRFacility.add(stop);
							}
						} // increasing search space loop ends
						
					} else {
						// has already PR facility
					}
				}
				else {
					System.out.println("Transit Stop name doesn't contain S, U, Berlin or Berlin-");
					this.stopsNotSUBerlin.add(stop);
				}
				
			}
		}
	}

	public Map<TransitStopFacility, Link> getTransitStop2nearestCarLink() {
		return transitStop2nearestCarLink;
	}

	public SortedMap<Id, PRCarLinkToNode> getCarLinkToNodes() {
		return carLinkToNodes;
	}

	public List<TransitStopFacility> getStopsWithoutPRFacility() {
		return stopsWithoutPRFacility;
	}

	public List<TransitStopFacility> getStopsNotSUBerlin() {
		return stopsNotSUBerlin;
	}

}
