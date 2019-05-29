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

import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.zone.ZonalSystemParams;
import org.matsim.core.config.ConfigGroup;

public class ZonalTaxiOptimizerParams extends DefaultTaxiOptimizerParams {
	public static final String SET_NAME = "ZonalTaxiOptimizer";

	private RuleBasedTaxiOptimizerParams ruleBasedTaxiOptimizerParams;
	private ZonalSystemParams zonalSystemParams;

	public ZonalTaxiOptimizerParams() {
		super(SET_NAME, false, false);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		switch (type) {
			case RuleBasedTaxiOptimizerParams.SET_NAME:
				return new RuleBasedTaxiOptimizerParams();
			case ZonalSystemParams.SET_NAME:
				return new ZonalSystemParams();
		}
		return super.createParameterSet(type);
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		switch (set.getName()) {
			case RuleBasedTaxiOptimizerParams.SET_NAME:
				ruleBasedTaxiOptimizerParams = (RuleBasedTaxiOptimizerParams)set;
				break;
			case ZonalSystemParams.SET_NAME:
				zonalSystemParams = (ZonalSystemParams)set;
				break;
		}

		super.addParameterSet(set);
	}

	ZonalSystemParams getZonalSystemParams() {
		return zonalSystemParams;
	}

	RuleBasedTaxiOptimizerParams getRuleBasedTaxiOptimizerParams() {
		return ruleBasedTaxiOptimizerParams;
	}

	@Override
	public int getReoptimizationTimeStep() {
		return ruleBasedTaxiOptimizerParams.getReoptimizationTimeStep();
	}

	@Override
	public void setReoptimizationTimeStep(int reoptimizationTimeStep) {
		ruleBasedTaxiOptimizerParams.setReoptimizationTimeStep(reoptimizationTimeStep);
	}
}
