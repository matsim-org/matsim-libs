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

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider.Mode;
import org.matsim.core.config.Config;

public class AssignmentTaxiOptimizerParams extends DefaultTaxiOptimizerParams {
	public static final String SET_NAME = "AssignmentTaxiOptimizer";

	public static final String MODE = "mode";
	static final String MODE_EXP = "Specifies the formula used to calculate assgnment cost."
			+ " See comments in TaxiToRequestAssignmentCostProvider."
			+ " The default mode is ARRIVAL_TIME (used fro simulating taxis in Berlin).";
	@NotNull
	private Mode mode = Mode.ARRIVAL_TIME;

	public static final String VEH_PLANNING_HORIZON_OVERSUPPLY = "vehPlanningHorizonOversupply";
	static final String VEH_PLANNING_HORIZON_OVERSUPPLY_EXP = "Vehicle planning horizon in the case of oversupply."
			+ " Only vehicles available within the time horizon are considered in the assignment procedure."
			+ " The value should not be smaller than 'reoptimizationTimeStep'."
			+ " The default value is 120 seconds (used for simulating taxis in Berlin)."
			+ " Discussion: M. Maciejewski, J. Bischoff, K. Nagel: An assignment-based approach to efficient real-time"
			+ " city-scale taxi dispatching. IEEE Intelligent Systems, 2016.";
	@Positive
	private double vehPlanningHorizonOversupply = 120;

	public static final String VEH_PLANNING_HORIZON_UNDERSUPPLY = "vehPlanningHorizonUndersupply";
	static final String VEH_PLANNING_HORIZON_UNDERSUPPLY_EXP = "Vehicle planning horizon in the case of undersupply."
			+ " Only vehicles available within the time horizon are considered in the assignment procedure."
			+ " The value should not be smaller than 'reoptimizationTimeStep'."
			+ " The default value is 30 seconds (used for simulating taxis in Berlin)."
			+ " Discussion: M. Maciejewski, J. Bischoff, K. Nagel: An assignment-based approach to efficient real-time"
			+ " city-scale taxi dispatching. IEEE Intelligent Systems, 2016.";
	@Positive
	private double vehPlanningHorizonUndersupply = 30;

	// TODO should we adjust both the limits based on the current demand-supply relation?
	public static final String NEAREST_REQUESTS_LIMIT = "nearestRequestsLimit";
	static final String NEAREST_REQUESTS_LIMIT_EXP = "Limits the number of open requests considered in"
			+ " the cost matrix calculation to 'nearestRequestsLimit' nearest to a given vehicle (straight line)."
			+ " Used to speed up computations."
			+ " Depending on the size of fleet, values 20 to 40 make a good trade-off between computational speed and"
			+ " quality of results."
			+ " To turn off this feature - specify a sufficiently big number (not recommended)."
			+ " The default value is 40 (used for simulating taxis in Berlin).";
	@Positive
	private int nearestRequestsLimit = 40;

	public static final String NEAREST_VEHICLES_LIMIT = "nearestVehiclesLimit";
	static final String NEAREST_VEHICLES_LIMIT_EXP = "Limits the number of available vehicles considered in"
			+ " the cost matrix calculation to 'nearestRequestsLimit' nearest to a given request (straight line)."
			+ " Used to speed up computations."
			+ " Depending on the size of fleet, values 20 to 40 make a good trade-off between computational speed and"
			+ " quality of results."
			+ " To turn off this feature - specify a sufficiently big number (not recommended)."
			+ " The default value is 40 (used for simulating taxis in Berlin).";
	@Positive
	private int nearestVehiclesLimit = 40;

	public static final String NULL_PATH_COST = "nullPathCost";
	static final String NULL_PATH_COST_EXP = "Specifies the cost for vehicle-request pairs not included in the matrix"
			+ " calculation. Should be high enough to prevent choosing such assignments."
			+ " The default value is 48 * 3600 seconds (2 days) (used for simulating taxis in Berlin).";
	@Positive
	private double nullPathCost = 48 * 3600;

	public AssignmentTaxiOptimizerParams() {
		this(SET_NAME);
	}

	protected AssignmentTaxiOptimizerParams(String paramSetName) {
		super(SET_NAME, 10, true, true);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		if (vehPlanningHorizonUndersupply < getReoptimizationTimeStep()
				|| vehPlanningHorizonOversupply < getReoptimizationTimeStep()) {
			throw new RuntimeException("'vehPlanningHorizonUndersupply' and 'vehPlanningHorizonOversupply'"
					+ "must not be less than 'reoptimizationTimeStep'");
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE, MODE_EXP);
		map.put(VEH_PLANNING_HORIZON_OVERSUPPLY, VEH_PLANNING_HORIZON_OVERSUPPLY_EXP);
		map.put(VEH_PLANNING_HORIZON_UNDERSUPPLY, VEH_PLANNING_HORIZON_UNDERSUPPLY_EXP);
		map.put(NEAREST_REQUESTS_LIMIT, NEAREST_REQUESTS_LIMIT_EXP);
		map.put(NEAREST_VEHICLES_LIMIT, NEAREST_VEHICLES_LIMIT_EXP);
		map.put(NULL_PATH_COST, NULL_PATH_COST_EXP);
		return map;
	}

	/**
	 * @return {@value #MODE_EXP}
	 */
	@StringGetter(MODE)
	public Mode getMode() {
		return mode;
	}

	/**
	 * @param mode {@value #MODE_EXP}
	 */
	@StringSetter(MODE)
	public void setMode(Mode mode) {
		this.mode = mode;
	}

	/**
	 * @return {@value #VEH_PLANNING_HORIZON_OVERSUPPLY_EXP}
	 */
	@StringGetter(VEH_PLANNING_HORIZON_OVERSUPPLY)
	public double getVehPlanningHorizonOversupply() {
		return vehPlanningHorizonOversupply;
	}

	/**
	 * @param vehPlanningHorizonOversupply {@value #VEH_PLANNING_HORIZON_OVERSUPPLY_EXP}
	 */
	@StringSetter(VEH_PLANNING_HORIZON_OVERSUPPLY)
	public void setVehPlanningHorizonOversupply(double vehPlanningHorizonOversupply) {
		this.vehPlanningHorizonOversupply = vehPlanningHorizonOversupply;
	}

	/**
	 * @return {@value #VEH_PLANNING_HORIZON_UNDERSUPPLY_EXP}
	 */
	@StringGetter(VEH_PLANNING_HORIZON_UNDERSUPPLY)
	public double getVehPlanningHorizonUndersupply() {
		return vehPlanningHorizonUndersupply;
	}

	/**
	 * @param vehPlanningHorizonUndersupply {@value #VEH_PLANNING_HORIZON_UNDERSUPPLY_EXP}
	 */
	@StringSetter(VEH_PLANNING_HORIZON_UNDERSUPPLY)
	public void setVehPlanningHorizonUndersupply(double vehPlanningHorizonUndersupply) {
		this.vehPlanningHorizonUndersupply = vehPlanningHorizonUndersupply;
	}

	/**
	 * @return {@value #NEAREST_REQUESTS_LIMIT_EXP}
	 */
	@StringGetter(NEAREST_REQUESTS_LIMIT)
	public int getNearestRequestsLimit() {
		return nearestRequestsLimit;
	}

	/**
	 * @param nearestRequestsLimit {@value #NEAREST_REQUESTS_LIMIT_EXP}
	 */
	@StringSetter(NEAREST_REQUESTS_LIMIT)
	public void setNearestRequestsLimit(int nearestRequestsLimit) {
		this.nearestRequestsLimit = nearestRequestsLimit;
	}

	/**
	 * @return {@value #NEAREST_VEHICLES_LIMIT_EXP}
	 */
	@StringGetter(NEAREST_VEHICLES_LIMIT)
	public int getNearestVehiclesLimit() {
		return nearestVehiclesLimit;
	}

	/**
	 * @param nearestVehiclesLimit {@value #NEAREST_VEHICLES_LIMIT_EXP}
	 */
	@StringSetter(NEAREST_VEHICLES_LIMIT)
	public void setNearestVehiclesLimit(int nearestVehiclesLimit) {
		this.nearestVehiclesLimit = nearestVehiclesLimit;
	}

	/**
	 * @return {@value #NULL_PATH_COST_EXP}
	 */
	@StringGetter(NULL_PATH_COST)
	public double getNullPathCost() {
		return nullPathCost;
	}

	/**
	 * @param nullPathCost {@value #NULL_PATH_COST_EXP}
	 */
	@StringSetter(NULL_PATH_COST)
	public void setNullPathCost(double nullPathCost) {
		this.nullPathCost = nullPathCost;
	}
}
