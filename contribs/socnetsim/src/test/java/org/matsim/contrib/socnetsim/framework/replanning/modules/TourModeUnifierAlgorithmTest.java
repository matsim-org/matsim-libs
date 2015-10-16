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

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class TourModeUnifierAlgorithmTest {

	@Test
	public void testPlanWithOneSingleTour() throws Exception {
		final Plan plan = new PlanImpl(PersonImpl.createPerson(Id.create("jojo", Person.class)));

		final Id<Link> anchorLink1 = Id.create( "anchor1" , Link.class );
		final Id<Link> anchorLink2 = Id.create( "anchor2" , Link.class );
		final Random random = new Random( 234 );

		final String stageType = "stage";
		final String mode = "the_mode";

		plan.addActivity( new ActivityImpl( "h" , anchorLink1 ) );
		plan.addLeg( new LegImpl( mode ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "w" , anchorLink2 ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "w" , anchorLink2 ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "h" , anchorLink1 ) );

		final StageActivityTypes types = new StageActivityTypesImpl( stageType );
		final int nActs = TripStructureUtils.getActivities( plan , types ).size();

		final PlanAlgorithm testee =
			new TourModeUnifierAlgorithm(
					types,
					new MainModeIdentifierImpl() );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected plan size",
				2 * nActs - 1,
				plan.getPlanElements().size() );
		for ( Trip trip : TripStructureUtils.getTrips( plan , types ) ) {
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
		final Plan plan = new PlanImpl(PersonImpl.createPerson(Id.create("jojo", Person.class)));

		final Id<Link> entranceLink = Id.create( "entrance" , Link.class );
		final Id<Link> exitLink = Id.create( "exit" , Link.class );
		final Id<Link> anchorLink1 = Id.create( "anchor1" , Link.class );
		final Id<Link> anchorLink2 = Id.create( "anchor2" , Link.class );
		final Random random = new Random( 234 );

		final String stageType = "stage";
		final String mode1 = "first_mode";
		final String mode2 = "second_mode";
		final String modeOfOpenTour = "space_shuttle";

		plan.addActivity( new ActivityImpl( "e" , entranceLink ) );
		plan.addLeg( new LegImpl( modeOfOpenTour ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( modeOfOpenTour ) );
		plan.addActivity( new ActivityImpl( stageType , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( modeOfOpenTour ) );
		plan.addActivity( new ActivityImpl( "h" , anchorLink1 ) );
		plan.addLeg( new LegImpl( mode1 ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "h" , anchorLink1 ) );
		plan.addLeg( new LegImpl( modeOfOpenTour ) );
		plan.addActivity( new ActivityImpl( stageType , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( modeOfOpenTour ) );
		plan.addActivity( new ActivityImpl( "w" , anchorLink2 ) );
		plan.addLeg( new LegImpl( mode2 ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "w" , anchorLink2 ) );
		plan.addLeg( new LegImpl( modeOfOpenTour ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( modeOfOpenTour ) );
		plan.addActivity( new ActivityImpl( "s" , exitLink ) );


		final StageActivityTypes types = new StageActivityTypesImpl( stageType );

		final PlanAlgorithm testee =
			new TourModeUnifierAlgorithm( 
					types,
					new MainModeIdentifierImpl() );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected plan size",
				31,
				plan.getPlanElements().size() );

		final List<Trip> trips = TripStructureUtils.getTrips( plan , types );

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
		final Plan plan = new PlanImpl(PersonImpl.createPerson(Id.create("jojo", Person.class)));

		final Id<Link> anchorLink = Id.create( "anchor" , Link.class );
		final Random random = new Random( 234 );

		final String stageType = "stage";
		final String mode1 = "first_mode";
		final String mode2 = "second_mode";

		plan.addActivity( new ActivityImpl( "h" , anchorLink ) );
		plan.addLeg( new LegImpl( mode1 ) );
		plan.addActivity( new ActivityImpl( stageType , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "h" , anchorLink ) );
		plan.addLeg( new LegImpl( mode2 ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , Id.create( random.nextLong() , Link.class ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "h" , anchorLink ) );

		final StageActivityTypes types = new StageActivityTypesImpl( stageType );
		final int nActs = TripStructureUtils.getActivities( plan , types ).size();

		final PlanAlgorithm testee =
			new TourModeUnifierAlgorithm( 
					types,
					new MainModeIdentifierImpl() );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected plan size",
				2 * nActs - 1,
				plan.getPlanElements().size() );

		final List<Trip> trips = TripStructureUtils.getTrips( plan , types );

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
}

