/* *********************************************************************** *
 * project: org.matsim.*
 * EventsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.events;

import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.events.algorithms.TravelTimeCalculator;
import org.matsim.events.handler.BasicEventHandlerI;
import org.matsim.events.handler.EventHandlerAgentNoRouteI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.world.World;

// TODO what about making this an official testcase?

public class EventsTest {

	public static void writeEvents() {

		System.out.println("  creating events object... ");
		Events events = new Events();
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		String filename = Gbl.getConfig().events().getOutputFile();
		if (filename == null) {
			filename = "tt_kill.txt";
		}
		EventWriterTXT writer = new EventWriterTXT(filename);
		events.addHandler(writer);
		System.out.println("  done");

		events.processEvent(new EventAgentDeparture(8*3600, "1", 1, "1"));
		events.processEvent(new EventAgentArrival(9*3600, "1", 1, "1"));

		writer.reset(0);
	}

	public static void readEvents01() {

		World world = Gbl.getWorld();

		// network
		System.out.println("  creating router network object... ");
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		NetworkLayer network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);;
		System.out.println("  done.");

		System.out.println("  reading network file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		// events
		System.out.println("  creating events object... ");
		Events events = new Events();
		System.out.println("  done.");

		System.out.println("  creating events reader...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		System.out.println("  done.");

		System.out.println("  adding events algorithms...");
		TravelTimeCalculator ttcAlgo = new TravelTimeCalculator(network);
		events.addHandler(ttcAlgo);
		System.out.println("  done");

		// read file and run algos
		System.out.println("  reading events file and (probably) running events algos");
		reader.readFile(Gbl.getConfig().events().getInputFile());
		System.out.println("  done.");

	}

	//TEST cases
	// speed test Result for 1Million calls:
	// conventional call costs 10 msecs
	// invoke()style call costs 862 msecs

	static class AllHandler implements BasicEventHandlerI{

		public void handleEvent(final BasicEvent event) {
			int i = 0;
			i++;
			//System.out.println("Handle all event");
		}
		public void reset(final int iteration) {
		}

	}

	static class TestHandler implements EventHandlerAgentNoRouteI, EventHandlerLinkEnterI{

		public void handleEvent(final EventAgentNoRoute event) {
			System.out.println("Handle agent event");

		}

		public void reset(final int iteration) {
		}

		public void handleEvent(final EventLinkEnter event) {
			System.out.println("Handle link event");
		}
	}

	static class TestHandler2 implements BasicEventHandlerI, EventHandlerAgentNoRouteI, EventHandlerLinkEnterI{

		public void handleEvent(final BasicEvent event) {
			System.out.println("Handle basic event");
			if(event.getClass() == EventAgentNoRoute.class) {
				System.out.println("Handle basic event is type NoRoute");
			}
		}

		public void reset(final int iteration) {
		}

		public void handleEvent(final EventAgentNoRoute event) {
			System.out.println("Handle agent event");
		}

		public void handleEvent(final EventLinkEnter event) {
			System.out.println("Handle link event");
		}
	}


	public static void speedTest() {
		Events events = new Events();
		AllHandler ta = new AllHandler();
		BasicEvent ev = new EventAgentNoRoute(0,"1",10,"2");

        long start_ms = System.currentTimeMillis(); // |-- START CLOCK -->
		for (int i = 0; i < 1000000; i++) {
			ta.handleEvent(ev);
		}
        long end_ms = System.currentTimeMillis();
        long dur = end_ms -start_ms;
        System.out.println("conventional call costs " + dur + " msecs");

		events.addHandler(ta);
//		List<? extends EventHandlerI> list = events.map.get(BasicEvent.class);
//		BasicEventHandlerI handi = (BasicEventHandlerI)list.get(0);
//		Method method = events.methods.get(BasicEvent.class);
//
//        start_ms = System.currentTimeMillis(); // |-- START CLOCK -->
//		for (int i = 0; i < 1000000; i++) {
//			//events.computeEvent(ev);
//			try {
//				method.invoke(handi,ev);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//        end_ms = System.currentTimeMillis();
//        dur = end_ms -start_ms;
//        System.out.println("invoke()style call costs " + dur + " msecs");

        start_ms = System.currentTimeMillis(); // |-- START CLOCK -->
		for (int i = 0; i < 1000000; i++) {
			events.computeEvent(ev);
			//events.callHandlerFast(ev.getClass(), ev,ta);
		}
        end_ms = System.currentTimeMillis();
        dur = end_ms -start_ms;
        System.out.println("compute event call costs " + dur + " msecs");
	}

	static void functionalTest() {
		Events events = new Events();
		TestHandler tt = new TestHandler();
		TestHandler2 t2 = new TestHandler2();
		AllHandler ta = new AllHandler();
		events.addHandler(tt);
		events.addHandler(t2);
		events.addHandler(ta);
        System.out.println("--------------------------------------");
		events.computeEvent(new EventAgentNoRoute(0,"1",1,"2"));
        System.out.println("--------------------------------------");
		events.computeEvent(new EventAgentArrival(0,"2",0,"3"));
        System.out.println("--------------------------------------");
		events.computeEvent(new EventLinkLeave(0, "1", 0, "1"));
        System.out.println("--------------------------------------");
		events.computeEvent(new EventLinkEnter(0, "2", 0, "1"));
        System.out.println("--------------------------------------");
		BasicEvent ev = new EventAgentNoRoute(0,"3",0,"30");
		events.computeEvent(ev);
        System.out.println("--------------------------------------");
        events.removeHandler(t2);
        events.clearHandlers();
	}

	public static void main(final String[] args) throws Exception {

		Gbl.createConfig(args);
		Gbl.createWorld();
//		Gbl.createFacilities();
		//speedTest();
		functionalTest();

		writeEvents();
//		readEvents01();

	}

}
