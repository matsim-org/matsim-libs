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

package playground.southafrica.freight.digicore.algorithms.concaveHull;

import java.util.ArrayList;
import java.util.List;

public class Triangle {
	/** ID of the triangle */
	private int id;

	/** Edges which compose the triangle */
	private List<Edge> edges = new ArrayList<Edge>();
	
	/** Neighbour triangles of this triangle */
	private List<Triangle> neighbours = new ArrayList<Triangle>();

	// vertices...

	/**
	 * Default constructor.
	 */
	public Triangle() {
		//
	}
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the triangle
	 */
	public Triangle(int id) {
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
	public List<Edge> getEdges() {
		return this.edges;
	}

	/**
	 * Defines the edges which compose the triangle.
	 * 
	 * @param edges
	 * 		the edges which compose the triangle
	 */
	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}

	/**
	 * Returns the neighbour triangles of the triangle.
	 * 
	 * @return
	 * 		the neighbour triangles of the triangle
	 */
	public List<Triangle> getNeighbours() {
		return this.neighbours;
	}

	/**
	 * Defines the neighbour triangles of the triangle.
	 * 
	 * @param neighbours
	 * 		the neighbour triangles of the triangle
	 */
	public void setNeighbours(List<Triangle> neighbours) {
		this.neighbours = neighbours;
	}


	/**
	 * Add an edge to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addEdge(Edge edge) {
		return getEdges().add(edge);
	}

	/**
	 * Add edges to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addEdges(List<Edge> edges) {
		return getEdges().addAll(edges);
	}

	/**
	 * Remove an edge of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeEdge(Edge edge) {
		return getEdges().remove(edge);
	}

	/**
	 * Remove edges of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeEdges(List<Edge> edges) {
		return getEdges().removeAll(edges);
	}
	
	
	/**
	 * Add a neighbour triangle to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addNeighbour(Triangle triangle) {
		return getNeighbours().add(triangle);
	}

	/**
	 * Add neighbour triangles to the triangle.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addNeighbours(List<Triangle> triangles) {
		return getNeighbours().addAll(triangles);
	}

	/**
	 * Remove a neighbour triangle of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeNeighbour(Triangle triangle) {
		return getNeighbours().remove(triangle);
	}

	/**
	 * Remove neighbour triangles of the triangle.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeNeighbours(List<Triangle> triangles) {
		return getNeighbours().removeAll(triangles);
	}
}
