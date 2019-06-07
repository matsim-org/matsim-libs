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

import org.locationtech.jts.geom.LineSegment;

public class HullEdge {
	/** ID of the edge */
	private int id;
	
	/** Geometry of the edge */
	private LineSegment geometry;

	/** Indicator to know if the edge is a border edge
	 *  of the triangulation framework */
	private boolean border;

	/** Origin vertex of the edge */
	private HullNode originNode;
	
	/** End vertex of the edge */
	private HullNode destinationNode;

	/** Triangles in relationship with this edge */
	private List<HullTriangle> triangles = new ArrayList<HullTriangle>();
	
	/** Edges in relationship with this edge */
	private List<HullEdge> incidentEdges = new ArrayList<HullEdge>();

	
	/**
	 * Default constructor.
	 */
	public HullEdge() {
		//
	}
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 */
	public HullEdge(int id) {
		this.id = id;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param geometry
	 * 		geometry of the edge (segment)
	 */
	public HullEdge(int id, LineSegment geometry) {
		this.id = id;
		this.geometry = geometry;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param border
	 * 		defines if the edge is a border edge
	 * 		or not in the triangulation framework
	 */
	public HullEdge(int id, boolean border) {
		this.id = id;
		this.border = border;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param geometry
	 * 		geometry of the edge (segment)
 	 * @param border
	 * 		defines if the edge is a border edge
	 * 		or not in the triangulation framework
	 */
	public HullEdge(int id, LineSegment geometry, boolean border) {
		this.id = id;
		this.geometry = geometry;
		this.border = border;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the edge
	 * @param geometry
	 * 		geometry of the edge (segment)
	 * @param oV
	 * 		origin vertex
	 * @param eV
	 * 		end vertex
	 * @param border
	 * 		defines if the edge is a border edge
	 * 		or not in the triangulation framework
	 */
	public HullEdge(int id, LineSegment geometry, HullNode oV, HullNode eV, boolean border) {
		this.id = id;
		this.geometry = geometry;
		this.originNode = oV;
		this.destinationNode = eV;
		this.border = border;
	}

	
	/**
	 * Returns the ID of the edge.
	 * 
	 * @return
	 * 		the ID of the edge
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Defines the ID of the edge.
	 * 
	 * @param id
	 * 		ID of the edge
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the geometry of the edge.
	 * 
	 * @return
	 * 		the geometry of the edge
	 */
	public LineSegment getGeometry() {
		return this.geometry;
	}

	/**
	 * Defines the geometry of the edge.
	 * 
	 * @param geometry
	 * 		geometry of the edge (segment)
	 */
	public void setGeometry(LineSegment geometry) {
		this.geometry = geometry;
	}

	/**
	 * Returns true if the edge is a border edge
	 * of the triangulation framework, false otherwise.
	 * 
	 * @return
	 * 		true if the edge is a border edge,
	 * 		false otherwise
	 */
	public boolean isBorder() {
		return this.border;
		/*TODO Change this so that it checks the number of triangles with which
		 * the edge is associated. At least throw a warning if the boolean
		 * attribute remains, by checking the number of related triangles before
		 * returning the boolean value. */
	}

	/**
	 * Defines the indicator to know if the edge
	 * is a border edge of the triangulation framework.
	 * 
	 * @param border
	 * 		true if the edge is a border edge,
	 * 		false otherwise
	 */
	public void setBorder(boolean border) {
		this.border = border;
		
		/* If an edge is changed to a border edge, also flag both origin and 
		 * destination node as border nodes. The opposite, however, is NOT true:
		 * that is, if an edge is an internal edge, one (or both) of its nodes
		 * may be border nodes.
		 */
		if(border == true){
			this.originNode.setBorder(true);
			this.destinationNode.setBorder(true);
		}
	}
	
	/**
	 * Returns the origin vertex of the edge.
	 * 
	 * @return
	 * 		the origin vertex of the edge
	 */	
	public HullNode getOriginNode() {
		return this.originNode;
	}

	/**
	 * Defines the origin vertex of the edge.
	 * 
	 * @param originNode
	 * 		origin vertex of the edge
	 */
	public void setOriginNode(HullNode originNode) {
		this.originNode = originNode;
	}

	/**
	 * Returns the end vertex of the edge.
	 * 
	 * @return
	 * 		the end vertex of the edge
	 */	
	public HullNode getDestinationNode() {
		return this.destinationNode;
	}

	/**
	 * Defines the end vertex of the edge.
	 * 
	 * @param destinationNodeV
	 * 		end vertex of the edge
	 */
	public void setDestinationNode(HullNode destinationNodeV) {
		this.destinationNode = destinationNodeV;
	}
	
	/**
	 * Returns the triangles in relationship with the edge.
	 * 
	 * @return
	 * 		the triangles in relationship with the edge
	 */
	public List<HullTriangle> getTriangles() {
		return this.triangles;
	}

	/**
	 * Defines the triangles in relationship with the edge.
	 * 
	 * @param triangles
	 * 		the triangles in relationship with the edge
	 */
	public void setTriangles(List<HullTriangle> triangles) {
		this.triangles = triangles;
	}

	/**
	 * Returns the edges in relationship with the edge.
	 * 
	 * @return
	 * 		the edges in relationship with the edge
	 */
	public List<HullEdge> getIncidentEdges() {
		return this.incidentEdges;
	}

	/**
	 * Defines the edges in relationship with the edge.
	 * 
	 * @param edges
	 * 		the edges in relationship with the edge
	 */
	public void setIncidentEdges(List<HullEdge> edges) {
		this.incidentEdges = edges;
	}

	/**
	 * Add a triangle in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addTriangle(HullTriangle triangle) {
		return getTriangles().add(triangle);
	}

	/**
	 * Add triangles in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addTriangles(List<HullTriangle> triangles) {
		return getTriangles().addAll(triangles);
	}

	/**
	 * Remove a triangle in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeTriangle(HullTriangle triangle) {
		return getTriangles().remove(triangle);
	}

	/**
	 * Remove triangles in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeTriangles(List<HullTriangle> triangles) {
		return getTriangles().removeAll(triangles);
	}

	/**
	 * Add an incident edge in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addIncidentEdge(HullEdge edge) {
		return getIncidentEdges().add(edge);
	}

	/**
	 * Add incident edges in relationship with the edge.
	 * 
	 * @return
	 * 		true if added, false otherwise
	 */
	public boolean addIncidentEdges(List<HullEdge> edges) {
		return getIncidentEdges().addAll(edges);
	}

	/**
	 * Remove an incident edge in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeIncidentEdge(HullEdge edge) {
		return getIncidentEdges().remove(edge);
	}

	/**
	 * Remove incident edges in relationship with the edge.
	 * 
	 * @return
	 * 		true if removed, false otherwise
	 */
	public boolean removeAllIncidentEdges(List<HullEdge> edges) {
		return getIncidentEdges().removeAll(edges);
	}
}
