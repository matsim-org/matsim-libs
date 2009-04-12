/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.api.basic.v01.population;

import org.matsim.api.basic.v01.TransportMode;


/**
* @author dgrether
*/
public interface BasicLeg extends BasicPlanElement {

	public TransportMode getMode();

	public void setMode(TransportMode mode);

	public BasicRoute getRoute();

	public void setRoute(BasicRoute route);

	public double getDepartureTime();

	public void setDepartureTime(final double seconds);

	public double getTravelTime();

	public void setTravelTime(final double seconds);

}