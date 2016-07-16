/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceStats.java
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

package playground.sergioo.typesPopulation2013.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 *
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * 	<li>average of the average leg distance per plan</li>
 * </ul>
 *
 * Is used by the standard Controler and fed the "really executed" "plans" which
 * are generated from Events during the simulation and which are also used by the scoring.
 * But you can also use it on other kinds of plans from your own code.
 *
 * @author anhorni, michaz
 */

public class TravelDistanceStats {

	final private Config config;
	final private Network network;
	final private BufferedWriter out;
	final private String fileName;

	private final boolean createPNG;
	private double[] history = null;

	private Thread[] threads = null;
	private StatsCalculator[] statsCalculators = null;
	private final AtomicBoolean hadException = new AtomicBoolean(false);
	private final ExceptionHandler exceptionHandler = new ExceptionHandler(this.hadException);

	private final static Logger log = Logger.getLogger(TravelDistanceStats.class);

	/**
	 * @param filename including the path, excluding the file type extension
	 * @param createPNG true if in every iteration, the distance statistics should be visualized in a graph and written to disk.
	 * @throws UncheckedIOException
	 */
	public TravelDistanceStats(final Config config, final Network network, final String filename, final boolean createPNG) throws UncheckedIOException {
		this.config = config;
		this.network = network;
		this.fileName = filename;
		this.createPNG = createPNG;
		if (this.createPNG) {
			int iterations = config.controler().getLastIteration() - config.controler().getFirstIteration();
			if (iterations > 5000) {
				iterations = 5000; // limit the history size
			}
			this.history = new double[iterations+1];
		}
		if (filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
			this.out = IOUtils.getBufferedWriter(filename);
		} else {
			this.out = IOUtils.getBufferedWriter(filename + ".txt");
		}
		try {
			this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void addIteration(int iteration, Map<Id<Plan>, Plan> map) {

		int numOfThreads = this.config.global().getNumberOfThreads();
		if (numOfThreads < 1) numOfThreads = 1;

		initThreads(numOfThreads);

		int roundRobin = 0;
		for (Plan plan : map.values()) {
			this.statsCalculators[roundRobin++ % numOfThreads].addPerson(plan);
		}

		log.info("[" + this.getClass().getSimpleName() + "] using " + numOfThreads + " thread(s).");
		// start threads
		for (Thread thread : this.threads) {
			thread.start();
		}
		// wait until each thread is finished
		try {
			for (Thread thread : this.threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		log.info("[" + this.getClass().getSimpleName() + "] all threads finished.");
		if (this.hadException.get()) {
			throw new RuntimeException("Some threads crashed, thus not all persons may have been handled.");
		}


		double sumAvgPlanLegTravelDistanceExecuted = 0.0;

		int nofLegTravelDistanceExecuted = 0;

		for (StatsCalculator statsCalculator : statsCalculators) {

			sumAvgPlanLegTravelDistanceExecuted += statsCalculator.sumAvgPlanLegTravelDistanceExecuted;
			nofLegTravelDistanceExecuted += statsCalculator.nofLegTravelDistanceExecuted;
		}

		// reset
		this.statsCalculators = null;
		this.threads = null;

		log.info("-- average of the average leg distance per plan (executed plans only): " + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted));
		log.info("(TravelDistanceStats takes an average over all legs that have a NetworkRoute.  These are usually all car legs.)") ;

		try {
			this.out.write(iteration + "\t" + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted) + "\t" + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = iteration - config.controler().getFirstIteration();
			this.history[index] = (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted);

			if (iteration != config.controler().getFirstIteration()) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Leg Travel Distance Statistics", "iteration", "average of the average leg distance per plan ");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + config.controler().getFirstIteration();
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history, 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(this.fileName + ".png", 800, 600);
			}
			if (index == (this.history.length - 1)) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}

	public void close() {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void initThreads(int numOfThreads) {
		if (this.threads != null) {
			throw new RuntimeException("threads are already initialized");
		}

		this.hadException.set(false);
		this.threads = new Thread[numOfThreads];
		this.statsCalculators = new StatsCalculator[numOfThreads];

		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			StatsCalculator statsCalculatorThread = new StatsCalculator(this.network);
			Thread thread = new Thread(statsCalculatorThread, this.getClass().getSimpleName() + "." + StatsCalculator.class.getSimpleName() + "." + i);
			thread.setUncaughtExceptionHandler(this.exceptionHandler);
			this.threads[i] = thread;
			this.statsCalculators[i] = statsCalculatorThread;
		}
	}

	private static class StatsCalculator implements Runnable {


		double sumAvgPlanLegTravelDistanceExecuted = 0.0;
		int nofLegTravelDistanceExecuted = 0;

		private Collection<Plan> persons;
		private Network network;

		public StatsCalculator(Network network) {
			this.network = network;
			persons = new ArrayList<Plan>();
		}

		public void addPerson(Plan plan) {
			persons.add(plan);
		}

		@Override
		public void run() {
			for (Plan plan : persons) {
				sumAvgPlanLegTravelDistanceExecuted += getAvgLegTravelDistance(plan);
				nofLegTravelDistanceExecuted++;
			}

		}

		private double getAvgLegTravelDistance(final Plan plan) {

			double planTravelDistance=0.0;
			int numberOfLegs=0;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					final Leg leg = (Leg) pe;
					if (leg.getRoute() instanceof NetworkRoute) {
						planTravelDistance += RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) leg.getRoute(), this.network);
						numberOfLegs++;
					} else {
						double distance = leg.getRoute().getDistance();
						if (!Double.isNaN(distance)) {
							planTravelDistance += distance;
						}
						numberOfLegs++;
					}
				}
			}
			if (numberOfLegs>0) {
				return planTravelDistance/numberOfLegs;
			}
			return 0.0;
		}
	}

	private static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicBoolean hadException;

		public ExceptionHandler(final AtomicBoolean hadException) {
			this.hadException = hadException;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.error("Thread " + t.getName() + " died with exception. Will stop after all threads finished.", e);
			this.hadException.set(true);
		}

	}

	
}
