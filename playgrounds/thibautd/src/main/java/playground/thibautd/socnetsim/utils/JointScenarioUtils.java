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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.thibautd.socnetsim.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.cliques.config.JointTimeModeChooserConfigGroup;
import playground.thibautd.socnetsim.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.cliques.config.JointTripsMutatorConfigGroup;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.JointPlansConfigGroup;
import playground.thibautd.socnetsim.population.JointPlansXmlReader;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.utils.DesiresConverter;

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
		return sc;
	}

	/**
	 * @param config a loaded config
	 * @return a ready to use scenario
	 */
	public static Scenario loadScenario(final Config config) {
		final Scenario scenario = createScenario( config );
		ScenarioUtils.loadScenario( scenario );

		final JointPlansConfigGroup jpConfig = (JointPlansConfigGroup)
			config.getModule( JointPlansConfigGroup.GROUP_NAME );
		if ( jpConfig.getFileName() != null) {
			final JointPlans jps = JointPlansXmlReader.readJointPlans(
					scenario.getPopulation(),
					jpConfig.getFileName() );
			scenario.addScenarioElement( jps );
		}
		else {
			scenario.addScenarioElement( new JointPlans() );
		}

		if ( config.plans().getInputPersonAttributeFile() != null ) {
			final ObjectAttributesXmlReader reader =
				new ObjectAttributesXmlReader(
						scenario.getPopulation().getPersonAttributes());
			reader.putAttributeConverter( Desires.class , new DesiresConverter() );
			reader.parse(
				config.plans().getInputPersonAttributeFile() );

			// put desires (if any) in persons for backward compatibility
			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				final Desires desires = (Desires)
					scenario.getPopulation().getPersonAttributes().getAttribute(
							person.getId().toString(),
							"desires" );
				if ( desires != null ) {
					((PersonImpl) person).createDesires( desires.getDesc() );
					for ( Map.Entry<String, Double> entry : desires.getActivityDurations().entrySet() ) {
						((PersonImpl) person).getDesires().putActivityDuration(
							entry.getKey(),
							entry.getValue() );
					}
				}
			}
		}

		return scenario;
	}

	/**
	 * @param configFile the path to the config file
	 * @return a loaded config, including proper setting of joint trips specific groups
	 */
	public static Config createConfig() {
		final Config config = ConfigUtils.createConfig();

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
		config.addModule(
				JointPlansConfigGroup.GROUP_NAME,
				new JointPlansConfigGroup());

		return config;
	}

	public static Config loadConfig(final String configFile) {
		final Config config = createConfig();
		if (configFile != null) ConfigUtils.loadConfig( config , configFile );
		return config;
	}
}

