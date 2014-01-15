/* *********************************************************************** *
 * project: org.matsim.*
 * RunGenericSocialNetwork.java
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
package playground.thibautd.socnetsim.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryLogging;

import playground.thibautd.config.NonFlatConfigWriter;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.population.SocialNetworkReader;
import playground.thibautd.socnetsim.replanning.grouping.DynamicGroupIdentifier;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.SocialNetworkConfigGroup;

/**
 * @author thibautd
 */
public class RunGenericSocialNetwork {
	private static final boolean DO_STRATEGY_TRACE = false;
	private static final boolean DO_SELECT_TRACE = false;
	private static final boolean DO_SCORING_TRACE = false;

	public static void runScenario( final Scenario scenario, final boolean produceAnalysis ) {
		final Config config = scenario.getConfig();
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
				config.getModule( GroupReplanningConfigGroup.GROUP_NAME );
		final SocialNetworkConfigGroup snConf = (SocialNetworkConfigGroup)
				config.getModule( SocialNetworkConfigGroup.GROUP_NAME );

		new SocialNetworkReader( scenario ).parse( snConf.getInputFile() );

		final ControllerRegistry controllerRegistry =
			RunUtils.loadDefaultRegistryBuilder( scenario )
				.withGroupIdentifier( 
						new DynamicGroupIdentifier(
							scenario ) )
				.build();

		final ImmutableJointController controller = RunUtils.initializeController( controllerRegistry );

		RunUtils.loadBeingTogetherListenner( controller );

		if (produceAnalysis) {
			RunUtils.loadDefaultAnalysis(
					weights.getGraphWriteInterval(),
					null , // cliques...
					controller );
		}

		if ( weights.getCheckConsistency() ) {
			// those listenners check the coordination behavior:
			// do not ad if not used
			RunUtils.addConsistencyCheckingListeners( controller );
		}
		RunUtils.addDistanceFillerListener( controller );

		// run it
		controller.run();

		// dump non flat config
		new NonFlatConfigWriter( config ).write( controller.getControlerIO().getOutputFilename( "output_config.xml.gz" ) );
	}

	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		if (DO_STRATEGY_TRACE) Logger.getLogger( GroupStrategyManager.class.getName() ).setLevel( Level.TRACE );
		if (DO_SELECT_TRACE) Logger.getLogger( HighestWeightSelector.class.getName() ).setLevel( Level.TRACE );
		if (DO_SCORING_TRACE) Logger.getLogger( "playground.thibautd.scoring" ).setLevel( Level.TRACE );
		final String configFile = args[ 0 ];

		// load "registry"
		final Scenario scenario = RunUtils.createScenario( configFile );
		runScenario( scenario , true );
	}
}
