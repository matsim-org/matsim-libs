/* *********************************************************************** *
 * project: org.matsim.*
 * VisibilityGraph.java
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

package playground.gregor.sim2denvironment.approxdecomp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playground.gregor.sim2denvironment.Algorithms;
import playground.gregor.sim2denvironment.approxdecomp.ApproxConvexDecomposer.PocketBridge;
import playground.gregor.sim2denvironment.approxdecomp.Graph.Link;
import playground.gregor.sim2denvironment.approxdecomp.Graph.Node;

import com.vividsolutions.jts.geom.Coordinate;

public class VisibilityGraph {

	private final ShortestPath aStar = new ShortestPath();
//	private final Coordinate[] shell;
	private final PocketBridge pb;
	
	private final Map<Integer,Node> nodes = new HashMap<Integer, Node>(); 
	private final List<Link> links = new ArrayList<Link>();

	private final Coordinate[] pocket;

	private final Coordinate[] pocketRing;

	private final Coordinate bMinus;

	private final Coordinate bPlus;

	private final Coordinate bMinusA;

	private final Coordinate bPlusB;

	private double superLength;
	private Node superNode;
	private Node betaMinus;
	private Node betaPlus;

	public VisibilityGraph(Coordinate[] shell, PocketBridge pb) {
		this.bMinus = shell[pb.betaMinus%shell.length];
		this.bPlus = shell[pb.betaPlus%shell.length];
		double dx = this.bPlus.x - this.bMinus.x;
		double dy = this.bPlus.y - this.bMinus.y;
		
		this.bMinusA = new Coordinate(this.bMinus.x + dy, this.bMinus.y - dx);
		this.bPlusB = new Coordinate(this.bPlus.x + dy, this.bPlus.y - dx);
		
		
		

		Coordinate [] pocket = new Coordinate[pb.betaPlus-pb.betaMinus+1];
		Coordinate [] pocketRing = new Coordinate[pb.betaPlus-pb.betaMinus+2];
		for (int i = 0; i < pocket.length; i++) {
			pocket[i] = shell[(i+pb.betaMinus)%shell.length];
			pocketRing[i] = pocket[i];
		}
		pocketRing[pocketRing.length-1] = pocketRing[0];
		this.pocketRing = pocketRing;
		this.pocket = pocket;
		
		this.pb = pb;
		build();
		
		
	
		//DEBUG
//		dumpGraph();
	}
	
	
	
	public double computeSPLength(int v) {
		double ret = 0;

		int idx = v - this.pb.betaMinus;
		Node n = this.nodes.get(idx);
		
		double onset = 0;
		Node target;
		if (Algorithms.isLeftOfLine(n.c, this.bMinus,this.bMinusA) <= 0){
			//beta minus;
			target = this.betaMinus;
		} else if (Algorithms.isLeftOfLine(n.c, this.bPlus,this.bPlusB) >= 0){
			//beta plus;
			target = this.betaPlus;
		} else {
			target = this.superNode;
			onset = this.superLength;
		}
		
		ret = this.aStar.getCost(n, target);
		
		return ret-onset;
	}




	private void build() {
		
		//create nodes
		for (int i = 0; i < this.pocket.length; i++) {
			createNode(i);
		}
		
		this.betaMinus = this.nodes.get(0);
		this.betaPlus = this.nodes.get(this.pocket.length-1);
		
		
		//create links
		for (int i = 0; i < this.pocket.length-1; i++) {
			for (int j = i+1; j < this.pocket.length; j++) {
				
				Node n0 = this.nodes.get(i);
				Node n1 = this.nodes.get(j);
				if (j-i == 1) {
					createLink(n0,n1);
				} else {
					tryCreateLink(n0,n1);
				}
			}
		}


		//super node
		Coordinate c = new Coordinate((this.bMinus.x+this.bPlus.x)/2,(this.bMinus.y+this.bPlus.y)/2);
		this.superNode = new Node(c,Integer.MIN_VALUE);
		this.nodes.put(Integer.MIN_VALUE, this.superNode);
		this.superLength = this.bMinus.distance(this.bPlus);
		
		//create perpendicular visibility lines
		for (int i = 1; i < this.pocket.length-1; i++) {
			Node nn = this.nodes.get(i);
			tryCreatePerpendicularLine(nn);
		}
		
	}



	private void tryCreatePerpendicularLine(Node nn) {
		if (Algorithms.isLeftOfLine(nn.c, this.bMinus,this.bMinusA) <= 0){
			return;
		}
		if (Algorithms.isLeftOfLine(nn.c, this.bPlus,this.bPlusB) >= 0){
			return;
		}
		
		
	    double vx = this.bPlus.x - this.bMinus.x;
	    double vy = this.bPlus.y - this.bMinus.y;
	    double wx = nn.c.x - this.bPlus.x;
	    double wy = nn.c.y - this.bPlus.y;
	    
	    //dot prod
	    double c1 = wx * vx + wy*vy;
	    double c2 = vx*vx + vy*vy;
	    double b = c1 / c2;
	    
	    double px = this.bPlus.x + b * vx;
	    double py = this.bPlus.y + b * vy;
	    Coordinate c = new Coordinate(px,py);
	    
		Node n = new Node(c,-nn.id);
		this.nodes.put(n.id, n);
		if (tryCreateLink(nn, n)) {
			Link l0 = new Link(n,this.superNode);
			l0.setLength(this.superLength);
			n.addOutLink(l0);
			this.links.add(l0);
		}
	    
	}


////TODO
//	private boolean tryCreateLinkFast(Node n0, Node n1) {
//		Coordinate c0 = n0.c;
//		Coordinate c1 = n1.c;
//
//		for (int i = 0; i < this.pocket.length-1; i++) {
//			Coordinate c2 = this.pocket[i];
//			Coordinate c3 = this.pocket[i+1];
//			double val0 = Algorithms.isLeftOfLine(c2, c0, c1);
//			double val1 = Algorithms.isLeftOfLine(c3, c0, c1);
//			if (val0*val1 < 0 ) {
//				double val2 = Algorithms.isLeftOfLine(c0, c2, c3);
//				double val3 = Algorithms.isLeftOfLine(c1, c2, c3);
//				if (val2*val3 < 0) {
//					return false;
//				}
//			}
//			
//			
//			
//
//			
//		}
	//replace this method by a "right of line segment" test for line segments {(n0.id,n0.id+1),...(n1.id-1,n1.id)} to become faster 
////		Coordinate m = new Coordinate((c0.x+c1.x)/2,(c0.y+c1.y)/2);
////		if (!Algorithms.contains(m, this.pocketRing)){
////			return false;
////		}
//		
//		createLink(n0,n1);
//		
//		return true;
//	}



	private boolean tryCreateLink(Node n0, Node n1) {
		Coordinate c0 = n0.c;
		Coordinate c1 = n1.c;

		for (int i = 0; i < this.pocket.length-1; i++) {
			Coordinate c2 = this.pocket[i];
			Coordinate c3 = this.pocket[i+1];
			double val0 = Algorithms.isLeftOfLine(c2, c0, c1);
			double val1 = Algorithms.isLeftOfLine(c3, c0, c1);
			if (val0*val1 < 0 ) {
				double val2 = Algorithms.isLeftOfLine(c0, c2, c3);
				double val3 = Algorithms.isLeftOfLine(c1, c2, c3);
				if (val2*val3 < 0) {
					return false;
				}
			}
			
		}
		
		Coordinate m = new Coordinate((c0.x+c1.x)/2,(c0.y+c1.y)/2);
		if (!Algorithms.contains(m, this.pocketRing)){
			return false;
		}
		createLink(n0,n1);
		
		return true;
		
	}
	
	private void createLink(Node n0, Node n1) {
		Link l0 = new Link(n0,n1);
		n0.addOutLink(l0);
		Link l1 = new Link(n1,n0);
		n1.addOutLink(l1);
		this.links.add(l0);
		this.links.add(l1);
	}





	private Node createNode(int id) {
		Coordinate c = this.pocket[id];
		Node n = new Node(c,id);
		this.nodes.put(id, n);
		return n;
	}



	
//	//DEBUG
//	private void dumpGraph() {
//		GeometryFactory geofac = new GeometryFactory();
//		for (Link l : this.links) {
//			Coordinate c0 = l.n0.c;
//			Coordinate c1 = l.n1.c;
//			LineString ls = geofac.createLineString(new Coordinate[]{c0,c1});
//			GisDebugger.addGeometry(ls);
//		}
//		GisDebugger.dump("/Users/laemmel/devel/convexdecomp/vgraph.shp");
//	}
}
