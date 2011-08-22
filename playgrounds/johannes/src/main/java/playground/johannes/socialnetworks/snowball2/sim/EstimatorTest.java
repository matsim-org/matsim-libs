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
import gnu.trove.TIntObjectIterator;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.FixedSizeRandomPartition;
import org.matsim.contrib.sna.graph.analysis.RandomPartition;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledEdgeDecorator;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;
import org.matsim.contrib.sna.snowball.analysis.SimplePiEstimator;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListener;
import org.matsim.core.config.Config;

import playground.johannes.socialnetworks.snowball2.sim.deprecated.NormalizedEstimator;


/**
 * @author illenberger
 *
 */
public class EstimatorTest implements SamplerListener {
	
	private static final Logger logger = Logger.getLogger(EstimatorTest.class);

	private int lastIteration;
	
	private Map<Vertex, int[]> vertexCounts;
	
	private Map<Edge, int[]> edgeCounts;
	
	private Map<Vertex, double[]> vertexProbas;
	
	private Map<Edge, double[]> edgeProbas;
	
	private Map<Vertex, double[]> vertexWeights;
	
	private int[] nSimulations = new int[INIT_CAPACITY];
	
	private int[] nSampledVertices = new int[INIT_CAPACITY];
	
	private TDoubleArrayList[] N_estim = new TDoubleArrayList[INIT_CAPACITY];
	
	private TDoubleArrayList[] M_estim = new TDoubleArrayList[INIT_CAPACITY];
	
	private PiEstimator estimator;
	
	private static final int INIT_CAPACITY = 100;
	
	private int maxIteration = 0;
	
	public EstimatorTest(Graph graph) {
		vertexCounts = new HashMap<Vertex, int[]>();
		edgeCounts = new HashMap<Edge, int[]>();
		vertexProbas = new HashMap<Vertex, double[]>();
		edgeProbas = new HashMap<Edge, double[]>();
		vertexWeights = new HashMap<Vertex, double[]>();
		
		for(Vertex v : graph.getVertices()) {
			vertexCounts.put(v, new int[INIT_CAPACITY]);
			vertexProbas.put(v, new double[INIT_CAPACITY]);
			vertexWeights.put(v, new double[INIT_CAPACITY]);
		}
		
		for(Edge e : graph.getEdges()) {
			edgeCounts.put(e, new int[INIT_CAPACITY]);
			edgeProbas.put(e, new double[INIT_CAPACITY]);
		}
	}
	
	public void reset(Graph graph, String estimtype) {
		lastIteration = 0;
		int N = graph.getVertices().size();
		if("estim1b".equalsIgnoreCase(estimtype))
			estimator = new SimplePiEstimator(N);
		else if("estim1a".equalsIgnoreCase(estimtype))
			estimator = new NormalizedEstimator(new SimplePiEstimator(N), N);
		else {
			logger.warn(String.format("Estimator type %1$s unkown!", estimtype));
			System.exit(-1);
		}
	}
	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		if(sampler.getIteration() > lastIteration) {
			int it = sampler.getIteration();
			lastIteration = it;
			maxIteration = Math.max(maxIteration, it - 1);
			nSimulations[it-1]++;
			nSampledVertices[it-1] += sampler.getNumSampledVertices();
			
			if(N_estim[it-1] == null)
				N_estim[it-1] = new TDoubleArrayList();
			
			estimator.update(sampler.getSampledGraph());
			
			double wsum = 0;
			for(SampledVertexDecorator<?> v : sampler.getSampledGraph().getVertices()) {
				if (v.isSampled() && v.getNeighbours().size() > 0) { // ignore isolated vertices
					Vertex delegate = v.getDelegate();
					vertexCounts.get(delegate)[it - 1]++;

					double[] probas = vertexProbas.get(delegate);
					double[] weights = vertexWeights.get(delegate);
					double p = estimator.probability(v);
					probas[it - 1] += p;
					weights[it - 1] += 1/p;
					
					wsum += 1/p;
				}
			}
			
			N_estim[it-1].add(wsum);
			/*
			 * edges
			 */
			if(M_estim[it - 1] == null)
				M_estim[it - 1] = new TDoubleArrayList();
			
			wsum = 0;
			for(SampledEdgeDecorator<?> edge : sampler.getSampledGraph().getEdges()) {
				SampledVertexDecorator<?> v_i = edge.getVertices().getFirst();
				SampledVertexDecorator<?> v_j = edge.getVertices().getSecond();
				if(v_i.isSampled() && v_j.isSampled()) {
					Edge delegate = edge.getDelegate();
					
					edgeCounts.get(delegate)[it - 1]++;
					
					double p_i = estimator.probability(v_i, it - 2);
					double p_j = estimator.probability(v_j, it - 2);

//					double p_e = p_i * p_j;
					double p_e = (p_i + p_j) - (p_i * p_j);

					double[] probas = edgeProbas.get(delegate);
					probas[it - 1] += p_e;

					wsum += 1 / p_e;

				}
			}
			
			M_estim[it - 1].add(wsum);
		}
		
		return true;
	}

	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
		beforeSampling(sampler, null);
	}

	private void analyze(Graph graph, String output) throws IOException {
		final int N = graph.getVertices().size();
		DescriptiveStatistics k_distr = Degree.getInstance().statistics(graph.getVertices());
		final int k_max = (int) k_distr.getMax(); 
		/*
		 * initialize arrays
		 */
		TDoubleDoubleHashMap kHist = Histogram.createHistogram(k_distr, new LinearDiscretizer(1.0), false);
		TDoubleDoubleHashMap[] pObs_k = new TDoubleDoubleHashMap[maxIteration + 1];
		TDoubleDoubleHashMap[] wObs_k = new TDoubleDoubleHashMap[maxIteration + 1];
		TDoubleDoubleHashMap[] pEstim_k = new TDoubleDoubleHashMap[maxIteration + 1];
		TDoubleDoubleHashMap[] wEstim_k = new TDoubleDoubleHashMap[maxIteration + 1];
		TDoubleDoubleHashMap[] N_estim_hist = new TDoubleDoubleHashMap[maxIteration + 1];
		TDoubleDoubleHashMap[] M_estim_hist = new TDoubleDoubleHashMap[maxIteration + 1];
		double[] mse_p = new double[maxIteration + 1];
		double[] mse_w = new double[maxIteration + 1];
		double[] bias_p = new double[maxIteration + 1];
		double[] bias_w = new double[maxIteration + 1];
		Distribution[] p_ratio = new Distribution[maxIteration + 1];
		Distribution[] w_ratio = new Distribution[maxIteration + 1];
		double[] N_estim_mean = new double[maxIteration + 1];
		double[] M_estim_mean = new double[maxIteration + 1];

		int[] samples = new int[maxIteration + 1];
		TIntIntHashMap[] samples_k = new TIntIntHashMap[maxIteration + 1];
		/*
		 * boxplot for probability to sample k
		 */
		List<TIntObjectHashMap<TDoubleArrayList>> pObsList_k = new ArrayList<TIntObjectHashMap<TDoubleArrayList>>(maxIteration + 1);
		for (int i = 0; i < maxIteration + 1; i++) {
			pObsList_k.add(new TIntObjectHashMap<TDoubleArrayList>());
		}
		int maxListSize = 0;
		/*
		 * iterate over all vertices
		 */
		for(Vertex v : graph.getVertices()) {
			int k = v.getNeighbours().size();
			int[] vertexCount = vertexCounts.get(v);
			double[] estimProba = vertexProbas.get(v);
			double[] estimWeigth = vertexWeights.get(v);
			/*
			 * iterate over all snowball iterations
			 */
			for(int i = 0; i <= maxIteration; i++) {
				int n_i = vertexCount[i];
				double pObs_i = n_i / (double) nSimulations[i];
				
				double pRnd_i = nSampledVertices[i] / ((double)nSimulations[i] * N);
				/*
				 * initialize arrays
				 */
				if(pObs_k[i] == null) {
					samples_k[i] = new TIntIntHashMap();
					pObs_k[i] = new TDoubleDoubleHashMap();
					wObs_k[i] = new TDoubleDoubleHashMap();
					pEstim_k[i] = new TDoubleDoubleHashMap();
					wEstim_k[i] = new TDoubleDoubleHashMap();
					p_ratio[i] = new Distribution();
					w_ratio[i] = new Distribution();
				}
				/*
				 * accumulate samples
				 */
				pObs_k[i].adjustOrPutValue(k, pObs_i, pObs_i);
				
				if (n_i > 0) {
					double wObs_i = 1/pObs_i;
					wObs_k[i].adjustOrPutValue(k, wObs_i, wObs_i);
					
					samples[i]++;
					
					double pEstimMean = estimProba[i] / (double) n_i;
					double wEstimMean = estimWeigth[i] / (double) n_i;
//					double wEstimMean = 1/pEstimMean;
					double diff_p = pObs_i - pEstimMean;
					double diff_w = wObs_i - wEstimMean;
					
					mse_p[i] += diff_p * diff_p;
					mse_w[i] += diff_w * diff_w;
					
					bias_p[i] += Math.abs(pObs_i - pRnd_i);
					bias_w[i] += Math.abs(wObs_i - 1/pRnd_i);
					
					pEstim_k[i].adjustOrPutValue(k, pEstimMean, pEstimMean);
					wEstim_k[i].adjustOrPutValue(k, wEstimMean, wEstimMean);
					samples_k[i].adjustOrPutValue(k, 1, 1);
					
					p_ratio[i].add(pEstimMean/pObs_i);
					w_ratio[i].add(wEstimMean/wObs_i);
				}
				/*
				 * boxplot for probability to sample k
				 */
				TIntObjectHashMap<TDoubleArrayList> k_table = pObsList_k.get(i);
				TDoubleArrayList list = k_table.get(k);
				
				if(list == null) {
					list = new TDoubleArrayList(nSimulations[i]);
					k_table.put(k, list);
				}
				
				list.add(pObs_i);
				maxListSize = Math.max(maxListSize, list.size());
			}
		}
		/*
		 * iterate over all edges
		 */
		TIntObjectHashMap<double[][]> p_edge_estim_it = new TIntObjectHashMap<double[][]>();
		TIntObjectHashMap<double[][]> p_edge_obs_it = new TIntObjectHashMap<double[][]>();
		TIntObjectHashMap<double[][]> p_edge_it = new TIntObjectHashMap<double[][]>();
		for(int it = 0; it <= maxIteration; it++) {
			double[][] p_edge_estim_k = new double[k_max+1][k_max+1];
			p_edge_estim_it.put(it, p_edge_estim_k);
			
			double[][] p_edge_obs_k = new double[k_max+1][k_max+1];
			p_edge_obs_it.put(it, p_edge_obs_k);
			
			double[][] p_edge_k = new double[k_max+1][k_max+1];
			p_edge_it.put(it, p_edge_k);
			
			int[][] n_edge_k = new int[k_max+1][k_max+1];
			
			for(Edge e : graph.getEdges()) {
				int cnt = edgeCounts.get(e)[it];
				if(cnt > 0) {
				double p_estim = edgeProbas.get(e)[it]/(double)cnt;
				double p_obs = cnt/(double)nSimulations[it];
			
				int k_i = e.getVertices().getFirst().getNeighbours().size();
				int k_j = e.getVertices().getSecond().getNeighbours().size();
				
				p_edge_estim_k[k_i][k_j] += p_estim;
				p_edge_estim_k[k_j][k_i] += p_estim;
				
				p_edge_obs_k[k_i][k_j] += p_obs;
				p_edge_obs_k[k_j][k_i] += p_obs;
				
				
				n_edge_k[k_i][k_j]++;
				n_edge_k[k_j][k_i]++;
				}
			}
			
			for (int i = 0; i < p_edge_estim_k.length; i++) {
				for (int j = i; j < p_edge_estim_k.length; j++) {
					if (i == j) {
						p_edge_estim_k[i][j] = p_edge_estim_k[i][j] / (double) n_edge_k[i][j];
						p_edge_obs_k[j][i] = p_edge_obs_k[j][i] / (double) n_edge_k[j][i];
					} else {

						p_edge_estim_k[i][j] = p_edge_estim_k[i][j] / (double) n_edge_k[i][j];
						p_edge_estim_k[j][i] = p_edge_estim_k[j][i] / (double) n_edge_k[j][i];

						p_edge_obs_k[i][j] = p_edge_obs_k[i][j] / (double) n_edge_k[i][j];
						p_edge_obs_k[j][i] = p_edge_obs_k[j][i] / (double) n_edge_k[j][i];
					}
				}
			}
			/*
			 * shared plot
			 */
			for (int i = 0; i < p_edge_estim_k.length; i++) {
				for (int j = 0; j < p_edge_estim_k.length; j++) {
					if(i < j) {
						p_edge_k[i][j] = p_edge_estim_k[i][j];
					} else {
						p_edge_k[i][j] = p_edge_obs_k[i][j];
					}
				}
			}
		}
		
		/*
		 * calculate averages
		 */
		for(int i = 0; i <= maxIteration; i++) {
			nSampledVertices[i] = (int) (nSampledVertices[i] / (double)nSimulations[i]);
			mse_p[i] = mse_p[i] / (double)samples[i];
			mse_w[i] = mse_w[i] / (double)samples[i];
			bias_p[i] = bias_p[i] / (double)samples[i];
			bias_w[i] = bias_w[i] / (double)samples[i];
			
			double[] values = N_estim[i].toNativeArray();
			N_estim_mean[i] = StatUtils.mean(values);
			N_estim_hist[i] = new Distribution(values).absoluteDistribution((StatUtils.max(values) - StatUtils.min(values))/100.0);
			
			values = M_estim[i].toNativeArray();
			M_estim_mean[i] = StatUtils.mean(values);
			M_estim_hist[i] = new Distribution(values).absoluteDistribution((StatUtils.max(values) - StatUtils.min(values))/100.0);
			
			if(pObs_k[i] != null) {
				TDoubleDoubleIterator it = pObs_k[i].iterator();
				for(int k = 0; k < pObs_k[i].size(); k++) {
					it.advance();
					it.setValue(it.value() / (double)kHist.get((int)it.key()));
				}
			}
			
			if(wObs_k[i] != null) {
				TDoubleDoubleIterator it = wObs_k[i].iterator();
				for(int k = 0; k < wObs_k[i].size(); k++) {
					it.advance();
					it.setValue(it.value() / (double)samples_k[i].get((int)it.key()));
				}
			}
			
			if(pEstim_k[i] != null) {
				TDoubleDoubleIterator it = pEstim_k[i].iterator();
				for(int k = 0; k < pEstim_k[i].size(); k++) {
					it.advance();
					it.setValue(it.value() / (double)samples_k[i].get((int)it.key()));
				}
			}
			
			if(wEstim_k[i] != null) {
				TDoubleDoubleIterator it = wEstim_k[i].iterator();
				for(int k = 0; k < wEstim_k[i].size(); k++) {
					it.advance();
					it.setValue(it.value() / (double)samples_k[i].get((int)it.key()));
				}
			}
		}
		/*
		 * write data
		 */
		writeIntArray(nSampledVertices, String.format("%1$s/n_sampled.txt", output), "it\tn");
		writeDoubleArray(mse_p, String.format("%1$s/mse_p.txt", output), "it\tmse");
		writeDoubleArray(mse_w, String.format("%1$s/mse_w.txt", output), "it\tmse");
		writeDoubleArray(bias_p, String.format("%1$s/bias_p.txt", output), "it\tbias");
		writeDoubleArray(bias_w, String.format("%1$s/bias_w.txt", output), "it\tbias");
		writeDoubleArray(N_estim_mean, String.format("%1$s/N_estim.txt", output), "it\tN_estim");
		writeDoubleArray(M_estim_mean, String.format("%1$s/M_estim.txt", output), "it\tM_estim");
		writeHistogramArray(pObs_k, output, "pobs");
		writeHistogramArray(wObs_k, output, "wobs");
		writeHistogramArray(pEstim_k, output, "pestim");
		writeHistogramArray(wEstim_k, output, "westim");
		writeHistogramArray(N_estim_hist, output, "N_estim");
		writeHistogramArray(M_estim_hist, output, "M_estim");
		
		writeDistributionArray(p_ratio, output, "p_ratio");
		writeDistributionBoxplot(p_ratio, output, "p_ratio_boxplot");
		writeDistributionArray(w_ratio, output, "w_ratio");
		writeDistributionBoxplot(w_ratio, output, "w_ratio_boxplot");
		
		writeMatrix(p_edge_obs_it, output, "pEdgeObs");
		writeMatrix(p_edge_estim_it, output, "pEdgeEstim");
		writeMatrix(p_edge_it, output, "pEdge");
		/*
		 * boxplot
		 */
		for(int i = 0; i < pObsList_k.size(); i++) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.pObsBoxplot.txt", output, i)));
			TIntObjectHashMap<TDoubleArrayList> k_table = pObsList_k.get(i);
			int[] keys = k_table.keys();
			Arrays.sort(keys);
			
			for(int k : keys) {
				writer.write(String.valueOf(k));
				writer.write("\t");
			}
			writer.newLine();
			
			for(int j = 0; j < maxListSize; j++) {
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
		/*
		 * weighted estim diff
		 */
		for (int i = 0; i <= maxIteration; i++) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.wdiff.txt", output, i)));
			writer.write("k\twdiff");
			writer.newLine();

			double ks[] = pObs_k[i].keys();
			Arrays.sort(ks);

			for (int j = 0; j < ks.length; j++) {
				double k = ks[j];
				double diff = (Math.abs(pEstim_k[i].get(k) - pObs_k[i].get(k))) * samples_k[i].get((int) k);
				writer.write(String.valueOf(k));
				writer.write("\t");
				writer.write(String.valueOf(diff));
				writer.newLine();
			}
			writer.close();

		}
	}
	
	private void writeIntArray(int[] array, String filename, String header) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(header);
		writer.newLine();
		for(int i = 0; i < array.length; i++) {
			writer.write(String.valueOf(i));
			writer.write("\t");
			writer.write(String.valueOf(array[i]));
			writer.newLine();
		}
		writer.close();
	}
	
	private void writeDoubleArray(double[] array, String filename, String header) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(header);
		writer.newLine();
		for(int i = 0; i < array.length; i++) {
			writer.write(String.valueOf(i));
			writer.write("\t");
			writer.write(String.valueOf(array[i]));
			writer.newLine();
		}
		writer.close();
	}
	
	private void writeHistogramArray(TDoubleDoubleHashMap[] array, String basedir, String filename) throws FileNotFoundException, IOException {
		for(int i = 0; i < array.length; i++) {
			Distribution.writeHistogram(array[i], String.format("%1$s/%2$s.%3$s.txt", basedir, i, filename));
		}
	}
	
	private void writeDistributionArray(Distribution[] distrs, String basedir, String filename) throws FileNotFoundException, IOException {
		for(int i = 0; i< distrs.length; i++) {
			double binsize = (distrs[i].max() - distrs[i].min())/100.0;
			Distribution.writeHistogram(distrs[i].absoluteDistribution(binsize), String.format("%1$s/%2$s.%3$s.txt", basedir, i, filename));
		}
	}
	
	private void writeDistributionBoxplot(Distribution[] distrs, String basedir, String filename) throws IOException {
		List<double[]> valueList = new ArrayList<double[]>(distrs.length);
		int maxLength = 0;
		for(int i = 0; i < distrs.length; i++) {
			double[] values = distrs[i].getValues();
			valueList.add(values);
			maxLength = Math.max(maxLength, values.length);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.txt", basedir, filename)));
		for(int i = 0; i < distrs.length; i++) {
			writer.write(String.valueOf(i));
			writer.write("\t");
		}
		writer.newLine();
		
		for (int k = 0; k < maxLength; k++) {
			for (int i = 0; i < distrs.length; i++) {
				double[] values = valueList.get(i);
				if(k < values.length) {
					writer.write(String.valueOf(values[k]));
				} else {
					writer.write("\t");
				}
				writer.write("\t");
			}
			writer.newLine();
		}
		
		writer.close();
	}
	
	private void writeMatrix(TIntObjectHashMap<double[][]> matrices, String basedir, String filename) {
		TIntObjectIterator<double[][]> it = matrices.iterator();
		for(int k = 0; k < matrices.size(); k++) {
			it.advance();
			double[][] matrix = it.value();
			SortedSet<Integer> seq = new TreeSet<Integer>();
			for(int i = 0; i < matrix.length; i++) {
				for(int j = 0; j < matrix.length; j++) {
					if(!Double.isNaN(matrix[i][j])) {
						seq.add(i);
						seq.add(j);
					}
				}
			}
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.%3$s.txt", basedir, it.key(), filename)));
				for(Integer i : seq) {
					writer.write("\t");
					writer.write(String.valueOf(i));
				}
				writer.newLine();
				for(Integer i : seq) {
					writer.write(String.valueOf(i));
					for(Integer j : seq) {
						writer.write("\t");
						writer.write(String.valueOf(matrix[i][j]));
					}
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void main(String args[]) throws IOException {
		final String MODULE_NAME = "estimatortest";
		
		Config config = Loader.loadConfig(args[0]);
		
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(config.getParam(MODULE_NAME, "graphfile"));
		
		int nSims = Integer.parseInt(config.findParam(MODULE_NAME, "simulations"));
		int seeds = Integer.parseInt(config.findParam(MODULE_NAME, "seeds"));
		double proba = Double.parseDouble(config.findParam(MODULE_NAME, "responserate"));
		String output = config.getParam(MODULE_NAME, "output");
		String estimtype = config.getParam(MODULE_NAME, "estimtype");
		
		Level level = Logger.getRootLogger().getLevel();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		EstimatorTest counter = new EstimatorTest(graph);
		for(int i = 0; i < nSims; i++) {
			Sampler<Graph, Vertex, Edge> sampler = new Sampler<Graph, Vertex, Edge>();
			sampler.setSeedGenerator(new FixedSizeRandomPartition<Vertex>(seeds, (long) (Math.random() * nSims)));
			sampler.setResponseGenerator(new RandomPartition<Vertex>(proba, (long) (Math.random() * nSims)));
			counter.reset(graph, estimtype);
			sampler.setListener(counter);
			sampler.run(graph);
			if(i % 10 == 0) {
				Logger.getRootLogger().setLevel(level);
				logger.info(String.format("%1$s simulations done.", i));
				Logger.getRootLogger().setLevel(Level.WARN);
			}
		}
		Logger.getRootLogger().setLevel(level);
		logger.info("Analyzing...");
		counter.analyze(graph, output);
		logger.info("Done.");
	}
	
}
