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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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
import playground.droeder.data.graph.MatchingGraph;
import playground.droeder.data.graph.MatchingNode;
import playground.droeder.data.graph.MatchingSegment;
import playground.droeder.data.graph.comparison.NodeCompare;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 * based on http://www.fsutmsonline.net/images/uploads/reports/FDOT_BC353_21_rpt.pdf
 */
public class GraphMatching {
	private static final Logger log = Logger.getLogger(GraphMatching.class);
	
	private MatchingGraph reference;
	private MatchingGraph matching;

	private Double deltaDist;
	private Double deltaPhi;
	
	public GraphMatching(MatchingGraph reference, MatchingGraph matching){
		this.reference = reference;
		this.matching = matching;
		this.deltaDist = Double.MAX_VALUE;
		this.deltaPhi = Double.MAX_VALUE;
	}
	
	public void setMaxDist(Double maxDeltaDist){
		this.deltaDist = maxDeltaDist;
	}
	
	public void setMaxAngle(Double maxDeltaPhi){
		this.deltaPhi = maxDeltaPhi;
	}
	
	public void bottomUpMatching() {
		log.info("starting bottom-up-Matching...");
		this.nodeMatching();
//		this.segmentMatching();
//		this.edgeMatching();
		log.info("bottom-up-matching finished...");
	}


	// ###### NODEMATCHING #######
	private Map<Id, List<NodeCompare>> nodeReference2match;
	private List<Id> unmatchedRefNodes;


	private void nodeMatching() {
		log.info("start matching Nodes...");
		this.nodeReference2match = new HashMap<Id, List<NodeCompare>>();
		this.unmatchedRefNodes = new ArrayList<Id>();
		
		List<NodeCompare> candidates;
		NodeCompare comp;

		// iterate over all nodes
		for(MatchingNode ref: this.reference.getNodes().values()){
			candidates = new ArrayList<NodeCompare>();
			//iterate over all possible candidates
			if(ref.getId().equals(new IdImpl(503013))){
				System.out.println();
			}
			for(MatchingNode match: this.matching.getNearestNodes(ref.getCoord().getX(), ref.getCoord().getY(), this.deltaDist)){
				comp = new NodeCompare(ref, match);
				if(ref.getId().equals(new IdImpl(9050301))){
					System.out.println();
				}
				if((comp.getDist() < this.deltaDist) && (comp.getPhi() < this.deltaPhi)){
					comp.setScore(((comp.getDist() / this.deltaDist) + (comp.getPhi() / this.deltaPhi)) / 2);
					candidates.add(comp);
				}
			}
			
			
			if(candidates.size() > 0){
				Collections.sort(candidates);
				this.nodeReference2match.put(ref.getId(), candidates);
			}else{
				this.unmatchedRefNodes.add(ref.getId());
			}
			
		}
		log.info(this.unmatchedRefNodes.size() +" nodes are unmatched!");
		log.info(this.nodeReference2match.size() + " of " + reference.getNodes().size() + " nodes are matched");
		log.info("node-matching finished... ");
	}

	// ##### SEGMENTMATCHING #####
	private Map<MatchingEdge, List<MatchingEdge>> ref2CandEdgesFromMappedNodes;
	private Map<MatchingEdge, List<MatchingEdge>> ref2CandEdgesFromSegmentMatching;

	private void segmentMatching() {
		log.info("start segment matching...");
		this.computeEdgeCandidatesFromNodes();
		
		List<MatchingEdge> tempCand = null;
		this.ref2CandEdgesFromSegmentMatching = new HashMap<MatchingEdge, List<MatchingEdge>>();
		
		//iterate over all candidate edges 
		for(Entry<MatchingEdge, List<MatchingEdge>> e: this.ref2CandEdgesFromMappedNodes.entrySet()){
			// with all algorithms
			
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
		this.ref2CandEdgesFromMappedNodes = new HashMap<MatchingEdge, List<MatchingEdge>>();
		Id candFrom, candTo, refFrom, refTo;
		List<MatchingEdge> tempCandidates;
		
		//iterate over all edges
		for(MatchingEdge ref : this.reference.getEdges().values()){
			tempCandidates = new ArrayList<MatchingEdge>();
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
						tempCandidates.add(cand);
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
	
	public Map<Id, List<NodeCompare>> getNodeIdRef2Match(){
		return this.nodeReference2match;
	}
	
	
	//####### results 2 shape ########
	public void nodes2Shape(String outPath){
		Map<String, Coord> ref;
		ref = new HashMap<String, Coord>();
		
		Map<String, SortedMap<String, String>> attrib = new HashMap<String, SortedMap<String,String>>();
		
		MatchingNode refNode, matchNode;
		int matched = 0;
		for(Entry<Id, List<NodeCompare>> e: this.nodeReference2match.entrySet()){
			refNode = this.reference.getNodes().get(e.getKey());
			matchNode = this.matching.getNodes().get(e.getValue().get(0).getCompId());
			
			ref.put("ref_" + String.valueOf(matched) + "_" + refNode.getId().toString(), refNode.getCoord());
			ref.put("match_" + String.valueOf(matched) + "_" + matchNode.getId().toString(), matchNode.getCoord());
			
			SortedMap<String, String> temp = new TreeMap<String, String>();
			temp.put("match_nr", String.valueOf(matched));
			attrib.put("ref_" + String.valueOf(matched) + "_" + refNode.getId().toString(), temp);
			attrib.put("match_" + String.valueOf(matched) + "_" + matchNode.getId().toString(), temp);
			matched++;
		}
		
		DaShapeWriter.writeDefaultPoints2Shape(outPath + "matched.shp", "ref", ref, attrib);
		
		Map<String, Coord> unmatched = new HashMap<String, Coord>();
		
		for(Id id : unmatchedRefNodes){
			unmatched.put(id.toString(), this.reference.getNodes().get(id).getCoord());
		}
		
		DaShapeWriter.writeDefaultPoints2Shape(outPath + "unmatched.shp", "unmatched", unmatched, null);
	}
	
	public void edges2Shape(String outpath){
		Map<String, SortedMap<Integer, Coord>> edges = new HashMap<String, SortedMap<Integer,Coord>>();
		SortedMap<Integer, Coord> temp;
		int i;
		for(Entry<Id, MatchingEdge> e: this.reference.getEdges().entrySet()){
			temp = new TreeMap<Integer, Coord>();
			i = 0;
			for(MatchingSegment s: e.getValue().getSegments()){
				temp.put(i, s.getStart());
				temp.put(i++, s.getEnd());
			}
			edges.put(e.getKey().toString(), temp);
		}
		DaShapeWriter.writeDefaultLineString2Shape(outpath + "refGraphSegments.shp", "refGraphSegments", edges, null);

		edges = new HashMap<String, SortedMap<Integer,Coord>>();
		for(Entry<Id, MatchingEdge> e: this.matching.getEdges().entrySet()){
			temp = new TreeMap<Integer, Coord>();
			i = 0;
			for(MatchingSegment s: e.getValue().getSegments()){
				temp.put(i, s.getStart());
				temp.put(i++, s.getEnd());
			}
			edges.put(e.getKey().toString(), temp);
		}
		DaShapeWriter.writeDefaultLineString2Shape(outpath + "matchingGraphSegments.shp", "matchingGraphSegments", edges, null);
	}
	
	

	public static void main(String[] args){
		final String PATH = DaPaths.OUTPUT + "bvg09/";
		final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
		final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
		final String OUT = DaPaths.OUTPUT + "geoAlgorithm/";
		

		TransitStopFacility fac = null;
		MatchingEdge e;
		MatchingNode start, end;
		
		ScenarioImpl visumSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		visumSc.getConfig().scenario().setUseTransit(true);
		TransitScheduleReader reader = new TransitScheduleReader(visumSc);
		reader.readFile(VISUMTRANSITFILE);
		MatchingGraph v = new MatchingGraph();
		List<TransitStopFacility> facs;
		ArrayList<Coord> shape;
		
		String temp;
		for(TransitLine line: visumSc.getTransitSchedule().getTransitLines().values()){
			temp = line.getId().toString().substring(0, 1);
			
			if(temp.equals("P") || temp.equals("S") || temp.equals("R") || temp.equals("V")) continue;
			
			for(TransitRoute route: line.getRoutes().values()){
				facs = new ArrayList<TransitStopFacility>();
				shape = new ArrayList<Coord>();
				for(TransitRouteStop stop : route.getStops()){
					facs.add(stop.getStopFacility());
					shape.add(stop.getStopFacility().getCoord());
				}
				if(facs.size() < 2) continue;
				
				if(v.getNodes().containsKey(facs.get(0).getId())){
					start = v.getNodes().get(facs.get(0).getId());
				}else{
					start = new MatchingNode(facs.get(0).getId(), facs.get(0).getCoord());
					v.addNode(start);
				}
				
				if(v.getNodes().containsKey(facs.get(facs.size()-1).getId())){
					end = v.getNodes().get(facs.get(facs.size()-1).getId());
				}else{
					end = new MatchingNode(facs.get(facs.size()-1).getId(), facs.get(facs.size()-1).getCoord());
					v.addNode(end);
				}
				
				e = new MatchingEdge(route.getId(), start, end);
				e.addShapePointsAndCreateSegments(shape);
				v.addEdge(e);
			}
		}
		
		ScenarioImpl hafasSc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		hafasSc.getConfig().scenario().setUseTransit(true);
		TransitScheduleReader reader2 = new TransitScheduleReader(hafasSc);
		reader2.readFile(HAFASTRANSITFILE);
		MatchingGraph h = new MatchingGraph();
		
		
		fac = null;
		for(TransitLine line: hafasSc.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute route: line.getRoutes().values()){
				facs = new ArrayList<TransitStopFacility>();
				shape = new ArrayList<Coord>();
				for(TransitRouteStop stop : route.getStops()){
					facs.add(stop.getStopFacility());
					shape.add(stop.getStopFacility().getCoord());
				}
				if(facs.size() < 2) continue;
				
				if(h.getNodes().containsKey(facs.get(0).getId())){
					start = h.getNodes().get(facs.get(0).getId());
				}else{
					start = new MatchingNode(facs.get(0).getId(), facs.get(0).getCoord());
					h.addNode(start);
				}
				
				if(h.getNodes().containsKey(facs.get(facs.size()-1).getId())){
					end = h.getNodes().get(facs.get(facs.size()-1).getId());
				}else{
					end = new MatchingNode(facs.get(facs.size()-1).getId(), facs.get(facs.size()-1).getCoord());
					h.addNode(end);
				}
				
				e = new MatchingEdge(route.getId(), h.getNodes().get(facs.get(0).getId()), h.getNodes().get(facs.get(facs.size()-1).getId()));
				e.addShapePointsAndCreateSegments(shape);
				h.addEdge(e);
			}
		}
		
		GraphMatching gm = new GraphMatching(v, h);
		gm.setMaxAngle(Math.PI / 4);
		gm.setMaxDist(500.0);
		gm.bottomUpMatching();
		gm.nodes2Shape(OUT);
		gm.edges2Shape(OUT);
//		for(Entry<Id, Id> e : r.getNodeRef2Match().entrySet()){
//			System.out.println(e.getKey() + " " + e.getValue());
//		}
	}
}

