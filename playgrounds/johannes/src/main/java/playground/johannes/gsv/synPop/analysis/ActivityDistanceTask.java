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
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class ActivityDistanceTask extends AnalyzerTask {

	public final static String KEY = "d.act";

	private final ActivityFacilities facilities;

	private final DistanceCalculator calc = CartesianDistanceCalculator.getInstance();

	private final String mode;
	
	public ActivityDistanceTask(ActivityFacilities facilities, String mode) {
		this.facilities = facilities;
		this.mode = mode;
	}

	protected DescriptiveStatistics statistics(Collection<ProxyPerson> persons, String purpose, String mode) {
		DescriptiveStatistics stats = new DescriptiveStatistics();

		for (ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();

			for (int i = 1; i < plan.getActivities().size(); i++) {

				ProxyObject thisAct = plan.getActivities().get(i);
				ProxyObject leg = plan.getLegs().get(i - 1);

				if (mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
					if (purpose == null || purpose.equalsIgnoreCase(thisAct.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
						ProxyObject prevAct = plan.getActivities().get(i - 1);
						Id<ActivityFacility> prevId = Id.create(prevAct.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
						ActivityFacility prevFac = facilities.getFacilities().get(prevId);

						Id<ActivityFacility> thisId = Id.create(thisAct.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
						ActivityFacility thisFac = facilities.getFacilities().get(thisId);

						Point p1 = MatsimCoordUtils.coordToPoint(prevFac.getCoord());
						Point p2 = MatsimCoordUtils.coordToPoint(thisFac.getCoord());

						double d = calc.distance(p1, p2);
						stats.addValue(d);
					}
				}
			}
		}

		return stats;
	}

	@Override
	public void analyze(Collection<ProxyPerson> persons, Map<String, DescriptiveStatistics> results) {
		Set<String> types = new HashSet<String>();
		for (ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();
			for (ProxyObject act : plan.getActivities()) {
				types.add(act.getAttribute(CommonKeys.ACTIVITY_TYPE));
			}
		}

		types.add(null);

		for (String type : types) {
			DescriptiveStatistics stats = statistics(persons, type, mode);

			if (type == null)
				type = "all";

			String key = getKey(type);
			results.put(key, stats);

			if (outputDirectoryNotNull()) {
				try {
					writeHistograms(stats, key, 1000, 100);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected String getKey(String type) {
		return String.format("%s.%s", KEY, type);
	}
}
