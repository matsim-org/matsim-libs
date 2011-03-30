/* *********************************************************************** *
 * project: org.matsim.*
 * Age.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AbstractVertexProperty;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class Age extends AbstractVertexProperty {

	private static Age instance;
	
	public static Age getInstance() {
		if(instance == null)
			instance = new Age();
		return instance;
	}
	
//	public DescriptiveStatistics statistics(Set<? extends Vertex> vertices) {
//		DescriptiveStatistics distr = new DescriptiveStatistics();
//		for(Vertex vertex : vertices) {
//			int age = ((SocialVertex) vertex).getPerson().getAge();
//			if(age > -1)
//				distr.addValue(age);
//		}
//		
//		return distr;
//	}
	
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		TObjectDoubleHashMap<Vertex> values = new TObjectDoubleHashMap<Vertex>();
		for(Vertex vertex : vertices) {
			int age = ((SocialVertex) vertex).getPerson().getAge();
			if(age > -1)
				values.put(vertex, age);
		}
		
		return values;
	}
	
	public TDoubleDoubleHashMap correlation(Set<? extends SocialVertex> vertices) {
		TDoubleArrayList values1 = new TDoubleArrayList(vertices.size() * 15);
		TDoubleArrayList values2 = new TDoubleArrayList(vertices.size() * 15);
		
		for(SocialVertex vertex : vertices) {
			int age1 = vertex.getPerson().getAge();
			if(age1 > -1) {
				for(SocialVertex neighbor : vertex.getNeighbours()) {
					int age2 = neighbor.getPerson().getAge();
					if(age2 > -1) {
						values1.add(age1);
						values2.add(age2);
					}
				}
			}
		}
		
		return Correlations.mean(values1.toNativeArray(), values2.toNativeArray());
	}
	
	public double correlationCoefficient(Set<? extends SocialEdge> edges) {
		TDoubleArrayList values1 = new TDoubleArrayList(edges.size());
		TDoubleArrayList values2 = new TDoubleArrayList(edges.size());
		
		for(SocialEdge edge : edges) {
			double a1 = edge.getVertices().getFirst().getPerson().getAge();
			double a2 = edge.getVertices().getSecond().getPerson().getAge();
			if(a1 > 0 && a2 > 0) {
				values1.add(a1);
				values2.add(a2);
				
				values1.add(a2);
				values2.add(a1);
			}
		}

		if(values1.isEmpty())
			return Double.NaN;
		else
			return new PearsonsCorrelation().correlation(values1.toNativeArray(), values2.toNativeArray());
	}
	
	public TDoubleObjectHashMap<DescriptiveStatistics> boxplot(Set<? extends SocialVertex> vertices) {
		TDoubleArrayList values1 = new TDoubleArrayList(vertices.size() * 15);
		TDoubleArrayList values2 = new TDoubleArrayList(vertices.size() * 15);
		
		for(SocialVertex vertex : vertices) {
			int age1 = vertex.getPerson().getAge();
			if(age1 > -1) {
				for(SocialVertex neighbor : vertex.getNeighbours()) {
					int age2 = neighbor.getPerson().getAge();
					if(age2 > -1) {
						values1.add(age1);
						values2.add(age2);
					}
				}
			}
		}
		
		return Correlations.statistics(values1.toNativeArray(), values2.toNativeArray(), new LinearDiscretizer(5.0));
	}
}
