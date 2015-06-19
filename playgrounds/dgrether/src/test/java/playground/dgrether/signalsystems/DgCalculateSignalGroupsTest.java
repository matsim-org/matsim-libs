/* *********************************************************************** *
 * project: org.matsim.*
 * DgCalculateSignalGroupsTest
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
package playground.dgrether.signalsystems;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.v11.LaneData11;
import org.matsim.lanes.data.v11.LaneDefinitions11;
import org.matsim.lanes.data.v11.LaneDefinitions11Impl;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory11;
import org.matsim.lanes.data.v11.LanesToLinkAssignment11;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.run.LaneDefinitonsV11ToV20Converter;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

import playground.dgrether.signalsystems.data.preprocessing.DgCalculateSignalGroups;


/**
 * Tests if the signal grouping algorithm works correctly for several crossing types.
 * @author dgrether
 *
 */
public class DgCalculateSignalGroupsTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void test3WayCrossing1Signal(){
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseSignalSystems(true);
		Scenario sc = ScenarioUtils.createScenario(config);
		
		this.create3WayNetwork(sc);
		SignalSystemsData signalSystems = new SignalSystemsDataImpl();
		this.create1SignalOn3WayCrossing(signalSystems);

		DgCalculateSignalGroups calcSignalGroups = new DgCalculateSignalGroups(signalSystems, sc.getNetwork());
		SignalGroupsData signalGroups = calcSignalGroups.calculateSignalGroupsData();

		Assert.assertNotNull(signalGroups);
		Map<Id<SignalGroup>, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(Id.create(1, SignalSystem.class));
		Assert.assertNotNull(groups4signal);
		Assert.assertEquals(2, groups4signal.size());
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			if (group.getSignalIds().size() == 2){
				Assert.assertTrue(group.getSignalIds().contains(Id.create(23, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(43, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(13, Signal.class)));
			}
			else {
				Assert.assertFalse(group.getSignalIds().contains(Id.create(23, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(43, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(13 ,Signal.class)));
			}
		}
	}

	@Test
	public void test3WayCrossingManySignals(){
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseLanes(true);
		config.scenario().setUseSignalSystems(true);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(config);

		this.create3WayNetwork(sc);
		this.createLanesFor3WayNetwork(sc);
		SignalSystemsData signalSystems = new SignalSystemsDataImpl();
		this.createManySignalsOn3WayCrossing(signalSystems);

		DgCalculateSignalGroups calcSignalGroups = new DgCalculateSignalGroups(signalSystems, sc.getNetwork(), (LaneDefinitions20) sc.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
		SignalGroupsData signalGroups = calcSignalGroups.calculateSignalGroupsData();

		Assert.assertNotNull(signalGroups);
		Map<Id<SignalGroup>, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(Id.create(1, SignalSystem.class));
		Assert.assertNotNull(groups4signal);
		Assert.assertEquals(3, groups4signal.size());
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			Assert.assertTrue(group.getSignalIds().size() >= 1);
			if (group.getSignalIds().contains(Id.create(232, Signal.class))){
				Assert.assertTrue(group.getSignalIds().contains(Id.create(231, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(232, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(432, Signal.class)));
				
				Assert.assertFalse(group.getSignalIds().contains(Id.create(431, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(131, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(132, Signal.class)));
			}
			else if (group.getSignalIds().contains(Id.create(131, Signal.class))) {
				Assert.assertTrue(group.getSignalIds().contains(Id.create(131, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(132, Signal.class)));
				
				Assert.assertFalse(group.getSignalIds().contains(Id.create(231, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(232, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(431, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(432, Signal.class)));
			}
			else {
				Assert.assertTrue(group.getSignalIds().contains(Id.create(431, Signal.class)));
				//TODO optimization include them
				//				Assert.assertTrue(group.getSignalIds().contains(Id.create(432)));
//				Assert.assertTrue(group.getSignalIds().contains(Id.create(132)));

				Assert.assertFalse(group.getSignalIds().contains(Id.create(131, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(231, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(232, Signal.class)));
			}
		}
		
	}

	@Test
	public void test4WayCrossing1Signal(){
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseSignalSystems(true);
		File f = new File("a");
		System.err.println(f.getAbsolutePath());
		//network
		String inputDirectory = this.testUtils.getClassInputDirectory();
		config.network().setInputFile(inputDirectory + "network.xml.gz");

		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		SignalSystemsData signalSystems = new SignalSystemsDataImpl();
		this.create1SignalOn4WayCrossing(signalSystems);

		DgCalculateSignalGroups calcSignalGroups = new DgCalculateSignalGroups(signalSystems, scenario.getNetwork());
		SignalGroupsData signalGroups = calcSignalGroups.calculateSignalGroupsData();

		Assert.assertNotNull(signalGroups);
		Map<Id<SignalGroup>, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(Id.create(1, SignalSystem.class));
		Assert.assertNotNull(groups4signal);
		boolean foundSignal2 = false;
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			Assert.assertEquals(2, group.getSignalIds().size());
			if (group.getSignalIds().contains(Id.create(2, Signal.class))){
				foundSignal2 = true;
				Assert.assertTrue(group.getSignalIds().contains(Id.create(2, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(6, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(4, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(8, Signal.class)));
			}
			else {
				Assert.assertTrue(group.getSignalIds().contains(Id.create(4, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(8, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(2, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(6, Signal.class)));
			}
		}
		Assert.assertTrue(foundSignal2);
	}
	

	private void create1SignalOn4WayCrossing(SignalSystemsData ss) {
		SignalSystemsDataFactory fac = ss.getFactory();
		SignalSystemData system = fac.createSignalSystemData(Id.create(1, SignalSystem.class));
		ss.addSignalSystemData(system);
		SignalData signal = fac.createSignalData(Id.create(2, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(2, Link.class));
		
		signal = fac.createSignalData(Id.create(4, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(4, Link.class));
		
		signal = fac.createSignalData(Id.create(6, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(6, Link.class));

		signal = fac.createSignalData(Id.create(8, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(8, Link.class));
	}

	@Test
	public void test4WayCrossingManySignals() throws JAXBException, SAXException, ParserConfigurationException, IOException{
		Config conf = ConfigUtils.createConfig();
		conf.scenario().setUseLanes(true);
		//network
		String inputDirectory = this.testUtils.getClassInputDirectory();
		conf.network().setInputFile(inputDirectory + "network.xml.gz");
		String laneDefinitions = inputDirectory	+ "testLaneDefinitions_v1.1.xml";
		String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
		new LaneDefinitonsV11ToV20Converter().convert(laneDefinitions, lanes20, conf.network().getInputFile());
		conf.network().setLaneDefinitionsFile(lanes20);
		conf.scenario().setUseSignalSystems(true);
		String signalSystemsFile = inputDirectory + "testSignalSystems_v2.0.xml";
		conf.signalSystems().setSignalSystemFile(signalSystemsFile);

		//load the network
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(conf);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(scenario.getConfig().signalSystems()).loadSignalsData());
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
//		
		//calculate the signal groups
		DgCalculateSignalGroups calcSignalGroups = new DgCalculateSignalGroups(signalsData.getSignalSystemsData(), scenario.getNetwork(), (LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
		SignalGroupsData signalGroups = calcSignalGroups.calculateSignalGroupsData();
		//test them
		Assert.assertNotNull(signalGroups);
		Map<Id<SignalGroup>, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(Id.create(1, SignalSystem.class));
		Assert.assertNotNull(groups4signal);
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			Assert.assertEquals(2, group.getSignalIds().size());
			if (group.getSignalIds().contains(Id.create(1, Signal.class))){
				Assert.assertTrue(group.getSignalIds().contains(Id.create(1, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(4, Signal.class)));
				
				Assert.assertFalse(group.getSignalIds().contains(Id.create(3, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(8, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(2, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(5, Signal.class)));
			}
			else if (group.getSignalIds().contains(Id.create(2, Signal.class))) {
				Assert.assertTrue(group.getSignalIds().contains(Id.create(2, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(5, Signal.class)));
				
				Assert.assertFalse(group.getSignalIds().contains(Id.create(3, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(8, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(1, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(4, Signal.class)));
			}
			else {
				Assert.assertTrue(group.getSignalIds().contains(Id.create(3, Signal.class)));
				Assert.assertTrue(group.getSignalIds().contains(Id.create(6, Signal.class)));
				
				Assert.assertFalse(group.getSignalIds().contains(Id.create(1, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(4, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(2, Signal.class)));
				Assert.assertFalse(group.getSignalIds().contains(Id.create(5, Signal.class)));
			}
		}

		
	
	}

	/**
	 * Creates 1 signal on each way for the 3 way network
	 */
	private void create1SignalOn3WayCrossing(SignalSystemsData ss) {
		SignalSystemsDataFactory fac = ss.getFactory();
		SignalSystemData system = fac.createSignalSystemData(Id.create(1, SignalSystem.class));
		ss.addSignalSystemData(system);
		SignalData signal = fac.createSignalData(Id.create(23, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(23, Link.class));
		
		signal = fac.createSignalData(Id.create(43, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(43, Link.class));
		
		signal = fac.createSignalData(Id.create(13, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(13, Link.class));
	}

	/**
	 * Creates 1 signal on each way for the 3 way network
	 */
	private void createManySignalsOn3WayCrossing(SignalSystemsData ss) {
		SignalSystemsDataFactory fac = ss.getFactory();
		SignalSystemData system = fac.createSignalSystemData(Id.create(1, SignalSystem.class));
		ss.addSignalSystemData(system);
		//on link 23
		SignalData signal = fac.createSignalData(Id.create(231, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(23, Link.class));
		signal.addLaneId(Id.create(1, Lane.class));

		signal = fac.createSignalData(Id.create(232, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(23, Link.class));
		signal.addLaneId(Id.create(2, Lane.class));
		
		//on link 43
		signal = fac.createSignalData(Id.create(431, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(43, Link.class));
		signal.addLaneId(Id.create(1, Lane.class));

		signal = fac.createSignalData(Id.create(432, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(43, Link.class));
		signal.addLaneId(Id.create(2, Lane.class));
		
		//on link 13
		signal = fac.createSignalData(Id.create(131, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(13, Link.class));
		signal.addLaneId(Id.create(1, Lane.class));
		
		signal = fac.createSignalData(Id.create(132, Signal.class));
		system.addSignalData(signal);
		signal.setLinkId(Id.create(13, Link.class));
		signal.addLaneId(Id.create(2, Lane.class));
	}
	
	/**
	 * Creates lanes for the 3 way network, ids ascending from left to right
	 * @param sc 
	 */
	private void createLanesFor3WayNetwork(ScenarioImpl sc) {
		LaneDefinitions11 lanes = new LaneDefinitions11Impl();
		
		LaneDefinitionsFactory11 fac = lanes.getFactory();
		//link 13
		LanesToLinkAssignment11 l2l = fac.createLanesToLinkAssignment(Id.create(13, Link.class));
		lanes.addLanesToLinkAssignment(l2l);
		LaneData11 lane = fac.createLane(Id.create(1, Lane.class));
		l2l.addLane(lane);
		lane.addToLinkId(Id.create(32, Link.class));
		lane = fac.createLane(Id.create(2, Lane.class));
		l2l.addLane(lane);
		lane.addToLinkId(Id.create(34, Link.class));
		//link 23
		l2l = fac.createLanesToLinkAssignment(Id.create(23, Link.class));
		lanes.addLanesToLinkAssignment(l2l);
		lane = fac.createLane(Id.create(1, Lane.class));
		l2l.addLane(lane);
		lane.addToLinkId(Id.create(34, Link.class));
		lane = fac.createLane(Id.create(2, Lane.class));
		l2l.addLane(lane);
		lane.addToLinkId(Id.create(31, Link.class));
		//link 43
		l2l = fac.createLanesToLinkAssignment(Id.create(43, Link.class));
		lanes.addLanesToLinkAssignment(l2l);
		lane = fac.createLane(Id.create(1, Lane.class));
		l2l.addLane(lane);
		lane.addToLinkId(Id.create(31, Link.class));
		lane = fac.createLane(Id.create(2, Lane.class));
		l2l.addLane(lane);
		lane.addToLinkId(Id.create(32, Link.class));
		sc.addScenarioElement(LaneDefinitions20.ELEMENT_NAME, new LaneDefinitionsV11ToV20Conversion().convertTo20(lanes, sc.getNetwork()));
	}
	
	/**
	 * Creates a three waynetwork that looks like:
	 * <p>
	 * 2----- 3 ------ 4
	 *          |     
	 *          | 
	 *          |
	 *          1
	 * </p>
	 */
	private void create3WayNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();
		Node n;
		Link l;
		n = fac.createNode(Id.create(1, Node.class), sc.createCoord(0.0, 0.0));
		net.addNode(n);
		n = fac.createNode(Id.create(2, Node.class), sc.createCoord(-10.0, 10.0));
		net.addNode(n);
		n = fac.createNode(Id.create(3, Node.class), sc.createCoord(0.0, 10.0));
		net.addNode(n);
		n = fac.createNode(Id.create(4, Node.class), sc.createCoord(10.0, 8.0));
		net.addNode(n);
		l = fac.createLink(Id.create(13, Link.class), Id.create(1, Node.class), Id.create(3, Node.class));
		net.addLink(l);
		l = fac.createLink(Id.create(31, Link.class), Id.create(3, Node.class), Id.create(1, Node.class));
		net.addLink(l);
		l = fac.createLink(Id.create(23, Link.class), Id.create(2, Node.class), Id.create(3, Node.class));
		net.addLink(l);
		l = fac.createLink(Id.create(32, Link.class), Id.create(3, Node.class), Id.create(2, Node.class));
		net.addLink(l);
		l = fac.createLink(Id.create(34, Link.class), Id.create(3, Node.class), Id.create(4, Node.class));
		net.addLink(l);
		l = fac.createLink(Id.create(43, Link.class), Id.create(4, Node.class), Id.create(3, Node.class));
		net.addLink(l);
	}

}
