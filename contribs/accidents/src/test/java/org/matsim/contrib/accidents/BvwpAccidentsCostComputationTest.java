package org.matsim.contrib.accidents;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author ikaddoura, mmayobre
 * 
 * 
 */
public class BvwpAccidentsCostComputationTest {

	@Test
	void test1() {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node n0 = factory.createNode(Id.createNodeId(0), new Coord(0, 1));
		network.addNode(n0);
		Node n1 = factory.createNode(Id.createNodeId(1), new Coord(1, 0));
		network.addNode(n1);
		
		Link link1 = factory.createLink(Id.createLinkId("link_1"), n0, n1);
		link1.setLength(15871.5137629840428417082875967025757);
		
		ArrayList<Integer> list = new ArrayList<>();
		list.add(0, 0); //Planfrei
		list.add(1, 0); //KFZ, außerhalb
		list.add(2, 2); //2 Lanes	
		
		double costs = AccidentCostComputationBVWP.computeAccidentCosts(4820, link1, list);
		Assertions.assertEquals(1772.13863066011, costs, MatsimTestUtils.EPSILON, "wrong cost");
	}

	@Test
	void test2() {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node n0 = factory.createNode(Id.createNodeId(0), new Coord(0, 1));
		network.addNode(n0);
		Node n1 = factory.createNode(Id.createNodeId(1), new Coord(1, 0));
		network.addNode(n1);
		
		Link link1 = factory.createLink(Id.createLinkId("link_1"), n0, n1);
		link1.setLength(1000);
		
		ArrayList<Integer> list = new ArrayList<>();
		list.add(0, 0); //Planfrei
		list.add(1, 0); //KFZ, außerhalb
		list.add(2, 2); //2 Lanes	
		
		double costs = AccidentCostComputationBVWP.computeAccidentCosts(1000, link1, list);
		Assertions.assertEquals(23.165, costs, MatsimTestUtils.EPSILON, "wrong cost");
	}

	@Test
	void test3() {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node n0 = factory.createNode(Id.createNodeId(0), new Coord(0, 1));
		network.addNode(n0);
		Node n1 = factory.createNode(Id.createNodeId(1), new Coord(1, 0));
		network.addNode(n1);
		
		Link link1 = factory.createLink(Id.createLinkId("link_1"), n0, n1);
		link1.setLength(1000);
		
		ArrayList<Integer> list = new ArrayList<>();
		list.add(0, 1); //Plangleich
		list.add(1, 3); //Innerhalb
		list.add(2, 3); //2 Lanes	
		
		double costs = AccidentCostComputationBVWP.computeAccidentCosts(1000, link1, list);
		Assertions.assertEquals(101.53, costs, MatsimTestUtils.EPSILON, "wrong cost");
	}
}
