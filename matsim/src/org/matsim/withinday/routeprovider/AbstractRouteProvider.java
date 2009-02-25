/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRouteProvider.java
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

package org.matsim.withinday.routeprovider;

import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.network.Link;


/**
 * @author dgrether
 *
 */
public abstract class AbstractRouteProvider implements RouteProvider {

	private int priority;

	/**
	 * Throws an IllegalArgumentException if the value is not in 0..10
	 * @see org.matsim.withinday.routeprovider.RouteProvider#setPriority(int)
	 */
	public void setPriority(int p) {
		if ((0 <= p) && (p <= 10)) {
			this.priority = p;
		}
		else {
			throw new IllegalArgumentException("The priority must be a value in 0..10!");
		}
	}

	public int getPriority() {
		return this.priority;
	}

	public abstract CarRoute requestRoute(Link departureLink, Link destinationLink, double time);

}
