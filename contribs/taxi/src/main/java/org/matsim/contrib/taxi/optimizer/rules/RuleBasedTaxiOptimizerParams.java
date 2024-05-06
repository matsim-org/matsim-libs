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

import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter.Goal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class RuleBasedTaxiOptimizerParams extends AbstractTaxiOptimizerParams {
	public static final String SET_NAME = "RuleBasedTaxiOptimizer";

	@Parameter
	@Comment("Optimisation goal, one of:"
			+ " MIN_WAIT_TIME (aka 'nearest idle taxi', a request-initiated dispatch),"
			+ " MIN_PICKUP_TIME (aka 'nearest open request', a vehicle-initiated dispatch),"
			+ " DEMAND_SUPPLY_EQUIL (aka 'demand-supply balancing' or 'nearest idle taxi/nearest open request';"
			+ " switches between two modes depending on the demand-supply balance)."
			+ " The default and recommended value is DEMAND_SUPPLY_EQUIL."
			+ " See: M. Maciejewski, J. Bischoff, K. Nagel: An assignment-based approach to efficient real-time"
			+ " city-scale taxi dispatching. IEEE Intelligent Systems, 2016.")
	@NotNull
	public Goal goal = Goal.DEMAND_SUPPLY_EQUIL;

	@Parameter
	@Comment("Limits the number of open requests considered during"
			+ " a vehicle-initiated dispatch to 'nearestRequestsLimit' nearest to a given location"
			+ " using pre-calculated distances between zone centroids."
			+ " Used to speed up computations."
			+ " Values 20 to 40 make a good trade-off between computational speed and quality of results."
			+ " To turn off this feature - specify a sufficiently big number (not recommended)."
			+ " The default value is 30.")
	@Positive
	public int nearestRequestsLimit = 30;

	@Parameter
	@Comment("Limits the number of idle vehicles considered during"
			+ " a request-initiated dispatch to 'nearestVehiclesLimit' nearest to a given location"
			+ " using pre-calculated distances between zone centroids."
			+ " Used to speed up computations."
			+ " Values 20 to 40 make a good trade-off between computational speed and quality of results."
			+ " To turn off this feature - specify a sufficiently big number (not recommended)."
			+ " The default value is 30.")
	@Positive
	public int nearestVehiclesLimit = 30;

	private ZoneSystemParams zoneSystemParams = new SquareGridZoneSystemParams();

	private void initSingletonParameterSets() {

		//insertion search params (one of: extensive, selective, repeated selective)
		addDefinition(SquareGridZoneSystemParams.SET_NAME, SquareGridZoneSystemParams::new,
			() -> zoneSystemParams,
			params -> zoneSystemParams = (SquareGridZoneSystemParams)params);

		addDefinition(GISFileZoneSystemParams.SET_NAME, GISFileZoneSystemParams::new,
			() -> zoneSystemParams,
			params -> zoneSystemParams = (GISFileZoneSystemParams)params);

		addDefinition(H3GridZoneSystemParams.SET_NAME, H3GridZoneSystemParams::new,
			() -> zoneSystemParams,
			params -> zoneSystemParams = (H3GridZoneSystemParams)params);
	}

	/**
	 * {@value #REOPTIMIZATION_TIME_STEP_EXP}
	 */
	@Parameter
	@Comment(REOPTIMIZATION_TIME_STEP_EXP)
	@Positive
	public int reoptimizationTimeStep = 1;

	public RuleBasedTaxiOptimizerParams() {
		super(SET_NAME, false, false);
		initSingletonParameterSets();
		initDefault();
	}

	private void initDefault() {
		((SquareGridZoneSystemParams) zoneSystemParams).cellSize = 1000.;
	}

	public int getReoptimizationTimeStep() {
		return reoptimizationTimeStep;
	}

	public ZoneSystemParams getZoneSystemParams() {
		return zoneSystemParams;
	}
}
