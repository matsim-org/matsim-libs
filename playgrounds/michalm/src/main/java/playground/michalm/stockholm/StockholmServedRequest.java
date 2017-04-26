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

package playground.michalm.stockholm;

import java.util.*;

import org.matsim.api.core.v01.*;

import playground.michalm.demand.taxi.ServedRequest;

public class StockholmServedRequest implements ServedRequest {
	final Id<ServedRequest> id;
	final List<TaxiTrace> trace;
	final String taxiId;

	public StockholmServedRequest(Id<ServedRequest> id, List<TaxiTrace> trace, String taxiId) {
		this.id = id;
		this.trace = trace;
		this.taxiId = taxiId;
	}

	@Override
	public Id<ServedRequest> getId() {
		return id;
	}

	@Override
	public Coord getFrom() {
		return trace.get(0).coord;
	}

	@Override
	public Coord getTo() {
		return trace.get(trace.size() - 1).coord;
	}

	@Override
	public Date getStartTime() {
		return trace.get(0).time;
	}
}