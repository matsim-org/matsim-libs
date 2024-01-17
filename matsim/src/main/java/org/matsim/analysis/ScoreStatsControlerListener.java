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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

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
public class ScoreStatsControlerListener implements StartupListener, IterationEndsListener, ShutdownListener, ScoreStats {

	public enum ScoreItem { worst, best, average, executed }

	private final Population population;
	private final OutputDirectoryHierarchy controllerIO;
	private final String delimiter;
	private final BufferedWriter out;

	private final ControllerConfigGroup controllerConfigGroup;

	private final Map<ScoreItem, Map< Integer, Double>> scoreHistory = new HashMap<>();

	private final Map<String, ScoreHist> perSubpop = new HashMap<>();

	private int minIteration = 0;

	private final static Logger log = LogManager.getLogger(ScoreStatsControlerListener.class);

	@Inject
	ScoreStatsControlerListener(ControllerConfigGroup controllerConfigGroup, Population population, OutputDirectoryHierarchy controllerIO,
															GlobalConfigGroup globalConfig ) {
		this.controllerConfigGroup = controllerConfigGroup;
		this.population = population;
		this.controllerIO = controllerIO;
		this.delimiter = globalConfig.getDefaultDelimiter();
		this.out = IOUtils.getBufferedWriter(controllerIO.getOutputFilename("scorestats.csv"));

		Set<String> subpopulations = population.getPersons().values().stream()
			.map(PopulationUtils::getSubpopulation)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		for (String sub : subpopulations) {
			this.perSubpop.put(sub, new ScoreHist(new HashMap<>(), IOUtils.getBufferedWriter(controllerIO.getOutputFilename("scorestats_" + sub + ".csv"))));
		}

		try {
			this.out.write("iteration" + this.delimiter + "avg_executed" + this.delimiter
				+ "avg_worst" + this.delimiter + "avg_average" + this.delimiter + "avg_best\n");
			for (Map.Entry<String, ScoreHist> e : this.perSubpop.entrySet()) {
				e.getValue().out.write("iteration" + this.delimiter + "avg_executed" + this.delimiter
					+ "avg_worst" + this.delimiter + "avg_average" + this.delimiter + "avg_best\n");
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		this.minIteration = this.controllerConfigGroup.getFirstIteration();
		//		int maxIter = controlerConfigGroup.getLastIteration();
		//		int iterations = maxIter - this.minIteration;
		//		if (iterations > 5000) iterations = 5000; // limit the history size
		for ( ScoreItem item : ScoreItem.values() ) {
			this.scoreHistory.put( item, new TreeMap<>() ) ;
			this.perSubpop.forEach((s, data) -> data.hist.put(item, new TreeMap<>()));
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		collectScoreInfo(event);
		if (isWriteGraph(event)) {
			writePng();
		}
	}

	private boolean isWriteGraph(IterationEndsEvent event) {
		// create chart when data of more than one iteration is available.
		return this.controllerConfigGroup.getCreateGraphsInterval() > 0 &&
			event.getIteration() % this.controllerConfigGroup.getCreateGraphsInterval() == 0 &&
			event.getIteration() > this.minIteration;
	}

	private void collectScoreInfo(final IterationEndsEvent event) {
		ScoreInfo info = new ScoreInfo();

		Map<String, ScoreInfo> perSubpop = new HashMap<>();
		this.perSubpop.forEach((subpop, d) -> perSubpop.put(subpop, new ScoreInfo()));

		for (Person person : this.population.getPersons().values()) {
			info.update(person);
			String subpop = PopulationUtils.getSubpopulation(person);
			if (subpop != null)
				perSubpop.get(subpop).update(person);
		}


		log.info("-- avg. score of the executed plan of each agent: " + (info.sumExecutedScores / info.nofExecutedScores));
		log.info("-- avg. score of the worst plan of each agent: " + (info.sumScoreWorst / info.nofScoreWorst));
		log.info("-- avg. of the avg. plan score per agent: " + (info.sumAvgScores / info.nofAvgScores));
		log.info("-- avg. score of the best plan of each agent: " + (info.sumScoreBest / info.nofScoreBest));

		try {
			info.write(event.getIteration(), this.out, this.delimiter);
			for (Map.Entry<String, ScoreHist> e : this.perSubpop.entrySet()) {
				perSubpop.get(e.getKey()).write(event.getIteration(), e.getValue().out, this.delimiter);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

//		int index = event.getIteration() - this.minIteration;

		this.scoreHistory.get( ScoreItem.worst ).put( event.getIteration(), info.sumScoreWorst / info.nofScoreWorst ) ;
		this.scoreHistory.get( ScoreItem.best ).put( event.getIteration(), info.sumScoreBest / info.nofScoreBest ) ;
		this.scoreHistory.get( ScoreItem.average ).put( event.getIteration(), info.sumAvgScores / info.nofAvgScores ) ;
		this.scoreHistory.get( ScoreItem.executed ).put( event.getIteration(), info.sumExecutedScores / info.nofExecutedScores ) ;
	}

	private void writePng() {
		XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
		chart.addSeries("avg. worst score", this.scoreHistory.get( ScoreItem.worst ) ) ;
		chart.addSeries("avg. best score", this.scoreHistory.get( ScoreItem.best) );
		chart.addSeries("avg. of plans' average score", this.scoreHistory.get( ScoreItem.average) );
		chart.addSeries("avg. executed score", this.scoreHistory.get( ScoreItem.executed ) );
		chart.addMatsimLogo();
		chart.saveAsPng(this.controllerIO.getOutputFilename("scorestats.png"), 800, 600);
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
			for (ScoreHist data : this.perSubpop.values()) {
				data.out.close();
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public Map<ScoreItem, Map<Integer, Double>> getScoreHistory() {
		return Collections.unmodifiableMap( this.scoreHistory ) ;
	}

	private record ScoreHist(Map<ScoreItem, Map< Integer, Double>> hist, BufferedWriter out) {}

	private static final class ScoreInfo {
		double sumScoreWorst = 0.0;
		double sumScoreBest = 0.0;
		double sumAvgScores = 0.0;
		double sumExecutedScores = 0.0;
		int nofScoreWorst = 0;
		int nofScoreBest = 0;
		int nofAvgScores = 0;
		int nofExecutedScores = 0;

		private void update(Person person) {
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
				if (PersonUtils.isSelected(plan)) {
					this.sumExecutedScores += score;
					this.nofExecutedScores++;
				}
			}

			if (worstPlan != null) {
				this.nofScoreWorst++;
				this.sumScoreWorst += worstScore;
			}
			if (bestPlan != null) {
				this.nofScoreBest++;
				this.sumScoreBest += bestScore;
			}
			if (cntScores > 0) {
				this.sumAvgScores += (sumScores / cntScores);
				this.nofAvgScores++;
			}
		}

		private void write(int iteration, BufferedWriter out, String delimiter) throws IOException {
			out.write(iteration + delimiter
				+ (this.sumExecutedScores / this.nofExecutedScores) + delimiter
				+ (this.sumScoreWorst / this.nofScoreWorst) + delimiter
				+ (this.sumAvgScores / this.nofAvgScores) + delimiter
				+ (this.sumScoreBest / this.nofScoreBest) + "\n");
			out.flush();
		}

	}
}
