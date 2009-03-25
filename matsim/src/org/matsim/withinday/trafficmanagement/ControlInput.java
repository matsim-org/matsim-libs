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

import org.matsim.core.api.population.NetworkRoute;

/**
 * @author dgrether
 *
 */
public interface ControlInput {

	public double getNashTime();

	public NetworkRoute getMainRoute();

	public NetworkRoute getAlternativeRoute();

	public void init();

	public int getNumberOfVehiclesOnRoute(NetworkRoute route);

	public void setMainRoute(NetworkRoute route);

	public void setAlternativeRoute(NetworkRoute route);

	public double getMeasuredRouteTravelTime(NetworkRoute route);

	public void finishIteration();

}
