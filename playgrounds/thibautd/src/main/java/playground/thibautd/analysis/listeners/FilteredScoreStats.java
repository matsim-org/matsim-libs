/* *********************************************************************** *
 * project: org.matsim.*
 * FilteredScoreStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * Similar to the core score stats, but
 *
 * <ul>
 * <li> does not need a Controler
 * <li> is able to plot stats for only a part of the population
 * (eg agents in a clique of a certain size, agent without car, etc)
 * </ul>
 * @author thibautd
 */
public class FilteredScoreStats implements IterationEndsListener, ShutdownListener {

	private final List<Double> averages = new ArrayList<Double>();
	private final List<Double> bests = new ArrayList<Double>();
	private final List<Double> worsts = new ArrayList<Double>();
	private final List<Double> executeds = new ArrayList<Double>();

	final private Population population;
	final private BufferedWriter out;
	final private String fileName;

	private final static Logger log = Logger.getLogger(FilteredScoreStats.class);

	public static interface PersonFilter {
		public boolean acceptPerson(Person person);
		public String getName();
	}

	private final PersonFilter filter;

	/**
	 * Creates a new ScoreStats instance.
	 *
	 * @param population
	 * @param filename including the path, excluding the file type extension
	 * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
	 * @throws UncheckedIOException
	 */
	public FilteredScoreStats(
			final PersonFilter filter,
			final Population population,
			final String filename) throws UncheckedIOException {
		this.filter = filter;
		this.population = population;
		this.fileName = filename;
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
	public void notifyIterationEnds(final IterationEndsEvent event) {
		double sumScoreWorst = 0.0;
		double sumScoreBest = 0.0;
		double sumAvgScores = 0.0;
		double sumExecutedScores = 0.0;
		int nofScoreWorst = 0;
		int nofScoreBest = 0;
		int nofAvgScores = 0;
		int nofExecutedScores = 0;

		for (Person person : this.population.getPersons().values()) {
			if ( !filter.acceptPerson( person ) ) continue;
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
		log.info( "scores for filter "+filter.getName() );
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

		worsts.add( sumScoreWorst / nofScoreWorst );
		bests.add( sumScoreBest / nofScoreBest );
		averages.add( sumAvgScores / nofAvgScores );
		executeds.add( sumExecutedScores / nofExecutedScores );

		// create chart when data of more than one iteration is available.
		if (worsts.size() == 1) return;

		XYLineChart chart =
			new XYLineChart(
					"Score Statistics for "+filter.getName(),
					"iteration",
					"score");
		double[] iterations = new double[worsts.size() + 1];
		final int firstIter = event.getIteration() - (worsts.size() - 1);
		for (int i = 0; i < iterations.length ; i++) {
			iterations[i] = i + firstIter;
		}

		chart.addSeries(
				"avg. worst score",
				iterations,
				toArray( worsts ));
		chart.addSeries(
				"avg. best score",
				iterations,
				toArray( bests ));
		chart.addSeries(
				"avg. of plans' average score",
				iterations,
				toArray( averages ));
		chart.addSeries(
				"avg. executed score",
				iterations,
				toArray( executeds ));
		chart.addMatsimLogo();
		chart.saveAsPng(this.fileName + ".png", 800, 600);
	}

	final static double[] toArray( final List<Double> l ) {
		final double[] a = new double[ l.size() ];
		
		int i=0;
		for (Double d : l) a[ i++ ] = d;

		return a;
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

