/* *********************************************************************** *
 * project: org.matsim.*
 * StayHome.java
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

/**
 *
 */
package playground.yu.utils;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * judges if a {@code Plan} is a "stay home" {@code Plan}
 *
 * @author yu
 *
 */
public class StayHomePlan {
	public static boolean stayHome(Plan plan) {
		List<PlanElement> pes = plan.getPlanElements();
		int size = pes.size();

		if (size > 3) {
			return false;
		}

		PlanElement firstPe = pes.get(0), lastPe = pes.get(size - 1);
		if (!(firstPe instanceof Activity) || !(lastPe instanceof Activity)) {
			return false;
		}

		String firstType = ((Activity) firstPe).getType(), lastType = ((Activity) lastPe)
				.getType();
		return (firstType.startsWith("h") || firstType.startsWith("H"))
				&& (lastType.startsWith("h") || lastType.startsWith("H"));
	}
}
