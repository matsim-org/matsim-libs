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
package playground.thibautd.replanning;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
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
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "jojo" ) ) );

		final Id anchorLink1 = new IdImpl( "anchor1" );
		final Id anchorLink2 = new IdImpl( "anchor2" );
		final Random random = new Random( 234 );

		final String stageType = "stage";
		final String mode = "the_mode";

		plan.addActivity( new ActivityImpl( "h" , anchorLink1 ) );
		plan.addLeg( new LegImpl( mode ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "w" , anchorLink2 ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "w" , anchorLink2 ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( stageType , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
		plan.addLeg( new LegImpl( "mode-"+random.nextLong() ) );
		plan.addActivity( new ActivityImpl( "type-"+random.nextLong() , new IdImpl( random.nextLong() ) ) );
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
}

