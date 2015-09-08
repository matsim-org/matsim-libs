/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeGenderTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import org.matsim.core.population.PersonUtils;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.Degree;
import playground.johannes.sna.graph.analysis.ModuleAnalyzerTask;
import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.analysis.ObservedDegree;
import playground.johannes.sna.snowball.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 * 
 */
public class DegreeGenderTask extends ModuleAnalyzerTask<Degree> {

	public DegreeGenderTask() {
		module = new ObservedDegree();
	}
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		if (getOutputDirectory() != null) {
			SampledGraph sampledGraph = (SampledGraph) graph;
			List<?> partitions = SnowballPartitions.createSampledPartitions(sampledGraph.getVertices());

			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "k_gender.txt"));
				writer.write("it\tmale\tfemale");
				writer.newLine();
				
				for (int i = 0; i < partitions.size(); i++) {
					Set<SocialVertex> parition = (Set<SocialVertex>) partitions.get(i);
					Set<SocialVertex> male = new HashSet<SocialVertex>();
					Set<SocialVertex> female = new HashSet<SocialVertex>();
					for (SocialVertex vertex : parition) {
						if ("m".equalsIgnoreCase(PersonUtils.getSex(vertex.getPerson().getPerson())))
							male.add(vertex);
						else if ("f".equalsIgnoreCase(PersonUtils.getSex(vertex.getPerson().getPerson())))
							female.add(vertex);
					}
					
					
					writer.write(String.valueOf(i));
					writer.write("\t");
					writer.write(String.valueOf(module.statistics(male).getMean()));
					writer.write("\t");
					writer.write(String.valueOf(module.statistics(female).getMean()));
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
