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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.network.Node;

public class NodeDataII {
	private static final Logger log = Logger.getLogger(NodeDataII.class);
	private final Node node;
	private double cost;
//	private int numPaths;
	private double time;
//	private Set<Node> fromMatSimNodes;
	private Node fromMatSimNode;
	private int iterationID;
	private double trace;
//	private List<NodeDataII> prev;
	private NodeDataII prev = null;
	private boolean isHead;
	private final boolean isShadow;
	private HashMap<Integer,NodeDataII> shadowNodes;
	private int shadowID = Integer.MIN_VALUE;
	private double prob;
	
	public NodeDataII(final Node n,final  boolean isShadow) {
		this.node = n;
//		this.numPaths = 0;
		this.cost = Double.POSITIVE_INFINITY;
//		this.fromMatSimNodes = new HashSet<Node>(3);
		this.shadowNodes = new HashMap<Integer,NodeDataII>();
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
	
//	public List<NodeDataII> getPrev() {
//		return this.prev;
//	}
	
	public Collection<NodeDataII> getShadowNodes() {
		return this.shadowNodes.values();
	}
	
	public NodeDataII getPrev() {
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
	
//	public int getNumPaths() {
//		return this.numPaths;
//	}

	//////////////////////////////////////////////////////////////////////
	// setter
	//////////////////////////////////////////////////////////////////////
	public void setHead(final boolean isHead) {
		this.isHead = isHead;
	}

	public void addShadow(NodeDataII shadow) {
		this.shadowNodes.put(shadow.getShadowID(), shadow);
		
	}
	
	public void setProb(double p) {
		this.prob = p;
		
	}
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////
	
	public NodeDataII drawNode() {
		// TODO Auto-generated method stub
		return null;
	}
	
//	public void resetVisited() {
//		this.numPaths = 0;
//		this.cost = Double.POSITIVE_INFINITY;
//	}
	

	
	public void visitInitNode(double startTime, int iterationID) {
		this.cost = 0;
//		this.numPaths = 1;
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
	
	public void visit(NodeDataII fromNodeData, double cost, double time, int iterationID, double trace) {
		this.time = time;
		this.cost = cost;
		this.iterationID = iterationID;
		this.trace = trace;
//		this.prev.add(fromNodeData);
		this.prev = fromNodeData;
		this.isHead = true;
//		this.fromMatSimNodes.add(fromNodeData.getMatsimNode());
		this.fromMatSimNode = fromNodeData.getMatsimNode();
//		this.numPaths = 1;
	}
	
	public void visitShadow(NodeDataII fromNodeData, double cost,	double time, int iterationID, double trace, int shadowID) {
		this.shadowID = shadowID;
		visit(fromNodeData, cost, time, iterationID, trace);
		
	}

	public void reset() {
		this.shadowNodes.clear();
		this.prev = null;
		this.fromMatSimNode = null;
		this.cost = Double.POSITIVE_INFINITY;
	}
	
//	public void touch(NodeDataII fromNodeData) {
////		this.prev.add(fromNodeData);
////		this.fromMatSimNodes.add(fromNodeData.getMatsimNode());
//	}
	
//	public void enlargeNumPaths(int paths) {
//		this.numPaths += paths;
//	}

	public void rmShadow(NodeDataII del) {
		this.shadowNodes.remove(del);
	}
	
	
	public static class ComparatorNodeDataII implements Comparator<NodeDataII>, Serializable {

		private static final long serialVersionUID = 1L;

		private boolean checkIDs = false;

		protected Map<Id, ? extends NodeDataII> nodeData;

		public ComparatorNodeDataII(Map<Id, ? extends NodeDataII> nodeData) {
			this.nodeData = nodeData;
		}

		public int compare(NodeDataII n1, NodeDataII n2) {
			double c1 = getKey(n1); // if a node
			double c2 = getKey(n2);

			return compare(n1, c1, n2, c2);
		}

		private int compare(NodeDataII n1, double c1, NodeDataII n2, double c2) {
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

		public double getKey(NodeDataII node) {
			return node.getCost();
		}
	}


























}
