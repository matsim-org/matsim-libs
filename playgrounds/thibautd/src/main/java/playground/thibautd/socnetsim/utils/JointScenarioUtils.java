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
package playground.thibautd.socnetsim.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReaderMatsimV2;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import playground.thibautd.socnetsim.framework.SocialNetworkConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.JointTimeModeChooserConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.JointTripsMutatorConfigGroup;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.population.JointPlansConfigGroup;
import playground.thibautd.socnetsim.framework.population.JointPlansXmlReader;
import playground.thibautd.socnetsim.framework.scoring.InternalizationConfigGroup;
import playground.thibautd.socnetsim.jointactivities.replanning.modules.prismiclocationchoice.PrismicLocationChoiceConfigGroup;
import playground.thibautd.socnetsim.jointactivities.replanning.modules.randomlocationchoice.RandomJointLocationChoiceConfigGroup;
import playground.thibautd.socnetsim.jointtrips.population.DriverRouteFactory;
import playground.thibautd.socnetsim.jointtrips.population.JointActingTypes;
import playground.thibautd.socnetsim.jointtrips.population.PassengerRouteFactory;
import playground.thibautd.socnetsim.usage.PlanLinkConfigGroup;
import playground.thibautd.socnetsim.usage.replanning.GroupReplanningConfigGroup;

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
		final ModeRouteFactory rFactory = ((PopulationFactoryImpl) sc.getPopulation().getFactory()).getModeRouteFactory();
		rFactory.setRouteFactory(
				JointActingTypes.DRIVER,
				new DriverRouteFactory());
		rFactory.setRouteFactory(
				JointActingTypes.PASSENGER,
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
			scenario.addScenarioElement( JointPlans.ELEMENT_NAME , new JointPlans() );
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
		new ConfigReaderMatsimV2( config ).parse( configFile );
		return config;
	}
}

