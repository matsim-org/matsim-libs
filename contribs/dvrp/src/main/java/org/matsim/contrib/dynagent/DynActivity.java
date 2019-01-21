/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent;

/**
 * Design suggestions:
 * <p>
 * The constructor is called before ActivityStartEvent and finalizeAction() is called after ActivityEndEvent.
 * These two methods are meant for initialising and cleaning up the activity and should not be used for simulating it.
 * For that purpose, use DynActivity.doSimStep(timeStep) with a special handling for the first/last time step.
 */
public interface DynActivity extends DynAction {
	double END_ACTIVITY_LATER = Double.MAX_VALUE;

	String getActivityType();

	/**
	 * Called in each time step after doSimStep().
	 *
	 * @return There are three possible outcomes:
	 * <ul>
	 * <li>{@code endTime == Double.POSITIVE_INFINITY} ==> stop simulating the agent (permanent sleep)</li>
	 * <li>{@code endTime <= timeStep} ==> end simulating the activity in this time step</li>
	 * <li>{@code endTime > timeStep} ==> continue simulating the activity (e.g. END_ACTIVITY_LATER could be used)</li>
	 * </ul>
	 */
	double getEndTime();

	/**
	 * DynActivity is performed at steps: now == beginTime + 1, ..., endTime
	 *
	 * @param now current time
	 */
	// TODO this method may possibly be pulled up to DynAction since even when travelling (either by
	// PuT or PrT) an agent may think, talk (also on the phone), collaborate etc.
	void doSimStep(double now);
}
