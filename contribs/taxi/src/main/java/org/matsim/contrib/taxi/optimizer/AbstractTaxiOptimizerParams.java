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

package org.matsim.contrib.taxi.optimizer;

import java.util.Map;

import jakarta.validation.constraints.Positive;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author michalm
 */
public abstract class AbstractTaxiOptimizerParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public static final String REOPTIMIZATION_TIME_STEP = "reoptimizationTimeStep";
	protected static final String REOPTIMIZATION_TIME_STEP_EXP = "Specifies how often the reoptimization algorithm is executed."
			+ " Must be a positive integer value. Smaller values mean lower reaction time."
			+ " However, algorithms that find more than 1 taxi-request matching in each run,"
			+ " such as AssignmentTaxiOptimizer, may produce better results"
			+ " if new requests are buffered over a longer period, e.g. 10 or 30 seconds."
			+ " Therefore, the default value is algorithm dependent.";

	public final boolean doUnscheduleAwaitingRequests;// PLANNED requests
	public final boolean doUpdateTimelines;// STARTED+PLANNED requests

	protected AbstractTaxiOptimizerParams(String paramSetName, boolean doUnscheduleAwaitingRequests,
			boolean doUpdateTimelines) {
		super(paramSetName);
		this.doUnscheduleAwaitingRequests = doUnscheduleAwaitingRequests;
		this.doUpdateTimelines = doUpdateTimelines;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(REOPTIMIZATION_TIME_STEP, REOPTIMIZATION_TIME_STEP_EXP);
		return map;
	}

	/**
	 * @return {@value #REOPTIMIZATION_TIME_STEP_EXP}
	 */
	@Positive
	public abstract int getReoptimizationTimeStep();
}
