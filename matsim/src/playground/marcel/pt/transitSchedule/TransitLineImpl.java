/* *********************************************************************** *
 * project: org.matsim.*
 * TransitLine.java
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

package playground.marcel.pt.transitSchedule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;

/**
 * Description of a single transit line. Can have multiple routes (e.g. from A to B and from B to A).
 * 
 * @author mrieser
 */
public class TransitLineImpl {

	private final Id lineId;
	private final Map<Id, TransitRouteImpl> transitRoutes = new LinkedHashMap<Id, TransitRouteImpl>();

	public TransitLineImpl(final Id id) {
		this.lineId = id;
	}

	public Id getId() {
		return this.lineId;
	}

	public void addRoute(final TransitRouteImpl transitRoute) {
		final Id id = transitRoute.getId();
		if (this.transitRoutes.containsKey(id)) {
			throw new IllegalArgumentException("There is already a transit route with id " + id.toString());
		}
		this.transitRoutes.put(id, transitRoute);
	}

	public Map<Id, TransitRouteImpl> getRoutes() {
		return Collections.unmodifiableMap(this.transitRoutes);
	}

	public void removeRoute(final TransitRouteImpl route) {
		this.transitRoutes.remove(route.getId());
	}

}
