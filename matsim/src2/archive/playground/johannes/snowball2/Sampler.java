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
package playground.johannes.snowball2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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

/**
 * @author illenberger
 * 
 */
public class Sampler {
	
	private static final String SAMPLED_KEY = "sampled";
	
	private static final String SAMPLED_ELEMENT_KEY = "sampledelement";
	
	private static final String SAMPLE_PROBA = "sampleproba";
	
	private static final String NON_RESPONDING = "nonresponding";
	
	private Random rnd;
	
	private double pTieNamed = 1;
	
	private double pResponse = 1;
	
	private double pFOF = 0;
	
	private SampledGraph sampledGraph;
	
	private int currentWave;
	
	private Collection<Vertex> lastSampledVertices;
	
	private Collection<Vertex> seeds;
	
	private final int numSeeds;
	
	private List<Map<Integer, Integer>> verticesPerDegree = new ArrayList<Map<Integer,Integer>>();
	
	public Sampler(Graph g, int nSeeds, long randomSeed) {
		rnd = new Random(randomSeed);
		numSeeds = nSeeds;
		for(Object v : g.getVertices()) {
			((Vertex)v).removeUserDatum(SAMPLED_ELEMENT_KEY);
			((Vertex)v).removeUserDatum(SAMPLED_KEY);
		}
		
		List<Vertex> vertices = new LinkedList<Vertex>(g.getVertices());
		Collections.shuffle(vertices, rnd);
		
//		for(Vertex v : vertices) {
//			if(v.getUserDatum("type").equals("1")) {
//				lastSampledVertices = new ArrayList<Vertex>();
//				lastSampledVertices.add(v);
//				break;
//			}
//		}
//		int i = 0;
//		for(Vertex v : vertices) {
//			if(v.getUserDatum("type").equals("2")) {
////				lastSampledVertices = new ArrayList<Vertex>();
//				lastSampledVertices.add(v);
//				i++;
//				if(i == 2)
//					break;
//			}
//		}
		
		lastSampledVertices = vertices.subList(0, nSeeds);
		sampledGraph = new SampledGraph();
		seeds = new LinkedList<Vertex>();
		for(Vertex v : lastSampledVertices) {
			SampledVertex vSampled = new SampledVertex(currentWave);
			for(Iterator<String> it = v.getUserDatumKeyIterator(); it.hasNext();) {
				String key = it.next();
				vSampled.addUserDatum(key, v.getUserDatum(key), UserDataKeys.COPY_ACT);
			}
			sampledGraph.addVertex(vSampled);
			seeds.add(vSampled);
			v.setUserDatum(SAMPLED_ELEMENT_KEY, vSampled, UserDataKeys.COPY_ACT);
			System.out.println("Type of seed is " + v.getUserDatum("type"));
		}
		currentWave = -1;
	}

	public Collection<Vertex> getSeeds() {
		return seeds;
	}
	
	public void setPTieNamed(double p) {
		pTieNamed = p;
	}
	
	public void setPResponse(double p) {
		pResponse = p;
	}

	public void setPFOF(double p) {
		pFOF = p;
	}
	
	public SampledGraph runWave() {
		currentWave++;
		Collection<Vertex> alters = new LinkedHashSet<Vertex>();
		/*
		 * Expand to each ego's alters.
		 */
		for (Vertex ego : lastSampledVertices) {
			rnd.nextDouble();
			/*
			 * The initial egos (first wave) always participate.
			 */
			if(currentWave == 0 || getProbaParticipate(ego) >= rnd.nextDouble()) {
				Boolean nonResponding = (Boolean) ego.getUserDatum(NON_RESPONDING);
				if (nonResponding == null || !nonResponding) {
					ego.setUserDatum(NON_RESPONDING, false, UserDataKeys.COPY_ACT);
					
					if (ego.getUserDatum(UserDataKeys.SAMPLED_KEY) == null) {
						/*
						 * FIXME: Dunno why this happends?
						 */
						alters.addAll(expand(ego));
					}
				}
			} else {
				ego.setUserDatum(NON_RESPONDING, true, UserDataKeys.COPY_ACT);
			}
		}

		lastSampledVertices = alters;
		
		return sampledGraph;
	}
	
	@SuppressWarnings("unchecked")
	private Collection<Vertex> expand(Vertex ego) {
		SampledVertex sampledEgo = (SampledVertex) ego.getUserDatum(SAMPLED_ELEMENT_KEY);
		sampledEgo.setSampled(currentWave);
//		sampledEgo.increaseVisited();
		ego.addUserDatum(SAMPLED_KEY, true, UserDataKeys.COPY_ACT);
		
		Set<Vertex> detectedAlters = new LinkedHashSet<Vertex>();
		Set<Edge> ties = ego.getOutEdges();

		for (Edge e : ties) {
			Vertex alter = e.getOpposite(ego);
//			if (e.getUserDatum(SAMPLED_KEY) == null) {
				e.setUserDatum(SAMPLED_KEY, true, UserDataKeys.COPY_ACT);				
				/*
				 * Ties may not be named.
				 */
				rnd.nextDouble();
				if (getProbaTieNamed(ego, alter) >= rnd.nextDouble()) {
					SampledVertex sampledAlter = (SampledVertex) alter.getUserDatum(SAMPLED_ELEMENT_KEY);
					if(sampledAlter != null) {
						sampledAlter.increaseVisited();
					}
					/*
					 * Do not sample alters twice.
					 */
					if (alter.getUserDatum(UserDataKeys.SAMPLED_KEY) == null) {
						detectedAlters.add(alter);

						if(sampledAlter == null) {
							/*
							 * Create a new vertex in the sampled graph.
							 */
							sampledAlter = new SampledVertex(currentWave);
							for(Iterator<String> it = alter.getUserDatumKeyIterator(); it.hasNext();) {
								String key = it.next();
								sampledAlter.addUserDatum(key, alter.getUserDatum(key), UserDataKeys.COPY_ACT);
							}
							
							sampledGraph.addVertex(sampledAlter);
							alter.setUserDatum(SAMPLED_ELEMENT_KEY, sampledAlter,
								UserDataKeys.COPY_ACT);
						} else {
							/*
							 * Get the corresponding vertex in the sampled graph.
							 */
							sampledAlter = (SampledVertex) alter
							.getUserDatum(SAMPLED_ELEMENT_KEY);
						}			
						/*
						 * Add the sampled edge, if it has not been sampled yet, i.e.,
						 * through FOF knowledge.
						 */
						if(e.getUserDatum(SAMPLED_ELEMENT_KEY) == null) {
							SampledEdge sampledEdge = new SampledEdge(sampledEgo,
									sampledAlter, currentWave);
							sampledGraph.addEdge(sampledEdge);
							e.setUserDatum(SAMPLED_ELEMENT_KEY, sampledEdge, UserDataKeys.COPY_ACT);
						}
					}
				}
//			} else {
//				SampledVertex sampledAlter = (SampledVertex) alter.getUserDatum(SAMPLED_ELEMENT_KEY);
//				if(sampledAlter != null)
//					sampledAlter.increaseVisited();
//			}
		}

		List<Vertex> v2set = new LinkedList(detectedAlters);
		for(Vertex v1 : detectedAlters) {
			for(Vertex v2 : v2set) {
				if(v1 != v2) {
					Edge e = v1.findEdge(v2); 
					if(e != null && e.getUserDatum(SAMPLED_ELEMENT_KEY) == null) {
						rnd.nextDouble();
						if(pFOF != 0 && pFOF >= rnd.nextDouble()) {
							SampledVertex ego1 = (SampledVertex)v1.getUserDatum(SAMPLED_ELEMENT_KEY);
							SampledVertex ego2 = (SampledVertex)v2.getUserDatum(SAMPLED_ELEMENT_KEY);
							SampledEdge sampledEdge = new SampledEdge(ego1, ego2, currentWave);
							sampledGraph.addEdge(sampledEdge);
							e.setUserDatum(SAMPLED_ELEMENT_KEY, sampledEdge, UserDataKeys.COPY_ACT);
						}
					}
				}
			}
			v2set.remove(v1);
		}
		
		return detectedAlters;
	}

	private double getProbaTieNamed(Vertex ego, Vertex alter) {
		return pTieNamed;
	}
	
	private double getProbaParticipate(Vertex ego) {
		return pResponse;
	}

	public void calculateSampleProbas(Graph g, int numVertex1, int numVertex2, int numVertexTotal) {
//		double growth = (numVertex1 - numVertex2)/(double)numVertexTotal;
//		Map<Integer, Double> probas = new HashMap<Integer, Double>();
		
		Set<Vertex> vertices = g.getVertices();
//		Map<Integer, Integer> degreeOccurence = new HashMap<Integer, Integer>();
//		for(Vertex v : vertices) {
//			Integer count = degreeOccurence.get(v.degree());
//			int cnt = 0;
//			if(count != null)
//				cnt = count;
//			cnt++;
//			degreeOccurence.put(v.degree(), cnt);
//		}
//		verticesPerDegree.add(degreeOccurence);
		
//		for(SampledVertex v : g.getVertices()) {
//			if(v.getWaveSampled() == 0) {
//				v.setSampleProbability(1);
//			} else if(v.getWaveSampled() == getCurrentWave()) {
//				v.setSampleProbability(1 - Math.pow((1 - growth), v.degree()));
//				probas.put(v.degree(), v.getSampleProbability());
//			}
//		}
		
		
		for(Vertex v : vertices) {
			double p;
			if(currentWave == 0) {
				p = numSeeds/(double)numVertexTotal;
				v.setUserDatum(SAMPLE_PROBA, p, UserDataKeys.COPY_ACT);
				
			} else {
//				double p_minus1 = (Double)v.getUserDatum(SAMPLE_PROBA);
				
				double p_w = 1 - Math.pow((1 - (numVertex1/(double)numVertexTotal)), v.degree());
				
				
//				p = p_minus1 + p_w - (p_minus1 * p_w);
				p =p_w;
				v.setUserDatum(SAMPLE_PROBA, p_w, UserDataKeys.COPY_ACT);
			}
			SampledVertex sample = (SampledVertex) v.getUserDatum(SAMPLED_ELEMENT_KEY);
			if(sample != null) {
				if(sample.getWaveSampled() == currentWave || sample.getWaveDetected() == currentWave)
					sample.setSampleProbability(p);
			}
//			probas.put(v.degree(), p);
		}
//		System.out.println(probas.toString());
	}
	
	public int getCurrentWave() {
		return currentWave;
	}
}
