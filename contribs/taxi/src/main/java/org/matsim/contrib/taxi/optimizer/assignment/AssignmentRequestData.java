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

package org.matsim.contrib.taxi.optimizer.assignment;

import org.matsim.contrib.drt.passenger.DrtRequest;

import com.google.common.collect.ImmutableList;

class AssignmentRequestData extends AssignmentDestinationData<DrtRequest> {

	static AssignmentRequestData create(double currentTime, double planningHorizon,
			Iterable<DrtRequest> unplannedRequests) {
		double maxEarliestStart = currentTime + planningHorizon;
		ImmutableList.Builder<DestEntry<DrtRequest>> builder = ImmutableList.builder();

		int idx = 0;
		int urgentReqCount = 0;
		for (DrtRequest r : unplannedRequests) {
			double earliestStart = r.getEarliestStartTime();
			if (earliestStart > maxEarliestStart) {// beyond the planning horizon
				continue;
			}
			if (earliestStart <= currentTime) {
				urgentReqCount++;
			}
			builder.add(new DestEntry<>(idx++, r, r.getFromLink(), earliestStart));
		}

		return new AssignmentRequestData(builder.build(), urgentReqCount);
	}

	private final int urgentReqCount;

	private AssignmentRequestData(ImmutableList<DestEntry<DrtRequest>> entries, int urgentReqCount) {
		super(entries);
		this.urgentReqCount = urgentReqCount;
	}

	int getUrgentReqCount() {
		return urgentReqCount;
	}
}
