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

package playground.marcel.pt.routes;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitStopFacility;


public class ExperimentalTransitRoute extends GenericRouteImpl {

	private Id accessStopId = null;
	private Id egressStopId = null;
	private Id lineId = null;
	private String description = null;
	
	public ExperimentalTransitRoute(Link startLink, Link endLink) {
		super(startLink, endLink);
	}
	
	public ExperimentalTransitRoute(TransitStopFacility accessFacility, TransitLine line, TransitStopFacility egressFacility) {
		this(accessFacility.getLink(), egressFacility.getLink());
		this.accessStopId = accessFacility.getId();
		this.lineId = line.getId();
		this.egressStopId = egressFacility.getId();
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
	
	@Override
	public void setRouteDescription(Link startLink, String routeDescription, Link endLink) {
		super.setRouteDescription(startLink, routeDescription, endLink);
		if (routeDescription.startsWith("PT1 ")) {
			String[] parts = StringUtils.explode(routeDescription, ' ', 5);
			this.accessStopId = new IdImpl(parts[1]);
			this.lineId = new IdImpl(parts[2]);
			this.egressStopId = new IdImpl(parts[3]);
			if (parts.length > 4) {
				this.description = parts[4];
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
		String str = "PT1 " + this.accessStopId.toString() + " " + this.lineId.toString() + " " + this.egressStopId.toString();
		if (this.description != null) {
			str = str + " " + this.description;
		}
		return str;
	}
	
}
