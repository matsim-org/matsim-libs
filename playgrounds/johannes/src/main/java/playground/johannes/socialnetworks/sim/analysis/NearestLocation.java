/* *********************************************************************** *
 * project: org.matsim.*
 * NearestLocation.java
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.sim.locationChoice.ChoiceSet;

/**
 * @author illenberger
 *
 */
public class NearestLocation implements PlansAnalyzerTask {

	private final Map<Person, ChoiceSet> choiceSet;
	
	private final String output;
	
	public NearestLocation(Map<Person, ChoiceSet> choiceSet, String output) {
		this.choiceSet = choiceSet;
		this.output = output;
	}
	
	@Override
	public void analyze(Set<Plan> plans, Map<String, Double> map) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for(Plan plan : plans) {
			for(int idx = 2; idx < plan.getPlanElements().size(); idx += 2) {
				Activity act = (Activity) plan.getPlanElements().get(idx);
				if(act.getType().startsWith("l")) {
					Activity prev = (Activity) plan.getPlanElements().get(idx - 2);
					
					Person person = plan.getPerson();
					List<Person> opportunities = choiceSet.get(person).getOpportunities();
					stats.addValue(nearestLocation(prev.getCoord(), opportunities));
				}
			}
		}
		try {
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(1000.0), false);
			TXTWriter.writeMap(hist, "d", "n", output + "/nearestLocs.txt");
			
			hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 100, 500), false);
			TXTWriter.writeMap(hist, "d", "n", output + "/nearestLocs.fixed.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private double nearestLocation(Coord origin, List<Person> opportunities) {
		double d_min = Double.MAX_VALUE;
//		Coord nearest = null;
		for(Person p : opportunities) {
			Coord dest = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getCoord();
			double dx = origin.getX() - dest.getX();
			double dy = origin.getY() - dest.getY();
			double d = Math.sqrt(dx*dx + dy*dy);
			if(d < d_min) {
				d_min = d;
//				nearest = dest;
			}
		}
		
		return d_min;
	}

}
