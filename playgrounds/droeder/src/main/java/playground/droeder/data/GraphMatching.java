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
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.droeder.DaPaths;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingEdgeCandidate;
import playground.droeder.data.graph.MatchingGraph;
import playground.droeder.data.graph.MatchingNode;
import playground.droeder.data.graph.algorithms.NodeAngleAlgo;
import playground.droeder.data.graph.algorithms.NodeDistAlgo;
import playground.droeder.data.graph.algorithms.SegmentAlgorithmImpl;
import playground.droeder.data.graph.algorithms.interfaces.EdgeAlgorithm;
import playground.droeder.data.graph.algorithms.interfaces.MatchingAlgorithm;
import playground.droeder.data.graph.algorithms.interfaces.NodeAlgorithm;
import playground.droeder.data.graph.algorithms.interfaces.SegmentAlgorithm;

/**
 * @author droeder
 * based on http://www.fsutmsonline.net/images/uploads/reports/FDOT_BC353_21_rpt.pdf
 */
public class GraphMatching {
	private static final Logger log = Logger.getLogger(GraphMatching.class);
	
	private MatchingGraph reference;
	private MatchingGraph matching;
	
	private List<EdgeAlgorithm>  edgeAlgos;
	private List<NodeAlgorithm>  nodeAlgos;
	private List<SegmentAlgorithm>  segmentAlgos;
	
	public GraphMatching(MatchingGraph reference, MatchingGraph matching){
		this.reference = reference;
		this.matching = matching;
		this.initDefaultAlgos();
	}
	
	private void initDefaultAlgos() {
		log.info("add default algorithms...");
		
		//node
		this.nodeAlgos = new ArrayList<NodeAlgorithm>();
		this.addAlgo(new NodeDistAlgo(500.0, this.matching));
		this.addAlgo(new NodeAngleAlgo(Math.PI / 2));
		
		//segment
		this.segmentAlgos = new ArrayList<SegmentAlgorithm>();
		this.addAlgo(new SegmentAlgorithmImpl(0.0)) ;

		//edge
		this.edgeAlgos = new ArrayList<EdgeAlgorithm>();
	}

	/**
	 * 
	 */
	public void topDownMatching() {
		log.info("starting top-down-Matching...");
		this.nodeMatching();
		this.segmentMatching();
//		this.edgeMatching();
		log.info("top-down-matching finished...");
	}


	// ###### NODEMATCHING #######
	private Map<Id, Id> nodeReference2match;
	private List<Id> unmatchedRefNodes;

	private void nodeMatching() {
		log.info("start matching Nodes...");
		this.nodeReference2match = new HashMap<Id, Id>();
		this.unmatchedRefNodes = new ArrayList<Id>();
		
		// some info
		log.info("running node-algorithms for every node in the following order:");
		for(NodeAlgorithm a: this.nodeAlgos){
			log.info(a.getClass().getName());
		}
		
		List<MatchingNode> candidates = new ArrayList<MatchingNode>(this.matching.getNodes().values()); 

		// iterate over all nodes
		for(MatchingNode refNode: this.reference.getNodes().values()){
			// with every algorithm
			for(NodeAlgorithm a: this.nodeAlgos){
				candidates = a.run(refNode, candidates);
			}
			if(candidates.size() > 0){
				// if there is one or more candidate-Node take the first
				this.nodeReference2match.put(refNode.getId(), candidates.get(0).getId());
			}else{
				// else, add the refNode to unmatchedList
				this.unmatchedRefNodes.add(refNode.getId());
			}
		}
		log.info(this.nodeReference2match.size() + " of " + reference.getNodes().size() + " nodes are matched");
		log.info("node-matching finished... ");
	}

	// ##### SEGMENTMATCHING #####
	private Map<MatchingEdge, List<MatchingEdgeCandidate>> ref2CandEdgesFromMappedNodes;
	private Map<MatchingEdge, List<MatchingEdgeCandidate>> ref2CandEdgesFromSegmentMatching;

	private void segmentMatching() {
		log.info("start segment matching...");
		this.computeEdgeCandidatesFromNodes();
		
		log.info("running segment-algorithms for every node in the following order:");
		for(SegmentAlgorithm a: this.segmentAlgos){
			log.info(a.getClass().getName());
		}
		List<MatchingEdgeCandidate> tempCand = null;
		this.ref2CandEdgesFromSegmentMatching = new HashMap<MatchingEdge, List<MatchingEdgeCandidate>>();
		
		//iterate over all candidate edges 
		for(Entry<MatchingEdge, List<MatchingEdgeCandidate>> e: this.ref2CandEdgesFromMappedNodes.entrySet()){
			// with all algorithms
			for(SegmentAlgorithm a : this.segmentAlgos){
				tempCand = a.run(e.getKey(), e.getValue());
			}
			
			// if there is at least one candidate, store
			if(tempCand.size() > 0){
				this.ref2CandEdgesFromSegmentMatching.put(e.getKey(), tempCand);
			}
		}
		
		// clear temporally matched Edges to save memory 
		this.ref2CandEdgesFromMappedNodes.clear();
		log.info("segment matching finished...");
	}
	
	private void computeEdgeCandidatesFromNodes() {
		log.info("compute candidate edges from mapped nodes");
		this.ref2CandEdgesFromMappedNodes = new HashMap<MatchingEdge, List<MatchingEdgeCandidate>>();
		Id candFrom, candTo, refFrom, refTo;
		List<MatchingEdgeCandidate> tempCandidates;
		
		//iterate over all edges
		for(MatchingEdge ref : this.reference.getEdges().values()){
			tempCandidates = new ArrayList<MatchingEdgeCandidate>();
			refFrom = ref.getFromNode().getId();
			refTo = ref.getToNode().getId();

			// if the matched nodes contain the start- and end-node, go on
			if(this.nodeReference2match.containsKey(refFrom) && 
					this.nodeReference2match.containsKey(refTo)){
				// iterate over all edges going out from the candidateStartNode which is mapped to the referenceStartNode 
				for(MatchingEdge cand : this.matching.getNodes().get(this.nodeReference2match.get(refFrom)).getOutEdges()){
					candFrom = cand.getFromNode().getId();
					candTo = cand.getToNode().getId();

					// if the refNodes and candNodes where mapped in NodeMatching, store the candidateEdge  
					if(this.nodeReference2match.get(refFrom).equals(candFrom) && this.nodeReference2match.get(refTo).equals(candTo)){
						tempCandidates.add(new MatchingEdgeCandidate(cand));
					}
				}
			}
			
			if(tempCandidates.size() > 0){
				this.ref2CandEdgesFromMappedNodes.put(ref, tempCandidates);
			}
		}
		
		log.info(this.ref2CandEdgesFromMappedNodes.size() + " of " + reference.getEdges().size() + " edges from the reference-Graph are preMapped");
	}

//	// ##### EDGEMATCHING #####
//	private void edgeMatching() {
//		// TODO Auto-generated method stub
//		
//	}
	
	public Map<Id, Id> getNodeIdRef2Match(){
		return this.nodeReference2match;
	}
	
	/**
	 * add some own implementations of <code>MatchingAlgorithm</code>. The algorithms are called in the order they are added
	 * @param algo
	 */
	public void addAlgo(MatchingAlgorithm algo){
		if(algo instanceof EdgeAlgorithm){
			this.edgeAlgos.add((EdgeAlgorithm) algo);
			log.info(algo.getClass().getSimpleName() + " registered...");
		}else if(algo instanceof NodeAlgorithm){
			this.nodeAlgos.add((NodeAlgorithm) algo);
			log.info(algo.getClass().getSimpleName() + " registered...");
		}else if(algo instanceof SegmentAlgorithm){
			this.segmentAlgos.add((SegmentAlgorithm) algo);
			log.info(algo.getClass().getSimpleName() + " registered...");
		}
	}
	
	/**
	 * removes all registered algorithms
	 */
	public void clearAlgorithms(){
		this.nodeAlgos.clear();
		this.segmentAlgos.clear();
		this.edgeAlgos.clear();
	}

	public static void main(String[] args){
		final String PATH = DaPaths.OUTPUT + "bvg09/";
		final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
		final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
		
		ScenarioImpl visumSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		visumSc.getConfig().scenario().setUseTransit(true);
		TransitScheduleReader reader = new TransitScheduleReader(visumSc);
		reader.readFile(VISUMTRANSITFILE);
		MatchingGraph v = new MatchingGraph();
		
		for(TransitStopFacility stop : visumSc.getTransitSchedule().getFacilities().values()){
			v.addNode(new MatchingNode(stop.getId(), stop.getCoord()));
		}
		
		TransitStopFacility fac = null;
		int i = 0;
		for(TransitLine line: visumSc.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute route: line.getRoutes().values()){
				for(TransitRouteStop stop : route.getStops()){
					if(!(fac == null)){
						v.addEdge(new MatchingEdge(new IdImpl(i), v.getNodes().get(fac.getId()), v.getNodes().get(stop.getStopFacility().getId())));
					}
					fac = stop.getStopFacility();
					i++;
				}
			}
		}
		
		ScenarioImpl hafasSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		hafasSc.getConfig().scenario().setUseTransit(true);
		TransitScheduleReader reader2 = new TransitScheduleReader(hafasSc);
		reader2.readFile(HAFASTRANSITFILE);
		MatchingGraph h = new MatchingGraph();
		
		for(TransitStopFacility stop : hafasSc.getTransitSchedule().getFacilities().values()){
			h.addNode(new MatchingNode(stop.getId(), stop.getCoord()));
		}
		
		fac = null;
		i = 0;
		for(TransitLine line: hafasSc.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute route: line.getRoutes().values()){
				for(TransitRouteStop stop : route.getStops()){
					if(!(fac == null)){
						h.addEdge(new MatchingEdge(new IdImpl(i), h.getNodes().get(fac.getId()), h.getNodes().get(stop.getStopFacility().getId())));
					}
					fac = stop.getStopFacility();
					i++;
				}
			}
		}
		
		GraphMatching r = new GraphMatching(v, h);
		r.topDownMatching();
//		for(Entry<Id, Id> e : r.getNodeRef2Match().entrySet()){
//			System.out.println(e.getKey() + " " + e.getValue());
//		}
	}
}

