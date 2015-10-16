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

import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.gsv.synPop.ProxyPlanTaskFactory;
import playground.johannes.socialnetworks.utils.CollectionUtils;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.processing.EpisodeTask;

import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 *
 */
public class ConcurrentProxyTaskRunner {

	public static void run(ProxyPlanTaskFactory factory, Collection<PlainPerson> persons, int numThreads) {
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(persons.size(), numThreads);
		final List<PlainPerson>[] segments = CollectionUtils.split(persons, n);
		/*
		 * create threads
		 */
		ProgressLogger.init(persons.size(), 1, 10);
		Thread[] threads = new Thread[numThreads];
		for(int i = 0; i < numThreads; i++) {
			final EpisodeTask task = factory.getInstance();
			final List<PlainPerson> subPersons = segments[i];
			threads[i] = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for(PlainPerson p : subPersons) {
						for(Episode plan : p.getEpisodes())
							task.apply(plan);
						
						ProgressLogger.step();
					}
					
				}
			});
			
			threads[i].start();
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < numThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ProgressLogger.termiante();
	}
}
