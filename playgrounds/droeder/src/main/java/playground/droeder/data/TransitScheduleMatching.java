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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
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
	
	private final String SEPARATOR = "_|_";

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
			osmLine = new IdImpl(e.getKey().toString().split(this.SEPARATOR)[0]);
			osm = osmSc.getTransitSchedule().getTransitLines().get(osmLine).getRoutes().get(e.getKey());
			
			hafasLine = new IdImpl(e.getValue().toString().split(this.SEPARATOR)[0]);
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
		//prepare for handling
		TransitSchedule sched = newSc.getTransitSchedule();
		if(!newSc.getTransitSchedule().getTransitLines().containsKey(osmLine)){
			sched.addTransitLine(sched.getFactory().createTransitLine(osmLine));
		}
		
		// create new route
		if(osm.getStops().size() == hafas.getStops().size()){
			this.mergeStops(osmLine, osm, hafas, sched);
		}else if((osm.getRoute().getLinkIds().size() + 2) < hafas.getStops().size()){
		// +2 because start- and endLink are not in the LinkIdList
			log.error("can not create new Route for osmLine " + osmLine.toString() + "/ osmRoute " + osm.getId().toString() + 
					"with HafasRoute " + hafas.getId() + ", because number osmLinks is smaller than number of hafasStops" +
							" and every stop should be located on an separat link...");
			return;
		}else {
			this.mergeStopsAndLinks(osmLine, osm, hafas, sched);
		}
	}

	/**
	 * @param osmLine 
	 * @param osm
	 * @param hafas
	 * @param sched
	 */
	private void mergeStops(Id osmLine, TransitRoute osm, TransitRoute hafas, TransitSchedule sched) {
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		for(int i = 0; i < osm.getStops().size(); i++){
			stops.add(this.createStopFacility(osm.getStops().get(i).getStopFacility().getLinkId(), hafas.getStops().get(i), sched));
		}
		LinkNetworkRouteImpl netRoute = new LinkNetworkRouteImpl(osm.getRoute().getStartLinkId(), osm.getRoute().getEndLinkId());
		netRoute.setLinkIds(osm.getRoute().getStartLinkId(), osm.getRoute().getLinkIds(), osm.getRoute().getEndLinkId());
		TransitRoute route = sched.getFactory().createTransitRoute(hafas.getId(), netRoute, stops, hafas.getTransportMode());
		sched.getTransitLines().get(osmLine).addRoute(route);
	}
	
	/**
	 * @param osmLine
	 * @param osm
	 * @param hafas
	 * @param sched
	 */
	private void mergeStopsAndLinks(Id osmLine, TransitRoute osm, TransitRoute hafas, TransitSchedule sched) {
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		//handle first stop
		stops.add(this.createStopFacility(osm.getRoute().getStartLinkId(), hafas.getStops().get(0), sched));
		//handle other stops - first and last stop are handled separately
		if(hafas.getStops().size() > 2){
//			ListIterator<TransitRouteStop> hafasIt = hafas.getStops().subList(1, hafas.getStops().size()-1).listIterator();
//			ListIterator<Id> osmIter = osm.getRoute().getLinkIds().listIterator();
			
			
			//TODO not finished yet
		}
		//handle last stop
		stops.add(this.createStopFacility(osm.getRoute().getEndLinkId(), hafas.getStops().get(osm.getStops().size()-1), sched));
		//create Route
		LinkNetworkRouteImpl netRoute = new LinkNetworkRouteImpl(osm.getRoute().getStartLinkId(), osm.getRoute().getEndLinkId());
		netRoute.setLinkIds(osm.getRoute().getStartLinkId(), osm.getRoute().getLinkIds(), osm.getRoute().getEndLinkId());
		TransitRoute route = sched.getFactory().createTransitRoute(hafas.getId(), netRoute, stops, hafas.getTransportMode());
		sched.getTransitLines().get(osmLine).addRoute(route);
	}

	private TransitRouteStop createStopFacility(Id osmLinkId, TransitRouteStop hafasStop, TransitSchedule sched){
		TransitStopFacility hafasFacility, newFacility;
		hafasFacility = hafasStop.getStopFacility();
		if(!sched.getFacilities().containsKey(hafasFacility.getId())){
			// osm-> linkId, hafas -> rest
			newFacility = sched.getFactory().createTransitStopFacility(hafasFacility.getId(), hafasFacility.getCoord(), hafasFacility.getIsBlockingLane());
			newFacility.setLinkId(osmLinkId);
			sched.addStopFacility(newFacility);
		}else{
			newFacility = sched.getFacilities().get(hafasFacility.getId());
		}
		// offsets from hafas
		return sched.getFactory().createTransitRouteStop(newFacility, hafasStop.getArrivalOffset(),hafasStop.getDepartureOffset());
	}


}
