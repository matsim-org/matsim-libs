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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyLeg;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.sna.math.DummyDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;

/**
 * @author johannes
 *
 */
public class LegDistanceTask implements ProxyAnalyzerTask {

	String outDir = "/home/johannes/gsv/mid2008/";
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.analysis.ProxyAnalyzerTask#analyze(java.util.Collection)
	 */
	@Override
	public void analyze(Collection<ProxyPerson> persons) {
		Set<String> purposes = new HashSet<String>();
		for(ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();
			for(int i = 0; i < plan.getActivities().size(); i++) {
				purposes.add((String) plan.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE));
			}
		}

		for(String purpose : purposes) {
			DescriptiveStatistics stats = statistics(persons, purpose);
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(1000), false);
			try {
				TXTWriter.writeMap(hist, "d", "n", outDir + "d."+purpose+".txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		DescriptiveStatistics stats = statistics(persons, null);
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(1000), false);
		try {
			TXTWriter.writeMap(hist, "d", "n", outDir + "d.all.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private DescriptiveStatistics statistics(Collection<ProxyPerson> persons, String purpose) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for(ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();
			for(ProxyLeg leg : plan.getLegs()) {
				if(purpose == null || ((String)leg.getAttribute(CommonKeys.LEG_PURPOSE)).equalsIgnoreCase(purpose)) {
					Double d = (Double) leg.getAttribute(CommonKeys.LEG_DISTANCE);
					if(d != null) {
						stats.addValue(d);
					}
				}
			}
		}
		
		return stats;
	}
}
