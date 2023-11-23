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

package org.matsim.core.population.algorithms;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Counter;

/**
 * An abstract/static helper class for running {@link AbstractPersonAlgorithm}s in parallel using threads.
 *
 * @author mrieser
 */
public final class ParallelPersonAlgorithmUtils {
	private ParallelPersonAlgorithmUtils(){} // do not instantiate

	private final static Logger log = LogManager.getLogger(ParallelPersonAlgorithmUtils.class);
	
	public interface PersonAlgorithmProvider {
		public PersonAlgorithm getPersonAlgorithm();
	}

	/**
	 * Handles each person of the given <code>population</code> with the specified <code>algorithm</code>,
	 * using up to <code>numberOfThreads</code> threads to speed things up. Use this method only if the given
	 * algorithm is thread-safe! Otherwise, use {@link #run(Population, int, PersonAlgorithmProvider)}.
	 *
	 * @param population
	 * @param numberOfThreads
	 * @param algorithm
	 */
	public static void run(final Population population, final int numberOfThreads, final PersonAlgorithm algorithm) {
		run(population, numberOfThreads, new PersonAlgorithmProvider() {
			@Override public PersonAlgorithm getPersonAlgorithm() {
				return algorithm;
			}
		});
	}

	/**
	 * Handles each person of the given <code>population</code> with a AbstractPersonAlgorithm provided by <code>algoProvider</code>,
	 * using up to <code>numberOfThreads</code> threads to speed things up. This method will request a new instance of the
	 * AbstractPersonAlgorithm for each thread it allocates, thus enabling the parallel use of non-thread-safe algorithms.
	 * For thread-safe algorithms, {@link #run(Population, int, AbstractPersonAlgorithm)} may be an easier method to use.
	 *
	 * @param population
	 * @param numberOfThreads
	 * @param algoProvider
	 */
	public static void run(final Population population, final int numberOfThreads, final PersonAlgorithmProvider algoProvider) {
		int numOfThreads = Math.max(numberOfThreads, 1); // it should be at least 1 here; we allow 0 in other places for "no threads"
		PersonAlgoThread[] algoThreads = new PersonAlgoThread[numOfThreads];
		Thread[] threads = new Thread[numOfThreads];
		String name = null;
		Counter counter = null;
		
		final AtomicBoolean hadException = new AtomicBoolean(false);
		final ExceptionHandler uncaughtExceptionHandler = new ExceptionHandler(hadException);

		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			PersonAlgorithm algo = algoProvider.getPersonAlgorithm();
			if (i == 0) {
				name = algo.getClass().getSimpleName();
				counter = new Counter("[" + name + "] handled person # ");
			}
			PersonAlgoThread algothread = new PersonAlgoThread(algo, counter);
			Thread thread = new Thread(algothread, name + "." + i);
			thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
			threads[i] = thread;
			algoThreads[i] = algothread;
		}

		// distribute workload between threads, as long as threads are not yet started, so we don't need synchronized data structures
		int i = 0;
		for (Person person : population.getPersons().values()) {
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
			throw new RuntimeException(e);
		}
		if (hadException.get()) {
			throw new RuntimeException("Exception while processing persons. Cannot guarantee that all persons have been fully processed.");
		}
	}

	/**
	 * The thread class that really handles the persons.
	 */
	private static class PersonAlgoThread implements Runnable {

		private final PersonAlgorithm personAlgo;
		private final List<Person> persons = new LinkedList<Person>();
		private final Counter counter;

		public PersonAlgoThread(final PersonAlgorithm algo, final Counter counter) {
			this.personAlgo = algo;
			this.counter = counter;
		}

		public void handlePerson(final Person person) {
			this.persons.add(person);
		}

		@Override
		public void run() {
			for (Person person : this.persons) {
				this.personAlgo.run(person);
				counter.incCounter();
			}
		}
	}
	
	/**
	 * @author mrieser
	 */
	private static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicBoolean hadException;

		public ExceptionHandler(final AtomicBoolean hadException) {
			this.hadException = hadException;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.error("Thread " + t.getName() + " died with exception while handling events.", e);
			this.hadException.set(true);
		}

	}

}
