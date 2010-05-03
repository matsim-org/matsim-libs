package org.matsim.signalsystems;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author aneumann
 * @author dgrether
 */
public class CalculateAngleTest {
	
	private static final Logger log = Logger.getLogger(CalculateAngleTest.class);
  
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
    * @author aneumann
    */
	@Test 
	public void testGetLeftLane() {
		Config conf = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		ScenarioImpl scenario = new ScenarioImpl(conf);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();

		Assert.assertEquals("Has to be 'null', since there is no other way back but Link 11.",
				null, CalculateAngle.getLeftLane(scenario.getNetwork().getLinks().get(new IdImpl("1"))));

		Assert.assertEquals(
				scenario.getNetwork().getLinks().get(scenario.createId("2")), CalculateAngle.getLeftLane(scenario.getNetwork().getLinks().get(scenario.createId("11"))));

		Assert.assertEquals(
				scenario.getNetwork().getLinks().get(scenario.createId("3")), CalculateAngle.getLeftLane(scenario.getNetwork().getLinks().get(scenario.createId("22"))));
		
		Assert.assertEquals(
				scenario.getNetwork().getLinks().get(scenario.createId("4")), CalculateAngle.getLeftLane(scenario.getNetwork().getLinks().get(scenario.createId("33"))));
		
		Assert.assertEquals(
				scenario.getNetwork().getLinks().get(scenario.createId("1")), CalculateAngle.getLeftLane(scenario.getNetwork().getLinks().get(scenario.createId("44"))));
		
		Assert.assertEquals(
				scenario.getNetwork().getLinks().get(scenario.createId("5")), CalculateAngle.getLeftLane(scenario.getNetwork().getLinks().get(scenario.createId("3"))));
				
	}
	
	/**
	 * @author dgrether
	 */
	@Test
	public void testGetOutLinksSortedByAngle(){
		//create the ids
		List<Id> ids = new ArrayList<Id>();
		for (int i = 0; i <= 5; i++){
			ids.add(i, new IdImpl(Integer.toString(i)));
		}
		Scenario scenario;
		double twicePi = Math.PI * 2;
		double piStep = Math.PI / 180.0;
		for (double angle = 0.0; angle < twicePi; angle = angle + piStep){
			scenario = new ScenarioImpl();
			createNetwork(scenario, angle, ids);
			Network net = scenario.getNetwork();
			TreeMap<Double, Link> m = CalculateAngle.getOutLinksSortedByAngle(net.getLinks().get(ids.get(1)));
			Entry<Double, Link> entry = m.firstEntry();
			Assert.assertEquals("For angle " + angle + "CalculateAngle returns not the correct order of outlinks", ids.get(2), entry.getValue().getId());
			entry = m.higherEntry(entry.getKey());
			Assert.assertEquals("For angle " + angle + "CalculateAngle returns not the correct order of outlinks", ids.get(3), entry.getValue().getId());
			entry = m.higherEntry(entry.getKey());
			Assert.assertEquals("For angle " + angle + "CalculateAngle returns not the correct order of outlinks", ids.get(4), entry.getValue().getId());
			
			Link leftLane = CalculateAngle.getLeftLane(net.getLinks().get(ids.get(1)));
			Assert.assertEquals(ids.get(2), leftLane.getId());
			
		}
	}
	
	/**
	 * creates a network with node 5 at 0,0 the other nodes build a cross spanning 200 m
	 * coordinates:
	 *       3
	 *       |
	 *   2--5--4
	 *       |
	 *       1
	 * @param sc
	 * @param ids 
	 */
	private void createNetwork(Scenario sc, double alpha, List<Id> ids){
		Network net = sc.getNetwork();
		NetworkFactory netfac = net.getFactory();
		//create the coordinates
		double x1 = Math.cos((alpha + (Math.PI * 3/2))) * 100.0;
		double y1 = Math.sin((alpha + (Math.PI * 3/2))) * 100.0;
		double x2 = Math.cos((alpha + (Math.PI))) * 100.0;
		double y2 = Math.sin((alpha + (Math.PI))) * 100.0;
		double x3 = Math.cos((alpha + (Math.PI / 2))) * 100.0;
		double y3 = Math.sin((alpha + (Math.PI / 2))) * 100.0;
		double x4 = Math.cos(alpha) * 100.0;
		double y4 = Math.sin(alpha) * 100.0;
		Coord coord1 = sc.createCoord(x1, y1);
		Coord coord2 = sc.createCoord(x2, y2);
		Coord coord3 = sc.createCoord(x3, y3);
		Coord coord4 = sc.createCoord(x4, y4);
		Coord coord5 = sc.createCoord(0, 0);
		//the nodes
		Node node = netfac.createNode(sc.createId("1"), coord1);
		net.addNode(node);
		node = netfac.createNode(sc.createId("2"), coord2);
		net.addNode(node);
		node = netfac.createNode(sc.createId("3"), coord3);
		net.addNode(node);
		node = netfac.createNode(sc.createId("4"), coord4);
		net.addNode(node);
		node = netfac.createNode(sc.createId("5"), coord5);
		net.addNode(node);
		//the links
		Link link = netfac.createLink(ids.get(1), ids.get(1), ids.get(5));
		net.addLink(link);
		link = netfac.createLink(ids.get(2), ids.get(5), ids.get(2));
		net.addLink(link);
		link = netfac.createLink(ids.get(3), ids.get(5), ids.get(3));
		net.addLink(link);
		link = netfac.createLink(ids.get(4), ids.get(5), ids.get(4));
		net.addLink(link);
	}
	
	
	
}