/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreTest.java
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

/**
 *
 */
package playground.yu.test;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class ScoreTest extends PersonAlgorithm {
	private double sumScoreWorst = 0.0, sumScoreBest = 0.0, sumAvgScores = 0.0,
			sumExecutedScores = 0.0;
	private int nofScoreWorst = 0, nofScoreBest = 0, nofAvgScores = 0,
			nofExecutedScores = 0;
	private BufferedWriter writer = null;

	/**
	 * 
	 */
	public ScoreTest(final String outputFilename) {
		try {
			writer = IOUtils.getBufferedWriter(outputFilename);
			writer.write("of each agent\tavg. score\tsum\tn\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(final Person person) {
		Plan worstPlan = null;
		Plan bestPlan = null;
		double sumScores = 0.0;
		double cntScores = 0;
		for (Plan plan : person.getPlans()) {

			if (BasicPlanImpl.isUndefinedScore(plan.getScore()))
				continue;
			// worst plan
			if (worstPlan == null)
				worstPlan = plan;
			else if (plan.getScore() < worstPlan.getScore())
				worstPlan = plan;
			// best plan
			if (bestPlan == null)
				bestPlan = plan;
			else if (plan.getScore() > bestPlan.getScore())
				bestPlan = plan;
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
			sumAvgScores += sumScores / cntScores;
			nofAvgScores++;
		}
	}

	public void end() {
		try {
			writer.write("executed: " + "\t"
					+ sumExecutedScores / nofExecutedScores + "\t"
					+ sumExecutedScores + "\t" + nofExecutedScores
					+ "\nworst: \t" + sumScoreWorst / nofScoreWorst + "\t"
					+ sumScoreWorst + "\t" + nofScoreWorst + "\navg.: \t"
					+ sumAvgScores / nofAvgScores + "\t" + sumAvgScores + "\t"
					+ nofAvgScores + "\nbest: \t" + sumScoreBest / nofScoreBest
					+ "\t" + sumScoreBest + "\t" + nofScoreBest + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// final String netFilename = "../data/ivtch/input/network.xml";
		final String netFilename = "test/yu/equil_test/equil_net.xml";
		// final String plansFilename =
		// "../data/ivtch/run265optChg_run270/output_plans.xml.gz";
		// final String plansFilename =
		// "../data/ivtch/input/_10pctZrhCarPtPlans_opt.xml.gz";
		// final String plansFilename = "test/yu/equil_test/100.plans.xml.gz";
		// final String plansFilename =
		// "test/yu/equil_test/equil269/output_plans.xml.gz";
		final String plansFilename = "test/yu/equil_test/equil269/ITERS/it.90/90.plans.xml.gz";
		// final String outputFilename =
		// "../data/ivtch/run265optChg_run270/scoreTest_input.txt";
		final String outputFilename = "test/yu/equil_test/equil269/90.scoreTest.txt";

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();

		ScoreTest st = new ScoreTest(outputFilename);
		population.addAlgorithm(st);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);

		population.runAlgorithms();

		st.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
