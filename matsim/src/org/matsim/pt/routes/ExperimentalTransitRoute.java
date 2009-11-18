/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalTransitRoute.java
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

package org.matsim.pt.routes;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitStopFacility;


public class ExperimentalTransitRoute extends GenericRouteImpl {

	private static final long serialVersionUID = 1L;

	private final static String SEPARATOR = "===";
	private final static String IDENTIFIER_1 = "PT1" + SEPARATOR;

	private Id accessStopId = null;
	private Id egressStopId = null;
	private Id lineId = null;
	private Id routeId = null;
	private String description = null;

	/*package*/ ExperimentalTransitRoute(final Link startLink, final Link endLink) {
		super(startLink, endLink);
	}

	public ExperimentalTransitRoute(final TransitStopFacility accessFacility, final TransitLine line, final TransitRoute route, final TransitStopFacility egressFacility) {
		this(accessFacility.getLink(), egressFacility.getLink());
		this.accessStopId = accessFacility.getId();
		this.lineId = (line == null ? null : line.getId());
		this.routeId = (route == null ? null : route.getId());
		this.egressStopId = egressFacility.getId();
	}

	@Override
	public ExperimentalTransitRoute clone() {
		return (ExperimentalTransitRoute) super.clone();
	}

	public Id getAccessStopId() {
		return this.accessStopId;
	}

	public Id getEgressStopId() {
		return this.egressStopId;
	}

	public Id getLineId() {
		return this.lineId;
	}

	public Id getRouteId() {
		return this.routeId;
	}

	@Override
	public void setRouteDescription(final Link startLink, final String routeDescription, final Link endLink) {
		super.setRouteDescription(startLink, routeDescription, endLink);
		if (routeDescription.startsWith(IDENTIFIER_1)) {
			String[] parts = routeDescription.split(SEPARATOR, 6);//StringUtils.explode(routeDescription, '\t', 6);
			this.accessStopId = new IdImpl(parts[1]);
			this.lineId = new IdImpl(parts[2]);
			this.routeId = new IdImpl(parts[3]);
			this.egressStopId = new IdImpl(parts[4]);
			if (parts.length > 5) {
				this.description = parts[5];
			} else {
				this.description = null;
			}
		} else {
			this.accessStopId = null;
			this.lineId = null;
			this.egressStopId = null;
		}
	}

	@Override
	public String getRouteDescription() {
		if (this.accessStopId == null) {
			return super.getRouteDescription();
		}
		String str = IDENTIFIER_1 + this.accessStopId.toString() + SEPARATOR + this.lineId.toString() + SEPARATOR +
				this.routeId.toString() + SEPARATOR + this.egressStopId.toString();
		if (this.description != null) {
			str = str + SEPARATOR + this.description;
		}
		return str;
	}

}
