/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RunLocationChoice.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.locationchoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.BestReplyLocationChoicePlanStrategy;
import org.matsim.contrib.locationchoice.bestresponse.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceContext;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Collection;

public class RunLocationChoiceFrozenEpsilons {
	private static final String MY_LOCATION_CHOICE = "MyLocationChoice";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);

		boolean problem = true ;
		Collection<StrategySettings> coll = config.strategy().getStrategySettings() ;
		for ( StrategySettings settings : coll ) {
			if ( settings.getStrategyName().equals( MY_LOCATION_CHOICE ) ) {
				problem = false ;
			}
		}
		if ( problem ) {
			throw new RuntimeException("You are using this script, but you have not set the " + MY_LOCATION_CHOICE + " strategy "
					+ "in your config file.  This is probably not what you want.  (Otherwise use that strategy, but set its "
					+ "weight/probability to zero.)") ;
		}
		// yy I have never tested the above consistency check.  Please get back to us if it does not work.  kai, may'15

		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final DestinationChoiceContext lcContext = new DestinationChoiceContext(scenario) ;
		scenario.addScenarioElement(DestinationChoiceContext.ELEMENT_NAME, lcContext);

		// ---

		Controler controler = new Controler(scenario);

		// ---

		DCScoringFunctionFactory scoringFunctionFactory = new DCScoringFunctionFactory(scenario, lcContext);
		scoringFunctionFactory.setUsingConfigParamsForScoring(true) ;
		controler.setScoringFunctionFactory(scoringFunctionFactory);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(MY_LOCATION_CHOICE).to( BestReplyLocationChoicePlanStrategy.class );
			}
		});

		// ---

		controler.run();
	}

}
