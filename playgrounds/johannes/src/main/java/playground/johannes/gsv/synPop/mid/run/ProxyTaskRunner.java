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
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.synpop.data.Episode;

import java.util.*;

/**
 * @author johannes
 *
 */
public class ProxyTaskRunner {

	public static void run(ProxyPersonTask task, Collection<ProxyPerson> persons) {
		for(ProxyPerson person : persons)
			task.apply(person);
	}
	
	public static void run(ProxyPlanTask task, Collection<ProxyPerson> persons) {
		run(task, persons, false);
	}
	
	public static void run(ProxyPlanTask task, Collection<ProxyPerson> persons, boolean verbose) {
		if(verbose) {
			ProgressLogger.init(persons.size(), 2, 10);
		}
		
		for(ProxyPerson person : persons) {
			for(Episode plan : person.getPlans())
				task.apply(plan);
			
			if(verbose)
				ProgressLogger.step();
		}
		
		if(verbose)
			ProgressLogger.termiante();
	}
	
	public static Set<ProxyPerson> runAndDeletePerson(ProxyPersonTask task, Collection<ProxyPerson> persons) {
		Set<ProxyPerson> newPersons = new HashSet<ProxyPerson>(persons.size());
		
		run(task, persons);
		
		for(ProxyPerson person : persons) {
			if(!"true".equalsIgnoreCase(person.getAttribute(CommonKeys.DELETE))) {
				newPersons.add(person);
			}
		}
		
		return newPersons;
	}
	
	public static void runAndDeletePerson(ProxyPlanTask task, Collection<ProxyPerson> persons) {
		run(task, persons);
		
		for(ProxyPerson person : persons) {
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
