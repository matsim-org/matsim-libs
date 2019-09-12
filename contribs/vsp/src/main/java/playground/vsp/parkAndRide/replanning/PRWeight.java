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
package playground.vsp.parkAndRide.replanning;

import org.matsim.api.core.v01.Id;

/**
 * The weight of a park-and-ride facility which is used for calculating the probability to be chosen.
 * 
 * @author ikaddoura
 *
 */
public class PRWeight implements Comparable<PRWeight> {
	private Id id; // park-and-ride ID
	private double weight;
	
	public PRWeight(Id id, Double weight) {
		this.id = id;
		this.weight = weight;
	}
	
	public Id getId() {
		return id;
	}
	public double getWeight() {
		return weight;
	}
	
	public int compareTo(PRWeight entry) {
		if (this.weight > entry.getWeight()) {
			return 1;
		} else if (this.weight == entry.getWeight()) {
			return 0; 
		}
		else return -1;
	}
}
