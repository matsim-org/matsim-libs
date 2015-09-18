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

package playground.johannes.gsv.matrices.plans2matrix;

import playground.johannes.synpop.data.*;

/**
 * @author johannes
 * 
 */
public class WeCommuterPredicate implements Predicate {

	private final double threshold;

	public WeCommuterPredicate(double threshold) {
		this.threshold = threshold;
	}
	
	@Override
	public boolean test(PlainPerson person, Attributable leg, Attributable prev, Attributable next) {
		boolean result = false;

		String day = person.getAttribute(CommonKeys.DAY);
		if (day != null) {
			if (day.equalsIgnoreCase(CommonValues.THURSDAY) || day.equalsIgnoreCase(CommonValues.FRIDAY) || day.equalsIgnoreCase(CommonValues.SATURDAY)
					|| day.equalsIgnoreCase(CommonValues.SUNDAY)) {
				
				Episode plan = person.getEpisodes().get(0);
				Attributable weLeg = null;
				
				int cnt = 0;
				for (int i = 0; i < plan.getLegs().size(); i++) {
					Attributable leg2 = plan.getLegs().get(i);
					Attributable prev2 = plan.getActivities().get(i);
					Attributable next2 = plan.getActivities().get(i + 1);

					String val = leg2.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
					if (val != null) {
						double d = Double.parseDouble(val);
						if (d > threshold) {
							if (ActivityTypes.WORK.equalsIgnoreCase(prev2.getAttribute(CommonKeys.ACTIVITY_TYPE))
									|| ActivityTypes.WORK.equalsIgnoreCase(next2.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
								cnt++;
								weLeg = leg2;
							}
						}
					}
				}

				if (cnt == 1) {
					if(weLeg == leg) {
						return true;
					}
				}

			}
		}

		return result;
	}

}
