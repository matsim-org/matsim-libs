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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.Desires;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.thibautd.config.NonFlatConfigReader;
import playground.thibautd.config.NonFlatConfigWriter;
import playground.thibautd.mobsim.PseudoSimConfigGroup;
import playground.thibautd.scoring.BeingTogetherScoring.LinearOverlapScorer;
import playground.thibautd.scoring.BeingTogetherScoring.LogOverlapScorer;
import playground.thibautd.scoring.BeingTogetherScoring.PersonOverlapScorer;
import playground.thibautd.scoring.FireMoneyEventsForUtilityOfBeingTogether;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.sharedvehicles.HouseholdBasedVehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;
import playground.thibautd.utils.GenericFactory;

/**
 * @author thibautd
 */
public class RunCliquesWithModularStrategies {
	private static final Logger log =
		Logger.getLogger(RunCliquesWithModularStrategies.class);

	private static final boolean DO_STRATEGY_TRACE = false;
	private static final boolean DO_SELECT_TRACE = false;
	private static final boolean DO_SCORING_TRACE = false;

	public static Scenario createScenario(final String configFile) {
		final Config config = JointScenarioUtils.createConfig();
		// needed for reading a non-flat format (other solution would be to put this in reader)
		final GroupReplanningConfigGroup weights = new GroupReplanningConfigGroup();
		config.addModule( weights );
		config.addModule( new ScoringFunctionConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
		config.addModule( new KtiInputFilesConfigGroup() );
		config.addModule( new PseudoSimConfigGroup() );
		new NonFlatConfigReader( config ).parse( configFile );
		final Scenario scenario = JointScenarioUtils.loadScenario( config );

		if ( config.scenario().isUseHouseholds() && weights.getUseLimitedVehicles() ) {
			scenario.addScenarioElement(
							VehicleRessources.ELEMENT_NAME,
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
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
					config.getModule( GroupReplanningConfigGroup.GROUP_NAME );
		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
					config.getModule( ScoringFunctionConfigGroup.GROUP_NAME );

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

		if ( scoringFunctionConf.getMarginalUtilityOfBeingTogether_s() > 0 ) {
			log.info( "add scorer for being together" );
			final FireMoneyEventsForUtilityOfBeingTogether socialScorer =
					new FireMoneyEventsForUtilityOfBeingTogether(
						controllerRegistry.getEvents(),
						scoringFunctionConf.getActTypeFilterForJointScoring(),
						scoringFunctionConf.getModeFilterForJointScoring(),
						getPersonOverlapScorerFactory(
							scoringFunctionConf,
							scenario.getPopulation() ),
						scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney(),
						(SocialNetwork) scenario.getScenarioElement(
							SocialNetwork.ELEMENT_NAME ));
			controllerRegistry.getEvents().addHandler( socialScorer );
			controller.addControlerListener( socialScorer );
		}
		else {
			log.info( "do NOT add scorer for being together" );
		}

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

		// dump non flat config
		new NonFlatConfigWriter( config ).write( controller.getControlerIO().getOutputFilename( "output_config.xml.gz" ) );
	}

	private static GenericFactory<PersonOverlapScorer, Id> getPersonOverlapScorerFactory(
			final ScoringFunctionConfigGroup scoringFunctionConf,
			final Population population) {
		switch ( scoringFunctionConf.getTogetherScoringForm() ) {
			case linear:
				return new GenericFactory<PersonOverlapScorer, Id>() {
						@Override
						public PersonOverlapScorer create( final Id id ) {
							return new LinearOverlapScorer(
									scoringFunctionConf.getMarginalUtilityOfBeingTogether_s() );
						}
					};
			case logarithmic:
				return new GenericFactory<PersonOverlapScorer, Id>() {
						@Override
						public PersonOverlapScorer create( final Id id ) {
							final PersonImpl person = (PersonImpl) population.getPersons().get( id );
							if ( person == null ) {
								// eg transit agent
								return new LinearOverlapScorer( 0 );
							}
							final Desires desires = person.getDesires();
							final double typicalDuration = desires.getActivityDuration( "leisure" );
							final double zeroDuration = typicalDuration * Math.exp( -10.0 / typicalDuration );
							return new LogOverlapScorer(
									scoringFunctionConf.getMarginalUtilityOfBeingTogether_s(),
									typicalDuration,
									zeroDuration);
						}
					};
			default:
				throw new RuntimeException( ""+scoringFunctionConf.getTogetherScoringForm() );
		}
	}

	private static SocialNetwork toSocialNetwork(
			final FixedGroupsIdentifier cliques) {
		final SocialNetwork socNet = new SocialNetwork();
		for ( Collection<? extends Id> clique : cliques.getGroupInfo() ) {
			for ( Id id1 : clique ) {
				for ( Id id2 : clique ) {
					socNet.addMonodirectionalTie( id1 , id2 );
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
		final Scenario scenario = createScenario( configFile );
		runScenario( scenario , true );
	}
}
