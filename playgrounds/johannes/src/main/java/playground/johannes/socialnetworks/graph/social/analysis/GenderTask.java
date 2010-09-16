/* *********************************************************************** *
 * project: org.matsim.*
 * GenderTask.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;

import playground.johannes.socialnetworks.graph.social.SocialGraph;

/**
 * @author illenberger
 *
 */
public class GenderTask extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			Gender gender = new Gender();
			int[][] matrix = gender.socioMatrix((SocialGraph) graph);
			
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "gender.txt"));
				writer.write("\tm\tf");
				writer.newLine();
				for(int i = 0; i < 2; i++) {
					if(i==0)
						writer.write("m");
					else
						writer.write("f");
					
					for(int j = 0; j < 2; j++) {
						writer.write("\t");
						writer.write(String.valueOf(matrix[i][j]));
						
					}
					writer.newLine();
				}
				
				writer.close();
				
				writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "gender.hist.txt"));
				writer.write("gender\tcount");
				writer.newLine();
				TObjectIntIterator<String> it = gender.distribution((SocialGraph) graph).iterator();
				for(int i = 0; i < 2; i++) {
					it.advance();
					writer.write(it.key());
					writer.write("\t");
					writer.write(String.valueOf(it.value()));
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
