/* *****e****************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser
 */
public class CalcLinkStatsTest {

	@RegisterExtension private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testAddData() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = s.getNetwork();
		NetworkFactory nf = network.getFactory();

		Node node1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = nf.createLink(Id.create("101", Link.class), node1, node2);
		Link link2 = nf.createLink(Id.create("102", Link.class), node2, node3);
		network.addLink(link1);
		network.addLink(link2);

		VolumesAnalyzer analyzer = new VolumesAnalyzer(3600, 86400, network);
		TravelTime ttimes = new FreeSpeedTravelTime();
		CalcLinkStats cls = new CalcLinkStats(network);

		Id<Vehicle> vehId = Id.create("1001", Vehicle.class);
		// generate some pseudo traffic for hour 0: 3 veh on link 1; 1 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1010, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1020, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1030, vehId, link2.getId()));

		// generate some pseudo traffic for hour 0: 1 veh on link 1; 4 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4010, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4020, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4030, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4040, vehId, link2.getId()));

		cls.addData(analyzer, ttimes);

		Assertions.assertEquals(3.0, cls.getAvgLinkVolumes(link1.getId())[0], 1e-8);
		Assertions.assertEquals(1.0, cls.getAvgLinkVolumes(link2.getId())[0], 1e-8);
		Assertions.assertEquals(1.0, cls.getAvgLinkVolumes(link1.getId())[1], 1e-8);
		Assertions.assertEquals(4.0, cls.getAvgLinkVolumes(link2.getId())[1], 1e-8);

		analyzer.reset(1);
		// generate some pseudo traffic for hour 0: 4 veh on link 1; 3 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1010, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1020, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1030, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1040, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1050, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1060, vehId, link2.getId()));

		// generate some pseudo traffic for hour 0: 4 veh on link 1; 2 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4010, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4020, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4030, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4040, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4040, vehId, link2.getId()));

		cls.addData(analyzer, ttimes);

		Assertions.assertEquals(3.5, cls.getAvgLinkVolumes(link1.getId())[0], 1e-8);
		Assertions.assertEquals(2.0, cls.getAvgLinkVolumes(link2.getId())[0], 1e-8);
		Assertions.assertEquals(2.5, cls.getAvgLinkVolumes(link1.getId())[1], 1e-8);
		Assertions.assertEquals(3.0, cls.getAvgLinkVolumes(link2.getId())[1], 1e-8);

		cls.reset();

		Assertions.assertEquals(0, cls.getAvgLinkVolumes(link1.getId()).length);
		Assertions.assertEquals(0, cls.getAvgLinkVolumes(link2.getId()).length);
	}

	/**
	 * Tests the travel times that are written out by {@link CalcLinkStats}.
	 *
	 * Currently, the travel times are only correct if the time bin size in {@link TravelTimeCalculatorConfigGroup} is set to 3600.
	 * If the time bin size is set to the default (900, see TODO below), {@link CalcLinkStats} wrongly assumes hourly time bins resulting in wrong travel times.
	 *
	 * @author ikaddoura
	 */
	@Test
	void testAddDataObservedTravelTime() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = s.getNetwork();
		NetworkFactory nf = network.getFactory();

		Node node1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = nf.createLink(Id.create("101", Link.class), node1, node2);
		Link link2 = nf.createLink(Id.create("102", Link.class), node2, node3);
		network.addLink(link1);
		network.addLink(link2);

		VolumesAnalyzer analyzer = new VolumesAnalyzer(3600, 86400, network);
		TravelTimeCalculatorConfigGroup ttcalcConfig = new TravelTimeCalculatorConfigGroup();

		ttcalcConfig.setTraveltimeBinSize(3600);
//		ttcalcConfig.setTraveltimeBinSize(900); // TODO

		TravelTimeCalculator ttimeCalculator = new TravelTimeCalculator(network, ttcalcConfig);
		CalcLinkStats cls = new CalcLinkStats(network);

		Id<Vehicle> vehId1 = Id.create("1001", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("1002", Vehicle.class);
		Id<Vehicle> vehId3 = Id.create("1003", Vehicle.class);
		Id<Vehicle> vehId4 = Id.create("1004", Vehicle.class);

		// generate some pseudo traffic for hour 0: 3 veh on link 1; 1 veh on link 2
		ttimeCalculator.handleEvent(new LinkEnterEvent(500,  vehId1, link1.getId()));
		ttimeCalculator.handleEvent(new LinkEnterEvent(500,  vehId2, link1.getId()));
		ttimeCalculator.handleEvent(new LinkEnterEvent(500,  vehId3, link1.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(1000, vehId1, link1.getId()));
		ttimeCalculator.handleEvent(new LinkEnterEvent(1000,  vehId1, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1000, vehId1, link1.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(1010, vehId2, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1010, vehId2, link1.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(1020, vehId3, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1020, vehId3, link1.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(1030, vehId1, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1030, vehId1, link2.getId()));

		// generate some pseudo traffic for hour 1: 1 veh on link 1; 4 veh on link 2
		ttimeCalculator.handleEvent(new LinkEnterEvent(3800,  vehId1, link1.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(4000, vehId1, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4000, vehId1, link1.getId()));
		ttimeCalculator.handleEvent(new LinkEnterEvent(4000,  vehId1, link2.getId()));
		ttimeCalculator.handleEvent(new LinkEnterEvent(4000,  vehId2, link2.getId()));
		ttimeCalculator.handleEvent(new LinkEnterEvent(4000,  vehId3, link2.getId()));
		ttimeCalculator.handleEvent(new LinkEnterEvent(4000,  vehId4, link2.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(4010, vehId1, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4010, vehId1, link2.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(4020, vehId2, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4020, vehId2, link2.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(4030, vehId3, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4030, vehId3, link2.getId()));
		ttimeCalculator.handleEvent(new LinkLeaveEvent(4040, vehId4, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4040, vehId4, link2.getId()));

		TravelTime ttimes = ttimeCalculator.getLinkTravelTimes();
		cls.addData(analyzer, ttimes);

		// volumes
		Assertions.assertEquals(3.0, cls.getAvgLinkVolumes(link1.getId())[0], 1e-8);
		Assertions.assertEquals(1.0, cls.getAvgLinkVolumes(link2.getId())[0], 1e-8);
		Assertions.assertEquals(1.0, cls.getAvgLinkVolumes(link1.getId())[1], 1e-8);
		Assertions.assertEquals(4.0, cls.getAvgLinkVolumes(link2.getId())[1], 1e-8);

		// travel times
		Assertions.assertEquals( 1530/3., cls.getAvgTravelTimes(link1.getId())[0], 1e-8 );
		Assertions.assertEquals( 30./1., cls.getAvgTravelTimes(link2.getId())[0], 1e-8 );
		Assertions.assertEquals( 200./1., cls.getAvgTravelTimes(link1.getId())[1], 1e-8 );
		Assertions.assertEquals( 100./4., cls.getAvgTravelTimes(link2.getId())[1], 1e-8 );
		Assertions.assertEquals( link2.getLength() / link2.getFreespeed(), cls.getAvgTravelTimes(link2.getId())[3], 1e-8 );

		cls.reset();

		Assertions.assertEquals(0, cls.getAvgLinkVolumes(link1.getId()).length);
		Assertions.assertEquals(0, cls.getAvgLinkVolumes(link2.getId()).length);
	}

	@Test
	void testWriteRead() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = s.getNetwork();
		NetworkFactory nf = network.getFactory();

		Node node1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		Link link1 = nf.createLink(Id.create("101", Link.class), node1, node2);
		Link link2 = nf.createLink(Id.create("102", Link.class), node2, node3);
		network.addLink(link1);
		network.addLink(link2);

		VolumesAnalyzer analyzer = new VolumesAnalyzer(3600, 86400, network);
		TravelTime ttimes = new FreeSpeedTravelTime();
		CalcLinkStats cls = new CalcLinkStats(network);

		Id<Vehicle> vehId = Id.create("1001", Vehicle.class);
		// generate some pseudo traffic for hour 0: 3 veh on link 1; 1 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1010, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1020, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1030, vehId, link2.getId()));

		// generate some pseudo traffic for hour 0: 1 veh on link 1; 4 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4010, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4020, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4030, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4040, vehId, link2.getId()));

		cls.addData(analyzer, ttimes);

		analyzer.reset(1);
		// generate some pseudo traffic for hour 0: 4 veh on link 1; 3 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1010, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1020, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1030, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1040, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1050, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(1060, vehId, link2.getId()));

		// generate some pseudo traffic for hour 0: 4 veh on link 1; 2 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4010, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4020, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4030, vehId, link1.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4040, vehId, link2.getId()));
		analyzer.handleEvent(new LinkLeaveEvent(4040, vehId, link2.getId()));

		cls.addData(analyzer, ttimes);

		String filename = this.util.getOutputDirectory() + "linkstats.txt";
		cls.writeFile(filename);
		Assertions.assertTrue(new File(filename).exists());
		CalcLinkStats cls2 = new CalcLinkStats(network);
		cls2.readFile(filename);

		Assertions.assertEquals(3.5, cls2.getAvgLinkVolumes(link1.getId())[0], 1e-8);
		Assertions.assertEquals(2.0, cls2.getAvgLinkVolumes(link2.getId())[0], 1e-8);
		Assertions.assertEquals(2.5, cls2.getAvgLinkVolumes(link1.getId())[1], 1e-8);
		Assertions.assertEquals(3.0, cls2.getAvgLinkVolumes(link2.getId())[1], 1e-8);
	}
}
