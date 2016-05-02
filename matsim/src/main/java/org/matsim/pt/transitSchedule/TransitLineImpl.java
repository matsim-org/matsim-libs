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

package org.matsim.pt.transitSchedule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;


/**
 * Description of a single transit line. Can have multiple routes (e.g. from A to B and from B to A).
 *
 * @author mrieser
 */
public class TransitLineImpl implements TransitLine {

	private final Id<TransitLine> lineId;
	private String name = null;
	private final Map<Id<TransitRoute>, TransitRoute> transitRoutes = new LinkedHashMap<Id<TransitRoute>, TransitRoute>(5);

	protected TransitLineImpl(final Id<TransitLine> id) {
		this.lineId = id;
	}

	@Override
	public Id<TransitLine> getId() {
		return this.lineId;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void addRoute(final TransitRoute transitRoute) {
		final Id<TransitRoute> id = transitRoute.getId();
		if (this.transitRoutes.containsKey(id)) {
			throw new IllegalArgumentException("There is already a transit route with id " + id.toString() + " with line " + this.lineId);
		}
		this.transitRoutes.put(id, transitRoute);
	}

	@Override
	public Map<Id<TransitRoute>, TransitRoute> getRoutes() {
		return Collections.unmodifiableMap(this.transitRoutes);
	}

	@Override
	public boolean removeRoute(final TransitRoute route) {
		return null != this.transitRoutes.remove(route.getId());
	}

	@Override
	public String toString() {
		return "[TransitLineImpl: line=" + this.lineId.toString() + ", #routes=" + this.transitRoutes.size() + "]";
	}
	
}
