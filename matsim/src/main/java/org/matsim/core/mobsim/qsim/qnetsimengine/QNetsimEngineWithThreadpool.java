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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
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
	private final Timing overallTiming = new Timing( "NetsimeEngine_overall");
	private final Timing nodesTiming = new Timing("NetsimeEngine_nodes");
	private final Timing linksTiming = new Timing("NetsimeEngine_links");
	private final DoubleList times = new DoubleArrayList();
	private final String timingOutuputPath;
	private ExecutorService pool;
	
	public QNetsimEngineWithThreadpool(final QSim sim) {
		this(sim, null);
	}

	@Inject
	public QNetsimEngineWithThreadpool(final QSim sim, QNetworkFactory netsimNetworkFactory) {
		super(sim, netsimNetworkFactory);
		timingOutuputPath = sim.getScenario().getConfig().controler().getOutputDirectory();
		this.numOfRunners = this.numOfThreads;
	}

	@Override
	public void afterSim() {

		var timings = this.getQnetsimEngineRunner().stream()
				.flatMap(runner -> Stream.of(runner.getLinksTiming(), runner.getNodesTiming()))
				.collect(Collectors.toList());

		var ownTimings = List.of(overallTiming, nodesTiming, linksTiming);
		timings.addAll(ownTimings);
		var outputPath = Paths.get(timingOutuputPath).resolve("timings.csv");

		try (var writer = Files.newBufferedWriter(outputPath); var printer = CSVFormat.Builder.create().build().print(writer)) {

			// print the header
			printer.print("time");
			for(var timing : timings) {
				printer.print(timing.getName());
			}
			printer.println();

			// now all the values
			// assuming that all the timings have the same number of values
			var firstTimingSize = timings.get(0).getDurations().size();
			for(int i = 0; i < firstTimingSize; i++) {
				// first the time step
				printer.print(times.getDouble(i));
				for (var timing : timings) {
					var duration = timing.getDurations().getLong(i);
					printer.print(duration);
				}
				printer.println();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		super.afterSim();
	}

	@Override
	public void finishMultiThreading() {
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
		times.add(time);
		for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
			engine.setTime(time);
		}

		try {
			var overallStartTime = System.nanoTime();
			for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
				((QNetsimEngineRunnerForThreadpool) engine).setMovingNodes(true);
			}

			var nodesStartTime = System.nanoTime();
			for (Future<Boolean> future : pool.invokeAll(this.getQnetsimEngineRunner())) {
				future.get();

			}
			var nodesDuration = System.nanoTime() - nodesStartTime;
			int nodeCount = this.getQnetsimEngineRunner().stream()
					.mapToInt(AbstractQNetsimEngineRunner::getNodeCounter)
					.sum();
			nodesTiming.addDuration(nodesDuration / Math.max(1, nodeCount));

			for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
				((QNetsimEngineRunnerForThreadpool) engine).setMovingNodes(false);
			}

			var linksStartTime = System.nanoTime();
			for (Future<Boolean> future : pool.invokeAll(this.getQnetsimEngineRunner())) {
				future.get();
			}
			var afterLinksTime = System.nanoTime();
			var linksDuration = afterLinksTime - linksStartTime;
			var linkCount = this.getQnetsimEngineRunner().stream()
					.mapToInt(AbstractQNetsimEngineRunner::getLinkCounter)
					.sum();
			linksTiming.addDuration(linksDuration / Math.max(1, linkCount));

			var overallDuration = afterLinksTime - overallStartTime;
			overallTiming.addDuration(overallDuration);
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
			QNetsimEngineRunnerForThreadpool engine = new QNetsimEngineRunnerForThreadpool("Runner_" + i);
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
