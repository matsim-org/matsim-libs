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
import java.util.List;

import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingEdgeCandidate;
import playground.droeder.data.graph.algorithms.interfaces.SegmentAlgorithm;

/**
 * @author droeder
 *
 */
public class SegmentAlgorithmImpl implements SegmentAlgorithm{
	
	private Double deltaDistMax;
	
	public SegmentAlgorithmImpl(Double deltaDistMax){
		this.deltaDistMax = deltaDistMax;
	}

	
	@Override
	public List<MatchingEdgeCandidate> run(MatchingEdge ref, List<MatchingEdgeCandidate> cands) {
		List<MatchingEdgeCandidate> newCands = new ArrayList<MatchingEdgeCandidate>();
		
		for(MatchingEdgeCandidate cand :  cands){
			cand.setRefEdgeId(ref.getId());
			cand.compareSegments2RefSegments(ref.getSegments());
		}
		return newCands;
	}
	
}


