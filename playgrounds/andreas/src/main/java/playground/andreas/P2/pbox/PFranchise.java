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

package playground.andreas.P2.pbox;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.replanning.PPlan;

/**
 * Simple Franchise system rejecting all routes already operated
 * 
 * @author aneumann
 *
 */
public class PFranchise {

	private final static Logger log = Logger.getLogger(PFranchise.class);
	
	private final boolean activated;
	private TreeSet<String> routeHashes = new TreeSet<String>();
	
	public PFranchise(boolean useFranchise) {
		this.activated = useFranchise;
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
		
		String routeHash = generateRouteHash(plan.getStopsToBeServed());
		boolean reject = this.routeHashes.contains(routeHash);
		this.routeHashes.add(routeHash);
		
		return reject;
	}

	/**
	 * Reset all route hashes to the routes currently in use
	 * 
	 * @param cooperatives
	 */
	public void reset(LinkedList<Cooperative> cooperatives) {
		
		if(this.activated){
			this.routeHashes = new TreeSet<String>();

			for (Cooperative cooperative : cooperatives) {
				Set<String> routesHashesOfCooperative = new TreeSet<String>();
				for (PPlan plan : cooperative.getAllPlans()) {
					String routeHash = generateRouteHash(plan.getStopsToBeServed());				
					if (this.routeHashes.contains(routeHash)) {
						if (!routesHashesOfCooperative.contains(routeHash)) {
							// This route is already served by another cooperative
							log.warn("Cooperative " + cooperative.getId() + " with plan " + plan.getId() + " managed to circumvent the franchise system with route " + routeHash);
						}
					}
					this.routeHashes.add(routeHash);
					routesHashesOfCooperative.add(routeHash);
				}
			}
		}
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
}
