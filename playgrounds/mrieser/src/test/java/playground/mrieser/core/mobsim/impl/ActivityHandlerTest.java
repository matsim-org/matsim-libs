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

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.testcases.utils.LogCounter;

import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.fakes.FakeSimEngine;

/**
 * @author mrieser
 */
public class ActivityHandlerTest {

	@Test
	public void testHandleStartHandleEnd() {
		Fixture f = new Fixture();
		FakeSimEngine engine = new FakeSimEngine();
		EventsCollector eventsCollector = new EventsCollector();
		engine.getEventsManager().addHandler(eventsCollector);
		ActivityHandler ah = new ActivityHandler(engine);

		Assert.assertEquals(0, eventsCollector.getEvents().size());
		Assert.assertEquals(0, engine.countHandleAgent);

		Assert.assertEquals(f.firstHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals("First Activity Start must not generate event.", 0, eventsCollector.getEvents().size());
		Assert.assertEquals(0, engine.countHandleAgent);

		ah.handleEnd(f.agent1);
		Assert.assertEquals(1, eventsCollector.getEvents().size());
		Assert.assertTrue(eventsCollector.getEvents().get(0) instanceof ActivityEndEvent);
		Assert.assertEquals(0, engine.countHandleAgent);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.workAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(2, eventsCollector.getEvents().size());
		Assert.assertEquals(0, engine.countHandleAgent);

		ah.handleEnd(f.agent1);
		Assert.assertEquals(3, eventsCollector.getEvents().size());
		Assert.assertEquals(0, engine.countHandleAgent);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.lastHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(4, eventsCollector.getEvents().size());
		Assert.assertEquals(0, engine.countHandleAgent);

		ah.handleEnd(f.agent1);
		Assert.assertEquals("Last Activity End must not generate event.", 4, eventsCollector.getEvents().size());
		Assert.assertEquals(0, engine.countHandleAgent);
	}

	@Test
	public void testDoSimStep_withDuration_useDuration() {
		Fixture f = new Fixture();
		((ActivityImpl) f.workAct).setMaximumDuration(8.0 * 3600);
		FakeSimEngine engine = new FakeSimEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(true);

		Assert.assertEquals(0, engine.countHandleAgent);

		Assert.assertEquals(f.firstHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);
		ah.handleEnd(f.agent1);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.workAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(16.0 * 3600 - 1.0);
		ah.doSimStep(16.0 * 3600 - 1.0);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);
		ah.handleEnd(f.agent1);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.lastHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(2, engine.countHandleAgent);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);

		engine.setCurrentTime(30.0 * 3600);
		ah.doSimStep(30.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);
	}

	@Test
	public void testDoSimStep_withDuration_ignoreDuration() {
		Fixture f = new Fixture();
		((ActivityImpl) f.workAct).setMaximumDuration(8.0 * 3600);
		FakeSimEngine engine = new FakeSimEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(false);

		Assert.assertEquals(0, engine.countHandleAgent);

		Assert.assertEquals(f.firstHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);

		ah.handleEnd(f.agent1);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.workAct, f.agent1.useNextPlanElement());
		ah.handleStart(f.agent1);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(17.0 * 3600 - 1.0);
		ah.doSimStep(17.0 * 3600 - 1.0);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(17.0 * 3600);
		ah.doSimStep(17.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);
		ah.handleEnd(f.agent1);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.lastHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(2, engine.countHandleAgent);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);

		engine.setCurrentTime(30.0 * 3600);
		ah.doSimStep(30.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);
	}

	/**
	 * mainly test that there is no exception when durations
	 * should be used but are not available.
	 */
	@Test
	public void testDoSimStep_withoutDuration_useDuration() {
		Fixture f = new Fixture();
		FakeSimEngine engine = new FakeSimEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(true);

		Assert.assertEquals(0, engine.countHandleAgent);

		Assert.assertEquals(f.firstHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);
		ah.handleEnd(f.agent1);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.workAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(17.0 * 3600 - 1.0);
		ah.doSimStep(17.0 * 3600 - 1.0);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(17.0 * 3600);
		ah.doSimStep(17.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);
		ah.handleEnd(f.agent1);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.lastHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(2, engine.countHandleAgent);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);

		engine.setCurrentTime(30.0 * 3600);
		ah.doSimStep(30.0 * 3600);
		Assert.assertEquals(2, engine.countHandleAgent);
	}

	/**
	 * check that an activity with missing end time is handled
	 * with a log statement.
	 */
	@Test
	public void testDoSimStep_missingEndTime_useDuration() {
		Fixture f = new Fixture();
		f.workAct.setEndTime(Time.UNDEFINED_TIME);
		FakeSimEngine engine = new FakeSimEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(true);

		LogCounter logCounter = new LogCounter(Level.ERROR);
		logCounter.activiate();

		Assert.assertEquals(0, engine.countHandleAgent);

		Assert.assertEquals(f.firstHomeAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleAgent);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);
		ah.handleEnd(f.agent1);

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.workAct, f.agent1.useNextPlanElement());

		Assert.assertEquals(0, logCounter.getErrorCount());
		ah.handleStart(f.agent1);
		Assert.assertEquals(1, logCounter.getErrorCount());
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(17.0 * 3600);
		ah.doSimStep(17.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(1, engine.countHandleAgent);

		logCounter.deactiviate();
		Assert.assertEquals(1, logCounter.getErrorCount());
	}

	@Test
	public void testKeepAlive() {
		Fixture f = new Fixture();
		FakeSimEngine engine = new FakeSimEngine();
		ActivityHandler ah = new ActivityHandler(engine);

		Assert.assertEquals(f.firstHomeAct, f.agent1.useNextPlanElement());

		Assert.assertFalse(ah.keepAlive());
		ah.handleStart(f.agent1);
		Assert.assertTrue(ah.keepAlive());

		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertTrue(ah.keepAlive());

		ah.doSimStep(8.0 * 3600);
		Assert.assertFalse(ah.keepAlive());

		f.agent1.useNextPlanElement(); // leg

		Assert.assertEquals(f.workAct, f.agent1.useNextPlanElement());

		ah.handleStart(f.agent1);
		Assert.assertTrue(ah.keepAlive());

		ah.doSimStep(16.0 * 3600);
		Assert.assertTrue(ah.keepAlive());

		ah.doSimStep(17.0 * 3600 - 1);
		Assert.assertTrue(ah.keepAlive());

		ah.doSimStep(17.0 * 3600);
		Assert.assertFalse(ah.keepAlive());

		ah.doSimStep(24.0 * 3600);
		Assert.assertFalse(ah.keepAlive());
	}

	private static class Fixture {
		public final Person person1;
		public final Plan plan1;
		public final Activity firstHomeAct;
		public final Activity workAct;
		public final Activity lastHomeAct;
		public final PlanAgent agent1;

		public Fixture() {
			this.person1 = new PersonImpl(new IdImpl("1"));
			this.plan1 = new PlanImpl();
			this.person1.addPlan(this.plan1);
			Coord c = new CoordImpl(0, 0);

			this.firstHomeAct = new ActivityImpl("home", c);
			this.firstHomeAct.setEndTime(8.0 * 3600);
			this.plan1.addActivity(this.firstHomeAct);

			Leg leg = new LegImpl(TransportMode.car);
			this.plan1.addLeg(leg);

			this.workAct = new ActivityImpl("work", c);
			this.workAct.setEndTime(17.0 * 3600);
			this.plan1.addActivity(this.workAct);

			leg = new LegImpl(TransportMode.car);
			this.plan1.addLeg(leg);

			this.lastHomeAct = new ActivityImpl("work", c);
			this.plan1.addActivity(this.lastHomeAct);

			this.agent1 = new DefaultPlanAgent(plan1, 1.0);
		}
	}

	private static class EventsCounter implements BasicEventHandler {

		public int count = 0;

		@Override
		public void reset(final int iteration) {
			this.count = 0;
		}

		@Override
		public void handleEvent(final Event event) {
			this.count++;
		}

	}

}
