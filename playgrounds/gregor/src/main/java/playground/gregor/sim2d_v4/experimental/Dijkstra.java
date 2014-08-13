/* *********************************************************************** *
 * project: org.matsim.*
 * Dijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.experimental;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD.Quad;

import com.vividsolutions.jts.geom.Envelope;

public class Dijkstra {
	
	private final Map<Quad,NodeData> nodesData = new HashMap<Quad,NodeData>();
	private Quad to;
	private final Envelope e;
	
	public Dijkstra(Envelope envelope) {
		this.e = envelope;
	}

	public LinkedList<Quad> computeShortestPath(LinearQuadTreeLD network, Quad from, Quad to) {
		
		this.nodesData.clear();
		for (Quad q : network.getQuads()) {
			NodeData nd = new NodeData();
			nd.cost = Double.POSITIVE_INFINITY;
			nd.predecessor = null;
			this.nodesData.put(q, nd);
		}
		this.to = to;
		Quad curr = from;
		NodeData nd = this.nodesData.get(curr);
		nd.cost = 0;
		Set<Quad> closed = new HashSet<Quad>();
		while (curr != to) {
			nd = this.nodesData.get(curr);
			closed.add(curr);
			expandGraph(curr,nd);
			double leastCost = Double.POSITIVE_INFINITY;
			for (Entry<Quad, NodeData> tmp: this.nodesData.entrySet()) {
				if (tmp.getValue().cost <= leastCost && !closed.contains(tmp.getKey())) {
					leastCost = tmp.getValue().cost;
					curr = tmp.getKey();
				}
			}
		}
		
		
		LinkedList<Quad> ret = reconstructPath(to);
		return ret;
	}

	private void expandGraph(Quad v, NodeData nd) {
		
		List<Quad> expanded = new ArrayList<Quad>();
		expand(v,expanded,LinearQuadTreeLD.D_EAST);
		expand(v,expanded,LinearQuadTreeLD.D_NORTH);
		expand(v,expanded,LinearQuadTreeLD.D_WEST);
		expand(v,expanded,LinearQuadTreeLD.D_SOUTH);
		
		double baseCost = calcCosts(v);
		for (Quad q : expanded) {

			double travelCost = calcCosts(q)+baseCost;
			double cost = nd.cost + travelCost;
			NodeData ndPrime = this.nodesData.get(q);
			if (cost < ndPrime.cost) {
				ndPrime.cost = cost;
				ndPrime.predecessor = v;
			}
			
		}
		
	}

	private double calcCosts(Quad q) {
		double length = q.getEnvelope().getWidth()/2;
//		double k1 = computeKey(q.getEnvelope().getMaxX()*100,q.getEnvelope().getMinX()*100);
//		double k2 = computeKey(q.getEnvelope().getMaxY()*100,q.getEnvelope().getMinY()*100);
//		double rnd =computeKey(k1,k2);
//		length += 1./(1000.*rnd);
		
		double cx = (q.getEnvelope().getMaxX()+q.getEnvelope().getMinX())/2;
		double cy = (q.getEnvelope().getMaxY()+q.getEnvelope().getMinY())/2;
		
		if (cy < -2.4) {
			return 1000;
		} else if (cy < 0) {
			if (cx < -5 || cx > 4) {
				return 1000;
			}
		} else if (cy > 4){
			return 1000;
		} else if (cx < -2.4 || cx > 0) {
			return 100000;
		}
		
		double density = (q.getColor()+1)/q.getEnvelope().getArea();//-.25;
		if (q.getEnvelope().getWidth() >= 1 && q.getColor() == 0) {
			density = 0.00001;
//			return 1000;
		}
		double speed = 1.34 *(1-Math.exp(-1.913*(1/density-1/5.4))); 

		speed = Math.max(0.001, speed);
		return length/speed;
	}
	
	private double computeKey(double loc, double level) {
		//pairing function from
		//Stephen Wolfram, A new kind of science, Wolfram Media, 2002.
		return loc < level ? level*level + loc : loc*loc + loc + level;
	}

	private void expand(Quad v, List<Quad> expanded, int direction) {
		Quad n = v.getNeighbor(direction);
		if (n == null) {
			return;
		}
		
		if (n.getEnvelope().getMinX()>=this.e.getMaxX() || n.getEnvelope().getMaxX() <= this.e.getMinX()) {
			return;
		}
		
//		if (n.getColor())
		if (n.getColor() <= 1){
			expanded.add(n);
			return;
		}
		if (direction == LinearQuadTreeLD.D_WEST) {
			addEastMostChilds(n,expanded);
		} else if (direction == LinearQuadTreeLD.D_SOUTH) {
			addNorthMostChilds(n,expanded);
		} else if (direction == LinearQuadTreeLD.D_EAST) {
			addWestMostChilds(n,expanded);
		} if (direction == LinearQuadTreeLD.D_NORTH) {
			addSouthMostChilds(n,expanded);
		} 
		
	}

	private void addEastMostChilds(Quad n, List<Quad> expanded) {
		if (n.getColor() <= 1) {
			expanded.add(n);
			return;
		}
		addEastMostChilds(n.getNEChild(), expanded);
		addEastMostChilds(n.getSEChild(), expanded);
	}
	private void addWestMostChilds(Quad n, List<Quad> expanded) {
		if (n.getColor() <= 1) {
			expanded.add(n);
			return;
		}
		addWestMostChilds(n.getNWChild(), expanded);
		addWestMostChilds(n.getSWChild(), expanded);
	}
	private void addNorthMostChilds(Quad n, List<Quad> expanded) {
		if (n.getColor() <= 1) {
			expanded.add(n);
			return;
		}
		addNorthMostChilds(n.getNEChild(), expanded);
		addNorthMostChilds(n.getNWChild(), expanded);
	}
	private void addSouthMostChilds(Quad n, List<Quad> expanded) {
		if (n.getColor() <= 1) {
			expanded.add(n);
			return;
		}
		addSouthMostChilds(n.getSWChild(), expanded);
		addSouthMostChilds(n.getSEChild(), expanded);
	}

	private LinkedList<Quad> reconstructPath(Quad v) {
		LinkedList<Quad> ret = new LinkedList<Quad>();
		NodeData nd = this.nodesData.get(v);
		while (nd.predecessor != null) {
			ret.addFirst(v);
			v=nd.predecessor;
			nd = this.nodesData.get(v);
		}
		return ret;
	}
	
	private static final class NodeData {
		Quad predecessor;
		double cost;
	}

}
