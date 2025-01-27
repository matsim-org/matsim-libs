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

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author michalm
 */
public final class MinCostFlowRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "minCostFlowRebalancingStrategy";

	@Parameter
	@Comment("alpha coefficient in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty mileage.")
	@PositiveOrZero
	public double targetAlpha = Double.NaN;

	@Parameter
	@Comment("beta constant in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty mileage.")
	@PositiveOrZero
	public double targetBeta = Double.NaN;

	public enum RebalancingTargetCalculatorType {
		EstimatedDemand, EqualRebalancableVehicleDistribution, EqualVehicleDensity, EqualVehiclesToPopulationRatio
	}

	@Parameter
	@Comment("Defines the calculator used for computing rebalancing targets per each zone"
			+ " (i.e. number of the desired vehicles)."
			+ " Can be one of [EstimatedDemand, EqualRebalancableVehicleDistribution,"
			+ " EqualVehicleDensity, EqualVehiclesToPopulationRatio]."
			+ " Current default is EstimatedDemand")
	@NotNull
	public RebalancingTargetCalculatorType rebalancingTargetCalculatorType = RebalancingTargetCalculatorType.EstimatedDemand;


	public enum TargetCoefficientSource {
		Static, FromZoneAttribute, FromZoneAttributeOrStatic
	}

	@Parameter
	@Comment("Defines whether the alpha and beta of the target function should be"
			+ " [Static] or [FromZoneAttribute] in which case alpha and beta can be provided per zone as an attribute."
			+ " [FromZoneAttributeOrStatic] will fall back to the static coefficients if no attribute is found for a given zone."
			+ " Use " + MinCostFlowRebalancingStrategy.REBALANCING_ZONAL_TARGET_ALPHA + " and " + MinCostFlowRebalancingStrategy.REBALANCING_ZONAL_TARGET_BETA
			+ " to set values accordingly.")
	public TargetCoefficientSource targetCoefficientSource = TargetCoefficientSource.Static;

	public enum ZonalDemandEstimatorType {PreviousIterationDemand, None}

	@Parameter
	@Comment("Defines the methodology for demand estimation."
			+ " Can be one of [PreviousIterationDemand, None]. Current default is PreviousIterationDemand")
	@NotNull
	public ZonalDemandEstimatorType zonalDemandEstimatorType = ZonalDemandEstimatorType.PreviousIterationDemand;

	@Parameter
	@Comment("Defines the time horizon for predicting the demand."
			+ " Used when 'zonalDemandEstimatorType' is not set to 'None'."
			+ " Default value is 1800 s.")
	@PositiveOrZero
	public int demandEstimationPeriod = 1800;

	public MinCostFlowRebalancingStrategyParams() {
		super(SET_NAME);
	}
}
