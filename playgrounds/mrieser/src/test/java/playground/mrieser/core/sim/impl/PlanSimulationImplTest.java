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

package playground.mrieser.core.sim.impl;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mrieser.core.sim.api.PlanElementHandler;

/**
 * @author mrieser
 */
public class PlanSimulationImplTest {

	@Test
	public void testSetGetPlanElementHandler_Interfaces() {
		PlanSimulationImpl sim = new PlanSimulationImpl();
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
		PlanSimulationImpl sim = new PlanSimulationImpl();
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
		PlanSimulationImpl sim = new PlanSimulationImpl();
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
		PlanSimulationImpl sim = new PlanSimulationImpl();
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

	@Test
	public void testHandleNextPlanElement_singlePlan() {
		Plan plan = new PlanImpl();
		Coord c = new CoordImpl(0, 0);
		plan.addActivity(new ActivityImpl("home", c));
		plan.addLeg(new LegImpl(TransportMode.car));
		plan.addActivity(new ActivityImpl("work", c));
		plan.addLeg(new LegImpl(TransportMode.car));
		plan.addActivity(new ActivityImpl("shop", c));
		plan.addLeg(new LegImpl(TransportMode.car));
		plan.addActivity(new ActivityImpl("home", c));

		PlanSimulationImpl sim = new PlanSimulationImpl();
		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		sim.setPlanElementHandler(Activity.class, actHandler);
		sim.setPlanElementHandler(Leg.class, legHandler);

		assertCounts(0, 0, 0, 0, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // home act
		assertCounts(1, 0, 0, 0, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // leg
		assertCounts(1, 1, 1, 0, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // work act
		assertCounts(2, 1, 1, 1, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // leg
		assertCounts(2, 2, 2, 1, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // shop act
		assertCounts(3, 2, 2, 2, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // leg
		assertCounts(3, 3, 3, 2, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // home act
		assertCounts(4, 3, 3, 3, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // proceed, just finish home act
		assertCounts(4, 4, 3, 3, actHandler, legHandler);
		sim.handleNextPlanElement(plan); // nothing to proceed
		assertCounts(4, 4, 3, 3, actHandler, legHandler); // numbers should not change
	}

	@Test
	public void testHandleNextPlanElement_multiplePlans() {
		Plan plan1 = new PlanImpl();
		Coord c = new CoordImpl(0, 0);
		plan1.addActivity(new ActivityImpl("home", c));
		plan1.addLeg(new LegImpl(TransportMode.car));
		plan1.addActivity(new ActivityImpl("work", c));
		plan1.addLeg(new LegImpl(TransportMode.car));
		plan1.addActivity(new ActivityImpl("shop", c));
		plan1.addLeg(new LegImpl(TransportMode.car));
		plan1.addActivity(new ActivityImpl("home", c));
		Plan plan2 = new PlanImpl();
		plan2.addActivity(new ActivityImpl("home", c));
		plan2.addLeg(new LegImpl(TransportMode.car));
		plan2.addActivity(new ActivityImpl("leisure", c));
		plan2.addLeg(new LegImpl(TransportMode.car));
		plan2.addActivity(new ActivityImpl("home", c));

		PlanSimulationImpl sim = new PlanSimulationImpl();
		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		sim.setPlanElementHandler(Activity.class, actHandler);
		sim.setPlanElementHandler(Leg.class, legHandler);

		assertCounts(0, 0, 0, 0, actHandler, legHandler);
		sim.handleNextPlanElement(plan1); // 1.home act
		assertCounts(1, 0, 0, 0, actHandler, legHandler);
		sim.handleNextPlanElement(plan1); // 1.leg
		assertCounts(1, 1, 1, 0, actHandler, legHandler);
		sim.handleNextPlanElement(plan2); // 2.home act
		assertCounts(2, 1, 1, 0, actHandler, legHandler);
		sim.handleNextPlanElement(plan1); // 1.work act
		assertCounts(3, 1, 1, 1, actHandler, legHandler);
		sim.handleNextPlanElement(plan2); // 2.leg
		assertCounts(3, 2, 2, 1, actHandler, legHandler);
		sim.handleNextPlanElement(plan2); // 2.leisure
		assertCounts(4, 2, 2, 2, actHandler, legHandler);
		sim.handleNextPlanElement(plan1); // 1.leg
		assertCounts(4, 3, 3, 2, actHandler, legHandler);
		sim.handleNextPlanElement(plan1); // 1.shop act
		assertCounts(5, 3, 3, 3, actHandler, legHandler);
		sim.handleNextPlanElement(plan1); // 1.leg
		assertCounts(5, 4, 4, 3, actHandler, legHandler);
		sim.handleNextPlanElement(plan2); // 2.leg
		assertCounts(5, 5, 5, 3, actHandler, legHandler);
		sim.handleNextPlanElement(plan1); // 1.home act
		assertCounts(6, 5, 5, 4, actHandler, legHandler);
		sim.handleNextPlanElement(plan2); // 2.home act
		assertCounts(7, 5, 5, 5, actHandler, legHandler);
	}

	private void assertCounts(final int actCountStart, final int actCountEnd, final int legCountStart, final int legCountEnd,
			final CountingPlanElementHandler actHandler, final CountingPlanElementHandler legHandler) {
		Assert.assertEquals("wrong number of activity starts", actCountStart, actHandler.countStart); // number should not increase!
		Assert.assertEquals("wrong number of activity ends", actCountEnd, actHandler.countEnd);
		Assert.assertEquals("wrong number of leg starts", legCountStart, legHandler.countStart); // number should not increase!
		Assert.assertEquals("wrong number of leg ends", legCountEnd, legHandler.countEnd);
	}


	/*package*/ class CountingPlanElementHandler implements PlanElementHandler {
		public int countStart = 0;
		public int countEnd = 0;
		@Override
		public void handleStart(PlanElement element, Plan plan) {
			this.countStart++;
		}
		@Override
		public void handleEnd(PlanElement element, Plan plan) {
			this.countEnd++;
		}
	}
}
