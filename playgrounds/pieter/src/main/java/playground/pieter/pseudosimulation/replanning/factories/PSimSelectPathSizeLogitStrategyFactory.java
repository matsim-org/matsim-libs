/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import playground.pieter.pseudosimulation.replanning.selectors.PSimPathSizeLogitSelector;

import javax.inject.Provider;

public class PSimSelectPathSizeLogitStrategyFactory implements
        Provider<PlanStrategy> {

    private Scenario scenario;

    public PSimSelectPathSizeLogitStrategyFactory(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
	public PlanStrategy get() {
		return new PlanStrategyImpl(new PSimPathSizeLogitSelector(scenario.getConfig().planCalcScore(), scenario.getNetwork()));
	}

}
