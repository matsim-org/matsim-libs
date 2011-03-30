/* *********************************************************************** *
 * project: org.matsim.*
 * VertexTableWriter.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.GraphUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Components;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;

import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledEdgeDecorator;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ApplySeedsFilter;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.SeedConnectionTask;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class VertexTableWriter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SocialSampledGraphProjection<SocialSparseGraph,SocialSparseVertex,SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/01-2011/graph/graph.graphml");

		ApplySeedsFilter filter = new ApplySeedsFilter();
		filter.apply(graph);
		VertexTableWriter w = new VertexTableWriter();
		w.writeVertexList(graph);
		w.writeEdgeList(graph);
	}

	public void writeVertexList(SocialSampledGraphProjection<SocialSparseGraph,SocialSparseVertex,SocialSparseEdge> graph) throws IOException {
		Components components = new Components();
		List<Set<Vertex>> comps = components.components(graph);
		TObjectIntHashMap<Vertex> colors = new TObjectIntHashMap<Vertex>();
		
		int k = 261;
		
		for(Set<Vertex> comp : comps) {
			List<Vertex> seeds = new LinkedList<Vertex>();
			for(Vertex v : comp) {
				if(((SampledVertex)v).isSampled()) {
					if(((SampledVertex)v).getIterationSampled() == 0) {
						seeds.add(v);
					}
				}
			}
			if(seeds.size() == 1) {
				colors.put(seeds.get(0), k);
				k+=5;
			} else {
				Collections.shuffle(seeds);
				int step = 100/seeds.size();
				for(int i = 0; i < seeds.size(); i++) {
//					colors.put(seeds.get(i), (i+1)*10);
					colors.put(seeds.get(i), (i*step)+261);
//					k+=3;
				}
			}
		}
		
//		Set<Id> seedIds = new HashSet<Id>();
//		for(SampledVertex vertex : graph.getVertices()) {
//			seedIds.add(((SocialVertex)vertex.getSeed()).getPerson().getId());
//		}
//		
//		List<Id> list = new ArrayList<Id>(seedIds);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/socialnets/data/ivt2009/01-2011/graph/vertexlist.txt"));
		writer.write("id\tcolor\tshape");
		writer.newLine();
		for(SocialSampledVertexDecorator<SocialSparseVertex> vertex : graph.getVertices()) {
//			if(vertex.isSampled() || isBridgeAlter(vertex)) {
			writer.write(vertex.getPerson().getId().toString());
			writer.write("\t");
			writer.write(String.valueOf(colors.get(vertex.getSeed())));
//			writer.write("#FFFFFF");
			writer.write("\t");
			if(vertex.isSampled())
				writer.write(String.valueOf(vertex.getIterationSampled() + 1));
//				writer.write("circle");
			else {
				writer.write("5");
//				writer.write("square");
			}
//			writer.write("\t");
//			writer.write("FFFFFF");
			writer.newLine();
//			}
		}
		writer.close();
	}
	
	public void writeEdgeList(SocialSampledGraphProjection<SocialSparseGraph,SocialSparseVertex,SocialSparseEdge> graph) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/socialnets/data/ivt2009/01-2011/graph/edgelist.txt"));
		
		SeedConnectionTask task = new SeedConnectionTask();
		Map<String, Double> map = new HashMap<String, Double>();
		Set<Edge> edges = new HashSet<Edge>();
		Set<Edge> yellowEdges = new HashSet<Edge>();
		task.setOutputDirectoy("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/");
		task.analyze(graph, map);
		for(List<SampledVertexDecorator<SocialVertex>> list : task.pathSet) {
//			boolean yellow = false;
//			if(list.size() == 18) {
//				yellow = true;
//			}
			for(int i = 0; i < list.size()-1; i++) {
				Edge e = GraphUtils.findEdge(list.get(i), list.get(i+1));
				if(e!=null) {
//					if(yellow)
//						yellowEdges.add(e);
//					else
						edges.add(e);
				}
			}
		}
		System.out.println("Black edges; "+edges.size());
		writer.write("ego1\tego2\tcolor");
		writer.newLine();
		
		for(SocialSampledEdgeDecorator<SocialSparseEdge> edge : graph.getEdges()) {
			SocialSampledVertexDecorator<SocialSparseVertex> v1 = (SocialSampledVertexDecorator<SocialSparseVertex>) edge.getVertices().getFirst();
			SocialSampledVertexDecorator<SocialSparseVertex> v2 = (SocialSampledVertexDecorator<SocialSparseVertex>) edge.getVertices().getSecond();
			boolean writeEdge = false;
			if(v1.isSampled() && v2.isSampled()) {
				writeEdge = true;
			} else {
				if(v1.isSampled() && v2.isDetected()) {
					if(isBridgeAlter(v2)) {
						System.out.println("is bridge alter");
						writeEdge = true;
					}
				} else if(v1.isDetected() && v2.isSampled()) {
					if(isBridgeAlter(v1)) {
						System.out.println("is bridge alter");
						writeEdge = true;
					}
				}
			}
			
			if(writeEdge) {
				writer.write(v1.getPerson().getId().toString());
				writer.write("\t");
				writer.write(v2.getPerson().getId().toString());
				writer.write("\t");
				if(yellowEdges.contains(edge))
					writer.write("black");
				else if(edges.contains(edge)) {
					writer.write("black");
				} else {
					writer.write("grey40");
				}
				writer.newLine();
				
			}
		}
		writer.close();
	}
	
	private boolean isBridgeAlter(SampledVertex vertex) {
		Set<SampledVertex> seeds = new HashSet<SampledVertex>();
		
		for(SampledVertex v : vertex.getNeighbours()) {
			seeds.add(v.getSeed());
		}
		
		if(seeds.size() > 1)
			return true;
		else
			return false;
	}
}
