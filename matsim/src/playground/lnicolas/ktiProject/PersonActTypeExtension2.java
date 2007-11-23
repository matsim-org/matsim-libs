/* *********************************************************************** *
 * project: org.matsim.*
 * PersonActTypeExtension2.java
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
import java.util.TreeSet;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;

public class PersonActTypeExtension2 extends PersonAlgorithm {

	private final String workActType = "w";
	private final String shopActType = "s";
	private final String eduActType = "e";
	private final String homeActType = "h";
	private final String leisureActType = "l";
	
	private final int homeActType1Duration = 1;
	private final int workActTypeDuration = 8;
	private final int eduActTypeDuration = 6;
	private final int homeActTypeDuration = 10;
	
//	private final int minHomeActTypeDuration = 6;
//	private final int maxHomeActTypeDuration = 12;
	
	private final int minActDuration = 1;
	
	TreeSet<String> activityTypes = new TreeSet<String>();
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	private int tooLongActChains = 0;
	private int plansCount = 0;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonActTypeExtension2() {
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Person person) {
		int remainingActTime = 24 - homeActTypeDuration; // hours
		
		checkStartEndHome(person);
		
		int count = processHomeAct(person);
		remainingActTime -= count * homeActType1Duration;
		
		count = processWorkOrEduAct(person, workActType, workActTypeDuration);
		TreeSet<String> remainingActTypes = new TreeSet<String>();
		remainingActTypes.add(shopActType);
		remainingActTypes.add(leisureActType);
		if (count > 0) {
			remainingActTime -= workActTypeDuration;
			remainingActTypes.add(eduActType);
		} else {
			count = processWorkOrEduAct(person, eduActType, eduActTypeDuration);
			if (count > 0) {
				remainingActTime -= eduActTypeDuration;
			}
		}
		distributeActTypes(person, remainingActTypes, remainingActTime);
		
		plansCount += person.getPlans().size();
	}
	
	private void distributeActTypes(Person person,
			TreeSet<String> actTypes, int totalActDuration) {
		ArrayList<BasicAct> acts = new ArrayList<BasicAct>();
		for (Plan plan : person.getPlans()) {
			BasicPlan.ActIterator it = plan.getIteratorAct();
			while (it.hasNext()) {
				BasicAct act = it.next();
				if (actTypes.contains(act.getType())) {
					acts.add(act);
				}
			}
		}
		if (acts.size() == 0) {
			return;
		}
		int[] actDurations = new int[acts.size()];
		int actDuration = 0;
		for (int i = 0; i < actDurations.length; i++) {
			actDurations[i] = minActDuration;
			actDuration += minActDuration;
		}
		while (actDuration < totalActDuration) {
			int r = Gbl.random.nextInt(actDurations.length);
			actDurations[r]++;
			actDuration++;
		}
		for (int i = 0; i < actDurations.length; i++) {
			String newType = acts.get(i).getType() + actDurations[i];
			acts.get(i).setType(newType);
			activityTypes.add(newType);
		}
		if (actDuration > totalActDuration) {
			for (Plan plan : person.getPlans()) {
				String s = "";
				Plan.ActIterator it = plan.getIteratorAct();
				while (it.hasNext()) {
					s += it.next().getType();
				}
				System.out.println("Person's " + person.getId() + " plan " + s +
						" exceeds the total possible act chain duration of "
						+ totalActDuration + " hours.");
				this.tooLongActChains++;
			}
		}
	}

	private int processWorkOrEduAct(Person person, String actType, int totalActDuration) {
		ArrayList<BasicAct> acts = new ArrayList<BasicAct>();
		for (Plan plan : person.getPlans()) {
			BasicPlan.ActIterator it = plan.getIteratorAct();
			while (it.hasNext()) {
				BasicAct act = it.next();
				if (actType.equals(act.getType())) {
					acts.add(act);
				}
			}
		}
		if (acts.size() == 0) {
			return 0;
		}
		int[] actDurations = new int[acts.size()];
		int actDuration = actDurations.length;
		for (int i = 0; i < actDurations.length; i++) {
			actDurations[i] = 1;
		}
		int i = 0;
		while (actDuration < totalActDuration) {
			actDurations[i]++;
			i++;
			if (i == actDurations.length) {
				i = 0;
			}
			actDuration++;
		}
		for (i = 0; i < actDurations.length; i++) {
			String newType = acts.get(i).getType() + actDurations[i];
			acts.get(i).setType(newType);
			activityTypes.add(newType);
		}
		if (acts.size() > totalActDuration) {
			for (Plan plan : person.getPlans()) {
				String s = "";
				Plan.ActIterator it = plan.getIteratorAct();
				while (it.hasNext()) {
					s += it.next().getType();
				}
				System.out.println("Person's " + person.getId() + " plan " + s +
						" contains too many " + actType + 
						" acts to distribute them over a duration of "
						+ totalActDuration + " hours!");
				tooLongActChains++;
			}
		}
		return acts.size();
	}

	private void checkStartEndHome(Person person) {
		for (Plan plan : person.getPlans()) {
			BasicAct startAct = (BasicAct) plan.getActsLegs().get(0);
			BasicAct endAct = (BasicAct) plan.getActsLegs().get(
					plan.getActsLegs().size() - 1);
			if (startAct.getType().equals(homeActType) == false
					|| endAct.getType().equals(homeActType) == false) {
				String s = "";
				Plan.ActIterator it = plan.getIteratorAct();
				while (it.hasNext()) {
					s += it.next().getType();
				}
				Gbl.errorMsg("Person's " + person.getId() + " plan " + s + " does not begin" +
						" or end with a home act. This is invalid.");
			}
		}
	}

	/**
	 * Converts the home acts "h" that are in-between (i.e. that are not at the end or 
	 * at the beginning of an activity chain) to homeActType1 "h1".
	 * @param person
	 * @return The number of act whose type was converted.
	 */
	private int processHomeAct(Person person) {
		int i = 0;
		int c = 0;
		String newActType = homeActType + homeActType1Duration;
		for (Plan plan : person.getPlans()) {
			BasicPlan.ActIterator it = plan.getIteratorAct();
			while (it.hasNext()) {
				BasicAct act = it.next();
				if (act.getType().equals(homeActType)
						&& i > 0
						&& it.hasNext()) {
					act.setType(newActType);
					c++;
				}
				i++;
			}
		}
		if (c > 0) {
			activityTypes.add(newActType);
		}
		
		return c;
	}

	private int getTypeOccurenceCount(Plan plan, String actType) {
		int c = 0;
		BasicPlan.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			BasicAct act = it.next();
			if (act.getType().equals(actType)) {
				c++;
			}
		}
		return c;
	}

	public void printInformation() {
		String out = "";
		for (String s : activityTypes) {
			out += s + " ";
		}
		System.out.println(out);
		
		System.out.println("Number of act chains which exceed 24 hours: "
				+ tooLongActChains + ". Total plans: " 
				+ plansCount);
	}
}
