/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author johannes
 *
 */
public class SpeedFactorAnalyzer extends AnalyzerTask {

	public static final String KEY = "speedFactor";
	
	@Override
	public void analyze(Collection<ProxyPerson> persons, Map<String, DescriptiveStatistics> results) {
		Set<String> modes = new HashSet<String>();
		for(ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();
			for(int i = 0; i < plan.getLegs().size(); i++) {
				modes.add(plan.getLegs().get(i).getAttribute(CommonKeys.LEG_MODE));
			}
		}

		modes.add(null);
		
		for(String mode : modes) {
			TDoubleArrayList distances = new TDoubleArrayList(persons.size() * 3);
			TDoubleArrayList durations = new TDoubleArrayList(persons.size() * 3);
			
			double sumDist = 0;
			double sumDur = 0;
			for(ProxyPerson person : persons) {
				ProxyPlan plan = person.getPlan();
				for(Element leg : plan.getLegs()) {
					if(mode == null || mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
					String distVal = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
					String startVal = leg.getAttribute(CommonKeys.LEG_START_TIME);
					String endVal = leg.getAttribute(CommonKeys.LEG_END_TIME);
					
					if(distVal != null && startVal != null && endVal != null) {
						double dist = Double.parseDouble(distVal);
						double start = Double.parseDouble(startVal);
						double end = Double.parseDouble(endVal);
			
						double tt = end - start;
						if(tt > 0) {
						distances.add(dist);
						durations.add(tt);
						
						sumDist += dist;
						sumDur += tt;
						}
					}
				}
			}
			}
			
			if(mode == null)
				mode = "all";
		
			String key = String.format("%s.%s", KEY, mode);
			
			double factor = sumDist/sumDur;
			
			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(factor);
			results.put(key, stats);
			
			if(outputDirectoryNotNull()) {
		
			TDoubleDoubleHashMap map = Correlations.mean(distances.toNativeArray(), durations.toNativeArray(), new LinearDiscretizer(1000));
			try {
				TXTWriter.writeMap(map, "Distance", "Traveltime", getOutputDirectory() + key + ".txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			}	
			
			
		}
		
		
	}

	
}
