package org.matsim.contrib.signals;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author aneumann
 * @author dgrether
 */
public class CalculateAngleTest {

	private static final Logger log = LogManager.getLogger(CalculateAngleTest.class);

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	* @author aneumann
	*/
	@Test
	void testGetLeftLane() {
		Config conf = utils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(conf);
		new MatsimNetworkReader(scenario.getNetwork()).parse(conf.network().getInputFileURL(conf.getContext()));

		Assertions.assertEquals(null, NetworkUtils.getLeftmostTurnExcludingU(scenario.getNetwork().getLinks().get(Id.create("1", Link.class))), "Has to be 'null', since there is no other way back but Link 11.");

		Assertions.assertEquals(
				scenario.getNetwork().getLinks().get(Id.create("2", Link.class)), NetworkUtils.getLeftmostTurnExcludingU(scenario.getNetwork().getLinks().get(Id.create("11", Link.class))));

		Assertions.assertEquals(
				scenario.getNetwork().getLinks().get(Id.create("3", Link.class)), NetworkUtils.getLeftmostTurnExcludingU(scenario.getNetwork().getLinks().get(Id.create("22", Link.class))));

		Assertions.assertEquals(
				scenario.getNetwork().getLinks().get(Id.create("4", Link.class)), NetworkUtils.getLeftmostTurnExcludingU(scenario.getNetwork().getLinks().get(Id.create("33", Link.class))));

		Assertions.assertEquals(
				scenario.getNetwork().getLinks().get(Id.create("1", Link.class)), NetworkUtils.getLeftmostTurnExcludingU(scenario.getNetwork().getLinks().get(Id.create("44", Link.class))));

		Assertions.assertEquals(
				scenario.getNetwork().getLinks().get(Id.create("5", Link.class)), NetworkUtils.getLeftmostTurnExcludingU(scenario.getNetwork().getLinks().get(Id.create("3", Link.class))));

	}

	/**
	 * @author dgrether
	 */
	@Test
	void testGetOutLinksSortedByAngle(){
		Scenario scenario;
		double twicePi = Math.PI * 2;
		double piStep = Math.PI / 180.0;
		for (double angle = 0.0; angle < twicePi; angle = angle + piStep){
			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			createNetwork(scenario, angle);
			Network net = scenario.getNetwork();
			TreeMap<Double, Link> m = NetworkUtils.getOutLinksSortedClockwiseByAngle(net.getLinks().get(Id.create(1, Link.class)));
			Entry<Double, Link> entry = m.firstEntry();
			Assertions.assertEquals(Id.create(2, Link.class), entry.getValue().getId(), "For angle " + angle + "CalculateAngle returns not the correct order of outlinks");
			entry = m.higherEntry(entry.getKey());
			Assertions.assertEquals(Id.create(3, Link.class), entry.getValue().getId(), "For angle " + angle + "CalculateAngle returns not the correct order of outlinks");
			entry = m.higherEntry(entry.getKey());
			Assertions.assertEquals(Id.create(4, Link.class), entry.getValue().getId(), "For angle " + angle + "CalculateAngle returns not the correct order of outlinks");

			Link leftLane = NetworkUtils.getLeftmostTurnExcludingU(net.getLinks().get(Id.create(1, Link.class)));
			Assertions.assertEquals(Id.create(2, Link.class), leftLane.getId());

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
	private void createNetwork(Scenario sc, double alpha){
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
		Coord coord1 = new Coord(x1, y1);
		Coord coord2 = new Coord(x2, y2);
		Coord coord3 = new Coord(x3, y3);
		Coord coord4 = new Coord(x4, y4);
		Coord coord5 = new Coord((double) 0, (double) 0);
		//the nodes
		Node node1 = netfac.createNode(Id.create("1", Node.class), coord1);
		net.addNode(node1);
		Node node2 = netfac.createNode(Id.create("2", Node.class), coord2);
		net.addNode(node2);
		Node node3 = netfac.createNode(Id.create("3", Node.class), coord3);
		net.addNode(node3);
		Node node4 = netfac.createNode(Id.create("4", Node.class), coord4);
		net.addNode(node4);
		Node node5 = netfac.createNode(Id.create("5", Node.class), coord5);
		net.addNode(node5);
		//the links
		Link link = netfac.createLink(Id.create(1, Link.class), node1, node5);
		net.addLink(link);
		link = netfac.createLink(Id.create(2, Link.class), node5, node2);
		net.addLink(link);
		link = netfac.createLink(Id.create(3, Link.class), node5, node3);
		net.addLink(link);
		link = netfac.createLink(Id.create(4, Link.class), node5, node4);
		net.addLink(link);
	}



}
