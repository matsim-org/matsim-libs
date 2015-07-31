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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainEpisode;
import playground.johannes.synpop.data.PlainPerson;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class Plans2PersonsTask implements ProxyPersonTask {

	private Set<PlainPerson> newPersons;

	public Plans2PersonsTask() {
		newPersons = new HashSet<PlainPerson>();
	}

	public Set<PlainPerson> getNewPersons() {
		return newPersons;
	}

	@Override
	public void apply(PlainPerson person) {
		int counter = 0;
		double w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));

		if (person.getPlans().size() > 1) {
			for (int i = 1; i < person.getPlans().size(); i++) {
				PlainPerson newPerson = new PlainPerson(String.format("%s.%s", person.getId(), counter++));
				for (Entry<String, String> entry : person.getAttributes().entrySet()) {
					newPerson.setAttribute(entry.getKey(), entry.getValue());
				}

				newPerson.addPlan(person.getPlans().get(i));

				double newW = w * 1 / 365.0;
				newPerson.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(newW));

				newPersons.add(newPerson);
			}

			Episode plan = person.getPlans().get(0);
			person.getPlans().clear();
			person.addPlan(plan);
		}
		/*
		 * adjust the weight of the original person
		 */
		double newW = w * 1 / 365.0;
		person.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(newW));
		/*
		 * add one person with an empty plan
		 */
		PlainPerson newPerson = new PlainPerson(String.format("%s.%s", person.getId(), counter++));
		for (Entry<String, String> entry : person.getAttributes().entrySet()) {
			newPerson.setAttribute(entry.getKey(), entry.getValue());
		}
		newPerson.addPlan(new PlainEpisode());

		newW = w * (365 - counter) / 365.0;
		newPerson.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(newW));

		newPersons.add(newPerson);
	}

}
