/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeShare.java
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
package playground.johannes.socialnetworks.snowball2.sim.postprocess;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;

/**
 * @author illenberger
 *
 */
public class EstimDiff extends RunLocator<EstimDiffContainer> {

	private static final Logger logger = Logger.getLogger(EstimDiff.class);
	
	private TDoubleDoubleHashMap targetDistr;
	
	private boolean normalize;
	
	private Distribution distribution = new Distribution();
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		File rootDir = new File(args[0]);
		final String dumpPattern = args[1];
		String analyzerKey = args[2];
		String outputTable = args[3];
		String graphFile = args[4];
		
		logger.info(String.format("Root dir = %1$s", rootDir));
		
		EstimDiff degreeShare = new EstimDiff();
		degreeShare.targetDistr = loadDegreeDistr(graphFile);
		degreeShare.normalize = false;
		Map<String, EstimDiffContainer[]> dataTable = degreeShare.locate(rootDir, dumpPattern, analyzerKey);
//		Map<String, TDoubleDoubleHashMap> avrTable = degreeShare.averageDistritburions(dataTable);
		Map<String, TDoubleDoubleHashMap> avrTable = degreeShare.average(dataTable);
		/*
		 * Write.
		 */
		logger.info("Writing table...");
		List<String> dumpKeys = degreeShare.sortDumpKeys(dataTable);
		
		double[] bins = degreeShare.targetDistr.keys();
		Arrays.sort(bins);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputTable));
		for(double bin : bins) {
			writer.write(String.valueOf(bin));
			writer.write("\t");
		}
		writer.newLine();
		
		
		for(String dumpKey : dumpKeys) {
			writer.write(dumpKey);
			writer.write("\t");
			for(double bin : bins) {
				TDoubleDoubleHashMap distr = avrTable.get(dumpKey);
				writer.write(String.valueOf(distr.get(bin)));
				writer.write("\t");
			}
			writer.newLine();
		}
		writer.close();
		
		logger.info("Done.");
	}
	
	private static TDoubleDoubleHashMap loadDegreeDistr(String graphFile) {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph(graphFile);
		
		return Histogram.createHistogram(Degree.getInstance().distribution(graph.getVertices()), new LinearDiscretizer(1.0), false);
	}

	@Override
	protected EstimDiffContainer[] createObjectArray(int nRuns) {
		return new EstimDiffContainer[nRuns];
	}

	@Override
	protected void inDirectory(String path, String dumpKey, int runIdx, EstimDiffContainer[] relDistrs) {
		File file = new File(path);
		TDoubleDoubleHashMap kDistr = loadDistribution(String.format("%1$s/obs/k.txt", file.getParent()));
		TDoubleDoubleHashMap pDistr = loadDistribution(String.format("%1$s/proba.txt", path));
		
		if(kDistr == null || pDistr == null)
			return;
			
		if(relDistrs[runIdx] == null)
			relDistrs[runIdx] = new EstimDiffContainer();

		TDoubleDoubleIterator it = kDistr.iterator();
		for (int i = 0; i < kDistr.size(); i++) {
			it.advance();
			int k = (int)it.key();
			
			relDistrs[runIdx].kSum.adjustOrPutValue(k, (int)it.value(), (int)it.value());
			double p = pDistr.get(k);
			relDistrs[runIdx].pSum.adjustOrPutValue(k, p, p);
			relDistrs[runIdx].kCount.adjustOrPutValue(k, 1, 1);
		}
	}

	private Map<String, TDoubleDoubleHashMap> average(Map<String, EstimDiffContainer[]> dataTable) {
		Map<String, TDoubleDoubleHashMap> avrTable = new HashMap<String, TDoubleDoubleHashMap>();
		int notFound = 0;
		for(Entry<String, EstimDiffContainer[]> entry : dataTable.entrySet()) {
			TDoubleDoubleHashMap averages = new TDoubleDoubleHashMap();
			EstimDiffContainer[] estimContainers = entry.getValue();
			TDoubleDoubleIterator it = targetDistr.iterator();
			
			TDoubleDoubleHashMap p_estim_distr = new TDoubleDoubleHashMap();
			TDoubleDoubleHashMap p_obs_distr = new TDoubleDoubleHashMap();
			
			for(int i = 0; i < targetDistr.size(); i++) {
				it.advance();
				int k = (int)it.key();
				int N_k = (int)it.value();
				
				int n_k_sum = 0;
				double p_k_sum = 0;
				int count = 0;
				int count_k = 0;
				for(EstimDiffContainer container : estimContainers) {
					if(container == null)
						notFound++;
					else {
						n_k_sum += container.kSum.get(k);
						p_k_sum += container.pSum.get(k);
						count_k += container.kCount.get(k);
						count++;
					}
				}
				
				double p_estim = p_k_sum/(double)count_k;
				double p_obs = n_k_sum/(double)(count * N_k);
			
				p_estim_distr.put(k, p_estim);
				p_obs_distr.put(k, p_obs);
//				averages.put(k, p_estim/p_obs);
			}
			
//			if(normalize) {
//				distribution.normalizedDistribution(p_estim_distr);
//				distribution.normalizedDistribution(p_obs_distr);
//			}
			
			double[] keys = p_obs_distr.keys();
			Arrays.sort(keys);
			for(int i = 1; i < keys.length; i++) {
				double x1 = keys[i-1];
				double x2 = keys[i];
//				double dx = x2 - x1;
				double dy_obs = p_obs_distr.get(x2) - p_obs_distr.get(x1);
				double dy_estim = p_estim_distr.get(x2) - p_estim_distr.get(x1);
				if(dy_obs == 0)
					averages.put(x1, Double.NaN);
				else
					averages.put(x1, dy_estim/dy_obs);
				
			}
//			it = targetDistr.iterator();
//			for(int i = 0; i < targetDistr.size(); i++) {
//				it.advance();
//				averages.put(it.key(), p_estim_distr.get(it.key()) / p_obs_distr.get(it.key()));
//			}
//			
			avrTable.put(entry.getKey(), averages);
//			
			try {
				Distribution.writeHistogram(p_obs_distr, String.format("/Users/jillenberger/Work/work/socialnets/snowball/output/%1$s.p_obs.txt", entry.getKey()));
				Distribution.writeHistogram(p_estim_distr, String.format("/Users/jillenberger/Work/work/socialnets/snowball/output/%1$s.p_estim.txt", entry.getKey()));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(notFound > 0)
			logger.warn(String.format("%1$s runs not available.", notFound));
		return avrTable;
	}

}
