/* *********************************************************************** *
 * project: org.matsim.*
 * QNetsimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import jakarta.inject.Inject;

import org.matsim.core.mobsim.qsim.QSim;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 * Split Up the old {@code QNetsimEngineRunner} which was implementing
 * 2 different approaches parallel.
 *
 * @author droeder@Senozon after
 *
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
final class QNetsimEngineWithThreadpool extends AbstractQNetsimEngine<QNetsimEngineRunnerForThreadpool> {

	private final int numOfRunners;
	private ExecutorService pool;

//	public QNetsimEngineWithThreadpool(final QSim sim) {
//		this(sim, null);
//	}

	@Inject QNetsimEngineWithThreadpool(final QSim sim, QNetworkFactory netsimNetworkFactory, NetworkModeDepartureHandler networkModeDepartureHandler) {
		super(sim, netsimNetworkFactory, networkModeDepartureHandler);
		this.numOfRunners = this.numOfThreads;
	}

	@Override public void finishMultiThreading() {
		this.pool.shutdown();
	}

	protected void run(double time) {
		// yy Acceleration options to try out (kai, jan'15):

		// (a) Try to do without barriers.  With our
		// message-based experiments a decade ago, it was better to let each runner decide locally when to proceed.  For intuition, imagine that
		// one runner is slowest on the links, and some other runner slowest on the nodes.  With the barriers, this cannot overlap.
		// With message passing, this was achieved by waiting for all necessary messages.  Here, it could (for example) be achieved with runner-local
		// clocks:
		// for ( all runners that own incoming links to my nodes ) { // (*)
		//    wait until runner.getTime() == myTime ;
		// }
		// processLocalNodes() ;
		// mytime += 0.5 ;
		// for ( all runners that own toNodes of my links ) { // (**)
		//    wait until runner.getTime() == myTime ;
		// }
		// processLocalLinks() ;
		// myTime += 0.5 ;

		// (b) Do deliberate domain decomposition rather than round robin (fewer runners to wait for at (*) and (**)).

		// (c) One thread that is much faster than all others is much more efficient than one thread that is much slower than all others.
		// So make sure that no thread sticks out in terms of slowness.  Difficult to achieve, though.  A decade back, we used a "typical" run
		// as input for the domain decomposition under (b).

		// set current Time
		for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
			engine.setTime(time);
		}

		try {
			for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
				((QNetsimEngineRunnerForThreadpool) engine).setMovingNodes(true);
			}
			for (Future<Boolean> future : pool.invokeAll(this.getQnetsimEngineRunner())) {
				future.get();
			}
			for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
				((QNetsimEngineRunnerForThreadpool) engine).setMovingNodes(false);
			}
			for (Future<Boolean> future : pool.invokeAll(this.getQnetsimEngineRunner())) {
				future.get();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e) ;
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private static class NamedThreadFactory implements ThreadFactory {
		private int count = 0;

		@Override
		public Thread newThread(Runnable r) {
			return new Thread( r , "QNetsimEngine_PooledThread_" + count++);
		}
	}

	@Override
	protected List<QNetsimEngineRunnerForThreadpool> initQSimEngineRunners() {
		List<QNetsimEngineRunnerForThreadpool> engines = new ArrayList<>();
		for (int i = 0; i < numOfRunners; i++) {
			QNetsimEngineRunnerForThreadpool engine = new QNetsimEngineRunnerForThreadpool();
			engines.add(engine);
		}
		return engines;
	}

	@Override
	protected void initMultiThreading() {
		this.pool = Executors.newFixedThreadPool(
				this.numOfThreads,
				new NamedThreadFactory());
	}
}
