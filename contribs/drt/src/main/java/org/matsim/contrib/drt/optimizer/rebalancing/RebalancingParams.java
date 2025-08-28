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

import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.systems.geom_free_zones.GeometryFreeZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

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
	private int interval = 1800;// [s]

	@Parameter
	@Comment("Specifies the minimum duration (seconds) a vehicle needs to be idle in order to be available for relocation.")
	@PositiveOrZero
	private double rebalancingTimeout = 0;

	@Parameter
	@Comment("Specifies the _remaining_ duration (seconds) a vehicle needs to be idle before the next task in order to be rebalanced. " +
			"This can be used to avoid rebalancing vehicles within smaller time gaps, e.g. before a prebooked stop. " +
			"This only applies to idle times that are followed by additional tasks (i.e., inter-task gaps). " +
			"Idle times at the end of the schedule are not affected by this threshold. Default is 3600 [s].")
	@PositiveOrZero
	private double rebalancingMinIdleGap = 3600;

	@Parameter
	@Comment(
			"Minimum remaining service time of an idle/busy vehicle to be considered as rebalancable/soon-idle (respectively)."
					+ " Default is 3600 s. In general, should be higher than interval (e.g. 2 x interval).")
	@Positive
	private double minServiceTime = 2 * getInterval();// [s]

	@Parameter
	@Comment("Maximum remaining time before busy vehicle becomes idle to be considered as soon-idle vehicle."
			+ " Default is 900 s. In general should be lower than interval (e.g. 0.5 x interval)")
	@PositiveOrZero
	private double maxTimeBeforeIdle = 0.5 * getInterval();// [s], if 0 then soon-idle vehicle will not be considered

	public interface RebalancingStrategyParams {
	}

	@NotNull
	private RebalancingStrategyParams rebalancingStrategyParams;

	public enum TargetLinkSelection {random, mostCentral}

	@Parameter("zoneTargetLinkSelection")
	@Comment("Defines how the target link of a zone is determined (e.g. for rebalancing)."
			+ " Possible values are [random,mostCentral]. Default behavior is mostCentral, where all vehicles are sent to the same link.")
	@NotNull
	private TargetLinkSelection targetLinkSelection = TargetLinkSelection.mostCentral;

	private ZoneSystemParams zoneSystemParams;

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

		addDefinition(SquareGridZoneSystemParams.SET_NAME, SquareGridZoneSystemParams::new,
				() -> zoneSystemParams,
				params -> zoneSystemParams = (SquareGridZoneSystemParams)params);

		addDefinition(GISFileZoneSystemParams.SET_NAME, GISFileZoneSystemParams::new,
				() -> zoneSystemParams,
				params -> zoneSystemParams = (GISFileZoneSystemParams)params);

		addDefinition(H3GridZoneSystemParams.SET_NAME, H3GridZoneSystemParams::new,
				() -> zoneSystemParams,
				params -> zoneSystemParams = (H3GridZoneSystemParams)params);

		addDefinition(GeometryFreeZoneSystemParams.SET_NAME, GeometryFreeZoneSystemParams::new,
				() -> zoneSystemParams,
				params -> zoneSystemParams = (GeometryFreeZoneSystemParams)params);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
	}

	public RebalancingStrategyParams getRebalancingStrategyParams() {
		return rebalancingStrategyParams;
	}

	@Positive
	public int getInterval() {
		return interval;
	}

	public void setInterval(@Positive int interval) {
		this.interval = interval;
	}

	@Positive
	public double getMinServiceTime() {
		return minServiceTime;
	}

	public void setMinServiceTime(@Positive double minServiceTime) {
		this.minServiceTime = minServiceTime;
	}

	@PositiveOrZero
	public double getMaxTimeBeforeIdle() {
		return maxTimeBeforeIdle;
	}

	public void setMaxTimeBeforeIdle(@PositiveOrZero double maxTimeBeforeIdle) {
		this.maxTimeBeforeIdle = maxTimeBeforeIdle;
	}

	public @NotNull TargetLinkSelection getTargetLinkSelection() {
		return targetLinkSelection;
	}

	public void setTargetLinkSelection(@NotNull TargetLinkSelection targetLinkSelection) {
		this.targetLinkSelection = targetLinkSelection;
	}

	public double getRebalancingTimeout() {
		return rebalancingTimeout;
	}

	public void setRebalancingTimeout(double rebalancingTimeout) {
		this.rebalancingTimeout = rebalancingTimeout;
	}

	public double getRebalancingMinIdleGap() {
		return rebalancingMinIdleGap;
	}

	public void setRebalancingMinIdleGap(double rebalancingMinIdleGap) {
		this.rebalancingMinIdleGap = rebalancingMinIdleGap;
	}

	public ZoneSystemParams getZoneSystemParams() {
		return zoneSystemParams;
	}
}
