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

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import com.google.common.base.Preconditions;

/**
 * @author michalm
 */
public final class RebalancingParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
	public interface RebalancingStrategyParams {
	}

	public static final String SET_NAME = "rebalancing";

	public static final String INTERVAL = "interval";
	static final String INTERVAL_EXP = "Specifies how often empty vehicle rebalancing is executed."
			+ " Must be positive. Default is 1800 s. Expects an Integer Value";

	public static final String MIN_SERVICE_TIME = "minServiceTime";
	static final String MIN_SERVICE_TIME_EXP = //
			"Minimum remaining service time of an idle/busy vehicle to be considered as rebalancable/soon-idle (respectively)."
					+ " Default is 3600 s. In general, should be higher than interval (e.g. 2 x interval).";

	public static final String MAX_TIME_BEFORE_IDLE = "maxTimeBeforeIdle";
	static final String MAX_TIME_BEFORE_IDLE_EXP = //
			"Maximum remaining time before busy vehicle becomes idle to be considered as soon-idle vehicle."
					+ " Default is 900 s. In general should be lower than interval (e.g. 0.5 x interval)";

	@Positive
	private int interval = 1800;// [s]

	@Positive
	private double minServiceTime = 2 * interval;// [s]

	@PositiveOrZero
	private double maxTimeBeforeIdle = 0.5 * interval;// [s], if 0 then soon-idle vehicle will not be considered

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
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Preconditions.checkArgument(getMinServiceTime() > getMaxTimeBeforeIdle(),
				RebalancingParams.MIN_SERVICE_TIME + "must be greater than" + RebalancingParams.MAX_TIME_BEFORE_IDLE);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(INTERVAL, INTERVAL_EXP);
		map.put(MIN_SERVICE_TIME, MIN_SERVICE_TIME_EXP);
		map.put(MAX_TIME_BEFORE_IDLE, MAX_TIME_BEFORE_IDLE_EXP);
		return map;
	}

	/**
	 * @return -- {@value #INTERVAL_EXP}
	 */
	@StringGetter(INTERVAL)
	public int getInterval() {
		return interval;
	}

	/**
	 * @param interval -- {@value #INTERVAL_EXP}
	 */
	@StringSetter(INTERVAL)
	public void setInterval(int interval) {
		this.interval = interval;
	}

	/**
	 * @return -- {@value #MIN_SERVICE_TIME_EXP}
	 */
	@StringGetter(MIN_SERVICE_TIME)
	public double getMinServiceTime() {
		return minServiceTime;
	}

	/**
	 * @param minServiceTime -- {@value #MIN_SERVICE_TIME_EXP}
	 */
	@StringSetter(MIN_SERVICE_TIME)
	public void setMinServiceTime(double minServiceTime) {
		this.minServiceTime = minServiceTime;
	}

	/**
	 * @return -- {@value #MAX_TIME_BEFORE_IDLE_EXP}
	 */
	@StringGetter(MAX_TIME_BEFORE_IDLE)
	public double getMaxTimeBeforeIdle() {
		return maxTimeBeforeIdle;
	}

	/**
	 * @param maxTimeBeforeIdle-- {@value #MAX_TIME_BEFORE_IDLE_EXP}
	 */
	@StringSetter(MAX_TIME_BEFORE_IDLE)
	public void setMaxTimeBeforeIdle(double maxTimeBeforeIdle) {
		this.maxTimeBeforeIdle = maxTimeBeforeIdle;
	}

	public RebalancingStrategyParams getRebalancingStrategyParams() {
		return rebalancingStrategyParams;
	}
}
