/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentsStats.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.UserDataKeys;
import playground.johannes.statistics.SampledGraphStatistics;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import gnu.trove.TObjectIntHashMap;

/**
 * @author illenberger
 *
 */
public class ComponentsStats extends GraphStatistic {

	private List<Vertex> seeds;
	
	private List<BufferedWriter> writers;

	private TObjectIntHashMap<Vertex> lastNumVisitedVertices;
	
	private TObjectIntHashMap<Vertex> lastNumSampledVertices;
	
	private TObjectIntHashMap<Vertex> deltaLastVisited;
	
	public ComponentsStats(Collection<Vertex> seeds, String outputDir) {
		super(outputDir);
		this.seeds = new ArrayList<Vertex>(seeds);
		
		lastNumSampledVertices = new TObjectIntHashMap<Vertex>();
		lastNumVisitedVertices = new TObjectIntHashMap<Vertex>();
		deltaLastVisited = new TObjectIntHashMap<Vertex>();
		
		writers = new ArrayList<BufferedWriter>();
		for(int i = 0; i < this.seeds.size(); i++) {
			try {
				BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "/" + i + ".component.txt");
				writer.write("Iter\tnumVertex\tnumEdge\tgrowth\tefficiency\tdegree\tclustering");
				writer.newLine();
				
				writers.add(writer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public DescriptiveStatistics calculate(Graph g, int iteration, DescriptiveStatistics reference) {
		if(g instanceof SampledGraph) {
			Set<Collection<SampledVertex>> clusters = SampledGraphStatistics.getDisconnectedComponents((SampledGraph)g);
			Collection<SampledGraph> subGraphs = new LinkedList<SampledGraph>();
			for(Collection<SampledVertex> cluster : clusters) {
				subGraphs.add(SampledGraphStatistics.extractGraphFromCluster(cluster));
			}
		
			for (int i = 0; i < seeds.size(); i++) {
				SampledGraph subGraph = graphForSeed(seeds.get(i), subGraphs);
				BufferedWriter writer = writers.get(i);

				int numSampledVertices = 0;
				Set<SampledVertex> vertices = subGraph.getVertices();
				for(SampledVertex v : vertices) {
					if(!v.isAnonymous())
						numSampledVertices++;
				}
							
				int numVisitedVertices = 0;
				for(SampledVertex v : vertices) {
						numVisitedVertices += v.getVisited();
				}
				
				int deltaSampled = numSampledVertices - lastNumSampledVertices.get(seeds.get(i));
				int deltaVisited = numVisitedVertices - lastNumVisitedVertices.get(seeds.get(i));
				double efficiency = deltaSampled/(double)deltaLastVisited.get(seeds.get(i));
				
				try {
					writer.write(String.valueOf(iteration));
					writer.write("\t");
					writer.write(String.valueOf(numSampledVertices));
					writer.write("\t");
					writer.write(String.valueOf(subGraph.numEdges()));
					writer.write("\t");
					writer.write(String.format(Locale.US, "%1.4f", numSampledVertices/(double)lastNumSampledVertices.get(seeds.get(i))));
					writer.write("\t");
					writer.write(String.format(Locale.US, "%1.4f", efficiency));
					writer.write("\t");
					writer.write(String.format(Locale.US, "%1.4f", SampledGraphStatistics.getDegreeStatistics(subGraph).getMean()));
					writer.write("\t");
					writer.write(String.format(Locale.US, "%1.4f", SampledGraphStatistics.getClusteringStatistics(subGraph).getMean()));
					writer.newLine();
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				lastNumSampledVertices.put(seeds.get(i), numSampledVertices);
				lastNumVisitedVertices.put(seeds.get(i), numVisitedVertices);
				deltaLastVisited.put(seeds.get(i), deltaVisited);
			}
		}
		
		return new DescriptiveStatistics();
	}

	private SampledGraph graphForSeed(Vertex seed, Collection<SampledGraph> subGraphs) {
		String id = (String)seed.getUserDatum(UserDataKeys.ID);
		for(SampledGraph g : subGraphs) {
			Set<SampledVertex> vertices = g.getVertices();
			for(SampledVertex v : vertices) {
				String id2 = (String)v.getUserDatum(UserDataKeys.ID);
				if(id.equals(id2))
					return g;
			}
		}
		
		return null;
	}
}
