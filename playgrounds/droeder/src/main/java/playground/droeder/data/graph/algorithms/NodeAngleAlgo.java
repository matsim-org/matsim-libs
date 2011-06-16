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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

import playground.droeder.GeoCalculator;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingNode;
import playground.droeder.data.graph.algorithms.interfaces.NodeAlgorithm;

/**
 * @author droeder
 *
 */
public class NodeAngleAlgo implements NodeAlgorithm{
	
	private Double deltaPhi;

	public NodeAngleAlgo(Double deltaPhi){
		this.deltaPhi = deltaPhi;
	}
	
	@Override
	public List<MatchingNode> run(MatchingNode ref,	List<MatchingNode> candidates) {
		double phi = Double.MAX_VALUE;
		double temp;
		List<MatchingNode> newCandidates = new ArrayList<MatchingNode>();

		//iterate over all candidateNodes
		for(MatchingNode cand : candidates){
			
			//iterate over all edges outgoing from referenceNode
			for(MatchingEdge refEdge : ref.getOutEdges()){
				//iterate over all edges outgoing from the candidateNode
				for(MatchingEdge candEdge : cand.getOutEdges()){
					temp = GeoCalculator.angleBeetween2Straights(new Tuple<Coord, Coord>(refEdge.getFromNode().getCoord(), refEdge.getToNode().getCoord()), 
							new Tuple<Coord, Coord>(candEdge.getFromNode().getCoord(), candEdge.getToNode().getCoord()));
					if(temp < phi){
						phi = temp;
					}
				}
			}
			// if the referenceNode and the candidateNode have any pair of links with a phi smaller than deltaPhi, the candidate is still a candidate
			if(phi<this.deltaPhi){
				newCandidates.add((MatchingNode) cand);
			}
			phi = Double.MAX_VALUE;
		}
		Collections.sort(newCandidates);

		return newCandidates;
	}

}
