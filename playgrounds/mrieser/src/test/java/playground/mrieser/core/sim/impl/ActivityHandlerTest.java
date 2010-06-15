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

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.utils.LogCounter;

import playground.mrieser.core.sim.api.NewSimEngine;

/**
 * @author mrieser
 */
public class ActivityHandlerTest {

	@Test
	public void testHandleStartHandleEnd() {
		Fixture f = new Fixture();
		SimTestEngine engine = new SimTestEngine();
		EventsCounter eventsCounter = new EventsCounter();
		engine.getEventsManager().addHandler(eventsCounter);
		ActivityHandler ah = new ActivityHandler(engine);

		Assert.assertEquals(0, eventsCounter.count);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleStart(f.firstHomeAct, f.plan1);
		Assert.assertEquals(1, eventsCounter.count);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleEnd(f.firstHomeAct, f.plan1);
		Assert.assertEquals(2, eventsCounter.count);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleStart(f.workAct, f.plan1);
		Assert.assertEquals(3, eventsCounter.count);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleEnd(f.workAct, f.plan1);
		Assert.assertEquals(4, eventsCounter.count);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleStart(f.lastHomeAct, f.plan1);
		Assert.assertEquals(5, eventsCounter.count);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleEnd(f.lastHomeAct, f.plan1);
		Assert.assertEquals(6, eventsCounter.count);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);
	}

	@Test
	public void testDoSimStep_withDuration_useDuration() {
		Fixture f = new Fixture();
		((ActivityImpl) f.workAct).setDuration(8.0 * 3600);
		SimTestEngine engine = new SimTestEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(true);

		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleStart(f.firstHomeAct, f.plan1);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);
		ah.handleEnd(f.firstHomeAct, f.plan1);

		ah.handleStart(f.workAct, f.plan1);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(16.0 * 3600 - 1.0);
		ah.doSimStep(16.0 * 3600 - 1.0);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);
		ah.handleEnd(f.workAct, f.plan1);

		ah.handleStart(f.lastHomeAct, f.plan1);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);

		engine.setCurrentTime(30.0 * 3600);
		ah.doSimStep(30.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);
	}

	@Test
	public void testDoSimStep_withDuration_ignoreDuration() {
		Fixture f = new Fixture();
		((ActivityImpl) f.workAct).setDuration(8.0 * 3600);
		SimTestEngine engine = new SimTestEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(false);

		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleStart(f.firstHomeAct, f.plan1);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);
		ah.handleEnd(f.firstHomeAct, f.plan1);

		ah.handleStart(f.workAct, f.plan1);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(17.0 * 3600 - 1.0);
		ah.doSimStep(17.0 * 3600 - 1.0);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(17.0 * 3600);
		ah.doSimStep(17.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);
		ah.handleEnd(f.workAct, f.plan1);

		ah.handleStart(f.lastHomeAct, f.plan1);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);

		engine.setCurrentTime(30.0 * 3600);
		ah.doSimStep(30.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);
	}

	/**
	 * mainly test that there is no exception when durations
	 * should be used but are not available.
	 */
	@Test
	public void testDoSimStep_withoutDuration_useDuration() {
		Fixture f = new Fixture();
		SimTestEngine engine = new SimTestEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(true);

		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleStart(f.firstHomeAct, f.plan1);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);
		ah.handleEnd(f.firstHomeAct, f.plan1);

		ah.handleStart(f.workAct, f.plan1);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(17.0 * 3600 - 1.0);
		ah.doSimStep(17.0 * 3600 - 1.0);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(17.0 * 3600);
		ah.doSimStep(17.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);
		ah.handleEnd(f.workAct, f.plan1);

		ah.handleStart(f.lastHomeAct, f.plan1);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);

		engine.setCurrentTime(30.0 * 3600);
		ah.doSimStep(30.0 * 3600);
		Assert.assertEquals(2, engine.countHandleNextPlanElement);
	}

	/**
	 * mainly test that there is no exception when durations
	 * should be used but are not available.
	 */
	@Test
	public void testDoSimStep_missingEndTime_useDuration() {
		Fixture f = new Fixture();
		f.workAct.setEndTime(Time.UNDEFINED_TIME);
		SimTestEngine engine = new SimTestEngine();
		ActivityHandler ah = new ActivityHandler(engine);
		ah.setUseActivityDurations(true);

		LogCounter logCounter = new LogCounter(Level.ERROR);
		logCounter.activiate();

		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		ah.handleStart(f.firstHomeAct, f.plan1);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600 - 1.0);
		ah.doSimStep(8.0 * 3600 - 1.0);
		Assert.assertEquals(0, engine.countHandleNextPlanElement);

		engine.setCurrentTime(8.0 * 3600);
		ah.doSimStep(8.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);
		ah.handleEnd(f.firstHomeAct, f.plan1);

		Assert.assertEquals(0, logCounter.getErrorCount());
		ah.handleStart(f.workAct, f.plan1);
		Assert.assertEquals(1, logCounter.getErrorCount());
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(16.0 * 3600);
		ah.doSimStep(16.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(17.0 * 3600);
		ah.doSimStep(17.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		engine.setCurrentTime(24.0 * 3600);
		ah.doSimStep(24.0 * 3600);
		Assert.assertEquals(1, engine.countHandleNextPlanElement);

		logCounter.deactiviate();
		Assert.assertEquals(1, logCounter.getErrorCount());
	}

	private static class Fixture {
		public final Person person1;
		public final Plan plan1;
		public final Activity firstHomeAct;
		public final Activity workAct;
		public final Activity lastHomeAct;

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
		}
	}

	private static class SimTestEngine implements NewSimEngine {

		private final EventsManager em = new EventsManagerImpl();
		private double time;
		public int countHandleNextPlanElement = 0;

		@Override
		public double getCurrentTime() {
			return this.time;
		}

		public void setCurrentTime(final double time) {
			this.time = time;
		}

		@Override
		public EventsManager getEventsManager() {
			return this.em;
		}

		@Override
		public void handleNextPlanElement(final Plan plan) {
			this.countHandleNextPlanElement++;
		}

		@Override
		public void runSim() {
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
