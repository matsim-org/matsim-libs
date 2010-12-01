/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnetworks.spatial;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.spatial.generators.EdgeCostFunction;
import playground.johannes.socialnetworks.graph.spatial.generators.GravityEdgeCostFunction;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class CostAnalysis {

	private static final double beta = 1;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> reader =
			new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		
		reader.setGraphProjectionBuilder(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		
		SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/04-2010/graph/graph.ch.21781.graphml");
		
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/plans/plans.0.003.xml");
		
//		List<SpatialVertex> vertices = new ArrayList<SpatialVertex>(graph.getVertices());
//		int N = vertices.size();
		
		EdgeCostFunction costFunction = new GravityEdgeCostFunction(1.6, 1, new CartesianDistanceCalculator());
		Distribution distr = new Distribution();
//		for(int i = 0; i < N; i++) {
//			double sum = 0;
//			for(int j = 0; j < N; j++) {
//				if(i != j) {
//					sum += costFunction.edgeCost(vertices.get(i), vertices.get(j));
//				}
//			}
//			distr.add(sum);
//		}
		
		Discretizer discretizer = new LinearDiscretizer(1000.0);
		for(VertexDecorator<SocialSparseVertex> vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled() && vertex.getNeighbours().size() > 0) {
				double sum = 0;
				for(Vertex neighbor : vertex.getNeighbours()) {
					sum += costFunction.edgeCost((SpatialVertex)vertex, (SpatialVertex)neighbor);
				}
				
				double total = 0;
				for(Vertex opportunity : graph2.getVertices()) {
//					total += costFunction.edgeCost((SpatialVertex)vertex, (SpatialVertex)opportunity);
					double dx = ((SpatialVertex) vertex).getPoint().getCoordinate().x - ((SpatialVertex) opportunity).getPoint().getCoordinate().x;
					double dy = ((SpatialVertex) vertex).getPoint().getCoordinate().y - ((SpatialVertex) opportunity).getPoint().getCoordinate().y;
					double d = Math.max(1.0, discretizer.index(Math.sqrt(dx*dx + dy*dy)));
					total += d;
				}
				distr.add(total/sum);
			}
		}

		Distribution.writeHistogram(distr.absoluteDistribution((distr.max() - distr.min())/50), "/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/plans/costs.txt");
	}
}
