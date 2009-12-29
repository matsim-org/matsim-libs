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
package playground.johannes.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.snowball2.SampledEdge;
import playground.johannes.snowball2.SampledGraph;
import playground.johannes.snowball2.SampledVertex;
import playground.johannes.snowball2.SparseVertex;
import playground.johannes.snowball2.UnweightedDijkstra;
import playground.johannes.snowball2.Centrality.CentralityGraph;
import playground.johannes.snowball2.Centrality.CentralityGraphDecorator;
import playground.johannes.snowball2.Centrality.CentralityVertex;
import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.statistics.GraphStatistics;

/**
 * @author illenberger
 *
 */
public class SampledGraphStatistics extends playground.johannes.statistics.GraphStatistics {

	public static DescriptiveStatistics getDegreeStatistics(SampledGraph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		Set<SampledVertex> vertices = g.getVertices();
		for(SampledVertex v : vertices) {
			if(!v.isAnonymous())
				stats.addValue(v.degree());
		}
		return stats;
	}
	
	public static DescriptiveStatistics getClusteringStatistics(SampledGraph g) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		Map<SampledVertex, Double> values = GraphStatistics.clusteringCoefficients(g);
		for(SampledVertex v : values.keySet()) {
			if(!v.isAnonymous()) {
				if(v.degree() == 1)
					stats.addValue(0.0);
				else
					stats.addValue(values.get(v));
			}
		}
		
		return stats;
	}
	
	public static SortedSet<Collection<SampledVertex>> getDisconnectedComponents(SampledGraph g) {
		TreeSet<Collection<SampledVertex>> clusters = new TreeSet<Collection<SampledVertex>>(new SizeComparator());
		CentralityGraphDecorator graphDecorator = new CentralityGraphDecorator(g);
		UnweightedDijkstra dijkstra = new UnweightedDijkstra((CentralityGraph) graphDecorator.getSparseGraph());
		Queue<CentralityVertex> vertices = new LinkedList<CentralityVertex>();
		for(SparseVertex v : graphDecorator.getSparseGraph().getVertices())
			vertices.add((CentralityVertex) v);
		
		CentralityVertex source;
		while((source = vertices.poll()) != null) {
			List<CentralityVertex> reached = dijkstra.run(source);
			reached.add(source);
			List<SampledVertex> reached2 = new LinkedList<SampledVertex>();
			for(CentralityVertex cv : reached)
				reached2.add((SampledVertex) graphDecorator.getVertex(cv));
			clusters.add(reached2);
			vertices.removeAll(reached);
		}
		
		return clusters;
	}
	
	public static SampledGraph extractGraphFromCluster(Collection<SampledVertex> cluster) {
		SampledGraph g = new SampledGraph();
		Map<SampledVertex, SampledVertex> mapping = new HashMap<SampledVertex, SampledVertex>();
		for(SampledVertex v : cluster) {
			SampledVertex vCopy = new SampledVertex(v.getWaveDetected());
			for(Iterator<String> it = v.getUserDatumKeyIterator(); it.hasNext();) {
				String key = it.next();
				vCopy.setUserDatum(key, v.getUserDatum(key), UserDataKeys.COPY_ACT);
			}
			g.addVertex(vCopy);
			mapping.put(v, vCopy);
		}
		for(SampledVertex v : cluster) {
			Set<SampledEdge> edges = v.getIncidentEdges();
			for(SampledEdge e : edges) {
				SampledVertex v1 = mapping.get(v);
				SampledVertex v2 = mapping.get(e.getOpposite(v));
				if (v1 != null && v2 != null) {
					SampledEdge eCopy = new SampledEdge(v1, v2, e.getWaveSampled());
					try {
						g.addEdge(eCopy);
					} catch (IllegalArgumentException ex) {
						// do nothing
					}
				}
			}
		}
		
		return g;
	}
}
