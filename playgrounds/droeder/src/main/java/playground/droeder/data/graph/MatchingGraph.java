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
package playground.droeder.data.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;



/**
 * @author droeder
 *
 */
public class MatchingGraph implements Cloneable{
	private static final Logger log = Logger.getLogger(MatchingGraph.class);
	
	private Map<Id, MatchingNode> nodes;
	private Map<Id, MatchingEdge> edges;

	private QuadTree<MatchingNode> quadtree;


	public MatchingGraph(){
		this.nodes = new HashMap<Id, MatchingNode>();
		this.edges = new HashMap<Id, MatchingEdge>();
	}

	public boolean addNode(MatchingNode n){
		if(this.nodes.containsKey(n.getId())){
			log.error("graph already contains a node with id: " + n.getId());
			return false;
		}
		this.nodes.put(n.getId(), n);
		return true;
	}
	
	public boolean addEdge(MatchingEdge e){
		if(this.edges.containsKey(e.getId())){
			log.error("graph already contains an edge with id: " + e.getId());
			return false;
		}
		this.nodes.get(e.getFromNode().getId()).addIncidentEdge(e);
		this.nodes.get		(e.getToNode().getId()).addIncidentEdge(e);
		this.edges.put(e.getId(), e);
		return true;
	}
	
	public Map<Id, MatchingEdge> getEdges(){
		return this.edges;
	}
	
	public Map<Id, MatchingNode> getNodes(){
		return this.nodes;
	}
	
	/**
	 * returns all <code>MatchingNodes</code> in distance, sorted by their space to (x/y)
	 * @param x
	 * @param y
	 * @param distance
	 * @return
	 */
	public Collection<MatchingNode> getNearestNodes(Double x, Double y, Double distance){
		if(quadtree == null) buildQuadTree();
		return quadtree.get(x, y, distance);
//		List<MatchingNode> nearest = new LinkedList<MatchingNode>();
//		for(MatchingNode n : quadtree.get(x, y, distance)){
//			n.setDist(new CoordImpl(x, y));
//			nearest.add(n);
//		}
//		Collections.sort(nearest);
//		return nearest;
	}
	
	public MatchingNode getNearestNode(Double x, Double y){
		if(quadtree == null) buildQuadTree();
		MatchingNode n = quadtree.get(x, y);
//		n.setDist(new CoordImpl(x, y));
		return n;
	}


	private void buildQuadTree() {
		Double minX = Double.MAX_VALUE;
		Double minY = Double.MAX_VALUE;
		Double maxX = Double.MIN_VALUE;
		Double maxY = Double.MIN_VALUE;
		
		Double x, y;
		for(MatchingNode n : this.nodes.values()){
			x = n.getCoord().getX();
			y = n.getCoord().getY();
			
			if(x<minX) minX = x;
			if(y<minY) minY = y;
			if(x>maxX) maxX = x;
			if(y>maxY) maxY = y;
		}
		
		this.quadtree = new QuadTree<MatchingNode>(minX, minY, maxX, maxY);
		
		for(MatchingNode n: this.nodes.values()){
			this.quadtree.put(n.getCoord().getX(), n.getCoord().getY(), (MatchingNode) n);
		}
	}
	
	@Override
	public MatchingGraph clone(){
		return this.clone();
	}
}
