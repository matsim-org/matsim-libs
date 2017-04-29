/* *********************************************************************** *
 * project: org.matsim.*
 * MultilayerInstanceWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.nmviljoen.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import playground.nmviljoen.gridExperiments.GridExperiment;
import playground.nmviljoen.gridExperiments.GridExperiment.Archetype;
import playground.nmviljoen.gridExperiments.NmvLink;
import playground.nmviljoen.gridExperiments.NmvNode;

public class MultilayerInstanceWriterTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testExperiment(){
		GridExperiment experiment = buildExperiment();
		
		assertEquals("Wrong archetype.", Archetype.NAVABI, experiment.getArchetype());
		assertEquals("Wrong instance number.", 123l, experiment.getInstanceNumber());
		
		assertNotNull("Should have physical network.", experiment.getPhysicalNetwork());
		assertEquals("Wrong number of nodes.", 3, experiment.getPhysicalNetwork().getVertexCount());
		assertEquals("Wrong number of edges.", 4, experiment.getPhysicalNetwork().getEdgeCount());

		assertNotNull("Should have logical network.", experiment.getPhysicalNetwork());
		assertEquals("Wrong number of nodes.", 2, experiment.getLogicalNetwork().getVertexCount());
		assertEquals("Wrong number of edges.", 2, experiment.getLogicalNetwork().getEdgeCount());
		
		assertEquals("Wrong association.", "1", experiment.getLogicalNodeFromPhysical("1"));
		assertEquals("Wrong association.", null, experiment.getLogicalNodeFromPhysical("2"));
		assertEquals("Wrong association.", "2", experiment.getLogicalNodeFromPhysical("3"));
		assertEquals("Wrong association.", "1", experiment.getPhysicalNodeFromLogical("1"));
		assertEquals("Wrong association.", "3", experiment.getPhysicalNodeFromLogical("2"));
		
		assertEquals("Wrong number of shortest path sets.", 2, experiment.getShortestPathSets().size());
		assertNotNull("Should find shortest parh set from '1' to '2'.", experiment.getShortestPathSets().get("1").get("2"));
		assertNotNull("Should find shortest parh set from '2' to '1'.", experiment.getShortestPathSets().get("2").get("1"));
	}
	
	@Test
	public void testV1() {
		GridExperiment experiment = buildExperiment();
		MultilayerInstanceWriter writer = new MultilayerInstanceWriter(experiment);
		
		try{
			writer.writeV1(utils.getOutputDirectory() + "test.xml.gz");
		} catch(Exception e){
			fail("Should write without throwing exception.");
		}
		
		BufferedReader br = IOUtils.getBufferedReader(utils.getOutputDirectory() + "test.xml.gz");
		try{
			assertTrue("Wrong line 1.", br.readLine().equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
			assertTrue("Wrong line 2.", br.readLine().equals("<!DOCTYPE multilayerNetwork SYSTEM \"http://matsim.org/files/dtd/multilayerNetwork_v1.dtd\">"));
			assertTrue("Wrong line 3.", br.readLine().equals(""));
			assertTrue("Wrong line 4.", br.readLine().equals("<multilayerNetwork archetype=\"Fully connected logical network\" number=\"123\">"));
			assertTrue("Wrong line 5.", br.readLine().equals("\t<physicalNetwork>"));
			assertTrue("Wrong line 6.", br.readLine().equals("\t\t<physicalNodes>"));
			assertTrue("Wrong line 7.", br.readLine().equals("\t\t\t<physicalNode id=\"1\" x=\"0.0\" y=\"0.0\"/>"));
			assertTrue("Wrong line 8.", br.readLine().equals("\t\t\t<physicalNode id=\"2\" x=\"10.0\" y=\"0.0\"/>"));
			assertTrue("Wrong line 9.", br.readLine().equals("\t\t\t<physicalNode id=\"3\" x=\"20.0\" y=\"0.0\"/>"));
			assertTrue("Wrong line 10.", br.readLine().equals("\t\t</physicalNodes>"));
			assertTrue("Wrong line 11.", br.readLine().equals(""));
			assertTrue("Wrong line 12.", br.readLine().equals("\t\t<physicalEdges>"));
			assertTrue("Wrong line 13.", br.readLine().equals("\t\t\t<physicalEdge fromId=\"1\" toId=\"2\" weight=\"1.0\"/>"));
			assertTrue("Wrong line 14.", br.readLine().equals("\t\t\t<physicalEdge fromId=\"2\" toId=\"1\" weight=\"1.0\"/>"));
			assertTrue("Wrong line 15.", br.readLine().equals("\t\t\t<physicalEdge fromId=\"2\" toId=\"3\" weight=\"1.0\"/>"));
			assertTrue("Wrong line 16.", br.readLine().equals("\t\t\t<physicalEdge fromId=\"3\" toId=\"2\" weight=\"1.0\"/>"));
			assertTrue("Wrong line 17.", br.readLine().equals("\t\t</physicalEdges>"));
			assertTrue("Wrong line 18.", br.readLine().equals("\t</physicalNetwork>"));
			assertTrue("Wrong line 19.", br.readLine().equals(""));
			assertTrue("Wrong line 20.", br.readLine().equals("\t<logicalNetwork>"));
			assertTrue("Wrong line 21.", br.readLine().equals("\t\t<logicalNodes>"));
			assertTrue("Wrong line 22.", br.readLine().equals("\t\t\t<logicalNode id=\"1\"/>"));
			assertTrue("Wrong line 23.", br.readLine().equals("\t\t\t<logicalNode id=\"2\"/>"));
			assertTrue("Wrong line 24.", br.readLine().equals("\t\t</logicalNodes>"));
			assertTrue("Wrong line 25.", br.readLine().equals(""));
			assertTrue("Wrong line 26.", br.readLine().equals("\t\t<logicalEdges>"));
			assertTrue("Wrong line 27.", br.readLine().equals("\t\t\t<logicalEdge fromId=\"1\" toId=\"2\" weight=\"1.0\"/>"));
			assertTrue("Wrong line 28.", br.readLine().equals("\t\t\t<logicalEdge fromId=\"2\" toId=\"1\" weight=\"1.0\"/>"));
			assertTrue("Wrong line 29.", br.readLine().equals("\t\t</logicalEdges>"));
			assertTrue("Wrong line 30.", br.readLine().equals("\t</logicalNetwork>"));
			assertTrue("Wrong line 31.", br.readLine().equals(""));
			assertTrue("Wrong line 32.", br.readLine().equals("\t<associations>"));
			assertTrue("Wrong line 33.", br.readLine().equals("\t\t<association logicalId=\"1\" physicalId=\"1\"/>"));
			assertTrue("Wrong line 34.", br.readLine().equals("\t\t<association logicalId=\"2\" physicalId=\"3\"/>"));
			assertTrue("Wrong line 35.", br.readLine().equals("\t</associations>"));
			assertTrue("Wrong line 36.", br.readLine().equals(""));
			assertTrue("Wrong line 37.", br.readLine().equals("\t<shortestPathSets>"));
			assertTrue("Wrong line 38.", br.readLine().equals("\t\t<set fromId=\"1\" toId=\"2\">"));
			assertTrue("Wrong line 39.", br.readLine().equals("\t\t\t<path> 1 2 3</path>"));
			assertTrue("Wrong line 40.", br.readLine().equals("\t\t</set>"));
			assertTrue("Wrong line 41.", br.readLine().equals("\t\t<set fromId=\"2\" toId=\"1\">"));
			assertTrue("Wrong line 42.", br.readLine().equals("\t\t\t<path> 3 2 1</path>"));
			assertTrue("Wrong line 43.", br.readLine().equals("\t\t</set>"));
			assertTrue("Wrong line 44.", br.readLine().equals("\t</shortestPathSets>"));
			assertTrue("Wrong line 45.", br.readLine().equals("</multilayerNetwork>"));
		} catch(Exception e){
			fail("Should read the output file without exception.");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				fail("Should not throw exception while closing reader.");
			}
		}
	}

	
	/**
	 * Construct a small multilayer network with the following structure:
	 * 
	 *  (1)<-------------------->(2)  Logical network
	 *   :                        :
	 *  [1]<-------->[2]<------->[3]  Physial network
	 * (0,0)       (10,0)      (20,0)
	 * @return
	 */
	public static GridExperiment buildExperiment(){
		GridExperiment experiment = new GridExperiment();
		experiment.setArchetype(Archetype.NAVABI);
		experiment.setInstanceNumber(123);

		/* Physical network. */
		NmvNode p1 = new NmvNode("1", 0.0, 0.0);
		NmvNode p2 = new NmvNode("2", 10.0, 0.0);
		NmvNode p3 = new NmvNode("3", 20.0, 0.0);
		NmvLink p12 = new NmvLink("1_2", 1.0);
		NmvLink p21 = new NmvLink("2_1", 1.0);
		NmvLink p23 = new NmvLink("2_3", 1.0);
		NmvLink p32 = new NmvLink("3_2", 1.0);
		DirectedGraph<NmvNode, NmvLink> physicalNetwork = new DirectedSparseMultigraph<>();
		physicalNetwork.addVertex(p1);
		physicalNetwork.addVertex(p2);
		physicalNetwork.addVertex(p3);
		physicalNetwork.addEdge(p12, p1, p2, EdgeType.DIRECTED);
		physicalNetwork.addEdge(p21, p2, p1, EdgeType.DIRECTED);
		physicalNetwork.addEdge(p23, p2, p3, EdgeType.DIRECTED);
		physicalNetwork.addEdge(p32, p3, p2, EdgeType.DIRECTED);
		experiment.setPhysicalNetwork(physicalNetwork);
		
		/* Logical network. */
		NmvNode l1 = new NmvNode("1");
		NmvNode l2 = new NmvNode("2");
		NmvLink l12 = new NmvLink("1_2", 1.0);
		NmvLink l21 = new NmvLink("2_1", 1.0);
		DirectedGraph<NmvNode, NmvLink> logicalNetwork = new DirectedSparseMultigraph<>();
		logicalNetwork.addVertex(l1);
		logicalNetwork.addVertex(l2);
		logicalNetwork.addEdge(l12, l1, l2, EdgeType.DIRECTED);
		logicalNetwork.addEdge(l21, l2, l1, EdgeType.DIRECTED);
		experiment.setLogicalNetwork(logicalNetwork);
		
		/* Association list. */
		experiment.addAssociation(l1.getId(), p1.getId());
		experiment.addAssociation(l2.getId(), p3.getId());
		
		/* Shortest path sets. */
		List<String> spab = new ArrayList<String>(3);
		spab.add("1"); spab.add("2"); spab.add("3");
		experiment.addShortestPath(l1.getId(), l2.getId(), spab);
		List<String> spba = new ArrayList<>(3);
		spba.add("3"); spba.add("2"); spba.add("1");
		experiment.addShortestPath(l2.getId(), l1.getId(), spba);
		
		return experiment;
	}

}
