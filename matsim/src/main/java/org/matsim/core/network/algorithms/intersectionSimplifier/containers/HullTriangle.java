/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package org.matsim.core.network.algorithms.intersectionSimplifier.containers;

import java.util.ArrayList;
import java.util.List;

public class HullTriangle {
	/** Id of the triangle */
	private int id;

	/** Edges which compose the triangle */
	private List<HullEdge> edges = new ArrayList<HullEdge>();
	
	/** Neighbour triangles of this triangle */
	private List<HullTriangle> neighbours = new ArrayList<HullTriangle>();

	// vertices...

	/**
	 * Default constructor.
	 */
	public HullTriangle() {
		//
	}
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		Id of the triangle
	 */
	public HullTriangle(int id) {
		this.id = id;
	}


	/**
	 * Returns the ID of the triangle.
	 * 
	 * @return
	 * 		the ID of the triangle
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Defines the ID of the triangle.
	 * 
	 * @param id
	 * 		ID of the triangle
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the edges which compose the triangle.
	 * 
	 * @return
	 * 		the edges of the triangle which compose the triangle
	 */
	public List<HullEdge> getEdges() {
		return this.edges;
	}

	/**
	 * Defines the edges which compose the triangle.
	 * 
	 * @param edges
	 * 		the edges which compose the triangle
	 */
	public void setEdges(List<HullEdge> edges) {
		this.edges = edges;
	}

	/**
	 * Returns the neighbour triangles of the triangle.
	 * 
	 * @return
	 * 		the neighbour triangles of the triangle
	 */
	public List<HullTriangle> getNeighbours() {
		return this.neighbours;
	}

	/**
	 * Defines the neighbour triangles of the triangle.
	 * 
	 * @param neighbours
	 * 		the neighbour triangles of the triangle
	 */
	public void setNeighbours(List<HullTriangle> neighbours) {
		this.neighbours = neighbours;
	}


	/**
	 * Add an edge to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addEdge(HullEdge edge) {
		return getEdges().add(edge);
	}

	/**
	 * Add edges to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addEdges(List<HullEdge> edges) {
		return getEdges().addAll(edges);
	}

	/**
	 * Remove an edge of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeEdge(HullEdge edge) {
		return getEdges().remove(edge);
	}

	/**
	 * Remove edges of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeEdges(List<HullEdge> edges) {
		return getEdges().removeAll(edges);
	}
	
	
	/**
	 * Add a neighbour triangle to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addNeighbour(HullTriangle triangle) {
		return getNeighbours().add(triangle);
	}

	/**
	 * Add neighbour triangles to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addNeighbours(List<HullTriangle> triangles) {
		return getNeighbours().addAll(triangles);
	}

	/**
	 * Remove a neighbour triangle of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeNeighbour(HullTriangle triangle) {
		return getNeighbours().remove(triangle);
	}

	/**
	 * Remove neighbour triangles of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeNeighbours(List<HullTriangle> triangles) {
		return getNeighbours().removeAll(triangles);
	}
}
