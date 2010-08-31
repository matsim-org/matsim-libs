/* *********************************************************************** *
 * project: org.matsim.*
 * ConnectTest.java
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

import gnu.trove.TIntIntHashMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.graph.analysis.Components;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;

import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class ConnectTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> reader =
			new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		
		reader.setGraphProjectionBuilder(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		
		SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/04-2010/graph/graph.graphml");
		
		SocialSparseVertex v1 = null;
		SocialSparseVertex v613 = null;
		int seeds = 0;
		for(VertexDecorator<SocialSparseVertex> v : graph.getVertices()) {
			if(((SampledVertexDecorator<SocialSparseVertex>)v).getIterationSampled() == 0)
				seeds++;
			
			if(v.getDelegate().getPerson().getId().toString().equals("1196")) {
				v1 = v.getDelegate();
			}
			if(v.getDelegate().getPerson().getId().toString().equals("2191")) {
				v613 = v.getDelegate();
			}
			if(v.getDelegate().getPerson().getId().toString().equals("10434")) {
				System.err.println("Found!");
			}
//			if(v1 != null && v613 != null)
//				break;
		}
		System.out.println(seeds + " seeds.");
//		System.out.print("Vertex 1196 has neighbors: ");
//		for(SampledSocialVertex v : v1.getNeighbours()) {
//			System.out.print(" " + v.getPerson().getId().toString());
//		}
//		System.out.println();
//		
//		
//		System.out.print("Vertex 2191 has neighbors: ");
//		for(SampledSocialVertex v : v613.getNeighbours()) {
//			System.out.print(" " + v.getPerson().getId().toString());
//		}
		
		HashSet<SocialSampledVertexDecorator<?>> ids = new HashSet<SocialSampledVertexDecorator<?>>();
		seeds = 0;
		List<Set<Vertex>> components = new Components().components(graph);
		for(Set<Vertex> component : components) {
			TIntIntHashMap map = new TIntIntHashMap();
			SocialSampledVertexDecorator<?> ego = null;
			for(Vertex vertex : component) {
				int it = ((SampledVertex)vertex).getIterationSampled();
				map.adjustOrPutValue(it, 1, 1);
				if(it == 0) {
					ego = (SocialSampledVertexDecorator<?>) vertex;
					seeds++;
					if(!ids.add(ego))
						System.err.println(ego.getPerson().getId().toString() + " is doubled!");
				}
			}
			if(ego == null) {
				for(Vertex v : component) {
					System.out.println(String.format("id: %1$s, it: %2$s.", ((SocialSampledVertexDecorator<?>)v).getPerson().getId(), ((SampledVertex)v).getIterationSampled()));
				}
			}
			String id = "null";
			if(ego != null) {
				System.out.println("Age=" + ego.getPerson().getAge());
				 id = ego.getPerson().getId().toString();
			}
//		
			
			System.out.println(String.format("ego id; %5$s, 0: %1$s, 1: %2$s, 2: %3$s, unsampled: %4$s.", map.get(0), map.get(1), map.get(2), map.get(-1), id));
			
			
		}
		
		System.out.println(seeds + " seeds.");
	}

}
