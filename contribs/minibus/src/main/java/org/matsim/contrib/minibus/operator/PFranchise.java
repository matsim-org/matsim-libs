/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.operator;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.genericUtils.GridNode;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simple Franchise system rejecting all routes already operated with respect to stops served and time of operation
 * 
 * @author aneumann
 *
 */
public final class PFranchise {

	private final static Logger log = Logger.getLogger(PFranchise.class);
	
	private final boolean activated;
	private final double gridSize;
	private TreeSet<String> routeHashes = new TreeSet<>();

	
	public PFranchise(boolean useFranchise, double gridSize) {
		this.activated = useFranchise;
		this.gridSize = gridSize;
		if(this.activated){
			log.info("Franchise system activated");
		} else{
			log.info("Franchise system NOT activated");
		}
	}

	public boolean planRejected(PPlan plan) {
		
		if(!this.activated){
			return false;
		}
		
		String routeHash = generateRouteHash(plan);
		boolean reject = this.routeHashes.contains(routeHash);
		this.routeHashes.add(routeHash);
		
		return reject;
	}

	/**
	 * Reset all route hashes to the routes currently in use
	 * 
	 * @param operators
	 */
	public void reset(LinkedList<Operator> operators) {
		
		if(this.activated){
			this.routeHashes = new TreeSet<>();

			for (Operator operator : operators) {
				Set<String> routesHashesOfOperator = new TreeSet<>();
				for (PPlan plan : operator.getAllPlans()) {
					String routeHash = generateRouteHash(plan);				
					if (this.routeHashes.contains(routeHash)) {
						if (routesHashesOfOperator.contains(routeHash)) {
							// This route is already served by the same operator
							log.warn("Operator " + operator.getId() + " offers the same plan twice. Plan: " + plan.getId() + " managed to circumvent the franchise system with route " + routeHash);
						} else {
							// This route is already served by another operator
							log.warn("Operator " + operator.getId() + " with plan " + plan.getId() + " managed to circumvent the franchise system with route " + routeHash);
						}
					}
					this.routeHashes.add(routeHash);
					routesHashesOfOperator.add(routeHash);
				}
			}
		}
	}
	
	private String generateRouteHash(PPlan plan) {
	//	return generateRouteHash(plan.getStopsToBeServed());
	//	return generateRouteHash(plan.getStartTime(), plan.getEndTime(), plan.getStopsToBeServed());
		return generateRouteHashWithGridNodes(plan.getStartTime(), plan.getEndTime(), plan.getStopsToBeServed());
	}

	/**
	 * Generates a unique String from the stops given
	 * 
	 * @param stopsToBeServed
	 * @return
	 */
	private String generateRouteHash(ArrayList<TransitStopFacility> stopsToBeServed){
		StringBuffer sB = null;
		for (TransitStopFacility transitStopFacility : stopsToBeServed) {
			if (sB == null) {
				sB = new StringBuffer();
				sB.append(transitStopFacility.getId().toString());
			} else {
				sB.append("-");
				sB.append(transitStopFacility.getId().toString()); 
			}			
		}
		return sB.toString();
	}

	private String generateRouteHash(double startTime, double endTime, ArrayList<TransitStopFacility> stopsToBeServed) {
		StringBuffer sB = new StringBuffer();
		sB.append(startTime);
		sB.append("-" + endTime);
		
		for (TransitStopFacility transitStopFacility : stopsToBeServed) {
			sB.append("-");
			sB.append(transitStopFacility.getId().toString()); 
		}

		return sB.toString();
	}
	
	private String generateRouteHashWithGridNodes(double startTime, double endTime, ArrayList<TransitStopFacility> stopsToBeServed) {
		StringBuffer sB = new StringBuffer();
		sB.append(startTime);
		sB.append("-" + endTime);
		
		String lastGridNodeId = null;
		for (TransitStopFacility transitStopFacility : stopsToBeServed) {
			String gridNodeId = GridNode.getGridNodeIdForCoord(transitStopFacility.getCoord(), this.gridSize);
			
			if (lastGridNodeId == null) {
				lastGridNodeId = gridNodeId;
			} else {
				if (gridNodeId.equalsIgnoreCase(lastGridNodeId)) {
					// still in same gridSquare
					continue;
				}
				lastGridNodeId = gridNodeId;
			}

			sB.append("-");
			sB.append(lastGridNodeId); 
		}

		return sB.toString();
	}
}
