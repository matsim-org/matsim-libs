/* *********************************************************************** *
 * project: org.matsim.*
 * JointControlerUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.thibautd.cliquessim.config.CliquesConfigGroup;
import playground.thibautd.cliquessim.config.JointTimeModeChooserConfigGroup;
import playground.thibautd.cliquessim.config.JointTripInsertorConfigGroup;
import playground.thibautd.cliquessim.config.JointTripsMutatorConfigGroup;
import playground.thibautd.cliquessim.population.PopulationWithJointTripsReader;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 *
 * @author thibautd
 */
public class JointControlerUtils {
	private JointControlerUtils() {}

	/**
	 * binds to createScenario(createConfig(configFile));
	 *
	 * @param configFile the path to the configFile
	 * @return a ready to use scenario
	 */
	public static Scenario createScenario(final String configFile) {
		return createScenario(createConfig(configFile));
	}

	/**
	 * @param config a loaded config
	 * @return a ready to use scenario
	 */
	public static Scenario createScenario(final Config config) {
		Scenario scenario = ScenarioUtils.createScenario( config );
		tuneScenario( scenario );

		//(new ScenarioLoaderImpl(scenario)).loadScenario();
		// ScenarioUtils.loadScenario(scenario);
		// Cannot load full joint information when using default loader: load manually.
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		loader.loadActivityFacilities();
		// TODO: adapt to new state
		(new PopulationWithJointTripsReader(scenario)).readFile(config.plans().getInputFile());

		return scenario;
	}

	public static void tuneScenario(final Scenario sc) {
		ModeRouteFactory rFactory = ((PopulationFactoryImpl) sc.getPopulation().getFactory()).getModeRouteFactory();
		rFactory.setRouteFactory(
				JointActingTypes.DRIVER,
				new RouteFactory() {
					@Override
					public Route createRoute(
						final Id s,
						final Id e) {
						return new DriverRoute( s , e );
					}
				});
		rFactory.setRouteFactory(
				JointActingTypes.PASSENGER,
				new RouteFactory() {
					@Override
					public Route createRoute(
						final Id s,
						final Id e) {
						return new PassengerRoute( s , e );
					}
				});
	}

	/**
	 * @param configFile the path to the config file
	 * @return a loaded config, including proper setting of joint trips specific groups
	 */
	public static Config createConfig(final String configFile) {
		Config config = ConfigUtils.createConfig();
		loadConfig( config , configFile );
		return config;
	}

	public static void loadConfig(final Config config, final String configFile) {
		// /////////////////////////////////////////////////////////////////////
		// initialize the config before passing it to the controler
		config.addCoreModules();
		config.addModule(
				CliquesConfigGroup.GROUP_NAME,
				new CliquesConfigGroup());
		config.addModule(
				JointTripsMutatorConfigGroup.GROUP_NAME,
				new JointTripsMutatorConfigGroup());
		config.addModule(
				JointTimeModeChooserConfigGroup.GROUP_NAME,
				new JointTimeModeChooserConfigGroup());
		config.addModule(
				JointTripInsertorConfigGroup.GROUP_NAME,
				new JointTripInsertorConfigGroup());

		//read the config file
		if (configFile != null) ConfigUtils.loadConfig(config, configFile);
	}
}

