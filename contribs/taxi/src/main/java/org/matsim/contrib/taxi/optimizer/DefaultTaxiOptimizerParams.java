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

import javax.validation.constraints.Positive;

import org.apache.commons.configuration.Configuration;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author michalm
 */
public class DefaultTaxiOptimizerParams extends ReflectiveConfigGroup {
	public static final String REOPTIMIZATION_TIME_STEP = "reoptimizationTimeStep";
	static final String REOPTIMIZATION_TIME_STEP_EXP = "Specifies how often the reoptimization algorithm is executed."
			+ " Must be a positive integer value. Default is 1 s (low reaction time)."
			+ " However, some algorithms, such as AssignmentTaxiOptimizer, may produce better results"
			+ " if new requests are buffered over a longer period, e.g. 10 or 30 seconds.";
	@Positive
	private int reoptimizationTimeStep = 1;

	public final boolean doUnscheduleAwaitingRequests;// PLANNED requests
	public final boolean doUpdateTimelines;// STARTED+PLANNED requests

	public DefaultTaxiOptimizerParams(Configuration optimizerConfig, boolean doUnscheduleAwaitingRequests,
			boolean doUpdateTimelines) {
		super(null);//FIXME pass it from the subclass

		this.doUnscheduleAwaitingRequests = doUnscheduleAwaitingRequests;
		this.doUpdateTimelines = doUpdateTimelines;

		reoptimizationTimeStep = optimizerConfig.getInt(REOPTIMIZATION_TIME_STEP, 1);
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
	@StringGetter(REOPTIMIZATION_TIME_STEP)
	public int getReoptimizationTimeStep() {
		return reoptimizationTimeStep;
	}

	/**
	 * @param reoptimizationTimeStep {@value #REOPTIMIZATION_TIME_STEP_EXP}
	 */
	@StringSetter(REOPTIMIZATION_TIME_STEP)
	public void setReoptimizationTimeStep(int reoptimizationTimeStep) {
		this.reoptimizationTimeStep = reoptimizationTimeStep;
	}
}
