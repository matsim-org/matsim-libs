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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mrieser.core.sim.api.PlanAgent;
import playground.mrieser.core.sim.fakes.FakeSimEngine;
import playground.mrieser.core.sim.impl.TimestepSimEngineTest.CountingPlanElementHandler;

/**
 * @author mrieser
 */
public class TeleportationHandlerTest {

	@Test
	public void testTeleportation() {
		Plan plan = new PlanImpl();
		Coord c = new CoordImpl(0, 0);
		plan.addActivity(new ActivityImpl("home", c));
		Leg leg = new LegImpl(TransportMode.car);
		leg.setTravelTime(100.0);
		plan.addLeg(leg);
		plan.addActivity(new ActivityImpl("work", c));
		leg = new LegImpl(TransportMode.car);
		leg.setTravelTime(80.0);
		plan.addLeg(leg);
		plan.addActivity(new ActivityImpl("home", c));
		PlanAgent agent = new DefaultPlanAgent(plan);

		FakeSimEngine engine = new FakeSimEngine();
		CountingPlanElementHandler actHandler = new CountingPlanElementHandler();

		TeleportationHandler th = new TeleportationHandler(engine);

		Assert.assertEquals(0, engine.countHandleAgent);
		agent.useNextPlanElement(); // home
		engine.setCurrentTime(5.0 * 3600);
		agent.useNextPlanElement(); // leg
		th.handleDeparture(agent);
		th.doSimStep(5.0 * 3600 + 1.0);
		Assert.assertEquals(0, engine.countHandleAgent);
		th.doSimStep(5.0 * 3600 + 99.0);
		Assert.assertEquals(0, engine.countHandleAgent);
		th.doSimStep(5.0 * 3600 + 100.0);
		Assert.assertEquals(1, engine.countHandleAgent);
		agent.useNextPlanElement(); // work
		agent.useNextPlanElement(); // leg

		engine.setCurrentTime(11.0 * 3600);
		th.handleDeparture(agent);
		th.doSimStep(11.0 * 3600 + 1.0);
		Assert.assertEquals(1, engine.countHandleAgent);
		th.doSimStep(11.0 * 3600 + 79.0);
		Assert.assertEquals(1, engine.countHandleAgent);
		th.doSimStep(11.0 * 3600 + 80.0);
		Assert.assertEquals(2, engine.countHandleAgent);
		th.doSimStep(11.0 * 3600 + 91.0);
		Assert.assertEquals(2, engine.countHandleAgent);
		th.doSimStep(11.0 * 3600 + 2000.0);
		Assert.assertEquals(2, engine.countHandleAgent);
	}

}
