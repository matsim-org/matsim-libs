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

package org.matsim.transitSchedule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;


/**
 * Description of a single transit line. Can have multiple routes (e.g. from A to B and from B to A).
 * 
 * @author mrieser
 */
public class TransitLineImpl implements TransitLine {

	private final Id lineId;
	private final Map<Id, TransitRoute> transitRoutes = new LinkedHashMap<Id, TransitRoute>();

	protected TransitLineImpl(final Id id) {
		this.lineId = id;
	}

	public Id getId() {
		return this.lineId;
	}

	public void addRoute(final TransitRoute transitRoute) {
		final Id id = transitRoute.getId();
		if (this.transitRoutes.containsKey(id)) {
			throw new IllegalArgumentException("There is already a transit route with id " + id.toString());
		}
		this.transitRoutes.put(id, transitRoute);
	}

	public Map<Id, TransitRoute> getRoutes() {
		return Collections.unmodifiableMap(this.transitRoutes);
	}

	public boolean removeRoute(final TransitRoute route) {
		return null != this.transitRoutes.remove(route.getId());
	}

}
