/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentsSize.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.Components;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class ComponentsSize extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if (getOutputDirectory() != null) {
			List<Set<Vertex>> comps = new Components().components(graph);
			try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/components.txt", getOutputDirectory())));
			writer.write("size\tseeds");
			writer.newLine();
			for (Set<Vertex> comp : comps) {
				writer.write(String.valueOf(comp.size()));
				writer.write("\t");
				int seeds = 0;
				for(Vertex v : comp) {
					if(((SampledVertex)v).isSampled()) {
						if(((SampledVertex)v).getIterationSampled() == 0) {
							seeds++;
						}
					}
				}
				writer.write(String.valueOf(seeds));
				writer.newLine();
			}
			writer.close();
//			try {
//				Distribution.writeHistogram(distr.absoluteDistribution(), String.format("%1$s/components.txt", getOutputDirectory()));
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
