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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class PTCountsNetworkSimplifierTest {
	
	static final String inputPath = "F:/simplifyTest/in/";
	static final String outputPath = "F:/simplifyTest/out/";
	
	/**
	 * Test simple network
	 */
	@Test
	public void testSimplifyEmptyNetwork(){
		String testName = "empty_";
		
		String inNetwork = PTCountsNetworkSimplifierTest.inputPath + "net.xml";
		String inSchedule = null;
		String inVehicles = null;
		String inCounts = null;

		String outNetwork = PTCountsNetworkSimplifierTest.outputPath + testName + "net.xml";
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
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(outNetwork);
		
		Assert.assertEquals(21, network.getLinks().size());
		Assert.assertEquals(10, network.getNodes().size());
		
		Assert.assertNull(network.getLinks().get(new IdImpl("1201_1202")));
		Assert.assertNull(network.getNodes().get(new IdImpl("1101")));
	}
	
	/**
	 * Test pt link
	 */
	@Test
	public void testSimplifyPTNetwork(){
		String testName = "pt_";
		
		String inNetwork = PTCountsNetworkSimplifierTest.inputPath + "net.xml";
		String inSchedule = PTCountsNetworkSimplifierTest.inputPath + "schedule.xml";
		String inVehicles = PTCountsNetworkSimplifierTest.inputPath + "vehicles.xml";
		String inCounts = null;
		
		String outNetwork = PTCountsNetworkSimplifierTest.outputPath + testName + "net.xml";
		String outSchedule = PTCountsNetworkSimplifierTest.outputPath + testName + "schedule.xml";
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
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(outNetwork);
		
		Assert.assertEquals(26, network.getLinks().size());
		Assert.assertEquals(14, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1103_1104")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1203_1204")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1303_1304")));
		
		Assert.assertNull(network.getLinks().get(new IdImpl("1201_1202")));
		Assert.assertNull(network.getNodes().get(new IdImpl("1101")));
	}
	
	/**
	 * Test links with count stations
	 */
	@Test
	public void testSimplifyCountsNetwork(){
		String testName = "counts_";
		
		String inNetwork = PTCountsNetworkSimplifierTest.inputPath + "net.xml";
		String inSchedule = null;
		String inVehicles = null;
		String inCounts = PTCountsNetworkSimplifierTest.inputPath + "counts.xml";
		
		String outNetwork = PTCountsNetworkSimplifierTest.outputPath + testName + "net.xml";
		String outSchedule = null;
		String outCounts = PTCountsNetworkSimplifierTest.outputPath + testName + "counts.xml";
		
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
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(outNetwork);
		
		Assert.assertEquals(23, network.getLinks().size());
		Assert.assertEquals(12, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1101_1102")));
		
		Assert.assertNull(network.getLinks().get(new IdImpl("1103_1104")));
		Assert.assertNull(network.getNodes().get(new IdImpl("1103")));
	}
	
	/** 
	 * Test PT and Counts at once
	 */
	@Test
	public void testSimplifyPTCountsNetwork(){
		String testName = "ptCounts_";
		
		String inNetwork = PTCountsNetworkSimplifierTest.inputPath + "net.xml";
		String inSchedule = PTCountsNetworkSimplifierTest.inputPath + "schedule.xml";
		String inVehicles = PTCountsNetworkSimplifierTest.inputPath + "vehicles.xml";
		String inCounts = PTCountsNetworkSimplifierTest.inputPath + "counts.xml";
		
		String outNetwork = PTCountsNetworkSimplifierTest.outputPath + testName + "net.xml";
		String outSchedule = PTCountsNetworkSimplifierTest.outputPath + testName + "schedule.xml";
		String outCounts = PTCountsNetworkSimplifierTest.outputPath + testName + "counts.xml";
		
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
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(outNetwork);
		
		Assert.assertEquals(28, network.getLinks().size());
		Assert.assertEquals(16, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1101_1102")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1103_1104")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1203_1204")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1303_1304")));
		
		Assert.assertNull(network.getLinks().get(new IdImpl("1201_1202")));
		Assert.assertNull(network.getNodes().get(new IdImpl("1201")));
	}
	
	/**
	 * Test additional links marked as blocked
	 */
	@Test
	public void testSimplifyElseNetwork(){
		String testName = "else_";
		
		String inNetwork = PTCountsNetworkSimplifierTest.inputPath + "net.xml";
		String inSchedule = null;
		String inVehicles = null;
		String inCounts = null;
		
		String outNetwork = PTCountsNetworkSimplifierTest.outputPath + testName + "net.xml";
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
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(outNetwork);
		
		Assert.assertEquals(29, network.getLinks().size());
		Assert.assertEquals(16, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1101_1102")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1201_1202")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1301_1302")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1204_1203")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1314_1313")));
		
		Assert.assertNull(network.getLinks().get(new IdImpl("1103_1104")));
		Assert.assertNull(network.getNodes().get(new IdImpl("1103")));
	}
	

	/**
	 * Test all at once
	 */
	@Test
	public void testSimplifyAllNetwork(){
		String testName = "all_";
		
		String inNetwork = PTCountsNetworkSimplifierTest.inputPath + "net.xml";
		String inSchedule = PTCountsNetworkSimplifierTest.inputPath + "schedule.xml";
		String inVehicles = PTCountsNetworkSimplifierTest.inputPath + "vehicles.xml";
		String inCounts = PTCountsNetworkSimplifierTest.inputPath + "counts.xml";
		
		String outNetwork = PTCountsNetworkSimplifierTest.outputPath + testName + "net.xml";
		String outSchedule = PTCountsNetworkSimplifierTest.outputPath + testName + "schedule.xml";
		String outCounts = PTCountsNetworkSimplifierTest.outputPath + testName + "counts.xml";
		
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
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(outNetwork);
		
		Assert.assertEquals(32, network.getLinks().size());
		Assert.assertEquals(18, network.getNodes().size());
		
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1101_1102")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1103_1104")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1203_1204")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1303_1304")));
		
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1101_1102")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1201_1202")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1301_1302")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1204_1203")));
		Assert.assertNotNull(network.getLinks().get(new IdImpl("1314_1313")));
		
		Assert.assertNull(network.getLinks().get(new IdImpl("1202_1201")));
	}
	
}
