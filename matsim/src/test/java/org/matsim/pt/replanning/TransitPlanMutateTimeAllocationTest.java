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

package org.matsim.pt.replanning;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.PtConstants;

/**
 * @author mrieser
 */
public class TransitPlanMutateTimeAllocationTest {

	@Test
	public void testRun() {
		// setup population with one person
		PersonImpl person = new PersonImpl(new IdImpl(1));
		PlanImpl plan = person.createAndAddPlan(true);
		ActivityImpl act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
		act.setEndTime(8.0 * 3600);
		plan.createAndAddLeg(TransportMode.transit_walk);
		ActivityImpl ptAct1 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new CoordImpl(0, 100));
		ptAct1.setDuration(0);
		plan.createAndAddLeg(TransportMode.pt);
		ActivityImpl ptAct2 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new CoordImpl(0, 100));
		ptAct2.setDuration(0);
		plan.createAndAddLeg(TransportMode.transit_walk);
		act = plan.createAndAddActivity("work", new CoordImpl(0, 500));
		act.setEndTime(16*3600);
		plan.createAndAddLeg(TransportMode.transit_walk);
		ActivityImpl ptAct3 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new CoordImpl(0, 100));
		ptAct3.setDuration(0);
		plan.createAndAddLeg(TransportMode.pt);
		ActivityImpl ptAct4 = plan.createAndAddActivity(PtConstants.TRANSIT_ACTIVITY_TYPE, new CoordImpl(0, 100));
		ptAct4.setDuration(0);
		plan.createAndAddLeg(TransportMode.transit_walk);
		plan.createAndAddActivity("work", new CoordImpl(0, 500));

		TransitPlanMutateTimeAllocation mutator = new TransitPlanMutateTimeAllocation(3600, new Random(2011));
		mutator.run(plan);

		Assert.assertEquals(0.0, ptAct1.getDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct2.getDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct3.getDuration(), 1e-8);
		Assert.assertEquals(0.0, ptAct4.getDuration(), 1e-8);
	}
}
