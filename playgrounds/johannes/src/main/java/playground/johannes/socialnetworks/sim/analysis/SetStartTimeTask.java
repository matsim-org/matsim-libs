/* *********************************************************************** *
 * project: org.matsim.*
 * SetStartTimeTask.java
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

import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class SetStartTimeTask extends PlansAnalyzerTask {

	@Override
	public void analyze(Set<Plan> plans, Map<String, DescriptiveStatistics> results) {
		for(Plan plan : plans) {
			for(int idx = 0; idx < plan.getPlanElements().size(); idx += 2) {
				Activity act = (Activity) plan.getPlanElements().get(idx);
				if(idx == 0)
					act.setStartTime(0);
				else {
					Activity prev = (Activity) plan.getPlanElements().get(idx - 2);
					Leg leg = (Leg) plan.getPlanElements().get(idx - 1);
					act.setStartTime(prev.getEndTime() + leg.getTravelTime());
				}
			}
		}

	}

}
