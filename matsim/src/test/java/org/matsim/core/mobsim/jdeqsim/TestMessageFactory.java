
/* *********************************************************************** *
 * project: org.matsim.*
 * TestMessageFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.jdeqsim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.testcases.MatsimTestUtils;

	public class TestMessageFactory {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	 // check if gc turned on
	 @Test
	 void testMessageFactory1(){
		MessageFactory.GC_ALL_MESSAGES();
		JDEQSimConfigGroup.setGC_MESSAGES(true);
		MessageFactory.disposeEndLegMessage(new EndLegMessage(null,null, TimeInterpretation.create(ConfigUtils.createConfig())));
		MessageFactory.disposeEnterRoadMessage(new EnterRoadMessage(null,null));
		MessageFactory.disposeStartingLegMessage(new StartingLegMessage(null,null));
		MessageFactory.disposeLeaveRoadMessage(new LeaveRoadMessage(null,null));
		MessageFactory.disposeEndRoadMessage(new EndRoadMessage(null,null));
		MessageFactory.disposeDeadlockPreventionMessage(new DeadlockPreventionMessage(null,null));

		assertEquals(0, MessageFactory.getEndLegMessageQueue().size());
		assertEquals(0, MessageFactory.getEnterRoadMessageQueue().size());
		assertEquals(0, MessageFactory.getStartingLegMessageQueue().size());
		assertEquals(0, MessageFactory.getLeaveRoadMessageQueue().size());
		assertEquals(0, MessageFactory.getEndRoadMessageQueue().size());
		assertEquals(0, MessageFactory.getEndLegMessageQueue().size());
	}

	 // check when gc turned off
	 @Test
	 void testMessageFactory2(){
		MessageFactory.GC_ALL_MESSAGES();
		JDEQSimConfigGroup.setGC_MESSAGES(false);
		MessageFactory.disposeEndLegMessage(new EndLegMessage(null,null, TimeInterpretation.create(ConfigUtils.createConfig())));
		MessageFactory.disposeEnterRoadMessage(new EnterRoadMessage(null,null));
		MessageFactory.disposeStartingLegMessage(new StartingLegMessage(null,null));
		MessageFactory.disposeLeaveRoadMessage(new LeaveRoadMessage(null,null));
		MessageFactory.disposeEndRoadMessage(new EndRoadMessage(null,null));
		MessageFactory.disposeDeadlockPreventionMessage(new DeadlockPreventionMessage(null,null));

		assertEquals(1, MessageFactory.getEndLegMessageQueue().size());
		assertEquals(1, MessageFactory.getEnterRoadMessageQueue().size());
		assertEquals(1, MessageFactory.getStartingLegMessageQueue().size());
		assertEquals(1, MessageFactory.getLeaveRoadMessageQueue().size());
		assertEquals(1, MessageFactory.getEndRoadMessageQueue().size());
		assertEquals(1, MessageFactory.getEndLegMessageQueue().size());
	}

	 // check check use of Message factory
	 @Test
	 void testMessageFactory3(){
		MessageFactory.GC_ALL_MESSAGES();
		JDEQSimConfigGroup.setGC_MESSAGES(false);
		MessageFactory.disposeEndLegMessage(new EndLegMessage(null,null, TimeInterpretation.create(ConfigUtils.createConfig())));
		MessageFactory.disposeEnterRoadMessage(new EnterRoadMessage(null,null));
		MessageFactory.disposeStartingLegMessage(new StartingLegMessage(null,null));
		MessageFactory.disposeLeaveRoadMessage(new LeaveRoadMessage(null,null));
		MessageFactory.disposeEndRoadMessage(new EndRoadMessage(null,null));
		MessageFactory.disposeDeadlockPreventionMessage(new DeadlockPreventionMessage(null,null));

		MessageFactory.getEndLegMessage(null, null, TimeInterpretation.create(ConfigUtils.createConfig()));
		MessageFactory.getEnterRoadMessage(null, null);
		MessageFactory.getStartingLegMessage(null, null);
		MessageFactory.getLeaveRoadMessage(null, null);
		MessageFactory.getEndRoadMessage(null, null);
		MessageFactory.getDeadlockPreventionMessage(null, null);

		assertEquals(0, MessageFactory.getEndLegMessageQueue().size());
		assertEquals(0, MessageFactory.getEnterRoadMessageQueue().size());
		assertEquals(0, MessageFactory.getStartingLegMessageQueue().size());
		assertEquals(0, MessageFactory.getLeaveRoadMessageQueue().size());
		assertEquals(0, MessageFactory.getEndRoadMessageQueue().size());
		assertEquals(0, MessageFactory.getEndLegMessageQueue().size());
	}

	 // check initialization using constructer
	 @Test
	 void testMessageFactory5(){
		MessageFactory.GC_ALL_MESSAGES();
		JDEQSimConfigGroup.setGC_MESSAGES(true);
		Scheduler scheduler=new Scheduler(new MessageQueue());
		Person person= PopulationUtils.getFactory().createPerson(Id.create("abc", Person.class));

		TimeInterpretation timeInterpretation = TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime, PlansConfigGroup.TripDurationHandling.ignoreDelays);
		Vehicle vehicle=new Vehicle(scheduler, person, timeInterpretation );

		assertEquals(true,MessageFactory.getEndLegMessage(scheduler, vehicle, timeInterpretation).scheduler==scheduler);
		assertEquals(true,MessageFactory.getEnterRoadMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getStartingLegMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getLeaveRoadMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getEndRoadMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getDeadlockPreventionMessage(scheduler, vehicle).scheduler==scheduler);

		assertEquals(true,MessageFactory.getEndLegMessage(scheduler, vehicle, timeInterpretation).vehicle==vehicle);
		assertEquals(true,MessageFactory.getEnterRoadMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getStartingLegMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getLeaveRoadMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getEndRoadMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getDeadlockPreventionMessage(scheduler, vehicle).vehicle==vehicle);
	}

	 // check initialization using rest
	 @Test
	 void testMessageFactory6(){
		MessageFactory.GC_ALL_MESSAGES();
		JDEQSimConfigGroup.setGC_MESSAGES(false);
		Scheduler scheduler=new Scheduler(new MessageQueue());
		Person person= PopulationUtils.getFactory().createPerson(Id.create("abc", Person.class));

		TimeInterpretation timeInterpretation = TimeInterpretation.create(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime, PlansConfigGroup.TripDurationHandling.ignoreDelays);
		Vehicle vehicle=new Vehicle(scheduler, person, timeInterpretation );

		assertEquals(true,MessageFactory.getEndLegMessage(scheduler, vehicle, timeInterpretation).scheduler==scheduler);
		assertEquals(true,MessageFactory.getEnterRoadMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getStartingLegMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getLeaveRoadMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getEndRoadMessage(scheduler, vehicle).scheduler==scheduler);
		assertEquals(true,MessageFactory.getDeadlockPreventionMessage(scheduler, vehicle).scheduler==scheduler);

		assertEquals(true,MessageFactory.getEndLegMessage(scheduler, vehicle, timeInterpretation).vehicle==vehicle);
		assertEquals(true,MessageFactory.getEnterRoadMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getStartingLegMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getLeaveRoadMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getEndRoadMessage(scheduler, vehicle).vehicle==vehicle);
		assertEquals(true,MessageFactory.getDeadlockPreventionMessage(scheduler, vehicle).vehicle==vehicle);
	}


}
