/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.pieter.distributed.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import playground.pieter.distributed.replanning.modules.SubtourModeChoiceForPlanGenomes;

import javax.inject.Inject;
import javax.inject.Provider;

public class SubtourModeChoiceStrategyFactory implements Provider<PlanStrategy> {

    private Scenario scenario;

    @Inject
    SubtourModeChoiceStrategyFactory(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new SubtourModeChoiceForPlanGenomes(scenario.getConfig()));
		strategy.addStrategyModule(new ReRoute(scenario));
		return strategy;
	}

}
