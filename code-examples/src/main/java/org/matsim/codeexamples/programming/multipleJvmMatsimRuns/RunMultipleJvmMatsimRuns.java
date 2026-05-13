/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
/**
 * 
 */
package org.matsim.codeexamples.programming.multipleJvmMatsimRuns;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Demonstrating how one can run multiple MATSim runs in paralerll, each within
 * their own Java Virtual Machine (JVM).
 * 
 * @author jwjoubert
 */
public class RunMultipleJvmMatsimRuns {
	final private static Logger LOG = LogManager.getLogger(RunMultipleJvmMatsimRuns.class);
	final private static int NUMBER_OF_THREADS = 4;
	final private static int NUMBER_OF_RUNS = 4;

	/**
	 * To run this you need two essential components:
	 * <ul>
	 * 		<li> a free-standing package of this repository. This is achieved 
	 * 			 by running '<code>mvn package -DskipTests=true</code>' on this 
	 * 			 repository, either on the command line/terminal, or from within
	 * 			 your Integrated Development Environment (IDE). Once done, the package
	 * 			 should appear in the <code>target/</code> folder as a file with 
	 * 			 a suffix '<code>*-jar-with-dependencies.jar</code>'.
	 * 		<li> 
	 * </ul>
	 * @param args
	 */
	public static void main(String[] args) {
		LOG.info("Running multiple MATSim runs in different JVMs...");
		File packageFile = new File("./target/matsim-code-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
		if(!packageFile.exists()) {
			LOG.error("No package found");
			throw new RuntimeException("Need to run 'mvn package -DskipTests=true' first.");
		} else {
			LOG.info("Cool, found your package file.");
		}
		
		
		ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		Map<Integer, Future<List<Double>>> jobs = new TreeMap<>();
		
		for(int run = 0; run < NUMBER_OF_RUNS; run++) {
			String foldername = String.format("./output/run_%03d/", run);
			MultipleJvmMatsimCallable mjm = new MultipleJvmMatsimCallable(foldername);
			Future<List<Double>> job = executor.submit(mjm);
			jobs.put(run, job);
		}
		/* Shutdown the multiple runs and wait for it to finish */
		executor.shutdown();
		while(!executor.isTerminated()) {
		}
		
		/* Report the scores for each individual in each run. */
		for(Integer run : jobs.keySet()) {
			List<Double> list = null;
			try {
				list = jobs.get(run).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot retrieve the multi-threaded job for run '" + run + "'");
			}
			LOG.info("Run '" + run + "':");
			for(Double d : list) {
				LOG.info("   -> " + String.valueOf(d));
			}
		}
		
		LOG.info("Done running multiple MATSim runs.");
	}

}
