/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleReader_v1.java
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

package playground.nmviljoen.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import playground.nmviljoen.gridExperiments.GridExperiment;
import playground.nmviljoen.gridExperiments.GridExperiment.Archetype;
import playground.nmviljoen.gridExperiments.NmvLink;
import playground.nmviljoen.gridExperiments.NmvNode;

public class MultilayerInstanceReader_v1 extends MatsimXmlParser {
	final private Logger log = Logger.getLogger(getClass());
	
	/* Elements */
	private final static String MULTILAYER_NETWORK = "multilayerNetwork";
	private final static String PHYSICAL_NETWORK = "physicalNetwork";
	private final static String PHYSICAL_NODES = "physicalNodes";
	private final static String PHYSICAL_NODE = "physicalNode";
	private final static String PHYSICAL_EDGES = "physicalEdges";
	private final static String PHYSICAL_EDGE = "physicalEdge";
	private final static String LOGICAL_NODES = "logicalNodes";
	private final static String LOGICAL_NODE = "logicalNode";
	private final static String LOGICAL_EDGES = "logicalEdges";
	private final static String LOGICAL_EDGE = "logicalEdge";
	private final static String ASSOCIATIONS = "associations";
	private final static String ASSOCIATION = "association";
	private final static String SETS = "shortestPathSets";
	private final static String SET = "set";
	private final static String PATH = "path";
	
	/* Attributes. */
	private final static String ARCHETYPE = "archetype";
	private final static String INSTANCE_NUMBER = "number";
	
	private final static String PHYSICAL_NODE_ID = "id";
	private final static String PHYSICAL_NODE_X = "x";
	private final static String PHYSICAL_NODE_Y = "y";
	
	private final static String PHYSICAL_EDGE_FROM = "fromId";
	private final static String PHYSICAL_EDGE_TO = "toId";
	private final static String PHYSICAL_EDGE_WEIGHT = "weight";
	
	private final static String LOGICAL_NETWORK = "logicalNetwork";
	private final static String LOGICAL_NODE_ID = "id";
	private final static String LOGICAL_NODE_NAME = "name";
	private final static String LOGICAL_NODE_CAP = "capacity";
	private final static String LOGICAL_EDGE_FROM = "fromId";
	private final static String LOGICAL_EDGE_TO = "toId";
	private final static String LOGICAL_EDGE_WEIGHT = "weight";
	private final static String ASSOCIATION_LOGICAL_ID = "logicalId";
	private final static String ASSOCIATION_PHYSICAL_ID = "physicalId";
	
	private final static String SET_FROM = "fromId";
	private final static String SET_TO = "toId";
	
	private GridExperiment experiment;
	private DirectedGraph<NmvNode, NmvLink> physicalNetwork = null;
	private DirectedGraph<NmvNode, NmvLink> logicalNetwork = null;
	private String currentSetFrom;
	private String currentSetTo;
	
	
	public MultilayerInstanceReader_v1(GridExperiment experiment) {
		this.experiment = experiment;
	}
	
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(MULTILAYER_NETWORK.equals(name)){
			Archetype archetype = Archetype.parseArchetypeFromDescription(atts.getValue(ARCHETYPE));
			this.experiment.setArchetype(archetype);
			int instance = Integer.parseInt(atts.getValue(INSTANCE_NUMBER));
			this.experiment.setInstanceNumber(instance);
		} else if(PHYSICAL_NETWORK.equals(name)){
			physicalNetwork = new DirectedSparseMultigraph<>();
		} else if(PHYSICAL_NODES.equals(name)){
			/* Do nothing. */
		} else if(PHYSICAL_NODE.equals(name)){
			String id = atts.getValue(PHYSICAL_NODE_ID);
			Double x = Double.parseDouble(atts.getValue(PHYSICAL_NODE_X));
			Double y = Double.parseDouble(atts.getValue(PHYSICAL_NODE_Y));
			NmvNode node;
			if(x != null && y != null){
				node = new NmvNode(id, x, y);
			} else{
				node = new NmvNode(id);
			}
			this.physicalNetwork.addVertex(node);
		} else if(PHYSICAL_EDGES.equals(name)){
			/* Do nothing. */
		} else if(PHYSICAL_EDGE.equals(name)){
			NmvNode fromNode = getNode(physicalNetwork, atts.getValue(PHYSICAL_EDGE_FROM));
			NmvNode toNode = getNode(physicalNetwork, atts.getValue(PHYSICAL_EDGE_TO));
			Double weight = Double.parseDouble(atts.getValue(PHYSICAL_EDGE_WEIGHT));
			NmvLink link = new NmvLink(fromNode.getId() + "_" + toNode.getId(), weight != null ? weight : 0.0);
			this.physicalNetwork.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);
		} else if(LOGICAL_NETWORK.equals(name)){
			this.logicalNetwork = new DirectedSparseMultigraph<>();
		} else if(LOGICAL_NODES.equals(name)){
			/* Do nothing. */
		} else if(LOGICAL_NODE.equals(name)){
			String id = atts.getValue(LOGICAL_NODE_ID);
			@SuppressWarnings("unused")
			String nodeName = atts.getValue(LOGICAL_NODE_NAME);
			@SuppressWarnings("unused")
			String nodeCap = atts.getValue(LOGICAL_NODE_CAP);
			NmvNode node = new NmvNode(id);
			this.logicalNetwork.addVertex(node);
		} else if(LOGICAL_EDGES.equals(name)){
			/* Do nothing. */
		} else if(LOGICAL_EDGE.equals(name)){
			NmvNode fromNode = getNode(logicalNetwork, atts.getValue(LOGICAL_EDGE_FROM));
			NmvNode toNode = getNode(logicalNetwork, atts.getValue(LOGICAL_EDGE_TO));
			Double weight = Double.parseDouble(atts.getValue(LOGICAL_EDGE_WEIGHT));
			NmvLink link = new NmvLink(fromNode.getId() + "_" + toNode.getId(), weight != null ? weight : 0.0);
			this.logicalNetwork.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);
		} else if(ASSOCIATIONS.equals(name)){
			/* Do nothing. */
		} else if(ASSOCIATION.equals(name)){
			String logicalId = atts.getValue(ASSOCIATION_LOGICAL_ID);
			String physicalId = atts.getValue(ASSOCIATION_PHYSICAL_ID);
			this.experiment.addAssociation(logicalId, physicalId);
		} else if(SETS.equals(name)){
			/* Do nothing. */
		} else if(SET.equals(name)){
			currentSetFrom = atts.getValue(SET_FROM);
			currentSetTo = atts.getValue(SET_TO);
		} else if(PATH.equals(name)){
			/* Do nothing. */
		} else {
			throw new RuntimeException(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(PHYSICAL_NODE.equals(name)){
			/* Do nothing. */
		} else if (PHYSICAL_EDGE.equals(name)){
			/* Do nothing. */
		} else if (PHYSICAL_EDGES.equals(name)){
			/* Do nothing. */
		} else if (PHYSICAL_NETWORK.equals(name)){
			this.experiment.setPhysicalNetwork(physicalNetwork);
			this.physicalNetwork = null;
			log.info("Physical network has " 
					+ this.experiment.getPhysicalNetwork().getVertexCount()
					+ " nodes, and "
					+ this.experiment.getPhysicalNetwork().getEdgeCount() 
					+ " vertices.");
		} else if (LOGICAL_EDGE.equals(name)){
			/* Do nothing. */
		} else if (LOGICAL_EDGES.equals(name)){
			/* Do nothing. */
		} else if (LOGICAL_NETWORK.equals(name)){
			this.experiment.setLogicalNetwork(logicalNetwork);
			this.logicalNetwork = null;
			log.info("Logical network has " 
					+ this.experiment.getLogicalNetwork().getVertexCount()
					+ " nodes, and "
					+ this.experiment.getLogicalNetwork().getEdgeCount() 
					+ " vertices.");
		} else if (ASSOCIATION.equals(name)){
			/* Do nothing. */
		} else if (ASSOCIATIONS.equals(name)){
			/* Do nothing. */
		} else if (PATH.equals(name)){
			List<String> path = new ArrayList<>();
			String[] sa = content.split(" ");
			for(String s : sa){
				if(!s.equals("")){ 
					path.add(s);
				}
			}
			this.experiment.addShortestPath(currentSetFrom, currentSetTo, path);
		} else if (SET.equals(name)){
			this.currentSetFrom = null;
			this.currentSetTo = null;
		} else if (SETS.equals(name)){
			/* Do nothing. */
		} else if(MULTILAYER_NETWORK.equals(name)){
			/* Do nothing, we should be complete */
		}
	}
	
	private NmvNode getNode(DirectedGraph<NmvNode, NmvLink> network, String id){
		NmvNode node = null;
		Iterator<NmvNode> iterator = network.getVertices().iterator();
		while(iterator.hasNext() && node==null){
			NmvNode nextNode = iterator.next();
			if(nextNode.getId().equals(id)){
				node = nextNode;
			}
		}
		return node;
	}
	

	
	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		/* Currently the only multilayer network type is v1 */
		if ("multilayerNetwork_v1.dtd".equals(doctype)) {
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}

