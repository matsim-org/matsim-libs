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
package playground.droeder.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.droeder.data.matching.MatchingGraph;
import playground.droeder.data.matching.MatchingNode;
import playground.droeder.data.matching.algorithms.NodeAngleAlgo;
import playground.droeder.data.matching.algorithms.NodeDistAlgo;

/**
 * @author droeder
 * based on http://www.fsutmsonline.net/images/uploads/reports/FDOT_BC353_21_rpt.pdf
 */
public class MatchingRunner {
	private static final Logger log = Logger.getLogger(MatchingRunner.class);
	
	private MatchingGraph reference;
	private MatchingGraph matching;
	
	public MatchingRunner(MatchingGraph reference, MatchingGraph matching){
		this.reference = reference;
		this.matching = matching;
	}
	
	/**
	 * 
	 */
	public void topDownMatching(Double deltaDist, Double deltaAngle) {
		log.info("starting Top-Down-Matching...");
		this.nodeMatching(deltaDist, deltaAngle);
//		this.segmentMatching();
//		this.edgeMatching();
	}

	// ###### NODEMATCHING #######
	private Map<Id, Id> nodeReference2match;
	private List<Id> unmatchedRefNodes;

	private void nodeMatching(Double deltaDist, Double deltaPhi) {
		log.info("start Matching Nodes...");
		this.nodeReference2match = new HashMap<Id, Id>();
		this.unmatchedRefNodes = new ArrayList<Id>();
		
		List<MatchingNode> candidates;
		
		for(MatchingNode refNode: this.reference.getNodes().values()){
			candidates = NodeDistAlgo.getNodesInDist(refNode, this.matching, deltaDist);
			candidates = NodeAngleAlgo.getMinAngleNodes(refNode, candidates, deltaPhi);
			
			if(candidates.size() > 0){
				this.nodeReference2match.put(refNode.getId(), candidates.get(0).getId());
			}else{
				this.unmatchedRefNodes.add(refNode.getId());
			}
			
		}
		
		log.info("matching nodes finished. " + this.nodeReference2match.size() + " of " 
				+ (this.nodeReference2match.size() + this.unmatchedRefNodes.size()) + " are matched...");
	}
	

//	// ##### SEGMENTMATCHING #####
//	private void segmentMatching() {
//		// TODO Auto-generated method stub
//		
//	}
//
//	// ##### EDGEMATCHING #####
//	private void edgeMatching() {
//		// TODO Auto-generated method stub
//		
//	}

}

