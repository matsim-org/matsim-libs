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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author michalm
 */
public final class MinCostFlowRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "minCostFlowRebalancingStrategy";

	public enum ZonalDemandAggregatorType {
		PreviousIteration, TimeDependentActivityBased, EqualVehicleDensity, FirstActivityCount,
		FleetSizeWeightedByPopulationShare
	}

	public static final String TARGET_ALPHA = "targetAlpha";
	static final String TARGET_ALPHA_EXP = "alpha coefficient in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty mileage.";

	public static final String TARGET_BETA = "targetBeta";
	static final String TARGET_BETA_EXP = "beta constant in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty mileage.";

	public static final String ZONAL_DEMAND_AGGREGATOR_TYPE = "zonalDemandAggregatorType";
	static final String ZONAL_DEMAND_AGGREGATOR_TYPE_EXP = "Defines the methodology for demand estimation. Can be one of either [PreviousIteration, TimeDependentActivityBased, EqualVehicleDensity, FirstActivityCount] Current default is PreviousIteration";

	@PositiveOrZero
	private double targetAlpha = Double.NaN;

	@PositiveOrZero
	private double targetBeta = Double.NaN;

	@NotNull
	private MinCostFlowRebalancingStrategyParams.ZonalDemandAggregatorType zonalDemandAggregatorType = ZonalDemandAggregatorType.PreviousIteration;

	public MinCostFlowRebalancingStrategyParams() {
		super(SET_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(TARGET_ALPHA, TARGET_ALPHA_EXP);
		map.put(TARGET_BETA, TARGET_BETA_EXP);
		map.put(ZONAL_DEMAND_AGGREGATOR_TYPE, ZONAL_DEMAND_AGGREGATOR_TYPE_EXP);
		return map;
	}

	/**
	 * @return -- {@value #TARGET_ALPHA_EXP}
	 */
	@StringGetter(TARGET_ALPHA)
	public double getTargetAlpha() {
		return targetAlpha;
	}

	/**
	 * @param targetAlpha -- {@value #TARGET_ALPHA_EXP}
	 */
	@StringSetter(TARGET_ALPHA)
	public void setTargetAlpha(double targetAlpha) {
		this.targetAlpha = targetAlpha;
	}

	/**
	 * @return -- {@value #TARGET_BETA_EXP}
	 */
	@StringGetter(TARGET_BETA)
	public double getTargetBeta() {
		return targetBeta;
	}

	/**
	 * @param targetBeta -- {@value #TARGET_BETA_EXP}
	 */
	@StringSetter(TARGET_BETA)
	public void setTargetBeta(double targetBeta) {
		this.targetBeta = targetBeta;
	}

	/**
	 * @return -- {@value #ZONAL_DEMAND_AGGREGATOR_TYPE_EXP}
	 */
	@StringGetter(ZONAL_DEMAND_AGGREGATOR_TYPE)
	public ZonalDemandAggregatorType getZonalDemandAggregatorType() {
		return zonalDemandAggregatorType;
	}

	/**
	 * @param aggregatorType -- {@value #ZONAL_DEMAND_AGGREGATOR_TYPE_EXP}
	 */
	@StringSetter(ZONAL_DEMAND_AGGREGATOR_TYPE)
	public void setZonalDemandAggregatorType(ZonalDemandAggregatorType aggregatorType) {
		this.zonalDemandAggregatorType = aggregatorType;
	}
}
