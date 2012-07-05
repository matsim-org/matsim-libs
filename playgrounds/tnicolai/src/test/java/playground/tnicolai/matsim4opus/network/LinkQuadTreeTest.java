/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.network;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author thomas
 *
 */
public class LinkQuadTreeTest {
	
	ScenarioImpl scenario;
	private String noteType2 = "2";
	private Node nodeA, nodeB, nodeC, nodeD;
	private Link linkA, linkB, linkC, linkD;
	
	@Test
	public void testRouteChoiceTestSpanningTree(){
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createNetwork();
		testNearestLink();
	}
	
	/**
	 * creating a test network
	 */
	protected void createNetwork() {
	
		/**
		*     Network:
		*     
		*     (A)            (C)
		*		\              \
		* 		 \              \
		*		  \              \
		*	   (a) \ (b)      (c) \ (d)
		*		    \              \
		*		     \              \
		*		      \              \
		*		      (B)             (D)
		*/
		
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		
		// add nodes
		nodeA = network.createAndAddNode(new IdImpl(1), this.scenario.createCoord(680035.1875, 250686.7031), this.noteType2); // A
		nodeB = network.createAndAddNode(new IdImpl(2), this.scenario.createCoord(681169.3125, 250051.0), this.noteType2);	  // B
		nodeC = network.createAndAddNode(new IdImpl(3), this.scenario.createCoord(680514.8125, 251011.4063), this.noteType2); // C
		nodeD = network.createAndAddNode(new IdImpl(4), this.scenario.createCoord(682524.125, 250161.4063 ), this.noteType2); // D

		// add links
		linkA = network.createAndAddLink(new IdImpl(1), nodeA, nodeB, 1321.0, 11.1111111111111, 20000.0, 1.); // a
		linkB =network.createAndAddLink(new IdImpl(2), nodeB, nodeA, 1321.0, 11.1111111111111, 20000.0, 1.); // b
		linkC =network.createAndAddLink(new IdImpl(3), nodeC, nodeD, 2361.0, 18.0555555555556, 10800.0, 1.); // c
		linkD =network.createAndAddLink(new IdImpl(4), nodeD, nodeC, 2361.0, 18.0555555555556, 10800.0, 1.); // d

	}
	
	protected void testNearestLink(){
		
		/**
		*     Network:
		*     
		*     (A)            (C)
		*		\           *4 \  *1
		* 		 \              \
		*		  \              \
		*	   (a) \ (b)      (c) \ (d)*2
		*		    \              \
		*		     \              \
		*		      \              \ *3
		*		      (B)             (D)
		*/
		
		// create measure points		
		CoordImpl c1 = new CoordImpl(680699.1, 250976.0);
		CoordImpl c2 = new CoordImpl(681500.0, 250700.0); //CoordImpl(681410.0, 250670.0);
		CoordImpl c3 = new CoordImpl(682419.0, 250232.0);
		CoordImpl c4 = new CoordImpl(680602.2, 250934.2);
		
		// get network
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();	
		
		Node n1 = network.getNearestNode(c1);
		Link l1 = network.getNearestRightEntryLink(c1);
		Assert.assertTrue(n1.getId().compareTo(nodeC.getId()) == 0);
		Assert.assertTrue(l1.getId().compareTo(linkD.getId()) == 0);
		
		Node n2 = network.getNearestNode(c2);
		Link l2 = network.getNearestLinkExactly(c2);
		Node ln2= l2.getToNode();
//		Assert.assertTrue(n2.getId().compareTo(node3.getId()) == 0);
		Assert.assertTrue(l2.getId().compareTo(linkD.getId()) == 0);
		Assert.assertTrue(ln2.getId().compareTo(nodeC.getId()) == 0);
		
		Node n3 = network.getNearestNode(c3);
		Link l3 = network.getNearestLink(c3);
		Assert.assertTrue(n3.getId().compareTo(nodeD.getId()) == 0);
		Assert.assertTrue(l3.getId().compareTo(linkD.getId()) == 0);
		
		Node n4 = network.getNearestNode(c4);
		Link l4 = network.getNearestLink(c4);
		Assert.assertTrue(n4.getId().compareTo(nodeD.getId()) == 0);
		Assert.assertTrue(l4.getId().compareTo(linkC.getId()) == 0);
		
		System.out.println("Done!");
	}
}
