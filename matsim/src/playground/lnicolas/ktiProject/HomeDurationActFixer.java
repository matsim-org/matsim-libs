/* *********************************************************************** *
 * project: org.matsim.*
 * HomeDurationActFixer.java
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

package playground.lnicolas.ktiProject;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;

public class HomeDurationActFixer extends PersonAlgorithm {

	TreeMap<String, ArrayList<Plan>> refPlans;
	
	public HomeDurationActFixer(Plans referencePopulation) {
		super();
		
		refPlans = new TreeMap<String, ArrayList<Plan>>();
		
		for (Person person : referencePopulation.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				String s = "";
				Plan.ActIterator it = plan.getIteratorAct();
				while (it.hasNext()) {
					s += it.next().getType();
				}
				if (refPlans.containsKey(s) == false) {
					refPlans.put(s, new ArrayList<Plan>());
				}
				refPlans.get(s).add(plan);
			}
		}
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			if (planContainsType(plan, "h")) {
				Plan refPlan = getRefPlan(plan);
				Plan.ActIterator it1 = plan.getIteratorAct();
				Plan.ActIterator it2 = refPlan.getIteratorAct();
				while (it1.hasNext()) {
					Act a1 = (Act) it1.next();
					Act a2 = (Act) it2.next();
					if (a1.getType().equals("h")) {
						a1.setDur(a2.getDur());
						a1.setEndTime(a2.getEndTime());
						a1.setStartTime(a2.getStartTime());
//						System.out.println(a2.getEndTime());
					}
				}
			}
		}
	}
	
	private boolean planContainsType(Plan plan, String actType) {
		BasicPlan.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			BasicAct act = it.next();
			if (act.getType().equals(actType)) {
				return true;
			}
		}
		return false;
	}

	private Plan getRefPlan(Plan plan) {
		String s = "";
		Plan.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			s += it.next().getType();
		}
		ArrayList<Plan> plans = refPlans.get(s);
		if (plans == null) {
			for (String k : refPlans.keySet()) {
				if (k.equals(s)) {
					System.out.println(s + " == " + k + "!!!");
				} else {
					System.out.println(s + " != " + k);
				}
			}
			Gbl.errorMsg("No reference plans of actleg size "
					+ plan.getActsLegs().size() + ": " + s);
		}
		for (Plan p : plans) {
			Plan.ActIterator it1 = plan.getIteratorAct();
			Plan.ActIterator it2 = p.getIteratorAct();
			boolean ok = true;
			while (it1.hasNext()) {
				Act a1 = (Act) it1.next();
				Act a2 = (Act) it2.next();
				if (a1.getType().equals("h") == false) {
					if (a1.getType().equals(a2.getType()) == false
							|| a1.getDur() != a2.getDur()) {
						ok = false;
						break;
					}
				}
				if (ok == true) {
					return p;
				}
			}
		}
		
		return null;
	}
}
