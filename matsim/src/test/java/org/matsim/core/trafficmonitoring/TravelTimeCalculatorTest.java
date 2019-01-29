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
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.junit.Assert;
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
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class TravelTimeCalculatorTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(TravelTimeCalculatorTest.class);

	public final void testTravelTimeCalculator_Array_Optimistic() throws IOException {
		String compareFile;
		MutableScenario scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataArrayFactory(scenario.getNetwork(), numSlots),
				aggregator, binSize, endTime, compareFile, false);
	}

	public final void testTravelTimeCalculator_Array_Optimistic_LinearInterpolation() throws IOException {
		String compareFile;
		MutableScenario scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_linearinterpolation.txt";
		scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		aggregator.connectTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataArrayFactory(scenario.getNetwork(), numSlots),
				aggregator, binSize, endTime, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Optimistic() throws IOException {
		String compareFile;
		MutableScenario scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, endTime, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Optimistic_LinearInterpolation() throws IOException {
		String compareFile;
		MutableScenario scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_linearinterpolation.txt";
		scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		aggregator.connectTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, endTime, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Pessimistic() throws IOException {
		String compareFile;
		MutableScenario scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 12*3600;
		int binSize = 1*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes_pessimistic.txt";
		scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new PessimisticTravelTimeAggregator(binSize, numSlots);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, endTime, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Pessimistic_LinearInterpolation() throws IOException {
		String compareFile;
		MutableScenario scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 12*3600;
		int binSize = 1*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_pessimistic_linearinterpolation.txt";
		scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new PessimisticTravelTimeAggregator(binSize, numSlots);
		aggregator.connectTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, endTime, compareFile, false);
	}

	private final void doTravelTimeCalculatorTest(final MutableScenario scenario, final TravelTimeDataFactory ttDataFactory,
			final AbstractTravelTimeAggregator aggregator, final int timeBinSize, final int endTime,
			final String compareFile, final boolean generateNewData) throws IOException {
		String networkFile = getClassInputDirectory() + "link10_network.xml";
		String eventsFile = getClassInputDirectory() + "link10_events.xml";

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		new MatsimEventsReader(events).readFile(eventsFile);

		EventsManager events2 = EventsUtils.createEventsManager();

		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, endTime, scenario.getConfig().travelTimeCalculator());
		ttcalc.setTravelTimeAggregator(aggregator);
		ttcalc.setTravelTimeDataFactory(ttDataFactory);
		events2.addHandler(ttcalc);
		for (Event e : collector.getEvents()) {
			events2.processEvent(e);
		}

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
			BufferedWriter outfile = null;
			try {
				outfile = new BufferedWriter(new FileWriter(compareFile));
				for (int i = 0; i < numberOfTimeSlotsToTest; i++) {
					double ttime = ttcalc.getLinkTravelTimes().getLinkTravelTime(link10, i*timeBinSize, null, null);
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
		for (int i = 0; i < numberOfTimeSlotsToTest; i++) {
			double ttime = ttcalc.getLinkTravelTimes().getLinkTravelTime(link10, i*timeBinSize, null, null);
			assertEquals(compareData[i], Double.toString(ttime));
		}
	}

	/**
	 * This method tests the functionality of the consolidateData-method in TravelTimeCalculator
	 * 
	 * @author mrieser, tthunig
	 */
	public void testLongTravelTimeInEmptySlot() {
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
		assertEquals(linkTravelTime1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + offset, null, null), EPSILON);
		assertEquals(linkTravelTime1-timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1*timeBinSize + offset, null, null), EPSILON);
		assertEquals(linkTravelTime1-2*timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2*timeBinSize + offset, null, null), EPSILON);
		assertEquals(linkTravelTime2, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 3*timeBinSize + offset, null, null), EPSILON);
		assertEquals(freeSpeedTT, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 4*timeBinSize + offset, null, null), EPSILON);
		assertEquals(freeSpeedTT, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 5*timeBinSize + offset, null, null), EPSILON);
	}
	
	/**
	 * Test linear interpolation of aggregated travel times at different positions of a time bin. (Previous tests only test the midpoint of each time bin.)
	 * 
	 * @author tthunig
	 */
	public void testInterpolatedTravelTimes() {
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
		assertEquals(linkTravelTime1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 0.5*timeBinSize, null, null), EPSILON);
		// travel time should decrease 1 second per time step between first and second time bin
		assertEquals(linkTravelTime1 - 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 0.5*timeBinSize + 1, null, null), EPSILON);
		assertEquals(linkTravelTime1 - 1*timeBinSize + 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1.5*timeBinSize - 1, null, null), EPSILON);
		assertEquals(linkTravelTime1 - 1*timeBinSize, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1.5*timeBinSize, null, null), EPSILON);
		// travel time should increase again by 1/3 seconds per time step between second and third time bin
		assertEquals(linkTravelTime1 - 1*timeBinSize + 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 1.5*timeBinSize + 3, null, null), EPSILON);
		assertEquals(linkTravelTime2 - 1, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2.5*timeBinSize - 3, null, null), EPSILON);
		assertEquals(linkTravelTime2, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, firstTimeBinStart + 2.5*timeBinSize, null, null), EPSILON);
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
		String eventsFilename = getClassInputDirectory() + "link10_events.xml";
		String networkFile = "test/scenarios/equil/network.xml";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		EventsManager events = EventsUtils.createEventsManager(); // DO NOT USE EventsBuilderImpl() here, as we do not have a population!

		TravelTimeCalculator ttCalc = new TravelTimeCalculator(network, config.travelTimeCalculator());
		events.addHandler(ttCalc);
				
		new MatsimEventsReader(events).readFile(eventsFilename);
		
		Link link10 = network.getLinks().get(Id.create("10", Link.class));

		assertEquals("wrong link travel time at 06:00.", 110.0, ttCalc.getLinkTravelTimes().getLinkTravelTime(link10, 6.0 * 3600, null, null), EPSILON);
		assertEquals("wrong link travel time at 06:15.", 359.9712023038157, ttCalc.getLinkTravelTimes().getLinkTravelTime(link10, 6.25 * 3600, null, null), EPSILON);
	}

	/**
	 * @author mrieser / senozon
	 */
	public void testGetLinkTravelTime_ignorePtVehiclesAtStop() {
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

		Assert.assertEquals("The time of transit vehicles at stop should not be counted", 100.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);
	}

	/**
	 * @author mrieser / senozon
	 */
	public void testGetLinkTravelTime_usePtVehiclesWithoutStop() {
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

		Assert.assertEquals("The time of transit vehicles at stop should not be counted", 125.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);

	}

	/**
	 * Enable filtering but set an empty string as modes to analyze.
	 * Expect that all link travel times are ignored.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_NoAnalyzedModes() {
		Network network = NetworkUtils.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("");
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

		Assert.assertEquals("No transport mode has been registered to be analyzed, therefore no vehicle/agent should be counted", 1000.0, ttc.getLinkTravelTimes().getLinkTravelTime(link2, 300, null, null), 1e-8);
		// 1000.0s is the freespeed travel time (euclidean link length: 1000m, default freespeed: 1m/s)
	}
	
	/**
	 * Enable filtering and analyze only car legs.
	 * Expect that walk legs are ignored.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_CarAnalyzedModes() {
		Network network = NetworkUtils.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes(TransportMode.car);
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

		Assert.assertEquals("Only transport mode has been registered to be analyzed, therefore no walk agent should be counted", 100.0, ttc.getLinkTravelTimes().getLinkTravelTime(link2, 200, null, null), 1e-8);
	}
	
	/**
	 * Disable filtering but also set analyzed modes to an empty string.
	 * Expect that still all modes are counted.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_NoFilterModes() {
		Network network = NetworkUtils.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("");
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

		Assert.assertEquals("Filtering analyzed transport modes is disabled, therefore count all modes", 200.0, ttc.getLinkTravelTimes().getLinkTravelTime(link2, 200, null, null), 1e-8);
	}
	
	/**
	 * Enable filtering but do not set transport modes which should be counted.
	 * Expect that the default value (=car) will be used for the modes to be counted.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_FilterDefaultModes() {
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

		Assert.assertEquals("Filtering analyzed transport modes is enabled, but no modes set. Therefore, use default (=car)", 100.0, 
				ttc.getLinkTravelTimes().getLinkTravelTime(link2, 200, null, null), 1e-8);
	}
}
