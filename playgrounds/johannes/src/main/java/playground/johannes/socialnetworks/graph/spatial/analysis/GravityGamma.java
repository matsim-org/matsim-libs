/* *********************************************************************** *
 * project: org.matsim.*
 * GravityGamma.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;

/**
 * @author illenberger
 *
 */
public class GravityGamma {

	private Discretizer discretizer = new LinearDiscretizer(1000.0);
	
	private DistanceCalculator calculator = new OrthodromicDistanceCalculator();
	
	private final double constant = 1.0;
	
	private final double budget = 65.0;
	
	public Distribution distribution(Set<? extends SpatialVertex> vertices) {
		Distribution distr = new Distribution();
		distr.addAll(values(vertices).getValues());
		return distr;
	}
	
	public TObjectDoubleHashMap<SpatialVertex> values(Set<? extends SpatialVertex> vertices) {
		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>(vertices.size());
		double totalsum = 0;
		double totalconst = 0;
		for(SpatialVertex vertex : vertices) {
			double sum = 0;
			for(SpatialVertex neighbor : vertex.getNeighbours()) {
				double d = Math.max(1.0, discretizer.index(calculator.distance(vertex.getPoint(), neighbor.getPoint())));
				sum += Math.log(d);
			}
			
			double val = (budget - vertex.getNeighbours().size() * constant) / sum;
			values.put(vertex, val);
			
			totalsum += sum;
			totalconst += vertex.getNeighbours().size() * constant;
		}
		
		double gamma = (vertices.size() * budget - totalconst)/totalsum;
		System.err.println("gamma = " + gamma);
		return values;
	}
}
