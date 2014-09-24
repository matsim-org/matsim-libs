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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 *
 */
public class Plans2PersonsTask implements ProxyPersonTask {

	private Set<ProxyPerson> newPersons;
	
	public Plans2PersonsTask() {
		newPersons = new HashSet<ProxyPerson>();
	}
	
	public Set<ProxyPerson> getNewPersons() {
		return newPersons;
	}
	
	@Override
	public void apply(ProxyPerson person) {
		if(person.getPlans().size() > 1) {
			for(int i = 1; i < person.getPlans().size(); i++) {
				ProxyPerson newPerson = new ProxyPerson(String.format("%s.%s", person.getId(), i));
				for(Entry<String, String> entry : person.getAttributes().entrySet()) {
					newPerson.setAttribute(entry.getKey(), entry.getValue());
				}
				
				newPerson.addPlan(person.getPlans().get(i));
				
				newPersons.add(newPerson);
			}
			
			ProxyPlan plan = person.getPlans().get(0);
			person.getPlans().clear();
			person.addPlan(plan);
		}

		
//		for(int i = 1; i < person.getPlans().size(); i++) {
//			person.getPlans().remove(1);
//		}
	}

}
