/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
import org.matsim.controler.Controler;
import org.matsim.controler.events.ControlerFinishIterationEvent;
import org.matsim.controler.events.ControlerShutdownEvent;
import org.matsim.controler.events.ControlerStartupEvent;
import org.matsim.controler.listener.ControlerFinishIterationListener;
import org.matsim.controler.listener.ControlerShutdownListener;
import org.matsim.controler.listener.ControlerStartupListener;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;

/**
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average score of the selected plan</li>
 * <li>average of the score of the worst plan of each agent</li>
 * <li>average of the score of the best plan of each agent</li>
 * <li>average of the average score of all plans of each agent</li>
 * </ul>
 * Plans with {@linkplain org.matsim.plans.Plan#isUndefinedScore(double) undefined scores}
 * are not included in the statistics. The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author mrieser
 */
public class ScoreStats implements ControlerStartupListener, ControlerFinishIterationListener, ControlerShutdownListener {

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	final private Plans population;
	final private BufferedWriter out;

	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;

	private final static Logger log = Logger.getLogger(ScoreStats.class);

	/**
	 * Creates a new ScoreStats instance.
	 *
	 * @param population
	 * @param filename
	 * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ScoreStats(final Plans population, final String filename, final boolean createPNG) throws FileNotFoundException, IOException {
		this.population = population;
		this.createPNG = createPNG;
		this.out = IOUtils.getBufferedWriter(filename);
		this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
	}

	public void notifyStartup(final ControlerStartupEvent event) {
		if (this.createPNG) {
			Controler controler = event.getControler();
			this.minIteration = controler.getMinimumIteration();
			int maxIter = controler.getMaximumIteration();
			int iterations = maxIter - this.minIteration;
			if (iterations > 10000) iterations = 1000; // limit the history size
			this.history = new double[4][iterations+1];
		}
	}

	public void notifyIterationFinished(final ControlerFinishIterationEvent event) {
		double sumScoreWorst = 0.0;
		double sumScoreBest = 0.0;
		double sumAvgScores = 0.0;
		double sumExecutedScores = 0.0;
		int nofScoreWorst = 0;
		int nofScoreBest = 0;
		int nofAvgScores = 0;
		int nofExecutedScores = 0;

		for (Person person : this.population.getPersons().values()) {
			Plan worstPlan = null;
			Plan bestPlan = null;
			double sumScores = 0.0;
			double cntScores = 0;
			for (Plan plan : person.getPlans()) {

				if (Plan.isUndefinedScore(plan.getScore())) {
					continue;
				}

				// worst plan
				if (worstPlan == null) {
					worstPlan = plan;
				} else if (plan.getScore() < worstPlan.getScore()) {
					worstPlan = plan;
				}

				// best plan
				if (bestPlan == null) {
					bestPlan = plan;
				} else if (plan.getScore() > bestPlan.getScore()) {
					bestPlan = plan;
				}

				// avg. score
				sumScores += plan.getScore();
				cntScores++;

				// executed plan?
				if (plan.isSelected()) {
					sumExecutedScores += plan.getScore();
					nofExecutedScores++;
				}
			}

			if (worstPlan != null) {
				nofScoreWorst++;
				sumScoreWorst += worstPlan.getScore();
			}
			if (bestPlan != null) {
				nofScoreBest++;
				sumScoreBest += bestPlan.getScore();
			}
			if (cntScores > 0) {
				sumAvgScores += (sumScores / cntScores);
				nofAvgScores++;
			}
		}
		log.info("-- avg. score of the executed plan of each agent: " + (sumExecutedScores / nofExecutedScores));
		log.info("-- avg. score of the worst plan of each agent: " + (sumScoreWorst / nofScoreWorst));
		log.info("-- avg. of the avg. plan score per agent: " + (sumAvgScores / nofAvgScores));
		log.info("-- avg. score of the best plan of each agent: " + (sumScoreBest / nofScoreBest));

		try {
			this.out.write(event.getIteration() + "\t" + (sumExecutedScores / nofExecutedScores) + "\t" +
					(sumScoreWorst / nofScoreWorst) + "\t" + (sumAvgScores / nofAvgScores) + "\t" + (sumScoreBest / nofScoreBest) + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_WORST][index] = (sumScoreWorst / nofScoreWorst);
			this.history[INDEX_BEST][index] = (sumScoreBest / nofScoreBest);
			this.history[INDEX_AVERAGE][index] = (sumAvgScores / nofAvgScores);
			this.history[INDEX_EXECUTED][index] = (sumExecutedScores / nofExecutedScores);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history[INDEX_WORST], 0, values, 0, index + 1);
				chart.addSeries("avg. worst score", iterations, values);
				System.arraycopy(this.history[INDEX_BEST], 0, values, 0, index + 1);
				chart.addSeries("avg. best score", iterations, values);
				System.arraycopy(this.history[INDEX_AVERAGE], 0, values, 0, index + 1);
				chart.addSeries("avg. of plans' average score", iterations, values);
				System.arraycopy(this.history[INDEX_EXECUTED], 0, values, 0, index + 1);
				chart.addSeries("avg. executed score", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(Controler.getOutputFilename("scorestats.png"), 800, 600);
			}
			if (index == this.history[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}

	public void notifyShutdown(final ControlerShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
