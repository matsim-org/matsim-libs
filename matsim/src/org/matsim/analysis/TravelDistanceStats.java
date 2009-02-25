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

package org.matsim.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.Population;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;

/**
 *
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average leg travel distance of the worst plan of each agent</li>
 * <li>average leg travel distance of the best plan of each agent</li>
 * <li>average leg travel distance of all plans of each agent</li>
 *  * <li>average leg travel distance of the selected plan</li>
 * </ul>

 * The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author anhorni
 */

/*
 * TODO: [AH] This is copy-paste based on "ScoreStats". Refactoring needed!
 */
public class TravelDistanceStats implements StartupListener, IterationEndsListener, ShutdownListener {

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	final private Population population;
	final private BufferedWriter out;

	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;

	private final static Logger log = Logger.getLogger(TravelDistanceStats.class);

	/**
	 * Creates a new ScoreStats instance.
	 *
	 * @param population
	 * @param filename
	 * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public TravelDistanceStats(final Population population, final String filename, final boolean createPNG) throws FileNotFoundException, IOException {
		this.population = population;
		this.createPNG = createPNG;
		this.out = IOUtils.getBufferedWriter(filename);
		this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
	}

	public void notifyStartup(final StartupEvent event) {
		if (this.createPNG) {
			Controler controler = event.getControler();
			this.minIteration = controler.getFirstIteration();
			int maxIter = controler.getLastIteration();
			int iterations = maxIter - this.minIteration;
			if (iterations > 10000) {
				iterations = 1000; // limit the history size
			}
			this.history = new double[4][iterations+1];
		}
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		double sumLegTravelDistanceWorst = 0.0;
		double sumLegTravelDistanceBest = 0.0;
		double sumLegTravelDistanceAvg = 0.0;
		double sumLegTravelDistanceExecuted = 0.0;
		int nofLegTravelDistanceWorst = 0;
		int nofLegTravelDistanceBest = 0;
		int nofLegTravelDistanceAvg = 0;
		int nofLegTravelDistanceExecuted = 0;

		for (Person person : this.population.getPersons().values()) {
			Plan worstPlan = null;
			Plan bestPlan = null;
			double sumLegTravelDistance = 0.0;
			double cntLegTravelDistance = 0;
			for (Plan plan : person.getPlans()) {
				// worst plan -----------------------------------------------------
				if (worstPlan == null) {
					worstPlan = plan;
				} else if (plan.getScore() < worstPlan.getScore()) {
					worstPlan = plan;
				}

				// best plan -------------------------------------------------------
				if (bestPlan == null) {
					bestPlan = plan;
				} else if (plan.getScore() > bestPlan.getScore()) {
					bestPlan = plan;
				}

				// avg. leg travel distance
				sumLegTravelDistance += getAvgLegTravelDistance(plan);
				cntLegTravelDistance++;

				// executed plan? --------------------------------------------------
				if (plan.isSelected()) {
					sumLegTravelDistanceExecuted += getAvgLegTravelDistance(plan);
					nofLegTravelDistanceExecuted++;
				}
			}

			if (worstPlan != null) {
				nofLegTravelDistanceWorst++;
				sumLegTravelDistanceWorst += getAvgLegTravelDistance(worstPlan);
			}
			if (bestPlan != null) {
				nofLegTravelDistanceBest++;
				sumLegTravelDistanceBest += getAvgLegTravelDistance(bestPlan);
			}
			if (cntLegTravelDistance > 0) {
				sumLegTravelDistanceAvg += (sumLegTravelDistance / cntLegTravelDistance);
				nofLegTravelDistanceAvg++;
			}
		}
		log.info("-- avg. leg travel distance of the executed plan of each agent: " + (sumLegTravelDistanceExecuted / nofLegTravelDistanceExecuted));
		log.info("-- avg. leg travel distance of the worst plan of each agent: " + (sumLegTravelDistanceWorst / nofLegTravelDistanceWorst));
		log.info("-- avg. of the avg. leg travel distance per agent: " + (sumLegTravelDistanceAvg / nofLegTravelDistanceAvg));
		log.info("-- avg. leg travel distance of the best plan of each agent: " + (sumLegTravelDistanceBest / nofLegTravelDistanceBest));

		try {
			this.out.write(event.getIteration() + "\t" + (sumLegTravelDistanceExecuted / nofLegTravelDistanceExecuted) + "\t" +
					(sumLegTravelDistanceWorst / nofLegTravelDistanceWorst) + "\t" + (sumLegTravelDistanceAvg / nofLegTravelDistanceAvg) + "\t" + (sumLegTravelDistanceBest / nofLegTravelDistanceBest) + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_WORST][index] = (sumLegTravelDistanceWorst / nofLegTravelDistanceWorst);
			this.history[INDEX_BEST][index] = (sumLegTravelDistanceBest / nofLegTravelDistanceBest);
			this.history[INDEX_AVERAGE][index] = (sumLegTravelDistanceAvg / nofLegTravelDistanceAvg);
			this.history[INDEX_EXECUTED][index] = (sumLegTravelDistanceExecuted / nofLegTravelDistanceExecuted);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Leg Travel Distance Statistics", "iteration", "avg. leg travel distance");
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
				chart.saveAsPng(Controler.getOutputFilename("traveldistancestats.png"), 800, 600);
			}
			if (index == this.history[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}

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

	private double getAvgLegTravelDistance(final Plan plan){

		double planTravelDistance=0.0;
		int numberOfLegs=0;

		final LegIterator leg_it = plan.getIteratorLeg();
		while (leg_it.hasNext()) {
			final Leg leg = (Leg)leg_it.next();
			planTravelDistance+=leg.getRoute().getDist();
			numberOfLegs++;
		}
		if (numberOfLegs>0) {
			return planTravelDistance/numberOfLegs;
		}
		return 0.0;
	}

}
