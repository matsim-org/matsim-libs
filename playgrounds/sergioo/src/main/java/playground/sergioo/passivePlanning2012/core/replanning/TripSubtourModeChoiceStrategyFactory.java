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

package playground.sergioo.passivePlanning2012.core.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;

import javax.inject.Inject;
import javax.inject.Provider;

public class TripSubtourModeChoiceStrategyFactory implements Provider<PlanStrategy> {

    private Scenario scenario;

    @Inject
    public TripSubtourModeChoiceStrategyFactory(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
	public PlanStrategy get() {
		BasePlanModulesStrategy strategy = new BasePlanModulesStrategy(scenario);
		strategy.addStrategyModule(new TransitActsRemoverModule(scenario.getConfig().global()));
		strategy.addStrategyModule(new SubtourModeChoice(scenario.getConfig()));
		strategy.addStrategyModule(new ReRoute(scenario));
		return strategy;
	}

}
