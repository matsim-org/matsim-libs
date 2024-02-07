/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtRequestInsertionRetryQueueTest {
	private static final double SUBMISSION_TIME = 100;
	private static final double MAX_WAIT_TIME = 100;
	private static final double MAX_TRAVEL_TIME = 200;
	private final DrtRequest request = DrtRequest.newBuilder()
			.id(Id.create("r", Request.class))
			.submissionTime(SUBMISSION_TIME)
			.earliestStartTime(SUBMISSION_TIME)
			.latestStartTime(SUBMISSION_TIME + MAX_WAIT_TIME)
			.latestArrivalTime(SUBMISSION_TIME + MAX_TRAVEL_TIME)
			.build();

	@Test
	void maxRequestAgeZero_noRetry() {
		var queue = new DrtRequestInsertionRetryQueue(params(10, 0));
		assertThat(queue.tryAddFailedRequest(request, SUBMISSION_TIME)).isFalse();
		assertThat(queue.getRequestsToRetryNow(9999)).isEmpty();
	}

	@Test
	void requestMaxAgeExceeded_noRetry() {
		var queue = new DrtRequestInsertionRetryQueue(params(2, 10));
		assertThat(queue.tryAddFailedRequest(request, SUBMISSION_TIME + 10)).isFalse();
		assertThat(queue.getRequestsToRetryNow(9999)).isEmpty();
	}

	@Test
	void requestMaxAgeNotExceeded_retry() {
		var queue = new DrtRequestInsertionRetryQueue(params(2, 10));
		assertThat(queue.tryAddFailedRequest(request, SUBMISSION_TIME)).isTrue();

		//too early for retry
		assertThat(queue.getRequestsToRetryNow(SUBMISSION_TIME + 1)).isEmpty();

		//retry
		double now = SUBMISSION_TIME + 2;
		assertThat(queue.getRequestsToRetryNow(now)).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(DrtRequest.newBuilder(request)
						.latestStartTime(now + MAX_WAIT_TIME)
						.latestArrivalTime(now + MAX_TRAVEL_TIME)
						.build());

		//empty queue
		assertThat(queue.getRequestsToRetryNow(SUBMISSION_TIME + 3)).isEmpty();
	}

	@Test
	void requestMaxAgeNotExceeded_lateRetry() {
		var queue = new DrtRequestInsertionRetryQueue(params(2, 10));
		assertThat(queue.tryAddFailedRequest(request, SUBMISSION_TIME)).isTrue();

		//retry
		double now = 999999;// no guarantee the method is called every second, so let's make a very late call
		assertThat(queue.getRequestsToRetryNow(now)).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(DrtRequest.newBuilder(request)
						.latestStartTime(now + MAX_WAIT_TIME)
						.latestArrivalTime(now + MAX_TRAVEL_TIME)
						.build());
	}

	@Test
	void queueOrderMaintained() {
		var queue = new DrtRequestInsertionRetryQueue(params(2, 10));
		assertThat(queue.tryAddFailedRequest(DrtRequest.newBuilder(request).id(Id.create("a", Request.class)).build(),
				SUBMISSION_TIME)).isTrue();
		assertThat(queue.tryAddFailedRequest(DrtRequest.newBuilder(request).id(Id.create("b", Request.class)).build(),
				SUBMISSION_TIME)).isTrue();
		assertThat(queue.tryAddFailedRequest(DrtRequest.newBuilder(request).id(Id.create("c", Request.class)).build(),
				SUBMISSION_TIME)).isTrue();
		assertThat(queue.tryAddFailedRequest(DrtRequest.newBuilder(request).id(Id.create("d", Request.class)).build(),
				SUBMISSION_TIME)).isTrue();

		//too early for retry
		assertThat(queue.getRequestsToRetryNow(SUBMISSION_TIME + 1)).isEmpty();

		//order is maintained
		assertThat(queue.getRequestsToRetryNow(SUBMISSION_TIME + 2)).extracting(req -> req.getId().toString())
				.containsExactly("a", "b", "c", "d");

		//empty queue
		assertThat(queue.getRequestsToRetryNow(SUBMISSION_TIME + 3)).isEmpty();
	}

	private DrtRequestInsertionRetryParams params(int interval, double maxAge) {
		var drtRequestInsertionRetryParams = new DrtRequestInsertionRetryParams();
		drtRequestInsertionRetryParams.retryInterval = interval;
		drtRequestInsertionRetryParams.maxRequestAge = maxAge;
		return drtRequestInsertionRetryParams;
	}
}
