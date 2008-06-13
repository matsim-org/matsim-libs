/* *********************************************************************** *
 * project: org.matsim.*
 * NodeDataII.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.multipath;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.network.Node;

public class NodeData {
	private final Node node;
	private double cost;
	private double time;
	private Node fromMatSimNode;
	private int iterationID;
	private double trace;
	private NodeData prev = null;
	private boolean isHead;
	private final boolean isShadow;
	private HashMap<Integer,NodeData> shadowNodes;
	private int shadowID = Integer.MIN_VALUE;
	private double prob;
	
	public NodeData(final Node n,final  boolean isShadow) {
		this.node = n;
		this.cost = Double.POSITIVE_INFINITY;
		this.shadowNodes = new HashMap<Integer,NodeData>();
		this.isShadow = isShadow;

	}
	

	//////////////////////////////////////////////////////////////////////
	// getter
	//////////////////////////////////////////////////////////////////////


	public double getCost() {
		return this.cost;
	}


	public Node getMatsimNode() {
		return this.node;
	}
	
	public Id getId() {
		return this.node.getId();
	}
	
	public double getTime() {
		return this.time;
	}
	
	
	public Collection<NodeData> getShadowNodes() {
		return this.shadowNodes.values();
	}
	
	public NodeData getPrev() {
		return this.prev;
	}
	
	public double getTrace() {
		return this.trace;
	}
	
	public boolean isHead() {
		return this.isHead;
	}
	
	public boolean isShadow() {
		return this.isShadow;
	}
	
	public int getShadowID() {
		return this.shadowID;
	}
	
	public boolean containsShadowNode(int shadowID) {
		return this.shadowNodes.containsKey(shadowID);
	}
	
	public double getProb() {
		return this.prob;
	}
	

	//////////////////////////////////////////////////////////////////////
	// setter
	//////////////////////////////////////////////////////////////////////
	public void setHead(final boolean isHead) {
		this.isHead = isHead;
	}

	public void addShadow(NodeData shadow) {
		this.shadowNodes.put(shadow.getShadowID(), shadow);
		
	}
	
	public void setProb(double p) {
		this.prob = p;
		
	}
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////
	
	public NodeData drawNode() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void visitInitNode(double startTime, int iterationID) {
		this.cost = 0;
		this.time = startTime;
		this.iterationID = iterationID;
		this.trace = 0;
		
	}
	
	public boolean isUTurn(Node n) {
		return this.fromMatSimNode == n ? true : false;
	}
	
	public boolean isVisited(int iterationID) {
		return (iterationID == this.iterationID);
	}
	
	public void visit(NodeData fromNodeData, double cost, double time, int iterationID, double trace) {
		this.time = time;
		this.cost = cost;
		this.iterationID = iterationID;
		this.trace = trace;
		this.prev = fromNodeData;
		this.isHead = true;
		this.fromMatSimNode = fromNodeData.getMatsimNode();
	}
	
	public void visitShadow(NodeData fromNodeData, double cost,	double time, int iterationID, double trace, int shadowID) {
		this.shadowID = shadowID;
		visit(fromNodeData, cost, time, iterationID, trace);
		
	}

	public void reset() {
		this.shadowNodes.clear();
		this.prev = null;
		this.fromMatSimNode = null;
		this.cost = Double.POSITIVE_INFINITY;
	}

	public void rmShadow(NodeData del) {
		this.shadowNodes.remove(del.getShadowID());
	}
	
	
	public static class ComparatorNodeData implements Comparator<NodeData>, Serializable {

		private static final long serialVersionUID = 1L;

		private boolean checkIDs = false;

		protected Map<Id, ? extends NodeData> nodeData;

		public ComparatorNodeData(Map<Id, ? extends NodeData> nodeData) {
			this.nodeData = nodeData;
		}

		public int compare(NodeData n1, NodeData n2) {
			double c1 = getKey(n1); // if a node
			double c2 = getKey(n2);

			return compare(n1, c1, n2, c2);
		}

		private int compare(NodeData n1, double c1, NodeData n2, double c2) {
			if (c1 < c2) {
				return -1;
			} else if (c1 == c2) {
				if (this.checkIDs) {
					return n1.getMatsimNode().compareTo(n2.getMatsimNode());
				}
				return 0;
			} else {
				return +1;
			}
		}

		public void setCheckIDs(boolean flag) {
			this.checkIDs = flag;
		}

		public double getKey(NodeData node) {
			return node.getCost();
		}
	}


























}
