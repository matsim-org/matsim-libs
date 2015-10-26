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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.MatsimTestUtils;

/**
 * @author mrieser
 */
public class CalcLinkStatsTest {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();
	
	@Test
	public void testAddData() {
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
		
		Id<Person> agentId = Id.create("1001", Person.class);
		// generate some pseudo traffic for hour 0: 3 veh on link 1; 1 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1010, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1020, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1030, agentId, link2.getId(), null));
		
		// generate some pseudo traffic for hour 0: 1 veh on link 1; 4 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4010, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4020, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4030, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4040, agentId, link2.getId(), null));
		
		cls.addData(analyzer, ttimes);
		
		Assert.assertEquals(3.0, cls.getAvgLinkVolumes(link1.getId())[0], 1e-8);
		Assert.assertEquals(1.0, cls.getAvgLinkVolumes(link2.getId())[0], 1e-8);
		Assert.assertEquals(1.0, cls.getAvgLinkVolumes(link1.getId())[1], 1e-8);
		Assert.assertEquals(4.0, cls.getAvgLinkVolumes(link2.getId())[1], 1e-8);
		
		analyzer.reset(1);
		// generate some pseudo traffic for hour 0: 4 veh on link 1; 3 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1010, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1020, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1030, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1040, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1050, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1060, agentId, link2.getId(), null));
		
		// generate some pseudo traffic for hour 0: 4 veh on link 1; 2 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4010, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4020, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4030, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4040, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4040, agentId, link2.getId(), null));
		
		cls.addData(analyzer, ttimes);
		
		Assert.assertEquals(3.5, cls.getAvgLinkVolumes(link1.getId())[0], 1e-8);
		Assert.assertEquals(2.0, cls.getAvgLinkVolumes(link2.getId())[0], 1e-8);
		Assert.assertEquals(2.5, cls.getAvgLinkVolumes(link1.getId())[1], 1e-8);
		Assert.assertEquals(3.0, cls.getAvgLinkVolumes(link2.getId())[1], 1e-8);
		
		cls.reset();

		Assert.assertEquals(0, cls.getAvgLinkVolumes(link1.getId()).length);
		Assert.assertEquals(0, cls.getAvgLinkVolumes(link2.getId()).length);
	}
	

	@Test
	public void testWriteRead() {
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
		
		Id<Person> agentId = Id.create("1001", Person.class);
		// generate some pseudo traffic for hour 0: 3 veh on link 1; 1 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1010, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1020, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1030, agentId, link2.getId(), null));
		
		// generate some pseudo traffic for hour 0: 1 veh on link 1; 4 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4010, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4020, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4030, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4040, agentId, link2.getId(), null));
		
		cls.addData(analyzer, ttimes);
		
		analyzer.reset(1);
		// generate some pseudo traffic for hour 0: 4 veh on link 1; 3 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(1000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1010, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1020, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1030, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1040, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1050, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(1060, agentId, link2.getId(), null));
		
		// generate some pseudo traffic for hour 0: 4 veh on link 1; 2 veh on link 2
		analyzer.handleEvent(new LinkLeaveEvent(4000, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4010, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4020, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4030, agentId, link1.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4040, agentId, link2.getId(), null));
		analyzer.handleEvent(new LinkLeaveEvent(4040, agentId, link2.getId(), null));
		
		cls.addData(analyzer, ttimes);
		
		String filename = this.util.getOutputDirectory() + "linkstats.txt";
		cls.writeFile(filename);
		Assert.assertTrue(new File(filename).exists());
		CalcLinkStats cls2 = new CalcLinkStats(network);
		cls2.readFile(filename);

		Assert.assertEquals(3.5, cls2.getAvgLinkVolumes(link1.getId())[0], 1e-8);
		Assert.assertEquals(2.0, cls2.getAvgLinkVolumes(link2.getId())[0], 1e-8);
		Assert.assertEquals(2.5, cls2.getAvgLinkVolumes(link1.getId())[1], 1e-8);
		Assert.assertEquals(3.0, cls2.getAvgLinkVolumes(link2.getId())[1], 1e-8);
	}
}
