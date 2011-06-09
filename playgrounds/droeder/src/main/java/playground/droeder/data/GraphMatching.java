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
import org.jfree.util.Log;
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
import playground.droeder.data.graph.GraphElement;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingGraph;
import playground.droeder.data.graph.MatchingNode;
import playground.droeder.data.graph.MatchingSegment;
import playground.droeder.data.graph.algorithms.MatchingAlgorithm;
import playground.droeder.data.graph.algorithms.NodeAngleAlgo;
import playground.droeder.data.graph.algorithms.NodeDistAlgo;

/**
 * @author droeder
 * based on http://www.fsutmsonline.net/images/uploads/reports/FDOT_BC353_21_rpt.pdf
 */
public class GraphMatching {
	private static final Logger log = Logger.getLogger(GraphMatching.class);
	
	private MatchingGraph reference;
	private MatchingGraph matching;
	
	private List<MatchingAlgorithm>  edgeAlgos;
	private List<MatchingAlgorithm>  nodeAlgos;
	private List<MatchingAlgorithm>  segmentAlgos;
	
	public GraphMatching(MatchingGraph reference, MatchingGraph matching){
		this.reference = reference;
		this.matching = matching;
		
		this.initDefaultAlgos();
	}
	
	private void initDefaultAlgos() {
		this.nodeAlgos = new ArrayList<MatchingAlgorithm>();
		this.nodeAlgos.add(new NodeDistAlgo(300.0, matching));
		this.nodeAlgos.add(new NodeAngleAlgo(Math.PI / 8));
		
		this.edgeAlgos = new ArrayList<MatchingAlgorithm>();
		this.segmentAlgos = new ArrayList<MatchingAlgorithm>();
	}

	/**
	 * 
	 */
	public void topDownMatching() {
		log.info("starting top-down-Matching...");
		this.nodeMatching();
//		this.segmentMatching();
//		this.edgeMatching();
	}

	// ###### NODEMATCHING #######
	private Map<Id, Id> nodeReference2match;
	private List<Id> unmatchedRefNodes;

	private void nodeMatching() {
		log.info("start matching Nodes...");
		this.nodeReference2match = new HashMap<Id, Id>();
		this.unmatchedRefNodes = new ArrayList<Id>();
		
		// some info
		log.info("running node-algorithms in following order for every node...");
		for(MatchingAlgorithm a: this.nodeAlgos){
			log.info(a.getClass().getSimpleName());
		}
		
		List<? extends GraphElement> candidates = new ArrayList<GraphElement>(this.matching.getNodes().values()); 

		// iterate over all nodes
		for(MatchingNode refNode: this.reference.getNodes().values()){
			// with every algo
			for(MatchingAlgorithm a: this.nodeAlgos){
				candidates = a.run(refNode, (List<? extends GraphElement>) candidates);

				if(candidates.size() > 0){
					// if there is one or more candidate-Node take the first
					this.nodeReference2match.put(refNode.getId(), candidates.get(0).getId());
				}else{
					// else, add the refNode to unmatched
					this.unmatchedRefNodes.add(refNode.getId());
				}
			}
			
		}
		
		log.info("node-matching finished... " + this.nodeReference2match.size() + " of " 
				+ reference.getNodes().size() + " nodes are matched...");
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
	
	public Map<Id, Id> getNodeIdRef2Match(){
		return this.nodeReference2match;
	}
	
	public void addAlgo(MatchingAlgorithm algo){
		Class<? extends GraphElement> clazz = algo.getProcessingClass();
		if(clazz == null){
			log.error("can't register algorithm, because no processing-class is given!");
		}else if(clazz.equals(MatchingEdge.class)){
			this.edgeAlgos.add(algo);
			log.info(algo.getClass().getSimpleName() + " registered...");
		}else if(clazz.equals(MatchingNode.class)){
			this.nodeAlgos.add(algo);
			log.info(algo.getClass().getSimpleName() + " registered...");
		}else if(clazz.equals(MatchingSegment.class)){
			this.segmentAlgos.add(algo);
			log.info(algo.getClass().getSimpleName() + " registered...");
		}
	}
	
	public void clearAlgos(){
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
		
		Double deltaDist = 300.0;
		Double deltaPhi = (Math.PI / 4);
		GraphMatching r = new GraphMatching(v, h);
//		r.addAlgo(new NodeDistAlgo(deltaDist, h));
//		r.addAlgo(new NodeAngleAlgo(deltaPhi));
		r.topDownMatching();
//		for(Entry<Id, Id> e : r.getNodeRef2Match().entrySet()){
//			System.out.println(e.getKey() + " " + e.getValue());
//		}
	}
}

