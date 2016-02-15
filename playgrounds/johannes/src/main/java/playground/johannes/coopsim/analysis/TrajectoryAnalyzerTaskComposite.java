/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryAnalyzerTaskComposite.java
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
package playground.johannes.coopsim.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetgen.sna.util.MultiThreading;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author illenberger
 *
 */
public class TrajectoryAnalyzerTaskComposite extends TrajectoryAnalyzerTask {

	private static final Logger logger = Logger.getLogger(TrajectoryAnalyzerTaskComposite.class);
	
	private List<TrajectoryAnalyzerTask> tasks;
	
	private int numThreads = MultiThreading.getNumAllowedThreads();
	
	public TrajectoryAnalyzerTaskComposite() {
		tasks = new LinkedList<TrajectoryAnalyzerTask>();
	}
	
	public void addTask(TrajectoryAnalyzerTask task) {
		tasks.add(task);
	}
	
	public void setOutputDirectory(String output) {
		for(TrajectoryAnalyzerTask task : tasks) {
			task.setOutputDirectory(output);
		}
	}
	
	public void setNumThreads(int n) {
		this.numThreads = n;
	}
	
	@Override
	public void analyze(final Set<Trajectory> trajectories, final Map<String, DescriptiveStatistics> results) {
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		List<Future<?>> futures = new ArrayList<>(tasks.size());
		
		for(TrajectoryAnalyzerTask task : tasks) {
			final TrajectoryAnalyzerTask task2 = task;
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					logger.debug(String.format("Running task %1$s...", task2.getClass().getSimpleName()));
					task2.analyze(trajectories, results);
				}
			};
			
			futures.add(executor.submit(runnable));
		}

		for(Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
	}

}
