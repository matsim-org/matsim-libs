/* *********************************************************************** *
 * project: org.matsim.*
 * Visitor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.interaction;

import org.matsim.population.Person;

/**
 * @author illenberger
 *
 */
public class Visitor {

	private Person p;
	
	private double enterTime;
	
	private double leaveTime;
	
	Visitor(Person p, double enterTime) {
		this.p = p;
		this.enterTime = enterTime;
		this.leaveTime = Double.NaN;
	}
	
	public Person getPerson() {
		return p;
	}
	
	public double getEnterTime() {
		return enterTime;
	}
	
	public double getLeaveTime() {
		return leaveTime;
	}
}
