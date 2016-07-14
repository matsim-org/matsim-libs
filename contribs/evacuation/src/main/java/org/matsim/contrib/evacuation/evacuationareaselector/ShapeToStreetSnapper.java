/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeToStreetSnapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.evacuationareaselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.evacuation.control.helper.Algorithms;
import org.matsim.contrib.evacuation.control.helper.shapetostreetsnapper.LinkSorter;
import org.matsim.contrib.evacuation.control.helper.shapetostreetsnapper.TravelCost;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class ShapeToStreetSnapper {
	
	GeometryFactory geofac = new GeometryFactory(new PrecisionModel(2));
	
	
	private static final double DETOUR_COEF = 5;
	private static double TRAVEL_COST_CUTOFF = 1000;
	private final Scenario sc;


	private double maxL;


	private QuadTree<Node> qTree;

	public ShapeToStreetSnapper(Scenario sc) {
		this.sc = sc;
		fixOneWayStreets();
		buildQuadTree();
	}

	private void buildQuadTree() {
		this.maxL = 0.;
		Envelope e = new Envelope();
		for (Link l : this.sc.getNetwork().getLinks().values()) {
			if (this.maxL < l.getLength()/2) {
				this.maxL = l.getLength()/2;
			}
			e.expandToInclude(MGC.coord2Coordinate(l.getFromNode().getCoord()));
			e.expandToInclude(MGC.coord2Coordinate(l.getToNode().getCoord()));
			
		}
		
		this.qTree = new QuadTree<Node>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		for (Node n : this.sc.getNetwork().getNodes().values()) {
			this.qTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		
	}

	public Polygon run(Polygon p) {
		List<Node> nodes  = getBoundaryNodes(p);
		FreeSpeedTravelTime fs = new FreeSpeedTravelTime();
		TravelDisutility cost = new TravelCost(p);
		LeastCostPathCalculator dijkstra = new Dijkstra(this.sc.getNetwork(), cost, fs);
		
		List<Node> finalNodes = new ArrayList<Node>();
		for (int i = 1; i < nodes.size(); i++) {
			Node n0 = nodes.get(i-1);
			Node n1 = nodes.get(i);
			Path path = dijkstra.calcLeastCostPath(n0, n1, 0, null, null);
			if (path != null && (path.travelCost < TRAVEL_COST_CUTOFF || path.travelCost < DETOUR_COEF * CoordUtils.calcEuclideanDistance(n0.getCoord(), n1.getCoord()))) {
				finalNodes.addAll(path.nodes.subList(0, path.nodes.size()-1));
			}else {
				finalNodes.add(n0);
			}
			
		}
		Node nn0 = nodes.get(nodes.size()-1);
		Node nn1 = nodes.get(0);
		Path path = dijkstra.calcLeastCostPath(nn0, nn1, 0, null, null);
		if (path.travelCost < TRAVEL_COST_CUTOFF || path.travelCost < DETOUR_COEF * CoordUtils.calcEuclideanDistance(nn0.getCoord(), nn1.getCoord()) ) {
			finalNodes.addAll(path.nodes.subList(0, path.nodes.size()-1));
		}

		Set<Integer> rmIdxs = new HashSet<Integer>();
		for (int i = 0; i < finalNodes.size()-2; i++) {
			Node n0 = finalNodes.get(i);
			for (int j = i+1; j < finalNodes.size()-1; j++) {
				Node n1 = finalNodes.get(j);
				if (n1 == n0) {
					for (int k = i+1; k <= j; k++) {
						rmIdxs.add(k);
					}
					i = j;
					break;
				}
			}
			
		}
		
		int idx = 0;
		Iterator<Node> it = finalNodes.iterator();
		while (it.hasNext()) {
			it.next();
			if (rmIdxs.contains(idx)) {
				it.remove();
			}
			idx++;
		}
		
		Coordinate [] coords = new Coordinate[finalNodes.size()+1];
		for (int i = 0; i < coords.length-1; i++) {
			coords[i] = MGC.coord2Coordinate(finalNodes.get(i).getCoord());
		}
		coords[coords.length-1] = coords[0];
		LinearRing shell = this.geofac.createLinearRing(coords);
		Polygon ep = this.geofac.createPolygon(shell, null);
	
		try {
			return (Polygon) ep.buffer(0).union(p);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return p;
		
	}
	


	private void fixOneWayStreets() {

		List<Link> rev = new ArrayList<Link>();
		for (Link link : this.sc.getNetwork().getLinks().values()) {
			
			boolean oneWay = true;
			for (Link l : link.getToNode().getOutLinks().values()) {
				if (l.getToNode().equals(link.getFromNode())) {
					oneWay = false;
					break;
				}
			}
			if (oneWay) {
				Link reverse = this.sc.getNetwork().getFactory().createLink(Id.create(link.getId().toString()+"reverse", Link.class), link.getToNode(), link.getFromNode());
				reverse.setFreespeed(link.getFreespeed());
				reverse.setLength(link.getLength());
				reverse.setNumberOfLanes(link.getNumberOfLanes());
				reverse.setCapacity(link.getCapacity());
				rev.add(reverse);
			}
			
		}
		for (Link reverse : rev) {
			this.sc.getNetwork().addLink(reverse);
		}
		
	}

	private List<Node> getBoundaryNodes(Polygon p) {
		
		List<Node> nodes = new ArrayList<Node>();
		for (int i = 1; i < p.getExteriorRing().getNumPoints(); i++) {
			Coordinate c0 = p.getExteriorRing().getCoordinateN(i-1);
			Coordinate c1 = p.getExteriorRing().getCoordinateN(i);
			handle(c0,c1,nodes,p);
		}
		Coordinate c0 = p.getExteriorRing().getCoordinateN(p.getExteriorRing().getCoordinates().length-1);
		Coordinate c1 = p.getExteriorRing().getCoordinateN(0);
		handle(c0,c1,nodes,p);
		return nodes;
	}

	private void handle(Coordinate c0, Coordinate c1, List<Node> nodes, Polygon p) {
		LineString ls = this.geofac.createLineString(new Coordinate[]{c0,c1});
		
		Coordinate center = new Coordinate((c0.x+c1.x)/2,(c0.y+c1.y)/2);
		double searchRange = Math.max(this.maxL, c0.distance(c1));
		Collection<Node> coll = this.qTree.getDisk(center.x, center.y, searchRange);
		
		List<Link> iLs = new ArrayList<Link>();
		for (Node n : coll) {
			for (Link l : n.getOutLinks().values()) {
				Coordinate tmpC0 = MGC.coord2Coordinate(l.getFromNode().getCoord());
				Coordinate tmpC1 = MGC.coord2Coordinate(l.getToNode().getCoord());
				LineString tmpLs = this.geofac.createLineString(new Coordinate[]{tmpC0,tmpC1});
				
				if (ls.intersects(tmpLs)) {
					iLs.add(l);
				}
			}
			for (Link l : n.getInLinks().values()) {
				Coordinate tmpC0 = MGC.coord2Coordinate(l.getFromNode().getCoord());
				Coordinate tmpC1 = MGC.coord2Coordinate(l.getToNode().getCoord());
				LineString tmpLs = this.geofac.createLineString(new Coordinate[]{tmpC0,tmpC1});
				
				if (ls.intersects(tmpLs)) {
					iLs.add(l);
				}
			}
		}
		
		LinkSorter sorter = new LinkSorter(c0, c1);
		Collections.sort(iLs, sorter);
		for (Link link : iLs) {
			Node tmp = link.getFromNode();
			if (Algorithms.contains(MGC.coord2Coordinate(tmp.getCoord()),p.getExteriorRing().getCoordinates())) {
				tmp = link.getToNode();
			} 
			
			if (Algorithms.contains(MGC.coord2Coordinate(tmp.getCoord()),p.getExteriorRing().getCoordinates())) {
				//link intersects polygon boundary but orig and dest node are inside polygon
				continue;
			}
			
			if (tmp.getInLinks().size() <= 2) {
				//polygon cuts network on a non-intersection node, so we walk downstream and try to find the next inetsection
				tmp = getBndNode(tmp,link);	
				if (Algorithms.contains(MGC.coord2Coordinate(tmp.getCoord()),p.getExteriorRing().getCoordinates())) {
					continue;
				}
			}
			
			if (nodes.size() == 0 || nodes.get(nodes.size()-1) != tmp) {
				nodes.add(tmp);
			}
		}		
	}

	private Node getBndNode(Node tmp, Link link) {
		

		Node origTmp = tmp;
		
		Node oldTmp = null; 
		if (link.getToNode() == tmp) {
			oldTmp = link.getFromNode();
		} else {
			oldTmp = link.getToNode();
		}
		while (tmp.getInLinks().size() <= 2) {
			for (Link l : tmp.getOutLinks().values()) {
				if (l.getToNode() != oldTmp) {
					oldTmp = tmp;
					tmp = l.getToNode();
					break;
				}
			}
			if (tmp == origTmp || oldTmp == origTmp) {
				return origTmp;
			}
		}
		
		return tmp;
	}
	
}
