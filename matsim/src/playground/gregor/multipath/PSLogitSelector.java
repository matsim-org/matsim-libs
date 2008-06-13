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

package playground.gregor.multipath;

import java.util.ArrayList;

public class PSLogitSelector extends LogitSelector {
	
	
	protected final  static double BETA = 4;
	protected final static double THETA = 2;
	
	
	protected void calcProbabilities(ArrayList<NodeData> toNodes) {
		ArrayList<Double> weights = new ArrayList<Double>(toNodes.size());
		double w_all = 0;
		for (NodeData toNode : toNodes) {
			NodeData curr = toNode;
			double w = 0;
			do {
				NodeData prev = curr.getPrev(); 
				String key = prev.getId().toString() + " " + curr.getId().toString();
				LogitLink l = this.pathTree.get(key);
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
}
