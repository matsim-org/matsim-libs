/* *********************************************************************** *
 * project: org.matsim.*
 * TravelMinCostI.java
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

package org.matsim.router.util;

import org.matsim.network.Link;

/**
 * @author lnicolas
 *
 */
public interface TravelMinCostI extends TravelCostI {

	/**
	 * @param link the link for which the minimal travel cost over all time slots is calculated
	 * @return Minimal costs to travel over the link <pre>link</pre>, departing at time <pre>time</pre>
	 */
	public double getLinkMinimumTravelCost(Link link);
}
