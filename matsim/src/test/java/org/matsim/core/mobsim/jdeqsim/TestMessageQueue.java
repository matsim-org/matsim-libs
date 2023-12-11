
/* *********************************************************************** *
 * project: org.matsim.*
 * TestMessageQueue.java
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
import org.matsim.core.mobsim.jdeqsim.util.DummyMessage;
import org.matsim.testcases.MatsimTestUtils;


	public class TestMessageQueue {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testPutMessage1(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.setMessageArrivalTime(1);

		Message m2=new DummyMessage();
		m2.setMessageArrivalTime(2);

		mq.putMessage(m1);
		mq.putMessage(m2);
		assertEquals(2, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m1);
	}

	 @Test
	 void testPutMessage2(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.setMessageArrivalTime(2);

		Message m2=new DummyMessage();
		m2.setMessageArrivalTime(1);

		mq.putMessage(m1);
		mq.putMessage(m2);
		assertEquals(2, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m2);
	}

	 @Test
	 void testPutMessage3(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.setMessageArrivalTime(2);

		Message m2=new DummyMessage();
		m2.setMessageArrivalTime(1);

		Message m3=new DummyMessage();
		m3.setMessageArrivalTime(1);

		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.putMessage(m3);
		assertEquals(3, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage().getMessageArrivalTime()==1);
	}

	 @Test
	 void testRemoveMessage1(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.setMessageArrivalTime(1);

		Message m2=new DummyMessage();
		m2.setMessageArrivalTime(2);

		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.removeMessage(m1);
		assertEquals(1, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m2);
		assertEquals(0, mq.getQueueSize());
	}

	 @Test
	 void testRemoveMessage2(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.setMessageArrivalTime(1);

		Message m2=new DummyMessage();
		m2.setMessageArrivalTime(2);

		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.removeMessage(m2);
		assertEquals(1, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m1);
		assertEquals(0, mq.getQueueSize());
	}

	 @Test
	 void testRemoveMessage3(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.setMessageArrivalTime(1);

		Message m2=new DummyMessage();
		m2.setMessageArrivalTime(1);

		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.removeMessage(m1);
		assertEquals(1, mq.getQueueSize());
		assertEquals(false, mq.isEmpty());
		assertEquals(true, mq.getNextMessage()==m2);
		assertEquals(0, mq.getQueueSize());
		assertEquals(true, mq.isEmpty());
	}

	 // a higher priority message will be at front of queue, if there are
	 // several messages with same time
	 @Test
	 void testMessagePriority(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.setMessageArrivalTime(1);
		m1.setPriority(10);

		Message m2=new DummyMessage();
		m2.setMessageArrivalTime(1);
		m2.setPriority(5);

		Message m3=new DummyMessage();
		m3.setMessageArrivalTime(1);
		m3.setPriority(20);

		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.putMessage(m3);

		assertEquals(true, mq.getNextMessage()==m3);
		assertEquals(true, mq.getNextMessage()==m1);
		assertEquals(true, mq.getNextMessage()==m2);
		assertEquals(0, mq.getQueueSize());
		assertEquals(true, mq.isEmpty());
	}



}
