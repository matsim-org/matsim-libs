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

package org.matsim.contrib.dvrp.passenger;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeSet;

import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class RequestQueue<R extends PassengerRequest> {
	public enum RequestStatus {oldRequest, newRequest, prescheduledRequest}

	public static final class RequestEntry<R extends PassengerRequest> {
		private final R request;
		private RequestStatus status;

		public RequestEntry(R request, RequestStatus status) {
			this.request = request;
			this.status = status;
		}

		public R getRequest() {
			return request;
		}

		public RequestStatus getStatus() {
			return status;
		}
	}

	public static <R extends PassengerRequest> RequestQueue<R> withLimitedAdvanceRequestPlanningHorizon(
			double planningHorizon) {
		//all immediate and selected advance (i.e. starting within the planning horizon) requests are scheduled
		return new RequestQueue<R>(planningHorizon);
	}

	public static <R extends PassengerRequest> RequestQueue<R> withInfiniteAdvanceRequestPlanningHorizon() {
		return new RequestQueue<R>(Double.POSITIVE_INFINITY);//all immediate and advance requests are scheduled
	}

	public static <R extends PassengerRequest> RequestQueue<R> withNoAdvanceRequestPlanningHorizon() {
		return new RequestQueue<R>(0);//immediate requests only
	}

	private static final Comparator<PassengerRequest> ABSOLUTE_COMPARATOR = Comparator.comparing(
			PassengerRequest::getEarliestStartTime)
			.thenComparing(PassengerRequest::getLatestStartTime)
			.thenComparing(Request::getSubmissionTime)
			.thenComparing(Request::getId);

	//all requests in the planning horizon (also includes old requests: never scheduled or unscheduled)
	private final Collection<R> schedulableRequests = new TreeSet<>(ABSOLUTE_COMPARATOR);

	//advance requests that are not in the planning horizon
	private final Queue<R> postponedRequests = new PriorityQueue<>(ABSOLUTE_COMPARATOR);

	private final double planningHorizon;

	private double lastTimeStep = -Double.MAX_VALUE;

	private RequestQueue(double planningHorizon) {
		this.planningHorizon = planningHorizon;
	}

	public void updateQueuesOnNextTimeSteps(double currentTime) {
		lastTimeStep = currentTime;
		while (!postponedRequests.isEmpty() && isSchedulable(postponedRequests.peek())) {
			schedulableRequests.add(postponedRequests.poll());
		}
	}

	public void addRequest(R request) {
		(isSchedulable(request) ? schedulableRequests : postponedRequests).add(request);
	}

	private boolean isSchedulable(R request) {
		return request.getEarliestStartTime() <= lastTimeStep + planningHorizon;
	}

	/**
	 * Assumes external code can modify schedulableRequests (e.g. remove scheduled requests and add unscheduled ones)
	 *
	 * @return requests to be inserted into vehicle schedules
	 */
	public Collection<R> getSchedulableRequests() {
		return schedulableRequests;
	}
}
