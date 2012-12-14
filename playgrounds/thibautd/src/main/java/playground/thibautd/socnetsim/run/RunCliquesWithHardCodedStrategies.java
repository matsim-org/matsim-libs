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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import playground.thibautd.cliquessim.config.CliquesConfigGroup;
import playground.thibautd.cliquessim.utils.JointControlerUtils;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;

/**
 * @author thibautd
 */
public class RunCliquesWithHardCodedStrategies {
	private static class Weights {
		public static final double RE_ROUTE = 0.1;
		public static final double TIME_MUTATOR = 0.1;
		public static final double JT_MUTATION = 0.1;
		public static final double MODE_MUTATION = 0.1;
		public static final double LOGIT_SELECT = 0.6;
	}

	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		// load "registry"
		final Config config = JointControlerUtils.createConfig( configFile );
		final CliquesConfigGroup cliquesConf = (CliquesConfigGroup)
					config.getModule( CliquesConfigGroup.GROUP_NAME );
		final Scenario scenario = ScenarioUtils.loadScenario( config );
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

		// run it
		controller.run();
	}
}

