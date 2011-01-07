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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.VertexProperty;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.DummyDiscretizer;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class VertexPropertyCorrelation {

	public static TDoubleDoubleHashMap mean(VertexProperty propY, VertexProperty porpX, Set<? extends Vertex> vertices) {
		return mean(propY, porpX, vertices, new DummyDiscretizer());
	}
	
	public static TDoubleDoubleHashMap mean(VertexProperty propY, VertexProperty propX, Set<? extends Vertex> vertices, Discretizer discretizer) {
		TObjectDoubleHashMap<Vertex> propValuesX = propX.values(vertices);
		Set<Vertex> filtered = filter(propValuesX);
		TObjectDoubleHashMap<Vertex> propValuesY = propY.values(filtered);
		
		return mean(propValuesY, propValuesX, discretizer);
	}
	
	private static <V extends Vertex> Set<V> filter(TObjectDoubleHashMap<V> propValuesX) {
		Set<V> filtered = new HashSet<V>();
		TObjectDoubleIterator<V> it = propValuesX.iterator();
		for(int i = 0; i < propValuesX.size(); i++) {
			it.advance();
			filtered.add(it.key());
		}
		return filtered;
	}
	
	public static TDoubleDoubleHashMap mean(TObjectDoubleHashMap<? extends Vertex> propValuesY, TObjectDoubleHashMap<? extends Vertex> propValuesX, Discretizer discretizer) {
		TDoubleArrayList valuesY = new TDoubleArrayList(propValuesX.size());
		TDoubleArrayList valuesX = new TDoubleArrayList(propValuesX.size());

		discretizeValues((TObjectDoubleHashMap<Vertex>) propValuesY, (TObjectDoubleHashMap<Vertex>) propValuesX, valuesX, valuesY, discretizer);
		
		return Correlations.mean(valuesX.toNativeArray(), valuesY.toNativeArray());
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
	
	public static TDoubleObjectHashMap<DescriptiveStatistics> statistics(TObjectDoubleHashMap<? extends Vertex> propValuesY, TObjectDoubleHashMap<? extends Vertex> propValuesX, Discretizer discretizer) {
		TDoubleArrayList valuesY = new TDoubleArrayList(propValuesX.size());
		TDoubleArrayList valuesX = new TDoubleArrayList(propValuesX.size());
		
		discretizeValues((TObjectDoubleHashMap<Vertex>)propValuesY, (TObjectDoubleHashMap<Vertex>)propValuesX, valuesX, valuesY, discretizer);
		
		return Correlations.statistics(valuesX.toNativeArray(), valuesY.toNativeArray(), discretizer);
	}
	
	private static void discretizeValues(TObjectDoubleHashMap<Vertex> propValuesY, TObjectDoubleHashMap<Vertex> propValuesX, TDoubleArrayList valuesX, TDoubleArrayList valuesY, Discretizer discretizer) {
		TObjectDoubleIterator<Vertex> it = propValuesX.iterator();
		
		for(int i = 0; i < propValuesX.size(); i++) {
			it.advance();
			if(propValuesY.containsKey(it.key())) {
				valuesY.add(propValuesY.get(it.key()));
				valuesX.add(discretizer.discretize(it.value()));
			}
		}
	}
}
