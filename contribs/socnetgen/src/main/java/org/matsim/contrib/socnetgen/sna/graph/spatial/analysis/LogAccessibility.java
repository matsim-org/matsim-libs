/* *********************************************************************** *
 * project: org.matsim.*
 * Accessability.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.analysis;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.gis.SpatialCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.math.Distribution;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class LogAccessibility {
	
	private static LogAccessibility instance;
	
	public static LogAccessibility getInstance() {
		if(instance == null)
			instance = new LogAccessibility();
		return instance;
	}
	
	public Distribution distribution(Set<? extends SpatialVertex> vertices, SpatialCostFunction costFunction, Set<Point> opportunities) {
		TObjectDoubleHashMap<SpatialVertex> values = values(vertices, costFunction, opportunities);
		TObjectDoubleIterator<SpatialVertex> it = values.iterator();
		Distribution distr = new Distribution();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			distr.add(it.value());
		}
		
		return distr;
	}

	@SuppressWarnings("unchecked")
	public TObjectDoubleHashMap<SpatialVertex> values(Set<? extends SpatialVertex> vertices, SpatialCostFunction costFunction, Set<Point> opportunities) {
		Set<SpatialVertex> spatialVertices = (Set<SpatialVertex>) vertices;
		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>(spatialVertices.size());

		ProgressLogger.init(spatialVertices.size(), 1, 5);
		for (SpatialVertex vertex : spatialVertices) {
			if (vertex.getPoint() != null) {
				double sum = 0;
				for (Point point : opportunities) {
					if (point != null) {
						double c = costFunction.costs(vertex.getPoint(), point);
						sum += Math.exp(-c);
					}
				}
				values.put(vertex, Math.log(sum));
				ProgressLogger.step();
			}
		}

		return values;
	}
}
