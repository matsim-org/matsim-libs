/* *********************************************************************** *
 * project: org.matsim.*
 * TourModeUnifierAlgorithmTest.java
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * @author thibautd
 */
public class TourModeUnifierAlgorithmTest {
	private static final Logger log = Logger.getLogger( TourModeUnifierAlgorithmTest.class );

	@Test
	public void testPlanWithOneSingleTour() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("jojo", Person.class)));

		final Id<Link> anchorLink1 = Id.create( "anchor1" , Link.class );
		final Id<Link> anchorLink2 = Id.create( "anchor2" , Link.class );
		final Random random = new Random( 234 );

		final String stageType = "stage interaction";
		final String mode = "the_mode";

		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", anchorLink1) );
		plan.addLeg( PopulationUtils.createLeg(mode) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("w", anchorLink2) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId(stageType, Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId(stageType, Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId(stageType, Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("w", anchorLink2) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId(stageType, Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", anchorLink1) );

//		final Set<String> types = new HashSet<>(); // formerly new StageActivityTypesImpl();
		final int nActs = TripStructureUtils.getActivities( plan , StageActivityHandling.ExcludeStageActivities ).size();

		final PlanAlgorithm testee =
			new TourModeUnifierAlgorithm(
					TripStructureUtils::isStageActivityType,
					new MainModeIdentifierImpl() );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected plan size",
				2 * nActs - 1,
				plan.getPlanElements().size() );
		for ( Trip trip : TripStructureUtils.getTrips( plan , TripStructureUtils::isStageActivityType ) ) {
			Assert.assertEquals(
					"unexpected size of trip "+trip,
					1,
					trip.getTripElements().size() );

			Assert.assertEquals(
					"unexpected mode of trip "+trip,
					mode,
					((Leg) trip.getTripElements().get( 0 )).getMode() );
		}
	}

	@Test
	public void testPlanWithTwoToursOnOpenTour() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("jojo", Person.class)));

		final Id<Link> entranceLink = Id.create( "entrance" , Link.class );
		final Id<Link> exitLink = Id.create( "exit" , Link.class );
		final Id<Link> anchorLink1 = Id.create( "anchor1" , Link.class );
		final Id<Link> anchorLink2 = Id.create( "anchor2" , Link.class );
		final Random random = new Random( 234 );

		final String stageType = "stage interaction";
		final String mode1 = "first_mode";
		final String mode2 = "second_mode";
		final String modeOfOpenTour = "space_shuttle";

		plan.addActivity( PopulationUtils.createActivityFromLinkId("e", entranceLink) );
		plan.addLeg( PopulationUtils.createLeg(modeOfOpenTour) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg(modeOfOpenTour) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId(stageType, Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg(modeOfOpenTour) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", anchorLink1) );
		plan.addLeg( PopulationUtils.createLeg(mode1) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", anchorLink1) );
		plan.addLeg( PopulationUtils.createLeg(modeOfOpenTour) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId(stageType, Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg(modeOfOpenTour) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("w", anchorLink2) );
		plan.addLeg( PopulationUtils.createLeg(mode2) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("w", anchorLink2) );
		plan.addLeg( PopulationUtils.createLeg(modeOfOpenTour) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg(modeOfOpenTour) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("s", exitLink) );


//		final Set<String> types = new HashSet<>();// formerly new StageActivityTypesImpl();

		final PlanAlgorithm testee =
			new TourModeUnifierAlgorithm( 
					TripStructureUtils::isStageActivityType,
					new MainModeIdentifierImpl() );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected plan size",
				31,
				plan.getPlanElements().size() );

		final List<Trip> trips = TripStructureUtils.getTrips( plan , TripStructureUtils::isStageActivityType );

		Assert.assertEquals(
				"unexpected number of trips",
				13,
				trips.size() );

		for ( int tripNr : new int[]{0,1,6,11,12} ) {
			Assert.assertEquals(
					"unexpected mode for trip "+tripNr,
					modeOfOpenTour,
					trips.get( tripNr ).getLegsOnly().get( 0 ).getMode() );
		}

		for ( int tripNr : new int[]{2,3,3,4,5} ) {
			Assert.assertEquals(
					"unexpected length for trip "+tripNr,
					1,
					trips.get( tripNr ).getLegsOnly().size() );

			Assert.assertEquals(
					"unexpected mode for trip "+tripNr,
					mode1,
					trips.get( tripNr ).getLegsOnly().get( 0 ).getMode() );
		}

		for ( int tripNr : new int[]{7,8,9,10} ) {
			Assert.assertEquals(
					"unexpected length for trip "+tripNr,
					1,
					trips.get( tripNr ).getLegsOnly().size() );

			Assert.assertEquals(
					"unexpected mode for trip "+tripNr,
					mode2,
					trips.get( tripNr ).getLegsOnly().get( 0 ).getMode() );
		}
	}

	@Test
	public void testPlanWithTwoHomeBasedTours() throws Exception {
		final Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create("jojo", Person.class)));

		final Id<Link> anchorLink = Id.create( "anchor" , Link.class );
		final Random random = new Random( 234 );

		final String stageType = "stage interaction";
		final String mode1 = "first_mode";
		final String mode2 = "second_mode";

		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", anchorLink) );
		plan.addLeg( PopulationUtils.createLeg(mode1) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId(stageType, Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", anchorLink) );
		plan.addLeg( PopulationUtils.createLeg(mode2) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("type-"+random.nextLong(), Id.create( random.nextLong() , Link.class )) );
		plan.addLeg( PopulationUtils.createLeg("mode-"+random.nextLong()) );
		plan.addActivity( PopulationUtils.createActivityFromLinkId("h", anchorLink) );

		final List<Activity> activities = TripStructureUtils.getActivities( plan, StageActivityHandling.ExcludeStageActivities );
		final int nActs = activities.size();

		Assert.assertEquals( 9, nActs );

		final PlanAlgorithm testee =
			new TourModeUnifierAlgorithm( 
					TripStructureUtils::isStageActivityType,
					new MainModeIdentifierImpl() );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected plan size",
				2 * nActs - 1,
				plan.getPlanElements().size() );

		final List<Trip> trips = TripStructureUtils.getTrips( plan , TripStructureUtils::isStageActivityType );

		Assert.assertEquals(
				"unexpected number of trips",
				8,
				trips.size() );

		for ( int tripNr : new int[]{0,1,2,3} ) {
			Assert.assertEquals(
					"unexpected length for trip "+tripNr,
					1,
					trips.get( tripNr ).getLegsOnly().size() );

			Assert.assertEquals(
					"unexpected mode for trip "+tripNr,
					mode1,
					trips.get( tripNr ).getLegsOnly().get( 0 ).getMode() );
		}

		for ( int tripNr : new int[]{4,5,6,7} ) {
			Assert.assertEquals(
					"unexpected length for trip "+tripNr,
					1,
					trips.get( tripNr ).getLegsOnly().size() );

			Assert.assertEquals(
					"unexpected mode for trip "+tripNr,
					mode2,
					trips.get( tripNr ).getLegsOnly().get( 0 ).getMode() );
		}
	}
	private static void printPlan( Plan plan ){
		StringBuilder msg = new StringBuilder();
		for( PlanElement planElement : plan.getPlanElements() ){
			if ( planElement instanceof Activity ) {
				msg.append( "| " ).append( ((Activity) planElement).getType() ).append( " |" );
			} else if ( planElement instanceof Leg ) {
				msg.append( "| " ).append( ((Leg) planElement).getMode() ).append( " |" );
			}
		}
		log.info( msg.toString() );
	}
}

