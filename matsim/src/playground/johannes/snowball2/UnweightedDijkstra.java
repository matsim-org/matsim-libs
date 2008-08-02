/* *********************************************************************** *
 * project: org.matsim.*
 * UnweightedDijkstra.java
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import playground.johannes.snowball2.Centrality.CentralityGraph;
import playground.johannes.snowball2.Centrality.CentralityVertex;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.importance.NodeRanking;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.random.generators.ErdosRenyiGenerator;
import edu.uci.ics.jung.statistics.GraphStatistics;

/**
 * @author illenberger
 *
 */
public class UnweightedDijkstra {
	
	public static double ratioSum;

	private Queue<DijkstraVertex> unsettledVertices;
	
	private DijkstraGraph g;
	
	public UnweightedDijkstra(CentralityGraph centralityGraph) {
		this.g = new DijkstraGraph(centralityGraph);
	}
	
	public List<CentralityVertex> run(CentralityVertex v) {
		DijkstraVertex source = g.getVertex(v); 
		unsettledVertices = new PriorityQueue<DijkstraVertex>(g.getVertices().size());
		for(SparseVertex dv : g.getVertices())
			((DijkstraVertex)dv).reset();
		
		source.setDistance(0);
		unsettledVertices.add(source);
		
		DijkstraVertex vertex;
		while((vertex = unsettledVertices.poll()) != null) {
			if(!vertex.isSettled()) {
				vertex.setSettled(true);
				
				for(SparseVertex sv : vertex.getNeighbours()) {
					DijkstraVertex neighbour = (DijkstraVertex)sv;
					if(!neighbour.isSettled()) {
						int d = vertex.getDistance() + 1; 
						if(d < neighbour.getDistance()) {
							neighbour.setPredecessor(vertex);
							neighbour.setDistance(d);
							if(neighbour.isVisited())
								unsettledVertices.remove(v);
							else
								neighbour.setVisited(true);
							
							unsettledVertices.add(neighbour);
							
						} else if(d == neighbour.getDistance()) {
							neighbour.addPredecessor(vertex);
						}
					}
				}
			}
		}
		
		int numVertex = 0;
		int numPathsTotal = 0;
		List<CentralityVertex> reachedVertices = new LinkedList<CentralityVertex>();
		
		int numPahts = 0;
		
		for (SparseVertex target : g.getVertices()) {
			if (target != source) {
				List<DijkstraVertex> vertexSet = new LinkedList<DijkstraVertex>();// (g.getVertices().size()*10);
				int[] paths = getPaths((DijkstraVertex) target, vertexSet);
				if (!vertexSet.isEmpty()) {
					vertexSet.remove(0);
					reachedVertices.add(((DijkstraVertex)target).getDelegate());

					numPathsTotal++;
					numVertex += paths[1];
					
					numPahts += paths[0];
					
					for (DijkstraVertex node : vertexSet) {
						node.getDelegate().addBetweenness(1 / (double) paths[0]);
					}
				}
			}
		}
		
		if(numPathsTotal == 0)
			v.setCloseness(0);
		else
			v.setCloseness(numVertex / (double)numPathsTotal);
		
//		System.out.println("Ration of path to vertices is " + numPathsTotal/(double)numPahts);
		ratioSum += numPathsTotal/(double)numPahts;
		return reachedVertices;
//		return numPathsTotal/(double)numPahts;
	}
	
	private int[] getPaths(DijkstraVertex target, Collection<DijkstraVertex> vertexSet) {
		
		if(target.getPredecessors().length == 1) {
			vertexSet.add(target);
			
			int[] paths = getPaths(target.getPredecessors()[0], vertexSet);
			
			paths[1]++;
			return paths;
		} else if(target.getPredecessors().length == 0) {
			return new int[]{1,0};
		} else {
			vertexSet.add(target);
			
			int[] paths = new int[]{0,0};

			for(DijkstraVertex v : target.getPredecessors()) {
				int[] pathsToPred = getPaths(v, vertexSet);
				paths[0] += pathsToPred[0];
				paths[1] = pathsToPred[1];
			}
			paths[1]++;
			
			return paths;
		}
	}
	
	public static void main(String args[]) {
		System.out.println("Loading graph...");
//		Graph g = new GraphMLFile().load(args[0]);
		Graph g = (Graph) new ErdosRenyiGenerator(10, 0.5).generateGraph();
		
		System.out.println("Converting graph...");
		SparseGraphDecorator sg = new SparseGraphDecorator(g);
		Set<SparseVertex> vertices = sg.getVertices();
		int size = vertices.size();
		
		System.out.println("Calculating shortest paths...");
		long time = System.currentTimeMillis();
//		int count = 0;
//		double sum = 0;
//		UnweightedDijkstra d = new UnweightedDijkstra(sg.getSparseGraph());
//		for(SparseVertex v : vertices) {
//			Collection<Collection<List<SparseVertex>>> paths = d.run(v);
//			
//			int vCount = 0;
//			int pCount = 0;
//			for(Collection<List<SparseVertex>> pathsPerTarget : paths) {
//				pCount += pathsPerTarget.size();
//				for(List<SparseVertex> path : pathsPerTarget) {
//					vCount += path.size();
//				}
//					
//			}
//			sum += vCount/(double)pCount;
//			count++;
//
//		}
		Centrality centrality = new Centrality();
		centrality.run(g, 0);
		System.out.println("Done. (" + (System.currentTimeMillis() - time)/1000.0f + " sec)");
		System.out.println("APL = " + centrality.getGraphCloseness());
		
//		double bcVal = 0;
//		for(SparseVertex v : vertices) {
//			bcVal += v.getBetweenness();
//		}
//		bcVal = bcVal/(double)count;
		System.out.println("Betweenness is " + centrality.getGraphBetweenness());
		
		double sum = 0;
		System.out.println("Calculating shortest paths (JUNG)...");
		time = System.currentTimeMillis();
		Map<Vertex, Double> aplMap = GraphStatistics.averageDistances(g);
		
		for(Double dd2 : aplMap.values())
			sum += dd2;
		
		System.out.println("Done. (" + (System.currentTimeMillis() - time)/1000.0f + " sec)");
		System.out.println("APL = " + sum/(double)aplMap.size());

		System.out.println("Calculating betweenness (JUNG)...");
		time = System.currentTimeMillis();
		BetweennessCentrality bc = new BetweennessCentrality(g, true, false);
		bc.evaluate();
		List<NodeRanking> rankings = bc.getRankings();
		double bcVal = 0;
		for(NodeRanking r : rankings)
			bcVal += r.rankScore;
		
		bcVal = bcVal/rankings.size();
		System.out.println("Done. (" + (System.currentTimeMillis() - time)/1000.0f + " sec)");
		System.out.println("Betweenness is " + bcVal);
	}
	
	private static class DijkstraGraph extends SparseGraph {

		private Map<SparseVertex, DijkstraVertex> vertexMapping = new LinkedHashMap<SparseVertex, DijkstraVertex>();
		
		public DijkstraGraph(SparseGraph g) {
			super(g.getVertices().size(), g.getEdges().size());
			
			for(SparseVertex v : g.getVertices()) {
				DijkstraVertex dv = new DijkstraVertex((CentralityVertex) v);
				addVertex(dv);
				vertexMapping.put(v, dv);
			}
			
			for(SparseEdge e : g.getEdges()) {
				DijkstraVertex v1 = vertexMapping.get(e.getEndPoints()[0]);
				DijkstraVertex v2 = vertexMapping.get(e.getEndPoints()[1]);
				addEdge(v1, v2);
			}
		}
		
		public DijkstraVertex getVertex(SparseVertex v) {
			return vertexMapping.get(v);
		}
	}
	
	private static class DijkstraVertex extends SparseVertex implements Comparable<DijkstraVertex>{
		
		private CentralityVertex delelgate;
		
		private boolean isSettled;
		
		private boolean isVisited;
		
		private int distance;
		
		private DijkstraVertex[] predecessors;
		
		private int numPathsOver;
		
		private int numVertexTotal;
		
		private int numPathFrom;
		
		public DijkstraVertex(CentralityVertex v) {
			delelgate = v;
		}

		public CentralityVertex getDelegate() {
			return delelgate;
		}
		
		public boolean isSettled() {
			return isSettled;
		}
		
		public void setSettled(boolean flag) {
			isSettled = flag;
		}
		
		public boolean isVisited() {
			return isVisited;
		}
		
		public void setVisited(boolean flag) {
			isVisited = flag;
		}
		
		public int getDistance() {
			return distance;
		}
		
		public void setDistance(int d) {
			distance = d;
		}
		
		public DijkstraVertex[] getPredecessors() {
			return predecessors;
		}
		
		public void setPredecessor(DijkstraVertex v) {
			if(predecessors.length != 1)
				predecessors = new DijkstraVertex[1];
			predecessors[0] = v;
		}
		
		public void addPredecessor(DijkstraVertex v) {
			DijkstraVertex[] newPredecessors = new DijkstraVertex[predecessors.length + 1];
			for(int i = 0; i < predecessors.length; i++)
				newPredecessors[i] = predecessors[i];
			newPredecessors[predecessors.length] = v;
		}
		
		public void reset() {
			isSettled = false;
			isVisited = false;
			distance = Integer.MAX_VALUE;
			predecessors = new DijkstraVertex[0];
		}
		
		public int compareTo(DijkstraVertex o) {
			int result = this.distance - o.distance;
			if(result == 0) {
				if(o.equals(this))
					return 0;
				else
					return this.hashCode() - o.hashCode();
			} else {
				return result;
			}
		}
	}
}
