/* *********************************************************************** *
 * project: org.matsim.*
 * TransitActRemoverTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

/**
 * Tests the behaviour of TransitActRemoverCorrectTravelTime on a simple PlanImpl
 * instance.
 * @author thibautd
 */
public class TransitActRemoverTest {
	private Plan plan;
	private double transitTravelTime;

	@Before
	public void createPlanWithTransit() {
		plan = PopulationUtils.createPlan();

		double now = 0;
		transitTravelTime = 0;

		Activity act = PopulationUtils.createAndAddActivity(plan, (String) "h");
		now += 234;
		act.setEndTime( now );

		Leg leg = PopulationUtils.createAndAddLeg( plan, (String) TransportMode.walk );
		leg.setDepartureTime( now );
		double tt = 945;
		leg.setTravelTime( tt );
		transitTravelTime += tt;
		now += tt;

		act = PopulationUtils.createAndAddActivity(plan, (String) PtConstants.TRANSIT_ACTIVITY_TYPE);
		act.setStartTime( now );
		tt = 2435;
		now += tt;
		transitTravelTime += tt;
		act.setEndTime( now );

		leg = PopulationUtils.createAndAddLeg( plan, (String) TransportMode.pt );
		leg.setDepartureTime( now );
		tt = 503;
		leg.setTravelTime( tt );
		transitTravelTime += tt;
		now += tt;

		act = PopulationUtils.createAndAddActivity(plan, (String) PtConstants.TRANSIT_ACTIVITY_TYPE);
		act.setStartTime( now );
		tt = 235;
		now += tt;
		transitTravelTime += tt;
		act.setEndTime( now );

		leg = PopulationUtils.createAndAddLeg( plan, (String) TransportMode.walk );
		leg.setDepartureTime( now );
		tt = 4509;
		leg.setTravelTime( tt );
		transitTravelTime += tt;
		now += tt;

		act = PopulationUtils.createAndAddActivity(plan, (String) "h");
		act.setStartTime( now );
	}

	/**
	 * tests whether the activities are really removed, and the travel time
	 * is properly set
	 */
	@Test
	public void runTest() {
		(new TransitActRemoverCorrectTravelTime()).run( plan );

		List<PlanElement> pes = plan.getPlanElements();

		Assert.assertEquals(
				"unexpected corrected plan length",
				3,
				pes.size());

		Assert.assertEquals(
				"unexpected transit travel time",
				transitTravelTime,
				((Leg) pes.get( 1 )).getTravelTime(),
				MatsimTestUtils.EPSILON);
	}
}

