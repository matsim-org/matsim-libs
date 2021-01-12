/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.etaxi.run;

import org.matsim.contrib.etaxi.optimizer.assignment.AssignmentETaxiOptimizerParams;
import org.matsim.contrib.etaxi.optimizer.rules.RuleBasedETaxiOptimizerParams;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ETaxiConfigGroups {
	public static TaxiConfigGroup createWithCustomETaxiOptimizerParams() {
		TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
		taxiCfg.addOptimizerParamsDefinition(AssignmentETaxiOptimizerParams.SET_NAME,
				AssignmentETaxiOptimizerParams::new);
		taxiCfg.addOptimizerParamsDefinition(RuleBasedETaxiOptimizerParams.SET_NAME,
				RuleBasedETaxiOptimizerParams::new);
		return taxiCfg;
	}
}
