/* *********************************************************************** *
 * project: org.matsim.*
 * EventsWriterXMLTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.events.implementations;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;

public class EventsWriterXMLTest {

	public static void main(String[] args) throws Exception {
		List<Event> events = collectEvents();
		testSequential(events);
		testParallel(events);
	}
	
	private static List<Event> collectEvents() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		CreateEventsList createEventsList = new CreateEventsList();
		eventsManager.addHandler(createEventsList);
		new EventsReaderXMLv1(eventsManager).parse("../../matsim/mysimulations/berlin/output/ITERS/it.0/0.events.xml.gz");
		return createEventsList.events;
	}
	
	private static void testParallel(List<Event> events) throws Exception {
		EventWriterXML eventWriterXML = new EventWriterXML("../../matsim/mysimulations/berlin/output/ITERS/it.0/0.events2.xml.gz");
		BasicEventHandler[] instances = new BasicEventHandler[2];		
		instances[0] = (BasicEventHandler) eventWriterXML.createInstance();
//		instances[1] = (BasicEventHandler) eventWriterXML.createInstance();
		
		TestRunnable runnable0 = new TestRunnable(instances[0], events);
//		TestRunnable runnable1 = new TestRunnable(instances[1], events);
		
		Thread thread0 = new Thread(runnable0);
//		Thread thread1 = new Thread(runnable1);
		
		thread0.setName("EventsEncodingThread0");
//		thread1.setName("EventsEncodingThread1");
		
		Gbl.startMeasurement();
		thread0.start();
//		thread1.start();
		
		thread0.join();
//		thread1.join();
		
		eventWriterXML.finishEventsHandling();
		eventWriterXML.closeFile();
		Gbl.printElapsedTime();
	}
	
	private static void testSequential(List<Event> events) {
		org.matsim.core.events.algorithms.EventWriterXML eventWriterXML = 
				new org.matsim.core.events.algorithms.EventWriterXML("../../matsim/mysimulations/berlin/output/ITERS/it.0/0.events2.xml.gz");
		
		EventsManager eventsManager1 = EventsUtils.createEventsManager();
		EventsManager eventsManager2 = EventsUtils.createEventsManager();
		
		eventsManager1.addHandler(eventWriterXML);
		eventsManager2.addHandler(eventWriterXML);
		
		Gbl.startMeasurement();
		for (Event event : events) {
			eventsManager1.processEvent(event);
			eventsManager2.processEvent(event);
		}
		eventWriterXML.closeFile();
		Gbl.printElapsedTime();

	}
	
	private static class TestRunnable implements Runnable {

		private List<Event> events;
		private EventsManager eventsManager = EventsUtils.createEventsManager();
		
		public TestRunnable(BasicEventHandler eventHandler, List<Event> events) {
			this.events = events;
			this.eventsManager.addHandler(eventHandler);
		}

		@Override
		public void run() {
			for (Event event : this.events) {
				this.eventsManager.processEvent(event);
				this.eventsManager.processEvent(event);
			}
			Gbl.printThreadCpuTime(Thread.currentThread());
		}
	}
	
	private static class CreateEventsList implements BasicEventHandler {

		List<Event> events = new ArrayList<Event>();
		
		@Override
		public void reset(int iteration) {
			// nothing to do here
		}

		@Override
		public void handleEvent(Event event) {
			this.events.add(event);
		}
		
	}
}
