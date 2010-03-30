/* *********************************************************************** *
 * project: org.matsim.*
 * VertexSamplingCounter.java
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
package playground.johannes.socialnetworks.snowball2.sim;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;

/**
 * @author illenberger
 *
 */
public class VertexSamplingCounter implements SamplerListener {

	private int lastIteration;
	
	private Map<Vertex, int[]> countTable;
	
	private Map<Vertex, double[]> probaTable;
	
	private int[] nsim = new int[15];
	
	private BiasedDistribution estimator;
	
	public VertexSamplingCounter(Graph graph) {
		countTable = new HashMap<Vertex, int[]>();
		probaTable = new HashMap<Vertex, double[]>();
		for(Vertex v : graph.getVertices()) {
			countTable.put(v, new int[15]);
			probaTable.put(v, new double[15]);
		}
		
		estimator = new Estimator7(graph.getVertices().size());
	}
	
	public void reset() {
		lastIteration = 0;
	}
	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		if(sampler.getIteration() > lastIteration) {
			int it = sampler.getIteration();;
			lastIteration = it;
			nsim[it-1]++;
			estimator.update(sampler.getSampledGraph());
			
			for(VertexDecorator<?> v : sampler.getSampledGraph().getVertices()) {
				int it_v = ((SampledVertex)v).getIterationSampled();
				if(it_v != -1 && it_v <= it) {
					Vertex delegate = v.getDelegate();
					int[] counts = countTable.get(delegate);
					counts[it - 1]++;
					
					double[] probas = probaTable.get(delegate);
					probas[it - 1] += estimator.getProbability((SampledVertex) v);
				}
			}
		}
		return true;
	}

	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
	}

	public static void main(String args[]) throws IOException {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		SparseGraph graph = reader.readGraph(args[0]);
		
		int nSims = Integer.parseInt(args[1]);
		
		VertexSamplingCounter counter = new VertexSamplingCounter(graph);
		for(int i = 0; i < nSims; i++) {
			Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>();
			sampler.setSeedGenerator(new RandomSeedGenerator(10, (long) (Math.random() * nSims)));
			counter.reset();
			sampler.setListener(counter);
			sampler.run(graph);
		}
		
		TIntIntHashMap[] count_k = new TIntIntHashMap[15];
		TDoubleDoubleHashMap[] p_obs_k = new TDoubleDoubleHashMap[15];
		TDoubleDoubleHashMap[] p_estim_k = new TDoubleDoubleHashMap[15];
		TIntIntHashMap[] count_estim_k = new TIntIntHashMap[15];
		
		List<TIntObjectHashMap<TDoubleArrayList>> k_samples = new ArrayList<TIntObjectHashMap<TDoubleArrayList>>(15);
		for(int i = 0; i < 15; i++) {
			k_samples.add(new TIntObjectHashMap<TDoubleArrayList>());
		}
		int maxSize = 0;
		
		double[] diffs = new double[15];
		int[] samples = new int[15];
		for(Vertex v : graph.getVertices()) {
			int k = v.getNeighbours().size();
			int[] counts = counter.countTable.get(v);
			double[] probas = counter.probaTable.get(v);
			for(int i = 0; i < counts.length; i++) {
				int n = counts[i];
				double p_obs = n / (double) counter.nsim[i];
				
				if(p_obs_k[i] == null) {
					count_k[i] = new TIntIntHashMap();
					count_estim_k[i] = new TIntIntHashMap();
					p_obs_k[i] = new TDoubleDoubleHashMap();
					p_estim_k[i] = new TDoubleDoubleHashMap();
				}
				
				if (n > 0) {
					
					double p_estim = probas[i] / (double) n;
					double diff = p_obs - p_estim;
					diffs[i] += diff;
					samples[i]++;
					
					p_estim_k[i].adjustOrPutValue(k, p_estim, p_estim);
					count_estim_k[i].adjustOrPutValue(k, 1, 1);
				}
				
				
					
				p_obs_k[i].adjustOrPutValue(k, p_obs, p_obs);
				
				count_k[i].adjustOrPutValue(k, 1, 1);
				
				TIntObjectHashMap<TDoubleArrayList> k_table = k_samples.get(i);
				TDoubleArrayList list = k_table.get(k);
				if(list == null) {
					list = new TDoubleArrayList(nSims);
					k_table.put(k, list);
				}
				
				list.add(p_obs);
				maxSize = Math.max(maxSize, list.size());
			}
		}
		
		for(int i = 0; i < diffs.length; i++) {
			diffs[i] = diffs[i] / (double)samples[i];
		}
		
		for(int i = 0; i < p_obs_k.length; i++) {
			if(p_obs_k[i] != null) {
				TDoubleDoubleIterator it = p_obs_k[i].iterator();
				for(int k = 0; k < p_obs_k[i].size(); k++) {
					it.advance();
					it.setValue(it.value() / (double)count_k[i].get((int)it.key()));
				}
			}
		}
		
		for(int i = 0; i < p_estim_k.length; i++) {
			if(p_estim_k[i] != null) {
				TDoubleDoubleIterator it = p_estim_k[i].iterator();
				for(int k = 0; k < p_estim_k[i].size(); k++) {
					it.advance();
					it.setValue(it.value() / (double)count_estim_k[i].get((int)it.key()));
				}
			}
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/diffs.txt", args[2])));
		writer.write("it\tp");
		writer.newLine();
		for(int i = 0; i < diffs.length; i++) {
			writer.write(String.valueOf(i));
			writer.write("\t");
			writer.write(String.valueOf(diffs[i]));
			writer.newLine();
		}
		writer.close();
		
		for(int i = 0; i < p_obs_k.length; i++) {
			if(p_obs_k[i] != null) {
				Distribution.writeHistogram(p_obs_k[i], String.format("%1$s/%2$s.pobs.txt", args[2], i));
			}
		}
		
		for(int i = 0; i < p_estim_k.length; i++) {
			if(p_estim_k[i] != null) {
				Distribution.writeHistogram(p_estim_k[i], String.format("%1$s/%2$s.pestim.txt", args[2], i));
			}
		}
		
		
		for(int i = 0; i < k_samples.size(); i++) {
			writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.k_samples.txt", args[2], i)));
			TIntObjectHashMap<TDoubleArrayList> k_table = k_samples.get(i);
			int[] keys = k_table.keys();
			Arrays.sort(keys);
			
			for(int k : keys) {
				writer.write(String.valueOf(k));
				writer.write("\t");
			}
			writer.newLine();
			
			for(int j = 0; j < maxSize; j++) {
				for(int k : keys) {
					TDoubleArrayList list = k_table.get(k);
					if(list != null) {
						if(j < list.size()) {
							writer.write(String.valueOf(list.get(j)));
						}
						writer.write("\t");
					}
				}
				writer.newLine();
			}
			writer.close();
		}
	}
	
}
