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

/**
 * 
 */
package air.scenario;

/**
 * @author sfuerbas
 *
 */
public class DgStarinfo {

	private String id;
	private double length;
	private double capacity;
	private double freespeed;

	public DgStarinfo(String id, double length, double capacity, double freespeed) {
		this.id = id;
		this.length = length;
		this.capacity = capacity;
		this.freespeed = freespeed;
	}

	public String getId() {
		return this.id;
	}

	public double getLength() {
		return this.length;
	}

	public double getCapacity() {
		return this.capacity;
	}

	public double getFreespeed() {
		return this.freespeed;
	}
	
	

}
