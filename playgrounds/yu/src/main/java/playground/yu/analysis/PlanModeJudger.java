/* *********************************************************************** *
 * project: org.matsim.*
 * PlanModeJudger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.analysis;

import java.util.Iterator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * judge, which transport mode was taken. This class can only be used with
 * plansfile, in that an agent only can take one transport mode in a day.
 * 
 * @author yu
 * 
 */
public class PlanModeJudger {
	private static boolean useMode(Plan plan, String mode) {
		for (Iterator<PlanElement> li = plan.getPlanElements().iterator(); li
				.hasNext();) {
			Object o = li.next();
			if (o instanceof Leg) {
				Leg l = (Leg) o;
				if (!l.getMode().equals(mode)) {
					return false;
				}
			}
		}
		return true;
	}

	public static String getMode(Plan plan) {
		String tmpMode = null;
		for (Iterator<PlanElement> li = plan.getPlanElements().iterator(); li
				.hasNext();) {
			Object o = li.next();
			if (o instanceof Leg) {
				Leg l = (Leg) o;
				String tmpMode2 = l.getMode();
				if (tmpMode != null) {
					if (!tmpMode.equals(tmpMode2)) {
						return "undefined";
					}
				} else {
					tmpMode = tmpMode2;
				}
			}
		}
		return tmpMode;
	}

	public static boolean useCar(Plan plan) {
		return useMode(plan, TransportMode.car);
	}

	public static boolean usePt(Plan plan) {
		return useMode(plan, TransportMode.pt);
	}

	public static boolean useMiv(Plan plan) {
		return useMode(plan, "miv");
	}

	public static boolean useRide(Plan plan) {
		return useMode(plan, TransportMode.ride);
	}

	public static boolean useMotorbike(Plan plan) {
		return useMode(plan, "motorbike");
	}

	public static boolean useTrain(Plan plan) {
		return useMode(plan, "train");
	}

	public static boolean useBus(Plan plan) {
		return useMode(plan, "bus");
	}

	public static boolean useTram(Plan plan) {
		return useMode(plan, "tram");
	}

	public static boolean useBike(Plan plan) {
		return useMode(plan, TransportMode.bike);
	}

	public static boolean useWalk(Plan plan) {
		return useMode(plan, TransportMode.walk);
	}

	public static boolean useUndefined(Plan plan) {
		return useMode(plan, "undefined");
	}
}
