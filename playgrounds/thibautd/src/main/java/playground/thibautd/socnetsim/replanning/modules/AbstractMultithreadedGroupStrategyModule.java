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
package playground.thibautd.socnetsim.replanning.modules;

import java.lang.Thread.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.replanning.GroupPlansAlgorithm;
import playground.thibautd.socnetsim.replanning.GroupStrategyModule;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGroupStrategyModule;

/**
 * @author thibautd
 */
public abstract class AbstractMultithreadedGroupStrategyModule implements GroupStrategyModule {
	static final private Logger log = Logger.getLogger(AbstractMultithreadedGroupStrategyModule.class);
	private final int numOfThreads;
	private final String name;

	public AbstractMultithreadedGroupStrategyModule(GlobalConfigGroup globalConfigGroup) {
		this( globalConfigGroup.getNumberOfThreads() );
	}

	public AbstractMultithreadedGroupStrategyModule(final int numOfThreads) {
		this.name = getClass().getSimpleName();
		this.numOfThreads = numOfThreads;
	}

	@Override
	public void handlePlans(final Collection<GroupPlans> groupPlans) {
		log.info( "running "+name+" on "+groupPlans.size()+" groups" );
		final PlanRunnable[] runnables = getPlanRunnables( groupPlans );
		final ExceptionHandler exceptionHandler = new ExceptionHandler();

		List<Thread> threads = new ArrayList<Thread>( numOfThreads );
		for (PlanRunnable runnable : runnables) {
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

	private PlanRunnable[] getPlanRunnables(Collection<GroupPlans> groupPlans) {
		final PlanRunnable[] runnables = new PlanRunnable[ numOfThreads ];

		final Counter counter = new Counter( "["+name+"] handled plan # " );
		for (int i=0; i < runnables.length; i++) {
			runnables[ i ] = new PlanRunnable( counter );
		}

		int count = 0;
		for (GroupPlans p : groupPlans) {
			runnables[ count % numOfThreads ].groupPlans.add( p );
			count++;
		}

		return runnables;
	}

	public abstract GroupPlansAlgorithm createAlgorithm();

	private class PlanRunnable implements Runnable {
		final List<GroupPlans> groupPlans = new ArrayList<GroupPlans>();
		final GroupPlansAlgorithm algo = createAlgorithm();
		final Counter counter;

		public PlanRunnable(final Counter counter) {
			this.counter = counter;
		}

		@Override
		public void run() {
			for (GroupPlans p : groupPlans) {
				algo.run( p );
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
}

