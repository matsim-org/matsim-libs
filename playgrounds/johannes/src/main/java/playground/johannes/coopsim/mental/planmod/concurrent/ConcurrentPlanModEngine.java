/* *********************************************************************** *
 * project: org.matsim.*
 * ConcurrentPlanModEngine.java
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
package playground.johannes.coopsim.mental.planmod.concurrent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.util.MultiThreading;

import playground.johannes.coopsim.mental.planmod.Choice2ModAdaptorFactory;
import playground.johannes.coopsim.mental.planmod.PlanModEngine;

/**
 * @author illenberger
 * 
 */
public class ConcurrentPlanModEngine implements PlanModEngine {

	private final PlanModExecutor executor;

	public ConcurrentPlanModEngine(Choice2ModAdaptorFactory factory) {
		int threads = MultiThreading.getNumAllowedThreads();
		executor = new PlanModExecutor(threads, threads, Integer.MAX_VALUE, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), new PlanModThreadFactory(factory));
	}

	@Override
	public void run(List<Plan> plans, Map<String, Object> choices) {
		/*
		 * create and submit tasks
		 */
		PlanModFutureTask[] tasks = new PlanModFutureTask[plans.size()];
		for (int i = 0; i < plans.size(); i++) {
			PlanModRunnable r = new PlanModRunnable(choices, plans.get(i));
			PlanModFutureTask t = new PlanModFutureTask(r);
			executor.execute(t);
			tasks[i] = t;
		}
		/*
		 * wait for completion
		 */
		for (int i = 0; i < tasks.length; i++) {
			try {
				tasks[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

}
