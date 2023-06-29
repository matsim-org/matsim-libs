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

package org.matsim.codeexamples.extensions.locationChoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastes;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

class RunLocationChoiceFrozenEpsilonsExample{

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args );

		config.strategy().addStrategySettings( new StrategyConfigGroup.StrategySettings().setStrategyName( FrozenTastes.LOCATION_CHOICE_PLAN_STRATEGY )
												 .setWeight( 0.1 ) );

		final FrozenTastesConfigGroup dccg = ConfigUtils.addOrGetModule(config, FrozenTastesConfigGroup.class ) ;
		dccg.setEpsilonScaleFactors("10.0" ); // larger value means locations are farther away
//		dccg.setAlgorithm( bestResponse ); // is default
		dccg.setFlexibleTypes( "shopping" );
//		dccg.setTravelTimeApproximationLevel( FrozenTastesConfigGroup.ApproximationLevel.localRouting ); // is default
//		dccg.setRandomSeed( 221177 ); // is default
		dccg.setDestinationSamplePercent( 5. );

		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// ---

		Controler controler = new Controler(scenario);

		// ---

		FrozenTastes.configure( controler );

		// ---

		controler.run();
	}

}
