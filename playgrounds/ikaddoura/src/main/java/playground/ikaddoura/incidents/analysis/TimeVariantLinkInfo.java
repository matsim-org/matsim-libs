/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.analysis;

/**
* @author ikaddoura
*/

public class TimeVariantLinkInfo {
	private double capacity;
	private double lanes;
	private double freespeed;
	
	private double delay; // TODO
	
	public TimeVariantLinkInfo(double capacity, double lanes, double freespeed) {
		this.capacity = capacity;
		this.lanes = lanes;
		this.freespeed = freespeed;
	}

	public double getCapacity() {
		return capacity;
	}
	
	public double getLanes() {
		return lanes;
	}
	public double getFreespeed() {
		return freespeed;
	}

	public double getDelay() {
		return delay;
	}

	public void setDelay(double delay) {
		this.delay = delay;
	}
	
}

