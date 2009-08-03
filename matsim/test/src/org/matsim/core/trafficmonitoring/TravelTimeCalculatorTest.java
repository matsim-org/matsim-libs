/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorTest.java
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

package org.matsim.core.trafficmonitoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class TravelTimeCalculatorTest extends MatsimTestCase {

	public final void testTravelTimeCalculator_Array_Optimistic() throws IOException {
		String compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		ScenarioImpl scenario = new ScenarioImpl();

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;
		
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataArrayFactory(scenario.getNetwork(), numSlots), 
				new OptimisticTravelTimeAggregator(numSlots, binSize), binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Optimistic() throws IOException {
		String compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		ScenarioImpl scenario = new ScenarioImpl();
		
		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;
		
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()), 
				new OptimisticTravelTimeAggregator(numSlots, binSize), binSize, compareFile, false);
	}
	
	public final void testTravelTimeCalculator_HashMap_Pessimistic() throws IOException {
		String compareFile = getClassInputDirectory() + "link10_ttimes_pessimistic.txt";
		ScenarioImpl scenario = new ScenarioImpl();
		
		int endTime = 12*3600;
		int binSize = 1*60;
		int numSlots = (endTime / binSize) + 1;
		
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				new PessimisticTravelTimeAggregator(binSize, numSlots), binSize, compareFile, false);
	}

	private final void doTravelTimeCalculatorTest(final ScenarioImpl scenario, final TravelTimeDataFactory ttDataFactory, 
			final AbstractTravelTimeAggregator aggregator, final int timeBinSize,
			final String compareFile, final boolean generateNewData) throws IOException {
		String networkFile = getClassInputDirectory() + "link10_network.xml";
		String eventsFile = getClassInputDirectory() + "link10_events.txt";

		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		Events events = new Events();
		List<BasicEvent> eventsList = new LinkedList<BasicEvent>();
		events.addHandler(new EventsCollector(eventsList));
		new MatsimEventsReader(events).readFile(eventsFile);
		events.printEventsCount();
		
		Events events2 = new Events();
		
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 30*3600, scenario.getConfig().travelTimeCalculator());
		ttcalc.setTravelTimeAggregator(aggregator);
		ttcalc.setTravelTimeDataFactory(ttDataFactory);
		events2.addHandler(ttcalc);
		for (BasicEvent e : eventsList){
			events2.processEvent(e);
		}
		
		// read comparison data
		BufferedReader infile = new BufferedReader(new FileReader(compareFile));
		String line;
		String[] compareData = new String[4*24];
		try {
			for (int i = 0; i < 4*24; i++) {
				line = infile.readLine();
				compareData[i] = line;
			}
		}
		finally {
			try {
				infile.close();
			} catch (IOException ignored) {}
		}

		// prepare comparison
		LinkImpl link10 = network.getLinks().get(new IdImpl("10"));

		if (generateNewData) {
			BufferedWriter outfile = null;
			try {
				outfile = new BufferedWriter(new FileWriter(compareFile));
				for (int i = 0; i < 4*24; i++) {
					double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
					outfile.write(Double.toString(ttime) + "\n");
				}
			}
			finally {
				if (outfile != null) {
					try {
						outfile.close();
					} catch (IOException ignored) {}
				}
			}
			fail("A new file containg data for comparison was created. No comparison was made.");
		}

		// do comparison
		for (int i = 0; i < 4*24; i++) {
			double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
			assertEquals(compareData[i], Double.toString(ttime));
		}
	}
	
	/**
	 * @author mrieser
	 */
	public void testLongTravelTimeInEmptySlot() {
		ScenarioImpl scenario = new ScenarioImpl(loadConfig(null));

		NetworkLayer network = (NetworkLayer) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		NodeImpl node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		NodeImpl node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		LinkImpl link1 = network.createLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 3600.0, 1.0);

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		PersonImpl person = new PersonImpl(new IdImpl(1));
		
		// generate some events that suggest a really long travel time
		double linkEnterTime1 = 7.0 * 3600 + 10;
		double linkTravelTime1 = 50.0 * 60; // 50minutes!
		double linkEnterTime2 = 7.75 * 3600 + 10;
		double linkTravelTime2 = 10.0 * 60; // 10minutes!
		
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1, person, link1));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime1 + linkTravelTime1, person, link1));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, person, link1));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime2, person, link1));

		assertEquals(50 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(10 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime2 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize
	}
	
	/**
	 * Tests that calculating LinkTravelTimes works also without reading in a complete scenario including population.
	 * 
	 * @author mrieser
	 * 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public void testReadFromFile_LargeScenarioCase() throws SAXException, ParserConfigurationException, IOException {
		/* Assume, you have a big events file from a huge scenario and you want to do data-mining...
		 * Then you likely want to calculate link travel times. This requires the network, but NOT
		 * the population. Thus, using "new Events(new EventsBuilderImpl(scenario))" is not appropriate
		 * and may not even be usable if the population is too big to read it in on a laptop for
		 * post-processing. So, it must be possible to only read the network an the events and still
		 * calculate link travel times.
		 */
		String eventsFilename = getClassInputDirectory() + "link10_events.txt";
		String networkFile = "test/scenarios/equil/network.xml";
		
		Config config = super.loadConfig(null);
		
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).parse(networkFile);
		
		Events events = new Events(); // DO NOT USE EventsBuilderImpl() here, as we do not have a population!
		
		TravelTimeCalculator ttCalc = new TravelTimeCalculator(network, config.travelTimeCalculator());
		events.addHandler(ttCalc);
		
		new MatsimEventsReader(events).readFile(eventsFilename);
		
		LinkImpl link10 = network.getLinks().get(new IdImpl("10"));
		
		assertEquals("wrong link travel time at 06:00.", 110.0, ttCalc.getLinkTravelTime(link10, 6.0 * 3600), EPSILON);
		assertEquals("wrong link travel time at 06:15.", 359.9712023038157, ttCalc.getLinkTravelTime(link10, 6.25 * 3600), EPSILON);
	}
	
	private static class EventsCollector implements BasicEventHandler {
		 
		private List<BasicEvent> eventsList;
		
		public EventsCollector(List<BasicEvent> eventsList) {
			this.eventsList = eventsList;
		}

		public void reset(int iteration) {
		}

		public void handleEvent(BasicEvent event) {
			this.eventsList.add(event);
		}
	}
 

}
