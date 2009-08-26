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

import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * @author dgrether
 *
 */
public interface ControlInput {

	public double getNashTime();

	public NetworkRouteWRefs getMainRoute();

	public NetworkRouteWRefs getAlternativeRoute();

	public void init();

	public int getNumberOfVehiclesOnRoute(NetworkRouteWRefs route);

	public void setMainRoute(NetworkRouteWRefs route);

	public void setAlternativeRoute(NetworkRouteWRefs route);

	public double getMeasuredRouteTravelTime(NetworkRouteWRefs route);

	public void finishIteration();

}
