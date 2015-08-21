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

package playground.johannes.synpop.source.mid2008.processing;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.util.*;

/**
 * @author johannes
 *
 */
public class TaskRunner {

	public static void run(PersonTask task, Collection<? extends Person> persons) {
		for(Person person : persons) task.apply(person);
	}
	
	public static void run(EpisodeTask task, Collection<? extends Person> persons) {
		run(task, persons, false);
	}
	
	public static void run(EpisodeTask task, Collection<? extends Person> persons, boolean verbose) {
		if(verbose) {
			ProgressLogger.init(persons.size(), 2, 10);
		}
		
		for(Person person : persons) {
			for(Episode plan : person.getEpisodes())
				task.apply(plan);
			
			if(verbose)
				ProgressLogger.step();
		}
		
		if(verbose)
			ProgressLogger.termiante();
	}
	
	public static <P extends Person> Set<P> runAndDeletePerson(PersonTask task, Collection<P>
			persons) {
		Set<P> newPersons = new HashSet<>(persons.size());
		
		run(task, persons);
		
		for(P person : persons) {
			if(!"true".equalsIgnoreCase(person.getAttribute(CommonKeys.DELETE))) {
				newPersons.add(person);
			}
		}
		
		return newPersons;
	}
	
	public static void runAndDeleteEpisode(EpisodeTask task, Collection<? extends Person> persons) {
		run(task, persons);
		
		for(Person person : persons) {
			List<Episode> remove = new ArrayList<>();
			for(Episode plan : person.getEpisodes()) {
				if("true".equalsIgnoreCase(plan.getAttribute(CommonKeys.DELETE))) {
					remove.add(plan);
				}
			}
			
			for(Episode plan : remove) {
				person.getEpisodes().remove(plan);
			}
			
		}
	}
}
