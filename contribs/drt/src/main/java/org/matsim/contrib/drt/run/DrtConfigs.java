/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtConfigs {
	private static final Logger LOGGER = LogManager.getLogger(DrtControlerCreator.class);

	public static void adjustMultiModeDrtConfig(MultiModeDrtConfigGroup multiModeDrtCfg,
			PlanCalcScoreConfigGroup planCalcScoreCfg, PlansCalcRouteConfigGroup plansCalcRouteCfg) {
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, planCalcScoreCfg, plansCalcRouteCfg);
		}
	}

	public static void adjustDrtConfig(DrtConfigGroup drtCfg, PlanCalcScoreConfigGroup planCalcScoreCfg,
			PlansCalcRouteConfigGroup plansCalcRouteCfg) {
		String drtStageActivityType = PlanCalcScoreConfigGroup.createStageActivityType(drtCfg.getMode());
		if (planCalcScoreCfg.getActivityParams(drtStageActivityType) == null) {
			addDrtStageActivityParams(planCalcScoreCfg, drtStageActivityType);
		}
	}

	private static void addDrtStageActivityParams(PlanCalcScoreConfigGroup planCalcScoreCfg, String stageActivityType) {
		PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(stageActivityType);
		params.setTypicalDuration(1);
		params.setScoringThisActivityAtAll(false);
		planCalcScoreCfg.getScoringParametersPerSubpopulation().values().forEach(k -> k.addActivityParams(params));
		if (planCalcScoreCfg.getScoringParameters(null) != null)
			planCalcScoreCfg.addActivityParams(params);
		LOGGER.info("drt interaction scoring parameters not set. Adding default values (activity will not be scored).");
	}

}
