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

import org.matsim.controler.events.ControlerFinishIterationEvent;
import org.matsim.controler.events.ControlerShutdownEvent;
import org.matsim.controler.listener.ControlerFinishIterationListener;
import org.matsim.controler.listener.ControlerShutdownListener;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
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
public class ScoreStats implements ControlerFinishIterationListener, ControlerShutdownListener {

	final private Plans population;
	final private BufferedWriter out;

	public ScoreStats(final Plans population, final String filename) throws FileNotFoundException, IOException {
		this.population = population;
		this.out = IOUtils.getBufferedWriter(filename);
		this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
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
		System.out.println("-- avg. score of the executed plan of each agent: " + (sumExecutedScores / nofExecutedScores));
		System.out.println("-- avg. score of the worst plan of each agent: " + (sumScoreWorst / nofScoreWorst));
		System.out.println("-- avg. of the avg. plan score per agent: " + (sumAvgScores / nofAvgScores));
		System.out.println("-- avg. score of the best plan of each agent: " + (sumScoreBest / nofScoreBest));

		try {
			this.out.write(event.getIteration() + "\t" + (sumExecutedScores / nofExecutedScores) + "\t" +
					(sumScoreWorst / nofScoreWorst) + "\t" + (sumAvgScores / nofAvgScores) + "\t" + (sumScoreBest / nofScoreBest) + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
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
