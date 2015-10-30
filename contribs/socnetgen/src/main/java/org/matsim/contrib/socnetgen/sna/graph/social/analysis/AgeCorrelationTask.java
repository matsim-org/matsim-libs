/* *********************************************************************** *
 * project: org.matsim.*
 * AgeCorrelationTask.java
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
package org.matsim.contrib.socnetgen.sna.graph.social.analysis;

import gnu.trove.TIntArrayList;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class AgeCorrelationTask extends ModuleAnalyzerTask<Age> {

	public AgeCorrelationTask(Age module) {
		setModule(module);
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SocialGraph graph = (SocialGraph) g;
		
		if(outputDirectoryNotNull()) {
			TIntArrayList egoVals = new TIntArrayList();
			TIntArrayList alterVals = new TIntArrayList();
			
			TObjectDoubleHashMap<Vertex> vals = module.values(graph.getVertices());
			TObjectDoubleIterator<Vertex> it = vals.iterator();
			for(int i = 0; i < vals.size(); i++) {
				it.advance();
				SocialVertex ego = (SocialVertex)it.key();
				int egoAge = ego.getPerson().getAge();
				if(egoAge > 0) {
					for(SocialVertex alter : ego.getNeighbours()) {
						int alterAge = alter.getPerson().getAge();
						if(alterAge > 0) {
							egoVals.add(egoAge);
							alterVals.add(alterAge);
						}
					}
					
				}
				
			}
			
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/age.xy.txt"));
				for(int i = 0; i < egoVals.size(); i++) {
					writer.write(String.valueOf(egoVals.get(i)));
					writer.write("\t");
					writer.write(String.valueOf(alterVals.get(i)));
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
