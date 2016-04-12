package playground.polettif.multiModalMap.tools;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.assertEquals;
import static playground.polettif.multiModalMap.tools.NetworkTools.*;

public class NetworkToolsTest {

	double testDelta = 1/1000.;
	Network network;

	Coord coordA = new Coord(0.0, 0.0);
	Coord coordB = new Coord(2.0, 0.0);
	Coord coordC = new Coord(2.0, 2.0);

	Coord coordD = new Coord(0.0, 2.0);
	Coord coordE = new Coord(-2.0, 2.0);
	Coord coordF = new Coord(-2.0, 0.0);
	Coord coordG = new Coord(-2.0, -2.0);
	Coord coordH = new Coord(0.0, -2.0);

	Coord coordI = new Coord(2.0, -2.0);
	Coord coordW = new Coord(-1.0, 3.0);
	Coord coordX = new Coord(0.5, 0.5);
	Coord coordY = new Coord(1.0, 0.0);
	Coord coordP = new Coord(0.7, 0.1);
	Coord coordZ = new Coord(1.0, -3.0);

	Node nodeA;
	Node nodeB;
	Node nodeC;
	Node nodeD;
	Node nodeX;
	Node nodeY;
	Node nodeZ;
	Node nodeW;
	Node nodeH;

	Link linkAB;
	Link linkBC;
	Link linkCD;
	Link linkDA;
	Link linkDB;
	Link linkDX;
	Link linkXA;
	Link linkAH;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepare() {
		network = NetworkUtils.createNetwork();
		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);

		nodeA = networkFactory.createNode(Id.createNodeId("A"), coordA);
		nodeB = networkFactory.createNode(Id.createNodeId("B"), coordB);
		nodeC = networkFactory.createNode(Id.createNodeId("C"), coordC);
		nodeD = networkFactory.createNode(Id.createNodeId("D"), coordD);
		nodeX = networkFactory.createNode(Id.createNodeId("X"), coordX);
		nodeY = networkFactory.createNode(Id.createNodeId("Y"), coordY);
		nodeZ = networkFactory.createNode(Id.createNodeId("Z"), coordZ);
		nodeW = networkFactory.createNode(Id.createNodeId("W"), coordW);
		nodeH = networkFactory.createNode(Id.createNodeId("H"), coordH);

		linkAB = networkFactory.createLink(Id.createLinkId("link:A:B"), nodeA, nodeB);
		linkBC = networkFactory.createLink(Id.createLinkId("link:B:C"), nodeB, nodeC);
		linkCD = networkFactory.createLink(Id.createLinkId("link:C:D"), nodeC, nodeD);
		linkDA = networkFactory.createLink(Id.createLinkId("link:D:A"), nodeD, nodeA);
		linkDB = networkFactory.createLink(Id.createLinkId("link:D:B"), nodeD, nodeB);
		linkDX = networkFactory.createLink(Id.createLinkId("link:D:X"), nodeD, nodeX);
		linkXA = networkFactory.createLink(Id.createLinkId("link:X:A"), nodeX, nodeA);
		linkAH = networkFactory.createLink(Id.createLinkId("link:A:H"), nodeA, nodeH);


		network.addNode(nodeA);
		network.addNode(nodeB);
		network.addNode(nodeC);
		network.addNode(nodeD);
		network.addNode(nodeX);
		network.addNode(nodeY);
		network.addNode(nodeZ);
		network.addNode(nodeW);
		network.addNode(nodeH);
		network.addLink(linkAB);
		network.addLink(linkBC);
		network.addLink(linkCD);
		network.addLink(linkDA);
		network.addLink(linkDB);
		network.addLink(linkDX);
		network.addLink(linkXA);
		network.addLink(linkAH);
	}

	/*
		     ^
             |
		 W   |
		     |
	 E   ·   D   ·   C
	         |
	 ·   ·   |   ·   ·
	         | X
	 F-------A---Y---B---->
	         |
	 ·   ·   |	 ·   ·
	    	 |
	 G   ·   H   ·   I
             |
	 ·   ·   |   Z   ·
	 */

	@Test
	public void testGetAzimuth() {
		assertEquals(0,   200* getAzimuth(coordA,coordD)/Math.PI, testDelta);
		assertEquals(50,  200* getAzimuth(coordA,coordC)/Math.PI, testDelta);
		assertEquals(100, 200* getAzimuth(coordA,coordB)/Math.PI, testDelta);
		assertEquals(150, 200* getAzimuth(coordA,coordI)/Math.PI, testDelta);
		assertEquals(200, 200* getAzimuth(coordA,coordH)/Math.PI, testDelta);
		assertEquals(250, 200* getAzimuth(coordA,coordG)/Math.PI, testDelta);
		assertEquals(300, 200* getAzimuth(coordA,coordF)/Math.PI, testDelta);
		assertEquals(350, 200* getAzimuth(coordA,coordE)/Math.PI, testDelta);
	}

	@Test
	public void testDistancePointLinesegment() {
		assertEquals(1.0, CoordUtils.calcEuclideanDistance(coordA, coordY), testDelta);

		assertEquals(2.0, CoordUtils.distancePointLinesegment(coordA, coordD, coordC), testDelta);
		assertEquals(1.0, CoordUtils.distancePointLinesegment(coordA, coordF, coordY), testDelta);

		assertEquals(CoordUtils.calcEuclideanDistance(coordH,coordZ), CoordUtils.distancePointLinesegment(coordH, coordG, coordZ), testDelta);
		assertEquals(CoordUtils.calcEuclideanDistance(coordH,coordZ), CoordUtils.distancePointLinesegment(coordG, coordH, coordZ), testDelta);
		assertEquals(CoordUtils.calcEuclideanDistance(coordA,coordC), CoordUtils.distancePointLinesegment(coordA, coordH, coordC), testDelta);

		assertEquals(CoordUtils.calcEuclideanDistance(coordA,coordB), CoordUtils.distancePointLinesegment(coordD, coordH, coordF), testDelta);
	}

	@Test
	public void testGetClosestPointOnLine() {
		Coord splitPoint = getClosestPointOnLine(coordD, coordB, coordX);

		System.out.println(splitPoint);
	}

	@Test
	public void checkTestDistances() {
//		System.out.println("P to A: " + CoordUtils.calcEuclideanDistance(coordA,coordP));
//		System.out.println("P to AB: "+ CoordUtils.distancePointLinesegment(coordA, coordB, coordP));
//		System.out.println("P to AX: "+ CoordUtils.distancePointLinesegment(coordA, coordX, coordP));
	}

	@Test
	public void testFindNClosestLinks() throws Exception {
		assertEquals("found the wrong link!", linkAH, findNClosestLinks((NetworkImpl) network, coordZ, 10, 10, 10).get(0));
		assertEquals("Not the correct number of closest links found!", 6, findNClosestLinks((NetworkImpl) network, coordX, 10, 6, 100).size());
		assertEquals("Not the correct number of closest links found!", 2, findNClosestLinks((NetworkImpl) network, coordX, 10, 2, 100).size());
		assertEquals("Not the correct closest link found!", linkAH, findNClosestLinks((NetworkImpl) network, coordZ, 10, 10, 100).get(0));
		assertEquals("Not the correct second closest link found!", linkAB, findNClosestLinks((NetworkImpl) network, coordZ, 10, 10, 100).get(1));
	}


}