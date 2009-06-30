/* *********************************************************************** *
 * project: org.matsim.*
 * Leg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.api.population;

import org.matsim.api.basic.v01.population.BasicLeg;

public interface Leg extends BasicLeg, PlanElement {

	public Route getRoute();

	public double getArrivalTime();
	// TODO kn Not sure what this means.  mv from interface to implementation?
	
	public void setArrivalTime(final double seconds);
	// TODO kn Not sure what this means.  mv from interface to implementation?

}