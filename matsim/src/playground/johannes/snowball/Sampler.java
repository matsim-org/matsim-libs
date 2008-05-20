/* *********************************************************************** *
 * project: org.matsim.*
 * Sampler.java
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
package playground.johannes.snowball;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import playground.johannes.socialnets.UserDataKeys;
import cern.colt.list.IntArrayList;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 * 
 */
public class Sampler {

	private long seed;
	
	private Random rnd;
	
	private double pTieNamed = 1;
	
	private double pParticipate = 1;
	
	public Sampler(long randomSeed) {
		seed = randomSeed;
	}

	public void setPTieNamed(double p) {
		pTieNamed = p;
	}
	
	public void setPParticipate(double p) {
		pParticipate = p;
	}
	
	private void init(Graph g) {
		for(Object v : g.getVertices()) {
			((Vertex)v).removeUserDatum(UserDataKeys.PARTICIPATE_KEY);
			((Vertex)v).removeUserDatum(UserDataKeys.WAVE_KEY);
		}
		
		for(Object e : g.getEdges()) {
			((Edge)e).removeUserDatum(UserDataKeys.WAVE_KEY);
		}
	}
	
	public void run(Graph g, int waves, int initialEgos) {
		rnd = new Random(seed);
		init(g);
		Collection<Vertex> egos = selectedIntialEgos(g, initialEgos);

		for (Vertex ego : egos) {
			tagAsVisited(ego, 0);
		}

		for (int wave = 1; wave <= waves; wave++) {
			if(wave == 1)
				System.out.print("Sampling wave " + wave + "...");
			else
				System.out.print(wave + "...");
			egos = runWave(egos, wave);
		}
		System.out.println(" done.");
	}

	@SuppressWarnings("unchecked")
	private Collection<Vertex> selectedIntialEgos(Graph g, int count) {
		List<Vertex> edges = new LinkedList<Vertex>(g.getVertices());
		Collections.shuffle(edges, rnd);
		return edges.subList(0, count);
	}

	private Collection<Vertex> runWave(Collection<Vertex> egos, int wave) {
		Collection<Vertex> alters = new LinkedList<Vertex>();
		/*
		 * Expand to each ego's alters.
		 */
		for (Vertex ego : egos) {
			rnd.nextDouble();
			/*
			 * The initial egos (first wave) always participate.
			 */
			if(wave == 1 || getProbaParticipate(ego) >= rnd.nextDouble()) {
				ego.addUserDatum(UserDataKeys.PARTICIPATE_KEY, true, UserDataKeys.COPY_ACT);
				alters.addAll(expand(ego, wave));
			}
		}

		return alters;
	}

	private void tagAsVisited(Vertex v, int wave) {
		/*
		 * Tag the destination vertex.
		 */
		IntArrayList vertexVisits = (IntArrayList) v.getUserDatum(UserDataKeys.WAVE_KEY);
		if (vertexVisits == null) {
			vertexVisits = new IntArrayList();
			v.addUserDatum(UserDataKeys.WAVE_KEY, vertexVisits, UserDataKeys.COPY_ACT);
		}
		vertexVisits.add(wave);
	}

	private void tagAsVisited(Edge e, int wave) {
		/*
		 * Tag the edge.
		 */
		IntArrayList edgeVisits = (IntArrayList) e.getUserDatum(UserDataKeys.WAVE_KEY);
		if (edgeVisits == null) {
			edgeVisits = new IntArrayList();
			e.addUserDatum(UserDataKeys.WAVE_KEY, edgeVisits, UserDataKeys.COPY_ACT);
		}
		edgeVisits.add(wave);
	}

	@SuppressWarnings("unchecked")
	private Collection<Vertex> expand(Vertex ego, int wave) {
		List<Vertex> sampledAlters = new LinkedList<Vertex>();
		Set<Edge> ties = ego.getOutEdges();

		for (Edge e : ties) {
			Vertex alter = e.getOpposite(ego);
			/*
			 * Do not go back to the ego and do not sample egos that have been
			 * already visited in a previous wave.
			 */
//			if (!alter.equals(ego)) {
				rnd.nextDouble();
				if (getProbaTieNamed(ego, alter) >= rnd.nextDouble()) {
					if (alter.getUserDatum(UserDataKeys.WAVE_KEY) == null) {
						sampledAlters.add(alter);
					}
					tagAsVisited(e, wave);
					tagAsVisited(alter, wave);
				}
//			}
		}

		return sampledAlters;
	}

	private double getProbaTieNamed(Vertex ego, Vertex alter) {
		return pTieNamed;
	}
	
	private double getProbaParticipate(Vertex ego) {
		return pParticipate;
	}

	public Graph extractSampledGraph(Graph fullGraph, boolean extend) {
		Graph sampledGraph = new SparseGraph();
		Map<Vertex, Vertex> vertexMapping = new HashMap<Vertex, Vertex>();

		for (Object v : fullGraph.getVertices()) {
			IntArrayList waves = (IntArrayList) ((Vertex) v)
					.getUserDatum(UserDataKeys.WAVE_KEY);
			if (waves != null) {
				Vertex newVertex = cloneVertex((Vertex)v);
				sampledGraph.addVertex(newVertex);
				vertexMapping.put((Vertex) v, newVertex);
			}
		}

		for (Object e : fullGraph.getEdges()) {
			IntArrayList waves = (IntArrayList) ((Edge) e)
					.getUserDatum(UserDataKeys.WAVE_KEY);
			Pair endPoints = ((Edge) e).getEndpoints();
			if (waves != null) {
				Edge newEdge = new UndirectedSparseEdge(vertexMapping
						.get(endPoints.getFirst()), vertexMapping.get(endPoints
						.getSecond()));
				newEdge.addUserDatum(UserDataKeys.WAVE_KEY, waves, UserDataKeys.COPY_ACT);
				sampledGraph.addEdge(newEdge);
			} else if(extend) {
				/*
				 * Check if one of the end points has been sampled.
				 */
				Vertex v1 = (Vertex)endPoints.getFirst();
				Vertex v2 = (Vertex)endPoints.getSecond();
				
				boolean notSampled = false;
				if(v1.getUserDatum(UserDataKeys.WAVE_KEY) != null)
					notSampled = true;
				else if(v2.getUserDatum(UserDataKeys.WAVE_KEY) != null)
					notSampled = true;
				
				if(notSampled) {
					Vertex v1clone = vertexMapping.get(v1);
					if(v1clone == null) {
						v1clone = cloneVertex(v1);
						sampledGraph.addVertex(v1clone);
					}
					Vertex v2clone = vertexMapping.get(v2);
					if(v2clone == null) {
						v2clone = cloneVertex(v2);
						sampledGraph.addVertex(v2clone);
					}
					
					Edge newEdge = new UndirectedSparseEdge(v1clone, v2clone);
					sampledGraph.addEdge(newEdge);
				}
			}
		}

		return sampledGraph;
	}

	private Vertex cloneVertex(Vertex v) {
		Vertex newVertex = new UndirectedSparseVertex();
		newVertex.addUserDatum(UserDataKeys.ID, ((Vertex)v).getUserDatum(UserDataKeys.ID), UserDataKeys.COPY_ACT);
		newVertex.addUserDatum(UserDataKeys.X_COORD, ((Vertex)v).getUserDatum(UserDataKeys.X_COORD), UserDataKeys.COPY_ACT);
		newVertex.addUserDatum(UserDataKeys.Y_COORD, ((Vertex)v).getUserDatum(UserDataKeys.Y_COORD), UserDataKeys.COPY_ACT);
		IntArrayList waves = (IntArrayList) ((Vertex)v).getUserDatum(UserDataKeys.WAVE_KEY); 
		if(waves != null)
			newVertex.addUserDatum(UserDataKeys.WAVE_KEY, waves, UserDataKeys.COPY_ACT);
		Boolean bool = (Boolean)((Vertex)v).getUserDatum(UserDataKeys.PARTICIPATE_KEY);
		if(bool != null)
			newVertex.addUserDatum(UserDataKeys.PARTICIPATE_KEY, bool, UserDataKeys.COPY_ACT);
		
		return newVertex;
	}
	
	public void removeDeadEnds(Graph g) {
		Set<Edge> edges = new HashSet<Edge>();
		Set<Vertex> vertices = new HashSet<Vertex>();
		for(Object v : g.getVertices()) {
			if(((Vertex)v).degree() == 1) {
				Edge e = (Edge) ((Vertex)v).getIncidentEdges().iterator().next();
//				g.removeEdge(e);
				edges.add(e);
//				g.removeVertex((Vertex)v);
				vertices.add((Vertex)v);
			}
		}
		
		for(Edge e : edges)
			g.removeEdge(e);
		
		for(Vertex v : vertices)
			g.removeVertex(v);
	}
	
	public void removeIsolates(Graph g) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		for(Object v : g.getVertices())
			if(((Vertex)v).degree() == 0)
				vertices.add((Vertex) v);
		
		for(Vertex v : vertices)
			g.removeVertex(v);
	}
	
//	public static void main(String args[]) {
//		Config config = Gbl.createConfig(args);
//		
//		final String MODULE_NAME = "snowballsampling";
//		String graphFile = config.getParam(MODULE_NAME, "graphFile");
//		String graphPajekFile = config.getParam(MODULE_NAME, "graphPajekFile");
//		String sampledPajekFile = config.getParam(MODULE_NAME, "sampledPajekFile");
//		String outDir ="";
//		
//		/*
//		 * Load the social network...
//		 */
//		logger.info("Loading social network...");
//		PersonGraphMLFileHandler fileHandler = new PersonGraphMLFileHandler();
//		GraphMLFile gmlFile = new GraphMLFile(fileHandler);
//		Graph g = gmlFile.load(graphFile);
//			
//		logger.info("Graph has " + g.numVertices() +" vertices, " + g.numEdges());
//		Histogram1D h = GraphStatistics.createClusteringCoefficientsHistogram(g, 0);
//		GraphStatistics.saveHistogram(h, "/Users/fearonni/vsp-work/socialnets/devel/snowball/cc-distr-orig.txt");
//			/*
//			 * Simulate snowball sampling...
//			 */
//			Sampler s = new Sampler(config.global().getRandomSeed());
//			s.run(g, 3, 5);
//			/*
//			 * Compute statistics...
//			 */
//			Map<String, Integer> sampledVertices = SampleStatistics.countSampledVertices(g);
//			StringBuilder sBuilder = new StringBuilder();
//			sBuilder.append("Sampled vertex statistics:\n");
//			for(String key : sampledVertices.keySet()) {
//				sBuilder.append("\t");
//				sBuilder.append(key);
//				sBuilder.append(":\t");
//				sBuilder.append(sampledVertices.get(key));
//				sBuilder.append("\n");
//			}
//			logger.info(sBuilder.toString());
//			
//			Map<String, Integer> sampledEdges = SampleStatistics.countSampledEdges(g);
//			sBuilder = new StringBuilder();
//			sBuilder.append("Sampled edge statistics:\n");
//			for(String key : sampledEdges.keySet()) {
//				sBuilder.append("\t");
//				sBuilder.append(key);
//				sBuilder.append(":\t");
//				sBuilder.append(sampledEdges.get(key));
//				sBuilder.append("\n");
//			}
//			logger.info(sBuilder.toString());
//			
//			Graph extSampledGraph = s.extractSampledGraph(g, true);
//			Graph reducedSampledGraph = s.extractSampledGraph(g, false);
////			s.removeDeadEnds(reducedSampledGraph);
//			
//			logger.info("Mean degrees: observed: " + 
//					GraphStatistics.meanDegree(g) + 
//					"\tsampled: "+GraphStatistics.meanDegreeSampled(extSampledGraph));
//			logger.info("Mean clustering coefficient: observed: " + 
//					GraphStatistics.meanClusterCoefficient(g) + 
//					"\tsampled: "+GraphStatistics.meanClusterCoefficient(reducedSampledGraph));
//
////			logger.info("Mean clustering coefficient: observed: " + 
////					0 + 
////					"\tsampled: "+GraphStatistics.meanClusterCoefficient(reducedSampledGraph));
//			
//			WeakComponentClusterer wcc = new WeakComponentClusterer();
//			ClusterSet observerCluster = wcc.extract(g);
//			ClusterSet sampledCluster = wcc.extract(reducedSampledGraph);
//			logger.info("Weak components: observed: " + 
//					observerCluster.size() +" components;\tsampled: " +
//					sampledCluster.size() + " components.");
//			
//			logger.info("Isolated nodes: observer: " + GraphStatistics.countIsolates(g) +
//					";\tsampled: " + GraphStatistics.countIsolates(extSampledGraph));
//			
//			h = GraphStatistics.createClusteringCoefficientsHistogram(reducedSampledGraph, 0);
//			GraphStatistics.saveHistogram(h, "/Users/fearonni/vsp-work/socialnets/devel/snowball/cc-distr-sampled.txt");
//			/*
//			 * Dump graph for visualization in Pajek.
//			 */
//			PajekVisWriter w = new PajekVisWriter();
//			w.write(extSampledGraph, sampledPajekFile);
//			w.write(g, graphPajekFile);
//		
//	}
}
