/* *********************************************************************** *
 * project: org.matsim.*
 * VertexPropertyCorrelation.java
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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.VertexProperty;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class VertexPropertyCorrelation {

	public static <V extends Vertex> TDoubleDoubleHashMap mean(VertexProperty<V> propY, VertexProperty<V> porpX, Set<? extends V> vertices) {
		return mean(propY, porpX, vertices, new LinearDiscretizer(1.0));
	}
	
	public static <V extends Vertex> TDoubleDoubleHashMap mean(VertexProperty<V> propY, VertexProperty<V> propX, Set<? extends V> vertices, Discretizer discretizer) {
		TObjectDoubleHashMap<V> propValuesX = propX.values(vertices);
		
		Set<V> filtered = new HashSet<V>();
		TObjectDoubleIterator<V> it = propValuesX.iterator();
		for(int i = 0; i < propValuesX.size(); i++) {
			it.advance();
			filtered.add(it.key());
		}
		TObjectDoubleHashMap<V> propValuesY = propY.values(filtered);
		
		return mean(propValuesY, propValuesX, filtered, discretizer);
	}
	
	public static <V extends Vertex> TDoubleDoubleHashMap mean(TObjectDoubleHashMap<V> propValuesY, TObjectDoubleHashMap<V> propValuesX, Set<? extends V> vertices, Discretizer discretizer) {
		double[] valuesY = new double[propValuesY.size()];
		double[] valuesX = new double[propValuesX.size()];
		int i = 0;
		for(V vertex : vertices) {
			valuesY[i] = propValuesY.get(vertex);
			valuesX[i] = discretizer.discretize(propValuesX.get(vertex));
			i++;
		}
		
		return Correlations.correlationMean(valuesX, valuesY);
	}
}
