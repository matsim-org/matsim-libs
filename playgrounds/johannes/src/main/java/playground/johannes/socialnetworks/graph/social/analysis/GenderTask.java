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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;

import playground.johannes.socialnetworks.graph.matrix.CalcCentrality;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.snowball2.analysis.ObservedDegree;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class GenderTask extends SocioMatrixTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			
			try {
				SocialGraph graph = (SocialGraph) g;

				Gender gender = new Gender();

					TObjectIntHashMap<String> distr = gender.distribution(graph.getVertices());
					BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/gender.txt"));
					TObjectIntIterator<String> it = distr.iterator();
					writer.write("gender\tcount");
					writer.newLine();
					for(int i = 0; i < distr.size(); i++) {
						it.advance();
						writer.write(it.key());
						writer.write("\t");
						writer.write(String.valueOf(it.value()));
						writer.newLine();
					}
					writer.close();
					
//					writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/gender.matrix.txt"));
					double[][] matrix = gender.socioMatrix(graph);
					List<String> values = gender.getAttributes();
					
					double[][] normMatrix = gender.normalizedSocioMatrix(matrix, distr, values);
					double[][] matrixAvr = gender.socioMatrixLocalAvr(graph);
					
					writeSocioMatrix(matrix, values, getOutputDirectory() + "/gender.matrix.txt");
					writeSocioMatrix(normMatrix, values, getOutputDirectory() + "/gender.matrix.norm.txt");
					writeSocioMatrix(matrixAvr, values, getOutputDirectory() + "/gender.matrix.norm2.txt");
					
					Degree degree = new ObservedDegree();
					TObjectDoubleHashMap<Vertex> kDistr = degree.values(graph.getVertices());
					
					TDoubleArrayList kValues = new TDoubleArrayList();
					TDoubleArrayList mValues = new TDoubleArrayList();
					calcValues(graph, kDistr, kValues, mValues, "m");
					TDoubleDoubleHashMap cor = Correlations.mean(kValues.toNativeArray(), mValues.toNativeArray(), 5);
					Correlations.writeToFile(cor, getOutputDirectory() + "/gender_k_m.txt", "k", "ratio male");
					
					kValues = new TDoubleArrayList();
					mValues = new TDoubleArrayList();
					calcValues(graph, kDistr, kValues, mValues, "f");
					cor = Correlations.mean(kValues.toNativeArray(), mValues.toNativeArray(), 5);
					Correlations.writeToFile(cor, getOutputDirectory() + "/gender_k_f.txt", "k", "ratio male");
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

	}
	
	private void calcValues(SocialGraph graph, TObjectDoubleHashMap<Vertex> kDistr, TDoubleArrayList kValues, TDoubleArrayList mValues, String sex) {
		TObjectDoubleIterator<Vertex> it2 = kDistr.iterator();
		for(int i = 0; i < kDistr.size(); i++) {
			it2.advance();
			if(sex.equalsIgnoreCase(((SocialVertex)it2.key()).getPerson().getPerson().getSex())) {
			kValues.add(it2.value());
			int male = 0;
			int total = 0;
			for(Vertex neighbor : it2.key().getNeighbours()) {
				if((((SocialVertex)neighbor).getPerson().getPerson().getSex()) != null)
					total++;
				
				if("m".equalsIgnoreCase(((SocialVertex)neighbor).getPerson().getPerson().getSex())) {
					male++;
				}
			}
			
			mValues.add(male/(double)total);
			}
		}
	}

}
