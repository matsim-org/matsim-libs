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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class TravelTimeCalculatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private final static Logger log = LogManager.getLogger(TravelTimeCalculatorTest.class);

	@Test
	final void testTravelTimeCalculator_Array_Optimistic() throws IOException {

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		String compareFile = utils.getClassInputDirectory() + "link10_ttimes.txt";
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		TimeSlotComputation travelTimeAggregator = new TimeSlotComputation( numSlots, binSize );
		TravelTimeGetter travelTimeGetter = new AveragingTravelTimeGetter( travelTimeAggregator ) ;
		doTravelTimeCalculatorTest(scenario,
				travelTimeAggregator, binSize, endTime, compareFile, false, utils.getClassInputDirectory(), travelTimeGetter );
	}

	@Test
	final void testTravelTimeCalculator_Array_Optimistic_LinearInterpolation() throws IOException {

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		String compareFile = utils.getClassInputDirectory() + "link10_ttimes_linearinterpolation.txt";
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		TimeSlotComputation aggregator = new TimeSlotComputation( numSlots, binSize );
		TravelTimeGetter travelTimeGetter = new LinearInterpolatingTravelTimeGetter( numSlots, binSize, aggregator );
		doTravelTimeCalculatorTest(scenario,
				aggregator, binSize, endTime, compareFile, false, utils.getClassInputDirectory(), travelTimeGetter );
	}

	private static void doTravelTimeCalculatorTest( final MutableScenario scenario,
									final TimeSlotComputation aggregator, final int timeBinSize, final int endTime,
									final String compareFile, final boolean generateNewData, String inputDirectory,
									TravelTimeGetter travelTimeGetter ) throws IOException {
		String networkFile = inputDirectory + "link10_network.xml";
		String eventsFile = inputDirectory + "link10_events.xml";

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		events.initProcessing();
		new MatsimEventsReader(events).readFile(eventsFile);
		events.finishProcessing();

		EventsManager events2 = EventsUtils.createEventsManager();

		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, endTime, scenario.getConfig().travelTimeCalculator());
		ttcalc.travelTimeGetter = travelTimeGetter;
		ttcalc.aggregator = aggregator;
		events2.addHandler(ttcalc);
		events2.initProcessing();
		for (Event e : collector.getEvents()) {
			events2.processEvent(e);
		}
		events2.finishProcessing();

		final int numberOfTimeSlotsToTest = 4*24;

		// read comparison data
		BufferedReader infile = IOUtils.getBufferedReader(compareFile);
		String line;
		String[] compareData = new String[numberOfTimeSlotsToTest];
		try {
			for (int i = 0; i < numberOfTimeSlotsToTest; i++) {
				line = infile.readLine();
				compareData[i] = line;
			}
		}
		finally {
			try {
				infile.close();
			} catch (IOException e) {
				log.error("could not close stream.", e);
			}
		}

		// prepare comparison
		Link link10 = network.getLinks().get(Id.create("10", Link.class));

		if (generateNewData) {
			try (BufferedWriter outfile = new BufferedWriter(new FileWriter(compareFile))) {
				for (int i = 0; i < numberOfTimeSlotsToTest; i++) {
					double ttime = ttcalc.getLinkTravelTimes().getLinkTravelTime(link10, i*timeBinSize, null, null);
					outfile.write(ttime + "\n");
				}
			}
			fail("A new file containg data for comparison was created. No comparison was made.");
		}

		// do comparison
		for (int i = 0; i < numberOfTimeSlotsToTest; i++) {
			double ttime = ttcalc.getLinkTravelTimes().getLinkTravelTime(link10, i*timeBinSize, null, null);
			assertEquals(Double.parseDouble(compareData[i]), ttime, 1e-3); // traveltimecalculator has a resolution of 0.001 seconds
		}
	}

	/**
	 * This method tests the functionality of the consolidateData-method in TravelTimeCalculator
	 *
	 * @author mrieser, tthunig
	 */
	@Test
	void testLongTravelTimeInEmptySlot() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		final Node fromNode = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		final Node toNode = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1000, 0));
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1000.0, 100.0, 3600.0, 1.0 );
		double freeSpeedTT = NetworkUtils.getFreespeedTravelTime(link1);
		Id<Vehicle> vehId = Id.create("1", Vehicle.class);

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());
		double firstTimeBinStart = 7.0 * 3600;

		// generate some events that suggest a really long travel time
		double linkTravelTime1 = 50.0 * 60; // 50minutes in first time bin
		double linkTravelTime2 = 10.0 * 60; // 10minutes in forth time bin
		ttcalc.handleEvent(new LinkEnterEvent(firstTimeBinStart, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(firstTimeBinStart + linkTravelTime1, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(firstTimeBinStart + 3*timeBinSize, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(firstTimeBinStart + 3*timeBinSize + linkTravelTime2, vehId, link1.getId()));

		double offset = 5*60; // offset of 5 minutes to test another time in the same time bin
		assertEquals(linkTravelTime1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime1-timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime1-2*timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime2, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 3*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(freeSpeedTT, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 4*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(freeSpeedTT, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 5*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
	}

	/**
	 * This method tests the functionality of the consolidateData-method in TravelTimeCalculator
	 * in combination with double time bins
	 *
	 * @author tthunig
	 */
	@Test
	void testLongTravelTimeInEmptySlotWithDoubleTimeBins() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		final Node fromNode = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		final Node toNode = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1000, 0));
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1000.0, 100.0, 3600.0, 1.0 );
		double freeSpeedTT = NetworkUtils.getFreespeedTravelTime(link1);
		Id<Vehicle> vehId = Id.create("1", Vehicle.class);

		double timeBinSize = 0.5; // time bin of 0.5 seconds
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());
		double firstTimeBinStart = 7.0 * 3600;

		// generate some events that suggest a really long travel time
		double linkTravelTime1 = 11.5; // 11.5 seconds in first time bin
		double linkTravelTime2 = 10.2; // 10.2 seconds in forth time bin
		ttcalc.handleEvent(new LinkEnterEvent(firstTimeBinStart, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(firstTimeBinStart + linkTravelTime1, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(firstTimeBinStart + 3*timeBinSize, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(firstTimeBinStart + 3*timeBinSize + linkTravelTime2, vehId, link1.getId()));

		double offset = 0.2; // offset of 0.2 seconds to test another time in the same time bin
//		System.out.println("firstTimeBinStart - offset (" + (firstTimeBinStart - offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart - offset, null, null));
//		System.out.println("firstTimeBinStart + offset (" + (firstTimeBinStart + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + offset, null, null));
//		System.out.println("firstTimeBinStart + 1xtimeBinSize + offset (" + (firstTimeBinStart + 1*timeBinSize + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1*timeBinSize + offset, null, null));
//		System.out.println("firstTimeBinStart + 2xtimeBinSize + offset (" + (firstTimeBinStart + 2*timeBinSize + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2*timeBinSize + offset, null, null));
//		System.out.println("firstTimeBinStart + 3xtimeBinSize + offset (" + (firstTimeBinStart + 3*timeBinSize + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 3*timeBinSize + offset, null, null));
//		System.out.println("firstTimeBinStart + 4xtimeBinSize + offset (" + (firstTimeBinStart + 4*timeBinSize + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 4*timeBinSize + offset, null, null));
//		System.out.println("firstTimeBinStart + 5xtimeBinSize + offset (" + (firstTimeBinStart + 5*timeBinSize + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 5*timeBinSize + offset, null, null));
//		System.out.println("firstTimeBinStart + linkTravelTime1 + offset (" + (firstTimeBinStart + linkTravelTime1 + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + linkTravelTime1 + offset, null, null));
//		System.out.println("firstTimeBinStart + 3xtimeBinSize + linkTravelTime2 + offset (" + (firstTimeBinStart + 3*timeBinSize + linkTravelTime2 + offset) + "): " + ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 3*timeBinSize + linkTravelTime2 + offset, null, null));
		assertEquals(linkTravelTime1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime1-timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime1-2*timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime2, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 3*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(freeSpeedTT, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 4*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);
		assertEquals(freeSpeedTT, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 5*timeBinSize + offset, null, null), MatsimTestUtils.EPSILON);

		/*
		 * This test results in strange rounding errors when 10.1 or 10.3 are used as linkTravelTime2.
		 * The measured travel time in the fourth time bin is then 10.099 or 10.299 respectively.
		 * For a linkTravelTime2 of 10.2 it works fine without rounding errors.
		 * I assume this results from some double vs. float issue in the way travel times are calculated within MATSim, but I can't find it.
		 * KN suggested to still commit it with 10.2 and write this comment... :-)
		 * Theresa, Jul'23
		 */
	}

	/**
	 * Test linear interpolation of aggregated travel times at different positions of a time bin. (Previous tests only test the midpoint of each time bin.)
	 *
	 * @author tthunig
	 */
	@Test
	void testInterpolatedTravelTimes() {
		Config config = ConfigUtils.createConfig();
		config.travelTimeCalculator().setTravelTimeGetterType("linearinterpolation");
		int timeBinSize = 15*60;
		config.travelTimeCalculator().setTraveltimeBinSize(timeBinSize);
		config.travelTimeCalculator().setMaxTime(12*3600);

		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		final Node fromNode = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		final Node toNode = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1000, 0));
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1000.0, 100.0, 3600.0, 1.0 );
		Id<Vehicle> vehId = Id.create("1", Vehicle.class);

		TravelTimeCalculator ttcalc = TravelTimeCalculator.create(network, config.travelTimeCalculator());
		double firstTimeBinStart = 7.0 * 3600;

		// generate some events that create different travel times in different time bins
		double linkTravelTime1 = 50.0 * 60; // 50minutes in first time bin
		double linkTravelTime2 = 40.0 * 60; // 40minutes in third time bin
		ttcalc.handleEvent(new LinkEnterEvent(firstTimeBinStart, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(firstTimeBinStart + linkTravelTime1, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(firstTimeBinStart + 2*timeBinSize, vehId, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(firstTimeBinStart + 2*timeBinSize + linkTravelTime2, vehId, link1.getId()));

		// travel time should be the measured aggregated travel time at the midpoint of each interval
		assertEquals(linkTravelTime1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 0.5*timeBinSize, null, null), MatsimTestUtils.EPSILON);
		// travel time should decrease 1 second per time step between first and second time bin
		assertEquals(linkTravelTime1 - 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 0.5*timeBinSize + 1, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime1 - 1*timeBinSize + 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1.5*timeBinSize - 1, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime1 - 1*timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1.5*timeBinSize, null, null), MatsimTestUtils.EPSILON);
		// travel time should increase again by 1/3 seconds per time step between second and third time bin
		assertEquals(linkTravelTime1 - 1*timeBinSize + 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1.5*timeBinSize + 3, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime2 - 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2.5*timeBinSize - 3, null, null), MatsimTestUtils.EPSILON);
		assertEquals(linkTravelTime2, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2.5*timeBinSize, null, null), MatsimTestUtils.EPSILON);
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
	@Test
	void testReadFromFile_LargeScenarioCase() throws SAXException, ParserConfigurationException, IOException {
		/* Assume, you have a big events file from a huge scenario and you want to do data-mining...
		 * Then you likely want to calculate link travel times. This requires the network, but NOT
		 * the population. Thus, using "new Events(new EventsBuilderImpl(scenario))" is not appropriate
		 * and may not even be usable if the population is too big to read it in on a laptop for
		 * post-processing. So, it must be possible to only read the network an the events and still
		 * calculate link travel times.
		 */
		String eventsFilename = utils.getClassInputDirectory() + "link10_events.xml";
		String networkFile = "test/scenarios/equil/network.xml";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		EventsManager events = EventsUtils.createEventsManager(); // DO NOT USE EventsBuilderImpl() here, as we do not have a population!

		TravelTimeCalculator ttCalc = new TravelTimeCalculator(network, config.travelTimeCalculator());
		events.addHandler(ttCalc);
		events.initProcessing();
		new MatsimEventsReader(events).readFile(eventsFilename);
		events.finishProcessing();

		Link link10 = network.getLinks().get(Id.create("10", Link.class));

		assertEquals(110.0, ttCalc.getLinkTravelTimes().getLinkTravelTime(link10, 6.0 * 3600, null, null), MatsimTestUtils.EPSILON, "wrong link travel time at 06:00.");
		assertEquals(359.9712023038157, ttCalc.getLinkTravelTimes().getLinkTravelTime(link10, 6.25 * 3600, null, null), 1e-3, "wrong link travel time at 06:15."); // traveltimecalculator has a resolution of 0.001 seconds
	}

	/**
	 * @author mrieser / senozon
	 */
	@Test
	void testGetLinkTravelTime_ignorePtVehiclesAtStop() {
		Network network = NetworkUtils.createNetwork();
        TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Vehicle> ptVehId = Id.create("ptVeh", Vehicle.class);
		Id<Vehicle> ivVehId = Id.create("ivVeh", Vehicle.class);

		ttc.handleEvent(new LinkEnterEvent(100, ivVehId, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(150, ptVehId, link1.getId()));
		ttc.handleEvent(new LinkLeaveEvent(200, ivVehId, link1.getId()));
		ttc.handleEvent(new VehicleArrivesAtFacilityEvent(240, ptVehId, Id.create("stop", TransitStopFacility.class), 0));
		ttc.handleEvent(new LinkLeaveEvent(350, ptVehId, link1.getId()));

		Assertions.assertEquals(100.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8, "The time of transit vehicles at stop should not be counted");
	}

	/**
	 * @author mrieser / senozon
	 */
	@Test
	void testGetLinkTravelTime_usePtVehiclesWithoutStop() {
        Network network = NetworkUtils.createNetwork();
        TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Vehicle> ptVehId = Id.create("ptVeh", Vehicle.class);
		Id<Vehicle> ivVehId = Id.create("ivVeh", Vehicle.class);

		ttc.handleEvent(new LinkEnterEvent(100, ivVehId, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(150, ptVehId, link1.getId()));
		ttc.handleEvent(new LinkLeaveEvent(200, ivVehId, link1.getId()));
		ttc.handleEvent(new LinkLeaveEvent(300, ptVehId, link1.getId()));

		Assertions.assertEquals(125.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8, "The time of transit vehicles at stop should not be counted");

	}

	/**
	 * Enable filtering but set an empty string as modes to analyze.
	 * Expect that all link travel times are ignored.
	 * @author cdobler
	 */
	@Test
	void testGetLinkTravelTime_NoAnalyzedModes() {
        Network network = NetworkUtils.createNetwork();
        TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModesAsString("" );
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 0));
		Node n3 = network.getFactory().createNode(Id.create(3, Node.class), new Coord(2000, 0));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		Link link2 = network.getFactory().createLink(Id.create(2, Link.class), n2, n3);
		network.addLink(link1);
		network.addLink(link2);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Vehicle> vehId = Id.create(1980, Vehicle.class);

		ttc.handleEvent(new VehicleEntersTrafficEvent(100, agId1, link1.getId(), vehId, TransportMode.car, 1.0));
		ttc.handleEvent(new LinkLeaveEvent(200, vehId, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(200, vehId, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(300, vehId, link2.getId()));

		Assertions.assertEquals(1000.0, ttc.getLinkTravelTimes().getLinkTravelTime(link2, 300, null, null), 1e-8, "No transport mode has been registered to be analyzed, therefore no vehicle/agent should be counted");
		// 1000.0s is the freespeed travel time (euclidean link length: 1000m, default freespeed: 1m/s)
	}

	/**
	 * Enable filtering and analyze only car legs.
	 * Expect that walk legs are ignored.
	 * @author cdobler
	 */
	@Test
	void testGetLinkTravelTime_CarAnalyzedModes() {
        Network network = NetworkUtils.createNetwork();
        TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModesAsString(TransportMode.car );
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 0));
		Node n3 = network.getFactory().createNode(Id.create(3, Node.class), new Coord(2000, 0));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		Link link2 = network.getFactory().createLink(Id.create(2, Link.class), n2, n3);
		network.addLink(link1);
		network.addLink(link2);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create(1511, Person.class);
		Id<Vehicle> vehId1 = Id.create(1980, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(1981, Vehicle.class);

		ttc.handleEvent(new VehicleEntersTrafficEvent(90, agId1, link1.getId(), vehId1, TransportMode.car, 1.0));
		ttc.handleEvent(new VehicleEntersTrafficEvent(100, agId2, link1.getId(), vehId2, TransportMode.walk, 1.0));
		ttc.handleEvent(new LinkLeaveEvent(100, vehId1, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(100, vehId1, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(110, vehId2, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(110, vehId2, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(200, vehId1, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(410, vehId2, link2.getId()));

		Assertions.assertEquals(100.0, ttc.getLinkTravelTimes().getLinkTravelTime(link2, 200, null, null), 1e-8, "Only transport mode has been registered to be analyzed, therefore no walk agent should be counted");
	}

	/**
	 * Disable filtering but also set analyzed modes to an empty string.
	 * Expect that still all modes are counted.
	 * @author cdobler
	 */
	@Test
	void testGetLinkTravelTime_NoFilterModes() {
        Network network = NetworkUtils.createNetwork();
        TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModesAsString("" );
		config.setFilterModes(false);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 0));
		Node n3 = network.getFactory().createNode(Id.create(3, Node.class), new Coord(2000, 0));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		Link link2 = network.getFactory().createLink(Id.create(2, Link.class), n2, n3);
		network.addLink(link1);
		network.addLink(link2);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create(1511, Person.class);
		Id<Vehicle> vehId1 = Id.create(1980, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(1981, Vehicle.class);

		ttc.handleEvent(new VehicleEntersTrafficEvent(90, agId1, link1.getId(), vehId1, TransportMode.car, 1.0));
		ttc.handleEvent(new VehicleEntersTrafficEvent(100, agId2, link1.getId(), vehId2, TransportMode.walk, 1.0));
		ttc.handleEvent(new LinkLeaveEvent(100, vehId1, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(100, vehId1, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(110, vehId2, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(110, vehId2, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(200, vehId1, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(410, vehId2, link2.getId()));

		Assertions.assertEquals(200.0, ttc.getLinkTravelTimes().getLinkTravelTime(link2, 200, null, null), 1e-8, "Filtering analyzed transport modes is disabled, therefore count all modes");
	}

	/**
	 * Enable filtering but do not set transport modes which should be counted.
	 * Expect that the default value (=car) will be used for the modes to be counted.
	 * @author cdobler
	 */
	@Test
	void testGetLinkTravelTime_FilterDefaultModes() {
        Network network = NetworkUtils.createNetwork();
        TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(1000, 0));
		Node n3 = network.getFactory().createNode(Id.create(3, Node.class), new Coord(2000, 0));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		Link link2 = network.getFactory().createLink(Id.create(2, Link.class), n2, n3);
		network.addLink(link1);
		network.addLink(link2);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create(1511, Person.class);
		Id<Vehicle> vehId1 = Id.create(1980, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(1981, Vehicle.class);

		ttc.handleEvent(new VehicleEntersTrafficEvent(90, agId1, link1.getId(), vehId1, TransportMode.car, 1.0));
		ttc.handleEvent(new VehicleEntersTrafficEvent(100, agId2, link1.getId(), vehId2, TransportMode.walk, 1.0));
		ttc.handleEvent(new LinkLeaveEvent(100, vehId1, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(100, vehId1, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(110, vehId2, link1.getId()));
		ttc.handleEvent(new LinkEnterEvent(110, vehId2, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(200, vehId1, link2.getId()));
		ttc.handleEvent(new LinkLeaveEvent(410, vehId2, link2.getId()));

		Assertions.assertEquals(100.0,
				ttc.getLinkTravelTimes().getLinkTravelTime(link2, 200, null, null), 1e-8, "Filtering analyzed transport modes is enabled, but no modes set. Therefore, use default (=car)");
	}
}
