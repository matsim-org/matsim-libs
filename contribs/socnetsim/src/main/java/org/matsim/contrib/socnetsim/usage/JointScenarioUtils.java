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
package org.matsim.contrib.socnetsim.usage;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.SocialNetworkConfigGroup;
import org.matsim.contrib.socnetsim.framework.cliques.config.CliquesConfigGroup;
import org.matsim.contrib.socnetsim.framework.cliques.config.JointTimeModeChooserConfigGroup;
import org.matsim.contrib.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import org.matsim.contrib.socnetsim.framework.cliques.config.JointTripsMutatorConfigGroup;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.population.JointPlansConfigGroup;
import org.matsim.contrib.socnetsim.framework.population.JointPlansXmlReader;
import org.matsim.contrib.socnetsim.framework.scoring.InternalizationConfigGroup;
import org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice.PrismicLocationChoiceConfigGroup;
import org.matsim.contrib.socnetsim.jointactivities.replanning.modules.randomlocationchoice.RandomJointLocationChoiceConfigGroup;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRouteFactory;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRouteFactory;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author thibautd
 */
public class JointScenarioUtils {
	private JointScenarioUtils() {}

	/**
	 * binds to createScenario(createConfig(configFile));
	 *
	 * @param configFile the path to the configFile
	 * @return a ready to use scenario
	 */
	public static Scenario loadScenario(final String configFile) {
		return loadScenario(loadConfig(configFile));
	}

	public static Scenario createScenario(final Config config) {
		final Scenario sc = ScenarioUtils.createScenario( config );
		final RouteFactoryImpl rFactory = ((PopulationFactoryImpl) sc.getPopulation().getFactory()).getRouteFactory();
		rFactory.setRouteFactory(
				DriverRoute.class,//JointActingTypes.DRIVER,
				new DriverRouteFactory());
		rFactory.setRouteFactory(
				PassengerRoute.class,//JointActingTypes.PASSENGER,
				new PassengerRouteFactory());
		return sc;
	}

	/**
	 * @param config a loaded config
	 * @return a ready to use scenario
	 */
	public static Scenario loadScenario(final Config config) {
		final Scenario scenario = createScenario( config );
		ScenarioUtils.loadScenario( scenario );
		enrichScenario( scenario );
		return scenario;
	}

	public static void enrichScenario(final Scenario scenario) {
		final Config config = scenario.getConfig();
		final JointPlansConfigGroup jpConfig = (JointPlansConfigGroup)
			config.getModule( JointPlansConfigGroup.GROUP_NAME );
		if ( jpConfig.getFileName() != null) {
			new JointPlansXmlReader( scenario ).parse( jpConfig.getFileName() );
		}
		else {
			scenario.addScenarioElement(JointPlans.ELEMENT_NAME, new JointPlans());
		}
	}

	public static Config createConfig() {
		final Config config = ConfigUtils.createConfig();

		addConfigGroups( config );

		return config;
	}

	public static void addConfigGroups(final Config config) {
		config.addModule( new CliquesConfigGroup());
		config.addModule( new JointTripsMutatorConfigGroup());
		config.addModule( new JointTimeModeChooserConfigGroup());
		config.addModule( new JointTripInsertorConfigGroup());
		config.addModule( new JointPlansConfigGroup());
		config.addModule( new GroupReplanningConfigGroup() );
		config.addModule( new SocialNetworkConfigGroup() );
		config.addModule( new RandomJointLocationChoiceConfigGroup() );
		config.addModule( new PlanLinkConfigGroup() );
		config.addModule( new PrismicLocationChoiceConfigGroup() );
		config.addModule( new InternalizationConfigGroup() );
	}

	public static Config loadConfig(final String configFile) {
		final Config config = createConfig();
		new ConfigReader( config ).parse( configFile );
		return config;
	}
}

