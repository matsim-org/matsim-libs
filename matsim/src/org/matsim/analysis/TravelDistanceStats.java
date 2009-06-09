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
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

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
	final private BufferedWriter out;

	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;

	private final static Logger log = Logger.getLogger(TravelDistanceStats.class);

	/**
	 * @param population
	 * @param filename
	 * @param createPNG true if in every iteration, the distance statistics should be visualized in a graph and written to disk.
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
		double sumAvgPlanLegTravelDistanceWorst = 0.0;
		double sumAvgPlanLegTravelDistanceBest = 0.0;
		double sumAvgPlanLegTravelDistanceAll = 0.0;
		double sumAvgPlanLegTravelDistanceExecuted = 0.0;
		int nofLegTravelDistanceWorst = 0;
		int nofLegTravelDistanceBest = 0;
		int nofLegTravelDistanceAvg = 0;
		int nofLegTravelDistanceExecuted = 0;

		for (Person person : this.population.getPersons().values()) {
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
		log.info("-- average of the average leg distance per plan (executed plans only): " + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted));
		log.info("-- average of the average leg distance per plan (worst plans only): " + (sumAvgPlanLegTravelDistanceWorst / nofLegTravelDistanceWorst));
		log.info("-- average of the average leg distance per plan (all plans): " + (sumAvgPlanLegTravelDistanceAll / nofLegTravelDistanceAvg));
		log.info("-- average of the average leg distance per plan (best plans only): " + (sumAvgPlanLegTravelDistanceBest / nofLegTravelDistanceBest));

		try {
			this.out.write(event.getIteration() + "\t" + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelDistanceExecuted) + "\t" +
					(sumAvgPlanLegTravelDistanceWorst / nofLegTravelDistanceWorst) + "\t" + (sumAvgPlanLegTravelDistanceAll / nofLegTravelDistanceAvg) + "\t" + (sumAvgPlanLegTravelDistanceBest / nofLegTravelDistanceBest) + "\n");
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

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				final Leg leg = (Leg) pe;
				planTravelDistance+=leg.getRoute().getDistance();
				numberOfLegs++;
			}
		}
		if (numberOfLegs>0) {
			return planTravelDistance/numberOfLegs;
		}
		return 0.0;
	}

}
