/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceAge.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.spatial.analysis.Distance;

/**
 * @author illenberger
 * 
 */
public class DistanceAge extends AnalyzerTask {

	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		TDoubleObjectHashMap<Set<SocialVertex>> partitions = new TDoubleObjectHashMap<Set<SocialVertex>>();

		Discretizer discr = new LinearDiscretizer(5.0);

		SocialGraph g = (SocialGraph) graph;
		for (SocialVertex vertex : g.getVertices()) {
			if (((SampledVertex) vertex).isSampled()) {
				double age = discr.discretize(vertex.getPerson().getAge());
				Set<SocialVertex> partition = partitions.get(age);
				if (partition == null) {
					partition = new HashSet<SocialVertex>();
					partitions.put(age, partition);
				}
				partition.add(vertex);
			}
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/d_age.txt"));
			writer.write("age\tdist");
			writer.newLine();
			
			Distance dist = new Distance();
			TDoubleObjectIterator<Set<SocialVertex>> it = partitions.iterator();
			for (int i = 0; i < partitions.size(); i++) {
				it.advance();
				double mean = dist.vertexMeanDistribution(it.value()).mean();
			
				writer.write(String.valueOf(it.key()));
				writer.write("\t");
				writer.write(String.valueOf(mean));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
