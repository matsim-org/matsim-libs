/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityFacilitySelector.java
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
package playground.johannes.coopsim.mental.choice;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.util.MatsimCoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class ActivityFacilitySelector implements ChoiceSelector {

	public static final String KEY = "facility";
	
	private final Map<String, FacilityChoiceSetGenerator> generators;
	
	public ActivityFacilitySelector() {
		generators = new HashMap<String, FacilityChoiceSetGenerator>();
	}
	
	public void addGenerator(String type, FacilityChoiceSetGenerator generator) {
		generators.put(type, generator);
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		String type = (String)choices.get(ActivityTypeSelector.KEY);
		FacilityChoiceSetGenerator generator = generators.get(type);
		
		@SuppressWarnings("unchecked")
		List<SocialVertex> egos = (List<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
		
		ChoiceSet<Id<ActivityFacility>> choiceSet = generator.generate(egos);
		Id<ActivityFacility> facility = choiceSet.randomChoice();
		
		choices.put(KEY, facility);
		
//		writeDistribution(egos, choiceSet, (String)choices.get(ActivityTypeSelector.KEY));
		
		return choices;
	}

	public static ActivityFacilities facilities;
	
	private DistanceCalculator calculator = new CartesianDistanceCalculator();
	
	private DescriptiveStatistics stats = new DescriptiveStatistics();
	
	private int count;
	
	private void writeDistribution(List<SocialVertex> egos, ChoiceSet<Id> choiceSet, String type) {
		
		for(SocialVertex ego : egos) {
			Point p1 = ego.getPoint();
			for(Id id : choiceSet.getChoices()) {
				ActivityFacility fac = facilities.getFacilities().get(id);
				Point p2 = MatsimCoordUtils.coordToPoint(fac.getCoord());
				
				double d = calculator.distance(p1, p2);
				stats.addValue(d);
			}
		}
		
		count++;
		
		if(count % 1000 == 0) {
			Discretizer discretizer = FixedSampleSizeDiscretizer.create(stats.getValues(), 50, 50);
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, discretizer, true);
			try {
				StatsWriter.writeHistogram(hist, "d", "p", String.format("/Users/jillenberger/Work/socialnets/locationChoice/output/%1$s.%2$s.choiceset.txt", count, type));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
