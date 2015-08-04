/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class LegDistanceTask extends AnalyzerTask {
	
	private static final Logger logger = Logger.getLogger(LegDistanceTask.class);

	private final String attKey;
	
	private final String mode;
	
	public LegDistanceTask(String key, String mode) {
		this.attKey = key;
		this.mode = mode;
	}
	
	protected DescriptiveStatistics statistics(Collection<PlainPerson> persons, String purpose, String mode) {
		DescriptiveStatistics stats = new DescriptiveStatistics();

		int cntNoVal = 0;

		for (PlainPerson person : persons) {
			Episode plan = person.getPlan();

			for(int i = 0; i < plan.getLegs().size(); i++) {
				Attributable leg = plan.getLegs().get(i);
				Attributable act = plan.getActivities().get(i + 1);
			
				if (mode == null || mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
					
					if (purpose == null || purpose.equalsIgnoreCase((String) act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
						
						String distStr = leg.getAttribute(attKey);
						if (distStr != null) {
							double d = Double.parseDouble(distStr);
							stats.addValue(d);
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

	protected String getKey(String purpose, String attKey) {
		String key = String.format("%s.%s", "d", purpose);
		if(attKey.equalsIgnoreCase(CommonKeys.LEG_GEO_DISTANCE)) {
			key = String.format("%s.%s", "d.geo", purpose);
		} else if(attKey.equalsIgnoreCase(CommonKeys.LEG_ROUTE_DISTANCE)){
			key = String.format("%s.%s", "d.route", purpose);
		}
		
		return key;
	}
	
	@Override
	public void analyze(Collection<PlainPerson> persons, Map<String, DescriptiveStatistics> results) {
		Set<String> purposes = new HashSet<String>();
		for (PlainPerson person : persons) {
			Episode plan = person.getPlan();
			for (int i = 0; i < plan.getActivities().size(); i++) {
				purposes.add((String) plan.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE));
			}
		}

		purposes.add(null);

		for (String purpose : purposes) {
			DescriptiveStatistics stats = statistics(persons, purpose, mode);

			if (purpose == null)
				purpose = "all";

			String key = getKey(purpose, attKey);
			
			results.put(key, stats);

			if (outputDirectoryNotNull()) {
				try {
					writeHistograms(stats, key, 1000, 50);
					writeHistograms(stats, new LinearDiscretizer(25000), key, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

}
