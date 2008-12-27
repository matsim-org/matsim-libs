/* *********************************************************************** *
 * project: org.matsim.*
 * PSLogitSelector.java
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

package org.matsim.utils.vis.routervis.multipathrouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.matsim.network.Node;

public abstract class LogitSelector {

	protected HashMap<String,LogitLink> pathTree = new HashMap<String,LogitLink>();

	public void run(final ArrayList<NodeData> toNodes, final NodeData fromNode) {

		builtPathTree(toNodes, fromNode);
		calcProbabilities(toNodes);
		
	}

	abstract void calcProbabilities(ArrayList<NodeData> toNodes);
	
	private Collection<LogitLink> builtPathTree(final ArrayList<NodeData> toNodes, final NodeData fromNode) {
		
		final HashSet<LogitLink> fromLinks = new HashSet<LogitLink>();
		
		for (NodeData curr : toNodes) {
			do {
				final LogitLink l = getDCLogitLink(curr.getPrev().getMatsimNode(), curr.getMatsimNode());

				l.setCost(curr.getCost() - curr.getPrev().getCost());
				
				l.incrPaths();
				
				if (curr.getPrev().getId() != fromNode.getId()) {
					final LogitLink p = getDCLogitLink(curr.getPrev().getPrev().getMatsimNode(),curr.getPrev().getMatsimNode());
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




	private LogitLink getDCLogitLink(final Node from, final Node to) {
		final String key =  from.getId().toString() +" " + to.getId().toString();
		LogitLink d = this.pathTree.get(key);
		if (d == null) {
			d = new LogitLink(key);
			this.pathTree.put(key,d);
		}
		return d;
	}


	static class LogitLink {
		private final HashSet <LogitLink> pred;
		private final HashSet <LogitLink> succ;
		int numPaths;
		private final String id;
		double cost = Double.MAX_VALUE;
		
		public LogitLink(final String id){
			this.pred = new HashSet<LogitLink>();
			this.succ = new HashSet<LogitLink>();
			this.numPaths = 0;
			this.id = id;
		}

		public void addPred(final LogitLink pred) {
			this.pred.add(pred);
		}
		
		public void addSucc(final LogitLink succ) {
			this.succ.add(succ);
		}
		
		public String getId() {
			return this.id;
		}
		
		public void incrPaths() {
			this.numPaths++;
		}
		
		public void setCost(final double cost) {
			this.cost = Math.min(this.cost, cost);
		}
		
	}
	
}