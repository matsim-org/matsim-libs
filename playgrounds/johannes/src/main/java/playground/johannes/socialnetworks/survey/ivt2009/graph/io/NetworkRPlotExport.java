/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkRPlotExport.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import gnu.trove.TIntArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.GraphUtils;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.matrix.AdjacencyMatrix;
import playground.johannes.sna.graph.matrix.Dijkstra;
import playground.johannes.sna.snowball.SampledEdge;
import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.SampledVertex;
import playground.johannes.sna.snowball.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.graph.io.PajekAttributes;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.AlterGraphFilter;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ApplySeedsFilter;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class NetworkRPlotExport {

	/**
	 * @param args
	 */
	public static void main(String args[]) throws IOException {
		Graph g = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");
		
		ApplySeedsFilter seedFiler = new ApplySeedsFilter();
		g = seedFiler.apply((SampledGraph) g);
		
		AlterGraphFilter filter = new AlterGraphFilter(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		g = (SampledGraph) filter.apply((SampledGraph) g);

		
		PajekWriter writer = new PajekWriter();
		
		final SeedComponentColorizer colorizer = new SeedComponentColorizer((SampledGraph) g);
		final Set<SampledEdge> pathEdges = getPathEdges((SampledGraph) g);
		
		PajekAttributes<Vertex, Edge> attrs = new PajekAttributes<Vertex, Edge>() {

			@Override
			public List<String> getVertexAttributes() {
				List<String> attrs = new ArrayList<String>();
				attrs.add(PajekAttributes.VERTEX_FILL_COLOR);
				return attrs;
			}

			@Override
			public List<String> getEdgeAttributes() {
				List<String> attrs = new ArrayList<String>();
				attrs.add(PajekAttributes.EDGE_WIDTH);
				return attrs;
			}

			@Override
			public String getVertexValue(Vertex v, String attribute) {
				if (PajekAttributes.VERTEX_FILL_COLOR.equals(attribute))
					return colorizer.getVertexFillColor((SampledVertex) v);
				else
					return null;
			}

			@Override
			public String getEdgeValue(Edge e, String attribute) {
				if(PajekAttributes.EDGE_WIDTH.equals(attribute)) {
					if(pathEdges.contains(e))
						return "1";
					else
						return "0.5";
				} else
					return null;
			}
		};
		writer.write(g, attrs, "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.net");
	}
	
	static private Set<SampledEdge> getPathEdges(SampledGraph graph) {
		List<? extends SampledVertex> seeds = new ArrayList<SampledVertex>(SnowballPartitions.createSampledPartition(graph.getVertices(), 0));
		
		AdjacencyMatrix<SampledVertex> y = new AdjacencyMatrix<SampledVertex>(graph);
		Dijkstra dijkstra = new Dijkstra(y);
		
		Set<SampledEdge> edges = new HashSet<SampledEdge>();
		
		for(int i = 0; i < seeds.size(); i++) {
			int idx_i = y.getIndex(seeds.get(i));
			dijkstra.run(idx_i, -1);
			for(int j = i; j < seeds.size(); j++) {
				int idx_j = y.getIndex(seeds.get(j));
				TIntArrayList path = dijkstra.getPath(idx_i, idx_j);
				if (path != null) {
					path.insert(0, idx_i);
					for (int k = 1; k < path.size(); k++) {
						SampledVertex v1 = y.getVertex(path.get(k - 1));
						SampledVertex v2 = y.getVertex(path.get(k));
						edges.add((SampledEdge) GraphUtils.findEdge(v1, v2));
					}
				}
			}
		}
		
		return edges;
	}
}
