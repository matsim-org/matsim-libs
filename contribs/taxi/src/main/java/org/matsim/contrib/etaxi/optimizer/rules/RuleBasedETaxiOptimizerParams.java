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

package org.matsim.contrib.etaxi.optimizer.rules;

import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.core.config.ConfigGroup;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;

public final class RuleBasedETaxiOptimizerParams extends AbstractTaxiOptimizerParams {
	public static final String SET_NAME = "RuleBasedETaxiOptimizer";

	@Parameter
	@Comment("Taxis with SOC below this level are considered undercharged"
			+ " and should be recharged. The value must be in (0, 1]."
			+ " The default value is 0.3 (used for simulating Nissan Leaf taxis).")
	// 30% SOC (=6 kWh) is enough to travel 40 km (all AUX off);
	// alternatively, in cold winter, it is enough to travel for 1 hour
	// (for approx. 20 km => 3kWh) with 3 kW-heating on
	@Positive
	@DecimalMax("1.0")
	public double minSoc = 0.3;

	@Parameter
	@Comment("Specifies how often idle vehicles are checked if they have become"
			+ " undercharged. Undercharged ones will be immediately sent to the nearest charging stations."
			+ " The default value is 300 (used for simulating Nissan Leaf taxis).")
	// in cold winter, 3kW heating consumes 1.25% SOC every 5 min
	@Positive
	public int socCheckTimeStep = 300;

	private RuleBasedTaxiOptimizerParams ruleBasedTaxiOptimizerParams;

	public RuleBasedETaxiOptimizerParams() {
		super(SET_NAME, false, false);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//insertion search params (one of: extensive, selective, repeated selective)
		addDefinition(RuleBasedTaxiOptimizerParams.SET_NAME, RuleBasedTaxiOptimizerParams::new,
			() -> ruleBasedTaxiOptimizerParams,
			params -> ruleBasedTaxiOptimizerParams = (RuleBasedTaxiOptimizerParams)params);
	}

	public RuleBasedTaxiOptimizerParams getRuleBasedTaxiOptimizerParams() {
		return ruleBasedTaxiOptimizerParams;
	}

	@Override
	public int getReoptimizationTimeStep() {
		return ruleBasedTaxiOptimizerParams.getReoptimizationTimeStep();
	}
}
