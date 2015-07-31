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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 * 
 */
public class PkmTask extends AnalyzerTask {

	private final String mode;

	public PkmTask(String mode) {
		this.mode = mode;
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
			double pkm = 0;
			for (ProxyPerson person : persons) {
				ProxyPlan plan = person.getPlans().get(0);

				for (int i = 1; i < plan.getLegs().size(); i++) {
					Element leg = plan.getLegs().get(i);
					if (mode == null || mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
						Element act = plan.getActivities().get(i + 1);
						if (purpose == null || purpose.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
							String value = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
							if (value != null) {
								pkm += Double.parseDouble(value);
							}
						}
					}
				}

			}

			if (purpose == null)
				purpose = "all";

			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(pkm);
			results.put(String.format("pkm.route.%s", purpose), stats);

		}

	}

}
