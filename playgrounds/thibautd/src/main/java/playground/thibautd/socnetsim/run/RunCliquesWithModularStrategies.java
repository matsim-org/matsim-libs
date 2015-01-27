/* *********************************************************************** *
 * project: org.matsim.*
 * RunCliquesWithModularStrategies.java
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
package playground.thibautd.socnetsim.run;

import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkImpl;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;

/**
 * @author thibautd
 */
public class RunCliquesWithModularStrategies {
	private static final boolean DO_STRATEGY_TRACE = false;
	private static final boolean DO_SELECT_TRACE = false;
	private static final boolean DO_SCORING_TRACE = false;

	public static void runScenario( final Scenario scenario, final boolean produceAnalysis ) {
		final Config config = scenario.getConfig();
		final CliquesConfigGroup cliquesConf = (CliquesConfigGroup)
					config.getModule( CliquesConfigGroup.GROUP_NAME );
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
					config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

		final FixedGroupsIdentifier cliques = 
			config.scenario().isUseHouseholds() ?
				new FixedGroupsIdentifier(
						((ScenarioImpl) scenario).getHouseholds() ) :
				FixedGroupsIdentifierFileParser.readCliquesFile(
						cliquesConf.getInputFile() );

		scenario.addScenarioElement( SocialNetwork.ELEMENT_NAME , toSocialNetwork( cliques ) );

		final ControllerRegistry controllerRegistry =
			RunUtils.loadDefaultRegistryBuilder( scenario )
				.withGroupIdentifier( cliques )
				.build();

		final ImmutableJointController controller = RunUtils.initializeController( controllerRegistry );

		RunUtils.loadBeingTogetherListenner( controller );

		if (produceAnalysis) {
			RunUtils.loadDefaultAnalysis(
					weights.getGraphWriteInterval(),
					cliques,
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
	}

	private static SocialNetwork toSocialNetwork(
			final FixedGroupsIdentifier cliques) {
		final SocialNetwork socNet = new SocialNetworkImpl();
		for ( Collection<Id<Person>> clique : cliques.getGroupInfo() ) {
			final Id[] ids = clique.toArray( new Id[ clique.size() ] );
			socNet.addEgos( clique );

			// we cannot just add monodirectional ties in a reflective social network.
			for ( int i=0; i < ids.length; i++ ) {
				for ( int j=i; j < ids.length; j++ ) {
					socNet.addBidirectionalTie( ids[ i ] , ids[ j ] );
				}
			}
		}
		return socNet;
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
