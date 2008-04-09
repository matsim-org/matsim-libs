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

import org.matsim.config.Config;
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
	public ScoreTest(String outputFilename) {
		try {
			this.writer = IOUtils.getBufferedWriter(outputFilename);
			this.writer.write("of each agent\tavg. score\tsum\tn\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(Person person) {
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
				this.sumExecutedScores += plan.getScore();
				this.nofExecutedScores++;
			}
		}
		if (worstPlan != null) {
			this.nofScoreWorst++;
			this.sumScoreWorst += worstPlan.getScore();
		}
		if (bestPlan != null) {
			this.nofScoreBest++;
			this.sumScoreBest += bestPlan.getScore();
		}
		if (cntScores > 0) {
			this.sumAvgScores += (sumScores / cntScores);
			this.nofAvgScores++;
		}
	}

	public void end() {
		try {
			this.writer.write("executed: " + "\t"
					+ (this.sumExecutedScores / this.nofExecutedScores) + "\t"
					+ this.sumExecutedScores + "\t" + this.nofExecutedScores
					+ "\nworst: \t" + (this.sumScoreWorst / this.nofScoreWorst) + "\t"
					+ this.sumScoreWorst + "\t" + this.nofScoreWorst + "\navg.: \t"
					+ (this.sumAvgScores / this.nofAvgScores) + "\t" + this.sumAvgScores
					+ "\t" + this.nofAvgScores + "\nbest: \t"
					+ (this.sumScoreBest / this.nofScoreBest) + "\t" + this.sumScoreBest
					+ "\t" + this.nofScoreBest + "\n");
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// final String netFilename = "../data/ivtch/input/network.xml";
		final String netFilename = "test/yu/equil_test/equil_net.xml";
		// final String plansFilename =
		// "../data/ivtch/run265optChg_run270/output_plans.xml.gz";
		// final String plansFilename =
		// "../data/ivtch/input/_10pctZrhCarPtPlans_opt.xml.gz";
		// final String plansFilename = "test/yu/equil_test/100.plans.xml.gz";
//		final String plansFilename = "test/yu/equil_test/equil269/output_plans.xml.gz";
		final String plansFilename = "test/yu/equil_test/equil269/ITERS/it.90/90.plans.xml.gz";
		// final String outputFilename =
		// "../data/ivtch/run265optChg_run270/scoreTest_input.txt";
		final String outputFilename = "test/yu/equil_test/equil269/90.scoreTest.txt";

		Gbl.startMeasurement();
		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();

		ScoreTest st = new ScoreTest(outputFilename);
		population.addAlgorithm(st);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);
		world.setPopulation(population);

		population.runAlgorithms();

		st.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
