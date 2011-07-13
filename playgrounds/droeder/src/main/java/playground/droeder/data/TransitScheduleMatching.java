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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.droeder.DaPaths;
import playground.droeder.GeoCalculator;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingGraph;
import playground.droeder.data.graph.MatchingNode;

/**
 * @author droeder
 *
 */
public class TransitScheduleMatching {
	
	private static final Logger log = Logger
			.getLogger(TransitScheduleMatching.class);
	
	private final String SEPARATOR = ";";

	private Double maxDeltaPhi;
	private Double maxDeltaDist;
	private Double lengthDiffPerc;
	private ScenarioImpl osmSc;
	private TransitSchedule hafasSched;
	
	public static void main(String[] args){
		final String DIR = DaPaths.OUTPUT;
		final String OSM = DIR + "osm/";
		final String HAFAS = DIR + "bvg09/";
		
		final String OSMNET = OSM + "osm_berlin_subway_net.xml";
		final String OSMSCHED = OSM + "osm_berlin_subway_sched.xml";
		final String HAFASSCHED = HAFAS + "transitSchedule-HAFAS-Coord.xml";
		
//		final String OUTFILE = OSM + "schedule_osm_hafas_merged.xml";
		
		ScenarioImpl osm = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		osm.getConfig().scenario().setUseTransit(true);
		new MatsimNetworkReader(osm).readFile(OSMNET);
		new TransitScheduleReader(osm).readFile(OSMSCHED);
		
		ScenarioImpl hafas = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		hafas.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(hafas).readFile(HAFASSCHED);
		
		new TransitScheduleMatching(Math.PI/4, 750.0, 0.2).run(osm, hafas.getTransitSchedule(), OSM);
	}
	
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
	public void run(ScenarioImpl osm, TransitSchedule hafas, String outDir){
		this.osmSc = osm;
		this.hafasSched = hafas;
		
		MatchingGraph osmG = createOsmGraph();
		MatchingGraph hafasG = createHafasGraph();
		
		GraphMatching gm = new GraphMatching(osmG, hafasG);
		gm.setMaxAngle(this.maxDeltaPhi);
		gm.setMaxDist(this.maxDeltaDist);
		gm.setMaxLengthTolerancePerc(this.lengthDiffPerc);
		gm.run();
		gm.matchedSegments2Shape(outDir);
		gm.baseSegments2Shape(outDir);
		
		createAndWriteNewSchedule(gm.getEdges(), outDir + "schedule_osm_hafas_merged.xml");
	}

	
	/**
	 * @return
	 */
	private MatchingGraph createHafasGraph() {
		log.info("creating HafasGraph...");
		MatchingGraph g = new MatchingGraph();
		Id edgeId, startNode, endNode;
		MatchingNode start, end;
		MatchingEdge e;
		ArrayList<Coord> shape;
		
		for(TransitLine l: this.hafasSched.getTransitLines().values()){
			if(!l.getId().toString().startsWith("U")) continue;
			for(TransitRoute r: l.getRoutes().values()){
				edgeId = new IdImpl(l.getId() + this.SEPARATOR + r.getId());
				startNode = new IdImpl(r.getStops().get(0).getStopFacility().getId().toString());
				endNode = new IdImpl(r.getStops().get(r.getStops().size()-1).getStopFacility().getId().toString());
				if(!g.getNodes().containsKey(startNode)){
					start = new MatchingNode(startNode, r.getStops().get(0).getStopFacility().getCoord());
					g.addNode(start);
				}else{
					start = g.getNodes().get(startNode);
				}
				if(!g.getNodes().containsKey(endNode)){
					end = new MatchingNode(endNode, r.getStops().get(r.getStops().size()-1).getStopFacility().getCoord());
					g.addNode(end);
				}else{
					end = g.getNodes().get(endNode);
				}
				e = new MatchingEdge(edgeId, start, end);
				shape = new ArrayList<Coord>();
				for(TransitRouteStop s: r.getStops()){
					shape.add(s.getStopFacility().getCoord());
				}
				e.addShapePointsAndCreateSegments(shape);
				g.addEdge(e);
			}
		}
		return g;
	}

	/**
	 * @return
	 */
	private MatchingGraph createOsmGraph() {
		log.info("creating osmGraph...");
		MatchingGraph g = new MatchingGraph();
		Id edgeId;
		Node startNode, endNode;
		MatchingNode start, end;
		MatchingEdge e;
		ArrayList<Coord> shape;
		
		for(TransitLine l: osmSc.getTransitSchedule().getTransitLines().values()){
//			if(!l.getId().toString().contains("U1")) continue;
			for(TransitRoute r: l.getRoutes().values()){
//				System.out.println(r.getId());
				edgeId = new IdImpl(l.getId().toString() + this.SEPARATOR + r.getId().toString());
				startNode = this.osmSc.getNetwork().getLinks().get(r.getRoute().getStartLinkId()).getFromNode();
				endNode = this.osmSc.getNetwork().getLinks().get(r.getRoute().getEndLinkId()).getToNode();
				if(g.getNodes().containsKey(startNode.getId())){
					start = g.getNodes().get(startNode.getId());
				}else{
					start = new MatchingNode(startNode.getId(), startNode.getCoord());
					g.addNode(start);
				}
				if(g.getNodes().containsKey(endNode.getId())){
					end = g.getNodes().get(endNode.getId());
				}else{
					end = new MatchingNode(endNode.getId(), endNode.getCoord());
					g.addNode(end);
				}
				e = new MatchingEdge(edgeId, start, end);
				shape = new ArrayList<Coord>();
				shape.add(startNode.getCoord());
				for(Id linkId: r.getRoute().getLinkIds()){
					shape.add(this.osmSc.getNetwork().getLinks().get(linkId).getToNode().getCoord());
//					System.out.print(linkId + "\t");
				}
//				System.out.println();
				shape.add(endNode.getCoord());
				e.addShapePointsAndCreateSegments(shape);
				g.addEdge(e);
			}
		}
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
		Id osmLine;
		
		for(Entry<Id, Id> e: edges.entrySet()){
			String[] split = e.getKey().toString().split(this.SEPARATOR);
			osmLine = new IdImpl(split[0]);
			osm = osmSc.getTransitSchedule().getTransitLines().get(osmLine).getRoutes().get(new IdImpl(split[1]));
			
			split = e.getValue().toString().split(this.SEPARATOR);
			hafas = hafasSched.getTransitLines().get(new IdImpl(split[0])).getRoutes().get(new IdImpl(split[1]));
			
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
		if(osm.getStops() == null){
			this.mergeStopsAndLinks(osmLine, osm, hafas, sched);
		}else if(osm.getStops().size() 
				== hafas.getStops().size()){
			this.mergeStops(osmLine, osm, hafas, sched);
		}else if((osm.getRoute().getLinkIds().size() + 2) < hafas.getStops().size()){
		// +2 because start- and endLink are not in the LinkIdList
			log.error("can not create new Route for osmLine " + osmLine.toString() + "/ osmRoute " + osm.getId().toString() + 
					"with HafasRoute " + hafas.getId() + ", because number of osmLinks is smaller than number of hafasStops" +
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
			stops.add(this.createRouteStop(osm.getStops().get(i).getStopFacility().getLinkId(), hafas.getStops().get(i), sched));
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
		stops.add(this.createRouteStop(osm.getRoute().getStartLinkId(), hafas.getStops().get(0), sched));
		//handle other stops - first and last stop are handled separately
		if(hafas.getStops().size() > 2){
			ListIterator<TransitRouteStop> hafasIt = hafas.getStops().subList(1, hafas.getStops().size()-1).listIterator();
			ListIterator<Id> osmIter = osm.getRoute().getLinkIds().listIterator();
			TransitRouteStop hafasStop, newStop;
			Id osmLinkId;
			Double minDistance = Double.MAX_VALUE, distance;
			
			log.error(osmLine + " " + osm.getId() + "... " + (osm.getRoute().getLinkIds().size() + 2) + " links, " + hafas.getStops().size() +" stops...");
			
			while(hafasIt.hasNext()){
				hafasStop = hafasIt.next();
				if(!osmIter.hasNext()){
//					log.error("can not distribute stops 2 links, not enough links for Line " + osmLine + "... " +
//							(osm.getRoute().getLinkIds().size() + 2) + " links, " + hafas.getStops().size() +" stops...");
					return;
				}
				while(osmIter.hasNext() && hafasIt.hasNext()){
					osmLinkId = osmIter.next();
					distance = this.calcDist(osmLinkId, hafasStop);
//					log.info("omsLink: " + osmLinkId + " distance " + distance);
					if(distance < minDistance){
						if(osmIter.hasNext()){
							minDistance = distance;
						}else{
							newStop = createRouteStop(osmLinkId, hafasStop, sched);
//							log.info("newStop: " + newStop.getStopFacility().getId() + " at Link: " + newStop.getStopFacility().getLinkId());
							stops.add(newStop);
						}
					}else if(distance >= minDistance){
						// TODO ????
						osmIter.previous();
						osmLinkId = osmIter.previous();
						newStop = createRouteStop(osmLinkId, hafasStop, sched);
//						log.info("newStop: " + newStop.getStopFacility().getId() + " at Link: " + newStop.getStopFacility().getLinkId());
						stops.add(newStop);
						osmIter.next();
						minDistance = Double.MAX_VALUE;
					}
				}
			}
		}
		//handle last stop
		stops.add(this.createRouteStop(osm.getRoute().getEndLinkId(), hafas.getStops().get(osm.getStops().size()-1), sched));
		//create Route
		LinkNetworkRouteImpl netRoute = new LinkNetworkRouteImpl(osm.getRoute().getStartLinkId(), osm.getRoute().getEndLinkId());
		netRoute.setLinkIds(osm.getRoute().getStartLinkId(), osm.getRoute().getLinkIds(), osm.getRoute().getEndLinkId());
		TransitRoute route = sched.getFactory().createTransitRoute(hafas.getId(), netRoute, stops, hafas.getTransportMode());
		if(!sched.getTransitLines().get(osmLine).getRoutes().containsKey(route.getId())){
			sched.getTransitLines().get(osmLine).addRoute(route);
		}else{
			log.error("there is already a route: " + route.getId() + " for TransitLine: " + osmLine);
		}
	}

	private Double calcDist(Id linkId, TransitRouteStop hafasStop){
		return GeoCalculator.distanceBetween2Points(this.osmSc.getNetwork().getLinks().get(linkId).getToNode().getCoord(), 
				hafasStop.getStopFacility().getCoord());
	}
	private TransitRouteStop createRouteStop(Id osmLinkId, TransitRouteStop hafasStop, TransitSchedule sched){
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
