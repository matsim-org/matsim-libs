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

package org.matsim.contrib.taxi.optimizer.zonal;

import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.zone.ZonalSystemParams;
import org.matsim.core.config.ConfigGroup;

public final class ZonalTaxiOptimizerParams extends AbstractTaxiOptimizerParams {
	public static final String SET_NAME = "ZonalTaxiOptimizer";

	private RuleBasedTaxiOptimizerParams ruleBasedTaxiOptimizerParams;
	private ZonalSystemParams zonalSystemParams;

	public ZonalTaxiOptimizerParams() {
		super(SET_NAME, false, false);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//insertion search params (one of: extensive, selective, repeated selective)
		addDefinition(RuleBasedTaxiOptimizerParams.SET_NAME, RuleBasedTaxiOptimizerParams::new,
			() -> ruleBasedTaxiOptimizerParams,
			params -> ruleBasedTaxiOptimizerParams = (RuleBasedTaxiOptimizerParams)params);
		addDefinition(ZonalSystemParams.SET_NAME, ZonalSystemParams::new,
			() -> zonalSystemParams,
			params -> zonalSystemParams = (ZonalSystemParams)params);
	}

	public ZonalSystemParams getZonalSystemParams() {
		return zonalSystemParams;
	}

	public RuleBasedTaxiOptimizerParams getRuleBasedTaxiOptimizerParams() {
		return ruleBasedTaxiOptimizerParams;
	}

	@Override
	public int getReoptimizationTimeStep() {
		return ruleBasedTaxiOptimizerParams.getReoptimizationTimeStep();
	}
}
