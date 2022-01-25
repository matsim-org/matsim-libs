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

package org.matsim.contrib.taxi.schedule;

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.DROPOFF;

import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.taxi.passenger.TaxiRequest;

import com.google.common.base.MoreObjects;

public class TaxiDropoffTask extends DefaultStayTask {
	public static final TaxiTaskType TYPE = new TaxiTaskType(DROPOFF);

	private final TaxiRequest request;

	public TaxiDropoffTask(double beginTime, double endTime, TaxiRequest request) {
		super(TYPE, beginTime, endTime, request.getToLink());
		this.request = request;
		request.setDropoffTask(this);
	}

	public TaxiRequest getRequest() {
		return request;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("request", request).add("super", super.toString()).toString();
	}
}
