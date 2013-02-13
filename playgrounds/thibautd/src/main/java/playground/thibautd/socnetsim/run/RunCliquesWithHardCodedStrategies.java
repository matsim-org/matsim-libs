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

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

import playground.thibautd.analysis.listeners.LegHistogramListenerWithoutControler;
import playground.thibautd.analysis.listeners.ModeAnalysis;
import playground.thibautd.analysis.listeners.TripModeShares;
import playground.thibautd.socnetsim.analysis.CliquesSizeGroupIdentifier;
import playground.thibautd.socnetsim.analysis.FilteredScoreStats;
import playground.thibautd.socnetsim.analysis.JointPlanSizeStats;
import playground.thibautd.socnetsim.analysis.JointTripsStats;
import playground.thibautd.socnetsim.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.selectors.AbstractHighestWeightSelector;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;

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
		final Config config = JointScenarioUtils.createConfig( configFile );
		final Scenario scenario = JointScenarioUtils.loadScenario( config );

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Activity act : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE )) {
					if (act.getCoord() != null) continue;
					if (act.getLinkId() == null) throw new NullPointerException();
					((ActivityImpl) act).setCoord(
						scenario.getNetwork().getLinks().get( act.getLinkId() ).getCoord() );
				}
			}
		}

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
					scenario.getScenarioElement( JointPlans.class ),
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
		final FixedGroupsIdentifier cliques = 
			FixedGroupsIdentifierFileParser.readCliquesFile(
					cliquesConf.getInputFile() );
		final GroupStrategyManager strategyManager =
			new GroupStrategyManager( 
					cliques,
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

		CliquesSizeGroupIdentifier groupIdentifier =
			new CliquesSizeGroupIdentifier(
					cliques.getGroupInfo() );

		controller.addControlerListener(
				new FilteredScoreStats(
					controller.getControlerIO(),
					controllerRegistry.getScenario(),
					groupIdentifier));

		controller.addControlerListener(
				new JointPlanSizeStats(
					controller.getControlerIO(),
					controllerRegistry.getScenario(),
					groupIdentifier));

		controller.addControlerListener(
				new JointTripsStats(
					controller.getControlerIO(),
					controllerRegistry.getScenario(),
					groupIdentifier));

		final CompositeStageActivityTypes actTypesForAnalysis = new CompositeStageActivityTypes();
		actTypesForAnalysis.addActivityTypes(
				controllerRegistry.getTripRouterFactory().createTripRouter().getStageActivityTypes() );
		actTypesForAnalysis.addActivityTypes(
				new StageActivityTypesImpl(
					Arrays.asList(
						JointActingTypes.PICK_UP,
						JointActingTypes.DROP_OFF ) ) );
		controller.addControlerListener(
				new TripModeShares(
					controller.getControlerIO(),
					controllerRegistry.getScenario(),
					new MainModeIdentifier() {
						private final MainModeIdentifier d = new MainModeIdentifierImpl();

						@Override
						public String identifyMainMode(
								final List<PlanElement> tripElements) {
							for (PlanElement pe : tripElements) {
								if ( !(pe instanceof Leg) ) continue;
								final String mode = ((Leg) pe).getMode();

								if (mode.equals( JointActingTypes.DRIVER ) ||
										mode.equals( JointActingTypes.PASSENGER ) ) {
									return mode;
								}
							}
							return d.identifyMainMode( tripElements );
						}
					},
					actTypesForAnalysis));

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

