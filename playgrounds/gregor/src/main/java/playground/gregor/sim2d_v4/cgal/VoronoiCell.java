/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiCell.java
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

package playground.gregor.sim2d_v4.cgal;

import java.util.ArrayList;
import java.util.List;

import be.humphreys.simplevoronoi.GraphEdge;

public class VoronoiCell {

	private double area = 0;
	private final int idx;
	private final VoronoiCenter c;
	private final List<VoronoiCenter> neighbors = new ArrayList<VoronoiCenter>();
	private final List<GraphEdge> edges = new ArrayList<GraphEdge>();
	private boolean isClosed = false;

	
	public VoronoiCell(VoronoiCenter c, int idx) {
		this.c = c;
		this.idx = idx;
	}

	
	public int getIdx() {
		return this.idx;
	}
	
	public double getPointX() {
		return this.c.getX();
	}
	public double getPointY() {
		return this.c.getY();
	}
	
	public double getArea() {
		return this.area;
	}
	
	/*package*/ void incrementAreaBy(double incr) {
		this.area += incr;
	}
	
	public void addNeighbor(VoronoiCenter n) {
		this.neighbors.add(n);
	}
	
	public List<VoronoiCenter> getNeighbors() {
		return this.neighbors;
	}
	
	public VoronoiCenter getVoronoiCenter(){
		return this.c;
	}


	public void addGraphEdge(GraphEdge ed) {
		this.edges .add(ed);
		
	}


	public List<GraphEdge> getGraphEdges() {
		return this.edges;
	}


	public void setIsClosed(boolean b) {
		this.isClosed = b;
	}
	
	public boolean isClosed() {
		return this.isClosed;
	}
}
