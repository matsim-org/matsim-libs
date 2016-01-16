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

package playground.christoph.energyflows.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 *
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * 	<li>average of the average leg distance per plan (worst plans only)</li>
 * 	<li>average of the average leg distance per plan (best plans only)</li>
 * 	<li>average of the average leg distance per plan (selected plans only)</li>
 * 	<li>average of the average leg distance per plan (all plans)</li>
 * </ul>
 *
 *
 * @author anhorni
 */

/*
 * TODO: [AH] This is copy-paste based on "ScoreStats". Refactoring needed!
 *
 */
public class TravelDistanceStats implements StartupListener, IterationEndsListener, ShutdownListener {

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	final private Population population;
	final private Network network;
	final private BufferedWriter out;
	final private String fileName;

	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;

	private Thread[] threads = null;
	private StatsCalculator[] statsCalculators = null;
	private final AtomicBoolean hadException = new AtomicBoolean(false);
	private final ExceptionHandler exceptionHandler = new ExceptionHandler(this.hadException);

	private final static Logger log = Logger.getLogger(TravelDistanceStats.class);

	/**
	 * @param population
	 * @param filename including the path, excluding the file type extension
	 * @param createPNG true if in every iteration, the distance statistics should be visualized in a graph and written to disk.
	 * @throws UncheckedIOException
	 */
	public TravelDistanceStats(final Population population, final Network network, final String filename, final boolean createPNG) throws UncheckedIOException {
		this.population = population;
		this.network = network;
		this.fileName = filename;
		this.createPNG = createPNG;
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

	@Override
	public void notifyStartup(final StartupEvent event) {
		if (this.createPNG) {
			MatsimServices controler = event.getServices();
			this.minIteration = controler.getConfig().controler().getFirstIteration();
			int maxIter = controler.getConfig().controler().getLastIteration();
			int iterations = maxIter - this.minIteration;
			if (iterations > 5000) {
				iterations = 5000; // limit the history size
			}
			this.history = new double[4][iterations+1];
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		int numOfThreads = event.getServices().getConfig().global().getNumberOfThreads();
		if (numOfThreads < 1) numOfThreads = 1;

		initThreads(numOfThreads);

		int roundRobin = 0;
		for (Person person : this.population.getPersons().values()) {
			this.statsCalculators[roundRobin++ % numOfThreads].addPerson(person);
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

		double sumAvgPlanLegTravelDistanceWorst = 0.0;
		double sumAvgPlanLegTravelDistanceBest = 0.0;
		double sumAvgPlanLegTravelDistanceAll = 0.0;
		double sumAvgPlanLegTravelDistanceExecuted = 0.0;
		int nofLegTravelDistanceWorst = 0;
		int nofLegTravelDistanceBest = 0;
		int nofLegTravelDistanceAvg = 0;
		int nofLegTravelDistanceExecuted = 0;

		for (StatsCalculator statsCalculator : statsCalculators) {
			sumAvgPlanLegTravelDistanceWorst += statsCalculator.sumAvgPlanLegTravelDistanceWorst;
			sumAvgPlanLegTravelDistanceBest += statsCalculator.sumAvgPlanLegTravelDistanceBest;
			sumAvgPlanLegTravelDistanceAll += statsCalculator.sumAvgPlanLegTravelDistanceAll;
			sumAvgPlanLegTravelDistanceExecuted += statsCalculator.sumAvgPlanLegTravelDistanceExecuted;
			nofLegTravelDistanceWorst += statsCalculator.nofLegTravelDistanceWorst;
			nofLegTravelDistanceBest += statsCalculator.nofLegTravelDistanceBest;
			nofLegTravelDistanceAvg += statsCalculator.nofLegTravelDistanceAvg;
			nofLegTravelDistanceExecuted += statsCalculator.nofLegTravelDistanceExecuted;
		}

		// reset
		this.statsCalculators = null;
		this.threads = null;

		log.info("-- average of the average leg distance per plan (executed plans only): " + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted));
		log.info("-- average of the average leg distance per plan (worst plans only): " + (sumAvgPlanLegTravelDistanceWorst / nofLegTravelDistanceWorst));
		log.info("-- average of the average leg distance per plan (all plans): " + (sumAvgPlanLegTravelDistanceAll / nofLegTravelDistanceAvg));
		log.info("-- average of the average leg distance per plan (best plans only): " + (sumAvgPlanLegTravelDistanceBest / nofLegTravelDistanceBest));
		log.info("(TravelDistanceStats takes an average over all legs that have a NetworkRoute.  These are usually all car legs.)") ;

		try {
			this.out.write(event.getIteration() + "\t" + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted) + "\t" +
					(sumAvgPlanLegTravelDistanceWorst / nofLegTravelDistanceWorst) + "\t" + (sumAvgPlanLegTravelDistanceAll / nofLegTravelDistanceAvg) 
					+ "\t" + (sumAvgPlanLegTravelDistanceBest / nofLegTravelDistanceBest) + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_WORST][index] = (sumAvgPlanLegTravelDistanceWorst / nofLegTravelDistanceWorst);
			this.history[INDEX_BEST][index] = (sumAvgPlanLegTravelDistanceBest / nofLegTravelDistanceBest);
			this.history[INDEX_AVERAGE][index] = (sumAvgPlanLegTravelDistanceAll / nofLegTravelDistanceAvg);
			this.history[INDEX_EXECUTED][index] = (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Leg Travel Distance Statistics", "iteration", "average of the average leg distance per plan ");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history[INDEX_WORST], 0, values, 0, index + 1);
				chart.addSeries("worst plan", iterations, values);
				System.arraycopy(this.history[INDEX_BEST], 0, values, 0, index + 1);
				chart.addSeries("best plan", iterations, values);
				System.arraycopy(this.history[INDEX_AVERAGE], 0, values, 0, index + 1);
				chart.addSeries("avg. of plans", iterations, values);
				System.arraycopy(this.history[INDEX_EXECUTED], 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(this.fileName + ".png", 800, 600);
			}
			if (index == (this.history[0].length - 1)) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the history of ltd in last iterations
	 */
	public double[][] getHistory() {
		return this.history.clone();
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

		double sumAvgPlanLegTravelDistanceWorst = 0.0;
		double sumAvgPlanLegTravelDistanceBest = 0.0;
		double sumAvgPlanLegTravelDistanceAll = 0.0;
		double sumAvgPlanLegTravelDistanceExecuted = 0.0;
		int nofLegTravelDistanceWorst = 0;
		int nofLegTravelDistanceBest = 0;
		int nofLegTravelDistanceAvg = 0;
		int nofLegTravelDistanceExecuted = 0;

		private Collection<Person> persons;
		private Network network;

		public StatsCalculator(Network network) {
			this.network = network;
			persons = new ArrayList<Person>();
		}

		public void addPerson(Person person) {
			persons.add(person);
		}

		@Override
		public void run() {
			for (Person person : persons) {
				Plan worstPlan = null;
				Plan bestPlan = null;
				double worstScore = Double.POSITIVE_INFINITY;
				double bestScore = Double.NEGATIVE_INFINITY;
				double sumAvgLegTravelDistance = 0.0;
				double cntAvgLegTravelDistance = 0;
				for (Plan plan : person.getPlans()) {

					if (plan.getScore() == null) {
						continue;
					}
					double score = plan.getScore().doubleValue();

					// worst plan -----------------------------------------------------
					if (worstPlan == null) {
						worstPlan = plan;
						worstScore = score;
					} else if (score < worstScore) {
						worstPlan = plan;
						worstScore = score;
					}

					// best plan -------------------------------------------------------
					if (bestPlan == null) {
						bestPlan = plan;
						bestScore = score;
					} else if (score > bestScore) {
						bestPlan = plan;
						bestScore = score;
					}

					// avg. leg travel distance
					sumAvgLegTravelDistance += getAvgLegTravelDistance(plan);
					cntAvgLegTravelDistance++;

					// executed plan? --------------------------------------------------
					if (plan.isSelected()) {
						sumAvgPlanLegTravelDistanceExecuted += getAvgLegTravelDistance(plan);
						nofLegTravelDistanceExecuted++;
					}
				}

				if (worstPlan != null) {
					nofLegTravelDistanceWorst++;
					sumAvgPlanLegTravelDistanceWorst += getAvgLegTravelDistance(worstPlan);
				}
				if (bestPlan != null) {
					nofLegTravelDistanceBest++;
					sumAvgPlanLegTravelDistanceBest += getAvgLegTravelDistance(bestPlan);
				}
				if (cntAvgLegTravelDistance > 0) {
					sumAvgPlanLegTravelDistanceAll += (sumAvgLegTravelDistance / cntAvgLegTravelDistance);
					nofLegTravelDistanceAvg++;
				}
			}
		}

		private double getAvgLegTravelDistance(final Plan plan) {

			double planTravelDistance=0.0;
			int numberOfLegs=0;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					final Leg leg = (Leg) pe;
					if (leg.getRoute() instanceof NetworkRoute) {
						planTravelDistance += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network);
						numberOfLegs++;
					}
					//  Seems that this averages only over routes of type NetworkRoute.  This is, minimally, not consistent with what we had before
					// (average over ALL modes).  kai/benjamin, apr'10
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
