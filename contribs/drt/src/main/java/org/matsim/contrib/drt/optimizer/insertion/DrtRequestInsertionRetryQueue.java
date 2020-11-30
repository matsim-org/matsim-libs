/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author Michal Maciejewski (michalm)
 */
class DrtRequestInsertionRetryQueue {
	private final DrtRequestInsertionRetryParams params;

	DrtRequestInsertionRetryQueue(DrtRequestInsertionRetryParams params) {
		this.params = params;
	}

	//priority queue not needed - retry interval is equal for all requests
	private final Deque<RequestRetryEntry> requestQueue = new LinkedList<>();

	boolean tryAddFailedRequest(DrtRequest request, double now) {
		if (request.getSubmissionTime() + params.getMaxRequestAge() < now + params.getRetryInterval()) {
			return false;//request is too old, not eligible for retry
		}
		requestQueue.addLast(new RequestRetryEntry(request, now));
		return true;
	}

	List<DrtRequest> getRequestsToRetryNow(double now) {
		double maxAttemptTimeForRetry = now - params.getRetryInterval();
		List<DrtRequest> requests = new ArrayList<>();
		while (!requestQueue.isEmpty() && requestQueue.getFirst().lastAttemptTime <= maxAttemptTimeForRetry) {
			var entry = requestQueue.removeFirst();
			//no guarantee that this method is called every second, so we need to calculate time delta, instead of
			//directly using the retry interval
			double timeDelta = now - entry.lastAttemptTime;
			var oldRequest = entry.request;
			//XXX alternatively make both latest start/arrival times modifiable
			var newRequest = DrtRequest.newBuilder(oldRequest)
					.latestStartTime(oldRequest.getLatestStartTime() + timeDelta)
					.latestArrivalTime(oldRequest.getLatestArrivalTime() + timeDelta)
					.build();
			requests.add(newRequest);
		}
		return requests;
	}

	private static class RequestRetryEntry {
		private final DrtRequest request;
		private final double lastAttemptTime;

		private RequestRetryEntry(DrtRequest request, double lastAttemptTime) {
			this.request = request;
			this.lastAttemptTime = lastAttemptTime;
		}
	}
}
