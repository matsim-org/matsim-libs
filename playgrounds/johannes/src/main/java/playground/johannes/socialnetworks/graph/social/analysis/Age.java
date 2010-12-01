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
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class Age {

	public Distribution distribution(Set<? extends SocialVertex> vertices) {
		Distribution distr = new Distribution();
		for(SocialVertex vertex : vertices) {
			int age = vertex.getPerson().getAge();
			if(age > -1)
				distr.add(age);
		}
		
		return distr;
	}
	
	public TObjectDoubleHashMap<SocialVertex> values(Set<? extends SocialVertex> vertices) {
		TObjectDoubleHashMap<SocialVertex> values = new TObjectDoubleHashMap<SocialVertex>();
		for(SocialVertex vertex : vertices) {
			int age = vertex.getPerson().getAge();
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
		
		return Correlations.correlationMean(values1.toNativeArray(), values2.toNativeArray());
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
			}
		}
//		int i_max = i;
//		double mean1 = StatUtils.mean(values1);
//		double mean2 = StatUtils.mean(values2);
//		
//		double sum = 0;
//		double sum1square = 0;
//		double sum2square = 0;
//		for(i = 0; i < i_max; i++) {
//			sum += (values1[i]-mean1) * (values2[i]-mean2);
//			sum1square += Math.pow(values1[i]-mean1, 2);
//			sum2square += Math.pow(values2[i]-mean2, 2);
//		}
//		
//		return sum / (Math.sqrt(sum1square * sum2square));
		return new PearsonsCorrelation().correlation(values1.toNativeArray(), values2.toNativeArray());
	}
	
	public void boxplot(Set<? extends SocialVertex> vertices, String file) {
		TIntArrayList values1 = new TIntArrayList(vertices.size() * 15);
		TIntArrayList values2 = new TIntArrayList(vertices.size() * 15);
		
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
		
		int maxLen = 0;
		TIntObjectHashMap<TIntArrayList> map = new TIntObjectHashMap<TIntArrayList>();
		for(int i = 0; i < values1.size(); i++) {
			int age = (int) (Math.ceil(values1.get(i)/5.0)*5);
			if(age > 0) {
			TIntArrayList list = map.get(age);
			if(list == null) {
				list = new TIntArrayList();
				map.put(age, list);
			}
			list.add(values2.get(i));
			
			maxLen = Math.max(maxLen, list.size());
			}
		}
		
		int keys[] = map.keys();
		Arrays.sort(keys);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for(int k = 0; k < keys.length; k++) {
				writer.write("\"");
				writer.write(String.valueOf(keys[k]));
				writer.write("\"");
				if(k+1 < keys.length)
					writer.write("\t");
			}
			writer.newLine();
			
			for(int i = 0; i < maxLen; i++) {
				for(int k = 0; k < keys.length; k++) {
					TIntArrayList list = map.get(keys[k]);
					if(i < list.size()) {
						writer.write(String.valueOf(list.get(i)));
					} else {
						writer.write("NA");
					}
					if(k+1 < keys.length)
						writer.write("\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
