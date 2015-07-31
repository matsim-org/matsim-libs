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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class PkmTaskSeason extends AnalyzerTask {

	private final String mode;

	public PkmTaskSeason(String mode) {
		this.mode = mode;
	}

	@Override
	public void analyze(Collection<PlainPerson> persons, Map<String, DescriptiveStatistics> results) {
		Set<String> seasons = new HashSet<String>();
		for (PlainPerson person : persons) {
			String month = (String) person.getAttribute(MIDKeys.PERSON_MONTH);
			if(month != null) {
				String season = "winter";
			
			if(month.equalsIgnoreCase(MIDKeys.APRIL)) {
				season = "summer";
			} else if(month.equalsIgnoreCase(MIDKeys.MAY)) {
				season = "summer";
			} else if(month.equalsIgnoreCase(MIDKeys.JUNE)) {
				season = "summer";
			} else if(month.equalsIgnoreCase(MIDKeys.JULY)) {
				season = "summer";
			} else if(month.equalsIgnoreCase(MIDKeys.AUGUST)) {
				season = "summer";
			} else if(month.equalsIgnoreCase(MIDKeys.SEPTEMBER)) {
				season = "summer";
			} else if(month.equalsIgnoreCase(MIDKeys.OCTOBER)) {
				season = "summer";
			}
			seasons.add(season);
			}
		}

//		purposes.add(null);

		for (String season : seasons) {
			double pkm = 0;
			for (PlainPerson person : persons) {
				String theSeason = person.getAttribute(CommonKeys.ACTIVITY_TYPE);

				Episode plan = person.getPlans().get(0);

				for (int i = 1; i < plan.getLegs().size(); i++) {
					Element leg = plan.getLegs().get(i);
					if (mode == null || mode.equalsIgnoreCase(leg.getAttribute(CommonKeys.LEG_MODE))) {
//						Element act = plan.getActivities().get(i + 1);
						if (season == null || season.equalsIgnoreCase(theSeason)) {
							String value = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
							if (value != null) {
								pkm += Double.parseDouble(value);
							}
						}
					}
				}

			}

			if (season == null)
				season = "all";

			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(pkm);
			results.put(String.format("pkm.route.%s", season), stats);

		}

	}

}
