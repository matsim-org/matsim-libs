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

package playground.sergioo.typesPopulation2013.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
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

import playground.sergioo.typesPopulation2013.population.PersonImplPops;

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
public class ScoreStats implements StartupListener, IterationEndsListener, ShutdownListener {

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	final private Population population;
	final private Map<Id<Population>, BufferedWriter> out = new HashMap<Id<Population>, BufferedWriter>();
	private String fileName;
	
	private final boolean createPNG;
	private Map<Id<Population>, double[][]> history = new HashMap<Id<Population>, double[][]>();
	private int minIteration = 0;

	private final static Logger log = Logger.getLogger(ScoreStats.class);

	/**
	 * Creates a new ScoreStats instance.
	 *
	 * @param population
	 * @param filename including the path, excluding the file type extension
	 * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
	 * @throws UncheckedIOException
	 */
	public ScoreStats(final Population population, final String filename, final boolean createPNG) throws UncheckedIOException {
		this.population = population;
		this.fileName = filename;
		this.createPNG = createPNG;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		fileName =  event.getControler().getControlerIO().getOutputFilename(fileName);
		Controler controler = event.getControler();
		this.minIteration = controler.getConfig().controler().getFirstIteration();
		int maxIter = controler.getConfig().controler().getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 5000) iterations = 5000; // limit the history size
		for(Person person:population.getPersons().values()) {
			Id<Population> popId = ((PersonImplPops)person).getPopulationId();
			if(history.get(popId)==null)
				history.put(popId, new double[4][iterations+1]);
		}
		for(Id<Population> popId:history.keySet()) {
			if (this.fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
				this.out.put(popId, IOUtils.getBufferedWriter(this.fileName.substring(0, this.fileName.length()-4)+"_"+popId+".txt"));
			} else {
				this.out.put(popId, IOUtils.getBufferedWriter(this.fileName +"_"+popId+ ".txt"));
			}
			try {
				this.out.get(popId).write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		for(Entry<Id<Population>, double[][]> his:this.history.entrySet()) {
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
			for (Person person : this.population.getPersons().values())
				if(((PersonImplPops)person).getPopulationId().equals(his.getKey())) {
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
						if (plan.isSelected(plan)) {
							sumExecutedScores += score;
							nofExecutedScores++;
							//					if (plan.getType() == Plan.Type.CAR) {
							//						nofExecutedIvPlans ++;
							//					}
							//					else if (plan.getType() == Plan.Type.PT) {
							//						nofExecutedOevPlans++;
							//					}
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
			log.info("-- avg. score of the executed plan of each agent ("+his.getKey()+"): " + (sumExecutedScores / nofExecutedScores));
	//		log.info("-- number of executed plans: "  + nofExecutedScores);
	//		log.info("-- number of executed iv plans: "  + nofExecutedIvPlans);
	//		log.info("-- number of executed oev plans: "  + nofExecutedOevPlans);
	//		log.info("-- modal split iv: "  + ((nofExecutedScores == 0) ? 0 : ((double)nofExecutedIvPlans / (double)nofExecutedScores * 100d)) +
	//				" % oev: " + ((nofExecutedScores == 0) ? 0 : ((double)nofExecutedOevPlans / (double)nofExecutedScores * 100d)) + " %");
			log.info("-- avg. score of the worst plan of each agent ("+his.getKey()+"): " + (sumScoreWorst / nofScoreWorst));
			log.info("-- avg. of the avg. plan score per agent ("+his.getKey()+"): " + (sumAvgScores / nofAvgScores));
			log.info("-- avg. score of the best plan of each agent ("+his.getKey()+"): " + (sumScoreBest / nofScoreBest));
	
			try {
				this.out.get(his.getKey()).write(event.getIteration() + "\t" + (sumExecutedScores / nofExecutedScores) + "\t" +
						(sumScoreWorst / nofScoreWorst) + "\t" + (sumAvgScores / nofAvgScores) + "\t" + (sumScoreBest / nofScoreBest) + "\n");
				this.out.get(his.getKey()).flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
	
			if (this.history != null) {
				int index = event.getIteration() - this.minIteration;
				his.getValue()[INDEX_WORST][index] = (sumScoreWorst / nofScoreWorst);
				his.getValue()[INDEX_BEST][index] = (sumScoreBest / nofScoreBest);
				his.getValue()[INDEX_AVERAGE][index] = (sumAvgScores / nofAvgScores);
				his.getValue()[INDEX_EXECUTED][index] = (sumExecutedScores / nofExecutedScores);
	
				if (this.createPNG && event.getIteration() != this.minIteration) {
					// create chart when data of more than one iteration is available.
					XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
					double[] iterations = new double[index + 1];
					for (int i = 0; i <= index; i++) {
						iterations[i] = i + this.minIteration;
					}
					double[] values = new double[index + 1];
					System.arraycopy(his.getValue()[INDEX_WORST], 0, values, 0, index + 1);
					chart.addSeries("avg. worst score", iterations, values);
					System.arraycopy(his.getValue()[INDEX_BEST], 0, values, 0, index + 1);
					chart.addSeries("avg. best score", iterations, values);
					System.arraycopy(his.getValue()[INDEX_AVERAGE], 0, values, 0, index + 1);
					chart.addSeries("avg. of plans' average score", iterations, values);
					System.arraycopy(his.getValue()[INDEX_EXECUTED], 0, values, 0, index + 1);
					chart.addSeries("avg. executed score", iterations, values);
					chart.addMatsimLogo();
					chart.saveAsPng(this.fileName +"_"+his.getKey()+ ".png", 800, 600);
				}
				if (index == (his.getValue()[0].length - 1)) {
					// we cannot store more information, so disable the graph feature.
					this.history = null;
				}
			}
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			for(Id<Population> popId:history.keySet())
				this.out.get(popId).close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the history of scores in last iterations
	 */
	public Map<Id<Population>, double[][]> getHistory() {
		if (this.history == null) {
			return null;
		}
		Map<Id<Population>, double[][]> historyC = new HashMap<Id<Population>, double[][]>();
		for(Entry<Id<Population>, double[][]> his:history.entrySet()) {
			historyC.put(his.getKey(), his.getValue().clone());
		}
		return historyC;
	}
}
