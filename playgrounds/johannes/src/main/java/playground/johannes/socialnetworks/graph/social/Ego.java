/* *********************************************************************** *
 * project: org.matsim.*
 * Ego.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.social;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * @author illenberger
 *
 */
public class Ego<P extends Person> extends SpatialSparseVertex {

	private static final Logger logger = Logger.getLogger(Ego.class);
	private P person;
	
	private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 21781);
	
	protected Ego(P person) {
		super(coord2Point(((Activity) person.getPlans().get(0).getPlanElements().get(0)).getCoord()));
		this.person = person;
	}
	
	public P getPerson() {
		return person;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends Ego<P>> getNeighbours() {
		return (List<? extends Ego<P>>) super.getNeighbours();
	}
	
	private static Point coord2Point(Coord coord) {
		logger.warn("Assuming CH1903LV03 coordinate referenc system.");
		return geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY()));
	}
}
