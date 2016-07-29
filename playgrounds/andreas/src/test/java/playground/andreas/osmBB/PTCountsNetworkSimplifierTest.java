/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.osmBB;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

public class PTCountsNetworkSimplifierTest extends MatsimTestCase{
	
	
	/**
	 * Test simple network
	 */
	@Test
	public void testSimplifyEmptyNetwork(){
		String inputPath = getClassInputDirectory();
		String outputPath = getOutputDirectory();
		
		String inNetwork = inputPath + "net.xml.gz";
		String inSchedule = null;
		String inVehicles = null;
		String inCounts = null;

		String outNetwork = outputPath + "net.xml.gz";
		String outSchedule = null;
		String outCounts = null;
		
		HashMap<String, String> nodeIdToCsNameMap = null;
		Set<String> linksBlocked = null;
		
		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(inNetwork, inSchedule, outNetwork, outSchedule, nodeIdToCsNameMap, inCounts, outCounts, inVehicles, linksBlocked);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptCountNetSimplifier.setMergeLinkStats(false);
		ptCountNetSimplifier.simplifyPTNetwork();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(outNetwork);
		
		Assert.assertEquals(21, network.getLinks().size());
		Assert.assertEquals(10, network.getNodes().size());
		
		Assert.assertNull(network.getLinks().get(Id.create("1201_1202", Link.class)));
		Assert.assertNull(network.getNodes().get(Id.create("1101", Node.class)));
	}
	
	/**
	 * Test pt link
	 */
	@Test
	public void testSimplifyPTNetwork(){
		String inputPath = getClassInputDirectory();
		String outputPath = getOutputDirectory();
		
		String inNetwork = inputPath + "net.xml.gz";
		String inSchedule = inputPath + "schedule.xml.gz";
		String inVehicles = inputPath + "vehicles.xml.gz";
		String inCounts = null;
		
		String outNetwork = outputPath + "net.xml.gz";
		String outSchedule = outputPath + "schedule.xml.gz";
		String outCounts = null;
		
		HashMap<String, String> nodeIdToCsNameMap = null;
		Set<String> linksBlocked = null;		
		
		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(inNetwork, inSchedule, outNetwork, outSchedule, nodeIdToCsNameMap, inCounts, outCounts, inVehicles, linksBlocked);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptCountNetSimplifier.setMergeLinkStats(false);
		ptCountNetSimplifier.simplifyPTNetwork();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(outNetwork);
		
		Assert.assertEquals(26, network.getLinks().size());
		Assert.assertEquals(14, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(Id.create("1103_1104", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1203_1204", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1303_1304", Link.class)));
		
		Assert.assertNull(network.getLinks().get(Id.create("1201_1202", Link.class)));
		Assert.assertNull(network.getNodes().get(Id.create("1101", Node.class)));
	}
	
	/**
	 * Test links with count stations
	 */
	@Test
	public void testSimplifyCountsNetwork(){
		String inputPath = getClassInputDirectory();
		String outputPath = getOutputDirectory();
				
		String inNetwork = inputPath + "net.xml.gz";
		String inSchedule = null;
		String inVehicles = null;
		String inCounts = inputPath + "counts.xml.gz";
		
		String outNetwork = outputPath + "net.xml.gz";
		String outSchedule = null;
		String outCounts = outputPath + "counts.xml.gz";
		
		HashMap<String, String> nodeIdToCsNameMap = new HashMap<String, String>();
		nodeIdToCsNameMap.put("1102", "CS1");
		nodeIdToCsNameMap.put("1202", "CS2");
		nodeIdToCsNameMap.put("1312", "CS3");
		
		Set<String> linksBlocked = null;
		
		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(inNetwork, inSchedule, outNetwork, outSchedule, nodeIdToCsNameMap, inCounts, outCounts, inVehicles, linksBlocked);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptCountNetSimplifier.setMergeLinkStats(false);
		ptCountNetSimplifier.simplifyPTNetwork();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(outNetwork);
		
		Assert.assertEquals(23, network.getLinks().size());
		Assert.assertEquals(12, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(Id.create("1101_1102", Link.class)));
		
		Assert.assertNull(network.getLinks().get(Id.create("1103_1104", Link.class)));
		Assert.assertNull(network.getNodes().get(Id.create("1103", Node.class)));
	}
	
	/** 
	 * Test PT and Counts at once
	 */
	@Test
	public void testSimplifyPTCountsNetwork(){
		String inputPath = getClassInputDirectory();
		String outputPath = getOutputDirectory();
				
		String inNetwork = inputPath + "net.xml.gz";
		String inSchedule = inputPath + "schedule.xml.gz";
		String inVehicles = inputPath + "vehicles.xml.gz";
		String inCounts = inputPath + "counts.xml.gz";
		
		String outNetwork = outputPath + "net.xml.gz";
		String outSchedule = outputPath + "schedule.xml.gz";
		String outCounts = outputPath + "counts.xml.gz";
		
		HashMap<String, String> nodeIdToCsNameMap = new HashMap<String, String>();
		nodeIdToCsNameMap.put("1102", "CS1");
		nodeIdToCsNameMap.put("1202", "CS2");
		nodeIdToCsNameMap.put("1312", "CS3");
		
		Set<String> linksBlocked = null;
		
		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(inNetwork, inSchedule, outNetwork, outSchedule, nodeIdToCsNameMap, inCounts, outCounts, inVehicles, linksBlocked);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptCountNetSimplifier.setMergeLinkStats(false);
		ptCountNetSimplifier.simplifyPTNetwork();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(outNetwork);
		
		Assert.assertEquals(28, network.getLinks().size());
		Assert.assertEquals(16, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(Id.create("1101_1102", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1103_1104", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1203_1204", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1303_1304", Link.class)));
		
		Assert.assertNull(network.getLinks().get(Id.create("1201_1202", Link.class)));
		Assert.assertNull(network.getNodes().get(Id.create("1201", Node.class)));
	}
	
	/**
	 * Test additional links marked as blocked
	 */
	@Test
	public void testSimplifyElseNetwork(){
		String inputPath = getClassInputDirectory();
		String outputPath = getOutputDirectory();
		
		String inNetwork = inputPath + "net.xml.gz";
		String inSchedule = null;
		String inVehicles = null;
		String inCounts = null;
		
		String outNetwork = outputPath + "net.xml.gz";
		String outSchedule = null;
		String outCounts = null;
		
		HashMap<String, String> nodeIdToCsNameMap = null;
		
		Set<String> linksBlocked = new TreeSet<String>();	
		linksBlocked.add("1101_1102");
		linksBlocked.add("1201_1202");
		linksBlocked.add("1301_1302");
		linksBlocked.add("1204_1203");
		linksBlocked.add("1314_1313");
		
		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(inNetwork, inSchedule, outNetwork, outSchedule, nodeIdToCsNameMap, inCounts, outCounts, inVehicles, linksBlocked);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptCountNetSimplifier.setMergeLinkStats(false);
		ptCountNetSimplifier.simplifyPTNetwork();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(outNetwork);
		
		Assert.assertEquals(29, network.getLinks().size());
		Assert.assertEquals(16, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(Id.create("1101_1102", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1201_1202", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1301_1302", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1204_1203", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1314_1313", Link.class)));
		
		Assert.assertNull(network.getLinks().get(Id.create("1103_1104", Link.class)));
		Assert.assertNull(network.getNodes().get(Id.create("1103", Node.class)));
	}
	

	/**
	 * Test all at once
	 */
	@Test
	public void testSimplifyAllNetwork(){
		String inputPath = getClassInputDirectory();
		String outputPath = getOutputDirectory();
		
		String inNetwork = inputPath + "net.xml.gz";
		String inSchedule = inputPath + "schedule.xml.gz";
		String inVehicles = inputPath + "vehicles.xml.gz";
		String inCounts = inputPath + "counts.xml.gz";
		
		String outNetwork = outputPath + "net.xml.gz";
		String outSchedule = outputPath + "schedule.xml.gz";
		String outCounts = outputPath + "counts.xml.gz";
		
		HashMap<String, String> nodeIdToCsNameMap = new HashMap<String, String>();
		nodeIdToCsNameMap.put("1102", "CS1");
		nodeIdToCsNameMap.put("1202", "CS2");
		nodeIdToCsNameMap.put("1312", "CS3");
		
		Set<String> linksBlocked = new TreeSet<String>();
		linksBlocked.add("1101_1102");
		linksBlocked.add("1201_1202");
		linksBlocked.add("1301_1302");
		linksBlocked.add("1204_1203");
		linksBlocked.add("1314_1313");
		
		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(inNetwork, inSchedule, outNetwork, outSchedule, nodeIdToCsNameMap, inCounts, outCounts, inVehicles, linksBlocked);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptCountNetSimplifier.setMergeLinkStats(false);
		ptCountNetSimplifier.simplifyPTNetwork();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(outNetwork);
		
		Assert.assertEquals(32, network.getLinks().size());
		Assert.assertEquals(18, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(Id.create("1101_1102", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1103_1104", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1203_1204", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1303_1304", Link.class)));
		
		Assert.assertNotNull(network.getLinks().get(Id.create("1101_1102", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1201_1202", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1301_1302", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1204_1203", Link.class)));
		Assert.assertNotNull(network.getLinks().get(Id.create("1314_1313", Link.class)));
		
		Assert.assertNull(network.getLinks().get(Id.create("1202_1201", Link.class)));
	}
	
}
