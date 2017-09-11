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

import org.matsim.contrib.taxi.data.TaxiRequest;

class AssignmentRequestData extends AssignmentDestinationData<TaxiRequest> {
	private int urgentReqCount = 0;

	AssignmentRequestData(double currentTime, double planningHorizon, Iterable<TaxiRequest> unplannedRequests) {
		double maxT0 = currentTime + planningHorizon;

		int idx = 0;
		for (TaxiRequest r : unplannedRequests) {
			double t0 = r.getEarliestStartTime();
			if (t0 > maxT0) {// beyond the planning horizon
				continue;
			}

			if (t0 <= currentTime) {
				urgentReqCount++;
			}
			entries.add(new DestEntry<TaxiRequest>(idx++, r, r.getFromLink(), t0));
		}
	}

	public int getUrgentReqCount() {
		return urgentReqCount;
	}
}