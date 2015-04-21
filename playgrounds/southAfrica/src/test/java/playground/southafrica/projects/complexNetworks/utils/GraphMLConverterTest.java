/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.utils;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import java.util.Iterator;

public class GraphMLConverterTest extends MatsimTestCase{
	
	/**
	 * Just tests to see if a MATSim {@link Network} can be written as a
	 * graphML file.
	 */
	public void testConvertToGraphML(){
		setupNetwork();
		
		try{
			GraphMLConverter.convertToGraphML(getOutputDirectory() + "network.xml", getOutputDirectory() + "blueprints.graphml");
		} catch(Exception e){
			fail();
		}
	}
	
	/**
	 * If the former test passed, it means there is a graphML file that we can
	 * read, and here we test if it has the same number of nodes (vertices) and
	 * links (edges) as the test graph.
	 * 
	 * TODO: Add more comprehensive testing, i.e. the attributes.
	 */
	public void testConvertFromGraphML(){
		setupNetwork();
		GraphMLConverter.convertToGraphML(getOutputDirectory() + "network.xml", getOutputDirectory() + "blueprints.graphml");
		Graph graph = null;
		try{
			graph = GraphMLConverter.convertFromGraphML(getOutputDirectory() + "blueprints.graphml");
		} catch(Exception e){
			fail();
		}
		
		int nodes = 0;
		Iterator<Vertex> vertices = graph.getVertices().iterator();
		while(vertices.hasNext()){
			vertices.next();
			nodes++;
		}
		assertEquals("Wrong number of nodes.", 4, nodes);

		int links = 0;
		Iterator<Edge> edges = graph.getEdges().iterator();
		while(edges.hasNext()){
			edges.next();
			links++;
		}
		assertEquals("Wrong number of links.", 6, links);
	}
	
	private void setupNetwork(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		NetworkFactory nf = network.getFactory();
		
		/* Nodes */
		Node n1 = nf.createNode(Id.create(1, Node.class), new CoordImpl(0, 5));
		Node n2 = nf.createNode(Id.create(2, Node.class), new CoordImpl(5, 10));
		Node n3 = nf.createNode(Id.create(3, Node.class), new CoordImpl(5, 0));
		Node n4 = nf.createNode(Id.create(4, Node.class), new CoordImpl(10, 5));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		
		/* Links */
		Link la = nf.createLink(Id.create("a", Link.class), n1, n2); la.setNumberOfLanes(1.0);
		Link lb = nf.createLink(Id.create("b", Link.class), n2, n4); lb.setNumberOfLanes(1.0);
		Link lc = nf.createLink(Id.create("c", Link.class), n4, n3); lc.setNumberOfLanes(1.0);
		Link ld = nf.createLink(Id.create("d", Link.class), n3, n1); ld.setNumberOfLanes(1.0);
		Link le = nf.createLink(Id.create("e", Link.class), n3, n2); le.setNumberOfLanes(2.0);
		Link lf = nf.createLink(Id.create("f", Link.class), n2, n3); lf.setNumberOfLanes(2.0);
		network.addLink(la);
		network.addLink(lb);
		network.addLink(lc);
		network.addLink(ld);
		network.addLink(le);
		network.addLink(lf);
		
		/* Write the network to file. */
		NetworkWriter nw = new NetworkWriter(sc.getNetwork());
		nw.write(getOutputDirectory() + "network.xml");
	}

}
