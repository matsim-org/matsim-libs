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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.droeder.data.graph.MatchingGraph;

/**
 * @author droeder
 *
 */
public class TransitScheduleMatching {
	
	private static final Logger log = Logger
			.getLogger(TransitScheduleMatching.class);

	private Double maxDeltaPhi;
	private Double maxDeltaDist;
	private Double lengthDiffPerc;
	private ScenarioImpl osmSc;
	private TransitSchedule hafasSched;
	
	/**
	 * @param base
	 * @param toMatch
	 */
	public TransitScheduleMatching(Double maxDeltaPhi, Double maxDeltaDist, Double lengthDiffPerc){
		this.maxDeltaDist = maxDeltaDist;
		this.maxDeltaPhi = maxDeltaPhi;
		this.lengthDiffPerc = lengthDiffPerc;
	}
	
	/**
	 * creates a new TransitSchedule for the <code>ScenarioImpl</code> based on the given TransitSchedule 
	 * @param osm
	 * @param hafas
	 */
	public void run(ScenarioImpl osm, TransitSchedule hafas, String outFile){
		this.osmSc = osm;
		this.hafasSched = hafas;
		
		MatchingGraph osmG = createOsmGraph();
		MatchingGraph hafasG = createHafasGraph();
		
		GraphMatching gm = new GraphMatching(osmG, hafasG);
		gm.setMaxAngle(this.maxDeltaPhi);
		gm.setMaxDist(this.maxDeltaDist);
		gm.setMaxLengthTolerancePerc(this.lengthDiffPerc);
		gm.run();
		
		createAndWriteNewSchedule(gm.getEdges(), outFile);
	}

	
	/**
	 * @return
	 */
	private MatchingGraph createHafasGraph() {
		log.info("creating HafasGraph...");
		MatchingGraph g = new MatchingGraph();
		
		
		
		return g;
	}

	/**
	 * @return
	 */
	private MatchingGraph createOsmGraph() {
		log.info("creating osmGraph...");
		MatchingGraph g = new MatchingGraph();
		
		
		
		return g;
	}
	
	/**
	 * @param edges
	 * @param outFile
	 */
	private void createAndWriteNewSchedule(Map<Id, Id> edges, String outFile) {
		//prepare new Scenario
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		
		//#################
		TransitRoute osm, hafas;
		Id osmLine, hafasLine;
		
		for(Entry<Id, Id> e: edges.entrySet()){
			osmLine = new IdImpl(e.getKey().toString().split("__")[0]);
			osm = osmSc.getTransitSchedule().getTransitLines().get(osmLine).getRoutes().get(e.getKey());
			
			hafasLine = new IdImpl(e.getValue().toString().split("__")[0]);
			hafas = hafasSched.getTransitLines().get(hafasLine).getRoutes().get(e.getValue());
			
			this.addStopsAndCreateRoute(osmLine, osm, hafas, sc);
		}
		
		TransitScheduleWriter writer = new TransitScheduleWriter(sc.getTransitSchedule());
		writer.writeFile(outFile);
	}

	
	/**
	 * @param osm
	 * @param hafas
	 */
	private void addStopsAndCreateRoute(Id osmLine, TransitRoute osm, TransitRoute hafas, ScenarioImpl newSc) {
		TransitSchedule sched = newSc.getTransitSchedule();
		TransitScheduleFactory fac = sched.getFactory();
		
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		
		List<Id> linkIds = osm.getRoute().getLinkIds();
		String mode = osm.getTransportMode();

		TransitLine line;
		TransitRoute newRoute;
		NetworkRoute netRoute;
		
		TransitStopFacility hafasFac, newFac;
		TransitRouteStop hafasStop, newStop;
		
		if(!sched.getTransitLines().containsKey(osmLine)){
			line = fac.createTransitLine(osmLine);
		}else{
			line = sched.getTransitLines().get(osmLine);
		}
		
		ListIterator<TransitRouteStop> hafasIt = hafas.getStops().listIterator();
		// make StopFacility for the first stop
		if(hafasIt.hasNext()){
			hafasStop = hafasIt.next();
			hafasFac = hafasStop.getStopFacility();
		}else{
			log.error("route has no stops...");
			return;
		}
		if(sched.getFacilities().containsKey(hafasFac.getId())){
			newFac = sched.getFacilities().get(hafasFac.getId());
		}else{
			newFac = fac.createTransitStopFacility(hafasFac.getId(), hafasFac.getCoord(), hafasFac.getIsBlockingLane());
			newFac.setLinkId(osm.getRoute().getStartLinkId());
			sched.addStopFacility(newFac);
		}
		newStop = fac.createTransitRouteStop(newFac, hafasStop.getArrivalOffset(), hafasStop.getDepartureOffset());
		stops.add(newStop);
		
		int pointer = 0, size = osm.getRoute().getLinkIds().size();
		
		// process other stops
		while(hafasIt.hasNext()){
			hafasStop = hafasIt.next();
			hafasFac = hafasStop.getStopFacility();
			
			if(sched.getFacilities().containsKey(hafasFac.getId())){
				newFac = sched.getFacilities().get(hafasFac.getId());
			}else{
				newFac = fac.createTransitStopFacility(hafasFac.getId(), hafasFac.getCoord(), hafasFac.getIsBlockingLane());
				newFac.setLinkId(findNextLink(osm.getRoute().getLinkIds().subList(pointer, size), newFac.getCoord()));
				pointer = osm.getRoute().getLinkIds().indexOf(newFac.getLinkId());
				sched.addStopFacility(newFac);
			}
			newStop = fac.createTransitRouteStop(newFac, hafasStop.getArrivalOffset(), hafasStop.getDepartureOffset());
			stops.add(newStop);
		}
		
		
		
		
		netRoute = new LinkNetworkRouteImpl(osm.getRoute().getStartLinkId(), osm.getRoute().getEndLinkId());
		netRoute.setLinkIds(osm.getRoute().getStartLinkId(), linkIds, osm.getRoute().getEndLinkId());
		
		newRoute = fac.createTransitRoute(osm.getId(), netRoute, stops, mode);
		
	}
	
	private Id findNextLink(List<Id> osmLinkIds, Coord hafasCoord){
		
		return null;
	}


}
