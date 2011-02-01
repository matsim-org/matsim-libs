/* *********************************************************************** *
 * project: org.matsim.*
 * AgeHomophilyTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;

/**
 * @author illenberger
 *
 */
public class AgeHomophilyTask extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		SocialGraph graph = (SocialGraph) g;
		
		final TObjectDoubleHashMap<SocialVertex> values = new TObjectDoubleHashMap<SocialVertex>();
		
		for(SocialVertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled()) {
				double rmse = 0;
				int cnt = 0;
				int egoAge = vertex.getPerson().getAge();//genderToInt(vertex.getPerson().getPerson().getSex());//
				if(egoAge > 0) {
				for(SocialVertex neighbour : vertex.getNeighbours()) {
					int alterAge = neighbour.getPerson().getAge();//genderToInt(neighbour.getPerson().getPerson().getSex());//
					if(alterAge > 0) {
						rmse += Math.min(alterAge, egoAge)/(double)Math.max(egoAge, alterAge);
						cnt++;
					}
				}
				
				if(cnt > 0) {
					rmse = rmse/(double)cnt;
					values.put(vertex, rmse);
				}
				}
			}
		}

		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		writer.setKmlPartitition(new KMLPartitions() {
			
			@Override
			public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
				List<Set<? extends SpatialVertex>> list = new ArrayList<Set<? extends SpatialVertex>>(1);
				Set<SpatialVertex> set = new HashSet<SpatialVertex>();
				for(Object vertex : values.keys()) {
					set.add((SpatialVertex) vertex);
				}
				list.add(set);
				return list;
			}
			
			@Override
			public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
				// TODO Auto-generated method stub
				
			}
		});
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		style.setVertexColorizer(new NumericAttributeColorizer(values));
		writer.setKmlVertexStyle(style);
		writer.addKMZWriterListener(style);
		writer.setDrawEdges(false);
		writer.write(graph, "/Users/jillenberger/Work/socialnets/homophily/age.kmz");
	}
	
	private int genderToInt(String gender) {
		if(gender == null)
			return 0;
		if(gender.equals("m"))
			return 1;
		else
			return 2;
	}
	
//	public static void main(String args[]) {
//		Graph graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/graph.graphml");
//		
//		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
//		
//		RemoveNoCoordinates filter = new RemoveNoCoordinates((GraphBuilder<? extends SpatialGraph, ? extends SpatialVertex, ? extends SpatialEdge>) builder);
//		graph = filter.apply((SpatialGraph) graph);
//		
//		new AgeHomophilyTask().analyze(graph, null);
//	}

}
