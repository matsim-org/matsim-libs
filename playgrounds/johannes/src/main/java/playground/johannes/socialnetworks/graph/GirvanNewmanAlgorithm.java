/* *********************************************************************** *
 * project: org.matsim.*
 * GirvanNewmanAlgorithm.java
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
package playground.johannes.socialnetworks.graph;

import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.EdgeDecorator;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.SparseGraphProjectionBuilder;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.io.PajekCommunityColorizer;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.io.KMLCommunityStlyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexDescriptor;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialPajekWriter;

/**
 * @author illenberger
 *
 */
public class GirvanNewmanAlgorithm {
	
	private static final Logger logger = Logger.getLogger(GirvanNewmanAlgorithm.class);

	private String outputDir;
	
	public <V extends Vertex> List<Set<Set<V>>> dendogram(Graph graph, int maxIterations, Handler handler) {
		List<Set<Set<V>>> dendogram = new ArrayList<Set<Set<V>>>();
		SparseGraphProjectionBuilder<Graph, V, Edge> builder = new SparseGraphProjectionBuilder<Graph, V, Edge>();
		GraphProjection<Graph, V, Edge> projection = builder.decorateGraph(graph);
//		projection.decorate();
		
		int iteration = 0;
		int lastSize = Integer.MAX_VALUE;
		while(projection.getEdges().size() > 0 && iteration < maxIterations) {
			logger.info(String.format("Calculating edge betweenness at level %1$s...", iteration));
			GraphCentrality c = new GraphCentrality(projection);
			c.calculate();
			
			
			double maxBC = 0;
			Edge maxBCEdge = null;
			TObjectIntIterator<Edge> it = c.getEdgeBetweenness().iterator();
			for(int i = 0; i < c.getEdgeBetweenness().size(); i++) {
				it.advance();
				if(it.value() > maxBC) {
					maxBC = it.value();
					maxBCEdge = it.key();
				}
			}
			
			builder.removeEdge(projection, (EdgeDecorator<Edge>) maxBCEdge);
			SortedSet<Set<V>> partition = Partitions.disconnectedComponents(projection);
		
			logger.info(String.format("Done - disconnected components... %1$s", partition.size()));
			
			if(partition.size() != lastSize) {
				dendogram.add(partition);
				lastSize = partition.size();
				if(handler != null)
					handler.handlePartition(projection, partition, iteration, dendogram);
			}
			
			iteration++;
		}
		
		return dendogram;
	}
	
	public static <V extends Vertex> void writeDendogram(List<Set<Set<V>>> dendogram, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		for(Set<Set<V>> level : dendogram) {
			for(Set<?> cluster : level) {
				writer.write(String.valueOf(cluster.size()));
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
	}
	
	public interface Handler {
		
		public <V extends Vertex> void handlePartition(GraphProjection projection, Set<Set<V>> components, int iteration, List<Set<Set<V>>> dendogram);
		
	}
	
	public class DumpHandler implements Handler {

		public <V extends Vertex> void handlePartition(GraphProjection projection, Set<Set<V>> partition, int level, List<Set<Set<V>>> dendogram) {
			//************************************************
			SortedSet<Set<V>> components = new TreeSet<Set<V>>(new Comparator<Collection<?>>() {
				public int compare(Collection<?> o1, Collection<?> o2) {
					int result = o2.size() - o1.size();
					if(result == 0) {
						if(o1 == o2)
							return 0;
						else
							/*
							 * Does not work for empty collections, but is
							 * ok for the purpose here.
							 */
							return o2.hashCode() - o1.hashCode();
					} else
						return result;
				}
			});
			for(Set<V> cluster : partition) {
				Set<V> newCluster = new HashSet<V>();
				for(Vertex v : cluster)
					newCluster.add(((VertexDecorator<V>)v).getDelegate());
				components.add(newCluster);
			}
			
			//************************************************
			KMLWriter writer = new KMLWriter();
			writer.setCoordinateTransformation(new CH1903LV03toWGS84());
			writer.setDrawEdges(false);
			writer.setDrawNames(false);
			writer.setVertexDescriptor(new KMLVertexDescriptor((SpatialSparseGraph) projection.getDelegate()));
			writer.setVertexStyle(new KMLCommunityStlyle(writer.getVertexIconLink(), components));
			try {
				writer.write((SpatialSparseGraph) projection.getDelegate(), String.format("%1$s/%2$s.communityGraph.kmz", outputDir, level));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//************************************************
			try {
				writeDendogram(dendogram, String.format("%1$s/%2$s.dendogram.txt", outputDir, level));
				
				SpatialPajekWriter pwriter = new SpatialPajekWriter();
				pwriter.write((SpatialSparseGraph) projection.getDelegate(), new PajekCommunityColorizer(components), String.format("%1$s/%2$s.graph.net", outputDir, level));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String args[]) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader(21781);
		Graph graph = reader.readGraph(args[0]);
//		ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(new SparseGraphFactory());
//		System.out.println("Generating graph...");
//		SparseGraph graph = generator.generate(200, 0.02, 4711);
//		GMLReader reader = new GMLReader();
//		Graph graph = reader.read("/Users/fearonni/Downloads/karate/karate.gml", new SparseGraphFactory());
		
		
		
		GirvanNewmanAlgorithm algo = new GirvanNewmanAlgorithm();
		algo.outputDir = args[1];
		
		writeDendogram(algo.dendogram(graph, Integer.parseInt(args[2]), algo.new DumpHandler()), String.format("%1$s/dendogram.txt", algo.outputDir));
		
		
	}
}
