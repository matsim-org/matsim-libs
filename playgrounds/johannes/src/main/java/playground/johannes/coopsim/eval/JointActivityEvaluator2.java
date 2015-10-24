/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityEvaluator.java
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
package playground.johannes.coopsim.eval;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.pysical.VisitorTracker;
import playground.johannes.mz2005.io.ActivityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class JointActivityEvaluator2 implements Evaluator {

	private double V_star;
	
	private final VisitorTracker tracker;
	
	private final Map<Person, SocialVertex> vertices;
	
	private final Map<Person, List<Person>> alters;
	
	private final double fVisit;
	
	private final double fCulture;
	
	private final double fGastro;
	
	private static boolean isLogging;
	
	private static DescriptiveStatistics stats;
	
	private static DescriptiveStatistics visitStats;
	
	private static DescriptiveStatistics cultureStats;
	
	private static DescriptiveStatistics gastroStats;
	
	public JointActivityEvaluator2(double V_start, VisitorTracker tracker, SocialGraph graph, double fVisit, double fCulture, double fGastro) {
		this.V_star = V_start;
		this.tracker = tracker;
		this.fVisit = fVisit;
		this.fCulture = fCulture;
		this.fGastro = fGastro;
		
		vertices = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		alters = new HashMap<Person, List<Person>>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices()) {
			vertices.put(v.getPerson().getPerson(), v);
			List<Person> neighbours = new ArrayList<Person>(v.getNeighbours().size());
			for(SocialVertex alter : v.getNeighbours()) {
				neighbours.add(alter.getPerson().getPerson());
			}
			alters.put(v.getPerson().getPerson(), neighbours);
		}
	}
	
	@Override
	public double evaluate(Trajectory trajectory) {
		double score = 0;

		String type = ((Activity)trajectory.getElements().get(2)).getType();
		List<Person> alterList = alters.get(trajectory.getPerson());
		
	
		int n = tracker.metAlters(trajectory.getPerson(), alterList);
		if(n >= 1 && V_star > 0) {
			double f_star = 0;
			
			if(type.equals(ActivityType.visit.name())) {
				f_star = fVisit;
			} else if(type.equals(ActivityType.culture.name())) {
				f_star = fCulture;
			} else if(type.equals(ActivityType.gastro.name())) {
				f_star = fGastro;
			} else if(type.equals(ActivityType.home.name())) {
				f_star = 0;
			} else {
				throw new IllegalArgumentException(String.format("Unknown activity type in joint activity scoring (%1$s).", type));
			}
			
			if(f_star > 0) {
				// round f_start to a realizable value
				double n_star = Math.round(f_star * alterList.size());
				n_star = Math.max(n_star, 1);
				f_star = n_star / (double)alterList.size();
				
				double f = n/(double)alterList.size();
				
//				V_star = V_star/Math.log(2) * Math.log(f_star + 1);
				score = -(V_star/(f_star * f_star)) * Math.pow((f - f_star), 2) + V_star;
				
				if(Double.isNaN(score)) {
					throw new RuntimeException("Joint score is NaN.");
				} else if(Double.isInfinite(score)) {
					throw new RuntimeException("Joint score is infty.");
				}
			} else {
				score = 0;
			}
		} else {
			score = 0;
		}
		
		if(isLogging && !type.equals(ActivityType.home.name())) {
			stats.addValue(score);
			if(type.equals(ActivityType.visit.name())) {
				visitStats.addValue(score);
			} else if(type.equals(ActivityType.culture.name())) {
				cultureStats.addValue(score);
			} else if(type.equals(ActivityType.gastro.name())) {
				gastroStats.addValue(score);
			}
		}
		
		return score;
	}
	
	public static void startLogging() {
		stats = new DescriptiveStatistics();
		visitStats = new DescriptiveStatistics();
		cultureStats = new DescriptiveStatistics();
		gastroStats = new DescriptiveStatistics();
		isLogging = true;
	}
	
	public static Map<String, DescriptiveStatistics> stopLogging() {
		Map<String, DescriptiveStatistics> map = new HashMap<String, DescriptiveStatistics>(4);
		map.put("all", stats);
		map.put(ActivityType.visit.name(), visitStats);
		map.put(ActivityType.culture.name(), cultureStats);
		map.put(ActivityType.gastro.name(), gastroStats);
		isLogging = false;
		return map;
	}

}
