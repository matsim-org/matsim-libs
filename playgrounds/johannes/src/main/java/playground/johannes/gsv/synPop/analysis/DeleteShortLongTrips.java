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

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 * 
 */
public class DeleteShortLongTrips implements ProxyPlanTask {

	private final double threshold;

	private final boolean shortTrips;

	public DeleteShortLongTrips(double threshold, boolean shortTrips) {
		this.threshold = threshold;
		this.shortTrips = shortTrips;
	}

	@Override
	public void apply(Episode plan) {
		for (int i = 0; i < plan.getLegs().size(); i++) {
			Attributable leg = plan.getLegs().get(i);
			String value = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
			if (value != null) {
				double dist = Double.parseDouble(value);
				if (shortTrips) {
					if (dist < threshold) {
						leg.setAttribute(CommonKeys.DELETE, "true");
					}
				} else {
					if (dist > threshold) {
						leg.setAttribute(CommonKeys.DELETE, "true");
					}
				}
			}
		}

		for (int i = 0; i < plan.getLegs().size(); i++) {
			Attributable leg = plan.getLegs().get(i);
			if ("true".equalsIgnoreCase(leg.getAttribute(CommonKeys.DELETE))) {
				Attributable prev = plan.getActivities().get(i);
				Attributable next = plan.getActivities().get(i + 1);

				if (ActivityType.HOME.equalsIgnoreCase(prev.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
					if (plan.getActivities().size() > i + 2) {
						Attributable act = plan.getActivities().get(i + 2);
						if (ActivityType.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
							next.setAttribute(CommonKeys.DELETE, "true");
						}
					}
				} else if (ActivityType.HOME.equalsIgnoreCase(next.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
					if (i - 1 >= 0) {
						Attributable act = plan.getActivities().get(i - 1);
						if (ActivityType.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
							prev.setAttribute(CommonKeys.DELETE, "true");
						}
					}
				}
			}
		}

		/*
		 * if there is only one leg
		 */
		if(plan.getLegs().size() == 1) {
			Attributable leg = plan.getLegs().get(0);
			if ("true".equalsIgnoreCase(leg.getAttribute(CommonKeys.DELETE))) {
				plan.getActivities().clear();
				plan.getLegs().clear();
			}
		} else {
		
		boolean flag = true;
		while (flag) {
			for (int i = 0; i < plan.getActivities().size(); i++) {
				Attributable act = plan.getActivities().get(i);
				if ("true".equalsIgnoreCase(act.getAttribute(CommonKeys.DELETE))) {
					flag = true;

					Attributable nextAct = plan.getActivities().get(i + 1);
					Attributable thisAct = plan.getActivities().get(i - 1);

					plan.getActivities().remove(i);
					plan.getActivities().remove(i);
					plan.getLegs().remove(i - 1);
					plan.getLegs().remove(i - 1);

					thisAct.setAttribute(CommonKeys.ACTIVITY_END_TIME, nextAct.getAttribute(CommonKeys.ACTIVITY_END_TIME));

					break;
				} else {
					flag = false;
				}
			}
		}
		}
	}

	private boolean isReturnLeg(Episode plan, int current, int candidate) {
		Attributable prevAct = plan.getActivities().get(current);
		Attributable nextAct = plan.getActivities().get(current + 1);

		if (candidate > 0 && candidate < plan.getLegs().size()) {
			Attributable prevAct2 = plan.getActivities().get(candidate);
			Attributable nextAct2 = plan.getActivities().get(candidate + 1);

			if (prevAct.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase(ActivityType.HOME)
					&& nextAct2.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase(ActivityType.HOME)) {
				return true;
			} else if (nextAct.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase(ActivityType.HOME)
					&& prevAct2.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase(ActivityType.HOME)) {
				return true;
			} else {
				return false;
			}

			// String prevId =
			// prevAct.getAttribute(CommonKeys.ACTIVITY_FACILITY);
			// String nextId =
			// nextAct.getAttribute(CommonKeys.ACTIVITY_FACILITY);
			//
			// String prevId2 =
			// prevAct2.getAttribute(CommonKeys.ACTIVITY_FACILITY);
			// String nextId2 =
			// nextAct2.getAttribute(CommonKeys.ACTIVITY_FACILITY);
			//
			// if(prevId.equalsIgnoreCase(nextId2) &&
			// nextId.equalsIgnoreCase(prevId2)) {
			// return true;
			// }
		}

		return false;
	}

	public static void main(String args[]) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);

		parser.parse("/home/johannes/gsv/mid2008/pop/hesen.car.wo3km.midjourneys.xml");

		DeleteShortLongTrips task = new DeleteShortLongTrips(100000, false);
		for (PlainPerson person : parser.getPersons()) {
			task.apply(person.getEpisodes().get(0));
		}

		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/mid2008/pop/hesen.car.3-300km.xml", parser.getPersons());

	}
}
