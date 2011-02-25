/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmLnDistance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ErgmLnDistance extends ErgmTerm {

	private Discretizer discretizer = new LinearDiscretizer(1.0);
	
	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();
	
	public ErgmLnDistance(AdjacencyMatrix<? extends SpatialVertex> y, double theta) {
		this.setTheta(theta);
	}
	
	@Override
	public <V extends Vertex> double ratio(AdjacencyMatrix<V> y, int i, int j, boolean yIj) {
		Point p1 = ((SpatialVertex) y.getVertex(i)).getPoint();
		Point p2 = ((SpatialVertex) y.getVertex(j)).getPoint();
		double d = distanceCalculator.distance(p1, p2);
		d = discretizer.index(d);
		d = Math.max(1.0, d);
		return Math.pow(d, - getTheta());
	}

}
