/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentConverter.java
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

/**
 * 
 */
package playground.nmviljoen.io.convertOldToNew;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jgrapht.alg.DirectedNeighborIndex;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import playground.nmviljoen.gridExperiments.GridExperiment;
import playground.nmviljoen.gridExperiments.GridExperiment.Archetype;
import playground.nmviljoen.gridExperiments.NmvLink;
import playground.nmviljoen.gridExperiments.NmvNode;
import playground.nmviljoen.io.MultilayerInstanceWriter;
import playground.southafrica.utilities.Header;

/**
 * Class to convert the old experiment files to the new XML format.
 * 
 * @author jwjoubert
 */
public class ExperimentConverter {
	final private static Logger LOG = Logger.getLogger(ExperimentConverter.class);
	private static DirectedGraph<NmvNode, NmvLink> physicalNetwork;
	private static DirectedGraph<NmvNode, NmvLink> shNetwork;
	private static DirectedGraph<NmvNode, NmvLink> dhNetwork;
	private static DirectedGraph<NmvNode, NmvLink> fcNetwork;
	private static Map<String, Coord> nodeMap;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ExperimentConverter.class.toString(), args);
		
		String physicalNetworkNodes = args[0];
		String physicalNetworkLinks = args[1];
		String singleHub = args[2];
		String doubleHub = args[3];
		String fullyConnected = args[4];
		
		
		
		
		String xmlFile = args[2];

		buildNodeMap(physicalNetworkNodes);
		addPhysicalEdges(physicalNetworkLinks);
		
		GridExperiment experiment = new GridExperiment();
		experiment.setArchetype(Archetype.MALIK);
		experiment.setInstanceNumber(123);
		experiment.setPhysicalNetwork(physicalNetwork);
		
		
		new MultilayerInstanceWriter(experiment).write(xmlFile);
		
		
		Header.printFooter();
	}
	
	private static void buildNodeMap(String physicalNodeFile){
		LOG.info("Reading the physical network nodes...");
		/* Build the physical network */
		physicalNetwork = new DirectedSparseMultigraph<>();
		nodeMap = new TreeMap<String, Coord>();
		BufferedReader brNodes = IOUtils.getBufferedReader(physicalNodeFile);
		try{
			String line = brNodes.readLine(); /* Header. */
			while((line = brNodes.readLine()) != null){
				String[] sa = line.split(",");
				String id = sa[0];
				double x = Double.parseDouble(sa[1]);
				double y = Double.parseDouble(sa[2]);
				Coord c = CoordUtils.createCoord(x, y);
				if(nodeMap.containsKey(id)){
					LOG.error("Duplicate node: " + line);
				} else{
					nodeMap.put(id, c);
					physicalNetwork.addVertex(new NmvNode(null, id, x, y));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(physicalNodeFile);
		} finally{
			try {
				brNodes.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(physicalNodeFile);
			}
		}
		LOG.info("Total number of nodes in map: " + nodeMap.size());
		LOG.info("Total number of nodes in physical network: " + physicalNetwork.getVertexCount());
	}
	
	
	private static void addPhysicalEdges(String physicalEdgeFile){
		LOG.info("Reading the physical network links...");
		BufferedReader brEdges = IOUtils.getBufferedReader(physicalEdgeFile);
		try{
			String line = brEdges.readLine(); /* Header. */
			while((line = brEdges.readLine()) != null){
				String[] sa = line.split(",");
				String fromId = sa[0];
				String toId = sa[1];
				double weight = Double.parseDouble(sa[2]);
				
				NmvNode fromNode = getNode(fromId);
				NmvNode toNode = getNode(toId);
				NmvLink link = new NmvLink(fromId + "_" + toId, weight);
				
				physicalNetwork.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(physicalEdgeFile);
		} finally{
			try {
				brEdges.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(physicalEdgeFile);
			}
		}
		LOG.info("Total number of edges in physical network: " + physicalNetwork.getEdgeCount());
		
	}
	
	private static NmvNode getNode(String id){
		NmvNode node = null;
		Iterator<NmvNode> iterator = physicalNetwork.getVertices().iterator();
		boolean found = false;
		while(!found && iterator.hasNext()){
			NmvNode thisNode = iterator.next();
			if(thisNode.getId().equalsIgnoreCase(id)){
				found = true;
				node = thisNode;
			}
		}
		if(node == null){
			throw new RuntimeException("Cannot find node '" + id + "'");
		}
		
		return node;
	}
	
	private static DirectedGraph<NmvNode, NmvLink> parseLogicalNetwork(String file){
		DirectedGraph<NmvNode, NmvLink> network = new DirectedSparseMultigraph<>();
		
		BufferedReader brEdges = IOUtils.getBufferedReader(file);
		try{
			String line = brEdges.readLine(); /* Header. */
			while((line = brEdges.readLine()) != null){
				String[] sa = line.split(",");
				String fromId = sa[0];
				String toId = sa[1];
				double weight = Double.parseDouble(sa[2]);
				
				NmvNode fromNode = getNode(fromId);
				NmvNode toNode = getNode(toId);
				NmvLink link = new NmvLink(fromId + "_" + toId, weight);
				
				physicalNetwork.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(file);
		} finally{
			try {
				brEdges.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(file);
			}
		}
		
		
		
		return network;
	}
	

}
