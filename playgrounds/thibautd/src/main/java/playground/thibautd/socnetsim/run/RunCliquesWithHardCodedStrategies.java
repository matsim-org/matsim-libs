/* *********************************************************************** *
 * project: org.matsim.*
 * RunCliquesWithHardCodedStrategies.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import playground.thibautd.analysis.listeners.CliqueScoreStats;
import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.cliquessim.config.CliquesConfigGroup;
import playground.thibautd.cliquessim.utils.JointControlerUtils;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;

/**
 * @author thibautd
 */
public class RunCliquesWithHardCodedStrategies {
	private static final boolean DO_TRACE = false;

	private static class Weights {
		public static final double RE_ROUTE = 0.1;
		public static final double TIME_MUTATOR = 0.1;
		public static final double JT_MUTATION = 0.1;
		public static final double MODE_MUTATION = 0.1;
		public static final double LOGIT_SELECT = 0.6;
	}

	public static void main(final String[] args) {
		if (DO_TRACE) Logger.getLogger( GroupStrategyManager.class.getName() ).setLevel( Level.TRACE );
		final String configFile = args[ 0 ];

		// load "registry"
		final Config config = JointControlerUtils.createConfig( configFile );
		final CliquesConfigGroup cliquesConf = (CliquesConfigGroup)
					config.getModule( CliquesConfigGroup.GROUP_NAME );
		// do not load joint plans (it fails with v5, and contrary to the "cliquessim"
		// setting, it is not useful if reading plans without joint trips.
		// XXX reading plans with joint trips will fail!
		final Scenario scenario = ScenarioUtils.createScenario( config );
		JointControlerUtils.tuneScenario( scenario );
		ScenarioUtils.loadScenario( scenario );
		final ControllerRegistry controllerRegistry =
			new ControllerRegistry(
					scenario,
					new CharyparNagelScoringFunctionFactory(
						config.planCalcScore(),
						scenario.getNetwork()) );

		// init strategies
		final GroupStrategyRegistry strategyRegistry = new GroupStrategyRegistry();
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createReRoute(
					config,
					controllerRegistry.getTripRouterFactory() ),
				Weights.RE_ROUTE);
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createTimeAllocationMutator(
					config,
					controllerRegistry.getTripRouterFactory() ),
				Weights.TIME_MUTATOR);
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createCliqueJointTripMutator( controllerRegistry ),
				Weights.JT_MUTATION);
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createSubtourModeChoice(
					config,
					controllerRegistry.getTripRouterFactory() ),
				Weights.MODE_MUTATION);
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createSelectExpBeta( config ),
				Weights.LOGIT_SELECT );

		// create strategy manager
		final GroupStrategyManager strategyManager =
			new GroupStrategyManager( 
					FixedGroupsIdentifierFileParser.readCliquesFile(
							cliquesConf.getInputFile() ),
					strategyRegistry,
					config.strategy().getMaxAgentPlanMemorySize());

		// create controler
		final ImmutableJointController controller =
			new ImmutableJointController(
					controllerRegistry,
					new GroupReplanningListenner(
						scenario.getPopulation(),
						strategyManager));

		controller.addControlerListener(
				new CliqueScoreStats(
					controllerRegistry.getScenario(),
					controller.getControlerIO(),
					controllerRegistry.getScenario().getConfig().controler().getFirstIteration(),
					controllerRegistry.getScenario().getConfig().controler().getLastIteration(),
					"scoresStats",
					true));
		controllerRegistry.getEvents().addHandler( new ModeAnalysis( true ) );

		// run it
		controller.run();
	}
}

