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
package playground.yu.demandModifications;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 *
 *
 * @author yu
 *
 */
public class StayHomePlan {
	/**
	 * judges if a {@code Plan} is a "stay home" {@code Plan}
	 *
	 * @param plan
	 * @return
	 */
	public static boolean isAStayHomePlan(Plan plan) {
		/*
		 * List<PlanElement> pes = plan.getPlanElements(); int size =
		 * pes.size();
		 *
		 * if (size != 3) { return false; }
		 *
		 * PlanElement firstPe = pes.get(0), lastPe = pes.get(size - 1); if
		 * (!(firstPe instanceof Activity) || !(lastPe instanceof Activity)) {
		 * return false; }
		 *
		 * String firstType = ((Activity) firstPe).getType(), lastType =
		 * ((Activity) lastPe) .getType(); if (firstType.equals(lastType)) {
		 * PlanElement pe = pes.get(1); if (!(pe instanceof Leg)) { return
		 * false; } Leg leg = (Leg) pe; if (!leg.getMode().equals("walk")) {
		 * return false; } Route route = leg.getRoute(); return route instanceof
		 * GenericRoute;
		 *
		 * }
		 *
		 * return false;
		 */
		String type = ((PlanImpl) plan).getType();
		if (type == null) {
			return false;
		}
		return type.equals(PlanImpl.DeprecatedConstants.WALK);
	}

	/**
	 * counts how many "stay home" {@code Plan}s has each {@code Person} in a
	 * {@code Population}.
	 *
	 * @param population
	 */
	public static void countInPopulation(Population population) {
		for (Person person : population.getPersons().values()) {
			int cnt = 0;
			for (Plan plan : person.getPlans()) {
				if (isAStayHomePlan(plan)) {
					cnt++;
				}
			}
			if (cnt > 1) {
				System.out.println("Person (" + person.getId() + ") has\t"
						+ cnt + "\t\"stay home\" Plans.");
			} else if (cnt == 0) {
				System.out.println("Person (" + person.getId()
						+ ") has NOT \"stay home\" Plans.");
			}
		}
	}

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils
				.loadConfig(args[0]));
		StayHomePlan.countInPopulation(scenario.getPopulation());
	}
}
