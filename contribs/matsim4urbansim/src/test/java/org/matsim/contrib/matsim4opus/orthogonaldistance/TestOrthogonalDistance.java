package org.matsim.contrib.matsim4opus.orthogonaldistance;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matsim4opus.utils.CreateOrthogonalTestNetwork;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Distances;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * This class tests the orthogonal distances MATSim calculates between nodes and links or between nodes and nodes via a link
 * for a simple network created with CreateOrthogonalTestNetwork.java.
 * 
 * The method getOrthogonalDistance assumes that a link has unlimited length.
 * So it gives just the distance between a point and a line.
 * 
 * This method calculates the distance between a point and a node via a link (which should contains the node) as the sum of
 * - the orthogonal distance between the point and the link and
 * - the distance between the intersection point of the orthogonal projection (from the point to the link) to the node.
 * If the orthogonal projection of the point to the line does not intersects the link, the method returns the euclidean distance between the point and the node.
 *
 * With our test network we are able to test different cases of distances between nodes and links:
 * 1. distance between a node and a link which contains the node (should be 0)
 * 2. distance between a node and a link which corresponds to the distance between the node and a point on the line corresponding to the link which does not lie on the link itself
 * 3. distance between a node and a link where the line from the node orthogonally to the link varies only in one coordinate direction (x- oder y-coordinate)
 * 4. distance between a node and a link where the line from the node orthogonally to the link varies in both coordinate directions, i.e. is diagonal
 * 
 * Also different cases of distances between nodes and nodes via links are possible:
 * 1. distance between a node and itself via a link who contains the node (should be 0)
 * 2. distance between a start and an end point of a link via the link itself (should be the length of the link)
 * 3. distance between a node and a end point of a link via the link itself where the line from the node orthogonally to the link varies only in one coordinate direction (x- oder y-coordinate)
 * 4. distance between a node and a end point of a link via the link itself where the line from the node orthogonally to the link varies in both coordinate directions, i.e. is diagonal
 * 5. distance between a node and a end point of a link via the link itself where the line from the node orthogonally to the link intersects the line at a point who does not lie on the link itself
 * 	  (in this special case the distance has to be the euclidean distance between the two nodes)
 * 
 * @author tthunig
 *
 */
public class TestOrthogonalDistance extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(TestOrthogonalDistance.class);
	
	/**
	 * This method tests the orthogonal distance between a node and a link.
	 * 
	 * In MATSim this distance is calculated as the distance between a point and a line.
	 * So the calculation assumes that a link has unlimited length.
	 */
	@Test
	public void testOrthogonalDistanceToLinks(){

		log.info("Start testing orthogonal distances to links.");
		
		long start = System.currentTimeMillis();
		
		Network network = CreateOrthogonalTestNetwork.createOrthogonalDistanceTestNetwork();			// creates a dummy network
		
		//test the orthogonal distances from node 2 to special links:
		double distanceFromNode2ToLink1 = NetworkUtil.getOrthogonalDistance2Link(network.getLinks().get(new IdImpl(1)), network.getNodes().get(new IdImpl(2)).getCoord());
		double distanceFromNode2ToLink7 = NetworkUtil.getOrthogonalDistance2Link(network.getLinks().get(new IdImpl(7)), network.getNodes().get(new IdImpl(2)).getCoord());
		double distanceFromNode2ToLink5 = NetworkUtil.getOrthogonalDistance2Link(network.getLinks().get(new IdImpl(5)), network.getNodes().get(new IdImpl(2)).getCoord());
		double distanceFromNode2ToLink3 = NetworkUtil.getOrthogonalDistance2Link(network.getLinks().get(new IdImpl(3)), network.getNodes().get(new IdImpl(2)).getCoord());
		
		log.info("distance from node 2 to link 1: " + distanceFromNode2ToLink1 );
		log.info("distance from node 2 to link 7: " + distanceFromNode2ToLink7 );
		log.info("distance from node 2 to link 5: " + distanceFromNode2ToLink5 );
		log.info("distance from node 2 to link 3: " + distanceFromNode2ToLink3 );
		Assert.assertTrue("distances to links do not correspond to the expected values",
				distanceFromNode2ToLink1 == 0.0   && // node 2 belongs to link 1
				distanceFromNode2ToLink7 == 375.0 && // with the assumption of unlimited link length, the distance between node 2 and link 7 corresponds to the euclidean distance of 375 m between node 2 and node 3
				distanceFromNode2ToLink5 == 500.0 && // the distance between node 2 to link 5 corresponds to the euclidean distance of 500 m between node 2 and node 1
				distanceFromNode2ToLink3 == 300.0 ); // the distance between node 2 to link 3 corresponds to the euclidean distance of 300 m between node 2 and the midpoint of link 3
		
		// cleaning up
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		log.info("Test orthogonal distances to links took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}
	
	/**
	 * This method tests the orthogonal distance between a node and a node via a given link.
	 * 
	 * In MATSim this distance is calculated as the sum of
	 * - the orthogonal distance between the node and the link and
	 * - the distance between the intersection point of the orthogonal projection (from the node to the line) to the network node.	
 	 *
	 */
	@Test
	public void testOrthogonalDistanceToNodes(){

		log.info("Start testing orthogonal distances to nodes.");
		
		long start = System.currentTimeMillis();
		
		Network network = CreateOrthogonalTestNetwork.createOrthogonalDistanceTestNetwork();			// creates a dummy network
		
		//test the orthogonal distances from node 2 to special nodes:	
		Distances distanceFromNode2ToNode2_Link1 = NetworkUtil.getDistance2Node(network.getLinks().get(new IdImpl(1)), network.getNodes().get(new IdImpl(2)).getCoord(), network.getNodes().get(new IdImpl(2)));
		Distances distanceFromNode2ToNode1_Link2 = NetworkUtil.getDistance2Node(network.getLinks().get(new IdImpl(2)), network.getNodes().get(new IdImpl(2)).getCoord(), network.getNodes().get(new IdImpl(1)));
		Distances distanceFromNode2ToNode4_Link5 = NetworkUtil.getDistance2Node(network.getLinks().get(new IdImpl(5)), network.getNodes().get(new IdImpl(2)).getCoord(), network.getNodes().get(new IdImpl(4)));
		Distances distanceFromNode2ToNode1_Link4 = NetworkUtil.getDistance2Node(network.getLinks().get(new IdImpl(4)), network.getNodes().get(new IdImpl(2)).getCoord(), network.getNodes().get(new IdImpl(1)));
		Distances distanceFromNode5ToNode1_Link1 = NetworkUtil.getDistance2Node(network.getLinks().get(new IdImpl(1)), network.getNodes().get(new IdImpl(5)).getCoord(), network.getNodes().get(new IdImpl(1)));
		
		//sum up the two parts of the distance for all calculated values
		double doubleDistanceFromNode2ToNode2_Link1 = distanceFromNode2ToNode2_Link1.getDisatancePoint2Road() + distanceFromNode2ToNode2_Link1.getDistanceRoad2Node();
		double doubleDistanceFromNode2ToNode1_Link2 = distanceFromNode2ToNode1_Link2.getDisatancePoint2Road() + distanceFromNode2ToNode1_Link2.getDistanceRoad2Node();
		double doubleDistanceFromNode2ToNode4_Link5 = distanceFromNode2ToNode4_Link5.getDisatancePoint2Road() + distanceFromNode2ToNode4_Link5.getDistanceRoad2Node();
		double doubleDistanceFromNode2ToNode1_Link4 = distanceFromNode2ToNode1_Link4.getDisatancePoint2Road() + distanceFromNode2ToNode1_Link4.getDistanceRoad2Node();
		double doubleDistanceFromNode5ToNode1_Link1 = distanceFromNode5ToNode1_Link1.getDisatancePoint2Road() + distanceFromNode5ToNode1_Link1.getDistanceRoad2Node();
				
		log.info("distance from node 2 to node 2 with link 1: " + doubleDistanceFromNode2ToNode2_Link1 );
		log.info("distance from node 2 to node 1 with link 2: " + doubleDistanceFromNode2ToNode1_Link2 );
		log.info("distance from node 2 to node 4 with link 5: " + doubleDistanceFromNode2ToNode4_Link5 );		
		log.info("distance from node 2 to node 1 with link 4: " + doubleDistanceFromNode2ToNode1_Link4 );
		log.info("distance from node 5 to node 1 with link 1: " + doubleDistanceFromNode5ToNode1_Link1 );
		Assert.assertTrue("distances to nodes do not correspond to the expected values",
				doubleDistanceFromNode2ToNode2_Link1 == 0.0   && // node 2 is the end point of link 1 -> no walking or driving necessary
				doubleDistanceFromNode2ToNode1_Link2 == 500.0 && // walking distance to link 2 = 0 m; length of link 2 = 500 m
				doubleDistanceFromNode2ToNode4_Link5 == 875.0 && // walking distance to link 4 = 500 m; length of link 5 = 375 m
				doubleDistanceFromNode2ToNode1_Link4 == 700.0 && // walking distance to link 4 = 300 m; part of link 4 to link 1 = 400 m
				doubleDistanceFromNode5ToNode1_Link1 == 625.0 ); // euclidean walking distance to node 1 = 625 m; no driving necessary
			
		// cleaning up
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		log.info("Test orthogonal distances to nodes took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}
		
}
