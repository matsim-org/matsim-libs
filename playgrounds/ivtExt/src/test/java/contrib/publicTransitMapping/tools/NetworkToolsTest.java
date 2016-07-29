package contrib.publicTransitMapping.tools;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static playground.polettif.publicTransitMapping.tools.NetworkTools.*;

public class NetworkToolsTest {

	private final double testDelta = 1/1000.;
	private Network network;

	private final Coord coordA = new Coord(0.0, 0.0);
	private final Coord coordB = new Coord(2.0, 0.0);
	private final Coord coordC = new Coord(2.0, 2.0);

	private final Coord coordD = new Coord(0.0, 2.0);
	private final Coord coordE = new Coord(-2.0, 2.0);
	private final Coord coordF = new Coord(-2.0, 0.0);
	private final Coord coordG = new Coord(-2.0, -2.0);
	private final Coord coordH = new Coord(0.0, -2.0);

	private final Coord coordI = new Coord(2.0, -2.0);
	private final Coord coordW = new Coord(-1.0, 3.0);
	private final Coord coordX = new Coord(0.5, 0.5);
	private final Coord coordY = new Coord(1.0, 0.0);
	private final Coord coordP = new Coord(0.7, 0.1);
	private final Coord coordZ = new Coord(1.0, -3.0);

	private Node nodeA;
	private Node nodeB;
	private Node nodeC;
	private Node nodeD;
	private Node nodeE;
	private Node nodeX;
	private Node nodeY;
	private Node nodeZ;
	private Node nodeW;
	private Node nodeH;

	private Link linkAB;
	private Link linkBC;
	private Link linkCD;
	private Link linkDA;
	private Link linkDB;
	private Link linkDX;
	private Link linkXA;
	private Link linkAH;
	private Link linkYE;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepare() {
		network = NetworkUtils.createNetwork();
		NetworkFactory networkFactory = network.getFactory();

		nodeA = networkFactory.createNode(Id.createNodeId("A"), coordA);
		nodeB = networkFactory.createNode(Id.createNodeId("B"), coordB);
		nodeC = networkFactory.createNode(Id.createNodeId("C"), coordC);
		nodeD = networkFactory.createNode(Id.createNodeId("D"), coordD);
		nodeE = networkFactory.createNode(Id.createNodeId("E"), coordE);
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
		linkYE = networkFactory.createLink(Id.createLinkId("link:Y:E"), nodeY, nodeE);


		network.addNode(nodeA);
		network.addNode(nodeB);
		network.addNode(nodeC);
		network.addNode(nodeD);
		network.addNode(nodeE);
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
		network.addLink(linkYE);
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
		assertEquals(0,   200* CoordTools.getAzimuth(coordA,coordD)/Math.PI, testDelta);
		assertEquals(50,  200* CoordTools.getAzimuth(coordA,coordC)/Math.PI, testDelta);
		assertEquals(100, 200* CoordTools.getAzimuth(coordA,coordB)/Math.PI, testDelta);
		assertEquals(150, 200* CoordTools.getAzimuth(coordA,coordI)/Math.PI, testDelta);
		assertEquals(200, 200* CoordTools.getAzimuth(coordA,coordH)/Math.PI, testDelta);
		assertEquals(250, 200* CoordTools.getAzimuth(coordA,coordG)/Math.PI, testDelta);
		assertEquals(300, 200* CoordTools.getAzimuth(coordA,coordF)/Math.PI, testDelta);
		assertEquals(350, 200* CoordTools.getAzimuth(coordA,coordE)/Math.PI, testDelta);
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
		Coord splitPoint = CoordTools.getClosestPointOnLine(coordD, coordB, coordX);

		System.out.println(splitPoint);
	}

	@Test
	public void testFindNClosestLinks() throws Exception {
		assertEquals("found the wrong link!", linkAH, findClosestLinks(network, coordZ, 10, 10, 1, null, 10).get(0));
		assertEquals("Not the correct number of closest links found!", 3, findClosestLinks(network, coordZ, 100, 2, 1, null, 100).size());
		assertEquals("Not the correct number of closest links found!", 5, findClosestLinks(network, coordW, 10, 2, 1, null, 100).size());

		assertEquals("Not the correct closest link found!", linkAH, findClosestLinks(network, coordZ, 10, 10, 1, null, 100).get(0));
		assertEquals("Not the correct third closest link found!", linkYE, findClosestLinks(network, coordX, 10, 10, 2, null, 100).get(2));

//		findClosestLinks(network, coordW, null, 10, 10, 1, 100).get(0);
//		findClosestLinks(network, coordW, null, 10, 10, 1, 100).get(1);

	}

	@Test
	public void testRightSide() {
		assertFalse(coordIsOnRightSideOfLink(coordX, linkAB));
		assertFalse(coordIsOnRightSideOfLink(coordE, linkAB));
		assertFalse(coordIsOnRightSideOfLink(coordX, linkBC));
		assertFalse(coordIsOnRightSideOfLink(coordX, linkCD));
		assertFalse(coordIsOnRightSideOfLink(coordX, linkDA));

		assertTrue(coordIsOnRightSideOfLink(coordE, linkXA));
		assertFalse(coordIsOnRightSideOfLink(coordI, linkXA));

		assertFalse(coordIsOnRightSideOfLink(coordF, linkYE));
		assertFalse(coordIsOnRightSideOfLink(coordI, linkYE));
		assertFalse(coordIsOnRightSideOfLink(coordH, linkYE));
		assertTrue(coordIsOnRightSideOfLink(coordW, linkYE));
		assertTrue(coordIsOnRightSideOfLink(coordB, linkYE));
		assertTrue(coordIsOnRightSideOfLink(coordC, linkYE));
	}


}