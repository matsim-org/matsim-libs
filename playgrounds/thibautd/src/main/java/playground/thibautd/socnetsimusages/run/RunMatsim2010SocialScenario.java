/* *********************************************************************** *
 * project: org.matsim.*
 * RunMatsim2010SocialScenario.java
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
package playground.thibautd.socnetsimusages.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import playground.ivt.matsim2030.Matsim2030Utils;
import playground.ivt.matsim2030.generation.ScenarioMergingConfigGroup;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;
import playground.thibautd.initialdemandgeneration.transformation.SocialNetworkedPopulationDilutionUtils;
import playground.thibautd.initialdemandgeneration.transformation.SocialNetworkedPopulationDilutionUtils.DilutionType;
import playground.thibautd.socnetsim.framework.SocialNetworkConfigGroup;
import playground.thibautd.socnetsim.framework.controller.JointDecisionProcessModule;
import playground.thibautd.socnetsim.jointactivities.scoring.JointActivitiesScoringModule;
import playground.thibautd.socnetsim.jointtrips.JointTripsModule;
import playground.thibautd.socnetsim.framework.controller.SocialNetworkModule;
import playground.thibautd.socnetsim.usage.analysis.SocnetsimDefaultAnalysisModule;
import playground.thibautd.socnetsim.jointtrips.population.JointActingTypes;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetworkReader;
import playground.thibautd.socnetsim.usage.replanning.DefaultGroupStrategyRegistryModule;
import playground.thibautd.socnetsim.run.RunUtils;
import playground.thibautd.socnetsim.run.ScoringFunctionConfigGroup;
import playground.thibautd.socnetsimusages.scoring.KtiScoringFunctionFactoryWithJointModes;
import playground.thibautd.socnetsim.usage.JointScenarioUtils;

/**
 * @author thibautd
 */
public class RunMatsim2010SocialScenario {
	private static final Logger log =
		Logger.getLogger(RunMatsim2010SocialScenario.class);

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
		controller.addOverridingModule( new JointDecisionProcessModule() );
		controller.addOverridingModule( new SocnetsimDefaultAnalysisModule() );
		controller.addOverridingModule( new JointActivitiesScoringModule() );
		controller.addOverridingModule( new DefaultGroupStrategyRegistryModule() );
		controller.addOverridingModule( new JointTripsModule() );
		controller.addOverridingModule( new SocialNetworkModule() );

		controller.setScoringFunctionFactory(
				new KtiScoringFunctionFactoryWithJointModes(
					new MATSim2010ScoringFunctionFactory(
						scenario,
						new StageActivityTypesImpl(
							PtConstants.TRANSIT_ACTIVITY_TYPE,
							JointActingTypes.INTERACTION) ),
					scenario ) );

		controller.run();
	}

	private static Scenario loadScenario(final Config config) {
		final Scenario scenario = JointScenarioUtils.loadScenario( config );
		RunUtils.enrichScenario(scenario);
		Matsim2030Utils.enrichScenario( scenario );
		ZurichScenarioUtils.enrichScenario( scenario );
		scenario.getConfig().controler().setCreateGraphs( false ); // cannot set that from config file...

		final SocialNetworkConfigGroup snConf = (SocialNetworkConfigGroup)
				config.getModule( SocialNetworkConfigGroup.GROUP_NAME );

		new SocialNetworkReader( scenario ).parse( snConf.getInputFile() );

		final SocialNetwork sn = (SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		for ( Id p : scenario.getPopulation().getPersons().keySet() ) {
			if ( !sn.getEgos().contains( p ) ) sn.addEgo( p );
		}

		final ScenarioMergingConfigGroup mergingGroup = (ScenarioMergingConfigGroup)
			config.getModule( ScenarioMergingConfigGroup.GROUP_NAME );
		if ( mergingGroup.getPerformDilution() ) {
			final SocialDilutionConfigGroup dilutionConfig = (SocialDilutionConfigGroup)
				config.getModule( SocialDilutionConfigGroup.GROUP_NAME );
			log.info( "performing \"dilution\" with method "+dilutionConfig.getDilutionType() );
			SocialNetworkedPopulationDilutionUtils.dilute(
					dilutionConfig.getDilutionType(),
					scenario,
					mergingGroup.getDilutionCenter(),
					mergingGroup.getDilutionRadiusM() );
		}

		assert sn.getEgos().size() == scenario.getPopulation().getPersons().size() : sn.getEgos().size() +" != "+ scenario.getPopulation().getPersons().size();
		return scenario;
	}

	private static Config loadConfig(final String configFile) {
		final Config config = ConfigUtils.createConfig();
		JointScenarioUtils.addConfigGroups( config );
		config.addModule( new KtiInputFilesConfigGroup() );
		RunUtils.addConfigGroups( config );
		// some redundancy here... just add scenarioMerging "by hand"
		//Matsim2030Utils.addDefaultGroups( config );
		config.addModule( new ScenarioMergingConfigGroup() );
		config.addModule( new SocialDilutionConfigGroup() );
		new MatsimConfigReader( config ).parse( configFile );
		return config;
	}

	private static class SocialDilutionConfigGroup extends ReflectiveConfigGroup {
		public static final String GROUP_NAME = "socialDilution";

		// this is the most efficient (it removes the most agents)
		private DilutionType dilutionType = DilutionType.areaOnly;

		public SocialDilutionConfigGroup() {
			super( GROUP_NAME );
		}

		@StringGetter( "dilutionType" )
		public DilutionType getDilutionType() {
			return this.dilutionType;
		}

		@StringSetter( "dilutionType" )
		public void setDilutionType(DilutionType dilutionType) {
			this.dilutionType = dilutionType;
		}
	}
}
