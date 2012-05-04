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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author Ihab
 *
 */
public class PRNodeSearch {
	private List<TransitStopFacility> transitStops = new ArrayList<TransitStopFacility>();
	private Map<TransitStopFacility, Link> transitStop2nearestCarLink = new HashMap<TransitStopFacility, Link>();
	private List<Node> carLinkToNodes = new ArrayList<Node>();

	public void searchForCarLink(Scenario scenario, double extensionRadius, int maxSearchSteps) {
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		for (TransitLine line : schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				for (TransitRouteStop stop : route.getStops()){
					transitStops.add(stop.getStopFacility());
				}
			}
		}
		
		// -------------------------------------------------------------
		
		boolean hasPRnode;
		for (TransitStopFacility stop : transitStops){
			hasPRnode = false;
			Id stopLinkId = stop.getLinkId();
			Link stopLink = scenario.getNetwork().getLinks().get(stopLinkId);
			if (stopLink.getAllowedModes().contains(TransportMode.car)){
				this.transitStop2nearestCarLink.put(stop, stopLink);
				if (!this.carLinkToNodes.contains(stopLink.getToNode())){
					this.carLinkToNodes.add(stopLink.getToNode());
				}
			}
			else {
				System.out.println("***");
				System.out.println("TransitStopFacility "+stop.getId()+" is not on a car-Link.");
				System.out.println("Searching for car-Links in area of TransitStop...");
				Coord coord = stop.getCoord();
				double searchRadius = 0; 
				for (int n = 0; n <= maxSearchSteps; n++){
					if (!hasPRnode){
						searchRadius = searchRadius + extensionRadius;
						System.out.println("Suchradius: " + searchRadius + " um die Koordinaten " + coord.toString() + ".");

						for (Link link : scenario.getNetwork().getLinks().values()){
							if (!hasPRnode){
								if (link.getAllowedModes().contains(TransportMode.car)){
									System.out.println("Der Link "+link.getId()+" ist ein car-Link.");
									double linkToNodeX = link.getToNode().getCoord().getX();
									double linkToNodeY = link.getToNode().getCoord().getY();
									double xDiff = Math.abs(linkToNodeX - stop.getCoord().getX());
									double yDiff = Math.abs(linkToNodeY - stop.getCoord().getY());
									double hyp = Math.sqrt(Math.pow(xDiff, 2)+Math.pow(yDiff, 2));
									if (hyp <= searchRadius){
										System.out.println("Der Link "+link.getId()+" liegt im Suchradius um die TransitStopCoordinates "+coord.toString()+".");
										this.transitStop2nearestCarLink.put(stop, link);
										if (!this.carLinkToNodes.contains(link.getToNode())){
											this.carLinkToNodes.add(link.getToNode());
										}
										hasPRnode = true;
									}
									else {
										System.out.println("Der Link "+link.getId()+" liegt NICHT im Suchradius um die TransitStopCoordinates "+coord.toString()+".");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public List<TransitStopFacility> getTransitStops() {
		return transitStops;
	}

	public Map<TransitStopFacility, Link> getTransitStop2nearestCarLink() {
		return transitStop2nearestCarLink;
	}

	public List<Node> getCarLinkToNodes() {
		return carLinkToNodes;
	}

}
