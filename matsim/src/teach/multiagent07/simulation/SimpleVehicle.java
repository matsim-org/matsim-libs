/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package teach.multiagent07.simulation;

import org.matsim.basic.v01.BasicLinkImpl;

public class SimpleVehicle extends Vehicle {

	private BasicLinkImpl depLink;
	private double depTime;
	
	public SimpleVehicle(BasicLinkImpl link, double time) {
		depLink = link;
		depTime = time;
	}
	@Override
	public BasicLinkImpl getDepartureLink() {
		return depLink;
	}

	@Override
	public double getDepartureTime() {
		return depTime;
	}

}
