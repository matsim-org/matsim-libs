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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import org.matsim.core.utils.collections.Tuple;
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
import playground.droeder.data.graph.comparison.EdgeCompare;
import playground.droeder.data.graph.comparison.NodeCompare;
import playground.droeder.data.graph.comparison.SegmentCompare;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 * based on http://www.fsutmsonline.net/images/uploads/reports/FDOT_BC353_21_rpt.pdf
 */
public class GraphMatching3 {
	private static final Logger log = Logger.getLogger(GraphMatching2.class);
	
	private MatchingGraph reference, candidate;

	private Double deltaDist;
	private Double deltaPhi;
	private Double maxRelLengthDiff;
	
	private Map<Id, Id> nodeRef2Cand;
	private Map<Id, Id> edgePreProcessRef2Cand;
	private Map<Id, Tuple<Id, Map<Id, Id>>> segmentRef2CandComplete;
	private Map<Id, Tuple<Id, Map<Id, Id>>> segmentRef2CandPartial;
	private Map<Id, Id> edgeRef2Cand;
	
	public String outDir;
	
	public GraphMatching3(MatchingGraph reference, MatchingGraph candidate, 
			Double deltaDist, Double deltaPhi, Double relLenghtDiff){
		this.reference = reference;
		this.candidate = candidate;
		this.deltaDist = deltaDist;
		this.deltaPhi = deltaPhi;
		this.maxRelLengthDiff = relLenghtDiff;
		this.nodeRef2Cand = new HashMap<Id, Id>();
		this.edgePreProcessRef2Cand = new HashMap<Id, Id>();
		this.edgeRef2Cand = new HashMap<Id, Id>();

		//<refEdgeId, <CandEdgeId, <refSegmentId, candSegmentId>>>
		this.segmentRef2CandComplete = new HashMap<Id, Tuple<Id,Map<Id, Id>>>();
		this.segmentRef2CandPartial = new HashMap<Id, Tuple<Id,Map<Id, Id>>>();
	}
	
	public void run(String outDir){
		this.outDir = outDir;
		log.info("starting bottom-up-matching...");
		this.bottomUpNodeMatching();
		this.bottomUpEdgePreProcessing();
		this.bottomUpSegmentMatching();
		this.bottomUpEdgeMatching();
		log.info("finished bottom-up-matching...");
		log.info("starting top-down-matching...");
		this.topDownEdgeMatching();
//		this.topDownSegmentMatching();
		this.topDownNodeMatching();
		log.info("finished top down matching...");
	}


	/**
	 * 
	 */
	private void bottomUpNodeMatching() {
		log.info("\tstarting node-Matching...");

		List<NodeCompare> compare;
		NodeCompare nc;
		int cnt = 0, msg = 1;
		
		// iterate over all nodes from the referenceGraph
		for(MatchingNode n : this.reference.getNodes().values()){
			compare = new ArrayList<NodeCompare>();
			// iterate over all nodes from the candidateGraph in the maximum distance
			for(MatchingNode c: this.candidate.getNearestNodes(n.getCoord().getX(), n.getCoord().getY(), this.deltaDist)){
				//compare the nodes
				nc = new NodeCompare(n, c);
				// add the node if the angle of the outgoing links is smaller then deltaPhi
				if(nc.getPhi() < this.deltaPhi){
					nc.setScore(deltaPhi);
					compare.add(nc);
				}
			}
			// map the best scored node from the candidateGraph to the referenceNode
			if(compare.size() > 0){
				Collections.sort(compare);
				Iterator<NodeCompare> it = compare.iterator();
				while(it.hasNext()){
					nc = it.next();
					// map the node if the candNode is not matched to any other refNode and go on
					if(!this.nodeRef2Cand.containsValue(nc.getCandId())){
						this.nodeRef2Cand.put(n.getId(), nc.getCandId());
						break;
					}
				}
			}
			cnt++;
			if(cnt%msg==0){
				msg*=2;
				log.info("\t\tprocessed " + cnt + " of " + this.reference.getNodes().size() + " nodes from the reference-graph...");
			}
			
		}
		this.nodes2Shp(this.nodeRef2Cand, this.outDir + "nodes_bottom_up.shp", "nodesBottomUp");
		log.info("\tfinished node-matching. " + 
				this.nodeRef2Cand.size() + " of " + this.reference.getNodes().size() 
				+ " nodes from the referenceGraph got a match...");
	}

	/**
	 * 
	 */
	private void bottomUpEdgePreProcessing() {
		log.info("\tstarting edge-preprocessing from nodes...");
		int cnt = 0, msg = 1;
		
		Id fromRef, toRef, fromCand, toCand;
		//iterate over all edges
		for(MatchingEdge e: this.reference.getEdges().values()){
			fromRef = e.getFromNode().getId();
			toRef = e.getToNode().getId();
			//if from and to-node are mapped check for fitting edges in the candidate-graph
			if(this.nodeRef2Cand.containsKey(fromRef) && this.nodeRef2Cand.containsKey(toRef)){
				fromCand = this.nodeRef2Cand.get(fromRef);
				toCand = this.nodeRef2Cand.get(toRef);
				//iterate over all outgoing edges from the candidate-from-node
				for(MatchingEdge c: this.candidate.getNodes().get(fromCand).getOutEdges()){
					// map it if the toNode is also a match
					if(c.getToNode().getId().equals(toCand)){
						this.edgePreProcessRef2Cand.put(e.getId(), c.getId());
						break;
					}
				}
			}
			
			cnt++;
			if(cnt%msg==0){
				msg*=2;
				log.info("\t\tprocessed " + cnt + " of " + this.reference.getEdges().size() + " edges from the reference-graph...");
			}
		}
		this.edge2Shp(this.edgePreProcessRef2Cand, this.outDir + "edgePreProcess.shp", "edgePreProcess");
		log.info("\tfinished edge-preprocessing. " + 
				this.edgePreProcessRef2Cand.size() + " of " + this.reference.getEdges().size() + 
				" edges from the referenceGraph got possible match at the candidate Graph...");
	}

	/**
	 * 
	 */
	private void bottomUpSegmentMatching() {
		log.info("\tstarting segment-matching...");
		int cnt = 0, msg = 1;
		
		//referenceSegment 2 candidateSegments
		Map<Id, Id> matchedSegments;
		SegmentCompare comp = null;
		List<SegmentCompare> tempCompareList;
		Collection<Id> ref, cand;
		
		//iterate over all preprocessed edges
		for(Entry<Id, Id> pre : this.edgePreProcessRef2Cand.entrySet()){
			matchedSegments = new HashMap<Id, Id>();
			cand = new ArrayList<Id>(candidate.getEdges().get(pre.getValue()).getSegmentMap().keySet());
			for(MatchingSegment refSeg : reference.getEdges().get(pre.getKey()).getSegments()){
				tempCompareList = new ArrayList<SegmentCompare>();
				for(MatchingSegment candSeg: candidate.getEdges().get(pre.getValue()).getSegments()){
					comp = new SegmentCompare(refSeg, candSeg);
					if(comp.isMatched(this.deltaDist, this.deltaPhi, this.maxRelLengthDiff)){
						comp.setScore(((comp.getAvDist()/ this.deltaDist) + (comp.getDeltaAngle() / this.deltaPhi))*0.5);
						tempCompareList.add(comp);
					}
				}
				if(!tempCompareList.isEmpty()){
					Collections.sort(tempCompareList);
					comp = tempCompareList.get(0);
					if(cand.contains(comp.getCandId())){
						cand.remove(comp.getCandId());
						matchedSegments.put(comp.getRefId(), comp.getCandId());
					}else{
						break;
					}
				}
				
			}
			if(matchedSegments.size() == reference.getEdges().get(pre.getKey()).getSegments().size()){
				this.segmentRef2CandComplete.put(pre.getKey(), new Tuple<Id, Map<Id,Id>>(pre.getKey(), matchedSegments));
			}else if(matchedSegments.size() > 0){
				this.segmentRef2CandPartial.put(pre.getKey(), new Tuple<Id, Map<Id,Id>>(pre.getKey(), matchedSegments));
			}
			cnt++;
			if(cnt%msg==0){
				msg*=2;
				log.info("\t\tprocessed " + cnt + " of " + this.edgePreProcessRef2Cand.size() + " prematched edges...");
			}
		}
		
		log.info("\tfinished segment-matching. " + 
				this.segmentRef2CandComplete.size() + " of " + this.edgePreProcessRef2Cand.size() + 
				" prematched edges from the referenceGraph got a match for all or some of their segments...");
	}


	/**
	 * 
	 */
	private void bottomUpEdgeMatching() {
		log.info("\tstarting edge-matching...");
		
		EdgeCompare comp;
		int cnt = 0, msg = 1;
		
		for(Entry<Id, Tuple<Id, Map<Id, Id>>> e: this.segmentRef2CandComplete.entrySet()){
			
		}
		
		
//		for(Entry<Id, Id> preMapped : edgePreProcessRef2Cand.entrySet()){
//			
//			comp = new EdgeCompare(reference.getEdges().get(preMapped.getKey()), candidate.getEdges().get(preMapped.getValue()));
//			/*
//			 * TODO don't know why so many prematches are deleted here!!!
//			 *		there must be a bug in the EdgeCompare, where the segments are compared
//			 */
//			if(comp.isMatched(this.deltaDist, this.deltaPhi, this.maxRelLengthDiff)){
//				edgeRef2Cand.put(preMapped.getKey(), preMapped.getValue());
//			}else{
//				//TODO just for debbuging
////				System.out.println(comp.toString());
//			}
//			cnt++;
//			if(cnt%msg==0){
//				msg*=2;
//				log.info("\t\tprocessed " + cnt + " of " + this.edgePreProcessRef2Cand.size() + " prematched edges from the reference-Graph...");
//			}
//			
//		}
//		this.edge2Shp(this.edgeRef2Cand, this.outDir + "edgeBottomUp.shp", "edgeBottomUp");
//		log.info("\tfinished edge-matching. " + 
//				this.edgeRef2Cand.size() + " of " + this.reference.getEdges().size() + 
//				" edges from the referenceGraph got a match...");
	}

	/**
	 * 
	 */
	private void topDownEdgeMatching() {
		log.info("\tstarting edge-matching...");
		int cnt = 0, msg = 1;
		
		MatchingNode refFrom;
		EdgeCompare comp;
		List<EdgeCompare> temp;
		for(MatchingEdge ref : reference.getEdges().values()){
			temp = new ArrayList<EdgeCompare>();
			cnt++;
			if(cnt%msg==0){
				msg*=2;
				log.info("\t\tprocessed " + cnt + " of " + this.reference.getEdges().size() + " edges from the reference-Graph...");
			}
			
			// only try to find a match, if there is not one yet
			if(this.edgeRef2Cand.containsKey(ref.getId())) continue;
			refFrom = ref.getFromNode();
			
			// find nearest nodes from the candidate Graph
			for(MatchingNode n: candidate.getNearestNodes(refFrom.getCoord().getX(), refFrom.getCoord().getY(), this.deltaDist)){
				// iterate over the outgoing edges
				for(MatchingEdge cand : n.getOutEdges()){
					comp = new EdgeCompare(ref, cand);
					// TODO to many matches are deleted here... probably there is a mistake in EdgeCompare
					if(comp.isMatched(this.deltaDist, this.deltaPhi, this.maxRelLengthDiff)){
						comp.setScore(comp.getAvDist()/this.deltaDist);
						temp.add(comp);
					}
				}
			}
			if(!temp.isEmpty()){
				Collections.sort(temp);
				for(EdgeCompare e: temp){
					if(!edgeRef2Cand.containsKey(e.getRefId())){
						this.edgeRef2Cand.put(e.getRefId(), e.getCandId());
						break;
					}
				}
			}
		}
		this.edge2Shp(this.edgeRef2Cand, this.outDir + "edgeTopDown.shp", "edgeTopDown");
		log.info("\tfinished edge-matching. " + 
				this.edgeRef2Cand.size() + " of " + this.reference.getEdges().size() + 
				" edges from the referenceGraph got a match...");
		
	}

//	/**
//	 * 
//	 */
//	private void topDownSegmentMatching() {
//		
//	}

	/**
	 * 
	 */
	private void topDownNodeMatching() {
		log.info("\tstarting node-Matching...");
		int cnt = 0, msg = 1;
		
		MatchingEdge ref, cand;
		for(Entry<Id, Id> e: this.edgeRef2Cand.entrySet()){
			ref = this.reference.getEdges().get(e.getKey());
			cand = this.candidate.getEdges().get(e.getValue());
			if(!this.nodeRef2Cand.containsKey(ref.getFromNode().getId())){
				this.nodeRef2Cand.put(ref.getFromNode().getId(), cand.getFromNode().getId());
			}
			
			if(!this.nodeRef2Cand.containsKey(ref.getToNode().getId())){
				this.nodeRef2Cand.put(ref.getToNode().getId(), cand.getToNode().getId());
			}
			
			cnt++;
			if(cnt%msg==0){
				msg*=2;
				log.info("\t\tprocessed " + cnt + " of " + this.edgeRef2Cand.size() + " mapped edges from the reference-Graph...");
			}
		}
		this.nodes2Shp(this.nodeRef2Cand, this.outDir + "nodes_top_down.shp", "nodesTopDown");
		log.info("\tfinished node-matching. " + 
				this.nodeRef2Cand.size() + " of " + this.reference.getNodes().size() 
				+ " nodes from the referenceGraph got a match...");
	}
	
	private void nodes2Shp(Map<Id, Id> nodeIds, String outFile, String name){
		Map<String, Coord> ref = new HashMap<String, Coord>();
		
		Map<String, SortedMap<String, String>> attrib = new HashMap<String, SortedMap<String,String>>();
		
		MatchingNode refNode, matchNode;
		int matched = 0;
		for(Entry<Id, Id> e: nodeIds.entrySet()){
			refNode = this.reference.getNodes().get(e.getKey());
			matchNode = this.candidate.getNodes().get(e.getValue());
			
			ref.put("ref_" + String.valueOf(matched) + "_" + refNode.getId().toString(), refNode.getCoord());
			ref.put("match_" + String.valueOf(matched) + "_" + matchNode.getId().toString(), matchNode.getCoord());
			
			SortedMap<String, String> temp = new TreeMap<String, String>();
			temp.put("match_nr", String.valueOf(matched));
			attrib.put("ref_" + String.valueOf(matched) + "_" + refNode.getId().toString(), temp);
			attrib.put("match_" + String.valueOf(matched) + "_" + matchNode.getId().toString(), temp);
			matched++;
		}
		
		DaShapeWriter.writeDefaultPoints2Shape(outFile, name, ref, attrib);
	}
	
	private void edge2Shp(Map<Id, Id> edgeIds, String outFile, String name){
		Map<String, SortedMap<Integer, Coord>> edges = new HashMap<String, SortedMap<Integer,Coord>>();
		Map<String, SortedMap<String, String>> attribs = new HashMap<String, SortedMap<String,String>>();
		
		SortedMap<Integer, Coord> coords;
		SortedMap<String, String> attribValues;
		
		MatchingEdge ref, cand;
		String refId, candId;
		int cnt;
		int matchNr = 0;
		
		for(Entry<Id, Id> e: edgeIds.entrySet()){
			attribValues = new TreeMap<String, String>();
			attribValues.put("matchNr", String.valueOf(matchNr));

			coords = new TreeMap<Integer, Coord>();
			ref = reference.getEdges().get(e.getKey());
			refId = String.valueOf(matchNr) + "_" + "ref_" + ref.getId().toString();
			cnt = 0;
			for(MatchingSegment s: ref.getSegments()){
				coords.put(cnt, s.getStart());
				cnt++;
				coords.put(cnt, s.getEnd());
				cnt++;
			}
			edges.put(refId, coords);
			attribs.put(refId, attribValues);
			
			cand = candidate.getEdges().get(e.getValue());
			candId = String.valueOf(matchNr) + "_" + "cand_" + cand.getId().toString();
			cnt = 0;
			coords =  new TreeMap<Integer, Coord>();
			for(MatchingSegment s: cand.getSegments()){
				coords.put(cnt, s.getStart());
				cnt++;
				coords.put(cnt, s.getEnd());
				cnt++;
			}
			edges.put(candId, coords);
			attribs.put(candId, attribValues);
			
			matchNr++;
		}
		
		DaShapeWriter.writeDefaultLineString2Shape(outFile, name, edges, attribs);
	}
	
//	public static void main(String[] args){
//		MatchingGraph ref = new MatchingGraph();
//		MatchingNode r1, r11, r2, r3;
//		
//		r1 = new MatchingNode(new IdImpl("r1"), new CoordImpl(0, 0));
//		r11 = new MatchingNode(new IdImpl("r11"), new CoordImpl(0, 0));
//		r2 = new MatchingNode(new IdImpl("r2"), new CoordImpl(0, 100));
//		r3 = new MatchingNode(new IdImpl("r3"), new CoordImpl(0,200));
//		ref.addNode(r1);
//		ref.addNode(r11);
//		ref.addNode(r2);
//		ref.addNode(r3);
//		
//		ref.addEdge(new MatchingEdge(new IdImpl("re12"), r1, r2));
//		ref.addEdge(new MatchingEdge(new IdImpl("re112"), r11, r2));
//		ref.addEdge(new MatchingEdge(new IdImpl("re23"), r2, r3));
//		ref.addEdge(new MatchingEdge(new IdImpl("re32"), r3, r2));
//		ref.addEdge(new MatchingEdge(new IdImpl("re21"), r2, r1));
//		
//		MatchingGraph cand = new MatchingGraph();
//		final MatchingNode m1, m2, m3, m4, m5, m6;
//		m1 = new MatchingNode(new IdImpl("m1"), new CoordImpl(1,-1));
//		m2 = new MatchingNode(new IdImpl("m2"), new CoordImpl(100,100));
//		m3 = new MatchingNode(new IdImpl("m3"), new CoordImpl(1,201));
//		m4 = new MatchingNode(new IdImpl("m4"), new CoordImpl(-10,-1));
//		m5 = new MatchingNode(new IdImpl("m5"), new CoordImpl(-11,100));
//		m6 = new MatchingNode(new IdImpl("m6"), new CoordImpl(-10,202));
//		cand.addNode(m1);
//		cand.addNode(m2);
//		cand.addNode(m3);
//		cand.addNode(m4);
//		cand.addNode(m5);
//		cand.addNode(m6);
//		
//		MatchingEdge me12 = new MatchingEdge(new IdImpl("me12"), m1, m2);
//	//	me12.addShapePointsAndCreateSegments(new ArrayList<Coord>(){{
//	//		add(m1.getCoord());
//	//		add(new CoordImpl(5,50));
//	//		add(m2.getCoord());
//	//	}});
//		cand.addEdge(me12);
//		cand.addEdge(new MatchingEdge(new IdImpl("me23"), m2, m3));
//		cand.addEdge(new MatchingEdge(new IdImpl("me32"), m3, m2));
//		cand.addEdge(new MatchingEdge(new IdImpl("me21"), m2, m1));
//		
//		MatchingEdge me45 = new MatchingEdge(new IdImpl("me45"), m4, m5);
//		me45.addShapePointsAndCreateSegments(new ArrayList<Coord>(){{
//			add(m4.getCoord());
//			add(new CoordImpl(5,25));
//			add(new CoordImpl(-5,50));
//			add(m5.getCoord());
//		}});
//		cand.addEdge(me45);
//		cand.addEdge(new MatchingEdge(new IdImpl("me56"), m5, m6));
//		cand.addEdge(new MatchingEdge(new IdImpl("me65"), m6, m5));
//		cand.addEdge(new MatchingEdge(new IdImpl("me54"), m5, m4));
//		
//		
//		final String OUT = DaPaths.OUTPUT + "geoAlgorithm/";
//		GraphMatching2 gm = new GraphMatching2(ref, cand, 25.0, Math.PI/8, 0.1);
//		gm.run();
//	}
	
	public static void main(String[] args){
		final String PATH = DaPaths.OUTPUT + "bvg09/";
		final String OUT = DaPaths.OUTPUT + "geoAlgorithm/";
		final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
		final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
		

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
			
			if(temp.equals("P") || temp.equals("S") || temp.equals("R") || temp.equals("V") || temp.equals("N")) continue;
			
			for(TransitRoute route: line.getRoutes().values()){
				facs = new ArrayList<TransitStopFacility>();
				shape = new ArrayList<Coord>();
				for(TransitRouteStop stop : route.getStops()){
					facs.add(stop.getStopFacility());
					shape.add(stop.getStopFacility().getCoord());
				}
				if(facs.size() < 2){
					log.error("can not create an edge for TransitRoute " + route.getId() + " on TransitLine " +
							line.getId() + " beacause it have less than 2 stops!");
					continue;
				}
				
				// create or get start-node
				if(v.getNodes().containsKey(facs.get(0).getId())){
					start = v.getNodes().get(facs.get(0).getId());
				}else{
					start = new MatchingNode(facs.get(0).getId(), facs.get(0).getCoord());
					v.addNode(start);
				}

				// create or get end-node
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
		
		
		for(TransitLine line: hafasSc.getTransitSchedule().getTransitLines().values()){
			for(TransitRoute route: line.getRoutes().values()){
				facs = new ArrayList<TransitStopFacility>();
				shape = new ArrayList<Coord>();
				for(TransitRouteStop stop : route.getStops()){
					facs.add(stop.getStopFacility());
					shape.add(stop.getStopFacility().getCoord());
				}
				if(facs.size() < 2){
					log.error("can not create an edge for TransitRoute " + route.getId() + " on TransitLine " +
							line.getId() + " beacause it have less than 2 stops!");
					continue;
				}
				
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
				
				e = new MatchingEdge(new IdImpl(line.getId() + "_" + route.getId()), 
						h.getNodes().get(facs.get(0).getId()), h.getNodes().get(facs.get(facs.size()-1).getId()));
				e.addShapePointsAndCreateSegments(shape);
				h.addEdge(e);
			}
		}
		
		GraphMatching3 gm = new GraphMatching3(v, h, 500.0, Math.PI/6, 0.1);
		gm.run(OUT);
	}
}
