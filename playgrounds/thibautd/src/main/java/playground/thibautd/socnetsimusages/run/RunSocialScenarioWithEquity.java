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
package playground.thibautd.socnetsimusages.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.SocialNetworkConfigGroup;
import org.matsim.contrib.socnetsim.framework.controller.JointDecisionProcessModule;
import org.matsim.contrib.socnetsim.framework.controller.SocialNetworkModule;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.contrib.socnetsim.jointactivities.scoring.JointActivitiesScoringModule;
import org.matsim.contrib.socnetsim.jointtrips.JointTripsModule;
import org.matsim.contrib.socnetsim.run.RunUtils;
import org.matsim.contrib.socnetsim.run.ScoringFunctionConfigGroup;
import org.matsim.contrib.socnetsim.usage.ConfigConfiguredPlanLinkIdentifierModule;
import org.matsim.contrib.socnetsim.usage.JointScenarioUtils;
import org.matsim.contrib.socnetsim.usage.analysis.SocnetsimDefaultAnalysisModule;
import org.matsim.contrib.socnetsim.usage.replanning.DefaultGroupStrategyRegistryModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import playground.ivt.analysis.IvtAnalysisModule;
import playground.ivt.analysis.tripstats.TripStatisticsModule;
import playground.ivt.matsim2030.Matsim2030Utils;
import playground.ivt.matsim2030.generation.ScenarioMergingConfigGroup;
import playground.thibautd.initialdemandgeneration.transformation.SocialNetworkedPopulationDilutionUtils;
import playground.thibautd.socnetsimusages.traveltimeequity.EquityConfigGroup;
import playground.thibautd.socnetsimusages.traveltimeequity.EquityStrategiesModule;
import playground.thibautd.socnetsimusages.traveltimeequity.KtiScoringWithEquityModule;

/**
 * @author thibautd
 */
public class RunSocialScenarioWithEquity {
	private static final Logger log = Logger.getLogger(RunSocialScenarioWithEquity.class);

	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		final String configFile = args[ 0 ];

		final Config config = loadConfig(configFile);
		if ( ((ScoringFunctionConfigGroup) config.getModule( ScoringFunctionConfigGroup.GROUP_NAME )).isUseKtiScoring() ) {
			log.warn( "the parameter \"useKtiScoring\" from module "+ScoringFunctionConfigGroup.GROUP_NAME+" will be set to false" );
			log.warn( "a KTI-like scoring is already set from the script." );
			((ScoringFunctionConfigGroup) config.getModule( ScoringFunctionConfigGroup.GROUP_NAME )).setUseKtiScoring( false );
		}

		final Scenario scenario = loadScenario(config);

		final Controler controller = new Controler( scenario );
		// One needs to add the various features one wants to use in one module to be safe:
		// this way, if two features conflict, a crash will occur at injection.
		controller.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						install(new JointDecisionProcessModule());
					}
				} );
		controller.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						install( new ConfigConfiguredPlanLinkIdentifierModule());
						install(new SocnetsimDefaultAnalysisModule());
						install(new JointActivitiesScoringModule());
						install(new DefaultGroupStrategyRegistryModule());
						install(new JointTripsModule());
						install(new SocialNetworkModule());
						install(new EquityStrategiesModule());
						install(new IvtAnalysisModule() );
						install(new TripStatisticsModule() );
					}
				});
		controller.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						install(new KtiScoringWithEquityModule());
					}
				} );


		new WorldConnectLocations( config ).connectFacilitiesWithLinks(
				scenario.getActivityFacilities(),
				(NetworkImpl) scenario.getNetwork() );
		controller.run();
	}

	private static Scenario loadScenario(final Config config) {
		final Scenario scenario = JointScenarioUtils.loadScenario(config);
		RunUtils.enrichScenario(scenario);
		//scenario.getConfig().controler().setCreateGraphs( false ); // cannot set that from config file...

		final SocialNetworkConfigGroup snConf = (SocialNetworkConfigGroup)
				config.getModule( SocialNetworkConfigGroup.GROUP_NAME );

		new SocialNetworkReader( scenario ).parse( snConf.getInputFile() );

		final SocialNetwork sn = (SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		for ( Id p : scenario.getPopulation().getPersons().keySet() ) {
			if ( !sn.getEgos().contains( p ) ) sn.addEgo( p );
		}

		assert sn.getEgos().size() == scenario.getPopulation().getPersons().size() : sn.getEgos().size() +" != "+ scenario.getPopulation().getPersons().size();
		return scenario;
	}

	private static Config loadConfig(final String configFile) {
		final Config config = ConfigUtils.createConfig();
		JointScenarioUtils.addConfigGroups( config );
		RunUtils.addConfigGroups( config );
		config.addModule( new ScenarioMergingConfigGroup() );
		config.addModule( new EquityConfigGroup() );
		new ConfigReader( config ).parse( configFile );
		return config;
	}
}
