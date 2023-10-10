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
import java.util.LinkedList;
import java.util.List;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public final class DefaultRequestQueue<R extends PassengerRequest> implements RequestQueue<R> {
	public static <R extends PassengerRequest> DefaultRequestQueue<R> withLimitedAdvanceRequestPlanningHorizon(
			double planningHorizon) {
		//all immediate and selected advance (i.e. starting within the planning horizon) requests are scheduled
		return new DefaultRequestQueue<>(planningHorizon);
	}

	public static <R extends PassengerRequest> DefaultRequestQueue<R> withInfiniteAdvanceRequestPlanningHorizon() {
		return new DefaultRequestQueue<>(Double.POSITIVE_INFINITY);//all immediate and advance requests are scheduled
	}

	public static <R extends PassengerRequest> DefaultRequestQueue<R> withNoAdvanceRequestPlanningHorizon() {
		return new DefaultRequestQueue<>(0);//immediate requests only
	}

	//all requests in the planning horizon (also includes old requests: never scheduled or unscheduled)
	private final Collection<R> schedulableRequests = new LinkedList<>();

	//advance requests that are not in the planning horizon
	private final List<R> postponedRequests = new LinkedList<>();

	private final double planningHorizon;

	private double lastTimeStep = -Double.MAX_VALUE;

	private DefaultRequestQueue(double planningHorizon) {
		this.planningHorizon = planningHorizon;
	}

	public void updateQueuesOnNextTimeSteps(double currentTime) {
		lastTimeStep = currentTime;
		
		var iterator = postponedRequests.iterator();
		
		while (iterator.hasNext()) {
			R request = iterator.next();
			
			if (isSchedulable(request)) {
				schedulableRequests.add(request);
				iterator.remove();
			}
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
