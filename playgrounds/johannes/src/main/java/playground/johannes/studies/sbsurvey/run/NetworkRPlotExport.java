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
package playground.johannes.studies.sbsurvey.run;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.list.array.TIntArrayList;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.socnetgen.sna.graph.GraphUtils;
import org.matsim.contrib.socnetgen.sna.graph.io.PajekAttributes;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.socnetgen.sna.graph.matrix.Dijkstra;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialPajekWriter;
import org.matsim.contrib.socnetgen.sna.snowball.SampledEdge;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballPartitions;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjectionBuilder;
import org.opengis.feature.simple.SimpleFeature;
import playground.johannes.studies.sbsurvey.analysis.AlterGraphFilter;
import playground.johannes.studies.sbsurvey.analysis.ApplySeedsFilter;
import playground.johannes.studies.sbsurvey.io.GraphReaderFacade;
import playground.johannes.studies.sbsurvey.io.SeedComponentColorizer;

import java.io.IOException;
import java.util.*;

/**
 * @author illenberger
 *
 */
public class NetworkRPlotExport {

	/**
	 * @param args
	 */
	public static void main(String args[]) throws IOException {
		SpatialGraph g = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");
		
		Collection<SimpleFeature> features = EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/Kanton.shp");
		Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry();
		geometry.setSRID(21781);
		
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		
//		SpatialFilter filter2 = new SpatialFilter((GraphBuilder<? extends SpatialGraph, ? extends SpatialVertex, ? extends SpatialEdge>) builder, geometry);
//		g = (SpatialGraph) filter2.apply(g);
		
		ApplySeedsFilter seedFiler = new ApplySeedsFilter();
		g = (SpatialGraph) seedFiler.apply((SampledGraph) g);
		
		AlterGraphFilter filter = new AlterGraphFilter(builder);
		g = (SpatialGraph) filter.apply((SampledGraph) g);

		
		SpatialPajekWriter writer = new SpatialPajekWriter();
		
		final SeedComponentColorizer colorizer = new SeedComponentColorizer((SampledGraph) g);
		final Set<SampledEdge> pathEdges = getPathEdges((SampledGraph) g);
		
		PajekAttributes<SpatialVertex, SpatialEdge> attrs = new PajekAttributes<SpatialVertex, SpatialEdge>() {

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
			public String getVertexValue(SpatialVertex v, String attribute) {
				if (PajekAttributes.VERTEX_FILL_COLOR.equals(attribute))
					return colorizer.getVertexFillColor((SampledVertex) v);
				else
					return null;
			}

			@Override
			public String getEdgeValue(SpatialEdge e, String attribute) {
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
