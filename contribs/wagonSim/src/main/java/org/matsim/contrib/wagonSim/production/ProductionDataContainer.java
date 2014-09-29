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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

/**
 * @author balmermi @ Seonzon AG
 * @since 3013-09-19
 */
public class ProductionDataContainer {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	final Map<Id<Node>,ProductionNode> productionNodes = new HashMap<>();
	final Map<String, RcpDeliveryType> rcpDeliveryTypes = new HashMap<>();
	
	final Map<Id<TrainClass>,TrainClass> trainClasss = new HashMap<>();
	
	final Map<Id<Connection>,Connection> connections = new HashMap<>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public ProductionDataContainer() {
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void printRcpDeliveryTypes() {
		for (Entry<String, RcpDeliveryType> e : rcpDeliveryTypes.entrySet()) {
			System.out.println(e.getKey().toString()+": "+e.getValue().toString());
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	public final void printProductionNodes() {
		System.out.println("RB Nodes:");
		for (Entry<Id<Node>, ProductionNode> e : productionNodes.entrySet()) {
			if (e.getValue() instanceof RbNode) {
				System.out.println(e.getKey().toString()+": "+e.getValue().toString());
			}
		}
		System.out.println("RCP Nodes:");
		for (Entry<Id<Node>, ProductionNode> e : productionNodes.entrySet()) {
			if (e.getValue() instanceof RcpNode) {
				System.out.println(e.getKey().toString()+": "+e.getValue().toString());
			}
		}
		System.out.println("Sat Nodes:");
		for (Entry<Id<Node>, ProductionNode> e : productionNodes.entrySet()) {
			if (e.getValue() instanceof SatNode) {
				System.out.println(e.getKey().toString()+": "+e.getValue().toString());
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	public final void printConnections() {
		System.out.println("Connections:");
		for (Entry<Id<Connection>, Connection> e : connections.entrySet()) {
			System.out.println(e.getKey().toString()+": "+e.getValue().toString());
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// inner classes: production network
	//////////////////////////////////////////////////////////////////////

	static abstract class ProductionNode {
		final Id<Node> id;
		ProductionNode parentNode = null;
		ProductionNode parentReceptionNode = null;
		final Set<ProductionNode> siblingNodes = new HashSet<ProductionNode>();
		
		ProductionNode(String name) { id = Id.create(name, Node.class); }
		
		@Override
		public String toString() {
			String str = "id="+id;
			if (parentNode == null) { str += ";parent="+parentNode; } else { str += ";parent="+parentNode.id; }
			if (parentReceptionNode == null) { str += ";reception="+parentReceptionNode; } else { str += ";reception="+parentReceptionNode.id; }
			if (siblingNodes.isEmpty()) { str += ";siblings="+siblingNodes.toString(); }
			else {
				str += ";siblings=[";
				for (ProductionNode pn : siblingNodes) { str += pn.id+";"; }
				str += "]";
			}
			return str;
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	
	static class SatNode extends ProductionNode {
		double minService = Double.NaN;
		
		SatNode(String name) { super(name); }

		@Override
		public String toString() {
			return super.toString()+";minService="+minService;
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	
	static class RcpNode extends ProductionNode {
		Boolean isBorder = null;
		RcpDeliveryType deliveryType = null;
		
		public RcpNode(String name) { super(name); }
		
		@Override
		public String toString() {
			String str = super.toString()+";isBorder="+isBorder;
			if (deliveryType == null) { str += ";deliveryType="+deliveryType; } else { str += ";deliveryType="+deliveryType.id; }
			return str;
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	
	static class RbNode extends ProductionNode {
		Integer maxTrainCreation = null;
		Integer maxWagonShuntings = null;
		Integer shuntingTime = null;
		
		public RbNode(String name) { super(name); }

		@Override
		public String toString() {
			return super.toString()+";maxTrainCreation="+maxTrainCreation+";maxWagonShuntings="+maxWagonShuntings+";shuntingTime="+shuntingTime;
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	
	static class RcpDeliveryType {
		final String id;
		final String desc;
		double [] hourlyDistribution = new double[24];

		public RcpDeliveryType(int id, String desc) {
			this.id = Integer.toString(id);
			this.desc = desc;
			for (int i=0; i<24; i++) { hourlyDistribution[i] = Double.NaN; }
		}

		@Override
		public String toString() {
			return "id="+id+";desc="+desc+";distr="+Arrays.toString(hourlyDistribution);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// inner classes: train class (not sure for what to use...)
	//////////////////////////////////////////////////////////////////////

	static class TrainClass {
		final Id<TrainClass> id;
		String desc = null;
		Integer speed = null;
		Integer weight = null;
		Integer length = null;

		public TrainClass(int id) { this.id = Id.create(id, TrainClass.class); }
	}
	
	//////////////////////////////////////////////////////////////////////
	// inner classes: production connections
	//////////////////////////////////////////////////////////////////////

	static class Connection {
		final Id<Connection> id; 
		final ProductionNode fromNode;
		final ProductionNode toNode;
		final List<ProductionNode> viaNodes = new ArrayList<ProductionNode>();
		
		Connection(Id<Connection> id, ProductionNode fromNode, ProductionNode toNode) { this.id = id; this.fromNode = fromNode; this.toNode = toNode; }
		
		@Override
		public String toString() {
			String str = "id="+id+";fromNode="+fromNode.id+";toNode="+toNode.id;
			if (viaNodes.isEmpty()) { str += ";viaNodes="+viaNodes.toString(); }
			else {
				str += ";viaNodes=[";
				for (ProductionNode pn : viaNodes) { str += pn.id+";"; }
				str += "]";
			}
			return str;
		}
	}

	
	//////////////////////////////////////////////////////////////////////

	static class ConnectionAttributes extends Connection {
		Integer maxTrainGrossWeight = null;
		Integer maxTrainLength = null;
		
		public ConnectionAttributes(Id<Connection> id, ProductionNode fromNode, ProductionNode toNode) { super(id,fromNode,toNode); }
	}
}
