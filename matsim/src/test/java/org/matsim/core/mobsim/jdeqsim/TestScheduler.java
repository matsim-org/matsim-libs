
/* *********************************************************************** *
 * project: org.matsim.*
 * TestScheduler.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.mobsim.jdeqsim.util.DummyMessage;
import org.matsim.core.mobsim.jdeqsim.util.DummyMessage1;
import org.matsim.core.mobsim.jdeqsim.util.DummySimUnit;

	public class TestScheduler {

	 // the time at the end of the simulation is equal to the time of the last message processed
	 @Test
	 void testSchedule1(){
		Scheduler scheduler=new Scheduler(new MessageQueue());
		SimUnit sm1=new DummySimUnit(scheduler);
		Message m1=new DummyMessage();
		sm1.sendMessage(m1, sm1, 9000);
		scheduler.startSimulation();
		Assertions.assertEquals(9000.0, scheduler.getSimTime(), 0.0);
	}

	 // a message is scheduled and unscheduled before starting the simulation
	 // this causes the simulation to stop immediatly (because no messages in queue)
	 @Test
	 void testUnschedule(){
		Scheduler scheduler=new Scheduler(new MessageQueue());
		SimUnit sm1=new DummySimUnit(scheduler);
		Message m1=new DummyMessage();
		sm1.sendMessage(m1, sm1, 1);
		scheduler.unschedule(m1);
		scheduler.startSimulation();
		Assertions.assertEquals(0.0, scheduler.getSimTime(), 0.0);
	}

	 // We shedule two messages, but the first message deletes upon handling the message the second message.
	 // This results in that the simulation stops not at time 10, but immediatly at time 1.
	 @Test
	 void testUnschedule2(){
		Scheduler scheduler=new Scheduler(new MessageQueue());
		SimUnit sm1=new DummySimUnit(scheduler);
		Message m1=new DummyMessage();
		sm1.sendMessage(m1, sm1, 10);

		DummyMessage1 m2=new DummyMessage1();
		m2.messageToUnschedule=m1;
		sm1.sendMessage(m2, sm1, 1);


		scheduler.startSimulation();
		Assertions.assertEquals(1.0, scheduler.getSimTime(), 0.0);
	}


}
