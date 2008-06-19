/* *********************************************************************** *
 * project: org.matsim.*
 * CountComponents.java
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball2.Centrality.CentralityGraph;
import playground.johannes.snowball2.Centrality.CentralityGraphDecorator;
import playground.johannes.snowball2.Centrality.CentralityVertex;
import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class CountComponents implements GraphStatistic {
	
	private BufferedWriter sumWriter; 
	
	private Collection<Collection<Vertex>> cSet;

	private Graph g;
	
//	private Graph originalGraph;
	
	private Set<String> types;
	
	private Map<String, Integer> total;
	
	private class SizeComparator implements Comparator<Collection<?>> {

		public int compare(Collection<?> o1, Collection<?> o2) {
			int result = o1.size() - o2.size();
			if(result == 0) {
				if(o1 == o2)
					return 0;
				else
					return o1.hashCode() - o2.hashCode();
			} else
				return result;
		}
		
	}
	
	public CountComponents(String outputDir, Graph g) {
//		originalGraph = g;
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
	
	public double run(Graph g) {
		this.g = g;
		cSet = new TreeSet<Collection<Vertex>>(new SizeComparator());
		CentralityGraphDecorator graphDecorator = new CentralityGraphDecorator(g);
		UnweightedDijkstra dijkstra = new UnweightedDijkstra((CentralityGraph) graphDecorator.getSparseGraph());
		Queue<CentralityVertex> vertices = new LinkedList<CentralityVertex>((Collection<? extends CentralityVertex>) graphDecorator.getSparseGraph().getVertices());
		CentralityVertex source;
		while((source = vertices.poll()) != null) {
			List<CentralityVertex> reached = dijkstra.run(source);
			reached.add(source);
			List<Vertex> reached2 = new LinkedList<Vertex>();
			for(CentralityVertex cv : reached)
				reached2.add(graphDecorator.getVertex(cv));
			cSet.add(reached2);
			vertices.removeAll(reached);
		}
		
		return cSet.size();
	}

	public Collection<Collection<Vertex>> getClusterSet() {
		return cSet;
	}
	
	public void dumpComponentSummary(String filename) {
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
