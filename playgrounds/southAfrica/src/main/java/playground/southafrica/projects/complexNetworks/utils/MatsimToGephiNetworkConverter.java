/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.TransformerUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.event.GraphEvent.Edge;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.io.GraphMLMetadata;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.io.graphml.GraphMetadata;

import playground.southafrica.utilities.Header;

/**
 * Class to read a MATSim {@link Network} and transform it so that it can be
 * read, as CSV file, into Gephi.
 * 
 * @author jwjoubert
 */
public class MatsimToGephiNetworkConverter {
	private final static Logger LOG = Logger.getLogger(MatsimToGephiNetworkConverter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(MatsimToGephiNetworkConverter.class.toString(), args);
		String networkFile = args[0];
		String gephiFile = args[1];
		
		MatsimToGephiNetworkConverter.convert(networkFile, gephiFile);
		
		Header.printFooter();
	}

	
	public static void convert(String networkFile, String gephiFile) {
		// Parse the MATSim network.
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc);
		mnr.readFile(networkFile);
		NetworkImpl n = (NetworkImpl) sc.getNetwork();
		
		// Create an empty graph.
		DirectedSparseGraph<Node, Link> graph = new DirectedSparseGraph<Node, Link>();
		
		// Populate the graph with the network elements.
		Iterator<Link> li = n.getLinks().values().iterator();
		while(li.hasNext()){
			Link l = li.next();
			graph.addEdge(l, l.getFromNode(), l.getToNode(), EdgeType.DIRECTED);
		}
		
		
		setupGraphML();

	}


	/**
	 * Determines how the MATSim network will be transformed into a GraphML
	 * object. This is the key area where modifications to the output should
	 * be made.
	 */
	private static void setupGraphML() {
		GraphMLWriter<Node, Link> gw = new GraphMLWriter<Node, Link>();
		
		/* Node Ids. Currently this is just the MATSim node Id. */
		Transformer<Node, String> t_nodeId = new Transformer<Node, String>() {
			@Override
			public String transform(Node node) {
				return node.getId().toString();
			}
		};

		/* Node data, including:
		 *  - longitude (x);
		 *  - latitude (y)
		 */
		Map<String, GraphMLMetadata<Node>> gmd_node = new TreeMap<String, GraphMLMetadata<Node>>();
		Transformer<Node, String> t_nodeX = new Transformer<Node, String>() {
			@Override
			public String transform(Node node) {
				return String.format("%.2f", node.getCoord().getX());
			}
		};
		GraphMLMetadata<Node> m_nodeX = new GraphMLMetadata<Node>("x", "0.00", t_nodeX);
		Transformer<Node, String> t_nodeY = new Transformer<Node, String>() {
			@Override
			public String transform(Node node) {
				return String.format("%.2f", node.getCoord().getY());
			}
		};
		GraphMLMetadata<Node> m_nodeY = new GraphMLMetadata<Node>("y", "0.00", t_nodeY);
		gmd_node.put("x", m_nodeX);
		gmd_node.put("y", m_nodeY);
		
		/* Link Ids. */
		Transformer<Link, String> t_linkId = new Transformer<Link, String>() {
			@Override
			public String transform(Link link) {
				return link.getId().toString();
			}
		};
		
		/* Link data, including:
		 *  - length;
		 *  - number of lanes;
		 *  - capacity (derivative of the above two, assuming 7m per vehicle);
		 */
		Map<String, GraphMLMetadata<Link>> gmd_link = new TreeMap<String, GraphMLMetadata<Link>>();
		Transformer<Link, String> t_linkLength = new Transformer<Link, String>() {
			@Override
			public String transform(Link link) {
				return String.format("%.2f", link.getLength());
			}
		};
		GraphMLMetadata<Link> m_linkLength = new GraphMLMetadata<Link>("length", "0.00", t_linkLength);
		Transformer<Link, String> t_linkLanes = new Transformer<Link, String>() {
			@Override
			public String transform(Link link) {
				return String.format("%d", link.getNumberOfLanes());
			}
		};
		GraphMLMetadata<Link> m_linkLanes = new GraphMLMetadata<Link>("lanes", "0.00", t_linkLanes);
		Transformer<Link, String> t_linkCapacity = new Transformer<Link, String>() {
			@Override
			public String transform(Link link) {
				return String.format("%.0f", link.getCapacity());
			}
		};
		GraphMLMetadata<Link> m_linkCapacity = new GraphMLMetadata<Link>("lanes", "0.00", t_linkCapacity);
		gmd_link.put("length", m_linkLength);
		gmd_link.put("lanes", m_linkLanes);
		gmd_link.put("capcity", m_linkCapacity);
		

		
		
	}


	public MatsimToGephiNetworkConverter() {
		
	}

	
	
}
