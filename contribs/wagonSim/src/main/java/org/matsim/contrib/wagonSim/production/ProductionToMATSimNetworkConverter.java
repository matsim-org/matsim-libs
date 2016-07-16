/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.production;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.Connection;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.ProductionNode;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.RbNode;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.RcpNode;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer.SatNode;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author balmermi
 * @since 2013-07-05
 */
public class ProductionToMATSimNetworkConverter {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////


	
	public static final double DEFAULT_CAPACITY = 99999.0;
	private static final Set<String> defaultModes = new HashSet<String>();
	private static final double RB_COORD_SHIFT = 500.0;
	private static final double RCP_COORD_SHIFT = 300.0;
	private static final double SAT_COORD_SHIFT = 100.0;
	
	private final Network network;
	private final ObjectAttributes nodeAttributes;
	private final ObjectAttributes linkAttributes;
	
	private static final Logger log = Logger.getLogger(ProductionToMATSimNetworkConverter.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ProductionToMATSimNetworkConverter(Network network, ObjectAttributes nodeAttributes, ObjectAttributes linkAttributes) {
		this.network = network;
		this.nodeAttributes = nodeAttributes;
		this.linkAttributes = linkAttributes;
		defaultModes.add(TransportMode.pt);
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public static final Set<String> getDefaultModes() {
		defaultModes.add(TransportMode.pt);
		return defaultModes;
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void convertNodes(ProductionDataContainer dataContainer, Network infraNetwork) {
		log.info("--- START converting nodes ---");
		NetworkFactory factory = network.getFactory();
		
		for (ProductionNode pNode : dataContainer.productionNodes.values()) {
			String nodeId = null;
			int index = pNode.id.toString().indexOf(ProductionParser.RB_NODE_POSTFIX);
			if (index == -1) { index = pNode.id.toString().indexOf(ProductionParser.RCP_NODE_POSTFIX); }
			if (index == -1) { index = pNode.id.toString().indexOf(ProductionParser.SAT_NODE_POSTFIX); }
			if (index == -1) { throw new RuntimeException("Production node id="+pNode.id+" is neither a RB nor a RCP nor a SAT node. Bailing out."); }
			nodeId = pNode.id.toString().substring(0,index);
			Node infraNode = infraNetwork.getNodes().get(Id.create(nodeId, Node.class));
			if (infraNode == null) { throw new RuntimeException("Production node id="+pNode.id+": A node id="+nodeId+" is not found in the infrastructure network. Bailing out."); }
			
			if (pNode instanceof RbNode) {
				RbNode n = (RbNode)pNode;
				Coord c = new Coord(infraNode.getCoord().getX() + RB_COORD_SHIFT, infraNode.getCoord().getY() + RB_COORD_SHIFT);
				Node node = factory.createNode(n.id,c);
				network.addNode(node);
				
				if (n.parentNode != null) { throw new RuntimeException("RB node id="+n.id+" must not contain a partent node. Bailing out."); }
				if (n.parentReceptionNode != null) { throw new RuntimeException("RB node id="+n.id+" must not contain a parent reception node. Bailing out."); }
				if (n.siblingNodes.isEmpty()) { log.info("RB node id="+n.id+" does not contain any RCP nodes."); }
				
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_PRODUCTIONNODE_TYPE,ProductionParser.RB_NODE_POSTFIX);
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_INFRANODE_ID,infraNode.getId().toString());
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_MAXTRAINCREATION,n.maxTrainCreation);
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_MAXWAGONSHUNTINGS,n.maxWagonShuntings);
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_SHUNTINGTIME,n.shuntingTime);
			}
			else if (pNode instanceof RcpNode) {
				RcpNode n = (RcpNode)pNode;
				Coord c = new Coord(infraNode.getCoord().getX() + RCP_COORD_SHIFT, infraNode.getCoord().getY() + RCP_COORD_SHIFT);
				Node node = factory.createNode(n.id,c);
				network.addNode(node);
				
				if (n.parentNode == null) { throw new RuntimeException("RCP node id="+n.id+" must contain a partent node. Bailing out."); }
				if (n.parentReceptionNode == null) { throw new RuntimeException("RCP node id="+n.id+" must contain a parent reception node. Bailing out."); }
				if (n.siblingNodes.isEmpty()) { log.info("RCP node id="+n.id+" does not contain any SAT nodes."); }
				
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_PRODUCTIONNODE_TYPE,ProductionParser.RCP_NODE_POSTFIX);
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_INFRANODE_ID,infraNode.getId().toString());
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_PARENTRECEPTIONNODE_ID,n.parentReceptionNode.id.toString());
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_ISBORDER,n.isBorder);
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_DELIVERYTYPE_ID,n.deliveryType.id.toString());
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_DELIVERYTYPE_DESC,n.deliveryType.desc);
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_DELIVERYTYPE_DISTR,Arrays.toString(n.deliveryType.hourlyDistribution));
			}
			else if (pNode instanceof SatNode) {
				SatNode n = (SatNode)pNode;
				Coord c = new Coord(infraNode.getCoord().getX() + SAT_COORD_SHIFT, infraNode.getCoord().getY() + SAT_COORD_SHIFT);
				Node node = factory.createNode(n.id,c);
				network.addNode(node);
				
				if (n.parentNode == null) { throw new RuntimeException("SAT node id="+n.id+" must contain a partent node. Bailing out."); }
				if (n.parentReceptionNode == null) { throw new RuntimeException("SAT node id="+n.id+" must contain a parent reception node. Bailing out."); }
				if (!n.siblingNodes.isEmpty()) { throw new RuntimeException("SAT node id="+n.id+" must not contain any sibling nodes. Bailing out."); }
				
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_PRODUCTIONNODE_TYPE,ProductionParser.SAT_NODE_POSTFIX);
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_INFRANODE_ID,infraNode.getId().toString());
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_PARENTRECEPTIONNODE_ID,n.parentReceptionNode.id.toString());
				nodeAttributes.putAttribute(n.id.toString(),WagonSimConstants.NODE_MINSERVICE,n.minService);
			}
			else { throw new RuntimeException("Production node id="+pNode.id+" is of unknown type. Bailing out."); }
		}
		log.info("--- END   converting nodes ---");
	}

	//////////////////////////////////////////////////////////////////////
	
	private final void convertHierarchyLinks(ProductionDataContainer dataContainer) {
		log.info("--- START converting Hierarchy links ---");
		NetworkFactory factory = network.getFactory();
		
		for (ProductionNode pNode : dataContainer.productionNodes.values()) {
			Node fromNode = network.getNodes().get(pNode.id);
			
			if (pNode.parentNode != null) {
				Node toNode = network.getNodes().get(pNode.parentNode.id);
				Link link = factory.createLink(Id.create(fromNode.getId().toString()+"-"+toNode.getId().toString(), Link.class),fromNode,toNode);
				network.addLink(link);
				link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(),toNode.getCoord()));
				link.setAllowedModes(defaultModes);
				link.setNumberOfLanes(1);
				link.setCapacity(DEFAULT_CAPACITY);
			}
			for (ProductionNode sNode : pNode.siblingNodes) {
				Node toNode = network.getNodes().get(sNode.id);
				Link link = factory.createLink(Id.create(fromNode.getId().toString()+"-"+toNode.getId().toString(), Link.class),fromNode,toNode);
				network.addLink(link);
				link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(),toNode.getCoord()));
				link.setAllowedModes(defaultModes);
				link.setNumberOfLanes(1);
				link.setCapacity(DEFAULT_CAPACITY);
			}
		}
		log.info("--- END   converting Hierarchy links ---");
	}

	//////////////////////////////////////////////////////////////////////
	
	private final void convertRoutesToLinks(ProductionDataContainer dataContainer) {
		log.info("--- START converting routes to links ---");
		NetworkFactory factory = network.getFactory();
		
		for (Connection c : dataContainer.connections.values()) {
			Node fromNode = network.getNodes().get(c.fromNode.id);
			Node toNode = network.getNodes().get(c.viaNodes.get(0).id);
			Id<Link> linkId = Id.create(fromNode.getId().toString()+"-"+toNode.getId().toString(), Link.class);
			if (!network.getLinks().containsKey(linkId)) {
				Link link = factory.createLink(linkId, fromNode, toNode);
				network.addLink(link);
				link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(),toNode.getCoord()));
				link.setAllowedModes(defaultModes);
				link.setNumberOfLanes(1);
				link.setCapacity(DEFAULT_CAPACITY);
			}
			for (int i=0; i<c.viaNodes.size()-1; i++) {
				fromNode = network.getNodes().get(c.viaNodes.get(i).id);
				toNode = network.getNodes().get(c.viaNodes.get(i+1).id);
				linkId = Id.create(fromNode.getId().toString()+"-"+toNode.getId().toString(), Link.class);
				if (!network.getLinks().containsKey(linkId)) {
					Link link = factory.createLink(linkId,fromNode,toNode);
					network.addLink(link);
					link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(),toNode.getCoord()));
					link.setAllowedModes(defaultModes);
					link.setNumberOfLanes(1);
					link.setCapacity(DEFAULT_CAPACITY);
				}
			}
		}
		log.info("--- END   converting routes to links ---");
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public final void convert(ProductionDataContainer dataContainer, Network infraNetwork) {
		convertNodes(dataContainer,infraNetwork);
		convertHierarchyLinks(dataContainer);
		convertRoutesToLinks(dataContainer);
	}
}
