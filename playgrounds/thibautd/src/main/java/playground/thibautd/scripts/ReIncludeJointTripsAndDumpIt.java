/* *********************************************************************** *
 * project: org.matsim.*
 * ReIncludeJointTripsAndDumpIt.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;

import playground.thibautd.jointtrips.population.Clique;
import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.jointtrippossibilities.JointTripPossibilitiesUtils;
import playground.thibautd.jointtrips.population.PopulationWithJointTripsWriterHandler;
import playground.thibautd.jointtrips.population.ScenarioWithCliques;
import playground.thibautd.jointtrips.router.JointPlanRouter;
import playground.thibautd.jointtrips.utils.JointControlerUtils;
import playground.thibautd.router.DefaultRoutingModuleFactory;
import playground.thibautd.router.LegRouterWrapper;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;
import playground.thibautd.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class ReIncludeJointTripsAndDumpIt {
	public static void main(final String[] args) {
		String configFile = args[ 0 ];
		String outputDir = args[ 1 ];

		MoreIOUtils.initOut( outputDir );

		Config config = JointControlerUtils.createConfig( configFile );
		ScenarioWithCliques scenario = JointControlerUtils.createScenario( config );

		PopulationWriter popWriter =
			new PopulationWriter(
					scenario.getPopulation(),
					scenario.getNetwork(),
					scenario.getKnowledges());
		popWriter.setWriterHandler(
				new PopulationWithJointTripsWriterHandler(
					scenario.getNetwork(),
					scenario.getKnowledges()));

		popWriter.write( outputDir +"/pop1-beforeRouting.xml.gz" );

		routePlans( scenario );
		popWriter.write( outputDir +"/pop2-afterRouting.xml.gz" );

		reinsertJointTrips( scenario );
		popWriter.write( outputDir +"/pop3-afterReinsertion.xml.gz" );
	}

	private static void routePlans(final ScenarioWithCliques scenario) {
		// init the router
		final FreespeedTravelTimeAndDisutility timeCost =
			new FreespeedTravelTimeAndDisutility(-1d, 1d, -1d);
		TripRouterFactory factory =
			new TripRouterFactory(
					scenario.getNetwork(),
					new TravelDisutilityFactory() {
						@Override
						public TravelDisutility createTravelDisutility(
								final PersonalizableTravelTime timeCalculator,
								final PlanCalcScoreConfigGroup cnScoringGroup) {
							return timeCost;
						}
					},
					new PersonalizableTravelTimeFactory() {
						@Override
						public PersonalizableTravelTime createTravelTime() {
							return timeCost;
						}
					},
					new DijkstraFactory(),
					new ModeRouteFactory());

		RoutingModuleFactory defaultFactory =
			new DefaultRoutingModuleFactory(
					scenario.getPopulation().getFactory(),
					scenario.getConfig().plansCalcRoute(),
					scenario.getConfig().planCalcScore());

		for (String mode : DefaultRoutingModuleFactory.HANDLED_MODES) {
			factory.setRoutingModuleFactory( mode , defaultFactory );
		}

		factory.setRoutingModuleFactory(
				JointActingTypes.PASSENGER,
				new RoutingModuleFactory() {
					@Override
					public RoutingModule createModule(
							final String mainMode,
							final TripRouterFactory factory) {
						return new LegRouterWrapper(
							mainMode,
							scenario.getPopulation().getFactory(),
							// "fake" router to avoid exceptions
							new TeleportationLegRouter(
								factory.getModeRouteFactory(),
								1,
								1),
							null,
							null);
					}
				});

		// route
		JointPlanRouter router = new JointPlanRouter( factory.createTripRouter() );

		for (Clique clique : scenario.getCliques().getCliques().values()) {
			JointPlan plan = (JointPlan) clique.getSelectedPlan();

			for (Plan indivPlan : plan.getIndividualPlans().values()) {
				router.run( indivPlan );
			}
		}
	}

	private static void reinsertJointTrips( final ScenarioWithCliques scenario ) {
		for (Clique clique : scenario.getCliques().getCliques().values()) {
			JointPlan plan = (JointPlan) clique.copySelectedPlan();

			//System.out.println( JointTripPossibilitiesUtils.getPerformedJointTrips( plan ) );
			//System.out.println( plan.getIndividualPlanElements() );

			JointTripPossibilitiesUtils.includeJointTrips(
					JointTripPossibilitiesUtils.getPerformedJointTrips( plan ),
					plan);
		}
	}
}

