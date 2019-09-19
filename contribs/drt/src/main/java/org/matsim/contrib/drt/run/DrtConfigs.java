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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtConfigs {
	private static final Logger LOGGER = Logger.getLogger(DrtControlerCreator.class);

	public static void adjustMultiModeDrtConfig(MultiModeDrtConfigGroup multiModeDrtCfg,
			PlanCalcScoreConfigGroup planCalcScoreCfg) {
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, planCalcScoreCfg);
		}
	}

	public static void adjustDrtConfig(DrtConfigGroup drtCfg, PlanCalcScoreConfigGroup planCalcScoreCfg) {
		DrtStageActivityType drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());
		if (drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.stopbased) ||
				drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.serviceAreaBased)) {
			if (planCalcScoreCfg.getActivityParams(drtStageActivityType.drtStageActivity) == null) {
				addDrtStageActivityParams(planCalcScoreCfg, drtStageActivityType.drtStageActivity);
			}
		}
		if (!planCalcScoreCfg.getModes().containsKey(drtStageActivityType.drtWalk)) {
			addDrtWalkModeParams(planCalcScoreCfg, drtStageActivityType.drtWalk);
		}
	}

	private static void addDrtStageActivityParams(PlanCalcScoreConfigGroup planCalcScoreCfg, String stageActivityType) {
		PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(stageActivityType);
		params.setTypicalDuration(1);
		params.setScoringThisActivityAtAll(false);
		planCalcScoreCfg.getScoringParametersPerSubpopulation().values().forEach(k -> k.addActivityParams(params));
		planCalcScoreCfg.addActivityParams(params);
		LOGGER.info("drt interaction scoring parameters not set. Adding default values (activity will not be scored).");
	}

	private static void addDrtWalkModeParams(PlanCalcScoreConfigGroup planCalcScoreCfg, String drtWalkMode) {
		PlanCalcScoreConfigGroup.ModeParams drtWalk = new PlanCalcScoreConfigGroup.ModeParams(drtWalkMode);
		PlanCalcScoreConfigGroup.ModeParams walk = planCalcScoreCfg.getModes().get(TransportMode.walk);
		drtWalk.setConstant(walk.getConstant());
		drtWalk.setMarginalUtilityOfDistance(walk.getMarginalUtilityOfDistance());
		drtWalk.setMarginalUtilityOfTraveling(walk.getMarginalUtilityOfTraveling());
		drtWalk.setMonetaryDistanceRate(walk.getMonetaryDistanceRate());
		planCalcScoreCfg.getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(drtWalk));
		LOGGER.info("drt_walk scoring parameters not set. Adding default values (same as for walk mode).");
	}
}
