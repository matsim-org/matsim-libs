/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.Carriers;

/**
 * As you can see, it is basically a copy of {@link org.matsim.analysis.ScoreStatsControlerListener}. However, it is modified to score {@link Carrier}s
 * rather than Persons. (Oct'13, schroeder)
 * <p>
 *
 * <p>Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average score of the selected plan</li>
 * <li>average of the score of the worst plan of each agent</li>
 * <li>average of the score of the best plan of each agent</li>
 * <li>average of the average score of all plans of each agent</li>
 * </ul>
 * Plans with undefined scores
 * are not included in the statistics. The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author mrieser
 */
public class CarrierScoreStats implements StartupListener, IterationEndsListener, ShutdownListener {

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	private BufferedWriter out;
	final private String fileName;

	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;

	private final Carriers carriers;

	private final static Logger log = LogManager.getLogger(CarrierScoreStats.class);

	/**
	 * Creates a new ScoreStats instance.
	 *
	 * @param filename including the path, excluding the file type extension
	 * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
	 */
	public CarrierScoreStats(Carriers carriers, final String filename, final boolean createPNG) throws UncheckedIOException {
		this.carriers = carriers;
		this.fileName = filename;
		this.createPNG = createPNG;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		if (fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
			this.out = IOUtils.getBufferedWriter(fileName);
		} else {
			this.out = IOUtils.getBufferedWriter(fileName + ".txt");
		}
		try {
			this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.minIteration = event.getServices().getConfig().controller().getFirstIteration();
		int maxIter = event.getServices().getConfig().controller().getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 5000) iterations = 5000; // limit the history size
		this.history = new double[4][iterations+1];
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		double sumScoreWorst = 0.0;
		double sumScoreBest = 0.0;
		double sumAvgScores = 0.0;
		double sumExecutedScores = 0.0;
		int nofScoreWorst = 0;
		int nofScoreBest = 0;
		int nofAvgScores = 0;
		int nofExecutedScores = 0;
//		int nofExecutedIvPlans = 0;
//		int nofExecutedOevPlans = 0;

//		for (Person person : this.population.getPersons().values()) {
		for (Carrier carrier : carriers.getCarriers().values()){
			CarrierPlan worstPlan = null;
			CarrierPlan bestPlan = null;
			double worstScore = Double.POSITIVE_INFINITY;
			double bestScore = Double.NEGATIVE_INFINITY;
			double sumScores = 0.0;
			double cntScores = 0;
			for (CarrierPlan plan : carrier.getPlans()) {

				if (plan.getScore() == null) {
					continue;
				}
				double score = plan.getScore();

				// worst plan
				if (worstPlan == null) {
					worstPlan = plan;
					worstScore = score;
				} else if (score < worstScore) {
					worstPlan = plan;
					worstScore = score;
				}

				// best plan
				if (bestPlan == null) {
					bestPlan = plan;
					bestScore = score;
				} else if (score > bestScore) {
					bestPlan = plan;
					bestScore = score;
				}

				// avg. score
				sumScores += score;
				cntScores++;

				// executed plan?
				if (carrier.getSelectedPlan().equals(plan)) {
					sumExecutedScores += score;
					nofExecutedScores++;
				}
			}

			if (worstPlan != null) {
				nofScoreWorst++;
				sumScoreWorst += worstScore;
			}
			if (bestPlan != null) {
				nofScoreBest++;
				sumScoreBest += bestScore;
			}
			if (cntScores > 0) {
				sumAvgScores += (sumScores / cntScores);
				nofAvgScores++;
			}
		}
		log.info("-- avg. score of the executed plan of each agent: {}", sumExecutedScores / nofExecutedScores);
		log.info("-- avg. score of the worst plan of each agent: {}", sumScoreWorst / nofScoreWorst);
		log.info("-- avg. of the avg. plan score per agent: {}", sumAvgScores / nofAvgScores);
		log.info("-- avg. score of the best plan of each agent: {}", sumScoreBest / nofScoreBest);

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

			if (this.createPNG && event.getIteration() != this.minIteration) {
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
				chart.saveAsPng(this.fileName + ".png", 800, 600);
			}
			if (index == (this.history[0].length - 1)) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShutdownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
