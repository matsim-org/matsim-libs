/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.contrib.socnetsim.framework.scoring;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEventsGenerator;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;

/**
 * @author thibautd
 */
public class GroupCompositionPenalizerTest {
	private static final Logger log = LogManager.getLogger( GroupCompositionPenalizerTest.class );
	private final Id<Link> linkId = Id.createLinkId( 1 );
	private final double utilOneCopart = 100;
	private final double utilAlone = -1;

	@Test
	void testFullOverlap() {
		test( new double[]{10 , 20} , new double[]{5 , 25} );
	}

	@Test
	void testInnerOverlap() {
		test( new double[]{5 , 25} , new double[]{10 , 20} );
	}

	@Test
	void testPartialOverlap() {
		test( new double[]{5 , 20 } , new double[]{10 , 25} );
	}

	@Test
	void testExactOverlap() {
		test( new double[]{10 , 20} , new double[]{10 , 20} );
	}

	@Test
	void testComeAndGo() {
		test( new double[]{10 , 11 , 15 , 20} , new double[]{10 , 20} );
	}

	@Test
	void testInstantaneousComeAndGo() {
		test( new double[]{10 , 15 , 15 , 20} , new double[]{5 , 20} );
	}

	private void test( final double[] times1 , final double[] times2 ) {
		final Config config = ConfigUtils.createConfig();
		final Scenario sc = ScenarioUtils.createScenario( config );
		createNetwork( sc );

		createPerson( sc , 1 , times1 );
		createPerson( sc , 2 , times2 );

		final SocialNetwork sn = createSocialNetwork( sc );

		final EventsManager events = new ParallelEventsManager(true);

		final GroupCompositionPenalizer penalizer = new GroupCompositionPenalizer(
				"leisure",
				new GroupCompositionPenalizer.UtilityOfTimeCalculator() {
					@Override
					public double getUtilityOfTime( final int nCoParticipants ) {
						return nCoParticipants == 1 ? utilOneCopart : utilAlone;
					}
				}
		);

		events.addHandler( new CourtesyEventsGenerator( events , sn ) );
		//events.addHandler( new EventLogger() );
		final EventsToScore eventsToScore = EventsToScore.createWithoutScoreUpdating(
				sc,
				new ScoringFunctionFactory() {
					@Override
					public ScoringFunction createNewScoringFunction( final Person person ) {
						log.info( "create scoring function for " + person );
						final SumScoringFunction sum = new SumScoringFunction();

						if ( person.getId().equals( Id.createPersonId( 1 ) ) ) {
							log.info( "adding penalizer for " + person );
							sum.addScoringFunction( penalizer );
						}

						return sum;
					}
				},
				events );

		eventsToScore.beginIteration( 1, false );

		new QSimBuilder(sc.getConfig()) //
				.useDefaults()
				.build(sc, events)
				.run();

		eventsToScore.finish();

		final double score = penalizer.getScore();
		Assertions.assertEquals(
				calcExpectedScore( times1[ 0 ] , times1[ times1.length - 1 ] , times2[ 0 ] , times2[ times2.length - 1 ]),
				score,
				1E-9,
				"unexpected score" );
	}

	private double calcExpectedScore( final double start1, final double end1, final double start2, final double end2 ) {
		final double lastStart = Math.max( start1 , start2 );
		final double firstEnd = Math.min( end1 , end2 );

		final double overlap = Math.max( 0 , firstEnd - lastStart );

		final double alone = end1 - start1 - overlap;
		return overlap * utilOneCopart + alone * utilAlone;
	}

	private SocialNetwork createSocialNetwork( final Scenario sc ) {
		final SocialNetwork sn = new SocialNetworkImpl( true );
		for ( Id<Person> ego : sc.getPopulation().getPersons().keySet() ) {
			sn.addEgo( ego );
			for ( Id<Person> alter : sc.getPopulation().getPersons().keySet() ) {
				if ( ego == alter ) break;
				sn.addBidirectionalTie( ego , alter );
			}

		}
		return sn;
	}

	private void createPerson(
			final Scenario sc,
			final int id,
			final double[] times ) {
		final PopulationFactory factory = sc.getPopulation().getFactory();
		final Person person = factory.createPerson( Id.createPersonId( id ) );
		sc.getPopulation().addPerson( person );

		final Plan plan = factory.createPlan();
		person.addPlan( plan );

		final Activity firstHome = factory.createActivityFromLinkId( "home" , linkId );
		firstHome.setEndTime( times[ 0 ] );
		plan.addActivity( firstHome );

		for ( int i = 1; i < times.length; i++ ) {
			plan.addLeg( createLeg( factory ) );

			final Activity leisure = factory.createActivityFromLinkId( "leisure", linkId );
			leisure.setEndTime( times[ i ] );
			plan.addActivity( leisure );
		}

		plan.addLeg( createLeg( factory ) );

		final Activity lastHome = factory.createActivityFromLinkId( "home" , linkId );
		plan.addActivity( lastHome );
	}

	private Leg createLeg( final PopulationFactory factory ) {
		final Leg l = factory.createLeg( "stay here" );
		l.setRoute( RouteUtils.createGenericRouteImpl(linkId, linkId) );
		l.setTravelTime( 0 );
		return l;
	}

	private void createNetwork( final Scenario sc ) {
		final Node n1 = sc.getNetwork().getFactory().createNode( Id.createNodeId( 1 ) , new Coord( 0 , 0 ) );
		final Node n2 = sc.getNetwork().getFactory().createNode( Id.createNodeId( 2 ) , new Coord( 1 , 1 ) );
		final Link l = sc.getNetwork().getFactory().createLink( linkId , n1 , n2 );

		sc.getNetwork().addNode( n1 );
		sc.getNetwork().addNode( n2 );
		sc.getNetwork().addLink( l );
	}

	private static class EventLogger implements BasicEventHandler {
		@Override
		public void handleEvent( final Event event ) {
			log.info( event.toString() );
		}

		@Override
		public void reset( final int iteration ) {}
	}
}

