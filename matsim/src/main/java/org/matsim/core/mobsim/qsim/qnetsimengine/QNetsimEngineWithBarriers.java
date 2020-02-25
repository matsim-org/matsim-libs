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
import java.util.concurrent.Phaser;

import javax.inject.Inject;

import org.matsim.core.mobsim.qsim.QSim;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author droeder@Senozon after
 * 
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
final class QNetsimEngineWithBarriers extends AbstractQNetsimEngine {

	// for detailed run time analysis - used in combination with QSim.analyzeRunTimes
	public static int numObservedTimeSteps = 24*3600;
	public static boolean printRunTimesPerTimeStep = false;
	
	private Phaser startBarrier;
	private Phaser endBarrier;
	
	public QNetsimEngineWithBarriers(final QSim sim) {
		this(sim, null);
	}

	@Inject
	public QNetsimEngineWithBarriers(final QSim sim, QNetworkFactory netsimNetworkFactory) {
		super(sim, netsimNetworkFactory);
	}

//	static AbstractAgentSnapshotInfoBuilder createAgentSnapshotInfoBuilder(Scenario scenario, SnapshotLinkWidthCalculator linkWidthCalculator) {
//		final SnapshotStyle snapshotStyle = scenario.getConfig().qsim().getSnapshotStyle();
//		switch(snapshotStyle) {
//		case queue:
//			return new QueueAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
//		case withHoles:
//		case withHolesAndShowHoles:
//			// the difference is not in the spacing, thus cannot be differentiated by using different classes.  kai, sep'14
//			// ??? kai, nov'15
//			return new QueueAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
//		case kinematicWaves:
//			log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not explicitly supported. Using \""+SnapshotStyle.withHoles+ "\" instead.");
//			return new QueueAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
//		case equiDist:
//			return new EquiDistAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
//		default:
//			log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not supported. Using equiDist");
//			return new EquiDistAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);
//		}
//	}

	@Override
	public void finishMultiThreading() {
		this.startBarrier.arriveAndAwaitAdvance();
	}


	/*
	 * The Threads are waiting at the startBarrier.
	 * We trigger them by reaching this Barrier. Now the
	 * Threads will start moving the Nodes and Links. We wait
	 * until all of them reach the endBarrier to move
	 * on. We should not have any Problems with Race Conditions
	 * because even if the Threads would be faster than this
	 * Thread, means the reach the endBarrier before
	 * this Method does, it should work anyway.
	 */
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
		this.startBarrier.arriveAndAwaitAdvance();
		this.endBarrier.arriveAndAwaitAdvance();
	}

	@Override
	protected List<AbstractQNetsimEngineRunner> initQSimEngineRunner() {
		this.startBarrier = new Phaser(this.numOfThreads + 1);
		Phaser separationBarrier = new Phaser(this.numOfThreads);
		this.endBarrier = new Phaser(this.numOfThreads + 1);
		List<AbstractQNetsimEngineRunner> engines = new ArrayList<>();
		for(int i = 0; i < this.numOfThreads; i++) {
			AbstractQNetsimEngineRunner engine = new QNetsimEngineRunnerWithBarriers(this.startBarrier, separationBarrier, endBarrier);
			engines.add(engine);
		}
		return engines;
	}

	@Override
	protected void initMultiThreading() {
		for (int i = 0; i < super.getQnetsimEngineRunner().size(); i++) {
			Thread thread = new Thread(super.getQnetsimEngineRunner().get(i));
			thread.setName("QNetsimEngineRunner_" + i);
			thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			thread.start();
		}
	}

}
