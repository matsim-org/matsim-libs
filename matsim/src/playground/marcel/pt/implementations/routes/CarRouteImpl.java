/* *********************************************************************** *
 * project: org.matsim.*
 * CarRouteImpl.java
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

package playground.marcel.pt.implementations.routes;

import java.util.List;

import org.matsim.basic.v01.Id;

import playground.marcel.pt.interfaces.routes.CarRoute;

public class CarRouteImpl implements CarRoute {

	private final Id departureLinkId;
	private final Id arrivalLinkId;
	private final List<Id> travelLinkIds;

	private double travelTime;

	public CarRouteImpl(final Id departureLinkId, final List<Id> travelLinkIds, final Id arrivalLink, final double travelTime) {
		this.departureLinkId = departureLinkId;
		this.travelLinkIds = travelLinkIds;
		this.arrivalLinkId = arrivalLink;
		this.travelTime = travelTime;
	}

	public Id getDepartureLinkId() {
		return this.departureLinkId;
	}

	public Id getArrivalLinkId() {
		return this.arrivalLinkId;
	}

	public List<Id> getLinkIds() {
		return this.travelLinkIds;
	}

	public double getTravelTime() {
		return this.travelTime;
	}

	public void setTravelTime(final double travelTime) {
		this.travelTime = travelTime;
	}

}
