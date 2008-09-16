/* *********************************************************************** *
 * project: org.matsim.*
 * TypeClusters.java
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.UserDataKeys;
import playground.johannes.statistics.GraphStatistics;
import playground.johannes.statistics.SampledGraphStatistics;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class TypeClusters extends GraphStatistic {

	private BufferedWriter sumWriter; 
	
	private Set<String> types;
	
	private Map<String, Integer> total;
	
	private Graph g;
	
	public TypeClusters(String outputDir) {
		super(outputDir);
		
		if(outputDir != null && g != null) {
			types = getTypes(g);
			total = getVerticesPerType(g.getVertices());
			try {
				sumWriter = IOUtils.getBufferedWriter(outputDir + "total.txt");
				
				for(String type : types) {
					sumWriter.write("type" + type);
					sumWriter.write("\t");
				}
				sumWriter.write("heteroEdges");
				sumWriter.newLine();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public DescriptiveStatistics calculate(Graph g, int iteration, DescriptiveStatistics reference) {
		
		Set<Vertex> vertices = g.getVertices();
		Map<String, Collection<Vertex>> clusters = new HashMap<String, Collection<Vertex>>();
		for(Vertex v : vertices) {
			String type = (String)v.getUserDatum(UserDataKeys.TYPE_KEY);
			if(type != null) {
				Collection<Vertex> cluster = clusters.get(type);
				if(cluster == null) {
					cluster = new LinkedList<Vertex>();
					clusters.put(type, cluster);
				}
				cluster.add(v);
			}
		}
		
		Map<String, Graph> subGraphs = new HashMap<String, Graph>();
		for(String type : clusters.keySet()) {
			subGraphs.put(type, extractCluster(clusters.get(type)));
		}
		
		DescriptiveStatistics degree;
		DescriptiveStatistics clustering;
		if(g instanceof SampledGraph) {
			degree = SampledGraphStatistics.getDegreeStatistics((SampledGraph) g);
			clustering = SampledGraphStatistics.getClusteringStatistics((SampledGraph)g);
		} else {
			degree = GraphStatistics.getDegreeStatistics(g);
			clustering = GraphStatistics.getClusteringStatistics(g);
		}
//		CountComponents cc = new CountComponents(null, null);
		for(String type : clusters.keySet()) {
			Graph subGraph = subGraphs.get(type);
			float z = (float)degree.getMean();
			float c = (float)clustering.getMean();
			int components = GraphStatistics.getDisconnectedComponents(subGraph).size();
			
			System.out.println(String.format("Subgraph of type %1$s has %2$s vertices, %3$s components, mean degree %4$s and mean clustering %5$s.",
											type, subGraph.numVertices(), components, z, c));
		}
		
		return new DescriptiveStatistics();
	}

	private Graph extractCluster(Collection<Vertex> cluster) {
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		Map<Vertex, UndirectedSparseVertex> mapping = new HashMap<Vertex, UndirectedSparseVertex>();
		for (Vertex v : cluster) {
			UndirectedSparseVertex vCopy = new UndirectedSparseVertex();
			g.addVertex(vCopy);
			mapping.put(v, vCopy);
		}
		for (Vertex v : cluster) {
			Set<Edge> edges = v.getIncidentEdges();
			for (Edge e : edges) {
				UndirectedSparseVertex v1 = mapping.get(v);
				UndirectedSparseVertex v2 = mapping.get(e.getOpposite(v));
				if (v2 != null) {
					UndirectedSparseEdge eCopy = new UndirectedSparseEdge(v1, v2);
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
	
	public void dumpComponentSummary(String filename, Set<Collection<Vertex>> cSet) {
		if(cSet != null) {
			try {
				
				BufferedWriter writer = IOUtils.getBufferedWriter(filename);
				writer.write("size");
				for(String type : types) {
					writer.write("\t");
					writer.write(type);
				}
				writer.newLine();

				Map<String, Integer> totalMap = new HashMap<String, Integer>();
				for(Collection<Vertex> cluster : cSet) {
					writer.write(String.valueOf(cluster.size()));
					Map<String, Integer> vMap = getVerticesPerType(cluster);

					for(String type : types) {
						writer.write("\t");
						Integer count = vMap.get(type);
						int cnt = 0;
						if(count == null)
							writer.write("0");
						else {
							writer.write(String.valueOf(count));
							cnt = count.intValue();
						}
						
						Integer total = totalMap.get(type);
						int sum = 0;
						if(total != null)
							sum = total.intValue();
						sum += cnt;
						totalMap.put(type, sum);
					}
					writer.newLine();
				}
				
				int heteroEdges = countHeterophileEdges(g);
				writer.write("hetero edges : ");
				writer.write(String.valueOf(heteroEdges));
				writer.close();
				
				for(String type : types) {
					Integer count = totalMap.get(type);
					if(count == null)
						sumWriter.write("0");
					else {
						int sum = total.get(type);
						sumWriter.write(String.valueOf(count/(double)sum));
					}
					
					sumWriter.write("\t");
				}
				sumWriter.write(String.valueOf(heteroEdges));
				sumWriter.newLine();
				sumWriter.flush();
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Set<String> getTypes(Graph g) {
		Set<String> types = new LinkedHashSet<String>();
		Set<Vertex> vertices = g.getVertices();
		for(Vertex v : vertices) {
			String type = (String)v.getUserDatum(UserDataKeys.TYPE_KEY);
			if(type != null)
				types.add(type);
		}
		return types;
	}
	
	private Map<String, Integer> getVerticesPerType(Collection<Vertex> cluster) {
		Map<String, Integer> vMap = new HashMap<String, Integer>();
		for(Vertex v : cluster) {
			String type = (String)v.getUserDatum(UserDataKeys.TYPE_KEY);
			if(type != null) {
				Integer count = vMap.get(type);
				int cnt = 0;
				if(count != null)
					cnt = count.intValue();
				cnt++;
				vMap.put(type, cnt);
			}
		}
		return vMap;
	}
	
	private int countHeterophileEdges(Graph g) {
		Set<Edge> edges = g.getEdges();
		int count = 0;
		for(Edge e : edges) {
			Pair p = e.getEndpoints();
			String v1 = (String)((Vertex)p.getFirst()).getUserDatum(UserDataKeys.TYPE_KEY);
			String v2 = (String)((Vertex)p.getSecond()).getUserDatum(UserDataKeys.TYPE_KEY);
			if(v1 != null && !v1.equals(v2))
				count++;
			
		}
		return count;
	}
}
