/* *********************************************************************** *
 * project: org.matsim.*
 * EducationTask.java
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

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 * 
 */
public class EducationTask extends SocioMatrixTask {

	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		if (getOutputDirectory() != null) {
			try {
				SocialGraph graph = (SocialGraph) g;

				Education edu = new Education();

				Set<SocialVertex> male = new HashSet<SocialVertex>();
				Set<SocialVertex> female = new HashSet<SocialVertex>();
				for(SocialVertex vertex : graph.getVertices()) {
					if("m".equalsIgnoreCase(vertex.getPerson().getPerson().getSex()))
						male.add(vertex);
					else if("f".equalsIgnoreCase(vertex.getPerson().getPerson().getSex()))
						female.add(vertex);
				}
//				double total = male.size() + female.size();
//				ObservedDegree degree = new ObservedDegree();
//				System.err.println("Degree female = " + degree.distribution(female).mean());
//				System.err.println("Degree male = " + degree.distribution(male).mean());
				
				TObjectIntHashMap<String> distr = edu.distribution(graph.getVertices());
				TObjectIntHashMap<String> distrMale = edu.distribution(male);
				TObjectIntHashMap<String> distrFemale = edu.distribution(female);
				
				BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/edu.txt"));
				TObjectIntIterator<String> it = distr.iterator();
				writer.write("edu\ttotal\tmale\tfemale\tshare_male\tshare_female");
				writer.newLine();
				for(int i = 0; i < distr.size(); i++) {
					it.advance();
					writer.write(it.key());
					writer.write("\t");
					writer.write(String.valueOf(it.value()));
					writer.write("\t");
					writer.write(String.valueOf(distrMale.get(it.key())));
					writer.write("\t");
					writer.write(String.valueOf(distrFemale.get(it.key())));
					
					writer.write("\t");
					writer.write(String.valueOf(distrMale.get(it.key())/(double)male.size()));
					writer.write("\t");
					writer.write(String.valueOf(distrFemale.get(it.key())/(double)female.size()));
					
					writer.newLine();
				}
				writer.close();
				
//				writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/edu.matrix.txt"));
				double[][] matrix = edu.socioMatrix(graph);
				List<String> values = edu.getAttributes();
				double[][] normMatrix = edu.normalizedSocioMatrix(matrix, distr, values);
				
				double[][] matrixAvr = edu.socioMatrixLocalAvr(graph);
				values = edu.getAttributes();
				
//				writeSocioMatrix(matrix, values, getOutputDirectory() + "/edu.matrix.txt");
//				writeSocioMatrix(normMatrix, values, getOutputDirectory() + "/edu.matrix.norm.txt");
				writeSocioMatrix(matrixAvr, values, getOutputDirectory() + "/edu.matrix.norm2.txt");
				
				/*
				 * 
				 * 
				 */
				Set<SocialVertex> academic = new HashSet<SocialVertex>();
				Set<SocialVertex> nonacademic = new HashSet<SocialVertex>();
				for(SocialVertex vertex : graph.getVertices()) {
					if("6".equalsIgnoreCase(vertex.getPerson().getEducation()) || "7".equalsIgnoreCase(vertex.getPerson().getEducation()))
						academic.add(vertex);
					else if(vertex.getPerson().getEducation() != null)
						nonacademic.add(vertex);
				}
//				double total = male.size() + female.size();
				ObservedDegree degree = new ObservedDegree();
				System.err.println("Degree academic = " + degree.distribution(academic).getMean());
				System.err.println("Degree nonacademic = " + degree.distribution(nonacademic).getMean());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
