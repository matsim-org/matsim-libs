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

import org.matsim.core.config.Config;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import playground.pieter.pseudosimulation.replanning.selectors.PSimExpBetaPlanSelector;

import javax.inject.Inject;

public class PSimSelectExpBetaPlanStrategyFactory implements
		PlanStrategyFactory {

    private Config config;

    @Inject
    PSimSelectExpBetaPlanStrategyFactory(Config config) {
        this.config = config;
    }

    @Override
	public PlanStrategy get() {
		return new PlanStrategyImpl(new PSimExpBetaPlanSelector(config.planCalcScore()));
	}

}
