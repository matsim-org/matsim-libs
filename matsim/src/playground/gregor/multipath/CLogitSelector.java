/* *********************************************************************** *
 * project: org.matsim.*
 * PSLSelector.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.network.Node;

public class CLogitSelector {

	private final  static double BETA = 1;
	private final static double THETA = 1;
	
	private HashMap<String,CLogitLink> pathTree = new HashMap<String,CLogitLink>();
	
	private double minCost = Double.MAX_VALUE;
	

	public void run(ArrayList<NodeDataII> toNodes, NodeDataII fromNode) {
		
		
		this.minCost = toNodes.get(0).getCost();
		builtPathTree(toNodes, fromNode);
		calcPSLogit(toNodes);
		
		
	}
	
	

	private void calcPSLogit(ArrayList<NodeDataII> toNodes) {
		ArrayList<Double> weights = new ArrayList<Double>(toNodes.size());
		double w_all = 0;
		for (NodeDataII toNode : toNodes) {
			NodeDataII curr = toNode;
			double w = 0;
			do {
				NodeDataII prev = curr.getPrev(); 
				String key = prev.getId().toString() + " " + curr.getId().toString();
				CLogitLink l = this.pathTree.get(key);
				w += l.cost/(toNode.getCost()* l.numPaths);
				
				curr = prev;
			} while (curr.getPrev() != null);
				w = Math.exp(-THETA * toNode.getCost()) + Math.pow(w, BETA); ///toNodes.get(0).getCost();
				weights.add(w);
				w_all += w; 
		}
		
		for (int i = 0; i < weights.size(); i++) {
			toNodes.get(i).setProb(weights.get(i)/w_all);
		}		
		
	}
	
	
	private void calcCLogit(ArrayList<NodeDataII> toNodes) {
		ArrayList<Double> g_all = new ArrayList<Double>();
		double all = 0;
		for (NodeDataII toNode : toNodes) {
			NodeDataII curr = toNode;
			double CF_k = 0;
			do {
				String key = curr.getPrev().getId().toString() + " " + curr.getId().toString();
				CLogitLink l = this.pathTree.get(key);
				CF_k += l.numPaths * l.cost / toNode.getCost();
				if (Double.isNaN(CF_k)) {
					int i=0; i++;
				}
				curr = curr.getPrev();
			} while (curr.getPrev() != null);
			CF_k = -BETA * Math.log(CF_k);
			double w = Math.exp(-toNode.getCost()/THETA + CF_k); 
		
			g_all.add(w);
			all += w;
			
		}
		
		for (int i = 0; i < g_all.size(); i++) {
			toNodes.get(i).setProb(g_all.get(i)/all);
		}
		
	}




//	private void calcLinkWeights(Collection<DCLogitLink> fromLinks) {
//		for (DCLogitLink l : fromLinks) {
//			l.setWeight(getOverlap(l) * Math.exp(-l.cost));
//			
//			
//			
//		}
//		
//	}




//	private double getOverlap(DCLogitLink l) {
//		return Math.pow(l.numPaths,-BETA * l.cost /this.minCost);
//	}




	private Collection<CLogitLink> builtPathTree(ArrayList<NodeDataII> toNodes, NodeDataII fromNode) {
		
		HashSet<CLogitLink> fromLinks = new HashSet<CLogitLink>();
		
		for (NodeDataII curr : toNodes) {
			do {
				CLogitLink l = getDCLogitLink(curr.getPrev().getMatsimNode(), curr.getMatsimNode());
				
//				if (!curr.getPrev().isShadow()) {
//				
//				}
				l.setCost(curr.getCost() - curr.getPrev().getCost());
				
				l.incrPaths();
				
				if (curr.getPrev().getId() != fromNode.getId()) {
					CLogitLink p = getDCLogitLink(curr.getPrev().getPrev().getMatsimNode(),curr.getPrev().getMatsimNode());
					l.addPred(p);
					p.addSucc(l);
				} else {
					fromLinks.add(l);
				}

				curr = curr.getPrev();
			} while (curr.getId() != fromNode.getId());

		}
		
		return fromLinks;
		
		
	}




	private CLogitLink getDCLogitLink(Node from, Node to) {
		String key =  from.getId().toString() +" " + to.getId().toString();
		CLogitLink d = this.pathTree.get(key);
		if (d == null) {
			d = new CLogitLink(from,to,key);
			this.pathTree.put(key,d);
		}
		return d;
	}




	private static class CLogitLink {
		private HashSet <CLogitLink> pred;
		private HashSet <CLogitLink> succ;
		private Node from;
		private Node to;
		private int numPaths;
		private String id;
		private double cost = Double.MAX_VALUE;
		private double weight;
		
		
		public CLogitLink(Node from, Node to, String id){
			this.pred = new HashSet<CLogitLink>();
			this.succ = new HashSet<CLogitLink>();
			this.from = from;
			this.to = to;
			this.numPaths = 0;
			this.id = id;
		}
		
		public void setWeight(double w) {
			this.weight = w;
			
		}

		public void addPred(CLogitLink  pred) {
			this.pred.add(pred);
		}
		
		public void addSucc(CLogitLink succ) {
			this.succ.add(succ);
		}
		
		public String getId() {
			return this.id;
		}
		
		public void incrPaths() {
			this.numPaths++;
		}
		
		public void setCost(double cost) {
			this.cost = Math.min(this.cost, cost);
		}
		
	}
	
}

