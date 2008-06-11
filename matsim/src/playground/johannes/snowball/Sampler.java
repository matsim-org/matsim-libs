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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserDataDelegate;

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
		rnd = new Random(seed);
	}

	public void setPTieNamed(double p) {
		pTieNamed = p;
	}
	
	public void setPParticipate(double p) {
		pParticipate = p;
	}
	
	public void init(Graph g) {
		for(Object v : g.getVertices()) {
			((Vertex)v).removeUserDatum(UserDataKeys.SAMPLED_KEY);
			((Vertex)v).removeUserDatum(UserDataKeys.DETECTED_KEY);
			((Vertex)v).removeUserDatum(UserDataKeys.ANONYMOUS_KEY);
		}
		
		for(Object e : g.getEdges()) {
			((Edge)e).removeUserDatum(UserDataKeys.SAMPLED_KEY);
		}
	}
	
	public void run(Graph g, int waves, int initialEgos) {
		init(g);
		Collection<Vertex> egos = selectedIntialEgos(g, initialEgos);

		for (Vertex ego : egos) {
			tagAsDetected(ego);
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
	public Collection<Vertex> selectedIntialEgos(Graph g, int count) {
		List<Vertex> vertices = new LinkedList<Vertex>(g.getVertices());
		Collections.shuffle(vertices, rnd);
		
		List<Vertex> egos = vertices.subList(0, count);
		for (Vertex ego : egos) {
			tagAsDetected(ego);
		}
		return egos;
	}

	public Collection<Vertex> runWave(Collection<Vertex> egos, int wave) {
		Collection<Vertex> alters = new LinkedHashSet<Vertex>();
		/*
		 * Expand to each ego's alters.
		 */
		for (Vertex ego : egos) {
			rnd.nextDouble();
			/*
			 * The initial egos (first wave) always participate.
			 */
			if(wave == 1 || getProbaParticipate(ego) >= rnd.nextDouble()) {
//				ego.addUserDatum(UserDataKeys.PARTICIPATE_KEY, true, UserDataKeys.COPY_ACT);
				if(ego.getUserDatum(UserDataKeys.SAMPLED_KEY) == null) {
					/*
					 * FIXME: Dunno why this happends?
					 */
					tagAsSampled(ego, wave);
					alters.addAll(expand(ego, wave));
				}
			}
		}

		return alters;
	}

	private void tagAsSampled(Vertex v, int wave) {
		v.addUserDatum(UserDataKeys.SAMPLED_KEY, wave, UserDataKeys.COPY_ACT);
		/*
		 * The vertex is no more anonymous.
		 */
		tagAsAnonymous(v, false);
	}

	private void tagAsSampled(Edge e, int wave) {
		if(e.getUserDatum(UserDataKeys.SAMPLED_KEY) == null)
			e.addUserDatum(UserDataKeys.SAMPLED_KEY, wave, UserDataKeys.COPY_ACT);
	}
	
	private void tagAsAnonymous(Vertex v, boolean flag) {
		if(flag) {
			if(v.getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null)
				v.addUserDatum(UserDataKeys.ANONYMOUS_KEY, true, UserDataKeys.COPY_ACT);
			else
				v.setUserDatum(UserDataKeys.ANONYMOUS_KEY, true, UserDataKeys.COPY_ACT);
		} else {
			v.removeUserDatum(UserDataKeys.ANONYMOUS_KEY);
		}
	}

	private void tagAsDetected(Vertex v) {
		Integer i = (Integer) v.getUserDatum(UserDataKeys.DETECTED_KEY);
		if(i == null)
			i = 0;
		i++;
		v.setUserDatum(UserDataKeys.DETECTED_KEY, i, UserDataKeys.COPY_ACT);
	}
	
	@SuppressWarnings("unchecked")
	private Collection<Vertex> expand(Vertex ego, int wave) {
		Set<Vertex> detectedAlters = new LinkedHashSet<Vertex>();
		Set<Edge> ties = ego.getOutEdges();

		for (Edge e : ties) {
			Vertex alter = e.getOpposite(ego);
			/*
			 * Do not sample egos that have been already visited in a previous wave.
			 */
			rnd.nextDouble();
			if (getProbaTieNamed(ego, alter) >= rnd.nextDouble()) {
				if (alter.getUserDatum(UserDataKeys.SAMPLED_KEY) == null) {
					detectedAlters.add(alter);
					tagAsAnonymous(alter, true);
				}
				tagAsDetected(alter);
				tagAsSampled(e, wave);
			}
		}

		return detectedAlters;
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
//			Boolean sampled = (Boolean) ((Vertex) v)
//					.getUserDatum(UserDataKeys.SAMPLED_KEY);
//			Boolean flag = (Boolean)((Vertex) v).getUserDatum(UserDataKeys.ANONYMOUS_KEY);
			Integer detected = (Integer)((Vertex) v).getUserDatum(UserDataKeys.DETECTED_KEY);
			if (detected != null) {
				Vertex newVertex = cloneVertex((Vertex)v);
				sampledGraph.addVertex(newVertex);
				vertexMapping.put((Vertex) v, newVertex);
			}
		}

		for (Object e : fullGraph.getEdges()) {
//			IntArrayList waves = (IntArrayList) ((Edge) e)
//					.getUserDatum(UserDataKeys.WAVE_KEY);
			Integer sampled = (Integer)((Edge) e).getUserDatum(UserDataKeys.SAMPLED_KEY);
			Pair endPoints = ((Edge) e).getEndpoints();
			if (sampled != null) {
				Edge newEdge = new UndirectedSparseEdge(vertexMapping
						.get(endPoints.getFirst()), vertexMapping.get(endPoints
						.getSecond()));
//				newEdge.addUserDatum(UserDataKeys.WAVE_KEY, waves, UserDataKeys.COPY_ACT);
				sampledGraph.addEdge(newEdge);
			} else if(extend) {
				/*
				 * Check if one of the end points has been sampled.
				 */
				Vertex v1 = (Vertex)endPoints.getFirst();
				Vertex v2 = (Vertex)endPoints.getSecond();
				
				boolean notSampled = false;
				if(v1.getUserDatum(UserDataKeys.DETECTED_KEY) != null)
					notSampled = true;
				else if(v2.getUserDatum(UserDataKeys.DETECTED_KEY) != null)
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
		
		Boolean anonymous = (Boolean)((Vertex)v).getUserDatum(UserDataKeys.ANONYMOUS_KEY);
		if(anonymous != null)
			newVertex.addUserDatum(UserDataKeys.ANONYMOUS_KEY, anonymous, UserDataKeys.COPY_ACT);
		
		Integer sampled = (Integer)((Vertex)v).getUserDatum(UserDataKeys.SAMPLED_KEY);
		if(sampled != null)
			newVertex.addUserDatum(UserDataKeys.SAMPLED_KEY, sampled, UserDataKeys.COPY_ACT);
		
		Integer detected = (Integer)((Vertex)v).getUserDatum(UserDataKeys.DETECTED_KEY);
		if(detected != null)
			newVertex.addUserDatum(UserDataKeys.DETECTED_KEY, detected, UserDataKeys.COPY_ACT);
		
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
	
	public void calculateSampleProbas(Graph g, double fracSampled) {
		Set<Vertex> vertices = new HashSet<Vertex>(g.getVertices());
		
		for(Vertex v : vertices) {
			if(v.getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null) {
				Double proba = (Double)v.getUserDatum(UserDataKeys.SAMPLE_PROBA_KEY);
				if(proba == null) {
					double p = 1 - Math.pow(1 - fracSampled, v.degree());
					v.addUserDatum(UserDataKeys.SAMPLE_PROBA_KEY, p, UserDataKeys.COPY_ACT);
				}
			}
		}
		
//		for(Vertex v : vertices) {
//			v.removeUserDatum(UserDataKeys.SAMPLE_PROBA_KEY);
//			double[] sampleProbas = new double[wave];
//			sampleProbas[0] = seedProba;
//			v.addUserDatum(UserDataKeys.SAMPLE_PROBA_KEY, sampleProbas, UserDataKeys.COPY_ACT);
//		}
		
//		for(int i = 1; i < wave; i++) {
//			for(Vertex v : vertices) {
//				double[] sampleProbas = (double[])v.getUserDatum(UserDataKeys.SAMPLE_PROBA_KEY);
//				double accumulatetProba = 1;
//				for(int k = 0; k < i; k++) {
//					double product = 1;
//					for(Object neighbour : v.getNeighbors()) {
//						if(((Vertex)neighbour).getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null) {
//							double[] sampleProbasNeighb =   (double[])((UserDataDelegate)neighbour).getUserDatum(UserDataKeys.SAMPLE_PROBA_KEY);
//							product *= (1 - sampleProbasNeighb[k]);
//						}
//					}
//					accumulatetProba *= product;	
//				}
//				sampleProbas[i] = 1 - accumulatetProba;
//			}
//		}
		
//		for(int i = 1; i < wave; i++) {
//			for(Vertex v : vertices) {
//				double[] sampleProbas = (double[])v.getUserDatum(UserDataKeys.SAMPLE_PROBA_KEY);
//				double product = 1;
//				for(Object neighbour : v.getNeighbors()) {
//					if(((Vertex)neighbour).getUserDatum(UserDataKeys.ANONYMOUS_KEY) == null) {
//						double[] sampleProbasNeighb = (double[])((UserDataDelegate)neighbour).getUserDatum(UserDataKeys.SAMPLE_PROBA_KEY);
//						product *= (1 - sampleProbasNeighb[i-1]);
//					}
//				}
//				sampleProbas[i] = 1 - product;
//			}
//		}
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
