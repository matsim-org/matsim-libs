/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.incidents;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Currently, the flow capacity values in the network file are given in 'vehicles per capacity period (default: 3600sec)'.
 * In contrast, the value specified in the network change events file is given in 'vehicles per sec'.
 * 
 * @author ikaddoura
 *
 */

public class NetworkChangeEventsTest {
	
	Id<Link> linkId1 = Id.create("link1", Link.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test1() {
		
		Scenario scenario = loadScenario();
				
//		LinkImpl link = (LinkImpl) scenario.getNetwork().getLinks().get(linkId1);
		TimeVariantLinkImpl link = (TimeVariantLinkImpl) scenario.getNetwork().getLinks().get(linkId1);
		
		Assert.assertEquals("Wrong capacity (before network change event).", 1000. / 3600., link.getFlowCapacityPerSec(6 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong capacity (after network change event).", 123., link.getFlowCapacityPerSec(16 * 3600.), MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong freespeed (before network change event).", 10., link.getFreespeed(6 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong freespeed (after network change event).", 123., link.getFreespeed(16 * 3600.), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Wrong capacity.", 1000., link.getCapacity(), MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Wrong capacity (before network change event).", 1000., link.getCapacity(6 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong capacity (before network change event)."
				+ "The value specified in the network change events file is given in 'vehicles per sec'.", 123. * 3600., link.getCapacity(16 * 3600.), MatsimTestUtils.EPSILON);
		
		// It seems that the flow capacity value in the network change event is interpreted as vehicles per second!
			
	}
		
	private Scenario loadScenario() {
		
		// (0) -----link1---- (1)
			
		Config config = ConfigUtils.createConfig();
		
		config.network().setTimeVariantNetwork(true);
		config.network().setInputFile(testUtils.getPackageInputDirectory() + "network_link1.xml");
		config.network().setChangeEventsInputFile(testUtils.getPackageInputDirectory() + "networkChangeEvent_link1.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		Network network = scenario.getNetwork();
//		
//		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
//		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(100., 0.));
//				
//		Link link1 = network.getFactory().createLink(linkId1, node0, node1);
//		
//		Set<String> modes = new HashSet<>();
//		modes.add("car");
//		
//		link1.setAllowedModes(modes);
//		link1.setCapacity(1000);
//		link1.setFreespeed(10.);
//		link1.setNumberOfLanes(2);
//		link1.setLength(1000);
//
//		network.addNode(node0);
//		network.addNode(node1);
//		network.addLink(link1);
		
//		NetworkWriter writer = new NetworkWriter(network);
//		writer.write(testUtils.getPackageInputDirectory() + "network_link1.xml");
		
		return scenario;
	}

}
