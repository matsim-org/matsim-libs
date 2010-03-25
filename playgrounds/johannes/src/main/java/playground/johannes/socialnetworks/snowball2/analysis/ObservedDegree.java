/* *********************************************************************** *
 * project: org.matsim.*
 * SampledDegree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;



/**
 * @author illenberger
 *
 */
public class ObservedDegree extends Degree {

	@SuppressWarnings("unchecked")
	@Override
	public Distribution distribution(Set<? extends Vertex> vertices) {
		return super.distribution(SnowballPartitions.<SampledVertex>createSampledPartition((Set<SampledVertex>)vertices));
	}

	@SuppressWarnings("unchecked")
	@Override
	public TObjectDoubleHashMap<Vertex> values(Collection<? extends Vertex> vertices) {
		return (TObjectDoubleHashMap<Vertex>) super.values(SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>) vertices));
	}

	@Override
	public double assortativity(Graph graph) {
		double product = 0;
		double sum = 0;
		double squareSum = 0;
		int edgecount = 0;
		for (Edge e : graph.getEdges()) {
			SampledVertex v1 = (SampledVertex) e.getVertices().getFirst();
			SampledVertex v2 = (SampledVertex) e.getVertices().getSecond();
			if(v1.isSampled() && v2.isSampled()) {
				int d_v1 = v1.getEdges().size();
				int d_v2 = v2.getEdges().size();

				sum += 0.5 * (d_v1 + d_v2);
				squareSum += 0.5 * (Math.pow(d_v1, 2) + Math.pow(d_v2, 2));
				product += d_v1 * d_v2;
				
				edgecount++;
			}
		}
		
		double norm = 1 / (double)edgecount;
		return ((norm * product) - Math.pow(norm * sum, 2)) / ((norm * squareSum) - Math.pow(norm * sum, 2));
	}

}
