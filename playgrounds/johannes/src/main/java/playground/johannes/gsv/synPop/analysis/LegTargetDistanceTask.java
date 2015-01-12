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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.sna.math.LinearDiscretizer;

/**
 * @author johannes
 * 
 */
public class LegTargetDistanceTask extends AnalyzerTask {

	private static final Logger logger = Logger.getLogger(LegTargetDistanceTask.class);

	public static final String KEY = "d.target";
	
	private final String modeFilter;
	
	public LegTargetDistanceTask(String mode) {
		this.modeFilter = mode;
	}

	private DescriptiveStatistics statistics(Collection<ProxyPerson> persons, String purpose, String mode) {
		DescriptiveStatistics stats = new DescriptiveStatistics();

		int cntNoVal = 0;

		LinearDiscretizer disc = new LinearDiscretizer(100);
		for (ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();
			for (ProxyObject leg : plan.getLegs()) {
				if (mode == null || mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
					if (purpose == null || purpose.equalsIgnoreCase((String) leg.getAttribute(CommonKeys.LEG_PURPOSE))) {
						String distStr = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
						if (distStr != null) {
							double d = Double.parseDouble(distStr);
							if(d > 100000)
							stats.addValue(d);//disc.discretize(d));
						} else {
							cntNoVal++;
						}
					}
				}
			}
		}

		if (cntNoVal > 0) {
			logger.warn(String.format("No value specified for %s trips of purpose %s.", cntNoVal, purpose));
		}
		return stats;
	}

	@Override
	public void analyze(Collection<ProxyPerson> persons, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for (ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();
			for (int i = 0; i < plan.getActivities().size(); i++) {
				purposes.add((String) plan.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE));
			}
		}

		purposes.add(null);

		for (String purpose : purposes) {
			DescriptiveStatistics stats = statistics(persons, purpose, modeFilter);

			if (purpose == null)
				purpose = "all";

			String key = String.format("%s.%s", KEY, purpose);
			results.put(key, stats);

			if (outputDirectoryNotNull()) {
				try {
					writeHistograms(stats, key, 1000, 50);
					writeHistograms(stats, new LinearDiscretizer(500), key, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
}
