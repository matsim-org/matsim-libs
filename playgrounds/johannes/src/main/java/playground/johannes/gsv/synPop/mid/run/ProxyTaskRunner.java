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

package playground.johannes.gsv.synPop.mid.run;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.util.*;

/**
 * @author johannes
 *
 */
public class ProxyTaskRunner {

	public static void run(ProxyPersonTask task, Collection<PlainPerson> persons) {
		for(PlainPerson person : persons)
			task.apply(person);
	}
	
	public static void run(ProxyPlanTask task, Collection<PlainPerson> persons) {
		run(task, persons, false);
	}
	
	public static void run(ProxyPlanTask task, Collection<PlainPerson> persons, boolean verbose) {
		if(verbose) {
			ProgressLogger.init(persons.size(), 2, 10);
		}
		
		for(PlainPerson person : persons) {
			for(Episode plan : person.getPlans())
				task.apply(plan);
			
			if(verbose)
				ProgressLogger.step();
		}
		
		if(verbose)
			ProgressLogger.termiante();
	}
	
	public static Set<PlainPerson> runAndDeletePerson(ProxyPersonTask task, Collection<PlainPerson> persons) {
		Set<PlainPerson> newPersons = new HashSet<PlainPerson>(persons.size());
		
		run(task, persons);
		
		for(PlainPerson person : persons) {
			if(!"true".equalsIgnoreCase(person.getAttribute(CommonKeys.DELETE))) {
				newPersons.add(person);
			}
		}
		
		return newPersons;
	}
	
	public static void runAndDeletePerson(ProxyPlanTask task, Collection<PlainPerson> persons) {
		run(task, persons);
		
		for(PlainPerson person : persons) {
			List<Episode> remove = new ArrayList<>();
			for(Episode plan : person.getPlans()) {
				if("true".equalsIgnoreCase(plan.getAttribute(CommonKeys.DELETE))) {
					remove.add(plan);
				}
			}
			
			for(Episode plan : remove) {
				person.getPlans().remove(plan);
			}
			
		}
	}
}
