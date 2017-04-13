/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxibus.tasks;

import java.util.*;

import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

public class TaxibusPickupTask extends StayTaskImpl implements TaxibusTaskWithRequests {
	private final Set<TaxibusRequest> requests;

	public TaxibusPickupTask(double beginTime, double endTime, Set<TaxibusRequest> requests) {
		super(beginTime, endTime, requests.iterator().next().getFromLink());

		this.requests = new HashSet<>(requests);
		for (TaxibusRequest req : this.requests) {
			req.setPickupTask(this);
		}
	}

	@Override
	public TaxibusTaskType getTaxibusTaskType() {
		return TaxibusTaskType.PICKUP;
	}

	@Override
	public Set<TaxibusRequest> getRequests() {
		return Collections.unmodifiableSet(requests);
	}

	@Override
	protected String commonToString() {
		return "[" + getTaxibusTaskType().name() + "]" + super.commonToString();
	}
}
