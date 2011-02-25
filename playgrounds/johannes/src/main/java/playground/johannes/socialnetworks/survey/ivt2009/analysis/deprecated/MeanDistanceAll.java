/* *********************************************************************** *
 * project: org.matsim.*
 * MeanDistanceAll.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AbstractVertexProperty;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.util.ProgressLogger;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class MeanDistanceAll extends AbstractVertexProperty {

	private static final Logger logger = Logger.getLogger(MeanDistanceAll.class);
	
	private Set<Point> destinations;
	
	public void setDestinations(Set<Point> destinations) {
		this.destinations = destinations;
	}
	
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		Set<? extends SpatialVertex> spatialVertices = (Set<? extends SpatialVertex>)vertices;
		DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>();
		logger.info("Calculating mean distance to all destinations...");
		ProgressLogger.init(spatialVertices.size(), 1, 5);
		for(SpatialVertex vertex : spatialVertices) {
			Point p1 = vertex.getPoint();
			if(p1 != null) {
				double sum = 0;
				int cnt = 0;
				for(Point p2 : destinations) {
					if(p2 != null) {
						double d = distanceCalculator.distance(p1, p2);
						sum += d;
						cnt++;
					}
				}
				values.put(vertex, sum/(double)cnt);
			}
			ProgressLogger.step();
		}
		return values;
	}

}
