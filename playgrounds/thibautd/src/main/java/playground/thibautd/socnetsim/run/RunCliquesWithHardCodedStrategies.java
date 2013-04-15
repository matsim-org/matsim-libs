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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.socnetsim.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ControllerRegistryBuilder;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.DefaultPlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreSumSelectorForRemoval;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.sharedvehicles.HouseholdBasedVehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.PrepareVehicleAllocationForSimAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.VehicleBasedIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;

/**
 * @author thibautd
 */
public class RunCliquesWithHardCodedStrategies {
	private static final boolean DO_STRATEGY_TRACE = false;
	private static final boolean DO_SELECT_TRACE = false;

	public static Scenario createScenario(final String configFile) {
		final Config config = JointScenarioUtils.loadConfig( configFile );
		config.addModule( WeightsConfigGroup.GROUP_NAME , new WeightsConfigGroup() );
		final Scenario scenario = JointScenarioUtils.loadScenario( config );

		if ( config.scenario().isUseHouseholds() ) {
			scenario.addScenarioElement(
							new HouseholdBasedVehicleRessources(
								((ScenarioImpl) scenario).getHouseholds() ) );
		}

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

	public static void runScenario( final Scenario scenario, final boolean produceAnalysis ) {
		final Config config = scenario.getConfig();
		final CliquesConfigGroup cliquesConf = (CliquesConfigGroup)
					config.getModule( CliquesConfigGroup.GROUP_NAME );
		final WeightsConfigGroup weights = (WeightsConfigGroup)
					config.getModule( WeightsConfigGroup.GROUP_NAME );

		final FixedGroupsIdentifier cliques = 
			config.scenario().isUseHouseholds() ?
			new FixedGroupsIdentifier(
					((ScenarioImpl) scenario).getHouseholds() ) :
			FixedGroupsIdentifierFileParser.readCliquesFile(
					cliquesConf.getInputFile() );

		final PlanLinkIdentifier planLinkIdentifier =
			weights.getDoSynchronize() ?
				new DefaultPlanLinkIdentifier() :
				new PlanLinkIdentifier() {
					@Override
					public boolean areLinked(
							final Plan p1,
							final Plan p2) {
						return false;
					}
				};

		final GenericPlanAlgorithm<ReplanningGroup> additionalPrepareAlgo =
			scenario.getScenarioElement( VehicleRessources.class ) != null ?
			new PrepareVehicleAllocationForSimAlgorithm(
					MatsimRandom.getLocalInstance(),
					scenario.getScenarioElement( JointPlans.class ),
					scenario.getScenarioElement( VehicleRessources.class ),
					planLinkIdentifier) :
			new GenericPlanAlgorithm<ReplanningGroup>() {
				@Override
				public void run(final ReplanningGroup plan) {
					// do nothing more than default
				}
			};

		final ControllerRegistry controllerRegistry =
			new ControllerRegistryBuilder( scenario )
					.withPlanRoutingAlgorithmFactory(
							RunUtils.createPlanRouterFactory( scenario ) )
					.withGroupIdentifier(
							cliques )
					.withPlanLinkIdentifier(
							planLinkIdentifier )
					.withAdditionalPrepareForSimAlgorithms(
							additionalPrepareAlgo )
					.withIncompatiblePlansIdentifierFactory(
						weights.getDoSynchronize() &&
						scenario.getScenarioElement( VehicleRessources.class ) != null ?
							new VehicleBasedIncompatiblePlansIdentifierFactory( TransportMode.car ) :
							new EmptyIncompatiblePlansIdentifierFactory() )
					.build();

		// init strategies
		final GroupStrategyRegistry strategyRegistry = new GroupStrategyRegistry();
		RunUtils.loadStrategyRegistry( strategyRegistry , controllerRegistry );

		// create strategy manager
		final GroupStrategyManager strategyManager =
			new GroupStrategyManager( 
					new LowestScoreSumSelectorForRemoval(),
					strategyRegistry,
					config.strategy().getMaxAgentPlanMemorySize());

		// create controler
		final ImmutableJointController controller =
			new ImmutableJointController(
					controllerRegistry,
					new GroupReplanningListenner(
						controllerRegistry,
						strategyManager));

		if (produceAnalysis) {
			RunUtils.loadDefaultAnalysis( cliques , controller );
		}

		if ( weights.getDoSynchronize() ) {
			// those listenners check the coordination behavior:
			// do not ad if not used
			RunUtils.addConsistencyCheckingListeners( controller );
		}

		// run it
		controller.run();
	}

	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		if (DO_STRATEGY_TRACE) Logger.getLogger( GroupStrategyManager.class.getName() ).setLevel( Level.TRACE );
		if (DO_SELECT_TRACE) Logger.getLogger( HighestWeightSelector.class.getName() ).setLevel( Level.TRACE );
		final String configFile = args[ 0 ];

		// load "registry"
		final Scenario scenario = createScenario( configFile );
		runScenario( scenario , true );
	}
}

