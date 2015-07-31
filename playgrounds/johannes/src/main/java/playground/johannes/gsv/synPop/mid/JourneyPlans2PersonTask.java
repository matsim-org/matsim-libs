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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.synpop.data.Episode;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class JourneyPlans2PersonTask implements ProxyPersonTask {

	private Set<ProxyPerson> newPersons = new HashSet<>();

	private final double periode = 90;
	
	public Set<ProxyPerson> getPersons() {
		return newPersons;
	}
	
	@Override
	public void apply(ProxyPerson person) {
		int counter = 0;
		double w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));

		Set<Episode> journeyPlans = new HashSet<>();

		for (Episode plan : person.getPlans()) {
			if ("midjourneys".equalsIgnoreCase(plan.getAttribute("datasource"))) {
				ProxyPerson newPerson = new ProxyPerson(String.format("%s.%s", person.getId(), counter++));
				for (Entry<String, String> entry : person.getAttributes().entrySet()) {
					newPerson.setAttribute(entry.getKey(), entry.getValue());
				}

				newPerson.addPlan(plan);

				double newW = w * 1 / periode;
				newPerson.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(newW));

				newPersons.add(newPerson);

				journeyPlans.add(plan);
			}
		}

		for (Episode plan : journeyPlans) {
			person.getPlans().remove(plan);
		}

//		/*
//		 * adjust the weight of the original person
//		 */
//		double newW = w * 1 / 365.0;
//		person.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(newW));
//		/*
//		 * add one person with an empty plan
//		 */
//		ProxyPerson newPerson = new ProxyPerson(String.format("%s.%s", person.getId(), counter++));
//		for (Entry<String, String> entry : person.getAttributes().entrySet()) {
//			newPerson.setAttribute(entry.getKey(), entry.getValue());
//		}
//		newPerson.addPlan(new ProxyPlan());
//
//		newW = w * (365 - counter) / 365.0;
//		newPerson.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(newW));
//
//		newPersons.add(newPerson);
	}

}
