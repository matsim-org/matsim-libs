/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractMultithreadedGroupStrategyModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.utils.misc.Counter;

import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.GenericStrategyModule;

/**
 * @author thibautd
 */
public abstract class AbstractMultithreadedGenericStrategyModule<T> implements GenericStrategyModule<T> {
	static final private Logger log = Logger.getLogger(AbstractMultithreadedGenericStrategyModule.class);
	private int numOfThreads;
	private final String name;

	public AbstractMultithreadedGenericStrategyModule(GlobalConfigGroup globalConfigGroup) {
		this( globalConfigGroup.getNumberOfThreads() );
	}

	public AbstractMultithreadedGenericStrategyModule(final int numOfThreads) {
		this.name = getName();
		this.numOfThreads = numOfThreads;
	}

	@Override
	public void handlePlans(
			final ReplanningContext replanningContext,
			final Collection<T> groupPlans) {
		log.info( "running "+name+" on "+groupPlans.size()+" groups" );
		final List<PlanRunnable<T>> runnables = getPlanRunnables( replanningContext , groupPlans );
		final ExceptionHandler exceptionHandler = new ExceptionHandler();

		List<Thread> threads = new ArrayList<Thread>( numOfThreads );
		for (PlanRunnable<T> runnable : runnables) {
			final Thread t = new Thread( runnable , name+"_"+threads.size());
			t.setUncaughtExceptionHandler( exceptionHandler );
			threads.add( t );
			t.start();
		}

		for (Thread t : threads) {
			try {
				t.join();
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		}

		if (exceptionHandler.hadException.get()) {
			throw new RuntimeException( "got exception while replanning" );
		}
	}

	private List<PlanRunnable<T>> getPlanRunnables(
			final ReplanningContext replanningContext,
			final Collection<T> groupPlans) {
		final List<PlanRunnable<T>> runnables = new ArrayList<PlanRunnable<T>>();

		final Counter counter = new Counter( "["+name+"] handled plan # " );
		for (int i=0; i < numOfThreads; i++) {
			runnables.add( new PlanRunnable<T>( counter , createAlgorithm(replanningContext) ) );
		}

		int count = 0;
		for (T p : groupPlans) {
			runnables.get( count % numOfThreads ).groupPlans.add( p );
			count++;
		}

		return runnables;
	}

	public abstract GenericPlanAlgorithm<T> createAlgorithm(ReplanningContext replanningContext);

	private static class PlanRunnable<T> implements Runnable {
		final List<T> groupPlans = new ArrayList<T>();
		final GenericPlanAlgorithm<T> algo;
		final Counter counter;

		public PlanRunnable(
				final Counter counter,
				final GenericPlanAlgorithm<T> algo) {
			this.counter = counter;
			this.algo = algo;
		}

		@Override
		public void run() {
			for (T p : groupPlans) {
				try {
					algo.run( p );
				}
				catch ( Exception e ) {
					throw new RuntimeException( "exception when handling "+p , e );
				}
				counter.incCounter();
			}
		}
	}

	private static class ExceptionHandler implements UncaughtExceptionHandler {
		private final AtomicBoolean hadException = new AtomicBoolean( false );

		@Override
		public void uncaughtException(final Thread t, final Throwable e) {
			log.error("Thread " + t.getName() + " died with exception. Will stop after all threads finished.", e);
			this.hadException.set(true);
		}

	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * It is possible to override this method, which is used in log messages.
	 * It is called once, at construction.
	 */
	protected String getName() {
		final String tmpName = getClass().getSimpleName();
		if (tmpName.length() == 0) {
			// anonymous class
			return "???";//createAlgorithm(replanningContext).getClass().getSimpleName();
		}

		return tmpName;
	}
}

