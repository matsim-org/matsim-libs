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
import java.util.HashMap;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.v11.Lane;
import org.matsim.lanes.data.v11.LaneDefinitions;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory;
import org.matsim.lanes.data.v11.LanesToLinkAssignment;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.run.LaneDefinitonsV11ToV20Converter;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

import playground.dgrether.signalsystems.data.preprocessing.DgCalculateSignalGroups;


/**
 * Tests if the signal grouping algorithm works correctly for several crossing types.
 * @author dgrether
 *
 */
public class DgCalculateSignalGroupsTest {
	
	public Map<Integer, Id> ids = new HashMap<Integer, Id>();
	
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
		Map<Id, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(this.getId(1));
		Assert.assertNotNull(groups4signal);
		Assert.assertEquals(2, groups4signal.size());
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			if (group.getSignalIds().size() == 2){
				Assert.assertTrue(group.getSignalIds().contains(this.getId(23)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(43)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(13)));
			}
			else {
				Assert.assertFalse(group.getSignalIds().contains(this.getId(23)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(43)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(13)));
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
		Map<Id, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(this.getId(1));
		Assert.assertNotNull(groups4signal);
		Assert.assertEquals(3, groups4signal.size());
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			Assert.assertTrue(group.getSignalIds().size() >= 1);
			if (group.getSignalIds().contains(this.getId(232))){
				Assert.assertTrue(group.getSignalIds().contains(this.getId(231)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(232)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(432)));
				
				Assert.assertFalse(group.getSignalIds().contains(this.getId(431)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(131)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(132)));
			}
			else if (group.getSignalIds().contains(this.getId(131))) {
				Assert.assertTrue(group.getSignalIds().contains(this.getId(131)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(132)));
				
				Assert.assertFalse(group.getSignalIds().contains(this.getId(231)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(232)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(431)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(432)));
			}
			else {
				Assert.assertTrue(group.getSignalIds().contains(this.getId(431)));
				//TODO optimization include them
				//				Assert.assertTrue(group.getSignalIds().contains(this.getId(432)));
//				Assert.assertTrue(group.getSignalIds().contains(this.getId(132)));

				Assert.assertFalse(group.getSignalIds().contains(this.getId(131)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(231)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(232)));
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
		Map<Id, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(this.getId(1));
		Assert.assertNotNull(groups4signal);
		boolean foundSignal2 = false;
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			Assert.assertEquals(2, group.getSignalIds().size());
			if (group.getSignalIds().contains(this.getId(2))){
				foundSignal2 = true;
				Assert.assertTrue(group.getSignalIds().contains(this.getId(2)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(6)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(4)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(8)));
			}
			else {
				Assert.assertTrue(group.getSignalIds().contains(this.getId(4)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(8)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(2)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(6)));
			}
		}
		Assert.assertTrue(foundSignal2);
	}
	

	private void create1SignalOn4WayCrossing(SignalSystemsData ss) {
		SignalSystemsDataFactory fac = ss.getFactory();
		SignalSystemData system = fac.createSignalSystemData(this.getId(1));
		ss.addSignalSystemData(system);
		SignalData signal = fac.createSignalData(this.getId(2));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(2));
		
		signal = fac.createSignalData(this.getId(4));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(4));
		
		signal = fac.createSignalData(this.getId(6));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(6));

		signal = fac.createSignalData(this.getId(8));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(8));
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
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
//		
		//calculate the signal groups
		DgCalculateSignalGroups calcSignalGroups = new DgCalculateSignalGroups(signalsData.getSignalSystemsData(), scenario.getNetwork(), (LaneDefinitions20) scenario.getScenarioElement(LaneDefinitions20.ELEMENT_NAME));
		SignalGroupsData signalGroups = calcSignalGroups.calculateSignalGroupsData();
		//test them
		Assert.assertNotNull(signalGroups);
		Map<Id, SignalGroupData> groups4signal = signalGroups.getSignalGroupDataBySystemId(this.getId(1));
		Assert.assertNotNull(groups4signal);
		for (SignalGroupData group : groups4signal.values()){
			Assert.assertNotNull(group.getSignalIds());
			Assert.assertEquals(2, group.getSignalIds().size());
			if (group.getSignalIds().contains(this.getId(1))){
				Assert.assertTrue(group.getSignalIds().contains(this.getId(1)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(4)));
				
				Assert.assertFalse(group.getSignalIds().contains(this.getId(3)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(8)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(2)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(5)));
			}
			else if (group.getSignalIds().contains(this.getId(2))) {
				Assert.assertTrue(group.getSignalIds().contains(this.getId(2)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(5)));
				
				Assert.assertFalse(group.getSignalIds().contains(this.getId(3)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(8)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(1)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(4)));
			}
			else {
				Assert.assertTrue(group.getSignalIds().contains(this.getId(3)));
				Assert.assertTrue(group.getSignalIds().contains(this.getId(6)));
				
				Assert.assertFalse(group.getSignalIds().contains(this.getId(1)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(4)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(2)));
				Assert.assertFalse(group.getSignalIds().contains(this.getId(5)));
			}
		}

		
	
	}

	/**
	 * Creates 1 signal on each way for the 3 way network
	 */
	private void create1SignalOn3WayCrossing(SignalSystemsData ss) {
		SignalSystemsDataFactory fac = ss.getFactory();
		SignalSystemData system = fac.createSignalSystemData(this.getId(1));
		ss.addSignalSystemData(system);
		SignalData signal = fac.createSignalData(this.getId(23));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(23));
		
		signal = fac.createSignalData(this.getId(43));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(43));
		
		signal = fac.createSignalData(this.getId(13));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(13));
	}

	/**
	 * Creates 1 signal on each way for the 3 way network
	 */
	private void createManySignalsOn3WayCrossing(SignalSystemsData ss) {
		SignalSystemsDataFactory fac = ss.getFactory();
		SignalSystemData system = fac.createSignalSystemData(this.getId(1));
		ss.addSignalSystemData(system);
		//on link 23
		SignalData signal = fac.createSignalData(this.getId(231));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(23));
		signal.addLaneId(this.getId(1));

		signal = fac.createSignalData(this.getId(232));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(23));
		signal.addLaneId(this.getId(2));
		
		//on link 43
		signal = fac.createSignalData(this.getId(431));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(43));
		signal.addLaneId(this.getId(1));

		signal = fac.createSignalData(this.getId(432));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(43));
		signal.addLaneId(this.getId(2));
		
		//on link 13
		signal = fac.createSignalData(this.getId(131));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(13));
		signal.addLaneId(this.getId(1));
		
		signal = fac.createSignalData(this.getId(132));
		system.addSignalData(signal);
		signal.setLinkId(this.getId(13));
		signal.addLaneId(this.getId(2));
	}
	
	/**
	 * Creates lanes for the 3 way network, ids ascending from left to right
	 * @param sc 
	 */
	private void createLanesFor3WayNetwork(ScenarioImpl sc) {
		LaneDefinitions lanes = sc.getLaneDefinitions11();
		
		LaneDefinitionsFactory fac = lanes.getFactory();
		//link 13
		LanesToLinkAssignment l2l = fac.createLanesToLinkAssignment(this.getId(13));
		lanes.addLanesToLinkAssignment(l2l);
		Lane lane = fac.createLane(this.getId(1));
		l2l.addLane(lane);
		lane.addToLinkId(this.getId(32));
		lane = fac.createLane(this.getId(2));
		l2l.addLane(lane);
		lane.addToLinkId(this.getId(34));
		//link 23
		l2l = fac.createLanesToLinkAssignment(this.getId(23));
		lanes.addLanesToLinkAssignment(l2l);
		lane = fac.createLane(this.getId(1));
		l2l.addLane(lane);
		lane.addToLinkId(this.getId(34));
		lane = fac.createLane(this.getId(2));
		l2l.addLane(lane);
		lane.addToLinkId(this.getId(31));
		//link 43
		l2l = fac.createLanesToLinkAssignment(this.getId(43));
		lanes.addLanesToLinkAssignment(l2l);
		lane = fac.createLane(this.getId(1));
		l2l.addLane(lane);
		lane.addToLinkId(this.getId(31));
		lane = fac.createLane(this.getId(2));
		l2l.addLane(lane);
		lane.addToLinkId(this.getId(32));
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
		n = fac.createNode(this.getId(1), sc.createCoord(0.0, 0.0));
		net.addNode(n);
		n = fac.createNode(this.getId(2), sc.createCoord(-10.0, 10.0));
		net.addNode(n);
		n = fac.createNode(this.getId(3), sc.createCoord(0.0, 10.0));
		net.addNode(n);
		n = fac.createNode(this.getId(4), sc.createCoord(10.0, 8.0));
		net.addNode(n);
		l = fac.createLink(this.getId(13), this.getId(1), this.getId(3));
		net.addLink(l);
		l = fac.createLink(this.getId(31), this.getId(3), this.getId(1));
		net.addLink(l);
		l = fac.createLink(this.getId(23), this.getId(2), this.getId(3));
		net.addLink(l);
		l = fac.createLink(this.getId(32), this.getId(3), this.getId(2));
		net.addLink(l);
		l = fac.createLink(this.getId(34), this.getId(3), this.getId(4));
		net.addLink(l);
		l = fac.createLink(this.getId(43), this.getId(4), this.getId(3));
		net.addLink(l);
	}


	private Id getId(int i) {
		if (!this.ids.containsKey(i)){
			this.ids.put(i, new IdImpl(i));
		}
		return this.ids.get(i);
	}
	

}
