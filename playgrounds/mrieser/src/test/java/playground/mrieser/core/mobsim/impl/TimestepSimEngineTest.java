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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.PlanElementHandler;
import playground.mrieser.core.mobsim.api.PlanSimulation;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.impl.PlanSimulationImpl;

/**
 * @author mrieser
 */
public class TimestepSimEngineTest {
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
		PlanAgent agent = new DefaultPlanAgent(plan);

		PlanSimulation planSim = new PlanSimulationImpl(null);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, null);
		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		planSim.setPlanElementHandler(Activity.class, actHandler);
		planSim.setPlanElementHandler(Leg.class, legHandler);

		assertCounts(0, 0, 0, 0, actHandler, legHandler);
		engine.handleAgent(agent); // home act
		assertCounts(1, 0, 0, 0, actHandler, legHandler);
		engine.handleAgent(agent); // leg
		assertCounts(1, 1, 1, 0, actHandler, legHandler);
		engine.handleAgent(agent); // work act
		assertCounts(2, 1, 1, 1, actHandler, legHandler);
		engine.handleAgent(agent); // leg
		assertCounts(2, 2, 2, 1, actHandler, legHandler);
		engine.handleAgent(agent); // shop act
		assertCounts(3, 2, 2, 2, actHandler, legHandler);
		engine.handleAgent(agent); // leg
		assertCounts(3, 3, 3, 2, actHandler, legHandler);
		engine.handleAgent(agent); // home act
		assertCounts(4, 3, 3, 3, actHandler, legHandler);
		engine.handleAgent(agent); // proceed, just finish home act
		assertCounts(4, 4, 3, 3, actHandler, legHandler);
		engine.handleAgent(agent); // nothing to proceed
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
		PlanAgent agent1 = new DefaultPlanAgent(plan1);
		Plan plan2 = new PlanImpl();
		plan2.addActivity(new ActivityImpl("home", c));
		plan2.addLeg(new LegImpl(TransportMode.car));
		plan2.addActivity(new ActivityImpl("leisure", c));
		plan2.addLeg(new LegImpl(TransportMode.car));
		plan2.addActivity(new ActivityImpl("home", c));
		PlanAgent agent2 = new DefaultPlanAgent(plan2);

		PlanSimulation planSim = new PlanSimulationImpl(null);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, null);
		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();
		CountingPlanElementHandler legHandler = new CountingPlanElementHandler();

		planSim.setPlanElementHandler(Activity.class, actHandler);
		planSim.setPlanElementHandler(Leg.class, legHandler);

		assertCounts(0, 0, 0, 0, actHandler, legHandler);
		engine.handleAgent(agent1); // 1.home act
		assertCounts(1, 0, 0, 0, actHandler, legHandler);
		engine.handleAgent(agent1); // 1.leg
		assertCounts(1, 1, 1, 0, actHandler, legHandler);
		engine.handleAgent(agent2); // 2.home act
		assertCounts(2, 1, 1, 0, actHandler, legHandler);
		engine.handleAgent(agent1); // 1.work act
		assertCounts(3, 1, 1, 1, actHandler, legHandler);
		engine.handleAgent(agent2); // 2.leg
		assertCounts(3, 2, 2, 1, actHandler, legHandler);
		engine.handleAgent(agent2); // 2.leisure
		assertCounts(4, 2, 2, 2, actHandler, legHandler);
		engine.handleAgent(agent1); // 1.leg
		assertCounts(4, 3, 3, 2, actHandler, legHandler);
		engine.handleAgent(agent1); // 1.shop act
		assertCounts(5, 3, 3, 3, actHandler, legHandler);
		engine.handleAgent(agent1); // 1.leg
		assertCounts(5, 4, 4, 3, actHandler, legHandler);
		engine.handleAgent(agent2); // 2.leg
		assertCounts(5, 5, 5, 3, actHandler, legHandler);
		engine.handleAgent(agent1); // 1.home act
		assertCounts(6, 5, 5, 4, actHandler, legHandler);
		engine.handleAgent(agent2); // 2.home act
		assertCounts(7, 5, 5, 5, actHandler, legHandler);
	}

	private void assertCounts(final int actCountStart, final int actCountEnd, final int legCountStart, final int legCountEnd,
			final CountingPlanElementHandler actHandler, final CountingPlanElementHandler legHandler) {
		Assert.assertEquals("wrong number of activity starts", actCountStart, actHandler.countStart); // number should not increase!
		Assert.assertEquals("wrong number of activity ends", actCountEnd, actHandler.countEnd);
		Assert.assertEquals("wrong number of leg starts", legCountStart, legHandler.countStart); // number should not increase!
		Assert.assertEquals("wrong number of leg ends", legCountEnd, legHandler.countEnd);
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
