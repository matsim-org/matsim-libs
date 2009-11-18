/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.api.core.v01.population;

import java.io.Serializable;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.internal.MatsimPopulationObject;

/**
 * @author nagel
 *
 */
public interface Route extends Serializable, MatsimPopulationObject {

	public double getDistance();

	public void setDistance(final double distance);

	/** @deprecated -- use leg.getTravelTime() instead. kai, aug09 */ 
	@Deprecated
	public double getTravelTime();

	/** @deprecated -- use leg.setTravelTime() instead. kai, aug09 */
	@Deprecated
	public void setTravelTime(final double travelTime);

	public Id getStartLinkId();

	public Id getEndLinkId();

}
