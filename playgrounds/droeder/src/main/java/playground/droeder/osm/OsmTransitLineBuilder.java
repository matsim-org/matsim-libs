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
package playground.droeder.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.droeder.DaPaths;
import playground.droeder.data.GraphMatching;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingGraph;
import playground.droeder.data.graph.MatchingNode;
import playground.droeder.data.graph.MatchingSegment;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class OsmTransitLineBuilder {
	private static final Logger log = Logger
			.getLogger(OsmTransitLineBuilder.class);
	
	private String osmFile;
	private String fromCoord;
	private String toCoord;
	private TransitSchedule timeTable;

	public OsmTransitLineBuilder(final String osmFile, final String fromCoord, final String toCoord, final TransitSchedule timeTableTomatch){
		this.osmFile = osmFile;
		this.fromCoord = fromCoord;
		this.toCoord = toCoord;
		this.timeTable = timeTableTomatch;
	}
	
	@SuppressWarnings("unchecked")
	public void run(final String outDir){
		String[] modes = new String[1];
		modes[0] = "subway";
		Osm2TransitlineNetworkReader networkReader = new Osm2TransitlineNetworkReader(this.osmFile, this.fromCoord, this.toCoord);
		networkReader.convertOsm2Matsim(modes);
		
		Map<Id, Map<Id, List<Link>>> potentialOSMRoutes = createNewRoutes(networkReader.getLine2Net());
		GraphMatching gm = new GraphMatching(createRefGraph(potentialOSMRoutes), createMatchGraph(this.timeTable));
		gm.setMaxAngle(Math.PI / 6);
		gm.setMaxDist(500.0);
		gm.setMaxLengthTolerancePerc(0.1);
		gm.run();
//		gm.nodes2Shape(outDir);
//		gm.baseSegments2Shape(outDir);
//		gm.matchedSegments2Shape(outDir);
//		gm.unmatchedAfterPrematchingOut(outDir);
		Map<String, SortedMap<Integer, Coord>> edgeMap = new HashMap<String, SortedMap<Integer,Coord>>();
		SortedMap<Integer , Coord> edges;
		Map<String, SortedMap<String, String>> attribs = new HashMap<String, SortedMap<String,String>>();
		SortedMap<String, String> values;
		
		MatchingEdge edge;
		int match = 0;
		for(Entry<Id, Id> e: gm.getPartlyMatchedEdges().entrySet()){
			values = new TreeMap<String, String>();
			values.put("matchNr", String.valueOf(match));
			
			edges = new TreeMap<Integer, Coord>();
			edge = gm.getRefGraph().getEdges().get(e.getKey());
			boolean first = true; 
			int cnt = 0;
			for(MatchingSegment s: edge.getSegments()){
				if(first){
					edges.put(cnt, s.getStart());
					first = false;
					cnt++;
				}
				edges.put(cnt, s.getEnd());
				cnt++;
			}
			edgeMap.put(e.getKey().toString(), edges);
			attribs.put(e.getKey().toString(), values);
			
			edges = new TreeMap<Integer, Coord>();
			edge = gm.getCandGraph().getEdges().get(e.getValue());
			first = true; 
			cnt = 0;
			for(MatchingSegment s: edge.getSegments()){
				if(first){
					edges.put(cnt, s.getStart());
					first = false;
					cnt++;
				}
				edges.put(cnt, s.getEnd());
				cnt++;
			}
			edgeMap.put(e.getValue().toString(), edges);
			attribs.put(e.getValue().toString(), values);
			
			match++;
		}
		DaShapeWriter.writeDefaultLineString2Shape(outDir + "parts.shp", "matchedParts", edgeMap, attribs);
		
//		Map<String, SortedMap<Integer, Coord>> lines = new HashMap<String, SortedMap<Integer,Coord>>();
//		for(Entry<Id, Map<Id, List<Link>>> e: potentialOSMRoutes.entrySet()){
//			for(Entry<Id, List<Link>> ee: e.getValue().entrySet()){
//				SortedMap<Integer, Coord> coord = new TreeMap<Integer, Coord>();
//				String name = e.getKey().toString() + "_" + String.valueOf(ee.getKey());
//				int ii = 0;
//				coord.put(ii, ee.getValue().get(0).getFromNode().getCoord());
//				for(Link l : ee.getValue()){
//					ii++;
//					coord.put(ii, l.getToNode().getCoord());
//				}
//				lines.put(name, coord);
//			}
//		}
//		DaShapeWriter.writeDefaultLineString2Shape(outDir + "lines.shp", "subways", lines, null);
		
//		for(Entry<Id, List<NetworkImpl>> lines : newLines.entrySet()){
//			for(int i = 0; i< lines.getValue().size(); i++){
//				String name = outDir + lines.getKey() + "_" + String.valueOf(i) + ".shp";
//				DaShapeWriter.writeLinks2Shape(name, lines.getValue().get(i).getLinks(), null);
//			}
//		}
	}
	

	/**
	 * @param timeTable2
	 * @return
	 */
	private MatchingGraph createMatchGraph(TransitSchedule timeTable) {
		MatchingGraph g = new MatchingGraph();
		for(TransitStopFacility stop : timeTable.getFacilities().values()){
			g.addNode(new MatchingNode(stop.getId(), stop.getCoord()));
		}
		
		
		ArrayList<Coord> shape;
		MatchingEdge e;
		MatchingNode start = null, end = null;
		for(TransitLine l: timeTable.getTransitLines().values()){
			if(!l.getId().toString().startsWith("U")) continue;
			for(TransitRoute r : l.getRoutes().values()){
				boolean first = true;
				shape = new ArrayList<Coord>();
				for(TransitRouteStop stop : r.getStops()){
					if(first){
						start = g.getNodes().get(stop.getStopFacility().getId());
						first = false;
					}else{
						end = g.getNodes().get(stop.getStopFacility().getId());
					}
					shape.add(stop.getStopFacility().getCoord());
				}
				
				e = new MatchingEdge(new IdImpl(l.getId() +"_" + r.getId()), start, end);
				e.addShapePointsAndCreateSegments(shape);
				g.addEdge(e);
			}
		}
		
		return g;
	}

	/**
	 * @param potentialOSMRoutes
	 * @return
	 */
	private MatchingGraph createRefGraph(Map<Id, Map<Id, List<Link>>> potentialOSMRoutes) {
		MatchingGraph g = new MatchingGraph();
		
		ListIterator<Link> lIt;
		ArrayList<Coord> shape;
		Link l ;
		MatchingNode start, end = null;
		for(Entry<Id, Map<Id, List<Link>>> line: potentialOSMRoutes.entrySet()){
			for(Entry<Id, List<Link>> route: line.getValue().entrySet()){
				shape = new ArrayList<Coord>();
				lIt = route.getValue().listIterator();
				l = lIt.next();
				if(g.getNodes().containsKey(l.getToNode().getId())){
					start = g.getNodes().get(l.getToNode().getId());
				}else{
					start = new MatchingNode(l.getToNode().getId(), l.getToNode().getCoord());
					g.addNode(start);
				}
//				shape.add(l.getFromNode().getCoord());
				shape.add(l.getToNode().getCoord());
				while(lIt.hasNext()){
					l = lIt.next();
					if(g.getNodes().containsKey(l.getToNode().getId())){
						end = g.getNodes().get(l.getToNode().getId());
					}else{
						end = new MatchingNode(l.getToNode().getId(), l.getToNode().getCoord());
					}
					shape.add(l.getToNode().getCoord());
				}
				if(!g.getNodes().containsKey(end.getId())){
					g.addNode(end);
				}
				MatchingEdge e = new MatchingEdge(new IdImpl(line.getKey() + "_" + route.getKey()), start, end);
				e.addShapePointsAndCreateSegments(shape);
				g.addEdge(e);
			}
		}
		return g;
	}

	/**
	 * @param line2Net
	 * @return
	 */
	private HashMap<Id, Map<Id, List<Link>>> createNewRoutes(Map<Id, NetworkImpl> line2Net) {
		FreespeedTravelTimeCost cost = new FreespeedTravelTimeCost(-1, 0, 0);
		Dijkstra router;
		
		HashMap<Id, Map<Id, List<Link>>> newLines = new HashMap<Id, Map<Id, List<Link>>>();
		NetworkImpl routeNet;
		
		for(Entry<Id, NetworkImpl> net : line2Net.entrySet()){
			Map<Id, List<Link>> newRoutes = new HashMap<Id, List<Link>>();
			int i = 0;
			for(Node from : findStartNode(net.getValue()).values()){
				for(Node to: findStartNode(net.getValue()).values()){
					if(!from.equals(to)){
						router = new Dijkstra(net.getValue(), cost, cost);
						Path p = router.calcLeastCostPath(from, to, 0);
						if(!(p==null)){
							routeNet = NetworkImpl.createNetwork();
							for(Node node: p.nodes){
								routeNet.createAndAddNode(node.getId(), node.getCoord());
							}
							for(Link l: p.links){
								routeNet.createAndAddLink(l.getId(), routeNet.getNodes().get(l.getFromNode().getId()), 
										routeNet.getNodes().get(l.getToNode().getId()), 
										l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes());
							}
							newRoutes.put(new IdImpl(i), p.links);
							i++;
						}
					}
				}
				if(newRoutes.size() > 0){
					newLines.put(net.getKey(), newRoutes);
				}else{
					log.error("can not create routes for line " + net.getKey());
				}
			}
		}
		return newLines;
	}

	public Map<Id, Node> findStartNode(Network n){
		Map<Id, Node> nodes = new HashMap<Id, Node>();
		
		for(Node no: n.getNodes().values()){
			if(no.getInLinks().size() == 1 && no.getOutLinks().size() == 1){
				nodes.put(no.getId(), no);
			}
		}
		return nodes;
	}
	
	public static void main(String[] args){
		final String OSM = DaPaths.OUTPUT + "osm2/";
		final String INFILE = OSM + "berlin_subway.osm";
		
		final String HAFAS = DaPaths.OUTPUT + "bvg09/";
		final String HAFASTRANSITFILE = HAFAS + "transitSchedule-HAFAS-Coord.xml";
		ScenarioImpl hafas = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		hafas.getConfig().scenario().setUseTransit(true);
		hafas.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(hafas).readFile(HAFASTRANSITFILE);
		
		new OsmTransitLineBuilder(INFILE, TransformationFactory.WGS84, TransformationFactory.DHDN_GK4, hafas.getTransitSchedule()).run(OSM);
		
	}

}
