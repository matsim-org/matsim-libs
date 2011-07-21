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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

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
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.droeder.DaPaths;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class OsmTransitLineBuilder {
	
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
		Osm2TransitlineNetworkReader networkBuilder = new Osm2TransitlineNetworkReader(this.osmFile, this.fromCoord, this.toCoord);
		networkBuilder.convertOsm2Matsim(modes);
		
		Map<Id, Map<Id, List<Link>>> newLines = createNewLines(networkBuilder.getLine2Net());
//		newLines = reduceNetworks(newLines);
		
		Map<String, SortedMap<Integer, Coord>> lines = new HashMap<String, SortedMap<Integer,Coord>>();
		for(Entry<Id, Map<Id, List<Link>>> e: newLines.entrySet()){
			for(Entry<Id, List<Link>> ee: e.getValue().entrySet()){
				SortedMap<Integer, Coord> coord = new TreeMap<Integer, Coord>();
				String name = e.getKey().toString() + "_" + String.valueOf(ee.getKey());
				int ii = 0;
				coord.put(ii, ee.getValue().get(0).getFromNode().getCoord());
				for(Link l : ee.getValue()){
					ii++;
					coord.put(ii, l.getToNode().getCoord());
				}
				lines.put(name, coord);
			}
		}
		DaShapeWriter.writeDefaultLineString2Shape(outDir + "lines.shp", "subways", lines, null);
		
//		for(Entry<Id, List<NetworkImpl>> lines : newLines.entrySet()){
//			for(int i = 0; i< lines.getValue().size(); i++){
//				String name = outDir + lines.getKey() + "_" + String.valueOf(i) + ".shp";
//				DaShapeWriter.writeLinks2Shape(name, lines.getValue().get(i).getLinks(), null);
//			}
//		}
	}
	
	/**
	 * @param newLines
	 * @return
	 */
	private Map<Id, Map<Id, List<Link>>> reduceNetworks(Map<Id, Map<Id, List<Link>>> newLines) {
		List<Link> version1;
		List<Link> version2;
		
//		for(Entry<Id, Map<Id, List<Link>>> e: newLines.entrySet()){
//			for(Entry<Id, List<Link>> l1: e.getValue().entrySet()){
//				version1 = l1.getValue();
//				for(Entry<Id, List<Link>> l2: e.getValue().entrySet()){
//					version2 = l2.getValue();
//					if(completeSubpart(version1, version2)){
//						
//					}else if(completeSubpart(version2, version1)){
//						
//					}else if(equal(version1, version2)){
//						
//					}
//				}
//			}
//		}
		return null;
	}

	/**
	 * @param line2Net
	 * @return
	 */
	private HashMap<Id, Map<Id, List<Link>>> createNewLines(Map<Id, NetworkImpl> line2Net) {
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
				newLines.put(net.getKey(), newRoutes);
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
		final String DIR = DaPaths.OUTPUT + "osm2/";
		final String INFILE = DIR + "berlin_subway.osm";
		new OsmTransitLineBuilder(INFILE, TransformationFactory.WGS84, TransformationFactory.DHDN_GK4, null).run(DIR);
		
	}

}
