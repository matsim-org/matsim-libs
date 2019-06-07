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

import org.locationtech.jts.geom.Coordinate;

public class HullNode {
	/** ID of the vertex */
	private int id;

	/** Coordinate of the vertex */
	private Coordinate coordinate;

	/** Indicator to know if the vertex is a border vertex
	 * of the triangulation framework */
	private boolean border;


	/**
	 * Constructor.
	 * 
	 * @param id
	 * 		ID of the vertex
	 * @param coordinate
	 * 		coordinate of the vertex
	 */
	public HullNode(int id, Coordinate coordinate) {
		this.id = id;
		this.setCoordinate(coordinate);
		this.border = false; /* Default is false. */
	}


	/**
	 * Returns the ID of the vertex.
	 * 
	 * @return
	 * 		the ID of the vertex
	 */	
	public int getId() {
		return this.id;
	}

	/**
	 * Defines the ID of the vertex.
	 * 
	 * @param id
	 * 		the ID of the vertex
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the coordinate of the vertex.
	 * 
	 * @return
	 * 		the coordinate of the vertex
	 */
	public Coordinate getCoordinate() {
		return this.coordinate;
	}

	/**
	 * Defines the coordinate of the vertex.
	 * 
	 * @param c
	 * 		the coordinate of the vertex
	 */
	public void setCoordinate(Coordinate c) {
		this.coordinate = c;
	}

	/**
	 * Returns true if the vertex is a border vertex
	 * of the triangulation framework, false otherwise.
	 * 
	 * @return
	 * 		true if the vertex is a border vertex,
	 * 		false otherwise
	 */
	public boolean isBorder() {
		return this.border;
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
	}
}
