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
package playground.johannes.socialnetworks.snowball;

import gnu.trove.TIntArrayList;
import gnu.trove.TObjectDoubleHashMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import playground.johannes.socialnetworks.graph.GraphProjection;
import playground.johannes.socialnetworks.graph.SparseGraphProjectionBuilder;
import playground.johannes.socialnetworks.graph.VertexDecorator;

/**
 * @author illenberger
 *
 */
public class Sampler {

	private int iteration;
	
	private Set<SampledVertex> egos;
	
	private final SparseGraphProjectionBuilder<SampledGraph, SampledVertex, SampledEdge> builder;
	
	private final GraphProjection<SampledGraph, SampledVertex, SampledEdge> projection;
	
	private final Random random;
	
	private final SampledGraph graph;
	
	private double responseRate;
	
	private final TIntArrayList numSampledVertices;
	
	private int maxNumSamples;
	
	private boolean useEstimator2;
	
	public Sampler(final SampledGraph g, final long randomSeed) {
		this.graph = g;
		this.random = new Random(randomSeed);
		this.graph.reset();
		this.iteration = -1;
		this.egos = new HashSet<SampledVertex>();
		builder = new SparseGraphProjectionBuilder<SampledGraph, SampledVertex, SampledEdge>();
		this.projection = builder.createGraph(g);
		this.numSampledVertices = new TIntArrayList();
		this.responseRate = 1;
		this.maxNumSamples = Integer.MAX_VALUE;
	}
	
	public void setResponseRate(final double p) {
		this.responseRate = p;
	}
	
	public void setNumMaxSamples(final int maxNumSamples) {
		this.maxNumSamples = maxNumSamples;
	}
	
	public void setUseEstimator2(boolean flag) {
		useEstimator2 = flag;
	}
	
	public double getResponseRate() {
		return this.responseRate;
	}
	
	public int getIteration() {
		return this.iteration;
	}
	
	public GraphProjection<SampledGraph, SampledVertex, SampledEdge> getProjection() {
		return this.projection;
	}
	
	public Set<SampledVertex> getEgos() {
		return this.egos;
	}
	
	public int getNumSampledVertices(final int it) {
		if (this.numSampledVertices.size() <= it || it < 0) {
			return 0;
		}
		return this.numSampledVertices.get(it);
	}
	
	public Set<SampledVertex> drawRandomSeedVertices(final int numSeeds) {
		for(SampledVertex v : this.graph.getVertices()) {
			this.random.nextDouble();
			if(this.random.nextDouble() > this.responseRate) {
				v.setIsNonResponding(true);
			}
		}
		
		List<SampledVertex> vertices = new LinkedList<SampledVertex>(this.graph.getVertices());
		Collections.shuffle(vertices, this.random);
		this.egos = new HashSet<SampledVertex>();
		for(SampledVertex v : vertices) {
			if(!v.isNonResponding()) {
				this.egos.add(v);
				if(this.egos.size() == numSeeds)
					break;
			}
		}
		for(SampledVertex v : this.egos) {
			v.detect(0);
//			v.setProjection(this.projection.addVertex(v));
			v.setProjection(builder.addVertex(projection, v));
		}
		return this.egos;
	}
	
	public void runIteration() {
		int n = getNumSampledVertices(this.iteration);
		this.iteration++;
		Set<SampledVertex> alters = new HashSet<SampledVertex>();
		
		
		for(SampledVertex v : this.egos) {
			if(this.iteration == 0) {
				alters.addAll(expand(v));
				n++;
			} else {
				v.setIsRequested(true);
				if(!v.isSampled() && !v.isNonResponding()) {
					alters.addAll(expand(v));
					n++;
				}
			}
			if(n >= this.maxNumSamples)
				break;
		}
		
		this.egos = alters;
		
		this.numSampledVertices.add(n);
		
		if(useEstimator2)
			calcNormalizedVertexWeights2();
		else
			calcNormalizedVertexWeights();
	}
	
	private Set<SampledVertex> expand(final SampledVertex ego) {
		ego.sample(this.iteration);
		
		HashSet<SampledVertex> alters = new HashSet<SampledVertex>();
		for(SampledEdge e : ego.getEdges()) {
			SampledVertex alter = e.getOpposite(ego);
			if(alter.isSampled() || alter.isDetected()) {
				if(e.getProjection() == null) {
//					e.setProjection(this.projection.addEdge(ego.getProjection(), alter.getProjection(), e));
					e.setProjection(builder.addEdge(projection, ego.getProjection(), alter.getProjection(), e));
				}	
			} else {
				alter.detect(this.iteration);
//				alter.setProjection(this.projection.addVertex(alter));
				alter.setProjection(builder.addVertex(projection, alter));
//				e.setProjection(this.projection.addEdge(ego.getProjection(), alter.getProjection(), e));
				e.setProjection(builder.addEdge(projection, ego.getProjection(), alter.getProjection(), e));
				alters.add(alter);
			}
		}
		
		return alters;
	}
	
	private void calcNormalizedVertexWeights() {
		List<SampledVertex> vList = new LinkedList<SampledVertex>();
		
		double wsum = 0;
		double frac = 0;
		if(this.iteration == 0 )
			frac = this.egos.size() / (double)this.graph.getVertices().size();
		else
			frac = getNumSampledVertices(this.iteration - 1)/(double)this.graph.getVertices().size();
		
		for(VertexDecorator<SampledVertex> v : this.projection.getVertices()) {
			if(v.getDelegate().isSampled()) {
				vList.add(v.getDelegate());
				double p = 1;
				if(this.iteration == 0)
					p = frac; 
				else {
					p = 1 - Math.pow(1 - frac, v.getEdges().size());
				}
				wsum += 1 / p;
			}
		}
		
		double norm = vList.size() / wsum;
		for(SampledVertex v : vList) {
			double p = 1;
			if(this.iteration == 0)
				p = frac;
			else
				p = 1 - Math.pow(1 - frac, v.getEdges().size());
			
			v.setSampleProbability(p);
			v.setNormalizedWeight(1 / p * norm);
		}
	}
	
	private void calcNormalizedVertexWeights2() {
		List<SampledVertex> vList = new LinkedList<SampledVertex>();
		
		double wsum = 0;
		double frac1 = 0;
		double frac2 = 0;
		
		if(this.iteration == 0 )
			frac1 = this.egos.size() / (double)this.graph.getVertices().size();
		else if(iteration > 1) {
			frac1 = getNumSampledVertices(this.iteration - 1)/(double)this.graph.getVertices().size();
			frac2 = getNumSampledVertices(this.iteration - 2)/(double)this.graph.getVertices().size();
		} else {
			frac1 = getNumSampledVertices(this.iteration - 1)/(double)this.graph.getVertices().size();
		}
		
		TObjectDoubleHashMap<SampledVertex> proba = new TObjectDoubleHashMap<SampledVertex>();
		for(VertexDecorator<SampledVertex> v : this.projection.getVertices()) {
			if(v.getDelegate().isSampled()) {
				vList.add(v.getDelegate());
//				double p = 1;
//				if(this.iteration == 0)
//					p = frac1;
//				else if(iteration == 1) {
//					p = 1 - Math.pow(1 - frac1, v.getEdges().size());
//				} else {
//					for(SparseVertex n : v.getNeighbours()) {
//						if(((VertexDecorator<SampledVertex>)n).getDelegate().isSampled()) {
//							p *= 1 - (1 - Math.pow(1 - frac2, n.getEdges().size()));
//						} else {
//							p *= 1 - frac1;
//						}
//					}
//					p = 1 - p;
//				}
				double p = getSamplingProba(v.getDelegate(), iteration, proba);
				wsum += 1 / p;
				v.getDelegate().setSampleProbability(p);
			}
		}
		
		double norm = vList.size() / wsum;
		for(SampledVertex v : vList) {
			v.setNormalizedWeight(1 / v.getSampleProbability() * norm);
		}
	}
	
	private double getSamplingProba(SampledVertex v, int i,
			TObjectDoubleHashMap<SampledVertex> proba) {
		double p = proba.get(v);
		if (p == 0) {
			if (v.isSampled()) {
				if (v.getIterationSampled() > i) {
					p = getNumSampledVertices(i)
							/ (double) graph.getVertices().size();
				} else if (i == 0) {
					p = egos.size() / (double) graph.getVertices().size();
				} else {
					p = 1;
					for (SampledVertex n : v.getNeighbours())
						p *= 1 - getSamplingProba(n, i - 1, proba);
					p = 1 - p;
				}
			} else {
				p = getNumSampledVertices(i)
						/ (double) graph.getVertices().size();
			}
			proba.put(v, p);
		}
		return p;
	}
	
//	private TreeMap<Integer, Double> p_k;
//	
//	private void calcNormalizedVertexWeights2() {
//		double p = 1;
//		double wsum = 0;
//		int n_sampled = 0;
//		TreeMap<Integer, Integer> n_neighbour_k = getNumNeighbourK();
//		TreeMap<Tuple<Integer, Integer>, Integer> neighbourMatrix = getNeighbourMatrix();
//		double N = this.graph.getVertices().size();
//		int n = getNumSampledVertices(this.iteration - 1);
//		
//		for (VertexDecorator<SampledVertex> v : this.projection.getVertices()) {
//			if (v.getDelegate().isSampled()) {
//				int k1 = v.getEdges().size();
//				if (this.iteration == 0) {
//					p = this.egos.size() / (double) this.graph.getVertices().size();
//				} else {
//					double sum = 0;
//					for(Integer k2 : this.p_k.keySet()) {
//						double p_k2 = getVal(k2, this.p_k);
////						double p_k1 = getVal(k1, p_k);
////						sum += p_k2 * (1 - Math.pow(1 - p_k1, k2));
//						int n_k2_k1 = getVal(new Tuple<Integer, Integer>(k2, k1), neighbourMatrix);						
//						sum += p_k2 * n_k2_k1 / n; 
//					}
//					double denominator = N * sum;
//					int nominator = getVal(k1, n_neighbour_k);
//					double f;
//					if(nominator > 0 && denominator > 0 && denominator >= nominator)
//						f = nominator/denominator;
//					else
//						f = n/N;
//					
//					p = 1 - Math.pow((1 - f), k1);
//					
//					if(Double.isNaN(p))
//						System.err.println("NaN");
//					if(p == 0)
//						System.err.println("0");
//					if(p > 1 || p < 0) {
//						System.err.println(">1");
//						p = 1;
//					}
//				}
//				v.getDelegate().setSampleProbability(p);
//				n_sampled++;
//				
//				wsum += 1/p;
//			}
//		}
//		
//		
//		
//		double norm = n_sampled / wsum;
//		this.p_k = new TreeMap<Integer, Double>();
//		for (VertexDecorator<SampledVertex> v : this.projection.getVertices()) {
//			if (v.getDelegate().isSampled()) {
//				int k = v.getEdges().size();
//				Double p_k_i = this.p_k.get(k);
//				if(p_k_i == null)
//					p_k_i = 0.0;
//				p = v.getDelegate().getSampleProbability();
//				p_k_i += 1 / p * norm;
//				this.p_k.put(k, p_k_i);
//				
//				v.getDelegate().setNormalizedWeight(1 / p * norm);
//			}
//		}
//		
//		double sum = 0;
//		for(Double d : this.p_k.values()) {
//			sum += d;
//		}
//		for(Integer k : this.p_k.keySet()) {
//			Double d = this.p_k.get(k);
//			d = d/sum;
//			this.p_k.put(k, d);
//		}
//		
//	}
//	private double getVal(final int k, final TreeMap<Integer, Double> map) {
//		Double d = map.get(k);
//		if(d == null)
//			return 0;
//		else
//			return d;
//		Entry<Integer, Double> floor = map.floorEntry(k);
//		Entry<Integer, Double> ceiling = map.ceilingEntry(k);
//		if(floor == null && ceiling == null)
//			return 0;
//		if(floor == null)
//			return ceiling.getValue();
//		else if (ceiling == null)
//			return floor.getValue();
//		else {
//			int diffFloor = Math.abs(k - floor.getKey());
//			int diffCeil = Math.abs(k - ceiling.getKey());
//			if(diffFloor < diffCeil)
//				return floor.getValue();
//			else
//				return ceiling.getValue();
//		}
//	}
	
//	private int getVal(final int k, final TreeMap<Integer, Integer> map) {
//		Entry<Integer, Integer> floor = map.floorEntry(k);
//		Entry<Integer, Integer> ceiling = map.ceilingEntry(k);
//		if(floor == null && ceiling == null)
//			return 0;
//		if(floor == null)
//			return ceiling.getValue();
//		else if (ceiling == null)
//			return floor.getValue();
//		else {
//			int diffFloor = Math.abs(k - floor.getKey());
//			int diffCeil = Math.abs(k - ceiling.getKey());
//			if(diffFloor < diffCeil)
//				return floor.getValue();
//			else
//				return ceiling.getValue();
//		}
//	}
	
//	private int getVal(final Tuple<Integer, Integer> key, final TreeMap<Tuple<Integer, Integer>, Integer> map) {
//		Integer e = map.get(key);
//		if(e == null)
//			return 0;
//		else
//			return e;
//		Entry<Tuple<Integer, Integer>, Integer> floor = map.floorEntry(key);
//		Entry<Tuple<Integer, Integer>, Integer> ceiling = map.ceilingEntry(key);
//		if(floor == null && ceiling == null)
//			return 0;
//		if(floor == null)
//			return ceiling.getValue();
//		else if (ceiling == null)
//			return floor.getValue();
//		else {
//			int p = key.getFirst() * key.getSecond();
//			int p1 = floor.getKey().getFirst() * floor.getKey().getSecond();
//			int p2 = ceiling.getKey().getFirst() * ceiling.getKey().getSecond();
//			int diffFloor = Math.abs(p - p1);
//			int diffCeil = Math.abs(p - p2);
//			if(diffFloor < diffCeil)
//				return floor.getValue();
//			else
//				return ceiling.getValue();
//		}
//	}
	
//	private TIntHashSet getDegrees() {
//		TIntHashSet k = new TIntHashSet();
//		for (VertexDecorator<SampledVertex> v : this.projection.getVertices()) {
//			if(v.getDelegate().isSampled()) {
//				k.add(v.getEdges().size());
//			}
//		}
//		return k;
//	}
	
//	private TreeMap<Integer, Integer> getNumNeighbourK() {
//		TreeMap<Integer, Integer> n_k = new TreeMap<Integer, Integer>();
//		for (VertexDecorator<SampledVertex> v : this.projection.getVertices()) {
//			if(v.getDelegate().isSampled() && v.getDelegate().getIterationSampled() <= (this.iteration - 1)) {
//				int k = v.getEdges().size();
//				Integer n = n_k.get(k);
//				if(n == null)
//					n = 0;
//				
//				for(Vertex v2 : v.getNeighbours()) {
//					if(((VertexDecorator<SampledVertex>)v2).getDelegate().isSampled() && ((VertexDecorator<SampledVertex>)v2).getDelegate().getIterationSampled() <= (this.iteration - 1))
//						n++;
//				}
//				if(n > 0)
//					n_k.put(k, n);
//			}
//		}
//		return n_k;
//	}
	
//	private TreeMap<Tuple<Integer, Integer>, Integer> getNeighbourMatrix() {
//		TreeMap<Tuple<Integer, Integer>, Integer> matrix = new TreeMap<Tuple<Integer, Integer>, Integer>(new TupleComparator());
//		for (VertexDecorator<SampledVertex> v : this.projection.getVertices()) {
//			if(v.getDelegate().isSampled() && v.getDelegate().getIterationSampled() <= (this.iteration - 1)) {
//				int k = v.getEdges().size();
//				Set<Tuple<Integer, Integer>> keys = new HashSet<Tuple<Integer,Integer>>();
//				for(Vertex v2 : v.getNeighbours()) {
//					if (((VertexDecorator<SampledVertex>) v2).getDelegate().isSampled()
//							&& ((VertexDecorator<SampledVertex>) v2).getDelegate().getIterationSampled() <= (this.iteration - 1)) {
//						int k2 = v2.getEdges().size();
//						Tuple<Integer, Integer> key = new Tuple<Integer, Integer>(k, k2);
//						keys.add(key);
//					}
//				}
//				for(Tuple<Integer, Integer> key : keys) {
//					Integer n = matrix.get(key);
//					if(n == null)
//						n = 0;
//					n++;
//					matrix.put(key, n);
//				}
//			}
//		}
//		
//		return matrix;
//	}
	
//	private static class TupleComparator implements Comparator<Tuple<Integer, Integer>> {
//
//		public int compare(final Tuple<Integer, Integer> o1,
//				final Tuple<Integer, Integer> o2) {
////			int result = o1.getFirst() - o2.getFirst();
////			if(result == 0)
////				result = o1.getSecond() - o2.getSecond();
////			return result;
//			int p1 = o1.getFirst() * o1.getSecond();
//			int p2 = o2.getFirst() * o2.getSecond();
//			int result = p1 - p2;
//			if(result == 0)
//				result = o1.getFirst() - o2.getFirst();
//			return result;
//		}
//		
//	}
}
