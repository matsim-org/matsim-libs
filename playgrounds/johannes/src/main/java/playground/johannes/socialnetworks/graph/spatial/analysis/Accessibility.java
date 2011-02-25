/* *********************************************************************** *
 * project: org.matsim.*
 * Accessibility.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.util.ProgressLogger;

import playground.johannes.socialnetworks.gis.SpatialCostFunction;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class Accessibility extends AbstractSpatialProperty {

	private static final Logger logger = Logger.getLogger(Accessibility.class);
	
	private final SpatialCostFunction function;
	
	public Accessibility(SpatialCostFunction function) {
		this.function = function;
	}
	
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		logger.info("Calculating accessibility...");
		
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>(vertices.size());
		@SuppressWarnings("unchecked")
		Set<? extends SpatialVertex> spatialVertices = (Set<? extends SpatialVertex>) vertices;
		Set<Point> targets = getTargets(spatialVertices);
		
		ProgressLogger.init(vertices.size(), 1, 5);
		
		for(SpatialVertex vertex : spatialVertices) {
			Point origin = vertex.getPoint();
			if(origin != null) {
				double sum = 0;
				for(Point target : targets) {
					if(origin != target) {
						sum += Math.exp(- function.costs(origin, target));
					}
				}
				
				values.put(vertex, sum);
				
				ProgressLogger.step();
			}
		}
		
		return values;
	}

	

}
