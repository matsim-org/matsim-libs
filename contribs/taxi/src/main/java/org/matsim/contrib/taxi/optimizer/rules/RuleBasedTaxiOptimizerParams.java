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

package org.matsim.contrib.taxi.optimizer.rules;

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter.Goal;

public class RuleBasedTaxiOptimizerParams extends DefaultTaxiOptimizerParams {
	public static final String GOAL = "goal";
	static final String GOAL_EXP = "Optimisation goal, one of:"
			+ " MIN_WAIT_TIME (aka 'nearest idle taxi', a request-initiated dispatch),"
			+ " MIN_PICKUP_TIME (aka 'nearest open request', a vehicle-initiated dispatch),"
			+ " DEMAND_SUPPLY_EQUIL (aka 'demand-supply balancing' or 'nearest idle taxi/nearest open request';"
			+ " switches between two modes depending on the demand-supply balance)."
			+ " The default and recommended value is DEMAND_SUPPLY_EQUIL."
			+ " See: M. Maciejewski, J. Bischoff, K. Nagel: An assignment-based approach to efficient real-time"
			+ " city-scale taxi dispatching. IEEE Intelligent Systems, 2016.";
	@NotNull
	private Goal goal = Goal.DEMAND_SUPPLY_EQUIL;

	public static final String NEAREST_REQUESTS_LIMIT = "nearestRequestsLimit";
	static final String NEAREST_REQUESTS_LIMIT_EXP = "Limits the number of open requests considered during"
			+ " a vehicle-initiated dispatch to 'nearestRequestsLimit' nearest to a given location"
			+ " using pre-calculated distances between zone centroids."
			+ " Used to speed up computations."
			+ " Values 20 to 40 make a good trade-off between computational speed and quality of results."
			+ " To turn off this feature - specify a sufficiently big number."
			+ " The default value is 30.";
	@Positive
	private int nearestRequestsLimit = 30;

	public static final String NEAREST_VEHICLES_LIMIT = "nearestVehiclesLimit";
	static final String NEAREST_VEHICLES_LIMIT_EXP = "Limits the number of idle vehicles considered during"
			+ " a request-initiated dispatch to 'nearestVehiclesLimit' nearest to a given location"
			+ " using pre-calculated distances between zone centroids."
			+ " Used to speed up computations."
			+ " Values 20 to 40 make a good trade-off between computational speed and quality of results."
			+ " To turn off this feature - specify a sufficiently big number."
			+ " The default value is 30.";
	@Positive
	private int nearestVehiclesLimit = 30;

	public static final String CELL_SIZE = "cellSize";
	static final String CELL_SIZE_EXP = "The side length of square zones used in zonal registers of idle vehicles"
			+ " and open requests. The default value is 1000 m. This value is good for urban areas. For large areas"
			+ " with sparsely distributed taxis and low taxi demand, you may consider using a bigger cell size."
			+ " On the other hand, if 'nearestRequestsLimit' or 'nearestVehiclesLimit' are very low,"
			+ " a smaller cell size may work better.";
	@Positive
	private double cellSize = 1000;

	public RuleBasedTaxiOptimizerParams(Configuration optimizerConfig) {
		super(optimizerConfig, false, false);

		goal = Goal.valueOf(optimizerConfig.getString(GOAL));

		nearestRequestsLimit = optimizerConfig.getInt(NEAREST_REQUESTS_LIMIT);
		nearestVehiclesLimit = optimizerConfig.getInt(NEAREST_VEHICLES_LIMIT);

		cellSize = optimizerConfig.getDouble(CELL_SIZE);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(GOAL, GOAL_EXP);
		map.put(NEAREST_REQUESTS_LIMIT, NEAREST_REQUESTS_LIMIT_EXP);
		map.put(NEAREST_VEHICLES_LIMIT, NEAREST_VEHICLES_LIMIT_EXP);
		map.put(CELL_SIZE, CELL_SIZE_EXP);
		return map;
	}

	/**
	 * @return {@value #GOAL_EXP}
	 */
	@StringGetter(GOAL)
	public Goal getGoal() {
		return goal;
	}

	/**
	 * @param goal {@value #GOAL_EXP}
	 */
	@StringSetter(GOAL)
	public void setGoal(Goal goal) {
		this.goal = goal;
	}

	/**
	 * @return {@value #NEAREST_REQUESTS_LIMIT_EXP}
	 */
	@StringGetter(NEAREST_REQUESTS_LIMIT)
	public Integer getNearestRequestsLimit() {
		return nearestRequestsLimit;
	}

	/**
	 * @param nearestRequestsLimit {@value #NEAREST_REQUESTS_LIMIT_EXP}
	 */
	@StringSetter(NEAREST_REQUESTS_LIMIT)
	public void setNearestRequestsLimit(Integer nearestRequestsLimit) {
		this.nearestRequestsLimit = nearestRequestsLimit;
	}

	/**
	 * @return {@value #NEAREST_VEHICLES_LIMIT_EXP}
	 */
	@StringGetter(NEAREST_VEHICLES_LIMIT)
	public Integer getNearestVehiclesLimit() {
		return nearestVehiclesLimit;
	}

	/**
	 * @param nearestVehiclesLimit {@value #NEAREST_VEHICLES_LIMIT_EXP}
	 */
	@StringSetter(NEAREST_VEHICLES_LIMIT)
	public void setNearestVehiclesLimit(Integer nearestVehiclesLimit) {
		this.nearestVehiclesLimit = nearestVehiclesLimit;
	}

	/**
	 * @return {@value #CELL_SIZE_EXP}
	 */
	@StringGetter(CELL_SIZE)
	public Double getCellSize() {
		return cellSize;
	}

	/**
	 * @param cellSize {@value #CELL_SIZE_EXP}
	 */
	@StringSetter(CELL_SIZE)
	public void setCellSize(Double cellSize) {
		this.cellSize = cellSize;
	}
}
