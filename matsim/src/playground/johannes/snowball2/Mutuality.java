/* *********************************************************************** *
 * project: org.matsim.*
 * Mutuality.java
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
package playground.johannes.snowball2;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.statistics.GraphStatistics;

/**
 * @author illenberger
 *
 */
public class Mutuality extends GraphStatistic {

	public Mutuality(String outputDir) {
		super(outputDir);
	}

//	@SuppressWarnings("unchecked")
//	public DescriptiveStatistics calculate(Graph g, int iteration,
//			DescriptiveStatistics reference) {
//		Map<Vertex, Double> clustering = GraphStatistics.clusteringCoefficients(g);
//		Set<Vertex> vertices = g.getVertices();
//		double z = 0;
//		double m = 0;
//		
//		boolean isSampled = false;
//		if(g instanceof SampledGraph)
//			isSampled = true;
//		
//		for(Vertex v : vertices) {
//			if(isSampled) {
//				if(!((SampledVertex)v).isAnonymous()) {
//					z += v.degree();
//					double c = clustering.get(v);
//					m += v.degree() /(double) (1 + Math.pow(c, 2) * (v.degree() - 1));
//				}
//			} else {
//				z += v.degree();
//				double c = clustering.get(v);
//				m += v.degree() /(double) (1 + Math.pow(c, 2) * (v.degree() - 1));
//			}
//		}
//		z = z / (double)vertices.size();
//		m = m / (double)vertices.size();
//		
//		double result = m / z;
//		
//		DescriptiveStatistics stats = new DescriptiveStatistics();
//		stats.addValue(result);
//		return stats;
//	}

	public DescriptiveStatistics calculate(Graph g, int iteration,
			DescriptiveStatistics reference) {
		Set<Vertex> vertices = g.getVertices();
		boolean isSampled = false;
		if(g instanceof SampledGraph)
			isSampled = true;
		
		int len2Paths = 0;
		int nVertex2Steps = 0;
		for(Vertex v : vertices) {
			if(!isSampled || (isSampled && !((SampledVertex)v).isAnonymous())) {
				
				Set<Vertex> n1Set = v.getNeighbors();
				Set<Vertex> n2Set = new HashSet<Vertex>();
				for(Vertex n1 : n1Set) {
					Set<Vertex> neighbours = n1.getNeighbors();
					for(Vertex neighbour : neighbours) {
						if(neighbour != v && !n1Set.contains(neighbour)) {
							n2Set.add(neighbour);
							len2Paths++;
						}
					}
				}
				
				nVertex2Steps += n2Set.size();
			}
		}
		
		double mutality = nVertex2Steps/(double)len2Paths;
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(mutality);
		return stats;
	}
}
