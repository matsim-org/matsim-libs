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

import playground.thibautd.analysis.listeners.LegHistogramListenerWithoutControler;
import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.cliquessim.config.CliquesConfigGroup;
import playground.thibautd.cliquessim.utils.JointControlerUtils;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.PlanLinks;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.selectors.AbstractHighestWeightSelector;

/**
 * @author thibautd
 */
public class RunCliquesWithHardCodedStrategies {
	private static final boolean DO_STRATEGY_TRACE = false;
	private static final boolean DO_SELECT_TRACE = false;

	public static class Weights {
		public double reRoute = 0.1;
		public double timeMutator = 0.1;
		public double jointTripMutation = 0.1;
		public double modeMutation = 0.1;
		public double logitSelection = 0.6;
		public boolean jtmOptimizes = true;

		public void setAllToZero() {
			reRoute = 0;
			timeMutator = 0;
			jointTripMutation = 0;
			modeMutation = 0;
			logitSelection = 0;
		}

		@Override
		public String toString() {
			return "{Weights: reRoute="+reRoute+
				", timeMutator="+timeMutator+
				", jointTripMutation="+jointTripMutation+
				(jtmOptimizes ? "with" : "without")+" optimization"+
				", modeMutation="+modeMutation+
				", logitSelection="+logitSelection+
				"}";
		}
	}

	public static Scenario createScenario(final String configFile) {
		final Config config = JointControlerUtils.createConfig( configFile );
		// do not load joint plans (it fails with v5, and contrary to the "cliquessim"
		// setting, it is not useful if reading plans without joint trips.
		// XXX reading plans with joint trips will fail!
		final Scenario scenario = ScenarioUtils.createScenario( config );
		JointControlerUtils.tuneScenario( scenario );
		ScenarioUtils.loadScenario( scenario );
		return scenario;
	}

	public static void runScenario(
			final Scenario scenario,
			final Weights weights) {
		final Config config = scenario.getConfig();
		final CliquesConfigGroup cliquesConf = (CliquesConfigGroup)
					config.getModule( CliquesConfigGroup.GROUP_NAME );
		final ControllerRegistry controllerRegistry =
			new ControllerRegistry(
					scenario,
					// TODO: import
					new PlanLinks(),
					new CharyparNagelScoringFunctionFactory(
						config.planCalcScore(),
						scenario.getNetwork()) );

		// init strategies
		final GroupStrategyRegistry strategyRegistry = new GroupStrategyRegistry();
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createReRoute(
					config,
					controllerRegistry.getTripRouterFactory() ),
				weights.reRoute);
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createTimeAllocationMutator(
					config,
					controllerRegistry.getTripRouterFactory() ),
				weights.timeMutator);
		if (weights.jtmOptimizes) {
			strategyRegistry.addStrategy(
					GroupPlanStrategyFactory.createCliqueJointTripMutator( controllerRegistry ),
					weights.jointTripMutation);
		}
		else {
			strategyRegistry.addStrategy(
					GroupPlanStrategyFactory.createNonOptimizingCliqueJointTripMutator( controllerRegistry ),
					weights.jointTripMutation);
		}
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createSubtourModeChoice(
					config,
					controllerRegistry.getTripRouterFactory() ),
				weights.modeMutation);
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createSelectExpBeta( config ),
				weights.logitSelection );

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
						controllerRegistry.getJointPlans(),
						strategyManager));

		controller.addControlerListener(
				new LegHistogramListenerWithoutControler(
					controllerRegistry.getEvents(),
					controller.getControlerIO() ));

		controllerRegistry.getEvents().addHandler( new ModeAnalysis( true ) );

		// run it
		controller.run();
	}

	public static void main(final String[] args) {
		if (DO_STRATEGY_TRACE) Logger.getLogger( GroupStrategyManager.class.getName() ).setLevel( Level.TRACE );
		if (DO_SELECT_TRACE) Logger.getLogger( AbstractHighestWeightSelector.class.getName() ).setLevel( Level.TRACE );
		final String configFile = args[ 0 ];

		// load "registry"
		final Scenario scenario = createScenario( configFile );
		runScenario( scenario , new Weights() );
	}
}

