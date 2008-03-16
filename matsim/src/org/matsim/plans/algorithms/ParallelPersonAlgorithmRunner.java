/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPersonAlgorithmRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.plans.algorithms;

import java.util.LinkedList;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Counter;

/**
 * An abstract/static helper class for running {@link PersonAlgorithm}s in parallel using threads.
 *
 * @author mrieser
 */
public abstract class ParallelPersonAlgorithmRunner {

	public interface PersonAlgorithmProvider {
		public PersonAlgorithm getPersonAlgorithm();
	}

	/**
	 * Handles each person of the given <code>population</code> with the specified <code>algorithm</code>,
	 * using up to <code>numberOfThreads</code> threads to speed things up. Use this method only if the given
	 * algorithm is thread-safe! Otherwise, use {@link #run(Plans, int, PersonAlgorithmProvider)}.
	 * 
	 * @param population
	 * @param numberOfThreads
	 * @param algorithm
	 */
	public static void run(final Plans population, final int numberOfThreads, final PersonAlgorithm algorithm) {
		run(population, numberOfThreads, new PersonAlgorithmProvider() {
			public PersonAlgorithm getPersonAlgorithm() {
				return algorithm;
			}
		});
	}
	
	/**
	 * Handles each person of the given <code>population</code> with a PersonAlgorithm provided by <code>algoProvider</code>,
	 * using up to <code>numberOfThreads</code> threads to speed things up. This method will request a new instance of the
	 * PersonAlgorithm for each thread it allocates, thus enabling the parallel use of non-thread-safe algorithms.
	 * For thread-safe algorithms, {@link #run(Plans, int, PersonAlgorithm)} may be an easier method to use.
	 * 
	 * @param population
	 * @param numberOfThreads
	 * @param algoProvider
	 */
	public static void run(final Plans population, final int numberOfThreads, final PersonAlgorithmProvider algoProvider) {
		int numOfThreads = Math.max(numberOfThreads, 1); // it should be at least 1 here; we allow 0 in other places for "no threads"
		PersonAlgoThread[] algoThreads = new PersonAlgoThread[numOfThreads];
		Thread[] threads = new Thread[numOfThreads];
		String name = null;
		Counter counter = null;
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			PersonAlgorithm algo = algoProvider.getPersonAlgorithm();
			if (i == 0) {
				name = algo.getClass().getSimpleName();
				counter = new Counter("[" + name + "] handled person # ");
			}
			PersonAlgoThread algothread = new PersonAlgoThread(i, algo, counter);
			Thread thread = new Thread(algothread, name + "." + i);
			threads[i] = thread;
			algoThreads[i] = algothread;
		}
		
		// distribute workload between threads, as long as threads are not yet started, so we don't need synchronized data structures
		int i = 0;
		for (Person person : population) {
			algoThreads[i % numOfThreads].handlePerson(person);
			i++;
		}
		
		// start the threads
		for (Thread thread : threads) {
			thread.start();
		}
		
		// wait for the threads to finish
		try {
			for (Thread thread : threads) {
				thread.join();
			}
			counter.printCounter();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		}
	}
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class PersonAlgoThread implements Runnable {

		public final int threadId;
		private final PersonAlgorithm personAlgo;
		private final List<Person> persons = new LinkedList<Person>();
		private final Counter counter;

		public PersonAlgoThread(final int i, final PersonAlgorithm algo, final Counter counter) {
			this.threadId = i;
			this.personAlgo = algo;
			this.counter = counter;
		}

		public void handlePerson(final Person person) {
			this.persons.add(person);
		}

		public void run() {
			for (Person person : this.persons) {
				this.personAlgo.run(person);
				counter.incCounter();
			}
		}
	}
}
