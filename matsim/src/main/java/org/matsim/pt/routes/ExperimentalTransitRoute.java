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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

@Deprecated
public class ExperimentalTransitRoute extends AbstractRoute implements TransitPassengerRoute {

	private final static String SEPARATOR = "===";
	private final static String IDENTIFIER_1 = "PT1" + SEPARATOR;

	/* package */ final static String ROUTE_TYPE = "experimentalPt1";

	private Id<TransitStopFacility> accessStopId = null;
	private Id<TransitStopFacility> egressStopId = null;
	private Id<TransitLine> lineId = null;
	private Id<TransitRoute> routeId = null;
	private String description = null;
	private String routeDescription;

	/* package */ ExperimentalTransitRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	public ExperimentalTransitRoute(final TransitStopFacility accessFacility, final TransitStopFacility egressFacility, final Id<TransitLine> lineId, final Id<TransitRoute> routeId) {
		this(accessFacility.getLinkId(), egressFacility.getLinkId());
		this.accessStopId = accessFacility.getId();
		this.lineId = lineId;
		this.routeId = routeId;
		this.egressStopId = egressFacility.getId();
	}

	/**
	 * Why do we need this constructor, if we only keep the id of the line/route?
	 */
	public ExperimentalTransitRoute(final TransitStopFacility accessFacility, final TransitLine line, final TransitRoute route, final TransitStopFacility egressFacility) {
		this(accessFacility, egressFacility, (line == null ? null : line.getId()), (route == null ? null : route.getId()));
	}

	@Override
	public ExperimentalTransitRoute clone() {
		return (ExperimentalTransitRoute) super.clone();
	}

	public Id<TransitStopFacility> getAccessStopId() {
		return this.accessStopId;
	}

	public Id<TransitStopFacility> getEgressStopId() {
		return this.egressStopId;
	}

	public Id<TransitLine> getLineId() {
		return this.lineId;
	}

	public Id<TransitRoute> getRouteId() {
		return this.routeId;
	}

	@Override
	public void setRouteDescription(final String routeDescription) {
//		super.setRouteDescription(routeDescription);
		this.routeDescription = routeDescription;
		if (routeDescription.startsWith(IDENTIFIER_1)) {
			String[] parts = routeDescription.split(SEPARATOR, 6);// StringUtils.explode(routeDescription, '\t', 6);
			this.accessStopId = Id.create(parts[1], TransitStopFacility.class);
			this.lineId = Id.create(parts[2], TransitLine.class);
			this.routeId = Id.create(parts[3], TransitRoute.class);
			this.egressStopId = Id.create(parts[4], TransitStopFacility.class);
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
//			return super.getRouteDescription();
			return this.routeDescription;
		}
		String str = IDENTIFIER_1 + this.accessStopId.toString() + SEPARATOR + this.lineId.toString() + SEPARATOR
				+ this.routeId.toString() + SEPARATOR + this.egressStopId.toString();
		if (this.description != null) {
			str = str + SEPARATOR + this.description;
		}
		return str;
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public TransitPassengerRoute getChainedRoute() {
		// Not supported
		return null;
	}

	@Override
	public String toString() {
		return "[ExpTransitRoute: access=" + this.accessStopId.toString() + " egress=" + this.egressStopId + " line="
				+ this.lineId + " route=" + this.routeId + " ]";
	}

	@Override
	public OptionalTime getBoardingTime() {
		return OptionalTime.undefined();
	}
}
