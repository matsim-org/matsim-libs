/* *********************************************************************** *
 * project: org.matsim.*
 * TripsToLegsAlgorithmTest.java
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
package playground.thibautd.router.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;


/**
 * @author thibautd
 */
public class TripsToLegsAlgorithmTest {
	private final static String STAGE_TYPE = "stage";

	private List<Plan> plans;
	private TripsToLegsAlgorithm t2lAlgo;

	@Before
	public void initPlans() {
		plans = new ArrayList<Plan>();
		// strict alternance
		PlanImpl newPlan = new PlanImpl();
		plans.add( newPlan );

		newPlan.createAndAddActivity( "act" );
		for (int i=0; i < 20; i++) {
			newPlan.createAndAddLeg( "leg" );
			newPlan.createAndAddActivity( "act" );
		}

		// multi-leg trips
		newPlan = new PlanImpl();
		plans.add( newPlan );

		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );

		// multi-leg-act trips
		newPlan = new PlanImpl();
		plans.add( newPlan );

		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( STAGE_TYPE );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( STAGE_TYPE );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( STAGE_TYPE );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( STAGE_TYPE );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( "act" );
		newPlan.createAndAddLeg( "leg" );
		newPlan.createAndAddActivity( STAGE_TYPE );
		newPlan.createAndAddActivity( "act" );
	}

	@Before
	public void initAlgo() {
		t2lAlgo =
			new TripsToLegsAlgorithm(
					new TripRouter(),
					new StageActivityTypesImpl( Arrays.asList( STAGE_TYPE ) ) );
	}

	@Test
	public void testActLegAlternance() throws Exception {
		for (Plan plan : plans) {
			t2lAlgo.run( plan );

			boolean lastWasAct = false;

			for ( PlanElement pe : plan.getPlanElements() ) {
				boolean isAct = pe instanceof Activity;
				Assert.assertFalse(
						"wrong act/trip alternance after algo: "+plan.getPlanElements(),
						isAct && lastWasAct);
			}
		}
	}

	@Test
	public void testStageActivityPresence() throws Exception {
		for (Plan plan : plans) {
			t2lAlgo.run( plan );

			for ( PlanElement pe : plan.getPlanElements() ) {
				boolean isAct = pe instanceof Activity;
				Assert.assertFalse(
						"got a stage act after algo: "+plan.getPlanElements(),
						isAct && ((Activity) pe).getType().equals( STAGE_TYPE ));
			}
		}	
	}
}

