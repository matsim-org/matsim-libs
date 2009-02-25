/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInput.java
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

package org.matsim.withinday.trafficmanagement;

import org.matsim.interfaces.core.v01.CarRoute;

/**
 * @author dgrether
 *
 */
public interface ControlInput {

	public double getNashTime();

	public CarRoute getMainRoute();

	public CarRoute getAlternativeRoute();

	public void init();

	public int getNumberOfVehiclesOnRoute(CarRoute route);

	public void setMainRoute(CarRoute route);

	public void setAlternativeRoute(CarRoute route);

	public double getMeasuredRouteTravelTime(CarRoute route);

	public void finishIteration();

}
