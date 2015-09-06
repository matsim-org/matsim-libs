/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.population.algorithms;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

/**
 * @author mrieser
 */
public class TripPlanMutateTimeAllocationTest {

	@Test
	public void testRun() {
		// setup population with one person
		PersonImpl person = (PersonImpl) PersonImpl.createPerson(Id.create(1, Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);
		ActivityImpl act = plan.createAndAddActivity("home", new Coord((double) 0, (double) 0));
		act.setEndTime(8.0 * 3600);
		plan.createAndAddLeg(TransportMode.transit_walk);
		ActivityImpl ptAct1 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100));
		ptAct1.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.pt);
		ActivityImpl ptAct2 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100));
		ptAct2.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.transit_walk);
		act = plan.createAndAddActivity("work", new Coord((double) 0, (double) 500));
		act.setEndTime(16*3600);
		plan.createAndAddLeg(TransportMode.transit_walk);
		ActivityImpl ptAct3 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100));
		ptAct3.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.pt);
		ActivityImpl ptAct4 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord((double) 0, (double) 100));
		ptAct4.setMaximumDuration(0);
		plan.createAndAddLeg(TransportMode.transit_walk);
		plan.createAndAddActivity("work", new Coord((double) 0, (double) 500));
		boolean affectingDuration = true ;

		TripPlanMutateTimeAllocation mutator =
				new TripPlanMutateTimeAllocation(
						new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ),
						3600.,
						affectingDuration, new Random(2011));
		mutator.run(plan);

		Assert.assertEquals(0.0, ptAct1.getMaximumDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct2.getMaximumDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct3.getMaximumDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct4.getMaximumDuration(), 1e-8);
	}
}
