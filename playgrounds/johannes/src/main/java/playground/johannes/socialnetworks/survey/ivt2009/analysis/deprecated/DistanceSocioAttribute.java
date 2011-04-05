/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceSocioAttribute.java
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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.analysis.ObservedDegree;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbability;
import playground.johannes.socialnetworks.graph.spatial.analysis.Distance;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedDistance;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class DistanceSocioAttribute extends AnalyzerTask {

	private Set<Point> choiceSet;
	
	public DistanceSocioAttribute(Set<Point> choiceSet) {
		this.choiceSet = choiceSet;
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> stats) {
		try {
			SocialGraph graph = (SocialGraph) g;
			Distance dist = new ObservedDistance();
			AcceptanceProbability acc = new AcceptanceProbability();
			Degree degree = new ObservedDegree();
			
			Discretizer discretizer = new LinearDiscretizer(1.0);
			/*
			 * male
			 */
			Set<SocialVertex> vertices = new HashSet<SocialVertex>();
			for (SocialVertex vertex : graph.getVertices()) {
				if ("m".equalsIgnoreCase(vertex.getPerson().getPerson().getSex()))
					vertices.add(vertex);
			}

			DescriptiveStatistics ds = new DescriptiveStatistics();
			ds.addValue(ResponseRate.responseRate((Set) vertices));
			stats.put("alpha_male", ds);
			
			Distribution distr = dist.distribution(vertices);
			Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), getOutputDirectory() + "/d_male_txt");
			ds = new DescriptiveStatistics();
			ds.addValue(distr.mean());
			stats.put("d_mean_male", ds);
			
			DescriptiveStatistics dStats = acc.distribution(vertices, choiceSet);
			TDoubleDoubleHashMap hist = Histogram.createHistogram(dStats, new LinearDiscretizer(1000.0), false);
			TXTWriter.writeMap(hist, "d", "p", getOutputDirectory()+"/p_acc_male.txt");
//			Distribution.writeHistogram(dStats.absoluteDistributionLog2(1000), getOutputDirectory()+"/p_acc_male.txt");
			
			DescriptiveStatistics kDistr = degree.distribution(vertices);
			stats.put("k_male", kDistr);
			Distribution.writeHistogram(Histogram.createHistogram(kDistr, new LinearDiscretizer(1.0), false), getOutputDirectory() + "/k_male.txt");
			
			System.out.println("Male: " + kDistr.getValues().length);
			/*
			 * female
			 */
			vertices = new HashSet<SocialVertex>();
			for (SocialVertex vertex : graph.getVertices()) {
				if ("f".equalsIgnoreCase(vertex.getPerson().getPerson().getSex()))
					vertices.add(vertex);
			}

			ds = new DescriptiveStatistics();
			ds.addValue(ResponseRate.responseRate((Set) vertices));
			stats.put("alpha_female", ds);
			
			distr = dist.distribution(vertices);
			Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), getOutputDirectory() + "/d_female_txt");
			ds = new DescriptiveStatistics();
			ds.addValue(distr.mean());
			stats.put("d_femean_male", ds);
			
			dStats = acc.distribution(vertices, choiceSet);
			hist = Histogram.createHistogram(dStats, new LinearDiscretizer(1000.0), false);
			TXTWriter.writeMap(hist, "d", "p", getOutputDirectory()+"/p_acc_female.txt");
//			Distribution.writeHistogram(acc.distribution(vertices, choiceSet).absoluteDistributionLog2(1000), getOutputDirectory()+"/p_acc_female.txt");
			
			kDistr = degree.distribution(vertices);
			stats.put("k_female", kDistr);
			Distribution.writeHistogram(Histogram.createHistogram(kDistr, new LinearDiscretizer(1.0), false), getOutputDirectory() + "/k_female.txt");

			System.out.println("Female: " + kDistr.getValues().length);
			/*
			 * edu
			 */
//			Education edu = new Education();
//			edu.socioMatrix(graph);
//			for(String att : edu.getAttributes()) {
//				vertices = new HashSet<SocialVertex>();
//				for (SocialVertex vertex : graph.getVertices()) {
//					if (att.equalsIgnoreCase(vertex.getPerson().getEducation()))
//						vertices.add(vertex);
//				}
//
//				stats.put("alpha_edu"+att, ResponseRate.responseRate((Set) vertices));
//				
//				distr = dist.distribution(vertices);
//				Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), getOutputDirectory() + "/d_edu"+att+".txt");
//				stats.put("d_mean_edu"+att, distr.mean());
//			
//				dStats = acc.distribution(vertices, choiceSet);
//				hist = Histogram.createHistogram(dStats, new LinearDiscretizer(1000.0), false);
//				TXTWriter.writeMap(hist, "d", "p", getOutputDirectory()+"/p_acc_edu"+att+".txt");
////				Distribution.writeHistogram(acc.distribution(vertices, choiceSet).absoluteDistributionLog2(1000), getOutputDirectory()+"/p_acc_edu"+att+".txt");
//				
//				kDistr = degree.distribution(vertices);
//				stats.put("k_mean_edu"+att, kDistr.getMean());
//				Distribution.writeHistogram(Histogram.createHistogram(kDistr, new LinearDiscretizer(1.0), false), getOutputDirectory() + "/k_edu"+att+".txt");
//
//				System.out.println("Edu"+att+": " + kDistr.getValues().length);
//			}
			/*
			 * income
			 */
			Map<Integer, Set<SocialVertex>> incomes = new HashMap<Integer, Set<SocialVertex>>();
			for(SocialVertex vertex : graph.getVertices()) {
				Set<SocialVertex> set = incomes.get(vertex.getPerson().getIncome());
				if(set == null) {
					set = new HashSet<SocialVertex>();
					incomes.put(vertex.getPerson().getIncome(), set);
				}
				set.add(vertex);
			}
			
			
			for(Entry<Integer, Set<SocialVertex>> entry : incomes.entrySet()) {
				vertices = entry.getValue();
				distr = dist.distribution(vertices);
				String att = String.valueOf(entry.getKey());
				
				
				Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), getOutputDirectory() + "/d_income"+att+".txt");
				ds = new DescriptiveStatistics();
				ds.addValue(distr.mean());
				stats.put("d_mean_income"+att, ds);
				
				dStats = acc.distribution(vertices, choiceSet);
				hist = Histogram.createHistogram(dStats, new LinearDiscretizer(1000.0), false);
				TXTWriter.writeMap(hist, "d", "p", getOutputDirectory()+"/p_acc_income"+att+".txt");
//				Distribution.writeHistogram(acc.distribution(vertices, choiceSet).absoluteDistributionLog2(1000), getOutputDirectory()+"/p_acc_income"+att+".txt");
				
				kDistr = degree.distribution(vertices);
				stats.put("k_income"+att, kDistr);
				Distribution.writeHistogram(Histogram.createHistogram(kDistr, discretizer, false), getOutputDirectory() + "/k_income"+att+".txt");

				System.out.println("Income"+att+": " + kDistr.getValues().length);
			}
			/*
			 * civil status
			 */
			Map<String, Set<SocialVertex>> civilstatus = new HashMap<String, Set<SocialVertex>>();
			for(SocialVertex vertex : graph.getVertices()) {
				Set<SocialVertex> set = civilstatus.get(vertex.getPerson().getCiviStatus());
				if(set == null) {
					set = new HashSet<SocialVertex>();
					civilstatus.put(vertex.getPerson().getCiviStatus(), set);
				}
				set.add(vertex);
			}
			
			for(Entry<String, Set<SocialVertex>> entry : civilstatus.entrySet()) {
				vertices = entry.getValue();
				distr = dist.distribution(vertices);
				String att = entry.getKey();
				
				
				Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), getOutputDirectory() + "/d_cstatus"+att+".txt");
				ds = new DescriptiveStatistics();
				ds.addValue(distr.mean());
				stats.put("d_mean_cstatus"+att, ds);
				
				dStats = acc.distribution(vertices, choiceSet);
				hist = Histogram.createHistogram(dStats, new LinearDiscretizer(1000.0), false);
				TXTWriter.writeMap(hist, "d", "p", getOutputDirectory()+"/p_acc_cstatus"+att+".txt");
//				Distribution.writeHistogram(acc.distribution(vertices, choiceSet).absoluteDistributionLog2(1000), getOutputDirectory()+"/p_acc_cstatus"+att+".txt");
				
				kDistr = degree.distribution(vertices);
				stats.put("k_cstatus"+att, kDistr);
				Distribution.writeHistogram(Histogram.createHistogram(kDistr, discretizer, false), getOutputDirectory() + "/k_cstatus"+att+".txt");

				System.out.println("CivStatus"+att+": " + kDistr.getValues().length);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
