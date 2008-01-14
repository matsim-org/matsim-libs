/* *********************************************************************** *
 * project: org.matsim.*
 * NewPtcheckControler.java
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

package playground.yu;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.matsim.analysis.ScoreStats;
import org.matsim.controler.Controler;

/**
 * test of PtCheck and PtRate, outputs Public-Transit user fraction
 * 
 * @author ychen
 * 
 */
public class NewPtcheckControler extends Controler {
	/**
	 * internal outputStream
	 */
	private DataOutputStream out;
	private ScoreStats scoreStats = null;

	/**
	 * adds a ControlerListener to Controler - PtRate
	 */
	@Override
	protected void loadData() {
		loadWorld();
		this.network = loadNetwork();
		loadFacilities();
		this.population = loadPopulation();
		try {
			// TODO [MR] I "abuse" createLegHistogramPNG here for ScoreStats...
			// create an own flag for this one.
			scoreStats = new ScoreStats(this.population,
					getOutputFilename("scorestats.txt"), true);
			addControlerListener(new PtRate(population,
					getOutputFilename("PtRate.txt"), getMaximumIteration(),
					config.getParam("planCalcScore", "traveling"), config
							.getParam("planCalcScore", "travelingPt")));
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(
							getOutputFilename("tollPaid.txt")))));
			out
					.writeBytes("Iter\tBetaTraveling\tBetaTravelingPt\ttoll_amount[€/m]\tavg. executed score\ttoll_paid[€]\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (scoreStats != null) {
			this.addControlerListener(scoreStats);
		}
	}

	@Override
	protected void finishIteration(int iteration) {
		super.finishIteration(iteration);
		try {
			out.writeBytes(iteration + "\t"
					+ config.getParam("planCalcScore", "traveling") + "\t"
					+ config.getParam("planCalcScore", "travelingPt") + "\t"
					+ toll.getCostArray()[0].amount+"\t" + scoreStats.getHistory()[3][iteration]
					+ ((tollCalc != null) ? tollCalc.getAllAgentsToll() : 0.0)+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// -------------------------MAIN FUNCTION--------------------
	/**
	 * @param args -
	 *            the path of config-file
	 */
	public static void main(final String[] args) {
		final NewPtcheckControler controler;
		controler = new NewPtcheckControler();
		controler.run(args);
		try {
			controler.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
