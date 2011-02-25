/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeShare.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListener;

/**
 * @author illenberger
 *
 */
public class DegreeShare implements SamplerListener {

	private TreeMap<Integer, TDoubleDoubleHashMap> table = new TreeMap<Integer, TDoubleDoubleHashMap>();
	
	private String output;

	private int prevN = 0;
	
	private int lastIteration = 0;
	
	public DegreeShare(String output) {
		this.output = output;
	}
	
	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
//		int n = sampler.getNumSampledVertices();
//		if (n > prevN) {
//			if (n < 1000) {
//				if (n % 100 == 0)
//					dump(sampler);
//			} else if (n < 10000) {
//				if (n % 1000 == 0)
//					dump(sampler);
//			} else {
//				if (n % 10000 == 0)
//					dump(sampler);
//			}
//		}
//		prevN = n;
//		return true;
		if (sampler.getIteration() > lastIteration) {
			 dump(sampler);
			 lastIteration = sampler.getIteration();
		 }
		
		 return true;
	}

	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
		DescriptiveStatistics stats = ObservedDegree.getInstance().statistics(sampler.getSampledGraph().getVertices());
		TDoubleDoubleHashMap trueHist = Histogram.createHistogram(stats, new LinearDiscretizer(1.0), false);
		
		for(Entry<Integer, TDoubleDoubleHashMap> entry : table.entrySet()) {
			TDoubleDoubleHashMap hist = entry.getValue();
			TDoubleDoubleHashMap share = new TDoubleDoubleHashMap();
			
			TDoubleDoubleIterator it = trueHist.iterator();
			for(int i = 0; i < trueHist.size(); i++) {
				it.advance();
				
				double current = hist.get(it.key());
				double real = it.value();
				
				share.put(it.key(), current/real);
			}
			
			entry.setValue(share);
		}
		
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(output + "k_share.txt"));

			double[] ks = table.get(table.lastKey()).keys();
			Arrays.sort(ks);
			for(double k : ks) {
				writer.write("\t");
				writer.write(String.valueOf(k));
			}
			writer.newLine();

			for (Entry<Integer, TDoubleDoubleHashMap> entry : table.entrySet()) {
				writer.write(entry.getKey().toString());

				for(double k : ks) {
					writer.write("\t");
					double val = entry.getValue().get(k);
//					if(val == 0)
//						writer.write("NA");
//					else
						writer.write(String.valueOf(val));
				}
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void dump(Sampler<?,?,?> sampler) {
		DescriptiveStatistics stats = ObservedDegree.getInstance().statistics(sampler.getSampledGraph().getVertices());
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(1.0), false);
		table.put(sampler.getIteration(), hist);
	}
}
