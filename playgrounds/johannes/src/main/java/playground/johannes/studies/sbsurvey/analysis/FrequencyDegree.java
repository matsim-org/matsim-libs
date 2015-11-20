/* *********************************************************************** *
 * project: org.matsim.*
 * FrequencyDegree.java
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
package playground.johannes.studies.sbsurvey.analysis;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Degree;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.sna.math.Distribution;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class FrequencyDegree extends ModuleAnalyzerTask<Degree> {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		try {
		SocialGraph g = (SocialGraph) graph;
		TDoubleArrayList values1 = new TDoubleArrayList();
		TDoubleArrayList values2 = new TDoubleArrayList();
		
		Frequency freq = new Frequency();
		TObjectDoubleHashMap<SocialVertex> meanFreq = freq.meanEdgeFrequency(g.getVertices());
		TObjectDoubleIterator<SocialVertex> it = meanFreq.iterator();
		for(int i = 0; i < meanFreq.size(); i++) {
			it.advance();
			values1.add(it.key().getNeighbours().size());
			values2.add(it.value());
		}
		
		
			Correlations.writeToFile(Correlations.mean(values1.toArray(), values2.toArray(), 5),
					getOutputDirectory()+"/freq_mean_k.txt", "k", "freq");
		/*
		 * freq sum
		 */
			values1 = new TDoubleArrayList();
			values2 = new TDoubleArrayList();
			
			freq = new Frequency();
			TObjectDoubleHashMap<SocialVertex> sumFreq = freq.sumEdgeFrequency(g.getVertices());
			it = sumFreq.iterator();
			for(int i = 0; i < meanFreq.size(); i++) {
				it.advance();
				values1.add(it.key().getNeighbours().size());
				values2.add(it.value());
			}
					
			Correlations.writeToFile(Correlations.mean(values1.toArray(), values2.toArray(), 2),
						getOutputDirectory()+"/freq_sum_k.txt", "k", "freq");
			
		
		Distribution distr = freq.sumEdgeFrequencyDistribtion(g.getVertices());
		Distribution.writeHistogram(distr.absoluteDistributionLog2(1), getOutputDirectory()+"/freq_i.txt");
		
		distr = freq.freqLengthDistribution(g.getVertices());
		Distribution.writeHistogram(distr.absoluteDistribution(1000), getOutputDirectory()+"freqLength.txt");
		
		distr = freq.sumFreqLengthDistribution(g.getVertices());
		Distribution.writeHistogram(distr.absoluteDistribution(1000), getOutputDirectory()+"freqCost_i.txt");
		
		/*
		 *sum(freq * cost) over k
		 */
		values1 = new TDoubleArrayList();
		values2 = new TDoubleArrayList();
		
		freq = new Frequency();
		TObjectDoubleHashMap<SocialVertex> sumFreqCost = freq.sumFreqCost(g.getVertices());
		it = sumFreqCost.iterator();
		for(int i = 0; i < meanFreq.size(); i++) {
			it.advance();
			values1.add(it.key().getNeighbours().size());
			values2.add(it.value());
		}
				
		Correlations.writeToFile(Correlations.mean(values1.toArray(), values2.toArray(), 2),
					getOutputDirectory()+"/sum_freqCost_k.txt", "k", "c_freq");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
