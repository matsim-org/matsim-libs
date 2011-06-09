/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.data.graph.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.vividsolutions.jts.index.bintree.NodeBase;

import playground.droeder.data.graph.GraphElement;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingNode;

/**
 * @author droeder
 *
 */
public class NodeAngleAlgo implements MatchingAlgorithm{
	
	private Class<MatchingNode> clazz;
	private Double deltaPhi;

	public NodeAngleAlgo(Double deltaPhi){
		this.clazz = MatchingNode.class;
		this.deltaPhi = deltaPhi;
	}
	
	@Override
	public List<? extends GraphElement> run(GraphElement ref,	List<? extends GraphElement> candidates) {
		double phi = Double.MAX_VALUE;
		double temp;
		List<MatchingNode> newCandidates = new ArrayList<MatchingNode>();

		//iterate over all candidateNodes
		for(GraphElement cand : candidates){
			
			//iterate over all edges outgoing from referenceNode
			for(MatchingEdge refEdge : ((MatchingNode) ref).getOutEdges()){
				//iterate over all edges outgoing from the candidateNode
				for(MatchingEdge candEdge : ((MatchingNode) cand).getOutEdges()){
					temp = getPhi(refEdge, candEdge);
					if(temp < phi){
						phi = temp;
					}
				}
			}
			// if the referenceNode and the candidateNode have any pair of links with a phi smaller than deltaPhi, the candidate is still a candidate
			if(phi<deltaPhi){
				newCandidates.add((MatchingNode) cand);
			}
			phi = Double.MAX_VALUE;
		}
		Collections.sort(newCandidates);

		return newCandidates;
	}
	
	private Double getPhi(MatchingEdge one, MatchingEdge two){
		double absOne, absTwo, scalar;
		Vector<Double> o = new Vector<Double>();
		Vector<Double> t = new Vector<Double>();
		
		o.add(0, one.getToNode().getCoord().getX() - one.getFromNode().getCoord().getX());
		o.add(1, one.getToNode().getCoord().getY() - one.getFromNode().getCoord().getY());
		
		t.add(0, two.getToNode().getCoord().getX() - two.getFromNode().getCoord().getX());
		t.add(1, two.getToNode().getCoord().getY() - two.getFromNode().getCoord().getY());
		
		absOne = Math.sqrt(Math.pow(o.get(0), 2) + Math.pow(o.get(1), 2));
		absTwo = Math.sqrt(Math.pow(t.get(0), 2) + Math.pow(t.get(1), 2));
		
		scalar = ((o.get(0)*t.get(0)) + (o.get(1)+ t.get(1)));
		
		return Math.acos(scalar/(absOne * absTwo));
	}

	@Override
	public Class<? extends GraphElement> getProcessingClass() {
		return this.clazz;
	}

}
