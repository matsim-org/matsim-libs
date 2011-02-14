/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.impl;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.PlanElementHandler;
import playground.mrieser.core.mobsim.impl.PlanMobsimImpl;

/**
 * @author mrieser
 */
public class PlanMobsimImplTest {

	@Test
	public void testSetGetPlanElementHandler_Interfaces() {
		PlanMobsimImpl sim = new PlanMobsimImpl(null);
		Assert.assertNull(sim.getPlanElementHandler(PlanElement.class));
		Assert.assertNull(sim.getPlanElementHandler(Activity.class));
		Assert.assertNull(sim.getPlanElementHandler(Leg.class));

		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		sim.setPlanElementHandler(Activity.class, actHandler);
		Assert.assertNull(sim.getPlanElementHandler(PlanElement.class));
		Assert.assertEquals(actHandler, sim.getPlanElementHandler(Activity.class));
		Assert.assertNull(sim.getPlanElementHandler(Leg.class));

		sim.setPlanElementHandler(Leg.class, legHandler);
		Assert.assertNull(sim.getPlanElementHandler(PlanElement.class));
		Assert.assertEquals(actHandler, sim.getPlanElementHandler(Activity.class));
		Assert.assertEquals(legHandler, sim.getPlanElementHandler(Leg.class));
	}

	@Test
	public void testSetGetPlanElementHandler_Implementations() {
		PlanMobsimImpl sim = new PlanMobsimImpl(null);
		Assert.assertNull(sim.getPlanElementHandler(Activity.class));
		Assert.assertNull(sim.getPlanElementHandler(Leg.class));

		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		sim.setPlanElementHandler(Activity.class, actHandler);
		Assert.assertEquals(actHandler, sim.getPlanElementHandler(ActivityImpl.class));
		Assert.assertNull(sim.getPlanElementHandler(LegImpl.class));

		sim.setPlanElementHandler(Leg.class, legHandler);
		Assert.assertEquals(actHandler, sim.getPlanElementHandler(ActivityImpl.class));
		Assert.assertEquals(legHandler, sim.getPlanElementHandler(LegImpl.class));
	}

	@Test
	public void testRemovePlanElementHandler() {
		PlanMobsimImpl sim = new PlanMobsimImpl(null);
		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		sim.setPlanElementHandler(Activity.class, actHandler);
		sim.setPlanElementHandler(Leg.class, legHandler);

		Assert.assertEquals(actHandler, sim.getPlanElementHandler(Activity.class));
		Assert.assertEquals(legHandler, sim.getPlanElementHandler(Leg.class));

		Assert.assertEquals(actHandler, sim.removePlanElementHandler(Activity.class));

		Assert.assertNull(sim.getPlanElementHandler(Activity.class));
		Assert.assertEquals(legHandler, sim.getPlanElementHandler(Leg.class));

		Assert.assertNull(sim.removePlanElementHandler(Activity.class));
	}

	@Test
	public void testSetPlanElementHandler_replace() {
		PlanMobsimImpl sim = new PlanMobsimImpl(null);
		CountingPlanElementHandler actHandler1 = new CountingPlanElementHandler();
		CountingPlanElementHandler actHandler2 = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		Assert.assertNull(sim.setPlanElementHandler(Activity.class, actHandler1));
		Assert.assertNull(sim.setPlanElementHandler(Leg.class, legHandler));

		Assert.assertEquals(actHandler1, sim.getPlanElementHandler(Activity.class));
		Assert.assertEquals(legHandler, sim.getPlanElementHandler(Leg.class));

		Assert.assertEquals(actHandler1, sim.setPlanElementHandler(Activity.class, actHandler2));

		Assert.assertEquals(actHandler2, sim.getPlanElementHandler(Activity.class));
		Assert.assertEquals(legHandler, sim.getPlanElementHandler(Leg.class));
	}

	/*package*/ static class CountingPlanElementHandler implements PlanElementHandler {
		public int countStart = 0;
		public int countEnd = 0;
		@Override
		public void handleStart(final PlanAgent agent) {
			this.countStart++;
		}
		@Override
		public void handleEnd(final PlanAgent agent) {
			this.countEnd++;
		}
	}
}
