/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing;

import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import com.google.common.base.Preconditions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author michalm
 */
public final class RebalancingParams extends ReflectiveConfigGroupWithConfigurableParameterSets {

	public static final String SET_NAME = "rebalancing";

	@Parameter
	@Comment("Specifies how often empty vehicle rebalancing is executed."
			+ " Must be positive. Default is 1800 s. Expects an Integer Value")
	@Positive
	public int interval = 1800;// [s]

	@Parameter
	@Comment(
			"Minimum remaining service time of an idle/busy vehicle to be considered as rebalancable/soon-idle (respectively)."
					+ " Default is 3600 s. In general, should be higher than interval (e.g. 2 x interval).")
	@Positive
	public double minServiceTime = 2 * interval;// [s]

	@Parameter
	@Comment("Maximum remaining time before busy vehicle becomes idle to be considered as soon-idle vehicle."
			+ " Default is 900 s. In general should be lower than interval (e.g. 0.5 x interval)")
	@PositiveOrZero
	public double maxTimeBeforeIdle = 0.5 * interval;// [s], if 0 then soon-idle vehicle will not be considered

	public interface RebalancingStrategyParams {
	}

	@NotNull
	private RebalancingStrategyParams rebalancingStrategyParams;

	public RebalancingParams() {
		super(SET_NAME);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//rebalancing strategies (one of: min cost flow, feedforward, plus one)
		addDefinition(MinCostFlowRebalancingStrategyParams.SET_NAME, MinCostFlowRebalancingStrategyParams::new,
				() -> (ConfigGroup)rebalancingStrategyParams,
				params -> rebalancingStrategyParams = (RebalancingStrategyParams)params);
		addDefinition(FeedforwardRebalancingStrategyParams.SET_NAME, FeedforwardRebalancingStrategyParams::new,
				() -> (ConfigGroup)rebalancingStrategyParams,
				params -> rebalancingStrategyParams = (RebalancingStrategyParams)params);
		addDefinition(PlusOneRebalancingStrategyParams.SET_NAME, PlusOneRebalancingStrategyParams::new,
				() -> (ConfigGroup)rebalancingStrategyParams,
				params -> rebalancingStrategyParams = (RebalancingStrategyParams)params);
		addDefinition(CustomRebalancingStrategyParams.SET_NAME, CustomRebalancingStrategyParams::new,
			() -> (ConfigGroup)rebalancingStrategyParams,
			params -> rebalancingStrategyParams = (RebalancingStrategyParams)params);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Preconditions.checkArgument(minServiceTime > maxTimeBeforeIdle,
				"minServiceTime must be greater than maxTimeBeforeIdle");
	}

	public RebalancingStrategyParams getRebalancingStrategyParams() {
		return rebalancingStrategyParams;
	}
}
