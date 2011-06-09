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
import playground.droeder.data.matching.MatchingEdge;
import playground.droeder.data.matching.MatchingGraph;
import playground.droeder.data.matching.MatchingNode;
import playground.droeder.data.matching.algorithms.NodeAngleAlgo;
import playground.droeder.data.matching.algorithms.NodeDistAlgo;

/**
 * @author droeder
 * based on http://www.fsutmsonline.net/images/uploads/reports/FDOT_BC353_21_rpt.pdf
 */
public class DataMatching {
	private static final Logger log = Logger.getLogger(DataMatching.class);
	
	private MatchingGraph reference;
	private MatchingGraph matching;
	
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
		
		DataMatching r = new DataMatching(v, h);
		r.topDownMatching(300.0, 1.0);
//		for(Entry<Id, Id> e : r.getNodeRef2Match().entrySet()){
//			System.out.println(e.getKey() + " " + e.getValue());
//		}
	}
	
	public DataMatching(MatchingGraph reference, MatchingGraph matching){
		this.reference = reference;
		this.matching = matching;
	}
	
	/**
	 * 
	 */
	public void topDownMatching(Double deltaDist, Double deltaAngle) {
		log.info("starting top-down-Matching...");
		this.nodeMatching(deltaDist, deltaAngle);
//		this.segmentMatching();
//		this.edgeMatching();
	}

	// ###### NODEMATCHING #######
	private Map<Id, Id> nodeReference2match;
	private List<Id> unmatchedRefNodes;

	private void nodeMatching(Double deltaDist, Double deltaPhi) {
		log.info("start matching Nodes...");
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

}

