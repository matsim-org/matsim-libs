/* *********************************************************************** *
 * project: org.matsim.*
 * CliqueScoreStats.java
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

package playground.thibautd.analysis.listeners;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.utils.BoxAndWhiskersChart;
import playground.thibautd.utils.BoxAndWhiskerXYNumberDataset;
import playground.thibautd.utils.XYChartUtils;

/**
 * Calculates at the end of each iteration the following statistics:
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
public class CliqueScoreStats implements StartupListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(CliqueScoreStats.class);

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	//final private PopulationWithCliques population;
	final private List<CliqueSizeInfo> infoPerSize = new ArrayList<CliqueSizeInfo>();

	private final String fileName;
	private boolean createPNG;
	private int minIteration = 0;

	// /////////////////////////////////////////////////////////////////////////
	// constructor and relatives
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates a new CliqueScoreStats instance.
	 *
	 * @param population the population, which cliques must remain unchanged during the
	 * iterations
	 * @param filename
	 * @param createPNG true if in every iteration, the scorestats should be
	 * visualized in a graph and written to disk.
	 * @throws UncheckedIOException
	 */
	public CliqueScoreStats(
			final String fileName,
			final boolean createPNG) throws UncheckedIOException {
		//this.population = population;
		this.createPNG = createPNG;
		this.fileName = fileName;
	}

	private Map<Integer, List<Clique>> getCliques(final PopulationWithCliques pop) {
		Map<Integer, List<Clique>> output = new HashMap<Integer, List<Clique>>();
		int currentSize;
		List<Clique> currentList;

		for (Clique clique : pop.getCliques().getCliques().values()) {
			currentSize = clique.getMembers().size();

			currentList = output.get(currentSize);
			if (currentList == null) {
				currentList = new ArrayList<Clique>();
				output.put(currentSize, currentList);
			}

			currentList.add(clique);
		}

		return output;
	}

	// /////////////////////////////////////////////////////////////////////////
	// listener methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		this.minIteration = controler.getFirstIteration();
		int maxIter = controler.getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 10000) iterations = 1000; // limit the history size
		BufferedWriter out;
		int cliqueSize;

		Map<Integer, List<Clique>> cliquesPerSize =
			getCliques((PopulationWithCliques) controler.getPopulation());

		try {
			for (Map.Entry<Integer, List<Clique>> entry : cliquesPerSize.entrySet()) {
				cliqueSize = entry.getKey();
				out = IOUtils.getBufferedWriter(
						controler.getControlerIO().getOutputFilename(
							fileName+"-size-"+cliqueSize+".txt"));
				out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
				this.infoPerSize.add(new CliqueSizeInfo(entry.getValue(), out, iterations));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		for (CliqueSizeInfo info : this.infoPerSize) {
			notifyIterationEnds(event, info);
		}
	}

	private void notifyIterationEnds(
			final IterationEndsEvent event,
			final CliqueSizeInfo info) {
		double sumScoreWorst = 0.0;
		double sumScoreBest = 0.0;
		double sumAvgScores = 0.0;
		double sumExecutedScores = 0.0;
		int nofScoreWorst = 0;
		int nofScoreBest = 0;
		int nofAvgScores = 0;
		int nofExecutedScores = 0;
		int iteration = event.getIteration();

		for (Clique clique : info.cliques) {
			for (Person person : clique.getMembers().values()) {
				Plan worstPlan = null;
				Plan bestPlan = null;
				double worstScore = Double.POSITIVE_INFINITY;
				double bestScore = Double.NEGATIVE_INFINITY;
				double sumScores = 0.0;
				double cntScores = 0;

				for (Plan plan : person.getPlans()) {
					if (plan.getScore() == null) {
						continue;
					}
					double score = plan.getScore().doubleValue();

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
					if (plan.isSelected()) {
						sumExecutedScores += score;
						nofExecutedScores++;
						info.nJointTripsChart.add(iteration, getNumberOfPickUps(plan));
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
		}
		log.info("-------> INFO FOR AGENTS IN CLIQUES OF SIZE "+info.cliqueSize);
		log.info("-- avg. score of the executed plan of each agent: " + (sumExecutedScores / nofExecutedScores));
		log.info("-- avg. score of the worst plan of each agent: " + (sumScoreWorst / nofScoreWorst));
		log.info("-- avg. of the avg. plan score per agent: " + (sumAvgScores / nofAvgScores));
		log.info("-- avg. score of the best plan of each agent: " + (sumScoreBest / nofScoreBest));

		try {
			info.out.write(event.getIteration() + "\t" + (sumExecutedScores / nofExecutedScores) + "\t" +
					(sumScoreWorst / nofScoreWorst) + "\t" + (sumAvgScores / nofAvgScores) + "\t" + (sumScoreBest / nofScoreBest) + "\n");
			info.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.createPNG) {
			int index = event.getIteration() - this.minIteration;
			info.history[INDEX_WORST][index] = (sumScoreWorst / nofScoreWorst);
			info.history[INDEX_BEST][index] = (sumScoreBest / nofScoreBest);
			info.history[INDEX_AVERAGE][index] = (sumAvgScores / nofAvgScores);
			info.history[INDEX_EXECUTED][index] = (sumExecutedScores / nofExecutedScores);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart(
						"Score Statistics, cliques of "+info.cliqueSize+" agents",
						"iteration",
						"score");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(info.history[INDEX_WORST], 0, values, 0, index + 1);
				chart.addSeries("avg. worst score", iterations, values);
				System.arraycopy(info.history[INDEX_BEST], 0, values, 0, index + 1);
				chart.addSeries("avg. best score", iterations, values);
				System.arraycopy(info.history[INDEX_AVERAGE], 0, values, 0, index + 1);
				chart.addSeries("avg. of plans' average score", iterations, values);
				System.arraycopy(info.history[INDEX_EXECUTED], 0, values, 0, index + 1);
				chart.addSeries("avg. executed score", iterations, values);
				chart.addMatsimLogo();
				XYChartUtils.integerXAxis(chart.getChart());
				chart.saveAsPng(event.getControler().getControlerIO().getOutputFilename(
							"scorestats-size-"+info.cliqueSize+".png"), 800, 600);
			}
			if (index == info.history[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.createPNG = false;
			}
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		try {
			for (CliqueSizeInfo info : this.infoPerSize) {
				info.out.close();
			}
		} catch (IOException e) {
			log.error("error while writing to file");
			e.printStackTrace();
		}

		for (CliqueSizeInfo info : this.infoPerSize) {
			XYChartUtils.integerAxes(info.nJointTripsChart.getChart());
			info.nJointTripsChart.saveAsPng(event.getControler().getControlerIO().getOutputFilename(
						"jointTrips-size-"+info.cliqueSize+".png"), 800, 600);
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private class CliqueSizeInfo {
		public final List<Clique> cliques;
		public final int cliqueSize;
		public final BufferedWriter out;
		public final double[][] history;
		public final BoxAndWhiskersChart nJointTripsChart;

		public CliqueSizeInfo(
				final List<Clique> cliques,
				final BufferedWriter writer,
				final int iterations) {
			this.cliques = cliques;
			this.cliqueSize = cliques.get(0).getMembers().size();
			this.out = writer;
			this.history = new double[4][iterations+1];
			this.nJointTripsChart = new BoxAndWhiskersChart(
					"number of pick ups per individual, cliques of size "+ this.cliqueSize,
					"iteration",
					"number of pick ups",
					1);
		}
	}

	private int getNumberOfPickUps(final Plan plan) {
		int count = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if ((pe instanceof Activity) &&
					((((Activity) pe).getType()).equals(JointActingTypes.PICK_UP))) {
				count++;
			}
		}
		return count;
	}
}
