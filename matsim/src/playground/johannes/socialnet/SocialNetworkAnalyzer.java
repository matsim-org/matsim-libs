/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnet;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.socialnet.io.SNGraphMLReader;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class SocialNetworkAnalyzer {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadPopulation();
		Scenario scenario = loader.getScenario();
		Population population = scenario.getPopulation();
		SNGraphMLReader<Person> reader = new SNGraphMLReader<Person>(population);
		SocialNetwork<Person> g = reader.readGraph(args[1]);
		
		String outputDir = args[2];
		
		/*
		 * Degree
		 */
		double k_mean = GraphStatistics.getDegreeStatistics(g).getMean();
		WeightedStatistics.writeHistogram(GraphStatistics.getDegreeDistribution(g).absoluteDistribution(), outputDir + "degree.hist.txt");
		/*
		 * Clustering
		 */
		double c_mean = GraphStatistics.getClusteringStatistics(g).getMean();
		WeightedStatistics.writeHistogram(GraphStatistics.getClusteringDegreeCorrelation(g), outputDir + "c_k.hist.txt");
		/*
		 * Edgelength distribution
		 */
		double d_mean = SocialNetworkStatistics.getEdgeLengthDistribution(g).mean();
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(g).absoluteDistribution(1000), outputDir + "dist.hist.txt");
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDistribution(g, true, 1000).absoluteDistribution(1000), outputDir + "dist.norm.hist.txt");
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDegreeCorrelation(g), outputDir + "d_k.hist.txt");
		/*
		 * Summary
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + "summary.txt"));
		writer.write("Mean Degree=");
		writer.write(String.valueOf(k_mean));
		writer.newLine();
		writer.write("Mean Lokal Clustering=");
		writer.write(String.valueOf(c_mean));
		writer.newLine();
		writer.write("Mean Edgelength=");
		writer.write(String.valueOf(d_mean));
		writer.newLine();
		writer.close();
	}

}
