/* *********************************************************************** *
 * project: org.matsim.*
 * GraphStatistics.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.graph;


import gnu.trove.TObjectDoubleHashMap;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * @author illenberger
 *
 */
public class GraphStatistics {

	public static DescriptiveStatistics getDegreeStatistics(Graph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(Vertex v : g.getVertices())
			stats.addValue(v.getEdges().size());
		return stats;
	}
	
	public static Frequency getDegreeDistribution(Graph g) {
		Frequency freq = new Frequency();
		for(Vertex v : g.getVertices())
			freq.addValue(v.getEdges().size());
		return freq;
	}
	
	public static TObjectDoubleHashMap<? extends Vertex> getClustringCoefficients(Graph g) {
		TObjectDoubleHashMap<Vertex> cc = new TObjectDoubleHashMap<Vertex>();
		for(Vertex v : g.getVertices()) {
			int k = v.getEdges().size();
			if(k == 0 || k == 1) {
				cc.put(v, 0.0);
			} else {
				int edgecount = 0;
				Set<Vertex> n1s = new HashSet<Vertex>(v.getNeighbours());
				for(Vertex n1 : v.getNeighbours()) {
					for(Vertex n2 : n1.getNeighbours()) {
						if (n2 != v) {
							if (n1s.contains(n2))
								edgecount++;
						}
					}
					n1s.remove(n1);
				}
				cc.put(v, 2 * edgecount / (double)(k*(k-1)));
			}
		}
		
		return cc;
	}
	
	public static DescriptiveStatistics getClusteringStatistics(Graph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		TObjectDoubleHashMap<? extends Vertex> cc = getClustringCoefficients(g);
		for(double d : cc.getValues())
			stats.addValue(d);
		return stats;
	}
	
	public static double getMutuality(Graph g) {
		int len2Paths = 0;
		int nVertex2Steps = 0;

		for (Vertex v : g.getVertices()) {
			Set<? extends Vertex> n1Set = v.getNeighbours();
			Set<Vertex> n2Set = new HashSet<Vertex>();
			for (Vertex n1 : n1Set) {
				for (Vertex n2 : n1.getNeighbours()) {
					if (n2 != v && !n1Set.contains(n2)) {
						n2Set.add(n2);
						len2Paths++;
					}
				}
			}
			nVertex2Steps += n2Set.size();
		}

		return nVertex2Steps / (double) len2Paths;
	}
	
	public static double getDegreeCorrelation(Graph g) {
		double product = 0;
		double sum = 0;
		double squareSum = 0;

		for (Edge e : g.getEdges()) {
			Vertex v1 = e.getVertices().getFirst();
			Vertex v2 = e.getVertices().getSecond();
			int d_v1 = v1.getEdges().size();
			int d_v2 = v2.getEdges().size();

			sum += 0.5 * (d_v1 + d_v2);
			squareSum += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2));
			product += d_v1 * d_v2;			
		}
		
		double norm = 1 / (double)g.getEdges().size();
		return ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
	}
}
