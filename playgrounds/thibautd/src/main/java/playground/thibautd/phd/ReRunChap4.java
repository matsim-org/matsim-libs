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
package playground.thibautd.phd;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.controller.CliquesModule;
import org.matsim.contrib.socnetsim.framework.controller.JointDecisionProcessModule;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.lib.tools.fileCreation.F2LConfigGroup;
import playground.thibautd.socnetsimusages.run.KtiInputFilesConfigGroup;
import playground.thibautd.socnetsimusages.run.ZurichScenarioUtils;
import playground.thibautd.socnetsimusages.traveltimeequity.EquityConfigGroup;
import playground.thibautd.socnetsimusages.traveltimeequity.EquityStrategiesModule;
import playground.thibautd.socnetsimusages.traveltimeequity.KtiScoringWithEquityModule;

/**
 * @author thibautd
 */
public class ReRunChap4 {
	private static final Logger log = Logger.getLogger( ReRunChap4.class );

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
						new JointDecisionProcessModule() );
		controller.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						install( new ConfigConfiguredPlanLinkIdentifierModule() );
						install( new SocnetsimDefaultAnalysisModule() );
						install( new JointActivitiesScoringModule() );
						install( new DefaultGroupStrategyRegistryModule() );
						install( new JointTripsModule() );
						//install( new SocialNetworkModule() );
						install( new CliquesModule() );
						install( new EquityStrategiesModule() );
						install( new StartingPointRandomizerModule() );
					}
				});
		controller.addOverridingModule(
						new KtiScoringWithEquityModule() );


		controller.run();
	}

	private static Scenario loadScenario(final Config config) {
		final Scenario scenario = JointScenarioUtils.loadScenario( config );
		RunUtils.enrichScenario(scenario);
		//Matsim2030Utils.enrichScenario( scenario );
		ZurichScenarioUtils.enrichScenario( scenario );

		return scenario;
	}

	private static Config loadConfig(final String configFile) {
		final Config config = ConfigUtils.createConfig();
		JointScenarioUtils.addConfigGroups( config );
		config.addModule( new KtiInputFilesConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
		config.addModule( new F2LConfigGroup() );
		RunUtils.addConfigGroups( config );
		// some redundancy here... just add scenarioMerging "by hand"
		//Matsim2030Utils.addDefaultGroups( config );
		config.addModule( new EquityConfigGroup() );
		new ConfigReader( config ).readFile( configFile );
		return config;
	}
}
