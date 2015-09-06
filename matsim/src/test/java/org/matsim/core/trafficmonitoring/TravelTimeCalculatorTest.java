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

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
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
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
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
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataArrayFactory(scenario.getNetwork(), numSlots),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_Array_Optimistic_LinearInterpolation() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_linearinterpolation.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		aggregator.connectTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataArrayFactory(scenario.getNetwork(), numSlots),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Optimistic() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Optimistic_LinearInterpolation() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 30*3600;
		int binSize = 15*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_linearinterpolation.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new OptimisticTravelTimeAggregator(numSlots, binSize);
		aggregator.connectTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Pessimistic() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 12*3600;
		int binSize = 1*60;
		int numSlots = (endTime / binSize) + 1;

		// by default: averaging travel times
		compareFile = getClassInputDirectory() + "link10_ttimes_pessimistic.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new PessimisticTravelTimeAggregator(binSize, numSlots);
		assertEquals(AveragingTravelTimeGetter.class, aggregator.getTravelTimeGetter().getClass());
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	public final void testTravelTimeCalculator_HashMap_Pessimistic_LinearInterpolation() throws IOException {
		String compareFile;
		ScenarioImpl scenario;
		AbstractTravelTimeAggregator aggregator;

		int endTime = 12*3600;
		int binSize = 1*60;
		int numSlots = (endTime / binSize) + 1;

		// use linear interpolation
		compareFile = getClassInputDirectory() + "link10_ttimes_pessimistic_linearinterpolation.txt";
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		aggregator = new PessimisticTravelTimeAggregator(binSize, numSlots);
		aggregator.connectTravelTimeGetter(new LinearInterpolatingTravelTimeGetter(numSlots, binSize));
		doTravelTimeCalculatorTest(scenario, new TravelTimeDataHashMapFactory(scenario.getNetwork()),
				aggregator, binSize, compareFile, false);
	}

	private final void doTravelTimeCalculatorTest(final ScenarioImpl scenario, final TravelTimeDataFactory ttDataFactory,
			final AbstractTravelTimeAggregator aggregator, final int timeBinSize,
			final String compareFile, final boolean generateNewData) throws IOException {
		String networkFile = getClassInputDirectory() + "link10_network.xml";
		String eventsFile = getClassInputDirectory() + "link10_events.txt";

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);

		EventsManagerImpl events = (EventsManagerImpl) EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		new MatsimEventsReader(events).readFile(eventsFile);

		EventsManager events2 = EventsUtils.createEventsManager();

		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 30*3600, scenario.getConfig().travelTimeCalculator());
		ttcalc.setTravelTimeAggregator(aggregator);
		ttcalc.setTravelTimeDataFactory(ttDataFactory);
		events2.addHandler(ttcalc);
		for (Event e : collector.getEvents()) {
			events2.processEvent(e);
		}

		// read comparison data
		BufferedReader infile = IOUtils.getBufferedReader(compareFile);
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
				for (int i = 0; i < 4*24; i++) {
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
		for (int i = 0; i < 4*24; i++) {
			double ttime = ttcalc.getLinkTravelTimes().getLinkTravelTime(link10, i*timeBinSize, null, null);
			assertEquals(compareData[i], Double.toString(ttime));
		}
	}

	/**
	 * @author mrieser
	 */
	public void testLongTravelTimeInEmptySlot() {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Link link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000.0, 100.0, 3600.0, 1.0);

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		PersonImpl person = new PersonImpl(Id.create("1", Person.class));

		// generate some events that suggest a really long travel time
		double linkEnterTime1 = 7.0 * 3600 + 10;
		double linkTravelTime1 = 50.0 * 60; // 50minutes!
		double linkEnterTime2 = 7.75 * 3600 + 10;
		double linkTravelTime2 = 10.0 * 60; // 10minutes!

		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1, person.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime1 + linkTravelTime1, person.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, person.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime2, person.getId(), link1.getId(), null));

		assertEquals(50 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60, person, null), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 1*timeBinSize, person, null), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 2*timeBinSize, person, null), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(10 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 3*timeBinSize, person, null), EPSILON);  // linkTravelTime2 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 4*timeBinSize, person, null), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 5*timeBinSize, person, null), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize
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

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).parse(networkFile);

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
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create("pt2011", Person.class);
		Id<Vehicle> vehId = Id.create(1980, Vehicle.class);

		ttc.handleEvent(new LinkEnterEvent(100, agId1, link1.getId(), null));
		ttc.handleEvent(new TransitDriverStartsEvent(140, agId2, vehId, Id.create("line1", TransitLine.class), Id.create("route1", TransitRoute.class), Id.create("dep1", Departure.class)));
		ttc.handleEvent(new LinkEnterEvent(150, agId2, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(200, agId1, link1.getId(), null));
		ttc.handleEvent(new VehicleArrivesAtFacilityEvent(240, vehId, Id.create("stop", TransitStopFacility.class), 0));
		ttc.handleEvent(new LinkLeaveEvent(350, agId2, link1.getId(), null));

		Assert.assertEquals("The time of transit vehicles at stop should not be counted", 100.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);
	}

	/**
	 * @author mrieser / senozon
	 */
	public void testGetLinkTravelTime_usePtVehiclesWithoutStop() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create("pt2011", Person.class);
		Id<Vehicle> vehId = Id.create(1980, Vehicle.class);

		ttc.handleEvent(new LinkEnterEvent(100, agId1, link1.getId(), null));
		ttc.handleEvent(new TransitDriverStartsEvent(140, agId2, vehId, Id.create("line1", TransitLine.class), Id.create("route1", TransitRoute.class), Id.create("dep1", Departure.class)));
		ttc.handleEvent(new LinkEnterEvent(150, agId2, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(200, agId1, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(300, agId2, link1.getId(), null));

		Assert.assertEquals("The time of transit vehicles at stop should not be counted", 125.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);

	}

	/**
	 * Enable filtering but set an empty string as modes to analyze.
	 * Expect that all link travel times are ignored.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_NoAnalyzedModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("");
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Person> agId1 = Id.create(1510, Person.class);
		
		ttc.handleEvent(new PersonDepartureEvent(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEvent(100, agId1, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(200, agId1, link1.getId(), null));

		Assert.assertEquals("No transport mode has been registered to be analyzed, therefore no vehicle/agent should be counted", 1.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);
	}
	
	/**
	 * Enable filtering and analyze only car legs.
	 * Expect that walk legs are ignored.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_CarAnalyzedModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("car");
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create(1511, Person.class);
		
		ttc.handleEvent(new PersonDepartureEvent(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEvent(100, agId1, link1.getId(), null));
		ttc.handleEvent(new PersonDepartureEvent(110, agId2, link1.getId(), TransportMode.walk));
		ttc.handleEvent(new LinkEnterEvent(110, agId2, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(200, agId1, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(410, agId2, link1.getId(), null));

		Assert.assertEquals("Only transport mode has been registered to be analyzed, therefore no walk agent should be counted", 100.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);
	}
	
	/**
	 * Disable filtering but also set analyzed modes to an empty string.
	 * Expect that still all modes are counted.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_NoFilterModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setAnalyzedModes("");
		config.setFilterModes(false);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create(1511, Person.class);
		
		ttc.handleEvent(new PersonDepartureEvent(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEvent(100, agId1, link1.getId(), null));
		ttc.handleEvent(new PersonDepartureEvent(110, agId2, link1.getId(), TransportMode.walk));
		ttc.handleEvent(new LinkEnterEvent(110, agId2, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(200, agId1, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(410, agId2, link1.getId(), null));

		Assert.assertEquals("Filtering analyzed transport modes is disabled, therefore count all modes", 200.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);
	}
	
	/**
	 * Enable filtering but do not set transport modes which should be counted.
	 * Expect that the default value (=car) will be used for the modes to be counted.
	 * @author cdobler
	 */
	public void testGetLinkTravelTime_FilterDefaultModes() {
		Network network = NetworkImpl.createNetwork();
		TravelTimeCalculatorConfigGroup config = new TravelTimeCalculatorConfigGroup();
		config.setTraveltimeBinSize(900);
		config.setFilterModes(true);
		TravelTimeCalculator ttc = new TravelTimeCalculator(network, config);

		Node n1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node n2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		network.addNode(n1);
		network.addNode(n2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), n1, n2);
		network.addLink(link1);

		Id<Person> agId1 = Id.create(1510, Person.class);
		Id<Person> agId2 = Id.create(1511, Person.class);
		
		ttc.handleEvent(new PersonDepartureEvent(100, agId1, link1.getId(), TransportMode.car));
		ttc.handleEvent(new LinkEnterEvent(100, agId1, link1.getId(), null));
		ttc.handleEvent(new PersonDepartureEvent(110, agId2, link1.getId(), TransportMode.walk));
		ttc.handleEvent(new LinkEnterEvent(110, agId2, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(200, agId1, link1.getId(), null));
		ttc.handleEvent(new LinkLeaveEvent(410, agId2, link1.getId(), null));

		Assert.assertEquals("Filtering analyzed transport modes is enabled, but no modes set. Therefore, use default (=car)", 100.0, ttc.getLinkTravelTimes().getLinkTravelTime(link1, 200, null, null), 1e-8);
	}
	
	/**
	 * Tests the example in the comment to TravelTimeCalculator.consolidateData()
	 * @author dgrether
	 */
	@Ignore
	public void estLongTravelTimeInEmptySlotShortBinsSimultaneousLinkEnter() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = scenario.getNetwork();
		((NetworkImpl) network).setCapacityPeriod(3600.0);
		Node node1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		network.addNode(node1);
		network.addNode(node2);
		Link link1 = network.getFactory().createLink(Id.create(1, Link.class), node1, node2);
		link1.setCapacity(6.0);
		link1.setFreespeed(5.56);
		link1.setLength(1000.0);
		link1.setNumberOfLanes(1.0);
		network.addLink(link1);
		
		int timeBinSize = 5*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		PersonImpl person1 = new PersonImpl(Id.create(1, Person.class));
		PersonImpl person2 = new PersonImpl(Id.create(2, Person.class));
		PersonImpl person3 = new PersonImpl(Id.create(3, Person.class));

		// generate some events that suggest a really long travel time
		double linkEnterTime1 = 7.0 * 3600;
		double linkTravelTime1 = 3.0 * 60; // 3minutes!
		double linkEnterTime2 = 7.0 * 3600;
		double linkTravelTime2 = 10.0 * 60; // 10minutes!
		double linkEnterTime3 = 7.0 * 3600 + 6.0 * 60;
		double linkTravelTime3 = 14.0 * 60;

		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1, person1.getId(), link1.getId(), Id.create(person1.getId().toString(), Vehicle.class)));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime1 + linkTravelTime1, person1.getId(), link1.getId(), Id.create(person1.getId().toString(), Vehicle.class)));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, person2.getId(), link1.getId(), Id.create(person2.getId().toString(), Vehicle.class)));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime2, person2.getId(), link1.getId(), Id.create(person2.getId().toString(), Vehicle.class)));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime3, person3.getId(), link1.getId(), Id.create(person3.getId().toString(), Vehicle.class)));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime3 + linkTravelTime3, person3.getId(), link1.getId(), Id.create(person3.getId().toString(), Vehicle.class)));

		
		assertEquals(6.5 * 60, ttcalc.getLinkTravelTime(link1.getId(), linkEnterTime1));
		assertEquals(14.0 * 60, ttcalc.getLinkTravelTime(link1.getId(), linkEnterTime3));
		assertEquals(19.0 * 60, ttcalc.getLinkTravelTime(link1.getId(), 7.0 * 3600.0 + 11.0 * 60));
	}
}
