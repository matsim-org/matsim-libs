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

public interface DynActivity extends DynAction {
	// Double.POSITIVE_INFINITY == stop simulating the agent (permanent sleep)
	public static final double END_ACTIVITY_LATER = Double.MAX_VALUE;

	String getActivityType();

	double getEndTime();

	/**
	 * DynActivity is performed at steps: now == beginTime + 1, ..., endTime
	 * 
	 * @param now
	 *            current time
	 */
	// TODO this method may possibly be pulled up to DynAction since even when travelling (either by
	// PuT or PrT) an agent may think, talk (also on the phone), collaborate etc.
	void doSimStep(double now);
}
