/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.barcelona.demand;

import java.util.Date;

import org.matsim.api.core.v01.*;

import playground.michalm.demand.taxi.ServedRequest;

public class BarcelonaServedRequest implements ServedRequest {
	public final Id<ServedRequest> id;
	public final Coord from;
	public final Coord to;
	public final Date startTime;
	public final Date endTime;
	public final int travelTime;
	public final double distance;

	public BarcelonaServedRequest(Id<ServedRequest> id, Coord from, Coord to, Date start, Date end, int travelTime,
			double distance) {
		this.id = id;
		this.from = from;
		this.to = to;
		this.startTime = start;
		this.endTime = end;
		this.travelTime = travelTime;
		this.distance = distance;
	}

	@Override
	public Id<ServedRequest> getId() {
		return id;
	}

	@Override
	public Coord getFrom() {
		return from;
	}

	@Override
	public Coord getTo() {
		return to;
	}

	@Override
	public Date getStartTime() {
		return startTime;
	}
}