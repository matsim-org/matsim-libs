/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.assignment;

import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider.Mode;
import org.matsim.core.config.Config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class AssignmentTaxiOptimizerParams extends AbstractTaxiOptimizerParams {
	public static final String SET_NAME = "AssignmentTaxiOptimizer";

	@Parameter
	@Comment("Specifies the formula used to calculate assgnment cost."
			+ " See comments in TaxiToRequestAssignmentCostProvider."
			+ " The default mode is ARRIVAL_TIME (used fro simulating taxis in Berlin).")
	@NotNull
	private Mode mode = Mode.ARRIVAL_TIME;

	@Parameter
	@Comment("Vehicle planning horizon in the case of oversupply."
			+ " Only vehicles available within the time horizon are considered in the assignment procedure."
			+ " The value should not be smaller than 'reoptimizationTimeStep'."
			+ " The default value is 120 seconds (used for simulating taxis in Berlin)."
			+ " Discussion: M. Maciejewski, J. Bischoff, K. Nagel: An assignment-based approach to efficient real-time"
			+ " city-scale taxi dispatching. IEEE Intelligent Systems, 2016.")
	@Positive
	private double vehPlanningHorizonOversupply = 120;

	@Parameter
	@Comment("Vehicle planning horizon in the case of undersupply."
			+ " Only vehicles available within the time horizon are considered in the assignment procedure."
			+ " The value should not be smaller than 'reoptimizationTimeStep'."
			+ " The default value is 30 seconds (used for simulating taxis in Berlin)."
			+ " Discussion: M. Maciejewski, J. Bischoff, K. Nagel: An assignment-based approach to efficient real-time"
			+ " city-scale taxi dispatching. IEEE Intelligent Systems, 2016.")
	@Positive
	private double vehPlanningHorizonUndersupply = 30;

	// TODO should we adjust both the limits based on the current demand-supply relation?
	@Parameter
	@Comment("Limits the number of open requests considered in"
			+ " the cost matrix calculation to 'nearestRequestsLimit' nearest to a given vehicle (straight line)."
			+ " Used to speed up computations."
			+ " Depending on the size of fleet, values 20 to 40 make a good trade-off between computational speed and"
			+ " quality of results."
			+ " To turn off this feature - specify a sufficiently big number (not recommended)."
			+ " The default value is 40 (used for simulating taxis in Berlin).")
	@Positive
	private int nearestRequestsLimit = 40;

	@Parameter
	@Comment("Limits the number of available vehicles considered in"
			+ " the cost matrix calculation to 'nearestRequestsLimit' nearest to a given request (straight line)."
			+ " Used to speed up computations."
			+ " Depending on the size of fleet, values 20 to 40 make a good trade-off between computational speed and"
			+ " quality of results."
			+ " To turn off this feature - specify a sufficiently big number (not recommended)."
			+ " The default value is 40 (used for simulating taxis in Berlin).")
	@Positive
	private int nearestVehiclesLimit = 40;

	@Parameter
	@Comment("Specifies the cost for vehicle-request pairs not included in the matrix"
			+ " calculation. Should be high enough to prevent choosing such assignments."
			+ " The default value is 48 * 3600 seconds (2 days) (used for simulating taxis in Berlin).")
	@Positive
	private double nullPathCost = 48 * 3600;

	@Parameter
	@Comment(REOPTIMIZATION_TIME_STEP_EXP)
	@Positive
	private int reoptimizationTimeStep = 10;

	public AssignmentTaxiOptimizerParams() {
		super(SET_NAME, true, true);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		if (getVehPlanningHorizonUndersupply() < getReoptimizationTimeStep()
				|| getVehPlanningHorizonOversupply() < getReoptimizationTimeStep()) {
			throw new RuntimeException("'vehPlanningHorizonUndersupply' and 'vehPlanningHorizonOversupply'"
					+ "must not be less than 'reoptimizationTimeStep'");
		}
	}

	/**
	 * {@value #REOPTIMIZATION_TIME_STEP_EXP}
	 */
	public int getReoptimizationTimeStep() {
		return reoptimizationTimeStep;
	}

	public @NotNull Mode getMode() {
		return mode;
	}

	public void setMode(@NotNull Mode mode) {
		this.mode = mode;
	}

	@Positive
	public double getVehPlanningHorizonOversupply() {
		return vehPlanningHorizonOversupply;
	}

	public void setVehPlanningHorizonOversupply(@Positive double vehPlanningHorizonOversupply) {
		this.vehPlanningHorizonOversupply = vehPlanningHorizonOversupply;
	}

	@Positive
	public double getVehPlanningHorizonUndersupply() {
		return vehPlanningHorizonUndersupply;
	}

	public void setVehPlanningHorizonUndersupply(@Positive double vehPlanningHorizonUndersupply) {
		this.vehPlanningHorizonUndersupply = vehPlanningHorizonUndersupply;
	}

	@Positive
	public int getNearestRequestsLimit() {
		return nearestRequestsLimit;
	}

	public void setNearestRequestsLimit(@Positive int nearestRequestsLimit) {
		this.nearestRequestsLimit = nearestRequestsLimit;
	}

	@Positive
	public int getNearestVehiclesLimit() {
		return nearestVehiclesLimit;
	}

	public void setNearestVehiclesLimit(@Positive int nearestVehiclesLimit) {
		this.nearestVehiclesLimit = nearestVehiclesLimit;
	}

	@Positive
	public double getNullPathCost() {
		return nullPathCost;
	}

	public void setNullPathCost(@Positive double nullPathCost) {
		this.nullPathCost = nullPathCost;
	}

	public void setReoptimizationTimeStep(@Positive int reoptimizationTimeStep) {
		this.reoptimizationTimeStep = reoptimizationTimeStep;
	}
}
