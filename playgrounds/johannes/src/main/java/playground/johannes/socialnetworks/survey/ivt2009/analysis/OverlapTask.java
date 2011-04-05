/* *********************************************************************** *
 * project: org.matsim.*
 * OverlapTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

/**
 * @author illenberger
 *
 */
public class OverlapTask extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> statsMap) {
		SocialSampledGraphProjection<SocialGraph, SocialVertex, SocialEdge> graph = (SocialSampledGraphProjection<SocialGraph, SocialVertex, SocialEdge>) g;
		
		Map<SampledVertexDecorator<SocialVertex>, Set> vertices = new HashMap<SampledVertexDecorator<SocialVertex>, Set>();
		
		for(SampledVertexDecorator<SocialVertex> vertex : graph.getVertices()) {
			Set<Object> seeds = new HashSet<Object>();
			for(SampledVertexDecorator<SocialVertex> neighbor : vertex.getNeighbours()) {
				if(neighbor.getSeed() == null)
					System.err.println("Seed is null!");
				else
					seeds.add(neighbor.getSeed());
			}
			
			if(seeds.size() > 1)
				vertices.put(vertex, seeds);
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/overlaps.txt"));
			
			writer.write("Ego\tSeed1\tSeed2");
			writer.newLine();
			for(Entry<SampledVertexDecorator<SocialVertex>, Set> entry : vertices.entrySet()) {
				writer.write(entry.getKey().getDelegate().getPerson().getId().toString());
				writer.write("\t");
				for(Object vertex : entry.getValue()) {
					writer.write(((SampledVertexDecorator<SocialVertex>)vertex).getDelegate().getPerson().getId().toString());
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

	public static void main(String args[]) {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/graph/graph.graphml");
		
		ApplySeedsFilter seedsFilder = new ApplySeedsFilter();
		seedsFilder.apply(graph);
		OverlapTask task = new OverlapTask();
		task.setOutputDirectoy("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/analysis");
		task.analyze(graph, null);
	}
}
