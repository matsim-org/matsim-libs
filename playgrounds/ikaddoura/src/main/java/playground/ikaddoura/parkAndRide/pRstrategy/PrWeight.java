/* *********************************************************************** *
 * project: org.matsim.*
 * PREntry.java
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
package playground.ikaddoura.parkAndRide.pRstrategy;

import org.matsim.api.core.v01.Id;

/**
 * @author Ihab
 *
 */
public class PrWeight implements Comparable<PrWeight> {
	private Id id; // ParkAndRideFacility-Id
	private double weight;
	
	public PrWeight(Id id, Double weight) {
		this.id = id;
		this.weight = weight;
	}
	
	public Id getId() {
		return id;
	}
	public double getWeight() {
		return weight;
	}
	
	public int compareTo(PrWeight entry) {
		if (this.weight > entry.getWeight()) {
			return 1;
		} else if (this.weight == entry.getWeight()) {
			return 0; 
		}
		else return -1;
	}
}
