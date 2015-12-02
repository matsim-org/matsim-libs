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
package org.matsim.contrib.socnetgen.sna.graph.analysis;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.DummyDiscretizer;
import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class VertexPropertyCorrelation {

	public static TDoubleDoubleHashMap mean(VertexProperty propY, VertexProperty porpX, Set<? extends Vertex> vertices) {
		return mean(propY, porpX, vertices, new DummyDiscretizer());
	}
	
	public static TDoubleDoubleHashMap mean(EdgeProperty propY, EdgeProperty porpX, Set<? extends Edge> edges) {
		return mean(propY, porpX, edges, new DummyDiscretizer());
	}
	
	public static TDoubleDoubleHashMap mean(VertexProperty propY, VertexProperty propX, Set<? extends Vertex> vertices, Discretizer discretizer) {
		TObjectDoubleHashMap<Vertex> propValuesX = propX.values(vertices);
		Set<Vertex> filtered = filter(propValuesX);
		TObjectDoubleHashMap<Vertex> propValuesY = propY.values(filtered);
		
		return mean(propValuesY, propValuesX, discretizer);
	}
	
	public static TDoubleDoubleHashMap mean(EdgeProperty propY, EdgeProperty propX, Set<? extends Edge> edges, Discretizer discretizer) {
		TObjectDoubleHashMap<Edge> propValuesX = propX.values(edges);
		Set<Edge> filtered = filter(propValuesX);
		TObjectDoubleHashMap<Edge> propValuesY = propY.values(filtered);
		
		return mean(propValuesY, propValuesX, discretizer);
	}
	
	private static <T> Set<T> filter(TObjectDoubleHashMap<T> propValuesX) {
		Set<T> filtered = new HashSet<T>();
		TObjectDoubleIterator<T> it = propValuesX.iterator();
		for(int i = 0; i < propValuesX.size(); i++) {
			it.advance();
			filtered.add(it.key());
		}
		return filtered;
	}
	
	public static <T> TDoubleDoubleHashMap mean(TObjectDoubleHashMap<T> propValuesY, TObjectDoubleHashMap<T> propValuesX, Discretizer discretizer) {
		TDoubleArrayList valuesY = new TDoubleArrayList(propValuesX.size());
		TDoubleArrayList valuesX = new TDoubleArrayList(propValuesX.size());

		discretizeValues(propValuesY, propValuesX, valuesX, valuesY, discretizer);
		
		return Correlations.mean(valuesX.toArray(), valuesY.toArray());
	}
	
	public static TDoubleObjectHashMap<DescriptiveStatistics> statistics(VertexProperty propY, VertexProperty porpX, Set<? extends Vertex> vertices) {
		return statistics(propY, porpX, vertices, new DummyDiscretizer());
	}
	
	public static TDoubleObjectHashMap<DescriptiveStatistics> statistics(VertexProperty propY, VertexProperty propX, Set<? extends Vertex> vertices, Discretizer discretizer) {
		TObjectDoubleHashMap<Vertex> propValuesX = propX.values(vertices);
		Set<Vertex> filtered = filter(propValuesX);
		TObjectDoubleHashMap<Vertex> propValuesY = propY.values(filtered);
		
		return statistics(propValuesY, propValuesX, discretizer);
	}
	
	public static <T> TDoubleObjectHashMap<DescriptiveStatistics> statistics(TObjectDoubleHashMap<T> propValuesY, TObjectDoubleHashMap<T> propValuesX, Discretizer discretizer) {
		TDoubleArrayList valuesY = new TDoubleArrayList(propValuesX.size());
		TDoubleArrayList valuesX = new TDoubleArrayList(propValuesX.size());
		
		discretizeValues(propValuesY, propValuesX, valuesX, valuesY, discretizer);
		
		return Correlations.statistics(valuesX.toArray(), valuesY.toArray(), discretizer);
	}
	
	private static <T> void discretizeValues(TObjectDoubleHashMap<T> propValuesY, TObjectDoubleHashMap<T> propValuesX, TDoubleArrayList valuesX, TDoubleArrayList valuesY, Discretizer discretizer) {
		TObjectDoubleIterator<T> it = propValuesX.iterator();
		
		for(int i = 0; i < propValuesX.size(); i++) {
			it.advance();
			if(propValuesY.containsKey(it.key())) {
				valuesY.add(propValuesY.get(it.key()));
				valuesX.add(discretizer.discretize(it.value()));
			}
		}
	}
}
