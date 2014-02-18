/* *********************************************************************** *
 * project: org.matsim.*
 * Run.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.examples.schedulebasedteleportation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

/**
 * @author thibautd
 */
public class Run {

	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Config config = loadConfig( configFile );
		final Scenario scenario = loadScenario( config );

		final Controler controler = new Controler( scenario );

		// this is where the magic happens
		controler.setTripRouterFactory(
				new ScheduleBasedTripRouterFactory(
					scenario ) );

		controler.run();
	}

	private static Scenario loadScenario( final Config config ) {
		// here comes some uglyness. the config should never be modified from the
		// code, but this is currently the only wy to get the transit containers
		// loaded while not simulating transit. This will be fixed soon.

		final boolean vehs = config.scenario().isUseVehicles();
		config.scenario().setUseVehicles( true );

		final boolean transit = config.scenario().isUseTransit();
		config.scenario().setUseTransit( true );

		final Scenario scenario = ScenarioUtils.loadScenario( config );

		// set back parameters to the values in the config
		config.scenario().setUseVehicles( vehs );
		config.scenario().setUseTransit( transit );

		return scenario;
	}

	private static Config loadConfig( final String configFile ) {
		final Config config = ConfigUtils.loadConfig( configFile );

		// this is normally done in the Controler if transit is enabled
		final ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);

		transitActivityParams.setOpeningTime(0.) ;
		transitActivityParams.setClosingTime(0.) ;

		config.planCalcScore().addActivityParams(transitActivityParams);

		return config;
	}
}
