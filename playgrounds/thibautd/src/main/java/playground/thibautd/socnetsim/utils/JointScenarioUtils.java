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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReaderMatsimV2;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.socnetsim.replanning.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.PlanLinkConfigGroup;
import playground.thibautd.socnetsim.SocialNetworkConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.JointTimeModeChooserConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.framework.cliques.config.JointTripsMutatorConfigGroup;
import playground.thibautd.socnetsim.population.DriverRouteFactory;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.population.JointPlansConfigGroup;
import playground.thibautd.socnetsim.framework.population.JointPlansXmlReader;
import playground.thibautd.socnetsim.population.PassengerRouteFactory;
import playground.thibautd.socnetsim.replanning.modules.prismiclocationchoice.PrismicLocationChoiceConfigGroup;
import playground.thibautd.socnetsim.replanning.modules.randomlocationchoice.RandomJointLocationChoiceConfigGroup;
import playground.thibautd.utils.DesiresConverter;

/**
 *
 * @author thibautd
 */
public class JointScenarioUtils {
	private static final Logger log =
		Logger.getLogger(JointScenarioUtils.class);

	private static final String UNKOWN_TRAVEL_CARD = "unknown";
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

		if ( config.plans().getInputPersonAttributeFile() != null ) {
			log.info( "re-reading attributes, this time using a converter for Desires." );
			final ObjectAttributesXmlReader reader =
				new ObjectAttributesXmlReader(
						scenario.getPopulation().getPersonAttributes());
			reader.putAttributeConverter( Desires.class , new DesiresConverter() );
			reader.parse(
				config.plans().getInputPersonAttributeFile() );

			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				// put desires (if any) in persons for backward compatibility
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

				// travel card
				final Boolean hasCard = (Boolean)
					scenario.getPopulation().getPersonAttributes().getAttribute(
							person.getId().toString(),
							"hasTravelcard" );
				if ( hasCard != null && hasCard ) {
					((PersonImpl) person).addTravelcard( UNKOWN_TRAVEL_CARD );
				}
			}
		}
	}

	/**
	 * @param configFile the path to the config file
	 * @return a loaded config, including proper setting of joint trips specific groups
	 */
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
		config.addModule( new KtiLikeScoringConfigGroup() );
		config.addModule( new PseudoSimConfigGroup() );
		config.addModule( new SocialNetworkConfigGroup() );
		config.addModule( new RandomJointLocationChoiceConfigGroup() );
		config.addModule( new PlanLinkConfigGroup() );
		config.addModule( new PrismicLocationChoiceConfigGroup() );
	}

	public static Config loadConfig(final String configFile) {
		final Config config = createConfig();
		new ConfigReaderMatsimV2( config ).parse( configFile );
		return config;
	}
}

