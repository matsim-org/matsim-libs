/* *********************************************************************** *
 * project: org.matsim.*
 * SparseVertex.java
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

/**
 * 
 */
package playground.johannes.snowball2;

/**
 * @author illenberger
 *
 */
public class SparseVertex {

	private SparseEdge[] edges = new SparseEdge[0];
	
	private SparseVertex[] neighbours = new SparseVertex[0];
	
	protected void addEdge(SparseEdge e) {
		SparseEdge[] newEdges = new SparseEdge[edges.length + 1];
		SparseVertex[] newNeighbours = new SparseVertex[edges.length + 1];
		
		for(int i = 0; i < edges.length; i++) {
			newEdges[i] = edges[i];
			newNeighbours[i] = neighbours[i];
		}
		
		newEdges[edges.length] = e;
		newNeighbours[neighbours.length] = e.getOpposite(this);
		
		edges = newEdges;
		neighbours = newNeighbours;	
	}
	
	public SparseEdge[] getEdges() {
		return edges;
	}
	
	public SparseVertex[] getNeighbours() {
		return neighbours;
	}
}
