package playground.polettif.multiModalMap.mapping;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.assertEquals;

public class ToolsTest {

	double testDelta = 1/1000.;
	Network network;

	Coord A = new Coord(0.0, 0.0);
	Coord B = new Coord(2.0, 0.0);
	Coord C = new Coord(2.0, 2.0);

	Coord D = new Coord(0.0, 2.0);
	Coord E = new Coord(-2.0, 2.0);
	Coord F = new Coord(-2.0, 0.0);
	Coord G = new Coord(-2.0, -2.0);
	Coord H = new Coord(0.0, -2.0);

	Coord I = new Coord(2.0, -2.0);
	Coord W = new Coord(-1.0, 3.0);
	Coord X = new Coord(0.5, 0.5);
	Coord Y = new Coord(1.0, 0.0);
	Coord P = new Coord(0.7, 0.1);

	Coord Z = new Coord(1.0, -3.0);


	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepare() {
		network = NetworkUtils.createNetwork();
		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);
		network.addNode(networkFactory.createNode(Id.createNodeId("A"), A));
		network.addNode(networkFactory.createNode(Id.createNodeId("B"), B));
		network.addNode(networkFactory.createNode(Id.createNodeId("C"), C));
		network.addNode(networkFactory.createNode(Id.createNodeId("D"), D));
		network.addNode(networkFactory.createNode(Id.createNodeId("X"), X));
		network.addNode(networkFactory.createNode(Id.createNodeId("Y"), Y));
		network.addNode(networkFactory.createNode(Id.createNodeId("Z"), Z));
		network.addNode(networkFactory.createNode(Id.createNodeId("W"), W));
		network.addLink(networkFactory.createLink(Id.createLinkId("link:A:B"), network.getNodes().get(Id.createNodeId("A")), network.getNodes().get(Id.createNodeId("B"))));
		network.addLink(networkFactory.createLink(Id.createLinkId("link:B:C"), network.getNodes().get(Id.createNodeId("B")), network.getNodes().get(Id.createNodeId("C"))));
		network.addLink(networkFactory.createLink(Id.createLinkId("link:C:D"), network.getNodes().get(Id.createNodeId("C")), network.getNodes().get(Id.createNodeId("D"))));
		network.addLink(networkFactory.createLink(Id.createLinkId("link:D:A"), network.getNodes().get(Id.createNodeId("D")), network.getNodes().get(Id.createNodeId("A"))));
		network.addLink(networkFactory.createLink(Id.createLinkId("link:D:B"), network.getNodes().get(Id.createNodeId("D")), network.getNodes().get(Id.createNodeId("B"))));
		network.addLink(networkFactory.createLink(Id.createLinkId("link:D:X"), network.getNodes().get(Id.createNodeId("D")), network.getNodes().get(Id.createNodeId("X"))));
		network.addLink(networkFactory.createLink(Id.createLinkId("link:X:A"), network.getNodes().get(Id.createNodeId("X")), network.getNodes().get(Id.createNodeId("A"))));
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
		assertEquals(0, 200*Tools.getAzimuth(A,D)/Math.PI, testDelta);
		assertEquals(50, 200*Tools.getAzimuth(A,C)/Math.PI, testDelta);
		assertEquals(100, 200*Tools.getAzimuth(A,B)/Math.PI, testDelta);
		assertEquals(150, 200*Tools.getAzimuth(A,I)/Math.PI, testDelta);
		assertEquals(200, 200*Tools.getAzimuth(A,H)/Math.PI, testDelta);
		assertEquals(250, 200*Tools.getAzimuth(A,G)/Math.PI, testDelta);
		assertEquals(300, 200*Tools.getAzimuth(A,F)/Math.PI, testDelta);
		assertEquals(350, 200*Tools.getAzimuth(A,E)/Math.PI, testDelta);
	}

	@Test
	public void testDistancePointLinesegment() {
		assertEquals(1.0, CoordUtils.calcEuclideanDistance(A, Y), testDelta);

		assertEquals(2.0, CoordUtils.distancePointLinesegment(A, D, C), testDelta);
		assertEquals(1.0, CoordUtils.distancePointLinesegment(A, F, Y), testDelta);

		assertEquals(CoordUtils.calcEuclideanDistance(H,Z), CoordUtils.distancePointLinesegment(H, G, Z), testDelta);
		assertEquals(CoordUtils.calcEuclideanDistance(H,Z), CoordUtils.distancePointLinesegment(G, H, Z), testDelta);
		assertEquals(CoordUtils.calcEuclideanDistance(A,C), CoordUtils.distancePointLinesegment(A, H, C), testDelta);

		assertEquals(CoordUtils.calcEuclideanDistance(A,B), CoordUtils.distancePointLinesegment(D, H, F), testDelta);
	}

	@Test
	public void testGetClosestPointOnLine() {
		Coord splitPoint = Tools.getClosestPointOnLine(D, B, X);

		System.out.println(splitPoint);
	}

	@Test
	public void checkTestDistances() {
		System.out.println("P to A: " + CoordUtils.calcEuclideanDistance(A,P));
		System.out.println("P to AB: "+ CoordUtils.distancePointLinesegment(A,B,P));
		System.out.println("P to AX: "+ CoordUtils.distancePointLinesegment(A,X, P));
	}

	@Test
	public void testFindNearestLink() throws Exception {
	//	assertEquals("found the wrong link!", Id.createLinkId("link:A:B"), getNearestLinkBrute(network, P).getId());
	}

	@Test
	public void testSplitLink() throws Exception {

//		network = splitLink(network, Id.createLinkId("link:D:B"), Y);
//		network.addLink(networkFactory.createLink(Id.createLinkId("l42"), network.getNodes().get(Id.createNodeId("pD")), network.getNodes().get(Id.createNodeId("B"))));

//		new NetworkWriter(network).write("C:/Users/polettif/Desktop/data/test/network_new.xml");
	}

}